package com.piotr_brus.learning.controller

import com.piotr_brus.learning.controller.NoteController.NoteResponse
import com.piotr_brus.learning.database.model.Note
import com.piotr_brus.learning.database.repository.NotesRepository
import jakarta.validation.constraints.NotBlank
import org.bson.types.ObjectId
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.time.Instant

private const val emptyTitleMessage = "Title can't be empty."
private const val noteNotFoundMessage = "Note not found."

@RestController
@RequestMapping("/notes")
class NoteController(
    val repository: NotesRepository
) {

    data class NoteRequest(
        val id: String?,
        @NotBlank(message = emptyTitleMessage)
        val title: String,
        val content: String,
        val color: Long,
    )

    data class NoteResponse(
        val id: String?,
        val title: String,
        val content: String,
        val color: Long,
        val createdAt: Instant
    )

    @PostMapping()
    fun save(
        @RequestBody body: NoteRequest
    ): NoteResponse {
        val note = repository.save(
            Note(
                id = body.id?.let { ObjectId(it) } ?: ObjectId.get(),
                title = body.title,
                content = body.content,
                color = body.color,
                createdAt = Instant.now(),
                ownerId = ObjectId(getOwnerId()),
            )
        )
        return note.toResponse()
    }

    @GetMapping
    fun findByOwnerId(): List<NoteResponse> {

        return repository.findByOwnerId(ObjectId(getOwnerId())).map {
            it.toResponse()
        }
    }

    @DeleteMapping(path = ["/{id}"])
    fun deleteById(@PathVariable id: String) {
        val note = repository.findById(ObjectId(id)).orElseThrow {
            IllegalArgumentException(noteNotFoundMessage)
        }

        if (note.ownerId.toHexString() == getOwnerId())
            repository.deleteById(ObjectId(id))
    }
}

private fun getOwnerId(): String {
    return SecurityContextHolder.getContext().authentication.principal as String
}

private fun Note.toResponse(): NoteController.NoteResponse {
    return NoteResponse(
        id = id.toHexString(),
        title = title,
        content = content,
        color = color,
        createdAt = createdAt,
    )
}