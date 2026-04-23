package com.skydown.android

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiHubAppFlowTest {
    @get:Rule
    val activityRule = ActivityScenarioRule<MainActivity>(
        Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_UI_TEST_SKIP_INTRO, true)
            putExtra(MainActivity.EXTRA_UI_TEST_START_ROUTE, "ai")
            putExtra(MainActivity.EXTRA_UI_TEST_SIGNED_IN_USER, true)
            putExtra(MainActivity.EXTRA_UI_TEST_USE_MOCK_AI_VISUAL, true)
        },
    )

    @Test
    fun aiHubVisualPromptShowsGeneratedImageFromAppFlow() {
        val visualModeButton = waitForNode(timeoutMillis = 15_000) { node ->
            node.text?.toString() == "Visual" && node.findClickableNode() != null
        }
        assertNotNull("Visual mode button should be visible", visualModeButton)
        assertTrue(
            "Visual mode button should be clickable",
            visualModeButton!!.findClickableNode()
                ?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true,
        )

        val promptField = waitForNode(timeoutMillis = 15_000) { node ->
            node.className?.toString() == "android.widget.EditText"
        }
        assertNotNull("Visual prompt field should be visible", promptField)
        val inputArguments = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                "Cinematic artist portrait with moody lighting",
            )
        }
        assertTrue(
            "Visual prompt field should accept text",
            promptField!!.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, inputArguments),
        )

        val generateButton = waitForNode(timeoutMillis = 15_000) { node ->
            node.text?.toString() == "Rendern" && node.findClickableNode() != null
        }
        assertNotNull("Generate visual button should be visible", generateButton)
        assertTrue(
            "Generate visual button should be clickable",
            generateButton!!.findClickableNode()
                ?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true,
        )

        val generatedVisual = waitForNode(timeoutMillis = 15_000) { node ->
            node.contentDescription?.toString() == "Generiertes Visual"
        }
        assertNotNull("Generated visual should be visible", generatedVisual)

        val saveButton = waitForNodeWithScroll(timeoutMillis = 15_000) { node ->
            node.text?.toString() == "Speichern" && node.findClickableNode() != null
        }
        assertNotNull("Save image button should be visible", saveButton)
    }

    private fun waitForNode(
        timeoutMillis: Long,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val deadline = SystemClock.uptimeMillis() + timeoutMillis

        while (SystemClock.uptimeMillis() < deadline) {
            uiAutomation.rootInActiveWindow
                ?.findMatchingNode(predicate)
                ?.let { return it }
            SystemClock.sleep(250)
        }

        return null
    }

    private fun waitForNodeWithScroll(
        timeoutMillis: Long,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        var nextScrollAt = 0L

        while (SystemClock.uptimeMillis() < deadline) {
            val root = uiAutomation.rootInActiveWindow
            root?.findMatchingNode(predicate)?.let { return it }

            val now = SystemClock.uptimeMillis()
            if (now >= nextScrollAt) {
                root?.findScrollableNode()?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                nextScrollAt = now + 600
            }
            SystemClock.sleep(250)
        }

        return null
    }

    private fun AccessibilityNodeInfo.findMatchingNode(
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        if (predicate(this)) return this

        for (index in 0 until childCount) {
            getChild(index)?.findMatchingNode(predicate)?.let { return it }
        }

        return null
    }

    private fun AccessibilityNodeInfo.findScrollableNode(): AccessibilityNodeInfo? {
        if (isScrollable) return this

        for (index in 0 until childCount) {
            getChild(index)?.findScrollableNode()?.let { return it }
        }

        return null
    }

    private fun AccessibilityNodeInfo.findClickableNode(): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = this
        while (current != null) {
            if (current.isClickable) {
                return current
            }
            current = current.parent
        }

        return null
    }
}
