package com.jabir.brot.domain.model

/**
 * Categorical confidence level derived from a 0.0–1.0 score.
 */
enum class ConfidenceLevel(val label: String) {
    HIGH("High confidence"),
    MEDIUM("Medium confidence"),
    LOW("Low confidence");

    companion object {
        /**
         * Maps a continuous confidence score to a categorical level.
         *
         * @param score A value in the range [0.0, 1.0].
         * @return The corresponding [ConfidenceLevel].
         */
        fun fromScore(score: Float): ConfidenceLevel = when {
            score > 0.8f -> HIGH
            score >= 0.5f -> MEDIUM
            else -> LOW
        }
    }
}
