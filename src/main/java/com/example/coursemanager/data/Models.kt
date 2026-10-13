package com.example.coursemanager.data

data class Category(
    val id: String? = null,
    val name: String = "",
    val description: String = ""
)

data class Course(
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val categoryId: String = "",
    val instructor: String = "",
    val price: Double = 0.0,
    val duration: Double = 0.0,
    val thumbnail: String = "",
    val trailerUrl: String = "",
    val createdAt: String = ""
)
