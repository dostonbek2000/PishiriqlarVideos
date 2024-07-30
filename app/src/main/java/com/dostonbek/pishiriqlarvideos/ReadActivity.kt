package com.dostonbek.pishiriqlarvideos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import com.dostonbek.pishiriqlarvideos.ui.theme.PishiriqlarVideosTheme
import com.google.firebase.database.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshState

class ReadActivity : ComponentActivity() {
    private val database = FirebaseDatabase.getInstance().reference.child("videos")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PishiriqlarVideosTheme {
                VideoListScreen()

            }
        }
    }

    @Composable
    fun VideoListScreen() {
        val context = LocalContext.current
        val videoDataList = remember { mutableStateListOf<VideoData>() }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isRefreshing by remember { mutableStateOf(false) }

        // Function to fetch video data
        fun fetchVideos() {
            val databaseRef = database
            databaseRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    videoDataList.clear()
                    for (dataSnapshot in snapshot.children) {
                        val videoData = dataSnapshot.getValue(VideoData::class.java)
                        if (videoData != null) {
                            videoDataList.add(videoData)
                        }
                    }
                    isLoading = false
                    isRefreshing = false
                }

                override fun onCancelled(error: DatabaseError) {
                    errorMessage = error.message
                    isLoading = false
                    isRefreshing = false
                }
            })
        }

        // Initial data fetch
        LaunchedEffect(Unit) {
            fetchVideos()
        }

        val swipeRefreshState = remember { SwipeRefreshState(isRefreshing) }

        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                isRefreshing = true
                fetchVideos()
            }
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    fetchVideos()
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Error: $errorMessage", color = Color.Red)
                }
            } else {
                LazyColumn {
                    items(videoDataList) { videoData ->
                        VideoItem(videoData) {
                            val intent = Intent(context, VideoActivity::class.java).apply {
                                putExtra("title", videoData.title)
                                putExtra("description", videoData.description)
                                putExtra("videoUrl", videoData.url)
                            }
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun VideoItem(videoData: VideoData, onClick: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(0.dp, 8.dp)
        ) {
            // Thumbnail Image
            val thumbnailPainter = rememberImagePainter(videoData.thumbnailUrl)
            Image(
                painter = thumbnailPainter,
                contentDescription = "Video Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(220.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Channel Image, Title, and Description Row
            Row(
                modifier = Modifier.padding(12.dp, 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Channel Image
                val channelImagePainter = rememberImagePainter("URL_TO_CHANNEL_IMAGE")
                Image(
                    painter = channelImagePainter,
                    contentDescription = "Channel Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Title and Description Column
                Column {
                    Text(
                        text = videoData.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        text = videoData.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
