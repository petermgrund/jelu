package io.github.bayang.jelu.dao

import io.github.bayang.jelu.dto.OwnershipEventDto
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import java.time.OffsetDateTime
import java.util.UUID

object OwnershipEventTable : UUIDTable("ownership_event") {
    val creationDate = timestampWithTimeZone("creation_date")
    val modificationDate = timestampWithTimeZone("modification_date")
    val userBook = reference("user_book", UserBookTable, onDelete = ReferenceOption.CASCADE)
    val status = enumerationByName("status", 50, OwnershipStatus::class)
    val eventDate: Column<OffsetDateTime?> = timestampWithTimeZone("event_date").nullable()
    val note: Column<String?> = varchar("note", 1000).nullable()
}

/**
 * One entry of a book's ownership history, ie "borrowed from the library on May 1"
 * then "bought on January 10". The latest entry is the current ownership state,
 * mirrored in the owned and borrowed flags of the user book.
 */
class OwnershipEvent(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<OwnershipEvent>(OwnershipEventTable) {
        /**
         * Entries without a date (unknown, or recorded through the owned and borrowed flags)
         * sit on the timeline at the moment they were recorded.
         */
        val TIMELINE_ORDER: Comparator<OwnershipEvent> =
            compareBy<OwnershipEvent>({ it.timelineDate }, { it.creationDate })
    }

    var creationDate by OwnershipEventTable.creationDate
    var modificationDate by OwnershipEventTable.modificationDate
    var userBook by UserBook referencedOn OwnershipEventTable.userBook
    var status by OwnershipEventTable.status
    var eventDate by OwnershipEventTable.eventDate
    var note by OwnershipEventTable.note

    val timelineDate: OffsetDateTime
        get() = eventDate ?: creationDate

    fun toOwnershipEventDto(): OwnershipEventDto =
        OwnershipEventDto(
            id = this.id.value,
            creationDate = this.creationDate,
            modificationDate = this.modificationDate,
            status = this.status,
            eventDate = this.eventDate,
            note = this.note,
        )
}

enum class OwnershipStatus {
    OWNED,
    BORROWED,
    NOT_OWNED,
    ;

    companion object {
        /** Owning a copy wins over borrowing one when both flags are set. */
        fun fromFlags(
            owned: Boolean?,
            borrowed: Boolean?,
        ): OwnershipStatus =
            when {
                owned == true -> OWNED
                borrowed == true -> BORROWED
                else -> NOT_OWNED
            }
    }
}
