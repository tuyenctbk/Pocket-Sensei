package com.example.ui.screens

import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.donation.DonationCatalog
import com.example.donation.DonationChannel
import com.example.donation.DonationConfig
import com.example.donation.DonationUiState
import com.example.donation.PlayDonationTier
import com.example.donation.QrCodeGenerator

@Composable
fun HomeDonationCompactBanner(
    onSupportClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_donation_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
        border = BorderStroke(1.dp, Color(0xFFF472B6).copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF472B6).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalCafe,
                    contentDescription = stringResource(R.string.cd_support_app),
                    tint = Color(0xFFF472B6),
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.donation_banner_title),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.donation_banner_subtitle),
                    fontSize = 11.sp,
                    color = Color(0xFFC7D2FE),
                    maxLines = 2
                )
            }
            Button(
                onClick = onSupportClick,
                modifier = Modifier.testTag("home_donation_banner_cta"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
            ) {
                Text(stringResource(R.string.donation_banner_cta), fontSize = 12.sp, color = Color.White)
            }
            IconButton(
                onClick = onDismissClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("donation_banner_dismiss")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_dismiss_donation_banner),
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileSupportSection(
    isActiveSupporter: Boolean,
    successfulOperationsCount: Int,
    onOpenDonationSheet: (DonationChannel) -> Unit,
    onOpenPrivacyPolicy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_support_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
        border = BorderStroke(
            1.dp,
            if (isActiveSupporter) Color(0xFFFBBF24).copy(alpha = 0.65f) else Color(0xFF4F46E5).copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isActiveSupporter) Icons.Default.Verified else Icons.Default.Favorite,
                        contentDescription = null,
                        tint = if (isActiveSupporter) Color(0xFFFBBF24) else Color(0xFFF472B6),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isActiveSupporter) {
                            stringResource(R.string.support_active_supporter)
                        } else {
                            stringResource(R.string.support_development_title)
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF312E81))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = stringResource(R.string.support_badge_ad_free),
                        fontSize = 10.sp,
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = if (isActiveSupporter) {
                    stringResource(R.string.support_active_desc)
                } else {
                    stringResource(R.string.support_inactive_desc, successfulOperationsCount)
                },
                fontSize = 12.sp,
                color = Color(0xFFC7D2FE)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onOpenDonationSheet(DonationChannel.GOOGLE_PLAY) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_donate_play_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(stringResource(R.string.channel_google_play), fontSize = 11.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = { onOpenDonationSheet(DonationChannel.PAYPAL) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_donate_paypal_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(stringResource(R.string.channel_paypal_qr), fontSize = 11.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = { onOpenDonationSheet(DonationChannel.KOFI) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_donate_kofi_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF472B6))
                ) {
                    Icon(Icons.Default.LocalCafe, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(stringResource(R.string.channel_kofi), fontSize = 11.sp, maxLines = 1)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenPrivacyPolicy)
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PrivacyTip,
                    contentDescription = stringResource(R.string.privacy_policy_title),
                    tint = Color(0xFFA5B4FC),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.privacy_policy_link),
                    fontSize = 12.sp,
                    color = Color(0xFFA5B4FC),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationSupportModal(
    initialChannel: DonationChannel,
    donationUiState: DonationUiState,
    onDismiss: () -> Unit,
    onLaunchPlayPurchase: (String) -> Unit,
    onOpenPayPal: (String?) -> Unit,
    onCopyPayPal: (String?) -> Unit,
    onSharePayPal: (String?) -> Unit,
    onOpenKofi: () -> Unit,
    onCopyKofi: () -> Unit,
    onClearStatus: () -> Unit,
    onChannelSelected: (DonationChannel) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedChannel by remember(initialChannel) { mutableStateOf(initialChannel) }
    var selectedPayPalPreset by remember { mutableStateOf("5") }
    var customPayPalAmount by remember { mutableStateOf("") }

    LaunchedEffect(initialChannel) {
        selectedChannel = initialChannel
    }

    ModalBottomSheet(
        onDismissRequest = {
            onClearStatus()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = Color(0xFF141233)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF6366F1), Color(0xFFEC4899))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (donationUiState.isActiveSupporter) Icons.Default.Verified else Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.cd_support_app),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.donation_sheet_subtitle),
                            fontSize = 12.sp,
                            color = Color(0xFFA5B4FC)
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_close), tint = Color(0xFFA5B4FC))
                }
            }

            if (donationUiState.isActiveSupporter) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                    border = BorderStroke(1.dp, Color(0xFF34D399))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399))
                        Text(
                            text = stringResource(R.string.donation_active_unlocked),
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Supporter Value Proposition Perks Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("donation_perks_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                border = BorderStroke(1.dp, Color(0xFF4F46E5).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.donation_perks_title),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.donation_perks_summary),
                            fontSize = 11.sp,
                            color = Color(0xFFC7D2FE)
                        )
                    }
                }
            }

            // Channel Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val channels = listOf(
                    DonationChannel.GOOGLE_PLAY to stringResource(R.string.channel_google_play),
                    DonationChannel.PAYPAL to stringResource(R.string.channel_paypal_tab),
                    DonationChannel.KOFI to stringResource(R.string.channel_kofi)
                )
                channels.forEach { (channel, label) ->
                    FilterChip(
                        selected = selectedChannel == channel,
                        onClick = {
                            if (selectedChannel != channel) {
                                selectedChannel = channel
                                onChannelSelected(channel)
                            }
                            onClearStatus()
                        },
                        label = { Text(label, fontSize = 12.sp, maxLines = 1) },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("donation_tab_${channel.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4F46E5),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E1B4B),
                            labelColor = Color(0xFFC7D2FE)
                        )
                    )
                }
            }

            AnimatedVisibility(visible = donationUiState.statusMessage != null) {
                donationUiState.statusMessage?.let { msg ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (donationUiState.isErrorStatus) Color(0xFF3F1D2B) else Color(0xFF1E3A2F)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (donationUiState.isErrorStatus) Color(0xFFF87171) else Color(0xFF34D399)
                        )
                    ) {
                        Text(
                            text = msg,
                            fontSize = 12.sp,
                            color = Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            when (selectedChannel) {
                DonationChannel.GOOGLE_PLAY -> {
                    Text(
                        text = stringResource(R.string.donation_play_desc),
                        fontSize = 12.sp,
                        color = Color(0xFFC7D2FE)
                    )

                    if (donationUiState.isPurchasing) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF818CF8))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(stringResource(R.string.donation_play_connecting), fontSize = 12.sp, color = Color.White)
                        }
                    }

                    DonationCatalog.playTiers.forEach { tier ->
                        val livePrice = donationUiState.productPrices[tier.productId] ?: tier.fallbackPrice
                        val (tierTitle, tierSubtitle, tierBadge) = localizedTierText(tier)
                        val isPopular = tier.productId == "pocket_sensei_tip_tier_2"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !donationUiState.isPurchasing) {
                                    onLaunchPlayPurchase(tier.productId)
                                }
                                .testTag("play_tier_${tier.productId}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPopular) Color(0xFF282462) else Color(0xFF1E1B4B)
                            ),
                            border = BorderStroke(
                                width = if (isPopular) 1.5.dp else 1.dp,
                                color = if (isPopular) Color(0xFFFBBF24) else Color(0xFF312E81)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = tierTitle,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isPopular) Color(0xFFFBBF24) else Color(0xFF4F46E5))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isPopular) stringResource(R.string.donation_recommended_tag) else tierBadge,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPopular) Color(0xFF0F172A) else Color.White
                                            )
                                        }
                                    }
                                    Text(
                                        text = tierSubtitle,
                                        fontSize = 11.sp,
                                        color = Color(0xFFA5B4FC)
                                    )
                                }
                                Button(
                                    onClick = { onLaunchPlayPurchase(tier.productId) },
                                    enabled = !donationUiState.isPurchasing,
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPopular) Color(0xFFFBBF24) else Color(0xFF34D399)
                                    )
                                ) {
                                    Text(
                                        text = livePrice,
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                DonationChannel.PAYPAL -> {
                    val activeAmount: String? = when (selectedPayPalPreset) {
                        "custom" -> customPayPalAmount.takeIf { it.isNotBlank() }
                        else -> selectedPayPalPreset
                    }
                    val paypalUrl = remember(activeAmount) {
                        DonationConfig.buildPayPalUrl(activeAmount)
                    }
                    val qrBitmap = remember(paypalUrl) {
                        QrCodeGenerator.generateQrBitmap(paypalUrl, sizePx = 420)
                    }

                    Text(
                        text = stringResource(R.string.donation_paypal_desc),
                        fontSize = 12.sp,
                        color = Color(0xFFC7D2FE)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DonationCatalog.paypalPresets.forEach { preset ->
                            FilterChip(
                                selected = selectedPayPalPreset == preset,
                                onClick = { selectedPayPalPreset = preset },
                                label = { Text(stringResource(R.string.donation_usd_preset_format, preset)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize(),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0284C7),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF1E1B4B),
                                    labelColor = Color(0xFFC7D2FE)
                                )
                            )
                        }
                        FilterChip(
                            selected = selectedPayPalPreset == "custom",
                            onClick = { selectedPayPalPreset = "custom" },
                            label = { Text(stringResource(R.string.donation_paypal_any)) },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize(),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E1B4B),
                                labelColor = Color(0xFFC7D2FE)
                            )
                        )
                    }

                    if (selectedPayPalPreset == "custom") {
                        OutlinedTextField(
                            value = customPayPalAmount,
                            onValueChange = { input ->
                                customPayPalAmount = input.filter { it.isDigit() || it == '.' }
                            },
                            label = { Text(stringResource(R.string.donation_paypal_custom_label)) },
                            placeholder = { Text(stringResource(R.string.donation_paypal_custom_hint)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )
                    }

                    // Live QR Bitmap Card
                    if (qrBitmap != null) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White)
                                    .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(18.dp))
                                    .padding(12.dp)
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = stringResource(R.string.cd_paypal_qr, paypalUrl),
                                    modifier = Modifier
                                        .size(180.dp)
                                        .testTag("paypal_qr_image")
                                )
                            }
                        }
                    }

                    // Copyable URL Box
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCopyPayPal(activeAmount) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = paypalUrl,
                                fontSize = 12.sp,
                                color = Color(0xFF38BDF8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.cd_copy_paypal_url),
                                tint = Color(0xFFA5B4FC),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onOpenPayPal(activeAmount) },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("paypal_open_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.donation_btn_open_paypal), fontSize = 12.sp, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = { onCopyPayPal(activeAmount) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("paypal_copy_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA5B4FC))
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.donation_btn_copy), fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { onSharePayPal(activeAmount) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("paypal_share_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF34D399))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.donation_btn_share), fontSize = 12.sp)
                        }
                    }
                }

                DonationChannel.KOFI -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                        border = BorderStroke(1.dp, Color(0xFFF472B6).copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.LocalCafe, contentDescription = null, tint = Color(0xFFF472B6))
                                Text(
                                    text = stringResource(R.string.donation_kofi_title),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = stringResource(R.string.donation_kofi_desc),
                                fontSize = 12.sp,
                                color = Color(0xFFC7D2FE)
                            )
                            Text(
                                text = DonationConfig.KOFI_PAGE_URL,
                                fontSize = 12.sp,
                                color = Color(0xFFF472B6),
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onOpenKofi,
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .testTag("kofi_open_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB2777))
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.donation_btn_open_kofi), fontSize = 12.sp, color = Color.White)
                                }
                                OutlinedButton(
                                    onClick = onCopyKofi,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("kofi_copy_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA5B4FC))
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.donation_btn_copy_link), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun localizedTierText(tier: PlayDonationTier): Triple<String, String, String> {
    return when (tier.productId) {
        "pocket_sensei_tip_tier_1" -> Triple(
            stringResource(R.string.tier1_title),
            stringResource(R.string.tier1_subtitle),
            stringResource(R.string.tier1_badge)
        )
        "pocket_sensei_tip_tier_2" -> Triple(
            stringResource(R.string.tier2_title),
            stringResource(R.string.tier2_subtitle),
            stringResource(R.string.tier2_badge)
        )
        "pocket_sensei_tip_tier_3" -> Triple(
            stringResource(R.string.tier3_title),
            stringResource(R.string.tier3_subtitle),
            stringResource(R.string.tier3_badge)
        )
        "pocket_sensei_tip_tier_4" -> Triple(
            stringResource(R.string.tier4_title),
            stringResource(R.string.tier4_subtitle),
            stringResource(R.string.tier4_badge)
        )
        else -> Triple(tier.title, tier.subtitle, tier.badgeLabel)
    }
}

@Composable
fun InAppPrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = Color(0xFF818CF8))
                Text(stringResource(R.string.privacy_policy_title), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        text = {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(android.graphics.Color.parseColor("#0F0E26"))
                        loadUrl(DonationConfig.PRIVACY_POLICY_ASSET_URL)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 280.dp, max = 420.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
            ) {
                Text(stringResource(R.string.btn_close), color = Color.White)
            }
        },
        containerColor = Color(0xFF1E1B4B)
    )
}
