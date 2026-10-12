package com.example.ui

import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberNavyBg
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay

private const val SPLASH_VIDEO_URL = "https://gamenetrokh.ir/vie/video_2026-09-27_07-08-50.mp4"
private const val MAX_WAIT_TIMEOUT_MS = 15_000L

@Composable
fun VideoSplashScreen(
    onVideoFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVideoReady by remember { mutableStateOf(false) }
    var hasFinished by remember { mutableStateOf(false) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    val finishOnce = {
        if (!hasFinished) {
            hasFinished = true
            try {
                videoViewRef?.stopPlayback()
            } catch (_: Exception) {}
            onVideoFinished()
        }
    }

    // Safety timeout: In case the video takes too long to load or fails on poor network
    LaunchedEffect(Unit) {
        delay(MAX_WAIT_TIMEOUT_MS)
        finishOnce()
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                videoViewRef?.stopPlayback()
            } catch (_: Exception) {}
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CyberNavyBg)
                .testTag("video_splash_screen"),
            contentAlignment = Alignment.Center
        ) {
            // Video Player
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        setVideoURI(Uri.parse(SPLASH_VIDEO_URL))

                        setOnPreparedListener { mp ->
                            try {
                                mp.isLooping = false
                                mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                                isVideoReady = true
                                start()
                            } catch (_: Exception) {
                                finishOnce()
                            }
                        }

                        setOnCompletionListener {
                            finishOnce()
                        }

                        setOnErrorListener { _, _, _ ->
                            // Network error or playback error -> continue to app without blocking
                            finishOnce()
                            true
                        }

                        videoViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Loading state indicator while video buffers
            AnimatedVisibility(
                visible = !isVideoReady,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.zIndex(5f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(CyanPrimary.copy(alpha = 0.25f), CyberSurfaceVariant)
                                )
                            )
                            .border(1.5.dp, CyanPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Logo",
                            tint = CyanPrimary,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    Spacer(modifier = Modifier.size(20.dp))

                    Text(
                        text = "MESHKIDNS",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    CircularProgressIndicator(
                        color = CyanPrimary,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp
                    )

                    Spacer(modifier = Modifier.size(10.dp))

                    Text(
                        text = "در حال بارگذاری...",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // Top Skip Button (Always visible so user can skip if desired)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f)
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { finishOnce() }
                        .testTag("skip_splash_video_btn"),
                    color = CyberNavyBg.copy(alpha = 0.75f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "رد کردن",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Skip",
                            tint = CyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
