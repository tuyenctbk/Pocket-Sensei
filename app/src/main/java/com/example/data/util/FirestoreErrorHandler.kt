package com.example.data.util

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
import org.json.JSONObject

enum class OperationType {
    GET,
    LIST,
    CREATE,
    UPDATE,
    DELETE
}

fun handleFirestoreError(exception: Exception, operation: OperationType, path: String) {
    val firestoreException = exception as? FirebaseFirestoreException
    val errorCode = firestoreException?.code?.name ?: "UNKNOWN"
    val errorDetails = JSONObject().apply {
        put("error_code", errorCode)
        put("operation", operation.name)
        put("path", path)
        put("raw_message", exception.message ?: "No message provided")
    }
    Log.e("FirestoreError", errorDetails.toString(), exception)
}
