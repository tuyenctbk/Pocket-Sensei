package com.example.ai

/**
 * Model-Agnostic Capability Tiers and Request Hygiene configuration.
 * Future-proofs model names, endpoints, and fallback policies so they are never
 * hardcoded inside UI screens or ViewModels.
 */
object AiModelConfig {

    // Tier 1 — Primary Standard: Used when quota is healthy (< 80% used) and Eco Mode is off
    const val PRIMARY_REASONING_MODEL = "gemini-3.8-flash"

    // Tier 2 — High-Efficiency Lite: Activated on >= 80% quota, 429 rate-limits, or Eco Lite Mode
    const val ECONOMY_LITE_MODEL = "gemini-3.5-flash-lite"

    // Tier 3 — Zero-Cost Local / Deterministic Fallback: Used when quota is 0 or offline
    const val LOCAL_REFLEX_MODEL = "local-reflex-engine"

    // BYOK Google AI Studio endpoint base
    const val BYOK_ENDPOINT_BASE = "https://generativelanguage.googleapis.com/v1beta/models/"

    // Token & Request Hygiene Guardrails
    const val DEDUPLICATION_CACHE_TTL_MS = 45_000L // 45s fingerprint deduplication
    const val SLIDING_CONTEXT_MAX_TURNS = 6 // Send only last 6 turns to stretch free tier 3x
    const val MAX_FREE_AUDIO_DURATION_SEC = 20 // Cap recording length for pronunciation checks
    const val ECO_LITE_THRESHOLD_PERCENT = 0.80f // 80% soft nudge trigger

    enum class AiCapabilityTier(
        val roleName: String,
        val modelName: String,
        val isZeroCostLocal: Boolean,
        val description: String
    ) {
        TIER_1_PRIMARY(
            roleName = "Primary Standard",
            modelName = PRIMARY_REASONING_MODEL,
            isZeroCostLocal = false,
            description = "High-precision phonetic & pitch accent reasoning"
        ),
        TIER_2_ECONOMY_LITE(
            roleName = "High-Efficiency Lite",
            modelName = ECONOMY_LITE_MODEL,
            isZeroCostLocal = false,
            description = "Fast, quota-friendly model tier with higher availability"
        ),
        TIER_3_ZERO_COST_LOCAL(
            roleName = "Zero-Cost Local Coach",
            modelName = LOCAL_REFLEX_MODEL,
            isZeroCostLocal = true,
            description = "On-device phonetic & pitch matching ($0 cloud tokens, streak preserved)"
        )
    }

    /**
     * Resolves the appropriate capability tier based on current quota, eco mode, rate-limits,
     * and offline status.
     */
    fun resolveTier(
        usagePercent: Float,
        ecoLiteMode: Boolean,
        isRateLimited: Boolean,
        isOfflineOrZeroQuota: Boolean
    ): AiCapabilityTier {
        return when {
            isOfflineOrZeroQuota -> AiCapabilityTier.TIER_3_ZERO_COST_LOCAL
            isRateLimited || ecoLiteMode || usagePercent >= ECO_LITE_THRESHOLD_PERCENT -> AiCapabilityTier.TIER_2_ECONOMY_LITE
            else -> AiCapabilityTier.TIER_1_PRIMARY
        }
    }
}
