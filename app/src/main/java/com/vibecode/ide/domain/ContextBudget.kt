package com.vibecode.ide.domain

import com.vibecode.ide.domain.model.ChatMessage

/**
 * Keeps outgoing requests inside a model's context window.
 *
 * Uses a character-based approximation (~4 chars/token) rather than a real
 * tokenizer — good enough to stay safely under budget without adding a new
 * dependency. Swap in a real tokenizer (e.g. JTokkit, which is pure-Java and
 * Android-compatible) later if you want exact counts.
 */
object ContextBudget {
    private const val MIN_OUTPUT_TOKENS = 1024
    private const val MAX_OUTPUT_TOKENS = 8192
    private const val OUTPUT_RESERVE_FRACTION = 0.25
    private const val MIN_INPUT_BUDGET = 512

    data class Plan(val history: List<ChatMessage>, val maxOutputTokens: Int)

    fun estimateTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)

    /**
     * Reserves a slice of the context window for the model's reply, then keeps as many of
     * the most recent messages as fit in what's left — oldest messages are dropped first.
     * The newest message is always kept even if it alone exceeds budget (better to try than
     * to send nothing).
     */
    fun plan(contextWindow: Int, systemPrompt: String, history: List<ChatMessage>): Plan {
        val reservedOutput = (contextWindow * OUTPUT_RESERVE_FRACTION).toInt()
            .coerceIn(MIN_OUTPUT_TOKENS, MAX_OUTPUT_TOKENS)
        val inputBudget = (contextWindow - reservedOutput - estimateTokens(systemPrompt))
            .coerceAtLeast(MIN_INPUT_BUDGET)

        val kept = ArrayDeque<ChatMessage>()
        var used = 0
        for (msg in history.asReversed()) {
            val t = estimateTokens(msg.content)
            if (used + t > inputBudget && kept.isNotEmpty()) break
            kept.addFirst(msg)
            used += t
        }
        return Plan(kept.toList(), reservedOutput)
    }
}
