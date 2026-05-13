package com.gramaangana.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gramaangana.data.model.MaintenanceItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance_items")
    fun getAllItems(): Flow<List<MaintenanceItem>>

    @Insert
    suspend fun insertItem(item: MaintenanceItem)

    @Update
    suspend fun updateItem(item: MaintenanceItem)
}
