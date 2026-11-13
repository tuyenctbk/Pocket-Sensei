package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.R
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

object OnboardingPreferences {
    private const val PREFS_NAME = "pocket_sensei_onboarding_prefs"
    private const val KEY_COMPLETED = "onboarding_completed"
    private const val KEY_PREFERRED_LANGUAGE = "onboarding_preferred_language"

    fun hasCompletedOnboarding(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_COMPLETED, false)
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean, preferredLanguage: String? = null) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .apply {
                putBoolean(KEY_COMPLETED, completed)
                if (!preferredLanguage.isNullOrBlank()) {
                    putString(KEY_PREFERRED_LANGUAGE, preferredLanguage)
                }
            }
            .apply()
    }

    fun getPreferredLanguage(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PREFERRED_LANGUAGE, null)
            ?.takeIf { it.isNotBlank() }
    }
}

private data class OnboardingPageSpec(
    @param:DrawableRes val imageRes: Int,
    @param:StringRes val badgeRes: Int,
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int,
    val icon: ImageVector,
    val accentColor: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    initialLanguage: String = "Japanese",
    supportedLanguages: List<String> = listOf(
        "Japanese", "Spanish", "French", "German", "Korean", "Mandarin", "Italian", "English"
    ),
    onComplete: (selectedLanguage: String) -> Unit
) {
    val pages = remember {
        listOf(
            OnboardingPageSpec(
                imageRes = R.drawable.img_onboarding_coach_1794550583192,
                badgeRes = R.string.onboarding_page1_badge,
                titleRes = R.string.onboarding_page1_title,
                subtitleRes = R.string.onboarding_page1_subtitle,
                icon = Icons.Default.GraphicEq,
                accentColor = Color(0xFF34D399)
            ),
            OnboardingPageSpec(
                imageRes = R.drawable.img_onboarding_roleplay_1794550583368,
                badgeRes = R.string.onboarding_page2_badge,
                titleRes = R.string.onboarding_page2_title,
                subtitleRes = R.string.onboarding_page2_subtitle,
                icon = Icons.Default.ChatBubble,
                accentColor = Color(0xFFF472B6)
            ),
            OnboardingPageSpec(
                imageRes = R.drawable.img_onboarding_mastery_1794550582985,
                badgeRes = R.string.onboarding_page3_badge,
                titleRes = R.string.onboarding_page3_title,
                subtitleRes = R.string.onboarding_page3_subtitle,
                icon = Icons.Default.EmojiEvents,
                accentColor = Color(0xFFFBBF24)
            )
        )
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    var selectedLanguage by remember(initialLanguage) { mutableStateOf(initialLanguage) }

    BackHandler(enabled = pagerState.currentPage > 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("onboarding_screen"),
        color = Color(0xFF0F0E26)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1648),
                            Color(0xFF0F0E26),
                            Color(0xFF0B0A1D)
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 560.dp)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header: Step indicator + Skip button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF1E1B4B))
                            .border(1.dp, Color(0xFF312E81), RoundedCornerShape(999.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("onboarding_step_badge")
                    ) {
                        Text(
                            text = stringResource(
                                R.string.onboarding_step_format,
                                pagerState.currentPage + 1,
                                pages.size
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFA5B4FC)
                        )
                    }

                    TextButton(
                        onClick = { onComplete(selectedLanguage) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("onboarding_skip_button")
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_skip),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFC7D2FE)
                        )
                    }
                }

                // Horizontal Pager for 3 Onboarding Slides
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("onboarding_pager")
                ) { pageIndex ->
                    val page = pages[pageIndex]
                    val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
                        .absoluteValue
                        .coerceIn(0f, 1f)
                    val cardScale = lerp(1f, 0.90f, pageOffset)
                    val cardAlpha = lerp(1f, 0.55f, pageOffset)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Hero Artwork Card with Pager Parallax
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (pageIndex == pages.lastIndex) 160.dp else 220.dp)
                                .graphicsLayer {
                                    scaleX = cardScale
                                    scaleY = cardScale
                                    alpha = cardAlpha
                                }
                                .testTag("onboarding_art_card_$pageIndex"),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF141233)),
                            border = BorderStroke(1.5.dp, page.accentColor.copy(alpha = 0.45f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    painter = painterResource(id = page.imageRes),
                                    contentDescription = stringResource(page.titleRes),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color(0xFF0F0E26).copy(alpha = 0.55f)
                                                )
                                            )
                                        )
                                )
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0xFF0F0E26).copy(alpha = 0.85f))
                                        .border(1.dp, page.accentColor.copy(alpha = 0.7f), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = page.icon,
                                        contentDescription = null,
                                        tint = page.accentColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = stringResource(page.badgeRes),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = page.accentColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = stringResource(page.titleRes),
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 29.sp,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = stringResource(page.subtitleRes),
                            fontSize = 14.sp,
                            color = Color(0xFFC7D2FE),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Interactive Target Language Picker on the Final Slide
                        if (pageIndex == pages.lastIndex) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_language_picker_card"),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                                border = BorderStroke(1.dp, Color(0xFF312E81))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(R.string.onboarding_select_language_label),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFDE68A)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        supportedLanguages.forEach { lang ->
                                            val isSelected = selectedLanguage.equals(lang, ignoreCase = true)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { selectedLanguage = lang },
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("onboarding_lang_${lang.lowercase()}"),
                                                label = {
                                                    Text(
                                                        text = "${getFlagForLanguage(lang)} ${localizedLanguageName(lang)}",
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFF4F46E5),
                                                    selectedLabelColor = Color.White,
                                                    containerColor = Color(0xFF141233),
                                                    labelColor = Color(0xFFC7D2FE)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Bar: Animated Page Dots + Back / Next / Start Speaking Controls
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        pages.forEachIndexed { idx, spec ->
                            val isSelected = pagerState.currentPage == idx
                            val width by animateDpAsState(
                                targetValue = if (isSelected) 28.dp else 8.dp,
                                animationSpec = tween(250, easing = FastOutSlowInEasing),
                                label = "dotWidth"
                            )
                            val dotColor by animateColorAsState(
                                targetValue = if (isSelected) spec.accentColor else Color(0xFF312E81),
                                animationSpec = tween(250),
                                label = "dotColor"
                            )
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(width)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pagerState.currentPage > 0) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                },
                                modifier = Modifier
                                    .height(52.dp)
                                    .weight(1f)
                                    .testTag("onboarding_back_button"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFF4F46E5)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC7D2FE))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.onboarding_back),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        val isLastPage = pagerState.currentPage == pages.lastIndex
                        Button(
                            onClick = {
                                if (isLastPage) {
                                    onComplete(selectedLanguage)
                                } else {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            modifier = Modifier
                                .height(52.dp)
                                .weight(if (pagerState.currentPage > 0) 1.4f else 1f)
                                .testTag(if (isLastPage) "onboarding_get_started_button" else "onboarding_next_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLastPage) Color(0xFF34D399) else Color(0xFF4F46E5),
                                contentColor = if (isLastPage) Color(0xFF0F172A) else Color.White
                            )
                        ) {
                            Text(
                                text = if (isLastPage) {
                                    stringResource(R.string.onboarding_get_started)
                                } else {
                                    stringResource(R.string.onboarding_next)
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
