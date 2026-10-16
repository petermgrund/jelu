package io.github.bayang.jelu.service

import io.github.bayang.jelu.dao.OwnershipEventRepository
import io.github.bayang.jelu.dto.CreateOwnershipEventDto
import io.github.bayang.jelu.dto.OwnershipEventDto
import io.github.bayang.jelu.dto.UpdateOwnershipEventDto
import io.github.bayang.jelu.dto.UserDto
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Ownership history of the books of a user. Every call checks that the book belongs to
 * the given user.
 */
@Component
class OwnershipEventService(
    private val ownershipEventRepository: OwnershipEventRepository,
) {
    @Transactional
    fun findAll(
        userBookId: UUID,
        user: UserDto,
    ): List<OwnershipEventDto> {
        val userBook = ownershipEventRepository.findUserBookOfUser(userBookId, user.id!!)
        return ownershipEventRepository.findByUserBook(userBook).map { it.toOwnershipEventDto() }
    }

    @Transactional
    fun save(
        userBookId: UUID,
        dto: CreateOwnershipEventDto,
        user: UserDto,
    ): OwnershipEventDto {
        val userBook = ownershipEventRepository.findUserBookOfUser(userBookId, user.id!!)
        return ownershipEventRepository.save(userBook, dto).toOwnershipEventDto()
    }

    @Transactional
    fun update(
        userBookId: UUID,
        eventId: UUID,
        dto: UpdateOwnershipEventDto,
        user: UserDto,
    ): OwnershipEventDto {
        val userBook = ownershipEventRepository.findUserBookOfUser(userBookId, user.id!!)
        val event = ownershipEventRepository.findEventOfUserBook(eventId, userBook)
        return ownershipEventRepository.update(event, dto).toOwnershipEventDto()
    }

    @Transactional
    fun delete(
        userBookId: UUID,
        eventId: UUID,
        user: UserDto,
    ) {
        val userBook = ownershipEventRepository.findUserBookOfUser(userBookId, user.id!!)
        val event = ownershipEventRepository.findEventOfUserBook(eventId, userBook)
        ownershipEventRepository.delete(event)
    }
}
