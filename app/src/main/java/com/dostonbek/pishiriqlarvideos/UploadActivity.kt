package com.dostonbek.pishiriqlarvideos

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class UploadActivity : ComponentActivity() {
    private val storage = FirebaseStorage.getInstance().reference
    private val database = FirebaseDatabase.getInstance().reference.child("videos")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UploadScreen()
        }
    }

    @Composable
    fun UploadScreen() {
        var selectedUri by remember { mutableStateOf<Uri?>(null) }
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var uploadState by remember { mutableStateOf(UploadState.IDLE) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        val launcher =
            rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                selectedUri = uri
            }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(text = "Upload Video", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { launcher.launch("video/*") }) {
                Text("Select Video")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                if (selectedUri != null) {
                    uploadVideo(selectedUri!!, title, description)
                    Toast.makeText(applicationContext, "Uploading...", Toast.LENGTH_SHORT).show()
                } else {
                    errorMessage = "Please select a video to upload."
                }
            }) {
                Text("Upload Video")
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (uploadState) {
                UploadState.UPLOADING -> CircularProgressIndicator()
                UploadState.SUCCESS -> Toast.makeText(
                    applicationContext,
                    "Uploaded",
                    Toast.LENGTH_SHORT
                ).show()

                UploadState.ERROR -> Text("Upload failed: $errorMessage")
                else -> Unit
            }
        }
    }

    private fun uploadVideo(uri: Uri, title: String, description: String) {
        val videoRef = storage.child("videos/${uri.lastPathSegment}")
        val uploadTask = videoRef.putFile(uri)


        uploadTask.addOnSuccessListener {
            videoRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                val videoData = VideoData(
                    url = downloadUrl.toString(),
                    thumbnailUrl = "", // Generate or select a thumbnail if needed
                    title = title,
                    description = description,
                    uploadTime = System.currentTimeMillis().toString()
                )
                database.push().setValue(videoData).addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        Toast.makeText(this, "Uploaded", Toast.LENGTH_SHORT).show()

                    } else {

                        Toast.makeText(this, " Not Uploaded", Toast.LENGTH_SHORT).show()

                    }
                }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()

        }
    }

    enum class UploadState {
        IDLE, UPLOADING, SUCCESS, ERROR
    }
}
