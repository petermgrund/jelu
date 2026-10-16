package io.github.bayang.jelu.dto

import io.github.bayang.jelu.dao.OwnershipStatus
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

data class OwnershipEventDto(
    val id: UUID?,
    val creationDate: OffsetDateTime?,
    val modificationDate: OffsetDateTime?,
    val status: OwnershipStatus,
    val eventDate: OffsetDateTime?,
    val note: String?,
)

data class CreateOwnershipEventDto(
    val status: OwnershipStatus,
    val eventDate: OffsetDateTime?,
    @field:Size(max = 1000, message = "Note must be at most 1000 characters")
    val note: String?,
)

data class UpdateOwnershipEventDto(
    val status: OwnershipStatus,
    val eventDate: OffsetDateTime?,
    @field:Size(max = 1000, message = "Note must be at most 1000 characters")
    val note: String?,
)
