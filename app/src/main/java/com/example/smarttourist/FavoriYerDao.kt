package com.example.smarttourist

import androidx.room.*

@Dao
interface FavoriYerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ekle(yeniYer: FavoriYer)

    @Delete
    suspend fun sil(yer: FavoriYer)

    @Query("SELECT * FROM favori_yerler")
    suspend fun tumFavoriler(): List<FavoriYer>

    @Query("DELETE FROM favori_yerler")
    suspend fun tumFavorileriSil()

}
