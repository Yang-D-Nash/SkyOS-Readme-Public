package com.skydown.shared.text

import kotlin.test.Test
import kotlin.test.assertEquals

class TemplateFormatTest {
    @Test
    fun formatTemplate_replacesIndexedPlaceholders_inOrder() {
        val template = "Hello %1\$s, you have %2\$d items."

        val result = template.formatTemplate("Sky", 3)

        assertEquals("Hello Sky, you have 3 items.", result)
    }

    @Test
    fun formatTemplate_usesEmptyString_whenArgumentMissing() {
        val template = "Missing arg: '%2\$s'"

        val result = template.formatTemplate("first")

        assertEquals("Missing arg: ''", result)
    }

    @Test
    fun formatTemplate_keepsPlaceholder_whenIndexIsInvalid() {
        val template = "Invalid index %x\$s stays untouched."

        val result = template.formatTemplate("value")

        assertEquals("Invalid index %x\$s stays untouched.", result)
    }
}
