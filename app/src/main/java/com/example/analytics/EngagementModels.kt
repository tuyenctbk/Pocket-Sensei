package com.example.analytics

enum class PromptType {
    UPDATE,
    RATE,
    SHARE,
    DONATION
}

enum class PromptAction {
    ACCEPTED,
    LATER,
    NEVER
}

enum class TriggerMoment {
    APP_START,
    POST_PRACTICE_IDLE,
    CELEBRATORY_MILESTONE
}

data class AppUpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val releaseNotes: String = "",
    val isMandatory: Boolean = false
)

data class EngagementSnapshot(
    val totalSessions: Int = 1,
    val positiveMilestones: Int = 0,
    val promptsShownThisSession: Int = 0,
    val lastAnyPromptShownMs: Long = 0L,
    val lastRatePromptShownMs: Long = 0L,
    val rateDismissCount: Int = 0,
    val rateCompletedOrOptedOut: Boolean = false,
    val lastSharePromptShownMs: Long = 0L,
    val shareDismissCount: Int = 0,
    val lastShareCompletedMs: Long = 0L,
    val shareOptedOut: Boolean = false,
    val lastUpdatePromptShownMs: Long = 0L,
    val snoozedUpdateVersionCode: Int = 0,
    val updateDismissCountForVersion: Int = 0,
    val firstInstallMs: Long = 0L,
    val successfulOperations: Int = 0,
    val isActiveSupporter: Boolean = false,
    val lastDonationPromptShownMs: Long = 0L,
    val donationPromptDismissCount: Int = 0,
    val donationPromptOptedOut: Boolean = false,
    val lastDonationBannerDismissMs: Long = 0L
)

sealed interface EngagementPrompt {
    val type: PromptType

    data class UpdatePrompt(
        val versionName: String,
        val versionCode: Int,
        val releaseNotes: String,
        val isMandatory: Boolean
    ) : EngagementPrompt {
        override val type: PromptType = PromptType.UPDATE
    }

    data class RatePrompt(
        val sessionsCompleted: Int,
        val milestonesReached: Int
    ) : EngagementPrompt {
        override val type: PromptType = PromptType.RATE
    }

    data class SharePrompt(
        val wordsMastered: Long,
        val latestScore: Int?
    ) : EngagementPrompt {
        override val type: PromptType = PromptType.SHARE
    }

    data class DonationPrompt(
        val successfulOperations: Int,
        val daysSinceInstall: Long
    ) : EngagementPrompt {
        override val type: PromptType = PromptType.DONATION
    }
}
