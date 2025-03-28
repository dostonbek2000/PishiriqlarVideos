package com.dostonbek.pishiriqlarvideos

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberImagePainter
import com.dostonbek.pishiriqlarvideos.ui.theme.ColorIcon
import com.dostonbek.pishiriqlarvideos.ui.theme.PishiriqlarVideosTheme
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.io.ByteArrayOutputStream

class UploadActivity : ComponentActivity() {
    private val storage = FirebaseStorage.getInstance().reference
    private val database = FirebaseDatabase.getInstance().reference.child("videos")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PishiriqlarVideosTheme {
                UploadScreen()
            }
        }
    }

    @Composable
    fun UploadScreen() {
        var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
        var selectedThumbnailUri by remember { mutableStateOf<Uri?>(null) }
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var uploadState by remember { mutableStateOf(UploadState.IDLE) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var uploadProgress by remember { mutableFloatStateOf(0f) }
        var showDialog by remember { mutableStateOf(false) }
        var uploadTask by remember { mutableStateOf<UploadTask?>(null) }

        val videoLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                selectedVideoUri = uri
            }

        val thumbnailLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                selectedThumbnailUri = uri
            }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(15.dp)
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RectangleShape)
                    .background(color = Color.White, shape = RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selectedThumbnailUri != null) {
                    Image(
                        painter = rememberImagePainter(selectedThumbnailUri),
                        contentDescription = "Selected Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.height(220.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.padding(12.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {


                Column(horizontalAlignment = Alignment.CenterHorizontally) {


                    Button(
                        onClick = { videoLauncher.launch("video/*") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedVideoUri != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = "Choose video")
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {


                    Button(
                        onClick = { thumbnailLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedThumbnailUri != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = "Choose thumbnail")
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(0.8f),
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                label = { Text(text = "Title") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(0.8f),
                value = description,
                onValueChange = { description = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                label = { Text(text = "Description") }
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = {
                if (selectedVideoUri != null) {
                    showDialog = true
                    uploadVideo(
                        selectedVideoUri!!,
                        selectedThumbnailUri,
                        title,
                        description
                    ) { progress ->
                        uploadProgress = progress
                    }
                } else {
                    errorMessage = "Please select a video to upload."
                }
            }) {
                Text(text = "Videoni yuklash")
            }

            Spacer(modifier = Modifier.height(20.dp))
            if (uploadState == UploadState.ERROR) {
                Text(text = "Upload failed: $errorMessage")
            }
        }

        if (uploadState == UploadState.SUCCESS) {
            Toast.makeText(applicationContext, "Uploaded", Toast.LENGTH_SHORT).show()
        }

        if (showDialog) {
            UploadProgressDialog(
                progress = uploadProgress,
                onDismiss = {
                    uploadTask?.cancel()
                    showDialog = false
                    uploadState = UploadState.IDLE
                }
            )
        }
    }

    @Composable
    fun UploadProgressDialog(progress: Float, onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Uploading Video") },
            text = {
                Column {
                    LinearProgressIndicator(progress = progress / 100f)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "${progress.toInt()}%")
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "If you close this dialog, the file will not be uploaded.")
                }
            },
            confirmButton = {
                Text(
                    "Close",
                    color = ColorIcon,
                    modifier = Modifier.clickable { onDismiss() }
                )
            },
            modifier = Modifier.fillMaxWidth(0.8f),
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        )
    }

    private fun uploadVideo(
        videoUri: Uri,
        thumbnailUri: Uri?,
        title: String,
        description: String,
        onProgress: (Float) -> Unit,
    ) {
        val videoRef = storage.child("videos/${videoUri.lastPathSegment}")
        val task = videoRef.putFile(videoUri)

        task.addOnProgressListener { snapshot ->
            val progress = (100 * snapshot.bytesTransferred / snapshot.totalByteCount).toFloat()
            onProgress(progress)
        }

        task.addOnSuccessListener {
            videoRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                val thumbnailUpload = if (thumbnailUri != null) {
                    { onComplete: (String) -> Unit ->
                        uploadCustomThumbnail(
                            thumbnailUri,
                            onComplete
                        )
                    }
                } else {
                    { onComplete: (String) -> Unit -> uploadThumbnail(videoUri, onComplete) }
                }

                thumbnailUpload { thumbnailUrl ->
                    val videoData = VideoData(
                        url = downloadUrl.toString(),
                        thumbnailUrl = thumbnailUrl,
                        title = title,
                        description = description,
                        uploadTime = System.currentTimeMillis().toString()
                    )
                    database.push().setValue(videoData).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            //showDialog = false
                            //uploadState = UploadState.SUCCESS
                        } else {
                            //errorMessage = "Failed to upload video metadata."
                            //uploadState = UploadState.ERROR
                        }
                    }
                }
            }
        }.addOnFailureListener { e ->
            // errorMessage = e.toString()
            // uploadState = UploadState.ERROR
        }
    }

    private fun uploadCustomThumbnail(thumbnailUri: Uri, onComplete: (String) -> Unit) {
        val thumbnailRef = storage.child("thumbnails/${thumbnailUri.lastPathSegment}")
        val uploadTask = thumbnailRef.putFile(thumbnailUri)

        uploadTask.addOnSuccessListener {
            thumbnailRef.downloadUrl.addOnSuccessListener { uri ->
                onComplete(uri.toString())
            }
        }.addOnFailureListener {
            onComplete("")  // Handle failure case
        }
    }

    private fun uploadThumbnail(videoUri: Uri, onComplete: (String) -> Unit) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(applicationContext, videoUri)
        val bitmap = retriever.getFrameAtTime(0)
        val stream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val data = stream.toByteArray()
        val thumbnailRef = storage.child("thumbnails/${videoUri.lastPathSegment}.png")

        val uploadTask = thumbnailRef.putBytes(data)
        uploadTask.addOnSuccessListener {
            thumbnailRef.downloadUrl.addOnSuccessListener { uri ->
                onComplete(uri.toString())
            }
        }.addOnFailureListener {
            onComplete("")  // Handle failure case
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        UploadScreen()
    }

    enum class UploadState {
        IDLE, UPLOADING, SUCCESS, ERROR
    }


}
