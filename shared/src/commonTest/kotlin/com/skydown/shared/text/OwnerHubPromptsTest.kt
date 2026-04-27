package com.skydown.shared.text

import kotlin.test.Test
import kotlin.test.assertTrue

class OwnerHubPromptsTest {
    @Test
    fun dailyBriefing_containsExpectedStructure() {
        val prompt = OwnerHubPrompts.dailyBriefing
        assertTrue(prompt.isNotBlank())
        assertTrue(prompt.contains("SkyOS Daily Briefing", ignoreCase = true))
        assertTrue(prompt.contains("Executive summary", ignoreCase = true))
        assertTrue(prompt.contains("Risks", ignoreCase = true))
        assertTrue(prompt.contains("Recommended actions", ignoreCase = true))
        assertTrue(prompt.contains("Health / Growth / Quality / Release", ignoreCase = true))
        assertTrue(prompt.contains("Constraints:", ignoreCase = true))
    }
}
