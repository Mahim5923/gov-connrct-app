package com.example.govchatbotapp.data

import android.util.Log

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import com.google.firebase.firestore.Query

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance(com.google.firebase.FirebaseApp.getInstance(), "gov-connect-app")
    
    fun getCurrentUser() = auth.currentUser

    suspend fun addComplaint(complaint: ComplaintEntity, onComplete: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            try {
                val complaintWithUserId = complaint.copy(userId = user.uid)
                db.collection("complaints").document(complaint.id).set(complaintWithUserId)
                    .addOnSuccessListener {
                        Log.d("FirebaseRepo", "Complaint successfully synced to cloud: ${complaint.id}")
                        onComplete(true, null)
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseRepo", "FAILED to sync complaint to cloud! Check Firestore Rules.", e)
                        onComplete(false, e.message)
                    }
                Log.d("FirebaseRepo", "Complaint queued locally: ${complaint.id}")
            } catch (e: Exception) {
                Log.e("FirebaseRepo", "Error queueing complaint locally", e)
                onComplete(false, e.message)
            }
        } else {
            Log.e("FirebaseRepo", "Error: User is null when saving complaint")
            onComplete(false, "User is not logged in")
        }
    }
    
    fun getUserComplaints(): Flow<List<ComplaintEntity>> = callbackFlow {
        val user = auth.currentUser
        if (user == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = db.collection("complaints")
            .whereEqualTo("userId", user.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FirebaseRepo", "Error fetching user complaints", e)
                    close(e)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val complaints = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ComplaintEntity::class.java)
                    }
                    trySend(complaints)
                }
            }
            
        awaitClose { listener.remove() }
    }
    
    fun getAllComplaints(): Flow<List<ComplaintEntity>> = callbackFlow {
        val listener = db.collection("complaints")
            // Removed orderBy to prevent potential index issues
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FirebaseRepo", "Error fetching all complaints", e)
                    close(e)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val complaints = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ComplaintEntity::class.java)
                    }
                    trySend(complaints)
                }
            }
            
        awaitClose { listener.remove() }
    }
    
    suspend fun updateComplaintStatus(id: String, newStatus: String) {
        db.collection("complaints").document(id).update("status", newStatus).await()
    }
}
