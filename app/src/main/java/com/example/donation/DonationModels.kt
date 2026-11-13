package com.example.donation

object DonationConfig {
    const val PAYPAL_ME_USERNAME = "pocketsenseiapp"
    const val KOFI_PAGE_URL = "https://ko-fi.com/pocketsensei"
    const val PRIVACY_POLICY_ASSET_URL = "file:///android_asset/privacy_policy.html"

    fun buildPayPalUrl(amountUsd: String?): String {
        val cleaned = amountUsd?.trim()?.replace("$", "")?.filter { it.isDigit() || it == '.' }
        val numeric = cleaned?.toDoubleOrNull()
        return if (numeric != null && numeric > 0.0) {
            val formatted = if (numeric % 1.0 == 0.0) {
                numeric.toInt().toString()
            } else {
                String.format(java.util.Locale.US, "%.2f", numeric)
            }
            "https://paypal.me/$PAYPAL_ME_USERNAME/${formatted}USD"
        } else {
            "https://paypal.me/$PAYPAL_ME_USERNAME"
        }
    }
}

enum class DonationChannel {
    GOOGLE_PLAY,
    PAYPAL,
    KOFI
}

data class PlayDonationTier(
    val productId: String,
    val title: String,
    val subtitle: String,
    val fallbackPrice: String,
    val badgeLabel: String
)

object DonationCatalog {
    val playTiers = listOf(
        PlayDonationTier(
            productId = "pocket_sensei_tip_tier_1",
            title = "Matcha Tea",
            subtitle = "Tier 1 • Small token of thanks",
            fallbackPrice = "$1.99",
            badgeLabel = "Tier 1"
        ),
        PlayDonationTier(
            productId = "pocket_sensei_tip_tier_2",
            title = "Sensei's Coffee",
            subtitle = "Tier 2 • Fuel new phrase packs",
            fallbackPrice = "$4.99",
            badgeLabel = "Popular"
        ),
        PlayDonationTier(
            productId = "pocket_sensei_tip_tier_3",
            title = "Dojo Bento",
            subtitle = "Tier 3 • Support 3D voice coaching",
            fallbackPrice = "$9.99",
            badgeLabel = "Tier 3"
        ),
        PlayDonationTier(
            productId = "pocket_sensei_tip_tier_4",
            title = "Master Patron",
            subtitle = "Tier 4 • Champion indie education",
            fallbackPrice = "$19.99",
            badgeLabel = "Tier 4"
        )
    )

    val paypalPresets = listOf("3", "5", "10")
}

data class DonationUiState(
    val isActiveSupporter: Boolean = false,
    val isBillingConnected: Boolean = false,
    val isPurchasing: Boolean = false,
    val productPrices: Map<String, String> = emptyMap(),
    val statusMessage: String? = null,
    val isErrorStatus: Boolean = false
)
