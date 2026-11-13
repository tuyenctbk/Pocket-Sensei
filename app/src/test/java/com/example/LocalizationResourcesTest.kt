package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalizationResourcesTest {

    private val supportedLocales = listOf(
        Locale.forLanguageTag("en"),
        Locale.forLanguageTag("ja"),
        Locale.forLanguageTag("es"),
        Locale.forLanguageTag("fr"),
        Locale.forLanguageTag("de"),
        Locale.forLanguageTag("ko"),
        Locale.forLanguageTag("zh-CN"),
        Locale.forLanguageTag("zh-TW"),
        Locale.forLanguageTag("it"),
        Locale.forLanguageTag("pt-BR"),
        Locale.forLanguageTag("ru"),
        Locale.forLanguageTag("ar"),
        Locale.forLanguageTag("hi"),
        Locale.forLanguageTag("vi"),
        Locale.forLanguageTag("th"),
        Locale.forLanguageTag("id"),
        Locale.forLanguageTag("tr"),
        Locale.forLanguageTag("pl"),
        Locale.forLanguageTag("nl"),
        Locale.forLanguageTag("uk"),
        Locale.forLanguageTag("sv")
    )

    @Test
    fun `all 21 locales load non-blank strings and valid format placeholders`() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()

        val simpleStringIds = listOf(
            R.string.app_name,
            R.string.splash_tagline,
            R.string.auth_sign_in_google,
            R.string.nav_coach,
            R.string.nav_roleplay,
            R.string.nav_vocab,
            R.string.nav_league,
            R.string.nav_profile,
            R.string.mic_permission_required,
            R.string.coach_practice_phrases,
            R.string.coach_waveform_title,
            R.string.coach_waveform_ready,
            R.string.coach_waveform_hint,
            R.string.vocab_bank_title,
            R.string.vocab_filter_mastery_all,
            R.string.vocab_status_needs_review,
            R.string.vocab_status_learning,
            R.string.vocab_status_mastered,
            R.string.vocab_btn_drill_weakest,
            R.string.vocab_empty_filtered_subtitle,
            R.string.vocab_btn_got_it,
            R.string.league_title,
            R.string.ai_card_title,
            R.string.blitz_title,
            R.string.support_development_title,
            R.string.privacy_policy_title,
            R.string.speech_recognizer_unavailable,
            R.string.hud_praise_speech,
            R.string.hud_greet_speech,
            R.string.error_evaluation_failed,
            R.string.error_roleplay_failed,
            R.string.coach_default_accent_focus,
            R.string.eval_tip_clean,
            R.string.eval_tip_slow_down,
            R.string.eval_encourage_high,
            R.string.eval_encourage_mid,
            R.string.eval_encourage_low,
            R.string.eval_pitch_guidance_default,
            R.string.eval_contour_default,
            R.string.eval_tip_default,
            R.string.eval_encourage_default,
            R.string.eval_pitch_guidance_cloud_default,
            R.string.roleplay_offline_translation_fallback,
            R.string.roleplay_offline_reply_ja_translation,
            R.string.roleplay_offline_reply_fr_translation,
            R.string.roleplay_offline_reply_es_translation,
            R.string.roleplay_offline_reply_ko_translation,
            R.string.roleplay_offline_reply_de_translation,
            R.string.roleplay_offline_reply_it_translation,
            R.string.roleplay_cloud_note_default,
            R.string.phrase_ja_1_translation,
            R.string.phrase_ja_1_focus,
            R.string.phrase_ja_2_translation,
            R.string.phrase_ja_2_focus,
            R.string.phrase_ja_3_translation,
            R.string.phrase_ja_3_focus,
            R.string.phrase_ja_4_translation,
            R.string.phrase_ja_4_focus,
            R.string.phrase_ja_5_translation,
            R.string.phrase_ja_5_focus,
            R.string.phrase_fr_1_translation,
            R.string.phrase_fr_1_focus,
            R.string.phrase_fr_2_translation,
            R.string.phrase_fr_2_focus,
            R.string.phrase_fr_3_translation,
            R.string.phrase_fr_3_focus,
            R.string.phrase_fr_4_translation,
            R.string.phrase_fr_4_focus,
            R.string.phrase_es_1_translation,
            R.string.phrase_es_1_focus,
            R.string.phrase_es_2_translation,
            R.string.phrase_es_2_focus,
            R.string.phrase_es_3_translation,
            R.string.phrase_es_3_focus,
            R.string.phrase_es_4_translation,
            R.string.phrase_es_4_focus,
            R.string.phrase_de_1_translation,
            R.string.phrase_de_1_focus,
            R.string.phrase_de_2_translation,
            R.string.phrase_de_2_focus,
            R.string.phrase_de_3_translation,
            R.string.phrase_de_3_focus,
            R.string.phrase_ko_1_translation,
            R.string.phrase_ko_1_focus,
            R.string.phrase_ko_2_translation,
            R.string.phrase_ko_2_focus,
            R.string.phrase_ko_3_translation,
            R.string.phrase_ko_3_focus,
            R.string.phrase_zh_1_translation,
            R.string.phrase_zh_1_focus,
            R.string.phrase_zh_2_translation,
            R.string.phrase_zh_2_focus,
            R.string.phrase_zh_3_translation,
            R.string.phrase_zh_3_focus,
            R.string.phrase_it_1_translation,
            R.string.phrase_it_1_focus,
            R.string.phrase_it_2_translation,
            R.string.phrase_it_2_focus,
            R.string.phrase_it_3_translation,
            R.string.phrase_it_3_focus,
            R.string.phrase_en_1_translation,
            R.string.phrase_en_1_focus,
            R.string.phrase_en_2_translation,
            R.string.phrase_en_2_focus,
            R.string.scenario_tokyo_cafe_title,
            R.string.scenario_tokyo_cafe_sensei,
            R.string.scenario_tokyo_cafe_user,
            R.string.scenario_tokyo_cafe_init_translation,
            R.string.scenario_paris_boulangerie_title,
            R.string.scenario_paris_boulangerie_sensei,
            R.string.scenario_paris_boulangerie_user,
            R.string.scenario_paris_boulangerie_init_translation,
            R.string.scenario_madrid_tapas_title,
            R.string.scenario_madrid_tapas_sensei,
            R.string.scenario_madrid_tapas_user,
            R.string.scenario_madrid_tapas_init_translation,
            R.string.scenario_seoul_market_title,
            R.string.scenario_seoul_market_sensei,
            R.string.scenario_seoul_market_user,
            R.string.scenario_seoul_market_init_translation,
            R.string.scenario_rome_gelato_title,
            R.string.scenario_rome_gelato_sensei,
            R.string.scenario_rome_gelato_user,
            R.string.scenario_rome_gelato_init_translation,
            R.string.scenario_berlin_tech_title,
            R.string.scenario_berlin_tech_sensei,
            R.string.scenario_berlin_tech_user,
            R.string.scenario_berlin_tech_init_translation,
            R.string.onboarding_skip,
            R.string.onboarding_back,
            R.string.onboarding_next,
            R.string.onboarding_get_started,
            R.string.onboarding_page1_badge,
            R.string.onboarding_page1_title,
            R.string.onboarding_page1_subtitle,
            R.string.onboarding_page2_badge,
            R.string.onboarding_page2_title,
            R.string.onboarding_page2_subtitle,
            R.string.onboarding_page3_badge,
            R.string.onboarding_page3_title,
            R.string.onboarding_page3_subtitle,
            R.string.onboarding_select_language_label,
            R.string.profile_btn_replay_onboarding,
            R.string.cd_toggle_sensei_stage,
            R.string.daily_goal_title,
            R.string.daily_goal_completed_badge,
            R.string.donation_perks_title,
            R.string.donation_perks_summary,
            R.string.donation_recommended_tag,
            R.string.streak_dialog_title,
            R.string.streak_status_active_today,
            R.string.streak_status_pending_today,
            R.string.coach_record_pitch_sample,
            R.string.coach_stop_pitch_rec,
            R.string.coach_play_mine,
            R.string.coach_stop_mine,
            R.string.pitch_live_mic_badge,
            R.string.result_play_sensei,
            R.string.vocab_due_now_badge,
            R.string.vocab_btn_repeat,
            R.string.streak_freeze_saved_banner,
            R.string.shadowing_title,
            R.string.shadowing_subtitle,
            R.string.shadowing_speed_slow,
            R.string.shadowing_speed_normal,
            R.string.shadowing_btn_start,
            R.string.shadowing_btn_stop,
            R.string.shadowing_step_listening,
            R.string.shadowing_step_repeat,
            R.string.custom_phrase_btn,
            R.string.custom_phrase_target_label,
            R.string.custom_phrase_phonetic_label,
            R.string.custom_phrase_translation_label,
            R.string.custom_phrase_focus_label,
            R.string.custom_save_btn,
            R.string.custom_scenario_btn,
            R.string.custom_scenario_templates_label,
            R.string.custom_scenario_title_label,
            R.string.custom_scenario_sensei_role_label,
            R.string.custom_scenario_user_role_label,
            R.string.custom_scenario_opening_label,
            R.string.scenario_template_interview_title,
            R.string.scenario_template_interview_sensei,
            R.string.scenario_template_interview_user,
            R.string.scenario_template_interview_open,
            R.string.scenario_template_hotel_title,
            R.string.scenario_template_hotel_sensei,
            R.string.scenario_template_hotel_user,
            R.string.scenario_template_hotel_open,
            R.string.reminder_channel_name,
            R.string.reminder_channel_desc,
            R.string.reminder_notif_title,
            R.string.reminder_card_title,
            R.string.reminder_card_subtitle,
            R.string.reminder_time_label,
            R.string.reminder_permission_denied,
            R.string.share_card_btn,
            R.string.share_card_chooser,
            R.string.share_card_headline_score,
            R.string.share_card_headline_streak
        )

        for (locale in supportedLocales) {
            val config = Configuration(baseContext.resources.configuration)
            config.setLocale(locale)
            val localizedContext = baseContext.createConfigurationContext(config)
            val tag = locale.toLanguageTag()

            assertEquals("app_name mismatch for $tag", "Pocket Sensei", localizedContext.getString(R.string.app_name))

            for (resId in simpleStringIds) {
                val value = localizedContext.getString(resId)
                assertTrue("Blank string $resId in $tag", value.isNotBlank())
            }

            fun num(n: Int): String = String.format(locale, "%d", n)

            // Exercise every string with positional format arguments
            assertTrue(localizedContext.getString(R.string.streak_day_format, 5).contains(num(5)))
            assertTrue(localizedContext.getString(R.string.prompt_update_title, "2.0").contains("2.0"))
            assertTrue(localizedContext.getString(R.string.prompt_rate_body, 8, 12).contains(num(12)))
            assertTrue(localizedContext.getString(R.string.prompt_share_score_note, 94).contains(num(94)))
            assertTrue(localizedContext.getString(R.string.prompt_share_body, "94%").contains("94%"))
            assertTrue(localizedContext.getString(R.string.prompt_donation_body, 10).contains(num(10)))
            assertTrue(localizedContext.getString(R.string.share_app_text, "https://example.com").contains("https://example.com"))
            assertTrue(localizedContext.getString(R.string.coach_focus_format, "pitch").contains("pitch"))
            assertTrue(localizedContext.getString(R.string.coach_listening_format, "konnichiwa").contains("konnichiwa"))
            assertTrue(localizedContext.getString(R.string.coach_transcribed_format, "ohayo").contains("ohayo"))
            assertTrue(localizedContext.getString(R.string.coach_waveform_db_format, "8.4").contains("8.4"))
            assertTrue(localizedContext.getString(R.string.coach_eval_note_format, "cached").contains("cached"))
            assertTrue(localizedContext.getString(R.string.roleplay_thinking, "Barista").contains("Barista"))
            assertTrue(localizedContext.getString(R.string.roleplay_input_placeholder, "Japanese").contains("Japanese"))
            assertTrue(localizedContext.getString(R.string.roleplay_sensei_note, "Good tone").contains("Good tone"))
            assertTrue(localizedContext.getString(R.string.vocab_tracked_count, 14).contains(num(14)))
            assertTrue(localizedContext.getString(R.string.vocab_mastery_summary_format, 14, 6).contains(num(6)))
            assertTrue(localizedContext.getString(R.string.vocab_filter_needs_review, 4).contains(num(4)))
            assertTrue(localizedContext.getString(R.string.vocab_filter_learning, 3).contains(num(3)))
            assertTrue(localizedContext.getString(R.string.vocab_filter_mastered, 7).contains(num(7)))
            assertTrue(localizedContext.getString(R.string.vocab_level_format, 4).contains(num(4)))
            assertTrue(localizedContext.getString(R.string.vocab_practice_count_format, 9).contains(num(9)))
            assertTrue(localizedContext.getString(R.string.vocab_accuracy_format, 88).contains(num(88)))
            assertTrue(localizedContext.getString(R.string.profile_stat_streak_value, 7).contains(num(7)))
            assertTrue(localizedContext.getString(R.string.profile_stat_practice_value, 45).contains(num(45)))
            assertTrue(localizedContext.getString(R.string.profile_telemetry_counts, 12, 34).contains(num(34)))
            assertTrue(localizedContext.getString(R.string.session_accuracy_format, 91).contains(num(91)))
            assertTrue(localizedContext.getString(R.string.session_meta_format, "Japanese", 6).contains(num(6)))
            assertTrue(localizedContext.getString(R.string.league_xp_badge, 240).contains(num(240)))
            assertTrue(localizedContext.getString(R.string.league_empty_title, "French").contains("French"))
            assertTrue(localizedContext.getString(R.string.league_user_you, "Kenji").contains("Kenji"))
            assertTrue(localizedContext.getString(R.string.league_row_meta, "Gold", 4, 92).contains(num(92)))
            assertTrue(localizedContext.getString(R.string.ai_cached_count, 9).contains(num(9)))
            assertTrue(localizedContext.getString(R.string.ai_cloud_quota_summary, 3, 20).contains(num(20)))
            assertTrue(localizedContext.getString(R.string.blitz_hit_feedback, 90, 45, 3).contains(num(90)))
            assertTrue(localizedContext.getString(R.string.blitz_miss_feedback, 52).contains(num(52)))
            assertTrue(localizedContext.getString(R.string.blitz_timer_label, 30).contains(num(30)))
            assertTrue(localizedContext.getString(R.string.blitz_score_label, 120).contains(num(120)))
            assertTrue(localizedContext.getString(R.string.blitz_combo_label, 4).contains(num(4)))
            assertTrue(localizedContext.getString(R.string.blitz_times_up, 180).contains(num(180)))
            assertTrue(localizedContext.getString(R.string.blitz_summary, 6).contains(num(6)))
            assertTrue(localizedContext.getString(R.string.support_inactive_desc, 15).contains(num(15)))
            assertTrue(localizedContext.getString(R.string.cd_paypal_qr, "$5").contains("$5"))
            assertTrue(localizedContext.getString(R.string.billing_error_fallback, "unavailable").contains("unavailable"))
            assertTrue(localizedContext.getString(R.string.billing_store_unavailable, "code 3").contains("code 3"))
            assertTrue(localizedContext.getString(R.string.billing_code_prefix, 3).contains(num(3)))
            assertTrue(localizedContext.getString(R.string.billing_item_unpublished, "Coffee").contains("Coffee"))
            assertTrue(localizedContext.getString(R.string.billing_checkout_error, "err").contains("err"))
            assertTrue(localizedContext.getString(R.string.paypal_copied_toast, "https://paypal.me/x").contains("https://paypal.me/x"))
            assertTrue(localizedContext.getString(R.string.paypal_share_text, "https://paypal.me/x").contains("https://paypal.me/x"))
            assertTrue(localizedContext.getString(R.string.kofi_copied_toast, "https://ko-fi.com/x").contains("https://ko-fi.com/x"))
            assertTrue(localizedContext.getString(R.string.percent_format, 88).contains(num(88)))
            assertTrue(localizedContext.getString(R.string.donation_usd_preset_format, "5").contains("5"))
            assertTrue(localizedContext.getString(R.string.eval_tip_missed_format, "konnichiwa").contains("konnichiwa"))
            assertTrue(localizedContext.getString(R.string.eval_contour_type_format, "Spanish").contains("Spanish"))
            assertTrue(localizedContext.getString(R.string.roleplay_offline_note_format, "hola").contains("hola"))
            assertTrue(localizedContext.getString(R.string.roleplay_offline_reply_default, "German").contains("German"))
            assertTrue(localizedContext.getString(R.string.onboarding_step_format, 1, 3).contains(num(1)))
            assertTrue(localizedContext.getString(R.string.onboarding_step_format, 1, 3).contains(num(3)))
            assertTrue(localizedContext.getString(R.string.daily_goal_progress_format, 2, 3).contains(num(2)))
            assertTrue(localizedContext.getString(R.string.daily_goal_progress_format, 2, 3).contains(num(3)))
            assertTrue(localizedContext.getString(R.string.streak_best_format, 14).contains(num(14)))
            assertTrue(localizedContext.getString(R.string.streak_new_day_banner_format, 6).contains(num(6)))
            assertTrue(localizedContext.getString(R.string.vocab_filter_due_today, 5).contains(num(5)))
            assertTrue(localizedContext.getString(R.string.vocab_next_interval_format, 7).contains(num(7)))
            assertTrue(localizedContext.getString(R.string.vocab_next_review_hint_format, 14).contains(num(14)))
            assertTrue(localizedContext.getString(R.string.streak_freeze_count_format, 2).contains(num(2)))
            val extraNonFormatIds = listOf(
                R.string.difficulty_custom,
                R.string.ai_byok_get_key_btn,
                R.string.topic_pack_generate_btn,
                R.string.roleplay_live_call_start,
                R.string.roleplay_live_call_active,
                R.string.topic_pack_phrase2_trans,
                R.string.topic_pack_phrase3_trans,
                R.string.topic_pack_sensei_role,
                R.string.topic_pack_user_role,
                R.string.scenario_shanghai_tea_title,
                R.string.scenario_shanghai_tea_sensei,
                R.string.scenario_shanghai_tea_user,
                R.string.scenario_shanghai_tea_init_translation,
                R.string.scenario_london_interview_title,
                R.string.scenario_london_interview_sensei,
                R.string.scenario_london_interview_user,
                R.string.scenario_london_interview_init_translation
            )
            for (resId in extraNonFormatIds) {
                val value = localizedContext.getString(resId)
                assertTrue("Blank string $resId in $tag", value.isNotBlank())
            }
            assertTrue(localizedContext.getString(R.string.topic_pack_phrase1_trans, "Ramen").contains("Ramen"))
            assertTrue(localizedContext.getString(R.string.custom_phrase_dialog_title, "Japanese").contains("Japanese"))
            assertTrue(localizedContext.getString(R.string.custom_scenario_dialog_title, "French").contains("French"))
            assertTrue(localizedContext.getString(R.string.reminder_notif_body_format, 9).contains(num(9)))
            assertTrue(localizedContext.getString(R.string.reminder_hour_format, 20).contains(num(20)))
            assertTrue(localizedContext.getString(R.string.share_card_streak_format, 12).contains(num(12)))
        }
    }

    @Test
    fun `store listing csv covers all 21 locales and obeys play character limits`() {
        val csvFile = listOf(
            File("store_listing.csv"),
            File("../store_listing.csv")
        ).firstOrNull { it.exists() }
            ?: error("Could not locate store_listing.csv from ${File(".").absolutePath}")

        val rows = parseCsv(csvFile.readText(Charsets.UTF_8))
        assertEquals("Expected header + 21 locale rows", 22, rows.size)

        val header = rows.first()
        assertEquals(listOf("Locale", "App Name", "Short Description", "Full Description"), header)

        val seenLocales = mutableSetOf<String>()
        for (row in rows.drop(1)) {
            assertEquals("Expected 4 columns for ${row.firstOrNull()}", 4, row.size)
            val locale = row[0]
            val appName = row[1]
            val shortDesc = row[2]
            val fullDesc = row[3]

            assertTrue("Duplicate locale $locale", seenLocales.add(locale))
            assertTrue("App Name empty for $locale", appName.isNotBlank())
            assertTrue("App Name > 30 chars for $locale (${appName.length}): $appName", appName.length <= 30)
            assertFalse("App Name contains promo 'Free' for $locale", appName.contains("Free", ignoreCase = true))
            assertFalse("App Name contains promo 'Best' for $locale", appName.contains("Best", ignoreCase = true))
            assertFalse("App Name contains '#1' for $locale", appName.contains("#1"))
            assertTrue("Short Description empty for $locale", shortDesc.isNotBlank())
            assertTrue("Short Description > 80 chars for $locale (${shortDesc.length}): $shortDesc", shortDesc.length <= 80)
            assertTrue("Full Description empty for $locale", fullDesc.isNotBlank())
            assertTrue("Full Description > 4000 chars for $locale (${fullDesc.length})", fullDesc.length <= 4000)
        }
    }

    @Test
    fun `root and asset privacy policy html files stay in sync`() {
        val rootPolicy = listOf(File("privacy_policy.html"), File("../privacy_policy.html")).firstOrNull { it.exists() }
        val assetPolicy = listOf(
            File("app/src/main/assets/privacy_policy.html"),
            File("src/main/assets/privacy_policy.html")
        ).firstOrNull { it.exists() }

        assertTrue("Root privacy_policy.html should exist", rootPolicy != null)
        assertTrue("Asset privacy_policy.html should exist", assetPolicy != null)
        assertEquals(rootPolicy!!.readText(Charsets.UTF_8), assetPolicy!!.readText(Charsets.UTF_8))
    }

    private fun parseCsv(input: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < input.length && input[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    ',' -> {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                        if (currentRow.any { it.isNotEmpty() }) {
                            rows.add(currentRow.toList())
                        }
                        currentRow.clear()
                    }
                    '\r' -> Unit
                    else -> currentField.append(ch)
                }
            }
            i++
        }
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            rows.add(currentRow.toList())
        }
        return rows
    }
}
