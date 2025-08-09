package com.example.dailybite.data.post

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {

    suspend fun createPost(
        ownerUid: String,
        mealType: String,
        description: String,
        imageBytes: ByteArray
    ): Result<String> = runCatching {
        val postId = firestore.collection("posts").document().id
        val path = "posts/$ownerUid/$postId.jpg"
        storage.reference.child(path).putBytes(imageBytes).await()

        val now = System.currentTimeMillis()
        val data = mapOf(
            "id" to postId,
            "ownerUid" to ownerUid,
            "mealType" to mealType,
            "description" to description,
            "imageStoragePath" to path,
            "createdAt" to now,
            "updatedAt" to now,
            "likesCount" to 0
        )
        firestore.collection("posts").document(postId).set(data).await()
        postId
    }

    fun feedQuery(): Query =
        firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)

    fun feedFlow() = callbackFlow<List<PostItem>> {
        val reg: ListenerRegistration = firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList()).isSuccess
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val ownerUid = doc.getString("ownerUid") ?: return@mapNotNull null
                    val mealType = doc.getString("mealType") ?: ""
                    val description = doc.getString("description") ?: ""
                    val path = doc.getString("imageStoragePath") ?: ""
                    val createdAt = doc.getLong("createdAt") ?: 0L
                    val likesCount = (doc.getLong("likesCount") ?: 0L).toInt()
                    PostItem(
                        id = id,
                        ownerUid = ownerUid,
                        mealType = mealType,
                        description = description,
                        imageStoragePath = path,
                        createdAt = createdAt,
                        likesCount = likesCount
                    )
                } ?: emptyList()
                trySend(list).isSuccess
            }
        awaitClose { reg.remove() }
    }

    fun myPostsFlow(ownerUid: String) = callbackFlow<List<PostItem>> {
        val reg: ListenerRegistration = firestore.collection("posts")
            .whereEqualTo("ownerUid", ownerUid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList()).isSuccess
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val mealType = doc.getString("mealType") ?: ""
                    val description = doc.getString("description") ?: ""
                    val path = doc.getString("imageStoragePath") ?: ""
                    val createdAt = doc.getLong("createdAt") ?: 0L
                    val likesCount = (doc.getLong("likesCount") ?: 0L).toInt()
                    PostItem(
                        id = id,
                        ownerUid = ownerUid,
                        mealType = mealType,
                        description = description,
                        imageStoragePath = path,
                        createdAt = createdAt,
                        likesCount = likesCount
                    )
                } ?: emptyList()
                trySend(list).isSuccess
            }
        awaitClose { reg.remove() }
    }

    suspend fun like(postId: String, userUid: String): Result<Unit> = runCatching {
        firestore.collection("posts").document(postId)
            .update("likesCount", FieldValue.increment(1))
            .await()

        val fidRef = firestore.collection("feedback")
            .document(postId)
            .collection("items")
            .document()

        val payload = mapOf(
            "id" to fidRef.id,
            "postId" to postId,
            "authorUid" to userUid,
            "type" to "like",
            "createdAt" to System.currentTimeMillis()
        )
        fidRef.set(payload).await()
    }

    suspend fun deletePost(postId: String, imageStoragePath: String): Result<Unit> = runCatching {
        // מוחקים קודם את התמונה מה־Storage
        if (imageStoragePath.isNotEmpty()) {
            storage.reference.child(imageStoragePath).delete().await()
        }
        // ואז את המסמך מה־Firestore
        firestore.collection("posts").document(postId).delete().await()

        // אופציונלי: מחיקת פידבקים של הפוסט
        // val items = firestore.collection("feedback").document(postId).collection("items").get().await()
        // items.documents.forEach { it.reference.delete().await() }
    }

    suspend fun addComment(postId: String, userUid: String, text: String): Result<Unit> = runCatching {
        val ref = firestore.collection("feedback").document(postId).collection("items").document()
        val payload = mapOf(
            "id" to ref.id,
            "postId" to postId,
            "authorUid" to userUid,
            "type" to "comment",
            "text" to text,
            "createdAt" to System.currentTimeMillis()
        )
        ref.set(payload).await()
    }

    fun commentsFlow(postId: String): Flow<List<CommentItem>> = callbackFlow {
        val reg: ListenerRegistration = firestore.collection("feedback").document(postId)
            .collection("items")
            .whereEqualTo("type", "comment")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList()).isSuccess; return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { d ->
                    CommentItem(
                        id = d.getString("id") ?: d.id,
                        postId = d.getString("postId") ?: return@mapNotNull null,
                        authorUid = d.getString("authorUid") ?: "",
                        text = d.getString("text") ?: "",
                        createdAt = d.getLong("createdAt") ?: 0L
                    )
                } ?: emptyList()
                trySend(list).isSuccess
            }
        awaitClose { reg.remove() }
    }
    // קריאת פוסט יחיד (למקרה שנרצה למשוך מהשרת)
    suspend fun getPost(postId: String): Result<PostItem> = runCatching {
        val doc = firestore.collection("posts").document(postId).get().await()
        val id = doc.getString("id") ?: doc.id
        val ownerUid = doc.getString("ownerUid") ?: error("ownerUid missing")
        val mealType = doc.getString("mealType") ?: ""
        val description = doc.getString("description") ?: ""
        val path = doc.getString("imageStoragePath") ?: ""
        val createdAt = doc.getLong("createdAt") ?: 0L
        val likesCount = (doc.getLong("likesCount") ?: 0L).toInt()
        PostItem(id, ownerUid, mealType, description, path, createdAt, likesCount)
    }

    // עדכון פוסט: שינוי תיאור/סוג ארוחה + אופציה להחלפת תמונה
    suspend fun updatePost(
        postId: String,
        mealType: String,
        description: String,
        imageStoragePath: String,
        newImageBytes: ByteArray? // null = בלי שינוי תמונה
    ): Result<Unit> = runCatching {
        if (newImageBytes != null) {
            // נעלה על אותו path כדי לא לשבור קישורים
            storage.reference.child(imageStoragePath).putBytes(newImageBytes).await()
        }
        firestore.collection("posts").document(postId).update(
            mapOf(
                "mealType" to mealType,
                "description" to description,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

}