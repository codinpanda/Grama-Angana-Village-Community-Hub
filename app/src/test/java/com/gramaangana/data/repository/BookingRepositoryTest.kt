package com.gramaangana.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import com.gramaangana.data.model.Booking
import com.gramaangana.data.model.DailySchedule
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BookingRepositoryTest {

    private lateinit var db: FirebaseFirestore
    private lateinit var collectionRef: CollectionReference
    private lateinit var documentRef: DocumentReference
    private lateinit var transaction: Transaction
    private lateinit var snapshot: DocumentSnapshot
    private lateinit var repository: BookingRepository

    @Before
    fun setup() {
        db = mockk()
        collectionRef = mockk()
        documentRef = mockk()
        transaction = mockk()
        snapshot = mockk()

        every { db.collection("daily_schedules") } returns collectionRef
        every { collectionRef.document(any()) } returns documentRef
        
        repository = BookingRepository(db)
    }

    @Test
    fun testRequestBookingTransaction_Success() = runBlocking {
        val newBooking = Booking(date = "2026-05-06", startTime = 600, endTime = 660) // 10:00 - 11:00
        val emptySchedule = DailySchedule("2026-05-06", emptyList())
        
        every { transaction.get(documentRef) } returns snapshot
        every { snapshot.exists() } returns true
        every { snapshot.toObject(DailySchedule::class.java) } returns emptySchedule
        every { transaction.set(documentRef, any()) } returns transaction

        // Capture the transaction block
        val functionSlot = slot<Transaction.Function<String>>()
        
        // Mock runTransaction to manually execute the block and return its result wrapped in a Task
        every { db.runTransaction(capture(functionSlot)) } answers {
            try {
                val res = functionSlot.captured.apply(transaction)
                Tasks.forResult(res)
            } catch (e: Exception) {
                Tasks.forException(e)
            }
        }

        val result = repository.requestBookingTransaction(newBooking)
        assertTrue(result.isSuccess)
        assertEquals("Booking successful!", result.getOrNull())
    }

    @Test
    fun testRequestBookingTransaction_Failure_Overlap() = runBlocking {
        val existingBooking = Booking(date = "2026-05-06", startTime = 600, endTime = 660, status = "APPROVED")
        val newBooking = Booking(date = "2026-05-06", startTime = 630, endTime = 690) // Overlaps!
        
        val scheduleWithBooking = DailySchedule("2026-05-06", listOf(existingBooking))
        
        every { transaction.get(documentRef) } returns snapshot
        every { snapshot.exists() } returns true
        every { snapshot.toObject(DailySchedule::class.java) } returns scheduleWithBooking

        val functionSlot = slot<Transaction.Function<String>>()
        
        every { db.runTransaction(capture(functionSlot)) } answers {
            try {
                val res = functionSlot.captured.apply(transaction)
                Tasks.forResult(res)
            } catch (e: Exception) {
                Tasks.forException(e)
            }
        }

        val result = repository.requestBookingTransaction(newBooking)
        assertTrue(result.isFailure)
        assertEquals("Time slot is already booked or overlaps with an existing booking.", result.exceptionOrNull()?.message)
    }
}
