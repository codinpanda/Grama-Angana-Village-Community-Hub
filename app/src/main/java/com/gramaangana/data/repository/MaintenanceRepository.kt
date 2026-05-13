package com.gramaangana.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.gramaangana.data.model.MaintenanceItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MaintenanceRepository(private val db: FirebaseFirestore) {

    fun getMaintenanceItems(): Flow<List<MaintenanceItem>> = callbackFlow {
        val collection = db.collection("maintenance_items")
        val subscription = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val itemsFromDb = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(MaintenanceItem::class.java)
            } ?: emptyList()

            // For Demo/Initial Launch: Provide defaults if DB is empty
            if (itemsFromDb.isEmpty()) {
                trySend(listOf(
                    MaintenanceItem(1, "Replacement Bulbs", "Energy efficient LED bulbs for hall.", 2500.0, 1200.0, "NEEDED"),
                    MaintenanceItem(2, "New Chairs (x50)", "Durable plastic chairs for large events.", 15000.0, 1500.0, "NEEDED"),
                    MaintenanceItem(3, "Wall Painting", "Fresh coat of paint for the main hall.", 45000.0, 38000.0, "NEEDED")
                ))
            } else {
                trySend(itemsFromDb)
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun addMaintenanceItem(item: MaintenanceItem): Result<Unit> {
        return try {
            db.collection("maintenance_items").document(item.itemName).set(item).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun closeMaintenanceItem(itemName: String): Result<Unit> {
        return try {
            db.collection("maintenance_items").document(itemName).update("status", "CLOSED").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun contributeToItem(itemId: String, amount: Double): Result<Unit> {
        val docRef = db.collection("maintenance_items").document(itemId)
        return try {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    val currentAmount = snapshot.getDouble("collectedAmount") ?: 0.0
                    transaction.update(docRef, "collectedAmount", currentAmount + amount)
                } else {
                    // Seed the item if it was a default but not yet in DB
                    val defaultItems = listOf(
                        MaintenanceItem(1, "Replacement Bulbs", "Energy efficient LED bulbs for hall.", 2500.0, 1200.0, "NEEDED"),
                        MaintenanceItem(2, "New Chairs (x50)", "Durable plastic chairs for large events.", 15000.0, 1500.0, "NEEDED"),
                        MaintenanceItem(3, "Wall Painting", "Fresh coat of paint for the main hall.", 45000.0, 38000.0, "NEEDED")
                    )
                    val item = defaultItems.find { it.itemName == itemId }
                    if (item != null) {
                        transaction.set(docRef, item.copy(collectedAmount = item.collectedAmount + amount))
                    } else {
                        throw Exception("Item not found")
                    }
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
