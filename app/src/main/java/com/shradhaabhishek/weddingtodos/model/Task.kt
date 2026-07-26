package com.shradhaabhishek.weddingtodos.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Task(
    @DocumentId
    val id: String = "",
    val task: String = "",
    val dueDate: String? = null,
    val username: String? = null,
    val location: String? = null,
    val description: String? = null,
    val completed: Boolean = false,
    val remark: String? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)
