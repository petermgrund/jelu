package io.github.bayang.jelu.service

import io.github.bayang.jelu.bookDto
import io.github.bayang.jelu.config.JeluProperties
import io.github.bayang.jelu.createUserBookDto
import io.github.bayang.jelu.dao.ReadingEventType
import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.CreateUserDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.LibraryFilter
import io.github.bayang.jelu.dto.UpdateReadingEventDto
import io.github.bayang.jelu.dto.UserBookLightDto
import io.github.bayang.jelu.dto.UserBookUpdateDto
import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.errors.JeluException
import io.github.bayang.jelu.utils.nowDateTime
import io.github.bayang.jelu.utils.nowInstant
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
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReadingEventServiceTest(
    @Autowired private val bookService: BookService,
    @Autowired private val userService: UserService,
    @Autowired private val jeluProperties: JeluProperties,
    @Autowired private val readingEventService: ReadingEventService,
) {
    companion object {
        @TempDir
        lateinit var tempDir: File
    }

    @BeforeAll
    fun setupUser() {
        userService.save(CreateUserDto(login = "testuser", password = "1234", admin = true))
        jeluProperties.files.images = tempDir.absolutePath
    }

    @AfterAll
    fun teardDown() {
        userService.findAll(null).forEach { userService.deleteUser(it.id!!) }
    }

    @AfterEach
    fun cleanTest() {
        tempDir.listFiles().forEach {
            it.deleteRecursively()
        }
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
    fun testUpdateUserbookWithNewStatusAddsItsOwnEvent() {
        val createBook = bookDto()
        val createUserBookDto = createUserBookDto(createBook, ReadingEventType.CURRENTLY_READING, nowDateTime())
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        Assertions.assertEquals(createBook.title, saved.book.title)
        Assertions.assertEquals(createBook.isbn10, saved.book.isbn10)
        Assertions.assertEquals(createBook.isbn13?.trim(), saved.book.isbn13)
        Assertions.assertEquals("This is a test summary with a newline", saved.book.summary)
        Assertions.assertEquals(createBook.publisher, saved.book.publisher)
        Assertions.assertEquals(createBook.pageCount, saved.book.pageCount)
        Assertions.assertEquals(createBook.goodreadsId, saved.book.goodreadsId)
        Assertions.assertNull(saved.book.librarythingId)
        Assertions.assertEquals(createUserBookDto.owned, saved.owned)
        Assertions.assertEquals(createUserBookDto.toRead, saved.toRead)
        Assertions.assertEquals(createUserBookDto.personalNotes, saved.personalNotes)
        Assertions.assertEquals(0, saved.percentRead)
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.book.creationDate)
        Assertions.assertNotNull(saved.book.modificationDate)
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, saved.lastReadingEvent)
        Assertions.assertNotNull(saved.lastReadingEventDate)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)

        val updater =
            UserBookUpdateDto(
                ReadingEventType.DROPPED,
                personalNotes = "new notes",
                owned = false,
                book = null,
                toRead = null,
                percentRead = 50,
                borrowed = null,
                currentPageNumber = null,
                price = null,
            )
        val updated = bookService.update(saved.id!!, updater, null)
        Assertions.assertEquals(createBook.title, updated.book.title)
        Assertions.assertEquals(createBook.isbn10, updated.book.isbn10)
        Assertions.assertEquals(createBook.isbn13?.trim(), updated.book.isbn13)
        Assertions.assertEquals("This is a test summary with a newline", updated.book.summary)
        Assertions.assertEquals(createBook.publisher, updated.book.publisher)
        Assertions.assertEquals(createBook.pageCount, updated.book.pageCount)
        Assertions.assertEquals(createBook.goodreadsId, updated.book.goodreadsId)
        Assertions.assertNull(updated.book.librarythingId)
        Assertions.assertEquals(updater.owned, updated.owned)
        Assertions.assertEquals(saved.toRead, updated.toRead)
        Assertions.assertEquals(updater.percentRead, updated.percentRead)
        Assertions.assertEquals(updater.personalNotes, updated.personalNotes)
        Assertions.assertNotNull(updated.creationDate)
        Assertions.assertNotNull(updated.modificationDate)
        Assertions.assertNotNull(updated.book.creationDate)
        Assertions.assertNotNull(updated.book.modificationDate)
        Assertions.assertEquals(ReadingEventType.DROPPED, updated.lastReadingEvent)
        Assertions.assertNotNull(updated.lastReadingEventDate)
        // stopping adds its own event, the reading event keeps its single date and gets no end
        Assertions.assertEquals(2, updated.readingEvents?.size)
        Assertions.assertEquals(2, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        val reading = updated.readingEvents!!.single { it.eventType == ReadingEventType.CURRENTLY_READING }
        Assertions.assertNull(reading.endDate)
        val stopped = updated.readingEvents!!.single { it.eventType == ReadingEventType.DROPPED }
        Assertions.assertEquals(stopped.endDate, stopped.startDate)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
    }

    @Test
    fun testUpdateUserbookWithImageAndEventNewEventRequired() {
        val createBook = bookDto()
        val createUserBookDto = createUserBookDto(createBook, ReadingEventType.FINISHED, nowDateTime())
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        Assertions.assertEquals(createBook.title, saved.book.title)
        Assertions.assertEquals(createBook.isbn10, saved.book.isbn10)
        Assertions.assertEquals(createBook.isbn13?.trim(), saved.book.isbn13)
        Assertions.assertEquals("This is a test summary with a newline", saved.book.summary)
        Assertions.assertEquals(createBook.publisher, saved.book.publisher)
        Assertions.assertEquals(createBook.pageCount, saved.book.pageCount)
        Assertions.assertEquals(createBook.goodreadsId, saved.book.goodreadsId)
        Assertions.assertNull(saved.book.librarythingId)
        Assertions.assertEquals(createUserBookDto.owned, saved.owned)
        Assertions.assertEquals(createUserBookDto.toRead, saved.toRead)
        Assertions.assertEquals(createUserBookDto.personalNotes, saved.personalNotes)
        Assertions.assertEquals(100, saved.percentRead)
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.book.creationDate)
        Assertions.assertNotNull(saved.book.modificationDate)
        Assertions.assertEquals(ReadingEventType.FINISHED, saved.lastReadingEvent)
        Assertions.assertNotNull(saved.lastReadingEventDate)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)

        val updater =
            UserBookUpdateDto(
                ReadingEventType.DROPPED,
                personalNotes = "new notes",
                owned = false,
                book = null,
                toRead = null,
                percentRead = 50,
                borrowed = null,
                currentPageNumber = null,
                price = null,
            )
        val updated = bookService.update(saved.id!!, updater, null)
        Assertions.assertEquals(createBook.title, updated.book.title)
        Assertions.assertEquals(createBook.isbn10, updated.book.isbn10)
        Assertions.assertEquals(createBook.isbn13?.trim(), updated.book.isbn13)
        Assertions.assertEquals("This is a test summary with a newline", updated.book.summary)
        Assertions.assertEquals(createBook.publisher, updated.book.publisher)
        Assertions.assertEquals(createBook.pageCount, updated.book.pageCount)
        Assertions.assertEquals(createBook.goodreadsId, updated.book.goodreadsId)
        Assertions.assertNull(updated.book.librarythingId)
        Assertions.assertEquals(updater.owned, updated.owned)
        Assertions.assertEquals(saved.toRead, updated.toRead)
        Assertions.assertEquals(updater.percentRead, updated.percentRead)
        Assertions.assertEquals(updated.borrowed, saved.borrowed)
        Assertions.assertEquals(updater.personalNotes, updated.personalNotes)
        Assertions.assertNotNull(updated.creationDate)
        Assertions.assertNotNull(updated.modificationDate)
        Assertions.assertNotNull(updated.book.creationDate)
        Assertions.assertNotNull(updated.book.modificationDate)
        Assertions.assertEquals(ReadingEventType.DROPPED, updated.lastReadingEvent)
        Assertions.assertNotNull(updated.lastReadingEventDate)
        Assertions.assertEquals(2, updated.readingEvents?.size)
        Assertions.assertEquals(2, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)
    }

    @Test
    fun testUpdateUserbookWithImageAndEventNewEventRequiredWithDateBefore() {
        val createBook = bookDto()
        val createUserBookDto = createUserBookDto(createBook, ReadingEventType.FINISHED, nowDateTime())
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        Assertions.assertEquals(createBook.title, saved.book.title)
        Assertions.assertEquals(createBook.isbn10, saved.book.isbn10)
        Assertions.assertEquals(createBook.isbn13?.trim(), saved.book.isbn13)
        Assertions.assertEquals("This is a test summary with a newline", saved.book.summary)
        Assertions.assertEquals(createBook.publisher, saved.book.publisher)
        Assertions.assertEquals(createBook.pageCount, saved.book.pageCount)
        Assertions.assertEquals(createBook.goodreadsId, saved.book.goodreadsId)
        Assertions.assertNull(saved.book.librarythingId)
        Assertions.assertEquals(createUserBookDto.owned, saved.owned)
        Assertions.assertEquals(createUserBookDto.toRead, saved.toRead)
        Assertions.assertEquals(createUserBookDto.personalNotes, saved.personalNotes)
        Assertions.assertEquals(100, saved.percentRead)
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.book.creationDate)
        Assertions.assertNotNull(saved.book.modificationDate)
        Assertions.assertEquals(ReadingEventType.FINISHED, saved.lastReadingEvent)
        Assertions.assertNotNull(saved.lastReadingEventDate)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)

        val thirtyDaysBefore = nowDateTime().minus(30, ChronoUnit.DAYS)
        val newEvent =
            readingEventService.save(
                CreateReadingEventDto(
                    ReadingEventType.CURRENTLY_READING,
                    saved.book.id,
                    thirtyDaysBefore,
                    null,
                ),
                user(),
            )
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, newEvent.eventType)

        var userbook = bookService.findUserBookById(saved.id!!)
        Assertions.assertEquals(ReadingEventType.FINISHED, userbook.lastReadingEvent)
        // TODO investigate exposed date change
        Assertions.assertTrue(userbook.lastReadingEventDate?.isAfter(nowDateTime().minus(242, ChronoUnit.MINUTES))!!)

        Assertions.assertEquals(2, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)

        readingEventService.deleteReadingEventById(newEvent.id!!)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        userbook = bookService.findUserBookById(saved.id)
        Assertions.assertEquals(ReadingEventType.FINISHED, userbook.lastReadingEvent)
        Assertions.assertTrue(userbook.lastReadingEventDate?.isAfter(nowDateTime().minus(242, ChronoUnit.MINUTES))!!)
    }

    @Test
    fun testNewEventKeepsOnlyItsEventDate() {
        val createBook = bookDto()
        val createUserBookDto = createUserBookDto(createBook, ReadingEventType.FINISHED, nowDateTime())
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        Assertions.assertEquals(createBook.title, saved.book.title)
        Assertions.assertEquals(createBook.isbn10, saved.book.isbn10)
        Assertions.assertEquals(createBook.isbn13?.trim(), saved.book.isbn13)
        Assertions.assertEquals("This is a test summary with a newline", saved.book.summary)
        Assertions.assertEquals(createBook.publisher, saved.book.publisher)
        Assertions.assertEquals(createBook.pageCount, saved.book.pageCount)
        Assertions.assertEquals(createBook.goodreadsId, saved.book.goodreadsId)
        Assertions.assertNull(saved.book.librarythingId)
        Assertions.assertEquals(createUserBookDto.owned, saved.owned)
        Assertions.assertEquals(createUserBookDto.toRead, saved.toRead)
        Assertions.assertEquals(createUserBookDto.personalNotes, saved.personalNotes)
        Assertions.assertEquals(100, saved.percentRead)
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.book.creationDate)
        Assertions.assertNotNull(saved.book.modificationDate)
        Assertions.assertEquals(ReadingEventType.FINISHED, saved.lastReadingEvent)
        Assertions.assertNotNull(saved.lastReadingEventDate)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)

        // an event has a single date, the event date wins over a start date sent with it
        val thirtyDaysBefore = nowDateTime().minus(30, ChronoUnit.DAYS)
        val newEvent =
            readingEventService.save(
                CreateReadingEventDto(
                    ReadingEventType.FINISHED,
                    saved.book.id,
                    thirtyDaysBefore,
                    thirtyDaysBefore.plus(1, ChronoUnit.DAYS),
                ),
                user(),
            )
        Assertions.assertTrue(newEvent.endDate!!.isEqual(thirtyDaysBefore))
        Assertions.assertTrue(newEvent.startDate!!.isEqual(thirtyDaysBefore))
    }

    @Test
    fun testUpdateUserbookWithImageAndEventNewEventRequiredWithDateAfter() {
        val createBook = bookDto()
        val createUserBookDto = createUserBookDto(createBook, ReadingEventType.FINISHED, nowDateTime())
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        Assertions.assertEquals(createBook.title, saved.book.title)
        Assertions.assertEquals(createBook.isbn10, saved.book.isbn10)
        Assertions.assertEquals(createBook.isbn13?.trim(), saved.book.isbn13)
        Assertions.assertEquals("This is a test summary with a newline", saved.book.summary)
        Assertions.assertEquals(createBook.publisher, saved.book.publisher)
        Assertions.assertEquals(createBook.pageCount, saved.book.pageCount)
        Assertions.assertEquals(createBook.goodreadsId, saved.book.goodreadsId)
        Assertions.assertNull(saved.book.librarythingId)
        Assertions.assertEquals(createUserBookDto.owned, saved.owned)
        Assertions.assertEquals(createUserBookDto.toRead, saved.toRead)
        Assertions.assertEquals(createUserBookDto.personalNotes, saved.personalNotes)
        Assertions.assertEquals(100, saved.percentRead)
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.book.creationDate)
        Assertions.assertNotNull(saved.book.modificationDate)
        Assertions.assertEquals(ReadingEventType.FINISHED, saved.lastReadingEvent)
        Assertions.assertNotNull(saved.lastReadingEventDate)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)

        val thirtyDaysAfter = nowDateTime().plus(30, ChronoUnit.DAYS)
        val newEvent =
            readingEventService.save(
                CreateReadingEventDto(
                    ReadingEventType.CURRENTLY_READING,
                    saved.book.id,
                    thirtyDaysAfter,
                    null,
                ),
                user(),
            )
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, newEvent.eventType)

        var userbook = bookService.findUserBookById(saved.id!!)
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, userbook.lastReadingEvent)
        Assertions.assertTrue(userbook.lastReadingEventDate?.isAfter(nowDateTime().plus(29, ChronoUnit.DAYS))!!)

        Assertions.assertEquals(2, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)

        readingEventService.deleteReadingEventById(newEvent.id!!)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        userbook = bookService.findUserBookById(saved.id)
        Assertions.assertEquals(ReadingEventType.FINISHED, userbook.lastReadingEvent)
        Assertions.assertTrue(userbook.lastReadingEventDate?.isAfter(nowDateTime().minus(682, ChronoUnit.MINUTES))!!)
    }

    @Test
    fun testFinishingAReadingAddsAReadEventAndDeletingEventsRestoresTheStatus() {
        val createBook = bookDto()
        val createUserBookDto = createUserBookDto(createBook, ReadingEventType.CURRENTLY_READING, nowDateTime(), true)
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        Assertions.assertEquals(createBook.title, saved.book.title)
        Assertions.assertEquals(createBook.isbn10, saved.book.isbn10)
        Assertions.assertEquals(createBook.isbn13?.trim(), saved.book.isbn13)
        Assertions.assertEquals("This is a test summary with a newline", saved.book.summary)
        Assertions.assertEquals(createBook.publisher, saved.book.publisher)
        Assertions.assertEquals(createBook.pageCount, saved.book.pageCount)
        Assertions.assertEquals(createBook.goodreadsId, saved.book.goodreadsId)
        Assertions.assertNull(saved.book.librarythingId)
        Assertions.assertEquals(createUserBookDto.owned, saved.owned)
        Assertions.assertNull(saved.toRead) // a book being read or already read is not one to read
        Assertions.assertEquals(createUserBookDto.personalNotes, saved.personalNotes)
        Assertions.assertEquals(0, saved.percentRead)
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.book.creationDate)
        Assertions.assertNotNull(saved.book.modificationDate)
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, saved.lastReadingEvent)
        Assertions.assertNotNull(saved.lastReadingEventDate)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)

        val thirtyDaysAfter = nowDateTime().plus(30, ChronoUnit.DAYS)
        val newEvent =
            readingEventService.save(
                CreateReadingEventDto(
                    ReadingEventType.FINISHED,
                    saved.book.id,
                    thirtyDaysAfter,
                    null,
                ),
                user(),
            )
        Assertions.assertEquals(ReadingEventType.FINISHED, newEvent.eventType)

        var userbook = bookService.findUserBookById(saved.id!!)
        Assertions.assertEquals(ReadingEventType.FINISHED, userbook.lastReadingEvent)
        Assertions.assertTrue(userbook.lastReadingEventDate?.isAfter(nowDateTime().plus(29, ChronoUnit.DAYS))!!)
        Assertions.assertNull(userbook.toRead)

        // finishing adds a read event next to the reading one instead of closing it
        Assertions.assertEquals(2, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)

        // deleting the read event takes the book back to reading, deleting that one too leaves no status
        readingEventService.deleteReadingEventById(newEvent.id!!)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        userbook = bookService.findUserBookById(saved.id)
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, userbook.lastReadingEvent)
        val readingEvent = readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).content.single()
        readingEventService.deleteReadingEventById(readingEvent.id!!)
        Assertions.assertEquals(0, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        userbook = bookService.findUserBookById(saved.id)
        Assertions.assertNull(userbook.lastReadingEvent)
        Assertions.assertNull(userbook.lastReadingEventDate)
    }

    @Test
    fun newEventWithoutExistingUserbookShouldCreateUserbook() {
        val user = user()
        val createBook = bookDto()
        val savedBook = bookService.save(createBook, null)
        val nbBooks =
            bookService
                .findAll(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Pageable.ofSize(30),
                    user(),
                    LibraryFilter.ANY,
                ).totalElements
        Assertions.assertEquals(1, nbBooks)

        var nbUserBooks = bookService.findUserBookByCriteria(user.id!!, null, null, null, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(0, nbUserBooks)

        Assertions.assertEquals(0, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)

        val dateAfter = nowDateTime().plus(1, ChronoUnit.DAYS)
        val newEvent =
            readingEventService.save(
                CreateReadingEventDto(
                    ReadingEventType.CURRENTLY_READING,
                    savedBook.id,
                    dateAfter,
                    null,
                ),
                user(),
            )
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, newEvent.eventType)
        nbUserBooks = bookService.findUserBookByCriteria(user.id, null, null, null, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(1, nbUserBooks)

        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
    }

    @Test
    fun newEventWithoutBookIdShouldThrowException() {
        val user = user()
        val nbBooks =
            bookService
                .findAll(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Pageable.ofSize(30),
                    user(),
                    LibraryFilter.ANY,
                ).totalElements
        Assertions.assertEquals(0, nbBooks)

        val nbUserBooks = bookService.findUserBookByCriteria(user.id!!, null, null, null, null, null, Pageable.ofSize(30)).totalElements
        Assertions.assertEquals(0, nbUserBooks)

        val dateAfter = nowDateTime().plus(1, ChronoUnit.DAYS)
        assertThrows<JeluException>("bookId is required") {
            readingEventService.save(
                CreateReadingEventDto(
                    ReadingEventType.CURRENTLY_READING,
                    null,
                    dateAfter,
                    null,
                ),
                user(),
            )
        }
        Assertions.assertEquals(0, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
    }

    @Test
    fun testUpdateEvent() {
        val createBook = bookDto()
        val createUserBookDto = createUserBookDto(createBook, ReadingEventType.FINISHED, nowDateTime(), true)
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        Assertions.assertEquals(createBook.title, saved.book.title)
        Assertions.assertEquals(createBook.isbn10, saved.book.isbn10)
        Assertions.assertEquals(createBook.isbn13?.trim(), saved.book.isbn13)
        Assertions.assertEquals("This is a test summary with a newline", saved.book.summary)
        Assertions.assertEquals(createBook.publisher, saved.book.publisher)
        Assertions.assertEquals(createBook.pageCount, saved.book.pageCount)
        Assertions.assertEquals(createBook.goodreadsId, saved.book.goodreadsId)
        Assertions.assertNull(saved.book.librarythingId)
        Assertions.assertEquals(createUserBookDto.owned, saved.owned)
        Assertions.assertNull(saved.toRead) // a book being read or already read is not one to read
        Assertions.assertEquals(createUserBookDto.personalNotes, saved.personalNotes)
        Assertions.assertEquals(100, saved.percentRead)
        Assertions.assertEquals(50, saved.currentPageNumber)
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.book.creationDate)
        Assertions.assertNotNull(saved.book.modificationDate)
        Assertions.assertEquals(ReadingEventType.FINISHED, saved.lastReadingEvent)
        Assertions.assertNotNull(saved.lastReadingEventDate)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)

        val thirtyDaysAfter = nowDateTime().plus(30, ChronoUnit.DAYS)
        val newEvent =
            readingEventService.save(
                CreateReadingEventDto(
                    ReadingEventType.CURRENTLY_READING,
                    saved.book.id,
                    thirtyDaysAfter,
                    null,
                ),
                user(),
            )
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, newEvent.eventType)

        var userbook = bookService.findUserBookById(saved.id!!)
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, userbook.lastReadingEvent)
        Assertions.assertTrue(userbook.lastReadingEventDate?.isAfter(nowDateTime().plus(29, ChronoUnit.DAYS))!!)
        Assertions.assertEquals(0, userbook.percentRead)
        Assertions.assertEquals(0, userbook.currentPageNumber)

        Assertions.assertEquals(2, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)

        val updated =
            readingEventService.updateReadingEvent(
                newEvent.id!!,
                UpdateReadingEventDto(
                    ReadingEventType.FINISHED,
                    nowDateTime().plus(40, ChronoUnit.DAYS),
                    null,
                ),
            )
        Assertions.assertEquals(ReadingEventType.FINISHED, updated.eventType)
        Assertions.assertFalse(updated.creationDate?.isAfter(nowDateTime().plus(2, ChronoUnit.HOURS))!!)
        Assertions.assertTrue(updated.endDate?.isAfter(nowDateTime().plus(39, ChronoUnit.DAYS))!!)
        Assertions.assertEquals(updated.endDate, updated.startDate)
        userbook = bookService.findUserBookById(saved.id)
        Assertions.assertEquals(ReadingEventType.FINISHED, userbook.lastReadingEvent)
        Assertions.assertNull(userbook.toRead)
        Assertions.assertTrue(userbook.lastReadingEventDate?.isAfter(nowDateTime().plus(39, ChronoUnit.DAYS))!!)
        Assertions.assertEquals(100, saved.percentRead)
        Assertions.assertEquals(50, saved.currentPageNumber)
    }

    @Test
    fun testEachStatusChangeAddsItsOwnDatedEvent() {
        val saved: UserBookLightDto = bookService.save(createUserBookDto(bookDto(), toRead = true), user(), null)
        Assertions.assertEquals(0, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)

        // the status menu sends no date, each change is stamped with the moment it happened
        val reading =
            readingEventService.save(
                CreateReadingEventDto(ReadingEventType.CURRENTLY_READING, saved.book.id, null, null),
                user(),
            )
        Assertions.assertNull(reading.endDate)
        val read =
            readingEventService.save(
                CreateReadingEventDto(ReadingEventType.FINISHED, saved.book.id, null, null),
                user(),
            )
        Assertions.assertEquals(read.endDate, read.startDate)
        Assertions.assertFalse(read.endDate!!.isBefore(reading.startDate))

        val events = readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).content
        Assertions.assertEquals(2, events.size)
        Assertions.assertNull(events.single { it.eventType == ReadingEventType.CURRENTLY_READING }.endDate)
        val userbook = bookService.findUserBookById(saved.id!!)
        Assertions.assertEquals(ReadingEventType.FINISHED, userbook.lastReadingEvent)
        Assertions.assertNull(userbook.toRead)
        Assertions.assertEquals(100, userbook.percentRead)
    }

    @Test
    fun testUpdateEventToReadingKeepsOnlyAStartDate() {
        val saved: UserBookLightDto =
            bookService.save(createUserBookDto(bookDto(), ReadingEventType.FINISHED, nowDateTime()), user(), null)
        val event = readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).content.single()
        val tenDaysBefore = nowDateTime().minus(10, ChronoUnit.DAYS)
        val updated =
            readingEventService.updateReadingEvent(
                event.id!!,
                UpdateReadingEventDto(ReadingEventType.CURRENTLY_READING, tenDaysBefore, null),
            )
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, updated.eventType)
        Assertions.assertEquals(tenDaysBefore.toEpochSecond(), updated.startDate!!.toEpochSecond())
        Assertions.assertNull(updated.endDate)
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, bookService.findUserBookById(saved.id!!).lastReadingEvent)
    }

    @Test
    fun testUpdateEventWithoutDateKeepsItsDate() {
        val fiveDaysBefore = nowDateTime().minus(5, ChronoUnit.DAYS)
        bookService.save(createUserBookDto(bookDto(), ReadingEventType.FINISHED, fiveDaysBefore), user(), null)
        val event = readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).content.single()
        val updated =
            readingEventService.updateReadingEvent(
                event.id!!,
                UpdateReadingEventDto(ReadingEventType.DROPPED, null, null),
            )
        Assertions.assertEquals(ReadingEventType.DROPPED, updated.eventType)
        Assertions.assertTrue(updated.endDate!!.isEqual(event.endDate))
        Assertions.assertTrue(updated.startDate!!.isEqual(event.endDate))
    }

    @Test
    fun testFindByDate() {
        val createBook = bookDto()
        val createUserBookDto = createUserBookDto(createBook, ReadingEventType.FINISHED, nowDateTime(), true)
        val saved: UserBookLightDto = bookService.save(createUserBookDto, user(), null)
        Assertions.assertEquals(createBook.title, saved.book.title)
        Assertions.assertEquals(createBook.isbn10, saved.book.isbn10)
        Assertions.assertEquals(createBook.isbn13?.trim(), saved.book.isbn13)
        Assertions.assertEquals("This is a test summary with a newline", saved.book.summary)
        Assertions.assertEquals(createBook.publisher, saved.book.publisher)
        Assertions.assertEquals(createBook.pageCount, saved.book.pageCount)
        Assertions.assertEquals(createBook.goodreadsId, saved.book.goodreadsId)
        Assertions.assertNull(saved.book.librarythingId)
        Assertions.assertEquals(createUserBookDto.owned, saved.owned)
        Assertions.assertNull(saved.toRead) // a book being read or already read is not one to read
        Assertions.assertEquals(createUserBookDto.personalNotes, saved.personalNotes)
        Assertions.assertEquals(100, saved.percentRead)
        Assertions.assertNotNull(saved.creationDate)
        Assertions.assertNotNull(saved.modificationDate)
        Assertions.assertNotNull(saved.book.creationDate)
        Assertions.assertNotNull(saved.book.modificationDate)
        Assertions.assertEquals(ReadingEventType.FINISHED, saved.lastReadingEvent)
        Assertions.assertNotNull(saved.lastReadingEventDate)
        Assertions.assertEquals(1, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)

        val thirtyDaysAfter = nowDateTime().plus(30, ChronoUnit.DAYS)
        val newEvent =
            readingEventService.save(
                CreateReadingEventDto(
                    ReadingEventType.CURRENTLY_READING,
                    saved.book.id,
                    thirtyDaysAfter,
                    null,
                ),
                user(),
            )
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, newEvent.eventType)

        var userbook = bookService.findUserBookById(saved.id!!)
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, userbook.lastReadingEvent)
        Assertions.assertTrue(userbook.lastReadingEventDate?.isAfter(nowDateTime().plus(29, ChronoUnit.DAYS))!!)

        Assertions.assertEquals(2, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)
        Assertions.assertEquals(0, File(jeluProperties.files.images).listFiles().size)

        val threeYearsBefore = OffsetDateTime.now(ZoneId.systemDefault()).minusYears(3)
        val newEventBefore =
            readingEventService.save(
                CreateReadingEventDto(
                    ReadingEventType.CURRENTLY_READING,
                    saved.book.id,
                    threeYearsBefore,
                    null,
                ),
                user(),
            )
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, newEventBefore.eventType)

        userbook = bookService.findUserBookById(saved.id)
        Assertions.assertEquals(ReadingEventType.CURRENTLY_READING, userbook.lastReadingEvent)
        Assertions.assertEquals(3, readingEventService.findAll(null, null, null, null, null, null, null, Pageable.ofSize(30)).totalElements)

        val farPast = OffsetDateTime.now(ZoneId.systemDefault()).minusYears(4).toLocalDate()
        val beforeAll = readingEventService.findAll(null, null, null, farPast, null, null, null, Pageable.ofSize(30))
        Assertions.assertEquals(3, beforeAll.totalElements)

        val nothingBefore = readingEventService.findAll(null, null, null, null, farPast, null, null, Pageable.ofSize(30))
        Assertions.assertEquals(0, nothingBefore.totalElements)

        val twoYearsBefore = OffsetDateTime.now(ZoneId.systemDefault()).minusYears(2).toLocalDate()
        val before2Years = readingEventService.findAll(null, null, null, twoYearsBefore, null, null, null, Pageable.ofSize(30))
        Assertions.assertEquals(2, before2Years.totalElements)

        val after2Years = readingEventService.findAll(null, null, null, null, twoYearsBefore, null, null, Pageable.ofSize(30))
        Assertions.assertEquals(1, after2Years.totalElements)

        val farFuture = OffsetDateTime.now(ZoneId.systemDefault()).plusYears(4).toLocalDate()
        val afterAll = readingEventService.findAll(null, null, null, farFuture, null, null, null, Pageable.ofSize(30))
        Assertions.assertEquals(0, afterAll.totalElements)

        val thirtyOneDaysAfter = LocalDate.ofInstant(nowInstant().plus(31, ChronoUnit.DAYS), ZoneId.systemDefault())
        val twentyNineDaysAfter = LocalDate.ofInstant(nowInstant().plus(29, ChronoUnit.DAYS), ZoneId.systemDefault())
        val between =
            readingEventService.findAll(
                null,
                null,
                null,
                twentyNineDaysAfter,
                thirtyOneDaysAfter,
                null,
                null,
                Pageable.ofSize(30),
            )
        Assertions.assertEquals(1, between.totalElements)

        val thirtyDaysAfterMorning = LocalDate.ofInstant(nowInstant().plus(30, ChronoUnit.DAYS), ZoneId.systemDefault())
        val betweenSameDayAtMorning =
            readingEventService.findAll(
                null,
                null,
                null,
                thirtyDaysAfterMorning,
                thirtyOneDaysAfter,
                null,
                null,
                Pageable.ofSize(30),
            )
        Assertions.assertEquals(1, betweenSameDayAtMorning.totalElements)

        val betweenFarDates = readingEventService.findAll(null, null, null, farPast, farFuture, null, null, Pageable.ofSize(30))
        Assertions.assertEquals(3, betweenFarDates.totalElements)
    }

    fun user(): UserDto {
        val userDetail = userService.loadUserByUsername("testuser")
        return (userDetail as JeluUser).user
    }
}
