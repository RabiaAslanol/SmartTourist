package com.example.smarttourist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favori_yerler")
data class FavoriYer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ad: String,
    val aciklama: String,
    val resimUrl: String,
    val latitude: Double,
    val longitude: Double
)
