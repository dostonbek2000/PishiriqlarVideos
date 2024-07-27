package com.dostonbek.pishiriqlarvideos

import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ReadActivity : ComponentActivity() {
    private val database = FirebaseDatabase.getInstance().reference.child("videos")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReadScreen()
        }
    }

    @Composable
    fun ReadScreen() {
        var videos by remember { mutableStateOf<List<VideoData>>(emptyList()) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            database.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val videoList = mutableListOf<VideoData>()
                    snapshot.children.forEach { childSnapshot ->
                        val video = childSnapshot.getValue(VideoData::class.java)
                        video?.let { videoList.add(it) }
                    }
                    videos = videoList
                }

                override fun onCancelled(error: DatabaseError) {
                    errorMessage = error.message
                }
            })
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(text = "Videos", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(16.dp))

            when {
                videos.isNotEmpty() -> {
                    LazyColumn {
                        items(videos) { video ->
                            VideoItem(video)
                        }
                    }
                }

                errorMessage != null -> {
                    Text("Error: $errorMessage")
                }

                else -> {
                    CircularProgressIndicator()
                }
            }
        }
    }

    @Composable
    fun VideoItem(video: VideoData) {
        var isPlaying by remember { mutableStateOf(false) }

        Column(modifier = Modifier.padding(8.dp)) {

            VideoPlayer(
                videoUrl = video.url,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),

                playWhenReady = isPlaying,
                onPlayPauseClick = { isPlaying = !isPlaying }
            )

            Text(text = video.title, color = Color.Black, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = video.description)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    playWhenReady: Boolean,
    onPlayPauseClick: () -> Unit,
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
            prepare()
            //    playWhenReady= playWhenReady
        }
    }


    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Column(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)

        )

        Spacer(modifier = Modifier.height(8.dp))


    }
}