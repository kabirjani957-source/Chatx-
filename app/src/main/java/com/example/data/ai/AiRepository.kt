package com.example.data.ai

import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.delay

data class AiChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class AiResponse(
    val text: String,
    val isPlaceholder: Boolean = true
)

/**
 * Abstraction for ChatX AI Assistant.
 * Backed by PlaceholderAiRepository while Firebase AI Logic is disabled.
 */
interface AiRepository {
    suspend fun sendMessage(
        history: List<AiChatMessage>,
        userPrompt: String,
        language: AppLanguage
    ): Result<AiResponse>
}

class PlaceholderAiRepository : AiRepository {
    override suspend fun sendMessage(
        history: List<AiChatMessage>,
        userPrompt: String,
        language: AppLanguage
    ): Result<AiResponse> {
        delay(550)
        val lower = userPrompt.lowercase()
        val body = when {
            "hindi" in lower || "हिन्दी" in lower ->
                "[Offline Sample Response]\nनमस्ते! मैं ChatX AI सहायक हूँ। आप इस संदेश को इस प्रकार भेज सकते हैं:\n\"नमस्ते, क्या हम आज शाम बात कर सकते हैं?\"\n\n(Note: Live Gemini responses require enabling Firebase AI Logic.)"

            "odia" in lower || "ଓଡ଼ିଆ" in lower ->
                "[Offline Sample Response]\nନମସ୍କାର! ମୁଁ ChatX AI ସହାୟକ। ଆପଣ ଏହି ବାର୍ତ୍ତା ପଠାଇପାରିବେ:\n\"ନମସ୍କାର, ଆଜି ସନ୍ଧ୍ୟାରେ କଥା ହେବା କି?\"\n\n(Note: Live Gemini responses require enabling Firebase AI Logic.)"

            "draft" in lower || "reply" in lower || "message" in lower ->
                "[Offline Sample Response]\nHere is a polished quick message draft for you:\n\"Hey! Just checking in — let me know when you're free for a quick call on ChatX.\"\n\n(Note: Live Gemini responses require enabling Firebase AI Logic.)"

            "summarize" in lower || "summary" in lower ->
                "[Offline Sample Response]\nSummary Preview:\n• Key topic: Coordinating ChatX features & availability\n• Action item: Confirm meeting time and share document\n\n(Note: Live Gemini responses require enabling Firebase AI Logic.)"

            else -> when (language) {
                AppLanguage.HINDI ->
                    "[ऑफ़लाइन नमूना उत्तर]\nआपने पूछा: \"$userPrompt\"\n\nलाइव Gemini AI उत्तर प्राप्त करने के लिए Firebase AI Logic सक्षम करें।"
                AppLanguage.ODIA ->
                    "[ଅଫଲାଇନ୍ ନମୁନା ଉତ୍ତର]\nଆପଣ ପଚାରିଲେ: \"$userPrompt\"\n\nଲାଇଭ୍ Gemini AI ଉତ୍ତର ପାଇଁ Firebase AI Logic ସକ୍ଷମ କରନ୍ତୁ।"
                AppLanguage.ENGLISH ->
                    "[Offline Sample Response]\nYou asked: \"$userPrompt\"\n\nChatX AI can help draft replies, translate between English, Hindi, and Odia, or summarize chats once Firebase AI Logic is enabled."
            }
        }
        return Result.success(AiResponse(text = body, isPlaceholder = true))
    }
}
