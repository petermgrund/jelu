package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.CreateReadingEventDto
import io.github.bayang.jelu.dto.UpdateReadingEventDto
import io.github.bayang.jelu.dto.UserDto
import io.github.bayang.jelu.errors.JeluException
import io.github.bayang.jelu.utils.nowDateTime
import io.github.bayang.jelu.utils.nowInstant
import io.github.bayang.jelu.utils.offset
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.javatime.year
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class ReadingEventRepository {
    fun findAll(
        eventTypes: List<ReadingEventType>?,
        userId: UUID?,
        bookId: UUID?,
        startedAfter: LocalDate?,
        startedBefore: LocalDate?,
        endedAfter: LocalDate?,
        endedBefore: LocalDate?,
        pageable: Pageable,
    ): Page<ReadingEvent> {
        val query =
            ReadingEventTable
                .join(UserBookTable, JoinType.LEFT)
                .selectAll()
        if (eventTypes != null && eventTypes.isNotEmpty()) {
            query.andWhere { ReadingEventTable.eventType inList eventTypes }
        }
        if (userId != null) {
            query.andWhere { UserBookTable.user eq userId }
        }
        if (bookId != null) {
            query.andWhere { UserBookTable.book eq bookId }
        }
        if (endedBefore != null) {
            val instant = OffsetDateTime.of(endedBefore, LocalTime.MAX, offset())
            query.andWhere { ReadingEventTable.endDate lessEq instant }
        }
        if (endedAfter != null) {
            val instant = OffsetDateTime.of(endedAfter, LocalTime.MIN, offset())
            query.andWhere { ReadingEventTable.endDate greaterEq instant }
        }
        if (startedBefore != null) {
            val instant = OffsetDateTime.of(startedBefore, LocalTime.MAX, offset())
            query.andWhere { ReadingEventTable.startDate lessEq instant }
        }
        if (startedAfter != null) {
            val instant = OffsetDateTime.of(startedAfter, LocalTime.MIN, offset())
            query.andWhere { ReadingEventTable.startDate greaterEq instant }
        }
        val total = query.count()
        query.limit(pageable.pageSize)
        query.offset(pageable.offset)
        val orders: Array<Pair<Expression<*>, SortOrder>> =
            parseSorts(pageable.sort, Pair(ReadingEventTable.modificationDate, SortOrder.DESC_NULLS_LAST), ReadingEventTable)
        query.orderBy(*orders)
        return PageImpl(
            ReadingEvent.wrapRows(query).toList(),
            pageable,
            total,
        )
    }

    fun findYears(
        eventTypes: List<ReadingEventType>?,
        userId: UUID?,
        bookId: UUID?,
    ): List<Int> {
        val query =
            ReadingEventTable
                .join(UserBookTable, JoinType.LEFT)
                .select(ReadingEventTable.endDate.year())
        if (eventTypes != null && eventTypes.isNotEmpty()) {
            query.andWhere { ReadingEventTable.eventType inList eventTypes }
        }
        if (userId != null) {
            query.andWhere { UserBookTable.user eq userId }
        }
        if (bookId != null) {
            query.andWhere { UserBookTable.book eq bookId }
        }
        query.withDistinct(true)
        // FIXME
        return query.map { resultRow -> resultRow[ReadingEventTable.endDate.year()] }.toList()
    }

    fun save(
        createReadingEventDto: CreateReadingEventDto,
        targetUser: UserDto,
    ): ReadingEvent {
        if (createReadingEventDto.bookId == null) {
            throw JeluException("Missing bookId to create reading event")
        }
        val foundBook: Book = Book[createReadingEventDto.bookId]
        return save(createReadingEventDto, foundBook, targetUser)
    }

    fun save(
        createReadingEventDto: CreateReadingEventDto,
        book: Book,
        targetUser: UserDto,
    ): ReadingEvent {
        var found: UserBook? =
            UserBook.find { UserBookTable.user eq targetUser.id and (UserBookTable.book.eq(book.id)) }.firstOrNull()
        val instant: Instant = nowInstant()
        if (found == null) {
            found =
                UserBook.new {
                    this.creationDate = instant
                    this.user = User[targetUser.id!!]
                    this.book = book
                }
        }
        found.modificationDate = instant
        return save(found, createReadingEventDto)
    }

    fun save(
        userBook: UserBook,
        createReadingEventDto: CreateReadingEventDto,
    ): ReadingEvent {
        val instant = nowDateTime()
        // every change of status is its own event with a single date: when the reading started,
        // finished or stopped. A start date sent without an event date is taken as that date.
        val explicitDate = createReadingEventDto.eventDate ?: createReadingEventDto.startDate
        val eventDate = explicitDate ?: instant
        // a change made now gives the book its status, a dated event only if it is the most recent one
        val lastDate = userBook.lastReadingEventDate
        val isLatestEvent = explicitDate == null || lastDate == null || explicitDate.isAfter(lastDate)
        if (isLatestEvent) {
            userBook.lastReadingEvent = createReadingEventDto.eventType
            userBook.lastReadingEventDate = eventDate
            // a book being read, finished or dropped is no longer a book the user wants to read,
            // a past reading added to the history does not change that
            if (userBook.toRead == true) {
                userBook.toRead = null
            }
        }
        synchronizeReadingProgress(createReadingEventDto, userBook)
        return ReadingEvent.new {
            this.creationDate = instant
            this.modificationDate = instant
            this.eventType = createReadingEventDto.eventType
            this.userBook = userBook
            this.startDate = eventDate
            this.endDate = singleEndDate(createReadingEventDto.eventType, eventDate)
        }
    }

    private fun synchronizeReadingProgress(
        createReadingEventDto: CreateReadingEventDto,
        userBook: UserBook,
    ) {
        if (createReadingEventDto.eventType == ReadingEventType.CURRENTLY_READING) {
            // we start a book, for the first time or it is a re-read
            // set the progress to zero
            userBook.percentRead = 0
            userBook.currentPageNumber = 0
        } else if (createReadingEventDto.eventType == ReadingEventType.FINISHED) {
            // and if we mark a book as read set progress to 100 and current page to last page
            userBook.percentRead = 100
            if (userBook.book.pageCount != null) {
                userBook.currentPageNumber = userBook.book.pageCount
            }
        }
    }

    // a finished or stopped event keeps its one date as both start and end, so the history and the
    // stats, which filter on either column, keep working; a current reading has no end yet
    private fun singleEndDate(
        eventType: ReadingEventType,
        date: OffsetDateTime,
    ): OffsetDateTime? =
        if (eventType == ReadingEventType.CURRENTLY_READING) {
            null
        } else {
            date
        }

    fun updateReadingEvent(
        readingEventId: UUID,
        updateReadingEventDto: UpdateReadingEventDto,
    ): ReadingEvent {
        val entity = ReadingEvent[readingEventId]
        return entity.apply {
            val instant = nowDateTime()
            this.modificationDate = instant
            // an event keeps a single date, moved by the event date (or a start date sent alone)
            val date = updateReadingEventDto.eventDate ?: updateReadingEventDto.startDate ?: this.lastEventDate
            val typeChanged = this.eventType != updateReadingEventDto.eventType
            this.eventType = updateReadingEventDto.eventType
            this.startDate = date
            this.endDate = singleEndDate(updateReadingEventDto.eventType, date)
            val lastEvent =
                this.userBook.readingEvents
                    .maxByOrNull { e -> e.lastEventDate }
            // changing the state of the latest event ends the wish to read the book,
            // editing the date of a past event does not
            if (typeChanged && lastEvent?.id == this.id && this.userBook.toRead == true) {
                this.userBook.toRead = null
            }
            if (lastEvent == null) {
                this.userBook.lastReadingEvent = null
                this.userBook.lastReadingEventDate = null
            } else {
                this.userBook.lastReadingEventDate = lastEvent.lastEventDate
                this.userBook.lastReadingEvent = lastEvent.eventType
            }
        }
    }

    fun deleteReadingEventById(eventId: UUID) {
        val entity: ReadingEvent = ReadingEvent[eventId]
        val userbook = entity.userBook
        if (entity.eventType == ReadingEventType.CURRENTLY_READING) {
            userbook.percentRead = 0
            userbook.currentPageNumber = null
        }
        entity.delete()
        // the most recent remaining event, by its date, gives the book its status back
        val lastEvent =
            userbook.readingEvents
                .maxByOrNull { e -> e.lastEventDate }
        if (lastEvent == null) {
            userbook.lastReadingEvent = null
            userbook.lastReadingEventDate = null
        } else {
            userbook.lastReadingEventDate = lastEvent.lastEventDate
            userbook.lastReadingEvent = lastEvent.eventType
        }
    }
}
