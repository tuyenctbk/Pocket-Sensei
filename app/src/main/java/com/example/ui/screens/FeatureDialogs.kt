package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Hands-Free Commuter Shadowing Mode controller bar for auto-playing and cycling phrases.
 */
@Composable
fun ShadowingModeControlBar(
    isActive: Boolean,
    isSlow: Boolean,
    stepLabel: String,
    stepProgress: Float,
    onToggleActive: () -> Unit,
    onToggleSlow: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shadowing_mode_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF172554) else Color(0xFF191642)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isActive) Color(0xFF38BDF8) else Color(0xFF312E81)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = if (isActive) Color(0xFF38BDF8) else Color(0xFFA5B4FC),
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.shadowing_title),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isActive) stepLabel else stringResource(R.string.shadowing_subtitle),
                            fontSize = 10.sp,
                            color = if (isActive) Color(0xFF7DD3FC) else Color(0xFF94A3B8)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = isSlow,
                        onClick = onToggleSlow,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("shadowing_slow_toggle"),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SlowMotionVideo,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(
                                    if (isSlow) R.string.shadowing_speed_slow else R.string.shadowing_speed_normal
                                ),
                                fontSize = 10.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4F46E5),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF141233),
                            labelColor = Color(0xFFC7D2FE)
                        )
                    )

                    Button(
                        onClick = onToggleActive,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("shadowing_toggle_btn"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) Color(0xFFEF4444) else Color(0xFF0284C7)
                        )
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                if (isActive) R.string.shadowing_btn_stop else R.string.shadowing_btn_start
                            ),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isActive) {
                LinearProgressIndicator(
                    progress = { stepProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF0F0E26)
                )
            }
        }
    }
}

/**
 * Dialog to create a custom user-authored practice phrase persisted locally in Room.
 */
@Composable
fun CustomPhraseDialog(
    language: String,
    isGeneratingTopicPack: Boolean = false,
    onGenerateTopicPack: ((topic: String) -> Unit)? = null,
    onSave: (targetText: String, phonetic: String, translation: String, accentFocus: String) -> Unit,
    onDismiss: () -> Unit
) {
    var targetText by remember { mutableStateOf("") }
    var phonetic by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf("") }
    var accentFocus by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("custom_phrase_dialog"),
        title = {
            Text(
                text = stringResource(R.string.custom_phrase_dialog_title, localizedLanguageName(language)),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text(stringResource(R.string.custom_phrase_target_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_phrase_target_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                if (onGenerateTopicPack != null) {
                    OutlinedButton(
                        onClick = { onGenerateTopicPack(targetText) },
                        enabled = targetText.isNotBlank() && !isGeneratingTopicPack,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_topic_pack_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                    ) {
                        Text(
                            text = stringResource(R.string.topic_pack_generate_btn),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                OutlinedTextField(
                    value = phonetic,
                    onValueChange = { phonetic = it },
                    label = { Text(stringResource(R.string.custom_phrase_phonetic_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_phrase_phonetic_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = translation,
                    onValueChange = { translation = it },
                    label = { Text(stringResource(R.string.custom_phrase_translation_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_phrase_translation_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = accentFocus,
                    onValueChange = { accentFocus = it },
                    label = { Text(stringResource(R.string.custom_phrase_focus_label)) },
                    singleLine = true,
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
                    onSave(targetText, phonetic, translation, accentFocus)
                },
                enabled = targetText.isNotBlank() && !isGeneratingTopicPack,
                modifier = Modifier.testTag("custom_phrase_save_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399))
            ) {
                Text(
                    text = stringResource(R.string.custom_save_btn),
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = Color(0xFFA5B4FC))
            }
        },
        containerColor = Color(0xFF1E1B4B)
    )
}

/**
 * Dialog to create a custom AI Roleplay scenario (Interview, Hotel Check-in, Clinic, or Custom).
 */
@Composable
fun CustomScenarioBuilderDialog(
    language: String,
    isGeneratingTopicPack: Boolean = false,
    onGenerateTopicPack: ((topic: String) -> Unit)? = null,
    onSave: (title: String, senseiRole: String, userRole: String, openingLine: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var senseiRole by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf("") }
    var openingLine by remember { mutableStateOf("") }

    val templateInterviewTitle = stringResource(R.string.scenario_template_interview_title)
    val templateInterviewSensei = stringResource(R.string.scenario_template_interview_sensei)
    val templateInterviewUser = stringResource(R.string.scenario_template_interview_user)
    val templateInterviewOpen = stringResource(R.string.scenario_template_interview_open)

    val templateHotelTitle = stringResource(R.string.scenario_template_hotel_title)
    val templateHotelSensei = stringResource(R.string.scenario_template_hotel_sensei)
    val templateHotelUser = stringResource(R.string.scenario_template_hotel_user)
    val templateHotelOpen = stringResource(R.string.scenario_template_hotel_open)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("custom_scenario_dialog"),
        title = {
            Text(
                text = stringResource(R.string.custom_scenario_dialog_title, localizedLanguageName(language)),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.custom_scenario_templates_label),
                    fontSize = 11.sp,
                    color = Color(0xFFA5B4FC)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            title = templateInterviewTitle
                            senseiRole = templateInterviewSensei
                            userRole = templateInterviewUser
                            openingLine = templateInterviewOpen
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("scenario_template_interview_btn")
                    ) {
                        Text(templateInterviewTitle, fontSize = 10.sp, color = Color(0xFF34D399))
                    }
                    OutlinedButton(
                        onClick = {
                            title = templateHotelTitle
                            senseiRole = templateHotelSensei
                            userRole = templateHotelUser
                            openingLine = templateHotelOpen
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("scenario_template_hotel_btn")
                    ) {
                        Text(templateHotelTitle, fontSize = 10.sp, color = Color(0xFFFBBF24))
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.custom_scenario_title_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_scenario_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                if (onGenerateTopicPack != null) {
                    OutlinedButton(
                        onClick = { onGenerateTopicPack(title) },
                        enabled = title.isNotBlank() && !isGeneratingTopicPack,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_topic_pack_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                    ) {
                        Text(
                            text = stringResource(R.string.topic_pack_generate_btn),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                OutlinedTextField(
                    value = senseiRole,
                    onValueChange = { senseiRole = it },
                    label = { Text(stringResource(R.string.custom_scenario_sensei_role_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = userRole,
                    onValueChange = { userRole = it },
                    label = { Text(stringResource(R.string.custom_scenario_user_role_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = openingLine,
                    onValueChange = { openingLine = it },
                    label = { Text(stringResource(R.string.custom_scenario_opening_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_scenario_opening_input"),
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
                    onSave(title, senseiRole, userRole, openingLine)
                },
                enabled = title.isNotBlank() && openingLine.isNotBlank() && !isGeneratingTopicPack,
                modifier = Modifier.testTag("custom_scenario_save_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
            ) {
                Text(
                    text = stringResource(R.string.custom_save_btn),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = Color(0xFFA5B4FC))
            }
        },
        containerColor = Color(0xFF1E1B4B)
    )
}

/**
 * Opt-in Evening Streak Reminder card inside ProfileScreen.
 */
@Composable
fun StreakReminderSettingsCard(
    enabled: Boolean,
    reminderHour: Int,
    onToggle: (Boolean) -> Unit,
    onSelectHour: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("streak_reminder_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
        border = BorderStroke(1.dp, if (enabled) Color(0xFFF97316) else Color(0xFF312E81))
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = if (enabled) Color(0xFFF97316) else Color(0xFFA5B4FC),
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.reminder_card_title),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.reminder_card_subtitle),
                            fontSize = 11.sp,
                            color = Color(0xFFA5B4FC)
                        )
                    }
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("streak_reminder_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFF97316),
                        uncheckedThumbColor = Color(0xFFC7D2FE),
                        uncheckedTrackColor = Color(0xFF312E81)
                    )
                )
            }

            AnimatedVisibility(visible = enabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.reminder_time_label),
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                    listOf(19, 20, 21).forEach { hour ->
                        FilterChip(
                            selected = reminderHour == hour,
                            onClick = { onSelectHour(hour) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("reminder_hour_$hour"),
                            label = {
                                Text(
                                    text = stringResource(R.string.reminder_hour_format, hour),
                                    fontSize = 11.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFF97316),
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
