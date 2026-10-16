package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.CreateOwnershipEventDto
import io.github.bayang.jelu.dto.UpdateOwnershipEventDto
import io.github.bayang.jelu.errors.JeluNotFoundException
import io.github.bayang.jelu.utils.nowDateTime
import io.github.bayang.jelu.utils.nowInstant
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class OwnershipEventRepository {
    fun findUserBookOfUser(
        userBookId: UUID,
        userId: UUID,
    ): UserBook {
        val userBook = UserBook.findById(userBookId)
        if (userBook == null || userBook.user.id.value != userId) {
            throw JeluNotFoundException("Book $userBookId not found")
        }
        return userBook
    }

    fun findEventOfUserBook(
        eventId: UUID,
        userBook: UserBook,
    ): OwnershipEvent {
        val event = OwnershipEvent.findById(eventId)
        if (event == null || event.userBook.id != userBook.id) {
            throw JeluNotFoundException("Ownership event $eventId not found")
        }
        return event
    }

    fun findByUserBook(userBook: UserBook): List<OwnershipEvent> = userBook.ownershipEvents.sortedWith(OwnershipEvent.TIMELINE_ORDER)

    fun save(
        userBook: UserBook,
        dto: CreateOwnershipEventDto,
    ): OwnershipEvent {
        val created = create(userBook, dto.status, dto.eventDate, dto.note)
        syncFlags(userBook, created)
        return created
    }

    fun update(
        event: OwnershipEvent,
        dto: UpdateOwnershipEventDto,
    ): OwnershipEvent {
        event.status = dto.status
        event.eventDate = dto.eventDate
        event.note = cleanNote(dto.note)
        event.modificationDate = nowDateTime()
        syncFlags(event.userBook)
        return event
    }

    fun delete(event: OwnershipEvent) {
        val userBook = event.userBook
        event.delete()
        syncFlags(userBook)
    }

    /**
     * Records a change made through the owned and borrowed flags (book forms, bulk edits,
     * imports, API clients). The date is unknown, so the entry sits at the moment it is
     * recorded, and the flags are left as the caller set them.
     */
    fun recordFlagsChange(
        userBook: UserBook,
        previous: OwnershipStatus,
    ) {
        val current = OwnershipStatus.fromFlags(userBook.owned, userBook.borrowed)
        if (current != previous) {
            create(userBook, current, null, null)
        }
    }

    private fun create(
        userBook: UserBook,
        status: OwnershipStatus,
        eventDate: OffsetDateTime?,
        note: String?,
    ): OwnershipEvent {
        val now = nowDateTime()
        return OwnershipEvent.new {
            this.creationDate = now
            this.modificationDate = now
            this.userBook = userBook
            this.status = status
            this.eventDate = eventDate
            this.note = cleanNote(note)
        }
    }

    /**
     * The latest entry of the history is the current state. Without any entry left
     * the book is not owned nor borrowed.
     */
    private fun syncFlags(
        userBook: UserBook,
        pending: OwnershipEvent? = null,
    ) {
        val events = userBook.ownershipEvents.toMutableList()
        if (pending != null && events.none { it.id == pending.id }) {
            events.add(pending)
        }
        val status = events.maxWithOrNull(OwnershipEvent.TIMELINE_ORDER)?.status ?: OwnershipStatus.NOT_OWNED
        userBook.owned = flag(userBook.owned, status == OwnershipStatus.OWNED)
        userBook.borrowed = flag(userBook.borrowed, status == OwnershipStatus.BORROWED)
        userBook.modificationDate = nowInstant()
    }

    // keep unset flags unset rather than turning them into false
    private fun flag(
        current: Boolean?,
        value: Boolean,
    ): Boolean? = if (value) true else current?.let { false }

    private fun cleanNote(note: String?): String? = note?.trim()?.takeIf { it.isNotEmpty() }
}
