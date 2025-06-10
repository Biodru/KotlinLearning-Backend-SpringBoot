package com.piotr_brus.learning.database.repository

import com.piotr_brus.learning.database.model.Note
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface NotesRepository: MongoRepository<Note, ObjectId> {
}