package io.github.bayang.jelu.controllers

import io.github.bayang.jelu.dto.CreateOwnershipEventDto
import io.github.bayang.jelu.dto.JeluUser
import io.github.bayang.jelu.dto.OwnershipEventDto
import io.github.bayang.jelu.dto.UpdateOwnershipEventDto
import io.github.bayang.jelu.dto.assertIsJeluUser
import io.github.bayang.jelu.service.OwnershipEventService
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class OwnershipEventsController(
    private val ownershipEventService: OwnershipEventService,
) {
    @GetMapping(path = ["/userbooks/{userBookId}/ownership-events"])
    fun ownershipEvents(
        @PathVariable("userBookId") userBookId: UUID,
        principal: Authentication,
    ): List<OwnershipEventDto> = ownershipEventService.findAll(userBookId, user(principal))

    @PostMapping(path = ["/userbooks/{userBookId}/ownership-events"])
    fun saveOwnershipEvent(
        @PathVariable("userBookId") userBookId: UUID,
        @RequestBody @Valid
        event: CreateOwnershipEventDto,
        principal: Authentication,
    ): OwnershipEventDto = ownershipEventService.save(userBookId, event, user(principal))

    @PutMapping(path = ["/userbooks/{userBookId}/ownership-events/{eventId}"])
    fun updateOwnershipEvent(
        @PathVariable("userBookId") userBookId: UUID,
        @PathVariable("eventId") eventId: UUID,
        @RequestBody @Valid
        event: UpdateOwnershipEventDto,
        principal: Authentication,
    ): OwnershipEventDto = ownershipEventService.update(userBookId, eventId, event, user(principal))

    @ApiResponse(responseCode = "204", description = "Deleted the ownership event")
    @DeleteMapping(path = ["/userbooks/{userBookId}/ownership-events/{eventId}"])
    fun deleteOwnershipEvent(
        @PathVariable("userBookId") userBookId: UUID,
        @PathVariable("eventId") eventId: UUID,
        principal: Authentication,
    ): ResponseEntity<Unit> {
        ownershipEventService.delete(userBookId, eventId, user(principal))
        return ResponseEntity.noContent().build()
    }

    private fun user(principal: Authentication) =
        principal.principal.let {
            assertIsJeluUser(it)
            (it as JeluUser).user
        }
}
