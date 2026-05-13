package com.gramaangana.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gramaangana.data.model.MaintenanceItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class MaintenanceDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: MaintenanceDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.maintenanceDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndReadItem_success() = runBlocking {
        val item = MaintenanceItem(
            itemName = "Test Item",
            description = "Test Description",
            targetAmount = 1000.0,
            collectedAmount = 0.0
        )
        
        dao.insertItem(item)
        
        val items = dao.getAllItems().first()
        assertEquals(1, items.size)
        assertEquals("Test Item", items[0].itemName)
        assertEquals(0.0, items[0].collectedAmount, 0.01)
    }

    @Test
    fun updateItem_success() = runBlocking {
        val item = MaintenanceItem(
            itemName = "Test Item",
            description = "Test Description",
            targetAmount = 1000.0,
            collectedAmount = 0.0
        )
        dao.insertItem(item)
        
        var items = dao.getAllItems().first()
        val insertedItem = items[0]
        
        val updatedItem = insertedItem.copy(collectedAmount = 500.0)
        dao.updateItem(updatedItem)
        
        items = dao.getAllItems().first()
        assertEquals(500.0, items[0].collectedAmount, 0.01)
    }
}
