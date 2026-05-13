package com.gramaangana.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.gramaangana.data.model.Booking
import com.gramaangana.data.model.DailySchedule
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BookingRepository(private val db: FirebaseFirestore) {

    init {
        // Optimization: Ensure offline persistence is configured for rural usage
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
        db.firestoreSettings = settings
    }


    fun getBookingsForDate(date: String): Flow<List<Booking>> = callbackFlow {
        val docRef = db.collection("daily_schedules").document(date)
        val subscription = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val schedule = snapshot?.toObject(DailySchedule::class.java)
            trySend(schedule?.bookings ?: emptyList())
        }
        awaitClose { subscription.remove() }
    }

    fun getAllPendingRequests(): Flow<List<Booking>> = callbackFlow {
        val subscription = db.collection("daily_schedules")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val allPending = snapshot?.documents?.flatMap { doc ->
                    val schedule = doc.toObject(DailySchedule::class.java)
                    schedule?.bookings?.filter { it.status == "PENDING" } ?: emptyList()
                } ?: emptyList()
                trySend(allPending)
            }
        awaitClose { subscription.remove() }
    }

    fun getMonthAvailability(yearMonth: String): Flow<Map<String, String>> = callbackFlow {
        // yearMonth format: "YYYY-MM"
        // Use a wide enough range to cover all possible days in any month
        val subscription = db.collection("daily_schedules")
            .whereGreaterThanOrEqualTo("date", "$yearMonth-01")
            .whereLessThanOrEqualTo("date", "$yearMonth-31")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val availability = snapshot?.documents?.associate { doc ->
                    val schedule = doc.toObject(DailySchedule::class.java)
                    val date = doc.id
                    // Filter for active/approved bookings to show accurate availability
                    val activeBookings = schedule?.bookings?.filter { it.status == "APPROVED" || it.status == "PENDING" } ?: emptyList()
                    val status = when {
                        activeBookings.isEmpty() -> "FREE"
                        activeBookings.size >= 3 -> "FULL"
                        else -> "PARTIAL"
                    }
                    date to status
                } ?: emptyMap()
                trySend(availability)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun requestBookingTransaction(newBooking: Booking): Result<String> {
        val dailyScheduleRef = db.collection("daily_schedules").document(newBooking.date)

        return try {
            val result = db.runTransaction { transaction ->
                val snapshot = transaction.get(dailyScheduleRef)
                
                val schedule = if (snapshot.exists()) {
                    snapshot.toObject(DailySchedule::class.java) ?: DailySchedule(newBooking.date)
                } else {
                    DailySchedule(newBooking.date)
                }

                val hasOverlap = schedule.bookings.any { existing ->
                    existing.status != "REJECTED" &&
                    newBooking.startTime < existing.endTime && 
                    newBooking.endTime > existing.startTime
                }

                if (hasOverlap) {
                    throw Exception("Time slot is already booked or overlaps with an existing booking.")
                }

                val updatedBookings = schedule.bookings + newBooking
                transaction.set(dailyScheduleRef, schedule.copy(bookings = updatedBookings))
                
                "Booking successful!"
            }.await()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
