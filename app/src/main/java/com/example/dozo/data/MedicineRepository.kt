package com.example.dozo.data

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
// No longer need com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.SetOptions

class MedicineRepository {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    // Get the current user's ID (or a default for anonymous users)
    private fun getUserId(): String = auth.currentUser?.uid ?: "anonymous_user"

    // --- Medicine Rules (The "what" and "when") ---

    fun getMedicinesFlow(): Flow<List<Medicine>> = callbackFlow {
        val listener = db.collection("users").document(getUserId()).collection("medicines")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error); return@addSnapshotListener
                }
                if (snapshot != null) {
                    // Use the new, non-deprecated .toObject(Class) syntax
                    val medicines = snapshot.documents.mapNotNull { it.toObject(Medicine::class.java) }
                    trySend(medicines)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun addMedicine(medicine: Medicine) {
        db.collection("users").document(getUserId())
            .collection("medicines")
            .document(medicine.id)
            .set(medicine)
            .await()
    }

    suspend fun deleteMedicine(medicine: Medicine) {
        db.collection("users").document(getUserId())
            .collection("medicines").document(medicine.id).delete().await()
    }

    // --- Dose History (The "did I take it") ---

    fun getDoseHistoryFlow(): Flow<Map<String, DoseStatus>> = callbackFlow {
        val listener = db.collection("users").document(getUserId()).collection("dose_history")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error); return@addSnapshotListener
                }
                if (snapshot != null) {
                    val statuses = snapshot.documents.mapNotNull {
                        // Use the new, non-deprecated .toObject(Class) syntax
                        val history = it.toObject(DoseHistory::class.java)
                        history?.let { h -> "${h.instanceId}-${h.doseId}" to h.status }
                    }.toMap()
                    trySend(statuses)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateDoseStatus(instanceId: String, doseId: String, newStatus: DoseStatus) {
        val key = "$instanceId-$doseId"
        val historyEntry = DoseHistory(instanceId, doseId, newStatus)
        db.collection("users").document(getUserId())
            .collection("dose_history").document(key).set(historyEntry).await()
    }

    // --- Deleted Instances (For "Undo" Feature) ---

    // Reads the LIST of deleted instance IDs from a single document
    fun getDeletedInstancesFlow(): Flow<List<String>> = callbackFlow {
        val docRef = db.collection("users").document(getUserId())
            .collection("user_data").document("deleted_instances")

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList()) // Send empty on error
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                // Get the 'ids' field, which is a List.
                val ids = (snapshot.get("ids") as? List<String>) ?: emptyList()
                trySend(ids)
            } else {
                trySend(emptyList()) // Send empty if doc doesn't exist
            }
        }
        awaitClose { listener.remove() }
    }

    // Writes a new instance ID to the 'ids' array in the document
    suspend fun addDeletedInstance(instanceId: String) {
        val docRef = db.collection("users").document(getUserId())
            .collection("user_data").document("deleted_instances")

        // .set() with SetOptions.merge() will create the document if it's missing,
        // or merge the new 'ids' value if the document already exists.
        val data = mapOf(
            "ids" to FieldValue.arrayUnion(instanceId)
        )
        // --- THIS IS THE FIX ---
        docRef.set(data, SetOptions.merge()).await()
    }

    // Atomically removes an ID from the 'ids' array in the document
    suspend fun removeDeletedInstance(instanceId: String) {
        val docRef = db.collection("users").document(getUserId())
            .collection("user_data").document("deleted_instances")

        val data = mapOf(
            "ids" to FieldValue.arrayRemove(instanceId)
        )
        // --- THIS IS THE FIX ---
        docRef.set(data, SetOptions.merge()).await()
    }
    suspend fun getMedicineById(medicineId: String): Medicine? {
        return try {
            db.collection("users").document(getUserId())
                .collection("medicines").document(medicineId)
                .get().await().toObject(Medicine::class.java)
        } catch (e: Exception) {
            null
        }
    }
}