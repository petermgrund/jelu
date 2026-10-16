package io.github.bayang.jelu.service

import io.github.bayang.jelu.bookDto
import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.createUserBookDto
import io.github.bayang.jelu.dao.OwnershipStatus
import io.github.bayang.jelu.dao.ReadingEventType
import io.github.bayang.jelu.dto.CreateOwnershipEventDto
import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.LibraryFilter
import io.github.bayang.jelu.dto.UpdateOwnershipEventDto
import io.github.bayang.jelu.dto.UserBookBulkUpdateDto
import io.github.bayang.jelu.dto.UserBookUpdateDto
import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.errors.JeluNotFoundException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import java.io.File
import java.time.OffsetDateTime
import java.time.ZoneOffset

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OwnershipEventServiceTest(
    @Autowired private val bookService: BookService,
    @Autowired private val userService: UserService,
    @Autowired private val jeluProperties: JeluProperties,
    @Autowired private val readingEventService: ReadingEventService,
    @Autowired private val ownershipEventService: OwnershipEventService,
) {
    companion object {
        @TempDir
        lateinit var tempDir: File
    }

    @BeforeAll
    fun setupUser() {
        userService.save(CreateUserDto(login = "owner", password = "1234", admin = true))
        userService.save(CreateUserDto(login = "someone-else", password = "1234", admin = false))
        jeluProperties.files.images = tempDir.absolutePath
    }

    @AfterAll
    fun teardDown() {
        userService.findAll(null).forEach { userService.deleteUser(it.id!!) }
    }

    @AfterEach
    fun cleanTest() {
        readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).content.forEach {
            readingEventService.deleteReadingEventById(it.id!!)
        }
        bookService
            .findUserBookByCriteria(user().id!!, null, null, null, null, null, Pageable.ofSize(30))
            .forEach { bookService.deleteUserBookById(it.id!!) }
        bookService.findAllAuthors(null, pageable = Pageable.ofSize(30)).forEach {
            bookService.deleteAuthorById(it.id!!)
        }
        bookService.findAll(null, null, null, null, null, null, null, null, Pageable.ofSize(30), user(), LibraryFilter.ANY).forEach {
            bookService.deleteBookById(it.id!!)
        }
    }

    @Test
    fun testOwnedBookStartsItsHistory() {
        val saved = bookService.save(createUserBookDto(bookDto(), owned = true), user(), null)
        val history = ownershipEventService.findAll(saved.id!!, user())
        Assertions.assertEquals(1, history.size)
        Assertions.assertEquals(OwnershipStatus.OWNED, history[0].status)
        Assertions.assertNull(history[0].eventDate)
        Assertions.assertEquals(1, bookService.findUserBookById(saved.id!!).ownershipEvents?.size)
    }

    @Test
    fun testBookNeitherOwnedNorBorrowedHasNoHistory() {
        val saved = bookService.save(createUserBookDto(bookDto(), owned = false), user(), null)
        Assertions.assertTrue(ownershipEventService.findAll(saved.id!!, user()).isEmpty())
    }

    @Test
    fun testBorrowedThenBought() {
        val saved = bookService.save(createUserBookDto(bookDto(), owned = false), user(), null)
        ownershipEventService.save(saved.id!!, CreateOwnershipEventDto(OwnershipStatus.BORROWED, date(2023, 5, 1), " Library "), user())
        var userBook = bookService.findUserBookById(saved.id!!)
        Assertions.assertEquals(false, userBook.owned)
        Assertions.assertEquals(true, userBook.borrowed)

        ownershipEventService.save(saved.id!!, CreateOwnershipEventDto(OwnershipStatus.OWNED, date(2024, 1, 10), "Bought"), user())
        userBook = bookService.findUserBookById(saved.id!!)
        Assertions.assertEquals(true, userBook.owned)
        Assertions.assertEquals(false, userBook.borrowed)

        val history = ownershipEventService.findAll(saved.id!!, user())
        Assertions.assertEquals(listOf(OwnershipStatus.BORROWED, OwnershipStatus.OWNED), history.map { it.status })
        Assertions.assertEquals("Library", history[0].note)
        Assertions.assertEquals(listOf(OwnershipStatus.BORROWED, OwnershipStatus.OWNED), userBook.ownershipEvents?.map { it.status })
    }

    @Test
    fun testPastEntryKeepsCurrentState() {
        // owned since an unknown date, the user then remembers borrowing it years ago
        val saved = bookService.save(createUserBookDto(bookDto(), owned = true), user(), null)
        ownershipEventService.save(saved.id!!, CreateOwnershipEventDto(OwnershipStatus.BORROWED, date(2019, 3, 2), null), user())
        val userBook = bookService.findUserBookById(saved.id!!)
        Assertions.assertEquals(true, userBook.owned)
        Assertions.assertEquals(false, userBook.borrowed)
        Assertions.assertEquals(listOf(OwnershipStatus.BORROWED, OwnershipStatus.OWNED), userBook.ownershipEvents?.map { it.status })
    }

    @Test
    fun testUpdateAndDeleteRecomputeCurrentState() {
        val saved = bookService.save(createUserBookDto(bookDto(), owned = false), user(), null)
        val borrowed =
            ownershipEventService.save(
                saved.id!!,
                CreateOwnershipEventDto(OwnershipStatus.BORROWED, date(2023, 5, 1), null),
                user(),
            )
        val gone =
            ownershipEventService.save(
                saved.id!!,
                CreateOwnershipEventDto(OwnershipStatus.NOT_OWNED, date(2023, 6, 1), "Returned"),
                user(),
            )
        Assertions.assertEquals(false, bookService.findUserBookById(saved.id!!).borrowed)

        // the return happened before borrowing it again
        ownershipEventService.update(
            saved.id!!,
            gone.id!!,
            UpdateOwnershipEventDto(OwnershipStatus.NOT_OWNED, date(2023, 4, 1), "Returned"),
            user(),
        )
        Assertions.assertEquals(true, bookService.findUserBookById(saved.id!!).borrowed)

        ownershipEventService.delete(saved.id!!, borrowed.id!!, user())
        val userBook = bookService.findUserBookById(saved.id!!)
        Assertions.assertEquals(false, userBook.borrowed)
        Assertions.assertEquals(false, userBook.owned)
        Assertions.assertEquals(1, userBook.ownershipEvents?.size)
    }

    @Test
    fun testFlagChangesAreRecorded() {
        val saved = bookService.save(createUserBookDto(bookDto(), owned = true), user(), null)
        val unchanged = userBookUpdate(owned = true, borrowed = null)
        bookService.update(saved.id!!, unchanged)
        Assertions.assertEquals(1, ownershipEventService.findAll(saved.id!!, user()).size)

        val updated = bookService.update(saved.id!!, userBookUpdate(owned = false, borrowed = true))
        Assertions.assertEquals(false, updated.owned)
        Assertions.assertEquals(true, updated.borrowed)
        val history = ownershipEventService.findAll(saved.id!!, user())
        Assertions.assertEquals(listOf(OwnershipStatus.OWNED, OwnershipStatus.BORROWED), history.map { it.status })
    }

    @Test
    fun testBulkEditRecordsOwnership() {
        val first = bookService.save(createUserBookDto(bookDto("first"), owned = false), user(), null)
        val second = bookService.save(createUserBookDto(bookDto("second"), owned = true), user(), null)
        val count =
            bookService.bulkEditUserbooks(
                UserBookBulkUpdateDto(
                    ids = listOf(first.id!!, second.id!!),
                    toRead = null,
                    owned = true,
                    removeTags = null,
                    addTags = null,
                ),
            )
        Assertions.assertEquals(2, count)
        Assertions.assertEquals(true, bookService.findUserBookById(first.id!!).owned)
        Assertions.assertEquals(listOf(OwnershipStatus.OWNED), ownershipEventService.findAll(first.id!!, user()).map { it.status })
        // already owned, nothing new to record
        Assertions.assertEquals(1, ownershipEventService.findAll(second.id!!, user()).size)
    }

    @Test
    fun testOtherUsersCannotReadOrChangeTheHistory() {
        val saved = bookService.save(createUserBookDto(bookDto(), owned = true), user(), null)
        val event = ownershipEventService.findAll(saved.id!!, user()).first()
        assertThrows<JeluNotFoundException> { ownershipEventService.findAll(saved.id!!, otherUser()) }
        assertThrows<JeluNotFoundException> {
            ownershipEventService.save(saved.id!!, CreateOwnershipEventDto(OwnershipStatus.NOT_OWNED, null, null), otherUser())
        }
        assertThrows<JeluNotFoundException> { ownershipEventService.delete(saved.id!!, event.id!!, otherUser()) }
        Assertions.assertEquals(true, bookService.findUserBookById(saved.id!!).owned)
    }

    @Test
    fun testStartingToReadEndsWantToRead() {
        val saved = bookService.save(createUserBookDto(bookDto(), toRead = true), user(), null)
        Assertions.assertEquals(true, saved.toRead)
        readingEventService.save(CreateReadingEventDto(ReadingEventType.CURRENTLY_READING, saved.book.id, null, null), user())
        Assertions.assertNull(bookService.findUserBookById(saved.id!!).toRead)
    }

    @Test
    fun testFinishingWithoutStartingEndsWantToRead() {
        val saved = bookService.save(createUserBookDto(bookDto(), toRead = true), user(), null)
        val updated = bookService.update(saved.id!!, userBookUpdate(lastReadingEvent = ReadingEventType.FINISHED))
        Assertions.assertEquals(ReadingEventType.FINISHED, updated.lastReadingEvent)
        Assertions.assertNull(updated.toRead)
    }

    @Test
    fun testPastReadingKeepsWantToRead() {
        // read years ago and wants to read it again
        val saved = bookService.save(createUserBookDto(bookDto(), ReadingEventType.FINISHED, date(2024, 2, 1)), user(), null)
        bookService.update(saved.id!!, userBookUpdate(toRead = true))
        readingEventService.save(
            CreateReadingEventDto(ReadingEventType.FINISHED, saved.book.id, date(2020, 2, 1), date(2020, 1, 1)),
            user(),
        )
        val userBook = bookService.findUserBookById(saved.id!!)
        Assertions.assertEquals(true, userBook.toRead)
        Assertions.assertEquals(2, userBook.readingEvents?.size)
    }

    private fun date(
        year: Int,
        month: Int,
        day: Int,
    ): OffsetDateTime = OffsetDateTime.of(year, month, day, 10, 0, 0, 0, ZoneOffset.UTC)

    private fun userBookUpdate(
        owned: Boolean? = null,
        borrowed: Boolean? = null,
        toRead: Boolean? = null,
        lastReadingEvent: ReadingEventType? = null,
    ) = UserBookUpdateDto(
        lastReadingEvent = lastReadingEvent,
        personalNotes = null,
        owned = owned,
        book = null,
        toRead = toRead,
        percentRead = null,
        currentPageNumber = null,
        borrowed = borrowed,
        price = null,
    )

    fun user(): UserDto = (userService.loadUserByUsername("owner") as JeluUser).user

    fun otherUser(): UserDto = (userService.loadUserByUsername("someone-else") as JeluUser).user
}
