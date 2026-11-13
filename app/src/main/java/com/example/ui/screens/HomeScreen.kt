package com.example.ui.screens

import android.Manifest
import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ai.PronunciationResult
import com.example.analytics.DailyStreakState
import com.example.analytics.EngagementPrompt
import com.example.analytics.PromptAction
import com.example.audio.SpeechManager
import com.example.data.model.LeaderboardEntry
import com.example.data.model.PracticeSession
import com.example.data.model.UserProfile
import com.example.donation.DonationChannel
import com.example.ui.DialogMessage
import com.example.ui.EvaluationUiState
import com.example.ui.LearningTab
import com.example.ui.MainViewModel
import com.example.ui.PracticePhrase
import com.example.ui.RoleplayScenario
import com.example.ui.RoleplayUiState
import com.example.ui.view.Sensei3DView
import com.example.ui.view.SenseiViewController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onReplayOnboarding: () -> Unit = {},
    onSignOutComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val controller = remember { SenseiViewController(context) }

    val currentTab by viewModel.currentTab.collectAsState()
    val targetLanguage by viewModel.targetLanguage.collectAsState()
    val profile by viewModel.profileState.collectAsState()
    val senseiMood by viewModel.senseiMood.collectAsState()
    val senseiCostume by viewModel.senseiCostume.collectAsState()
    val activePrompt by viewModel.activeEngagementPrompt.collectAsState()
    val isActiveSupporter by viewModel.isActiveSupporter.collectAsState()
    val shouldShowDonationBanner by viewModel.shouldShowHomeDonationBanner.collectAsState()
    val donationUiState by viewModel.donationUiState.collectAsState()
    val openDonationRequested by viewModel.openDonationSheetRequested.collectAsState()
    val successfulOperations by viewModel.successfulOperationsCount.collectAsState()
    val dailyStreakState by viewModel.dailyStreakState.collectAsState()
    val effectiveStreak = maxOf(
        dailyStreakState.currentStreak.toLong(),
        profile?.currentStreak ?: 0L
    )

    var showDonationModal by remember { mutableStateOf(false) }
    var donationInitialChannel by remember { mutableStateOf(DonationChannel.GOOGLE_PLAY) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showStreakDialog by remember { mutableStateOf(false) }
    var manualStageExpanded by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(openDonationRequested) {
        if (openDonationRequested) {
            donationInitialChannel = DonationChannel.GOOGLE_PLAY
            viewModel.trackDonationSheetOpened(DonationChannel.GOOGLE_PLAY)
            showDonationModal = true
            viewModel.consumeOpenDonationSheetRequest()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPurchases()
                viewModel.refreshDailyEngagement()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler(enabled = currentTab != LearningTab.COACH) {
        viewModel.setTab(LearningTab.COACH)
    }

    val localizedMoodLabel = localizedMoodName(senseiMood)

    // Sync Sensei mood & costume with 3D scene
    DisposableEffect(senseiMood, senseiCostume, localizedMoodLabel) {
        controller.setMood(senseiMood, localizedMoodLabel)
        controller.setCostume(senseiCostume)
        onDispose {}
    }

    // Audio & Speech Manager
    val speechManager = remember {
        SpeechManager(
            context = context,
            onSpeakStart = { durationMs, text ->
                controller.speak(durationMs, text)
            },
            onSpeakEnd = {
                controller.setMood("idle", context.getString(R.string.mood_idle))
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            speechManager.destroy()
        }
    }

    LaunchedEffect(targetLanguage) {
        speechManager.setLanguage(targetLanguage)
    }

    // Microphone Permission Launcher
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (!isGranted) {
            Toast.makeText(
                context,
                context.getString(R.string.mic_permission_required),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val configuration = LocalConfiguration.current
    val isTv = remember(context) {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
    val isTvOrWide = isTv || (configuration.screenWidthDp >= 840 && configuration.screenWidthDp > configuration.screenHeightDp)

    Scaffold(
        bottomBar = {
            if (!isTvOrWide) {
                SenseiBottomNavigation(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.setTab(it) }
                )
            }
        },
        containerColor = Color(0xFF0F0E26)
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isTvOrWide) {
                SenseiNavigationRail(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.setTab(it) }
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Top App Bar
                TopBar(
                targetLanguage = targetLanguage,
                languages = viewModel.supportedLanguages,
                streak = effectiveStreak,
                practicedToday = dailyStreakState.practicedToday,
                freezesAvailable = dailyStreakState.freezesAvailable,
                freezeSavedStreak = dailyStreakState.freezeSavedStreak,
                isActiveSupporter = isActiveSupporter,
                onSupportClick = {
                    donationInitialChannel = DonationChannel.GOOGLE_PLAY
                    viewModel.trackDonationSheetOpened(DonationChannel.GOOGLE_PLAY)
                    showDonationModal = true
                },
                onStreakClick = {
                    viewModel.trackStreakDialogOpened(effectiveStreak.toInt())
                    showStreakDialog = true
                },
                onLanguageSelected = { lang ->
                    viewModel.setTargetLanguage(lang)
                    speechManager.setLanguage(lang)
                }
            )

                if (isTvOrWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.36f)
                                .fillMaxHeight()
                                .background(Color(0xFF0F0E26))
                        ) {
                            Sensei3DView(
                                controller = controller,
                                modifier = Modifier.fillMaxSize()
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val costumes = listOf("traditional", "crimson", "cyber", "sakura")
                                        val nextIdx = (costumes.indexOf(senseiCostume) + 1) % costumes.size
                                        val nextCostume = costumes[nextIdx]
                                        viewModel.setSenseiCostume(nextCostume)
                                        controller.setCostume(nextCostume)
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF312E81).copy(alpha = 0.85f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = stringResource(R.string.cd_cycle_outfit),
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        controller.praise()
                                        controller.playChime("praise")
                                        speechManager.speak(context.getString(R.string.hud_praise_speech), targetLanguage)
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF312E81).copy(alpha = 0.85f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = stringResource(R.string.cd_praise_sensei),
                                            tint = Color(0xFFF472B6),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(0.64f)
                                .fillMaxHeight()
                                .background(Color(0xFF141233))
                        ) {
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(180)) +
                                        slideInVertically(animationSpec = tween(180)) { it / 24 })
                                        .togetherWith(fadeOut(animationSpec = tween(140)))
                                },
                                label = "tvTabTransition"
                            ) { tab ->
                                when (tab) {
                                    LearningTab.COACH -> CoachScreen(
                                        viewModel = viewModel,
                                        speechManager = speechManager,
                                        hasMicPermission = hasMicPermission,
                                        shouldShowDonationBanner = shouldShowDonationBanner && !isActiveSupporter,
                                        onOpenDonationSheet = {
                                            donationInitialChannel = DonationChannel.GOOGLE_PLAY
                                            viewModel.trackDonationSheetOpened(DonationChannel.GOOGLE_PLAY)
                                            showDonationModal = true
                                        },
                                        onDismissDonationBanner = { viewModel.dismissHomeDonationBanner() },
                                        onRequestMicPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                        onPraiseAndChime = {
                                            controller.praise()
                                            controller.playChime("praise")
                                        }
                                    )
                                    LearningTab.ROLEPLAY -> RoleplayScreen(
                                        viewModel = viewModel,
                                        speechManager = speechManager,
                                        hasMicPermission = hasMicPermission,
                                        onRequestMicPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                                    )
                                    LearningTab.VOCAB -> VocabMasteryScreen(
                                        viewModel = viewModel,
                                        speechManager = speechManager
                                    )
                                    LearningTab.LEAGUE -> LeagueScreen(viewModel = viewModel)
                                    LearningTab.PROFILE -> ProfileScreen(
                                        viewModel = viewModel,
                                        isActiveSupporter = isActiveSupporter,
                                        successfulOperationsCount = successfulOperations,
                                        onOpenDonationSheet = { channel ->
                                            donationInitialChannel = channel
                                            viewModel.trackDonationSheetOpened(channel)
                                            showDonationModal = true
                                        },
                                        onOpenPrivacyPolicy = { showPrivacyPolicyDialog = true },
                                        onReplayOnboarding = onReplayOnboarding,
                                        onSignOut = {
                                            val credentialManager = CredentialManager.create(context)
                                            coroutineScope.launch {
                                                withContext(NonCancellable) {
                                                    try {
                                                        credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                                    } catch (_: Exception) {}
                                                    runCatching { Firebase.auth.signOut() }
                                                    onSignOutComplete()
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Context-aware & User-Collapsible 3D Sensei Stage
                    val autoStageHeight = when (currentTab) {
                LearningTab.COACH -> 220.dp
                LearningTab.ROLEPLAY -> 150.dp
                LearningTab.VOCAB, LearningTab.LEAGUE, LearningTab.PROFILE -> 108.dp
            }
            val effectiveExpanded = manualStageExpanded ?: (currentTab == LearningTab.COACH || currentTab == LearningTab.ROLEPLAY)
            val targetStageHeight = when (manualStageExpanded) {
                true -> 240.dp
                false -> 102.dp
                null -> autoStageHeight
            }
            val animatedStageHeight by animateDpAsState(
                targetValue = targetStageHeight,
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                label = "senseiStageHeight"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedStageHeight)
                    .background(Color(0xFF0F0E26))
            ) {
                Sensei3DView(
                    controller = controller,
                    modifier = Modifier.fillMaxSize()
                )

                // Stage Collapse / Expand HUD Control (Bottom Start)
                IconButton(
                    onClick = { manualStageExpanded = !effectiveExpanded },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .size(48.dp)
                        .testTag("stage_expand_toggle")
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF312E81).copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (effectiveExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = stringResource(R.string.cd_toggle_sensei_stage),
                            tint = Color(0xFFC7D2FE),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Quick Action HUD (Costume, Praise animation, Greet)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = {
                            val costumes = listOf("traditional", "crimson", "cyber", "sakura")
                            val nextIdx = (costumes.indexOf(senseiCostume) + 1) % costumes.size
                            val nextCostume = costumes[nextIdx]
                            viewModel.setSenseiCostume(nextCostume)
                            controller.setCostume(nextCostume)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("costume_cycle_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF312E81).copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = stringResource(R.string.cd_cycle_outfit),
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            controller.praise()
                            controller.playChime("praise")
                            speechManager.speak(context.getString(R.string.hud_praise_speech), targetLanguage)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF312E81).copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = stringResource(R.string.cd_praise_sensei),
                                tint = Color(0xFFF472B6),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            speechManager.speak(context.getString(R.string.hud_greet_speech), targetLanguage, slow = false)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF312E81).copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = stringResource(R.string.cd_sensei_greet),
                                tint = Color(0xFFA5B4FC),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Lower Tab Content with Smooth Animated Transition
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF141233))
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(180)) +
                            slideInVertically(animationSpec = tween(180)) { it / 24 })
                            .togetherWith(fadeOut(animationSpec = tween(140)))
                    },
                    label = "tabTransition"
                ) { tab ->
                    when (tab) {
                        LearningTab.COACH -> CoachScreen(
                            viewModel = viewModel,
                            speechManager = speechManager,
                            hasMicPermission = hasMicPermission,
                            shouldShowDonationBanner = shouldShowDonationBanner && !isActiveSupporter,
                            onOpenDonationSheet = {
                                donationInitialChannel = DonationChannel.GOOGLE_PLAY
                                viewModel.trackDonationSheetOpened(DonationChannel.GOOGLE_PLAY)
                                showDonationModal = true
                            },
                            onDismissDonationBanner = { viewModel.dismissHomeDonationBanner() },
                            onRequestMicPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            onPraiseAndChime = {
                                controller.praise()
                                controller.playChime("praise")
                            }
                        )
                        LearningTab.ROLEPLAY -> RoleplayScreen(
                            viewModel = viewModel,
                            speechManager = speechManager,
                            hasMicPermission = hasMicPermission,
                            onRequestMicPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                        )
                        LearningTab.VOCAB -> VocabMasteryScreen(
                            viewModel = viewModel,
                            speechManager = speechManager
                        )
                        LearningTab.LEAGUE -> LeagueScreen(
                            viewModel = viewModel
                        )
                        LearningTab.PROFILE -> ProfileScreen(
                            viewModel = viewModel,
                            isActiveSupporter = isActiveSupporter,
                            successfulOperationsCount = successfulOperations,
                            onOpenDonationSheet = { channel ->
                                donationInitialChannel = channel
                                viewModel.trackDonationSheetOpened(channel)
                                showDonationModal = true
                            },
                            onOpenPrivacyPolicy = { showPrivacyPolicyDialog = true },
                            onReplayOnboarding = onReplayOnboarding,
                            onSignOut = {
                                val credentialManager = CredentialManager.create(context)
                                coroutineScope.launch {
                                    withContext(NonCancellable) {
                                        try {
                                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                        } catch (_: Exception) {}
                                        runCatching { Firebase.auth.signOut() }
                                        onSignOutComplete()
                                    }
                                }
                            }
                        )
                    }
                }
            }
            }
        }
        }
    }

    activePrompt?.let { prompt ->
        EngagementPromptDialog(
            prompt = prompt,
            onAction = { action -> viewModel.onEngagementPromptAction(prompt, action) }
        )
    }

    if (showDonationModal) {
        DonationSupportModal(
            initialChannel = donationInitialChannel,
            donationUiState = donationUiState,
            onDismiss = {
                viewModel.clearDonationStatus()
                showDonationModal = false
            },
            onLaunchPlayPurchase = { productId ->
                viewModel.launchPlayDonation(context, productId)
            },
            onOpenPayPal = { amount -> viewModel.openPayPalDonation(context, amount) },
            onCopyPayPal = { amount -> viewModel.copyPayPalLink(context, amount) },
            onSharePayPal = { amount -> viewModel.sharePayPalLink(context, amount) },
            onOpenKofi = { viewModel.openKofiDonation(context) },
            onCopyKofi = { viewModel.copyKofiLink(context) },
            onClearStatus = { viewModel.clearDonationStatus() },
            onChannelSelected = { channel -> viewModel.trackDonationChannelSelected(channel) }
        )
    }

    if (showPrivacyPolicyDialog) {
        InAppPrivacyPolicyDialog(
            onDismiss = { showPrivacyPolicyDialog = false }
        )
    }

    if (showStreakDialog) {
        DailyStreakDialog(
            streakCount = effectiveStreak.toInt(),
            bestStreak = maxOf(dailyStreakState.bestStreak, effectiveStreak.toInt()),
            practicedToday = dailyStreakState.practicedToday,
            freezesAvailable = dailyStreakState.freezesAvailable,
            freezeSavedStreak = dailyStreakState.freezeSavedStreak,
            onShareStreak = {
                showStreakDialog = false
                viewModel.shareStreakMilestone(effectiveStreak.toInt())
            },
            onPracticeNow = {
                showStreakDialog = false
                viewModel.setTab(LearningTab.COACH)
            },
            onDismiss = { showStreakDialog = false }
        )
    }
}

@Composable
private fun EngagementPromptDialog(
    prompt: EngagementPrompt,
    onAction: (PromptAction) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onAction(PromptAction.LATER) },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (prompt) {
                        is EngagementPrompt.UpdatePrompt -> Icons.Default.Refresh
                        is EngagementPrompt.RatePrompt -> Icons.Default.Star
                        is EngagementPrompt.SharePrompt -> Icons.Default.Share
                        is EngagementPrompt.DonationPrompt -> Icons.Default.Favorite
                    },
                    contentDescription = null,
                    tint = Color(0xFFFBBF24)
                )
                Text(
                    text = when (prompt) {
                        is EngagementPrompt.UpdatePrompt ->
                            stringResource(R.string.prompt_update_title, prompt.versionName)
                        is EngagementPrompt.RatePrompt ->
                            stringResource(R.string.prompt_rate_title)
                        is EngagementPrompt.SharePrompt ->
                            stringResource(R.string.prompt_share_title)
                        is EngagementPrompt.DonationPrompt ->
                            stringResource(R.string.prompt_donation_title)
                    },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = when (prompt) {
                        is EngagementPrompt.UpdatePrompt -> prompt.releaseNotes.ifBlank {
                            stringResource(R.string.prompt_update_default_notes)
                        }
                        is EngagementPrompt.RatePrompt ->
                            stringResource(
                                R.string.prompt_rate_body,
                                prompt.sessionsCompleted,
                                prompt.milestonesReached
                            )
                        is EngagementPrompt.SharePrompt -> {
                            val scoreNote = prompt.latestScore?.let {
                                stringResource(R.string.prompt_share_score_note, it)
                            }
                            if (scoreNote != null) {
                                stringResource(R.string.prompt_share_body, scoreNote)
                            } else {
                                stringResource(R.string.prompt_share_body_plain)
                            }
                        }
                        is EngagementPrompt.DonationPrompt ->
                            stringResource(R.string.prompt_donation_body, prompt.successfulOperations)
                    },
                    fontSize = 13.sp,
                    color = Color(0xFFC7D2FE)
                )

                val isMandatoryUpdate = (prompt as? EngagementPrompt.UpdatePrompt)?.isMandatory == true
                if (!isMandatoryUpdate) {
                    TextButton(
                        onClick = { onAction(PromptAction.NEVER) },
                        modifier = Modifier.testTag("prompt_never_button")
                    ) {
                        Text(
                            text = stringResource(R.string.prompt_dont_ask_again),
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAction(PromptAction.ACCEPTED) },
                modifier = Modifier.testTag("prompt_accept_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
            ) {
                Text(
                    text = when (prompt) {
                        is EngagementPrompt.UpdatePrompt -> stringResource(R.string.prompt_update_now)
                        is EngagementPrompt.RatePrompt -> stringResource(R.string.prompt_rate_cta)
                        is EngagementPrompt.SharePrompt -> stringResource(R.string.prompt_share_cta)
                        is EngagementPrompt.DonationPrompt -> stringResource(R.string.prompt_donation_cta)
                    },
                    color = Color.White
                )
            }
        },
        dismissButton = {
            val isMandatoryUpdate = (prompt as? EngagementPrompt.UpdatePrompt)?.isMandatory == true
            if (!isMandatoryUpdate) {
                TextButton(
                    onClick = { onAction(PromptAction.LATER) },
                    modifier = Modifier.testTag("prompt_later_button")
                ) {
                    Text(stringResource(R.string.prompt_maybe_later), color = Color(0xFFA5B4FC))
                }
            }
        },
        containerColor = Color(0xFF1E1B4B)
    )
}

@Composable
private fun TopBar(
    targetLanguage: String,
    languages: List<String>,
    streak: Long,
    practicedToday: Boolean,
    freezesAvailable: Int = 0,
    freezeSavedStreak: Boolean = false,
    isActiveSupporter: Boolean,
    onSupportClick: () -> Unit,
    onStreakClick: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title & Language Selector
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E1B4B))
                    .clickable { menuExpanded = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.topbar_brand_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF4F46E5))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${getFlagForLanguage(targetLanguage)} ${localizedLanguageName(targetLanguage)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(Color(0xFF1E1B4B))
            ) {
                languages.forEach { lang ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${getFlagForLanguage(lang)} ${localizedLanguageName(lang)}",
                                color = if (lang == targetLanguage) Color(0xFFA5B4FC) else Color.White,
                                fontWeight = if (lang == targetLanguage) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onLanguageSelected(lang)
                            menuExpanded = false
                        }
                    )
                }
            }
        }

        // Right Action Cluster: Persistent Support/Supporter Action + Interactive Daily Streak Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isActiveSupporter) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF422006))
                        .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(16.dp))
                        .clickable { onSupportClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("header_supporter_badge")
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = stringResource(R.string.cd_active_supporter),
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.supporter_badge_short),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFDE68A)
                    )
                }
            } else {
                IconButton(
                    onClick = onSupportClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("header_support_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = stringResource(R.string.cd_support_app),
                        tint = Color(0xFFF472B6),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HeaderStreakBadge(
                streakCount = streak.toInt(),
                practicedToday = practicedToday,
                freezesAvailable = freezesAvailable,
                freezeSavedStreak = freezeSavedStreak,
                onClick = onStreakClick
            )
        }
    }
}

@Composable
internal fun HeaderStreakBadge(
    streakCount: Int,
    practicedToday: Boolean,
    freezesAvailable: Int = 0,
    freezeSavedStreak: Boolean = false,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "headerStreakPulse")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = if (practicedToday) 0.94f else 0.92f,
        targetValue = if (practicedToday) 1.20f else 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (practicedToday) 640 else 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flameScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = if (practicedToday) 0.22f else 0.08f,
        targetValue = if (practicedToday) 0.58f else 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (practicedToday) 640 else 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val animatedStreakCount by animateIntAsState(
        targetValue = streakCount.coerceAtLeast(0),
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "animatedStreakCount"
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            practicedToday -> Color(0xFF431407)
            freezeSavedStreak -> Color(0xFF0C4A6E)
            streakCount > 0 -> Color(0xFF312E81)
            else -> Color(0xFF1E1B4B)
        },
        animationSpec = tween(280),
        label = "streakBgColor"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            practicedToday -> Color(0xFFF97316)
            freezeSavedStreak -> Color(0xFF38BDF8)
            streakCount > 0 -> Color(0xFFFBBF24).copy(alpha = 0.65f)
            else -> Color(0xFF475569)
        },
        animationSpec = tween(280),
        label = "streakBorderColor"
    )
    val flameTint by animateColorAsState(
        targetValue = when {
            practicedToday -> Color(0xFFF97316)
            freezeSavedStreak -> Color(0xFF38BDF8)
            streakCount > 0 -> Color(0xFFFBBF24)
            else -> Color(0xFF94A3B8)
        },
        animationSpec = tween(280),
        label = "streakFlameTint"
    )

    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("header_streak_badge")
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                flameTint.copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension * 0.62f
                    )
                }
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = stringResource(R.string.cd_streak),
                    tint = flameTint,
                    modifier = Modifier
                        .size(18.dp)
                        .scale(flameScale)
                        .testTag("header_streak_fire_icon")
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.streak_day_format, animatedStreakCount),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (freezesAvailable > 0 || freezeSavedStreak) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.AcUnit,
                    contentDescription = null,
                    tint = Color(0xFF7DD3FC),
                    modifier = Modifier
                        .size(12.dp)
                        .testTag("header_streak_freeze_icon")
                )
            }
        }
    }
}

@Composable
internal fun DailyStreakDialog(
    streakCount: Int,
    bestStreak: Int,
    practicedToday: Boolean,
    freezesAvailable: Int = 0,
    freezeSavedStreak: Boolean = false,
    onShareStreak: (() -> Unit)? = null,
    onPracticeNow: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("daily_streak_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = stringResource(R.string.cd_streak),
                    tint = if (practicedToday) Color(0xFFF97316) else Color(0xFFFBBF24)
                )
                Text(
                    text = stringResource(R.string.streak_dialog_title),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF141233))
                        .border(
                            1.5.dp,
                            if (practicedToday) Color(0xFFF97316) else Color(0xFF4F46E5),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.streak_day_format, streakCount),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (practicedToday) Color(0xFFFDBA74) else Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.streak_best_format, maxOf(bestStreak, streakCount)),
                            fontSize = 12.sp,
                            color = Color(0xFFA5B4FC),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Streak Freeze conservation status pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF0C4A6E).copy(alpha = 0.7f))
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .testTag("streak_freeze_badge"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AcUnit,
                        contentDescription = null,
                        tint = Color(0xFF7DD3FC),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.streak_freeze_count_format, freezesAvailable),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE0F2FE)
                    )
                }

                if (freezeSavedStreak) {
                    Text(
                        text = stringResource(R.string.streak_freeze_saved_banner),
                        fontSize = 11.sp,
                        color = Color(0xFF7DD3FC),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("streak_freeze_saved_text")
                    )
                }

                Text(
                    text = stringResource(
                        if (practicedToday) R.string.streak_status_active_today
                        else R.string.streak_status_pending_today
                    ),
                    fontSize = 13.sp,
                    color = if (practicedToday) Color(0xFF34D399) else Color(0xFFFDE68A),
                    textAlign = TextAlign.Center
                )

                if (streakCount > 0 && onShareStreak != null) {
                    OutlinedButton(
                        onClick = onShareStreak,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("streak_share_card_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF34D399))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.share_card_btn),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = if (practicedToday) onDismiss else onPracticeNow,
                modifier = Modifier.testTag("streak_dialog_primary_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (practicedToday) Color(0xFF4F46E5) else Color(0xFFF97316)
                )
            ) {
                Text(
                    text = stringResource(
                        if (practicedToday) R.string.btn_close else R.string.vocab_btn_practice_coach
                    ),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = if (!practicedToday) {
            {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_close), color = Color(0xFFA5B4FC))
                }
            }
        } else null,
        containerColor = Color(0xFF1E1B4B)
    )
}

@Composable
private fun CoachScreen(
    viewModel: MainViewModel,
    speechManager: SpeechManager,
    hasMicPermission: Boolean,
    shouldShowDonationBanner: Boolean,
    onOpenDonationSheet: () -> Unit,
    onDismissDonationBanner: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onPraiseAndChime: () -> Unit = {}
) {
    val context = LocalContext.current
    val selectedPhrase by viewModel.selectedPhrase.collectAsState()
    val targetLanguage by viewModel.targetLanguage.collectAsState()
    val evalState by viewModel.evaluationState.collectAsState()
    val dailyGoal by viewModel.dailyGoalProgress.collectAsState()
    val dailyStreak by viewModel.dailyStreakState.collectAsState()
    val customPhrasesAll by viewModel.customPhrasesState.collectAsState()
    val aiSettings by viewModel.aiSettingsState.collectAsState()
    val isGeneratingTopicPack by viewModel.isGeneratingTopicPack.collectAsState()
    val hasRecordedAudio by speechManager.hasRecordedAudio.collectAsState()
    val isPlayingRecording by speechManager.isPlayingRecording.collectAsState()
    val isPcmRecording by speechManager.isPcmRecording.collectAsState()
    val isListening by speechManager.isListening.collectAsState()

    val customPhrasesForLang = remember(customPhrasesAll, targetLanguage) {
        customPhrasesAll
            .filter { it.language.equals(targetLanguage, ignoreCase = true) }
            .map { entity ->
                PracticePhrase(
                    id = "custom_phrase_${entity.id}",
                    targetText = entity.targetText,
                    phonetic = entity.phonetic.ifBlank { entity.targetText },
                    translation = entity.translation,
                    difficulty = "Custom",
                    accentFocus = entity.accentFocus.ifBlank {
                        context.getString(R.string.coach_default_accent_focus)
                    },
                    accentFocusRes = if (entity.accentFocus.isBlank()) R.string.coach_default_accent_focus else 0
                )
            }
    }
    val builtInPhrases = remember(targetLanguage) { viewModel.getPhrasesForLanguage(targetLanguage) }
    val phrases = remember(customPhrasesForLang, builtInPhrases) {
        customPhrasesForLang + builtInPhrases
    }

    var manualInputText by remember(selectedPhrase.id) { mutableStateOf("") }
    var showManualDialog by remember { mutableStateOf(false) }
    var showDojoBlitzDialog by remember { mutableStateOf(false) }
    var showCustomPhraseDialog by remember { mutableStateOf(false) }
    var showByokQuickDialog by remember { mutableStateOf(false) }
    var isShadowingActive by remember { mutableStateOf(false) }
    var shadowingSlow by remember { mutableStateOf(false) }
    var shadowingIsListeningPhase by remember { mutableStateOf(true) }
    var shadowingProgress by remember { mutableStateOf(0f) }
    var lastCelebratedGoalCount by remember { mutableStateOf(dailyGoal.first) }
    var lastPracticedToday by remember { mutableStateOf(dailyStreak.practicedToday) }
    var showStreakUnlockedBanner by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            isShadowingActive = false
        }
    }

    LaunchedEffect(isListening, isPcmRecording) {
        if (isListening || isPcmRecording) {
            isShadowingActive = false
        }
    }

    LaunchedEffect(isShadowingActive, shadowingSlow, selectedPhrase.id, phrases.size) {
        if (!isShadowingActive || phrases.isEmpty()) {
            shadowingProgress = 0f
            return@LaunchedEffect
        }
        val activePhrase = selectedPhrase
        shadowingIsListeningPhase = true
        shadowingProgress = 0.08f
        speechManager.speak(activePhrase.targetText, targetLanguage, slow = shadowingSlow)
        val speakDurationMs = ((activePhrase.targetText.length * 75L) * if (shadowingSlow) 1.35f else 1.0f)
            .toLong()
            .coerceIn(1800L, 6500L)
        val speakSteps = 10
        for (i in 1..speakSteps) {
            if (!isShadowingActive) return@LaunchedEffect
            delay(speakDurationMs / speakSteps)
            shadowingProgress = (i.toFloat() / speakSteps) * 0.5f
        }
        shadowingIsListeningPhase = false
        val repeatPauseMs = 3200L
        val repeatSteps = 16
        for (i in 1..repeatSteps) {
            if (!isShadowingActive) return@LaunchedEffect
            delay(repeatPauseMs / repeatSteps)
            shadowingProgress = 0.5f + (i.toFloat() / repeatSteps) * 0.5f
        }
        if (isShadowingActive && phrases.isNotEmpty()) {
            val currentIdx = phrases.indexOfFirst { it.id == activePhrase.id }
            val nextPhrase = phrases[(currentIdx + 1).coerceAtLeast(0) % phrases.size]
            viewModel.selectPhrase(nextPhrase)
        }
    }

    LaunchedEffect(dailyGoal.first) {
        if (dailyGoal.first >= dailyGoal.second && lastCelebratedGoalCount < dailyGoal.second) {
            onPraiseAndChime()
        }
        lastCelebratedGoalCount = dailyGoal.first
    }

    LaunchedEffect(dailyStreak.practicedToday) {
        if (dailyStreak.practicedToday && !lastPracticedToday) {
            showStreakUnlockedBanner = true
            onPraiseAndChime()
        }
        lastPracticedToday = dailyStreak.practicedToday
    }

    LaunchedEffect(evalState) {
        val current = evalState
        if (current is EvaluationUiState.Success && current.result.score >= 85) {
            onPraiseAndChime()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DailyDojoGoalCard(
                doneCount = dailyGoal.first,
                targetCount = dailyGoal.second,
                streakCount = dailyStreak.currentStreak,
                practicedToday = dailyStreak.practicedToday,
                justUnlockedStreak = showStreakUnlockedBanner
            )
        }
        if (shouldShowDonationBanner) {
            item {
                HomeDonationCompactBanner(
                    onSupportClick = onOpenDonationSheet,
                    onDismissClick = onDismissDonationBanner
                )
            }
        }
        // Phrase Selection Carousel + Custom Phrase + Zero-Cost Dojo Blitz Quick Launch
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.coach_practice_phrases),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFA5B4FC)
                )
                OutlinedButton(
                    onClick = { showDojoBlitzDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("dojo_blitz_launch_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24))
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.coach_dojo_blitz_btn),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item(key = "add_custom_phrase_chip") {
                    OutlinedButton(
                        onClick = { showCustomPhraseDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("add_custom_phrase_btn"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF34D399))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.custom_phrase_btn),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                items(phrases, key = { it.id }) { phrase ->
                    val isSelected = phrase.id == selectedPhrase.id
                    val isCustom = phrase.id.startsWith("custom_phrase_")
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectPhrase(phrase) },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                        label = {
                            Text(
                                text = phrase.targetText.take(18) + if (phrase.targetText.length > 18) "..." else "",
                                fontSize = 12.sp
                            )
                        },
                        trailingIcon = if (isCustom && isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.btn_cancel),
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clickable {
                                            phrase.id.removePrefix("custom_phrase_").toIntOrNull()?.let { id ->
                                                viewModel.deleteCustomPhrase(id)
                                            }
                                        }
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4F46E5),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E1B4B),
                            labelColor = Color(0xFFC7D2FE)
                        )
                    )
                }
            }
        }

        // Target Phrase Card with Slow Audio & Focus
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF312E81))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF312E81))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = localizedDifficultyName(selectedPhrase.difficulty),
                                fontSize = 11.sp,
                                color = Color(0xFFA5B4FC),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            // Normal Audio Playback
                            IconButton(
                                onClick = {
                                    speechManager.speak(selectedPhrase.targetText, targetLanguage, slow = false)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF312E81)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = stringResource(R.string.cd_normal_speed),
                                        tint = Color(0xFF818CF8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Slow Speed (0.75x) for accent clarity
                            IconButton(
                                onClick = {
                                    speechManager.speak(selectedPhrase.targetText, targetLanguage, slow = true)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF312E81)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SlowMotionVideo,
                                        contentDescription = stringResource(R.string.cd_slow_speed),
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = selectedPhrase.targetText,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Text(
                        text = selectedPhrase.phonetic,
                        fontSize = 14.sp,
                        color = Color(0xFFA5B4FC),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Text(
                        text = if (selectedPhrase.translationRes != 0) {
                            stringResource(selectedPhrase.translationRes)
                        } else {
                            selectedPhrase.translation
                        },
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val focusLabel = if (selectedPhrase.accentFocusRes != 0) {
                            stringResource(selectedPhrase.accentFocusRes)
                        } else {
                            selectedPhrase.accentFocus
                        }
                        Text(
                            text = stringResource(R.string.coach_focus_format, focusLabel),
                            fontSize = 12.sp,
                            color = Color(0xFFFCD34D)
                        )
                    }
                }
            }
        }

        // Hands-Free Commuter Shadowing Loop
        item {
            ShadowingModeControlBar(
                isActive = isShadowingActive,
                isSlow = shadowingSlow,
                stepLabel = stringResource(
                    if (shadowingIsListeningPhase) R.string.shadowing_step_listening
                    else R.string.shadowing_step_repeat
                ),
                stepProgress = shadowingProgress,
                onToggleActive = {
                    if (isShadowingActive) {
                        isShadowingActive = false
                        speechManager.stopSpeaking()
                    } else {
                        isShadowingActive = true
                    }
                },
                onToggleSlow = { shadowingSlow = !shadowingSlow }
            )
        }

        // Dedicated Pronunciation Coach Component: Live Canvas Waveform Visualizer & Audio Capture
        item {
            PronunciationCoachPanel(
                speechManager = speechManager,
                targetLanguage = targetLanguage,
                targetPhrase = selectedPhrase.targetText,
                hasMicPermission = hasMicPermission,
                onRequestMicPermission = onRequestMicPermission,
                onOpenManualInput = { showManualDialog = true },
                onTranscriptReady = { resultText, contour ->
                    viewModel.evaluateSpeech(selectedPhrase.targetText, resultText, contour)
                }
            )
        }

        // Evaluation Results Pane with Animated Entrance
        item {
            AnimatedVisibility(
                visible = evalState !is EvaluationUiState.Idle,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                when (evalState) {
                    is EvaluationUiState.Evaluating -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF818CF8),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.coach_evaluating),
                                    color = Color(0xFFA5B4FC),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    is EvaluationUiState.Success -> {
                        val result = (evalState as EvaluationUiState.Success).result
                        val showPostOpSupport = remember(result) {
                            viewModel.canShowPostOperationSupportCard(result.score)
                        }
                        PronunciationResultCard(
                            result = result,
                            hasRecordedAudio = hasRecordedAudio,
                            isPlayingRecording = isPlayingRecording,
                            onPlayMine = { speechManager.togglePlayRecordedAudio() },
                            onPlaySensei = { speechManager.speak(selectedPhrase.targetText, targetLanguage, false) },
                            onShareCard = {
                                viewModel.sharePronunciationMilestone(
                                    phraseText = selectedPhrase.targetText,
                                    phoneticText = selectedPhrase.phonetic,
                                    score = result.score
                                )
                            },
                            showSupportMicroFooter = showPostOpSupport,
                            onSupportClick = onOpenDonationSheet,
                            onUnlockByokClick = if (!aiSettings.hasByokKey) {
                                { showByokQuickDialog = true }
                            } else null,
                            onSave = {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.toast_saved_vocab_firestore),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                    is EvaluationUiState.Error -> {
                        val msg = (evalState as EvaluationUiState.Error).message
                        Text(
                            text = stringResource(R.string.coach_eval_note_format, msg),
                            color = Color(0xFFF87171),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    EvaluationUiState.Idle -> {}
                }
            }
        }
    }

    // Manual input dialog
    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = { Text(stringResource(R.string.manual_dialog_title), color = Color.White) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.manual_dialog_desc),
                        fontSize = 12.sp,
                        color = Color(0xFFA5B4FC)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualInputText,
                        onValueChange = { manualInputText = it },
                        placeholder = { Text(selectedPhrase.targetText) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showManualDialog = false
                        val toSend = manualInputText.ifBlank { selectedPhrase.targetText }
                        manualInputText = ""
                        viewModel.evaluateSpeech(selectedPhrase.targetText, toSend, speechManager.lastUserPitchContour.value)
                    }
                ) {
                    Text(stringResource(R.string.btn_analyze))
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
            containerColor = Color(0xFF1E1B4B)
        )
    }

    if (showCustomPhraseDialog) {
        CustomPhraseDialog(
            language = targetLanguage,
            isGeneratingTopicPack = isGeneratingTopicPack,
            onGenerateTopicPack = { topic ->
                viewModel.generateTopicLessonPack(topic) {
                    showCustomPhraseDialog = false
                }
            },
            onSave = { target, phonetic, translation, focus ->
                viewModel.addCustomPhrase(targetLanguage, target, phonetic, translation, focus)
                showCustomPhraseDialog = false
            },
            onDismiss = { showCustomPhraseDialog = false }
        )
    }

    if (showByokQuickDialog) {
        ByokQuickSetupDialog(
            onSaveByokKey = { key ->
                viewModel.saveByokApiKey(key)
            },
            onDismiss = { showByokQuickDialog = false }
        )
    }

    if (showDojoBlitzDialog) {
        DojoBlitzGameDialog(
            language = targetLanguage,
            phrases = phrases,
            speechManager = speechManager,
            hasMicPermission = hasMicPermission,
            onRequestMicPermission = onRequestMicPermission,
            onPraiseSensei = onPraiseAndChime,
            onGameFinished = { pointsEarned, phrasesCleared ->
                viewModel.completeDojoBlitzSession(pointsEarned, phrasesCleared)
            },
            onDismiss = { showDojoBlitzDialog = false }
        )
    }
}

@Composable
private fun PronunciationResultCard(
    result: PronunciationResult,
    hasRecordedAudio: Boolean = false,
    isPlayingRecording: Boolean = false,
    onPlayMine: () -> Unit = {},
    onPlaySensei: () -> Unit = {},
    onShareCard: () -> Unit = {},
    showSupportMicroFooter: Boolean = false,
    onSupportClick: () -> Unit = {},
    onUnlockByokClick: (() -> Unit)? = null,
    onSave: () -> Unit
) {
    val scoreColor = when {
        result.score >= 85 -> Color(0xFF34D399) // Emerald
        result.score >= 70 -> Color(0xFFFBBF24) // Amber
        else -> Color(0xFFF87171) // Rose
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, scoreColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Score Badge & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.result_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = result.encouragement,
                        fontSize = 12.sp,
                        color = Color(0xFFA5B4FC)
                    )
                }

                val animatedScore by animateIntAsState(
                    targetValue = result.score.coerceIn(0, 100),
                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                    label = "scoreCounter"
                )
                val animatedSweep by animateFloatAsState(
                    targetValue = (result.score.coerceIn(0, 100) / 100f) * 360f,
                    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                    label = "scoreArc"
                )

                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokePx = 4.dp.toPx()
                        drawArc(
                            color = scoreColor.copy(alpha = 0.2f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = scoreColor,
                            startAngle = -90f,
                            sweepAngle = animatedSweep,
                            useCenter = false,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$animatedScore",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = scoreColor
                        )
                        Text(
                            text = stringResource(R.string.result_score_denominator),
                            fontSize = 9.sp,
                            color = scoreColor
                        )
                    }
                }
            }

            // Metric Breakdown Bars
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ScoreBar(stringResource(R.string.result_phonetic_precision), result.phoneticAccuracy)
                ScoreBar(stringResource(R.string.result_pitch_accent), result.intonationPitch)
                ScoreBar(stringResource(R.string.result_fluency_rhythm), result.fluency)
            }

            // Pitch Accent & Intonation Contour Graph
            PitchContourVisualizer(result = result)

            // Physical Tongue & Mouth Placement Coaching
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF312E81)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = stringResource(R.string.cd_mouth_coach),
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.result_placement_tip_title),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCD34D)
                        )
                        Text(
                            text = result.coachingTip,
                            fontSize = 12.sp,
                            color = Color.White,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Mispronounced parts if any
            if (result.mispronouncedParts.isNotEmpty()) {
                Column {
                    Text(
                        text = stringResource(R.string.result_phonemes_to_refine),
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        result.mispronouncedParts.forEach { part ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.25f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = part,
                                    fontSize = 11.sp,
                                    color = Color(0xFFFCA5A5),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Native Phrasings
            if (result.nativePhrasings.isNotEmpty()) {
                Column {
                    Text(
                        text = stringResource(R.string.result_native_phrasings),
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    result.nativePhrasings.forEach { phrase ->
                        Text(
                            text = "• $phrase",
                            fontSize = 12.sp,
                            color = Color(0xFFC7D2FE)
                        )
                    }
                }
            }

            // Audio A/B Comparison & Visual Milestone Share Card Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasRecordedAudio) {
                    OutlinedButton(
                        onClick = onPlayMine,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("result_play_mine_btn"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24))
                    ) {
                        Icon(
                            imageVector = if (isPlayingRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                if (isPlayingRecording) R.string.coach_stop_mine else R.string.coach_play_mine
                            ),
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                OutlinedButton(
                    onClick = onPlaySensei,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("result_play_sensei_btn"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA5B4FC))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.result_play_sensei),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onShareCard,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("share_milestone_card_btn"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF34D399))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.share_card_btn),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            // Engine source + Saved confirmation chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF312E81))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("eval_engine_source_badge")
                ) {
                    Text(
                        text = localizedEngineSource(result.engineSource),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFDE68A)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF34D399).copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.cd_synced),
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.result_saved_to_vocab),
                        fontSize = 11.sp,
                        color = Color(0xFF34D399)
                    )
                }
            }

            if (onUnlockByokClick != null &&
                (result.engineSource.contains("Daily cap", ignoreCase = true) ||
                    result.engineSource.contains("Offline", ignoreCase = true) ||
                    result.engineSource.contains("On-Device", ignoreCase = true))
            ) {
                OutlinedButton(
                    onClick = onUnlockByokClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("result_unlock_byok_btn"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFDE68A))
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.ai_byok_add_prompt),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showSupportMicroFooter) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF312E81).copy(alpha = 0.65f))
                        .clickable { onSupportClick() }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("post_op_support_footer"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.result_support_micro_footer),
                        fontSize = 11.sp,
                        color = Color(0xFFC7D2FE),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.result_support_cta_arrow),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF472B6)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreBar(label: String, score: Int) {
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(600, easing = FastOutSlowInEasing)
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 11.sp, color = Color(0xFFC7D2FE))
            Text(
                text = stringResource(R.string.percent_format, score),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF818CF8),
            trackColor = Color(0xFF312E81)
        )
    }
}

@Composable
private fun PitchContourVisualizer(result: PronunciationResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141233)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF312E81))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.pitch_comparison_title),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (result.hasMeasuredAudioContour) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF064E3B))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                .testTag("live_f0_badge")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = stringResource(R.string.pitch_live_mic_badge),
                                fontSize = 9.sp,
                                color = Color(0xFF6EE7B7),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF312E81))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = result.pitchContourType,
                            fontSize = 10.sp,
                            color = Color(0xFFA5B4FC),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
            ) {
                val w = size.width
                val h = size.height
                val gridColor = Color(0xFF312E81)

                // Horizontal pitch guide lines (Low / Mid / High)
                listOf(0.25f, 0.5f, 0.75f).forEach { ratio ->
                    drawLine(
                        color = gridColor.copy(alpha = 0.7f),
                        start = Offset(0f, h * ratio),
                        end = Offset(w, h * ratio),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                fun pointsToOffsets(points: List<Int>): List<Offset> {
                    val safe = if (points.size >= 2) points else listOf(50, 75, 60, 45)
                    return safe.mapIndexed { idx, value ->
                        val x = (idx.toFloat() / (safe.size - 1).coerceAtLeast(1)) * (w - 24f) + 12f
                        val normalizedY = 1f - (value.coerceIn(10, 95) / 100f)
                        val y = (normalizedY * (h - 20f)) + 10f
                        Offset(x, y)
                    }
                }

                fun buildSmoothPath(offsets: List<Offset>): Path {
                    val path = Path()
                    if (offsets.isEmpty()) return path
                    path.moveTo(offsets.first().x, offsets.first().y)
                    for (i in 0 until offsets.size - 1) {
                        val p0 = offsets[i]
                        val p1 = offsets[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                    return path
                }

                val targetOffsets = pointsToOffsets(result.targetPitchPoints)
                val learnerOffsets = pointsToOffsets(result.learnerPitchPoints)

                // Native Target Curve (Emerald)
                drawPath(
                    path = buildSmoothPath(targetOffsets),
                    color = Color(0xFF34D399),
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Learner Spoken Curve (Amber Dashed)
                drawPath(
                    path = buildSmoothPath(learnerOffsets),
                    color = Color(0xFFFBBF24),
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                )

                targetOffsets.forEach { pt ->
                    drawCircle(color = Color(0xFF34D399), radius = 4.dp.toPx(), center = pt)
                }
                learnerOffsets.forEach { pt ->
                    drawCircle(color = Color(0xFFFBBF24), radius = 3.5.dp.toPx(), center = pt)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34D399))
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.pitch_legend_native),
                        fontSize = 10.sp,
                        color = Color(0xFF34D399)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFBBF24))
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.pitch_legend_yours),
                        fontSize = 10.sp,
                        color = Color(0xFFFBBF24)
                    )
                }
            }

            if (result.pitchGuidance.isNotBlank()) {
                Text(
                    text = result.pitchGuidance,
                    fontSize = 11.sp,
                    color = Color(0xFFC7D2FE),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun RoleplayScreen(
    viewModel: MainViewModel,
    speechManager: SpeechManager,
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit
) {
    val selectedScenario by viewModel.selectedScenario.collectAsState()
    val dialogMessages by viewModel.dialogMessages.collectAsState()
    val suggestedReplies by viewModel.suggestedReplies.collectAsState()
    val roleplayState by viewModel.roleplayUiState.collectAsState()
    val isListening by speechManager.isListening.collectAsState()
    val speakingEpoch by speechManager.speakingEpoch.collectAsState()
    val targetLanguage by viewModel.targetLanguage.collectAsState()
    val customScenarios by viewModel.customScenariosState.collectAsState()
    val isGeneratingTopicPack by viewModel.isGeneratingTopicPack.collectAsState()

    val mappedCustomScenarios = remember(customScenarios) {
        customScenarios.map { entity ->
            RoleplayScenario(
                id = "custom_sc_${entity.id}",
                title = entity.title,
                language = entity.language,
                description = entity.title,
                senseiRole = entity.senseiRole,
                userRole = entity.userRole,
                initialMessage = entity.openingLine,
                initialPhonetic = "",
                initialTranslation = ""
            )
        }
    }
    val allScenarios = remember(mappedCustomScenarios) {
        mappedCustomScenarios + MainViewModel.defaultScenarios
    }

    var manualInput by remember(selectedScenario.id) { mutableStateOf("") }
    var showCreateScenarioDialog by remember { mutableStateOf(false) }
    var isLiveVoiceCallActive by remember { mutableStateOf(false) }
    var lastSpokenMsgId by remember(selectedScenario.id) { mutableStateOf("") }
    val listState = rememberLazyListState()

    DisposableEffect(Unit) {
        onDispose {
            isLiveVoiceCallActive = false
        }
    }

    // Auto-scroll to bottom of conversation
    LaunchedEffect(dialogMessages.size) {
        if (dialogMessages.isNotEmpty()) {
            listState.animateScrollToItem(dialogMessages.size - 1)
        }
    }

    // Hands-Free Live Voice Call: Auto-speak Sensei's latest reply
    LaunchedEffect(dialogMessages.size, isLiveVoiceCallActive) {
        if (!isLiveVoiceCallActive || dialogMessages.isEmpty()) return@LaunchedEffect
        val latest = dialogMessages.last()
        if (!latest.isUser && latest.id != lastSpokenMsgId) {
            lastSpokenMsgId = latest.id
            speechManager.speak(latest.text, selectedScenario.language, slow = false)
        }
    }

    // Hands-Free Live Voice Call: Auto-start listening after Sensei finishes speaking
    LaunchedEffect(speakingEpoch, isLiveVoiceCallActive) {
        if (!isLiveVoiceCallActive || speakingEpoch == 0L) return@LaunchedEffect
        delay(650L)
        if (isLiveVoiceCallActive && hasMicPermission && !isListening && roleplayState !is RoleplayUiState.Thinking) {
            speechManager.startListening(selectedScenario.language) { spoken ->
                viewModel.sendRoleplayMessage(spoken)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Scenario Selection Carousel with Language Flags, Live Voice Call Toggle & Custom Scenario Creator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.roleplay_scenarios_title),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFA5B4FC)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = isLiveVoiceCallActive,
                    onClick = {
                        if (!hasMicPermission && !isLiveVoiceCallActive) {
                            onRequestMicPermission()
                        } else {
                            isLiveVoiceCallActive = !isLiveVoiceCallActive
                            if (!isLiveVoiceCallActive) {
                                speechManager.stopSpeaking()
                                speechManager.stopListening()
                            }
                        }
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("roleplay_live_voice_toggle"),
                    leadingIcon = {
                        Icon(
                            imageVector = if (isLiveVoiceCallActive) Icons.Default.GraphicEq else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(
                                if (isLiveVoiceCallActive) R.string.roleplay_live_call_active else R.string.roleplay_live_call_start
                            ),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0284C7),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1E1B4B),
                        labelColor = Color(0xFF7DD3FC)
                    )
                )
                OutlinedButton(
                    onClick = { showCreateScenarioDialog = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("create_custom_scenario_btn"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF34D399))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.custom_scenario_btn),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(allScenarios, key = { it.id }) { scenario ->
                val isSelected = scenario.id == selectedScenario.id
                val isCustom = scenario.id.startsWith("custom_sc_")
                val scenarioTitle = if (scenario.titleRes != 0) {
                    stringResource(scenario.titleRes)
                } else {
                    scenario.title
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectScenario(scenario) },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    label = { Text("${getFlagForLanguage(scenario.language)} $scenarioTitle", fontSize = 12.sp) },
                    trailingIcon = if (isCustom && isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.btn_cancel),
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable {
                                        scenario.id.removePrefix("custom_sc_").toIntOrNull()?.let { id ->
                                            viewModel.deleteCustomScenario(id)
                                        }
                                    }
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF4F46E5),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1E1B4B),
                        labelColor = Color(0xFFC7D2FE)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dialog Bubbles List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(dialogMessages, key = { it.id }) { msg ->
                DialogBubble(
                    message = msg,
                    onPlayAudio = { slow ->
                        speechManager.speak(msg.text, selectedScenario.language, slow = slow)
                    }
                )
            }

            if (roleplayState is RoleplayUiState.Thinking) {
                item {
                    val senseiRoleLabel = if (selectedScenario.senseiRoleRes != 0) {
                        stringResource(selectedScenario.senseiRoleRes)
                    } else {
                        selectedScenario.senseiRole
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF818CF8),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.roleplay_thinking, senseiRoleLabel),
                            fontSize = 12.sp,
                            color = Color(0xFFA5B4FC)
                        )
                    }
                }
            } else if (roleplayState is RoleplayUiState.Error) {
                item {
                    val errorMsg = (roleplayState as RoleplayUiState.Error).message
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("roleplay_error_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3F1D2B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF87171))
                    ) {
                        Text(
                            text = errorMsg,
                            color = Color(0xFFFCA5A5),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Suggested Reply Quick Chips
        if (suggestedReplies.isNotEmpty()) {
            Column {
                Text(
                    text = stringResource(R.string.roleplay_suggested_label),
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(suggestedReplies, key = { it }) { reply ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF312E81))
                                .clickable {
                                    viewModel.sendRoleplayMessage(reply)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "💬 $reply",
                                fontSize = 11.sp,
                                color = Color(0xFFC7D2FE)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Speaking / Sending input bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = manualInput,
                onValueChange = { manualInput = it },
                placeholder = {
                    Text(
                        text = if (isListening) {
                            stringResource(R.string.roleplay_listening_placeholder)
                        } else {
                            stringResource(
                                R.string.roleplay_input_placeholder,
                                localizedLanguageName(selectedScenario.language)
                            )
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1E1B4B),
                    unfocusedContainerColor = Color(0xFF1E1B4B)
                )
            )

            // Microphone trigger
            IconButton(
                onClick = {
                    if (!hasMicPermission) {
                        onRequestMicPermission()
                    } else if (isListening) {
                        speechManager.stopListening()
                    } else {
                        speechManager.startListening(selectedScenario.language) { spoken ->
                            viewModel.sendRoleplayMessage(spoken)
                        }
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isListening) Color(0xFFEF4444) else Color(0xFF4F46E5))
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = stringResource(R.string.cd_speak_to_sensei),
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Send typed message
            if (manualInput.isNotBlank()) {
                IconButton(
                    onClick = {
                        val text = manualInput
                        manualInput = ""
                        viewModel.sendRoleplayMessage(text)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4F46E5))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.cd_send),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showCreateScenarioDialog) {
        CustomScenarioBuilderDialog(
            language = targetLanguage,
            isGeneratingTopicPack = isGeneratingTopicPack,
            onGenerateTopicPack = { topic ->
                viewModel.generateTopicLessonPack(topic) {
                    showCreateScenarioDialog = false
                }
            },
            onSave = { title, senseiRole, userRole, openingLine ->
                viewModel.addCustomScenario(
                    title = title,
                    language = targetLanguage,
                    senseiRole = senseiRole,
                    userRole = userRole,
                    openingLine = openingLine
                )
                showCreateScenarioDialog = false
            },
            onDismiss = { showCreateScenarioDialog = false }
        )
    }
}

@Composable
private fun DialogBubble(
    message: DialogMessage,
    onPlayAudio: (slow: Boolean) -> Unit
) {
    val isUser = message.isUser
    var showTranslation by remember(message.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Text(
            text = if (message.speakerRes != 0) stringResource(message.speakerRes) else message.speaker,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isUser) Color(0xFF818CF8) else Color(0xFFA5B4FC),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Color(0xFF4338CA) else Color(0xFF1E1B4B)
            ),
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )

                    if (!isUser) {
                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            IconButton(
                                onClick = { onPlayAudio(false) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = stringResource(R.string.cd_normal_voice),
                                    tint = Color(0xFFA5B4FC),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { onPlayAudio(true) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SlowMotionVideo,
                                    contentDescription = stringResource(R.string.cd_slow_voice),
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (message.phonetic.isNotBlank()) {
                    Text(
                        text = message.phonetic,
                        fontSize = 12.sp,
                        color = Color(0xFFA5B4FC),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                val resolvedTranslation = if (message.translationRes != 0) {
                    stringResource(message.translationRes)
                } else {
                    message.translation
                }
                if (resolvedTranslation.isNotBlank()) {
                    TextButton(
                        onClick = { showTranslation = !showTranslation },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text(
                            text = if (showTranslation) {
                                stringResource(R.string.roleplay_hide_translation)
                            } else {
                                stringResource(R.string.roleplay_show_translation)
                            },
                            fontSize = 11.sp,
                            color = Color(0xFFFBBF24)
                        )
                    }

                    AnimatedVisibility(visible = showTranslation) {
                        Text(
                            text = resolvedTranslation,
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }

                if (message.accentNote.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF312E81))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.roleplay_sensei_note, message.accentNote),
                            fontSize = 11.sp,
                            color = Color(0xFFC7D2FE)
                        )
                    }
                }
            }
        }
    }
}

// Vocab tab implemented in VocabMasteryScreen.kt using Room VocabularyWord entity.

@Composable
private fun ProfileScreen(
    viewModel: MainViewModel,
    isActiveSupporter: Boolean,
    successfulOperationsCount: Int,
    onOpenDonationSheet: (DonationChannel) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onSignOut: () -> Unit
) {
    val profile by viewModel.profileState.collectAsState()
    val sessions by viewModel.sessionsListState.collectAsState()
    val dailyStreakState by viewModel.dailyStreakState.collectAsState()
    val effectiveStreakCount = if (dailyStreakState.lastPracticeDay != null) {
        dailyStreakState.currentStreak
    } else {
        (profile?.currentStreak ?: 0L).toInt()
    }
    val user = runCatching { Firebase.auth.currentUser }.getOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // User Profile Hero Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF312E81))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val safeDisplayName = user?.displayName?.takeIf { it.isNotBlank() }
                    val safeEmail = user?.email?.takeIf { it.isNotBlank() }
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF6366F1), Color(0xFFA855F7))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (safeDisplayName ?: "S").take(1).uppercase(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = safeDisplayName ?: stringResource(R.string.profile_default_name),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = safeEmail ?: if (user != null) {
                                stringResource(R.string.profile_signed_in_google)
                            } else {
                                stringResource(R.string.profile_guest_mode)
                            },
                            fontSize = 12.sp,
                            color = Color(0xFFA5B4FC)
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF312E81))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.profile_tier_badge),
                                fontSize = 10.sp,
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Sensei 3D Avatar Wardrobe Selector
        item {
            val activeCostume by viewModel.senseiCostume.collectAsState()
            val costumes = listOf(
                "traditional" to stringResource(R.string.costume_traditional),
                "crimson" to stringResource(R.string.costume_crimson),
                "cyber" to stringResource(R.string.costume_cyber),
                "sakura" to stringResource(R.string.costume_sakura)
            )
            Column {
                Text(
                    text = stringResource(R.string.profile_outfit_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(costumes) { (id, label) ->
                        FilterChip(
                            selected = activeCostume == id,
                            onClick = { viewModel.setSenseiCostume(id) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF4F46E5),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E1B4B),
                                labelColor = Color(0xFFC7D2FE)
                            )
                        )
                    }
                }
            }
        }

        // Stats Grid
        item {
            Text(
                text = stringResource(R.string.profile_stats_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.profile_stat_streak),
                    value = if (effectiveStreakCount == 1) {
                        stringResource(R.string.streak_day_format, effectiveStreakCount)
                    } else {
                        stringResource(R.string.profile_stat_streak_value, effectiveStreakCount)
                    },
                    icon = Icons.Default.LocalFireDepartment,
                    color = Color(0xFFF97316)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.profile_stat_mastered),
                    value = "${profile?.wordsMastered ?: 0}",
                    icon = Icons.Default.EmojiEvents,
                    color = Color(0xFFFBBF24)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.profile_stat_practice_time),
                    value = stringResource(
                        R.string.profile_stat_practice_value,
                        (profile?.totalMinutesPracticed ?: 5L).toInt()
                    ),
                    icon = Icons.Default.School,
                    color = Color(0xFF38BDF8)
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.profile_stat_avg_accuracy),
                    value = stringResource(R.string.percent_format, (profile?.avgPronunciationScore ?: 85L).toInt()),
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF34D399)
                )
            }
        }

        // Recent Sessions
        item {
            Text(
                text = stringResource(R.string.profile_recent_sessions_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (sessions.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.profile_no_sessions),
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        } else {
            items(sessions.take(5)) { sess ->
                SessionCard(session = sess)
            }
        }

        // Local Room Cache & App Event Telemetry Summary
        item {
            val localWords by viewModel.localVocabularyState.collectAsState()
            val recentEvents by viewModel.recentAppEvents.collectAsState()
            val totalEventCount by viewModel.totalAppEventCount.collectAsState()

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF312E81))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.profile_telemetry_title),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(
                                R.string.profile_telemetry_counts,
                                localWords.size,
                                totalEventCount
                            ),
                            fontSize = 11.sp,
                            color = Color(0xFF34D399),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    recentEvents.take(3).forEach { ev ->
                        Text(
                            text = "• [${ev.category}] ${ev.eventName}",
                            fontSize = 11.sp,
                            color = Color(0xFFA5B4FC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.shareAppManually() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_app_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA5B4FC))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.profile_btn_share_app), fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.rateAppManually() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rate_app_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24))
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.profile_btn_rate_app), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Zero-Cost AI Coaching Engine & Personal BYOK Key Settings
        item {
            val aiSettings by viewModel.aiSettingsState.collectAsState()
            val cachedEvalCount by viewModel.cachedEvalCount.collectAsState()

            AiModeSettingsCard(
                settings = aiSettings,
                cachedEvalCount = cachedEvalCount,
                onModeSelected = { mode -> viewModel.setAiCoachingMode(mode) },
                onSaveByokKey = { key -> viewModel.saveByokApiKey(key) }
            )
        }

        // Opt-In Daily Evening Streak Reminder (WorkManager)
        item {
            val context = LocalContext.current
            val reminderEnabled by viewModel.streakReminderEnabled.collectAsState()
            val reminderHour by viewModel.streakReminderHour.collectAsState()
            val notifPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    viewModel.setStreakReminderEnabled(true, reminderHour)
                } else {
                    viewModel.setStreakReminderEnabled(false, reminderHour)
                    Toast.makeText(
                        context,
                        context.getString(R.string.reminder_permission_denied),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            StreakReminderSettingsCard(
                enabled = reminderEnabled,
                reminderHour = reminderHour,
                onToggle = { wantEnabled ->
                    if (!wantEnabled) {
                        viewModel.setStreakReminderEnabled(false, reminderHour)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            viewModel.setStreakReminderEnabled(true, reminderHour)
                        } else {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        viewModel.setStreakReminderEnabled(true, reminderHour)
                    }
                },
                onSelectHour = { hour ->
                    viewModel.setStreakReminderEnabled(true, hour)
                }
            )
        }

        // Static Support Development & Privacy Section
        item {
            ProfileSupportSection(
                isActiveSupporter = isActiveSupporter,
                successfulOperationsCount = successfulOperationsCount,
                onOpenDonationSheet = onOpenDonationSheet,
                onOpenPrivacyPolicy = onOpenPrivacyPolicy
            )
        }

        // Interactive App Tour Replay & Sign Out
        item {
            OutlinedButton(
                onClick = onReplayOnboarding,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("replay_onboarding_button"),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4F46E5)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC7D2FE))
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.profile_btn_replay_onboarding))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF312E81))
            ) {
                Text(stringResource(R.string.profile_btn_sign_out), color = Color(0xFFFCA5A5))
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF312E81))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(text = title, fontSize = 11.sp, color = Color(0xFFA5B4FC))
        }
    }
}

@Composable
private fun SessionCard(session: PracticeSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF312E81))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = localizedScenarioTitle(session.scenarioTitle), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Text(
                    text = stringResource(R.string.session_accuracy_format, session.avgScore),
                    color = Color(0xFF34D399),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = stringResource(
                    R.string.session_meta_format,
                    localizedLanguageName(session.language),
                    session.turnsCount
                ),
                fontSize = 11.sp,
                color = Color(0xFFA5B4FC),
                modifier = Modifier.padding(top = 2.dp)
            )
            if (session.feedbackSummary.isNotBlank()) {
                Text(
                    text = "💡 ${session.feedbackSummary}",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun LeagueScreen(viewModel: MainViewModel) {
    val leaderboard by viewModel.leaderboardState.collectAsState()
    val profile by viewModel.profileState.collectAsState()
    val filter by viewModel.leaderboardFilter.collectAsState()
    val currentUid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull() ?: "guest_local"

    val filteredEntries = remember(leaderboard, filter) {
        if (filter == "All") leaderboard
        else leaderboard.filter { it.targetLanguage.equals(filter, ignoreCase = true) }
    }

    val myEntry = leaderboard.firstOrNull { it.userId == currentUid }
    val myXp = myEntry?.totalXp ?: ((profile?.wordsMastered ?: 0L) * 50L + (profile?.totalMinutesPracticed ?: 5L) * 20L)
    val myTier = when {
        myXp >= 2500 -> stringResource(R.string.tier_black_belt)
        myXp >= 1500 -> stringResource(R.string.tier_gold)
        myXp >= 800 -> stringResource(R.string.tier_silver)
        myXp >= 300 -> stringResource(R.string.tier_bronze)
        else -> stringResource(R.string.tier_white_belt)
    }
    val nextTierXp = when {
        myXp >= 2500 -> 2500L
        myXp >= 1500 -> 2500L
        myXp >= 800 -> 1500L
        myXp >= 300 -> 800L
        else -> 300L
    }
    val progress = (myXp.toFloat() / nextTierXp.toFloat()).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4F46E5))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.league_title),
                                fontSize = 12.sp,
                                color = Color(0xFFA5B4FC),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "🥋 $myTier",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFBBF24)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF312E81))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.league_xp_badge, myXp.toInt()),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF34D399)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFFBBF24),
                        trackColor = Color(0xFF312E81)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.league_rank_up_hint),
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        item {
            val filters = listOf("All", "Japanese", "Spanish", "French", "German", "Korean", "Mandarin", "Italian", "English")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filters) { lang ->
                    FilterChip(
                        selected = filter == lang,
                        onClick = { viewModel.setLeaderboardFilter(lang) },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                        label = { Text(localizedLanguageName(lang), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4F46E5),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E1B4B),
                            labelColor = Color(0xFFC7D2FE)
                        )
                    )
                }
            }
        }

        if (filteredEntries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.league_empty_title, localizedLanguageName(filter)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.league_empty_subtitle),
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            itemsIndexed(
                items = filteredEntries,
                key = { _, entry -> "${entry.userId}_${entry.targetLanguage}" }
            ) { index, entry ->
                LeaderboardRowCard(
                    rank = index + 1,
                    entry = entry,
                    isCurrentUser = entry.userId == currentUid
                )
            }
        }
    }
}

@Composable
internal fun DailyDojoGoalCard(
    doneCount: Int,
    targetCount: Int,
    streakCount: Int = 0,
    practicedToday: Boolean = false,
    justUnlockedStreak: Boolean = false
) {
    val safeTarget = targetCount.coerceAtLeast(1)
    val clampedProgress by animateFloatAsState(
        targetValue = (doneCount.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "dailyGoalProgress"
    )
    val isMet = doneCount >= safeTarget
    val borderColor by animateColorAsState(
        targetValue = if (isMet) Color(0xFF34D399) else Color(0xFF312E81),
        label = "dailyGoalBorder"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_goal_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF191642)),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = if (isMet) Color(0xFF34D399) else Color(0xFFFBBF24),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.daily_goal_title),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (practicedToday) Color(0xFF431407) else Color(0xFF312E81))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                            .testTag("coach_streak_chip"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = stringResource(R.string.cd_streak),
                            tint = if (practicedToday) Color(0xFFF97316) else Color(0xFFA5B4FC),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = stringResource(R.string.streak_day_format, streakCount),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (practicedToday) Color(0xFFFDBA74) else Color(0xFFC7D2FE)
                        )
                    }

                    if (isMet) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFF064E3B))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.daily_goal_completed_badge),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399)
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.daily_goal_progress_format, doneCount, safeTarget),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFA5B4FC)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = justUnlockedStreak && streakCount > 0) {
                Text(
                    text = stringResource(R.string.streak_new_day_banner_format, streakCount),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDBA74),
                    modifier = Modifier.testTag("coach_streak_unlocked_banner")
                )
            }

            LinearProgressIndicator(
                progress = { clampedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = if (isMet) Color(0xFF34D399) else Color(0xFFFBBF24),
                trackColor = Color(0xFF0F0E26)
            )
        }
    }
}

@Composable
private fun LeaderboardRowCard(
    rank: Int,
    entry: LeaderboardEntry,
    isCurrentUser: Boolean
) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFBBF24)
        2 -> Color(0xFFE2E8F0)
        3 -> Color(0xFFF97316)
        else -> Color(0xFF818CF8)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) Color(0xFF28245E) else Color(0xFF1E1B4B)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isCurrentUser) 1.5.dp else 1.dp,
            color = if (isCurrentUser) Color(0xFF818CF8) else Color(0xFF312E81)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(rankColor.copy(alpha = 0.2f))
                        .border(1.dp, rankColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$rank",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = rankColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isCurrentUser) {
                                stringResource(R.string.league_user_you, entry.displayName)
                            } else {
                                entry.displayName
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = getFlagForLanguage(entry.targetLanguage),
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.league_row_meta,
                            localizedTierName(entry.leagueTier),
                            entry.streakDays.toInt(),
                            entry.avgAccuracy
                        ),
                        fontSize = 11.sp,
                        color = Color(0xFFA5B4FC)
                    )
                }
            }

            Text(
                text = stringResource(R.string.league_xp_badge, entry.totalXp.toInt()),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF34D399)
            )
        }
    }
}

@Composable
private fun SenseiNavigationRail(
    currentTab: LearningTab,
    onTabSelected: (LearningTab) -> Unit
) {
    NavigationRail(
        containerColor = Color(0xFF0F0E26),
        contentColor = Color.White,
        modifier = Modifier.fillMaxHeight()
    ) {
        val items = listOf(
            Triple(LearningTab.COACH, stringResource(R.string.nav_coach), Icons.Filled.School to Icons.Outlined.School),
            Triple(LearningTab.ROLEPLAY, stringResource(R.string.nav_roleplay), Icons.Filled.ChatBubble to Icons.Outlined.ChatBubble),
            Triple(LearningTab.VOCAB, stringResource(R.string.nav_vocab), Icons.Filled.Star to Icons.Outlined.Star),
            Triple(LearningTab.LEAGUE, stringResource(R.string.nav_league), Icons.Filled.EmojiEvents to Icons.Outlined.EmojiEvents),
            Triple(LearningTab.PROFILE, stringResource(R.string.nav_profile), Icons.Filled.Person to Icons.Outlined.Person)
        )
        Spacer(modifier = Modifier.height(12.dp))
        items.forEach { (tab, label, icons) ->
            val selected = currentTab == tab
            NavigationRailItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) icons.first else icons.second,
                        contentDescription = label
                    )
                },
                label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = Color(0xFF4F46E5),
                    unselectedIconColor = Color(0xFF818CF8),
                    unselectedTextColor = Color(0xFF818CF8)
                )
            )
        }
    }
}

@Composable
private fun SenseiBottomNavigation(
    currentTab: LearningTab,
    onTabSelected: (LearningTab) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF0F0E26),
        contentColor = Color.White
    ) {
        val coachLabel = stringResource(R.string.nav_coach)
        val roleplayLabel = stringResource(R.string.nav_roleplay)
        val vocabLabel = stringResource(R.string.nav_vocab)
        val leagueLabel = stringResource(R.string.nav_league)
        val profileLabel = stringResource(R.string.nav_profile)

        NavigationBarItem(
            selected = currentTab == LearningTab.COACH,
            onClick = { onTabSelected(LearningTab.COACH) },
            icon = {
                Icon(
                    imageVector = if (currentTab == LearningTab.COACH) Icons.Filled.School else Icons.Outlined.School,
                    contentDescription = coachLabel
                )
            },
            label = { Text(coachLabel, fontSize = 10.sp, maxLines = 1, softWrap = false) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                indicatorColor = Color(0xFF4F46E5),
                unselectedIconColor = Color(0xFF818CF8),
                unselectedTextColor = Color(0xFF818CF8)
            )
        )

        NavigationBarItem(
            selected = currentTab == LearningTab.ROLEPLAY,
            onClick = { onTabSelected(LearningTab.ROLEPLAY) },
            icon = {
                Icon(
                    imageVector = if (currentTab == LearningTab.ROLEPLAY) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubble,
                    contentDescription = roleplayLabel
                )
            },
            label = { Text(roleplayLabel, fontSize = 10.sp, maxLines = 1, softWrap = false) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                indicatorColor = Color(0xFF4F46E5),
                unselectedIconColor = Color(0xFF818CF8),
                unselectedTextColor = Color(0xFF818CF8)
            )
        )

        NavigationBarItem(
            selected = currentTab == LearningTab.VOCAB,
            onClick = { onTabSelected(LearningTab.VOCAB) },
            icon = {
                Icon(
                    imageVector = if (currentTab == LearningTab.VOCAB) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = vocabLabel
                )
            },
            label = { Text(vocabLabel, fontSize = 10.sp, maxLines = 1, softWrap = false) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                indicatorColor = Color(0xFF4F46E5),
                unselectedIconColor = Color(0xFF818CF8),
                unselectedTextColor = Color(0xFF818CF8)
            )
        )

        NavigationBarItem(
            selected = currentTab == LearningTab.LEAGUE,
            onClick = { onTabSelected(LearningTab.LEAGUE) },
            icon = {
                Icon(
                    imageVector = if (currentTab == LearningTab.LEAGUE) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents,
                    contentDescription = leagueLabel
                )
            },
            label = { Text(leagueLabel, fontSize = 10.sp, maxLines = 1, softWrap = false) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                indicatorColor = Color(0xFF4F46E5),
                unselectedIconColor = Color(0xFF818CF8),
                unselectedTextColor = Color(0xFF818CF8)
            )
        )

        NavigationBarItem(
            selected = currentTab == LearningTab.PROFILE,
            onClick = { onTabSelected(LearningTab.PROFILE) },
            icon = {
                Icon(
                    imageVector = if (currentTab == LearningTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = profileLabel
                )
            },
            label = { Text(profileLabel, fontSize = 10.sp, maxLines = 1, softWrap = false) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                indicatorColor = Color(0xFF4F46E5),
                unselectedIconColor = Color(0xFF818CF8),
                unselectedTextColor = Color(0xFF818CF8)
            )
        )
    }
}

@Composable
private fun localizedMoodName(mood: String): String {
    return when (mood.lowercase()) {
        "listening" -> stringResource(R.string.mood_listening)
        "speaking" -> stringResource(R.string.mood_speaking)
        "praising" -> stringResource(R.string.mood_praising)
        "thinking" -> stringResource(R.string.mood_thinking)
        "correcting" -> stringResource(R.string.mood_correcting)
        else -> stringResource(R.string.mood_idle)
    }
}

@Composable
internal fun localizedLanguageName(language: String): String {
    return when (language.lowercase()) {
        "all" -> stringResource(R.string.lang_all)
        "japanese", "ja" -> stringResource(R.string.lang_japanese)
        "spanish", "es" -> stringResource(R.string.lang_spanish)
        "french", "fr" -> stringResource(R.string.lang_french)
        "german", "de" -> stringResource(R.string.lang_german)
        "korean", "ko" -> stringResource(R.string.lang_korean)
        "mandarin", "chinese", "zh" -> stringResource(R.string.lang_mandarin)
        "italian", "it" -> stringResource(R.string.lang_italian)
        "english", "en" -> stringResource(R.string.lang_english)
        else -> language
    }
}

@Composable
private fun localizedDifficultyName(difficulty: String): String {
    return when (difficulty.lowercase()) {
        "beginner" -> stringResource(R.string.difficulty_beginner)
        "intermediate" -> stringResource(R.string.difficulty_intermediate)
        "advanced" -> stringResource(R.string.difficulty_advanced)
        "core" -> stringResource(R.string.difficulty_core)
        "custom" -> stringResource(R.string.difficulty_custom)
        else -> difficulty
    }
}

@Composable
private fun localizedTierName(tier: String): String {
    return when (tier) {
        "Black Belt Sensei" -> stringResource(R.string.tier_black_belt)
        "Gold Tier" -> stringResource(R.string.tier_gold)
        "Silver Tier" -> stringResource(R.string.tier_silver)
        "Bronze Tier" -> stringResource(R.string.tier_bronze)
        "White Belt" -> stringResource(R.string.tier_white_belt)
        else -> tier
    }
}

@Composable
private fun localizedEngineSource(source: String): String {
    return when {
        source.contains("Room Cache", ignoreCase = true) -> stringResource(R.string.engine_cache)
        source.contains("Offline Coach", ignoreCase = true) -> stringResource(R.string.engine_offline)
        source.contains("Daily cap", ignoreCase = true) -> stringResource(R.string.engine_offline_cap)
        source.contains("Offline Fallback", ignoreCase = true) -> stringResource(R.string.engine_offline_fallback)
        source.contains("Firebase AI", ignoreCase = true) -> stringResource(R.string.engine_firebase)
        source.contains("BYOK", ignoreCase = true) -> stringResource(R.string.engine_byok)
        else -> source
    }
}

@Composable
private fun localizedScenarioTitle(title: String): String {
    return when (title) {
        "Tokyo Café Order" -> stringResource(R.string.scenario_tokyo_cafe_title)
        "Parisian Boulangerie" -> stringResource(R.string.scenario_paris_boulangerie_title)
        "Madrid Tapas Bar" -> stringResource(R.string.scenario_madrid_tapas_title)
        "Seoul Night Market" -> stringResource(R.string.scenario_seoul_market_title)
        "Florence Gelateria" -> stringResource(R.string.scenario_rome_gelato_title)
        "Berlin Tech Meetup" -> stringResource(R.string.scenario_berlin_tech_title)
        "Shanghai Teahouse" -> stringResource(R.string.scenario_shanghai_tea_title)
        "London Studio Coffee" -> stringResource(R.string.scenario_london_interview_title)
        else -> title
    }
}

fun getFlagForLanguage(lang: String): String {
    return when (lang.lowercase()) {
        "japanese", "ja" -> "🇯🇵"
        "french", "fr" -> "🇫🇷"
        "spanish", "es" -> "🇪🇸"
        "german", "de" -> "🇩🇪"
        "korean", "ko" -> "🇰🇷"
        "mandarin", "chinese", "zh" -> "🇨🇳"
        "italian", "it" -> "🇮🇹"
        else -> "🇺🇸"
    }
}
