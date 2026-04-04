package com.hudsom.kotlinceapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comunidades")
data class Comunidade(
    @PrimaryKey val id: String = "",
    val nome: String = "",
    val lider: String = "",
    val descricao: String = "",
    val ownerId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val endereco: String = "",
    val imagem: String = ""
)
