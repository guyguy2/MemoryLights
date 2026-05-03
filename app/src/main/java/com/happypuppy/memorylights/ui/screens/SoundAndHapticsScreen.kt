package com.happypuppy.memorylights.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happypuppy.memorylights.R
import com.happypuppy.memorylights.domain.enums.SoundPack
import com.happypuppy.memorylights.ui.theme.SurfaceContainer
import com.happypuppy.memorylights.ui.theme.SurfaceContainerFade
import kotlinx.coroutines.launch

/**
 * Sound & Haptics settings sub-screen (F18). Hosts the sound-pack picker.
 * Vibration toggle is intentionally not duplicated here — the Game
 * screen's top-bar vibration icon is the single canonical control.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundAndHapticsScreen(
    currentSoundPack: SoundPack,
    onSoundPackSelected: (SoundPack) -> Unit,
    onBackPressed: () -> Unit
) {
    val soundPackListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    fun toast(message: String) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    val canScrollDown by remember {
        derivedStateOf { soundPackListState.canScrollForward }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sound & Haptics") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.music_note_24px),
                    contentDescription = "Sound Packs",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Sound Packs",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainer)
            ) {
                LazyColumn(
                    state = soundPackListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(SoundPack.entries.toList()) { soundPack ->
                        SoundPackOption(
                            soundPack = soundPack,
                            isSelected = soundPack == currentSoundPack,
                            onSelect = {
                                onSoundPackSelected(soundPack)
                                toast("Sound pack: ${soundPack.displayName}")
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }

                if (canScrollDown) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        SurfaceContainerFade
                                    )
                                )
                            )
                            .clickable {
                                coroutineScope.launch {
                                    soundPackListState.animateScrollBy(100f)
                                }
                            }
                            .padding(vertical = 8.dp)
                            .align(Alignment.BottomCenter),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "More options",
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "More sound options",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                val showTopFade by remember {
                    derivedStateOf {
                        soundPackListState.firstVisibleItemIndex > 0 ||
                                soundPackListState.firstVisibleItemScrollOffset > 0
                    }
                }

                if (showTopFade) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        SurfaceContainerFade,
                                        Color.Transparent
                                    )
                                )
                            )
                            .align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}
