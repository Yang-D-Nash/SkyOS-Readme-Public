package com.skydown.shared.usecase

import com.skydown.shared.model.LoginInput
import com.skydown.shared.model.MerchandiseItem
import com.skydown.shared.model.MerchandiseVariant
import com.skydown.shared.model.RegistrationConsentInput
import com.skydown.shared.model.RegistrationInput
import com.skydown.shared.text.SharedText
import com.skydown.shared.text.formatTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthAndVariantUseCaseTest {
    @Test
    fun validateRegistration_returnsNull_forValidInput() {
        val input = registrationInput()

        assertNull(AuthValidation.validateRegistration(input))
    }

    @Test
    fun validateLogin_returnsMessage_whenCredentialsMissing() {
        val input = LoginInput(email = "", password = " ")

        assertEquals(
            SharedText.AUTH_LOGIN_EMAIL_PASSWORD_REQUIRED,
            AuthValidation.validateLogin(input),
        )
    }

    @Test
    fun validateRegistration_returnsMessage_whenPasswordsDoNotMatch() {
        val input = registrationInput(confirmPassword = "secret321")

        assertEquals(
            SharedText.AUTH_REGISTER_PASSWORD_MISMATCH,
            AuthValidation.validateRegistration(input),
        )
    }

    @Test
    fun validateRegistrationConsent_returnsMessage_whenTermsNotAccepted() {
        val consent = registrationConsent(acceptedTerms = false)

        assertEquals(
            SharedText.AUTH_REGISTER_CONSENT_REQUIRED,
            AuthValidation.validateRegistrationConsent(consent),
        )
    }

    @Test
    fun resolveVariant_returnsMatchedVariant_whenSizeAndColorMatchIgnoringCase() {
        val expected = MerchandiseVariant(id = "v1", size = "M", color = "Black")
        val item = merchandiseItemWithVariants(
            expected,
            MerchandiseVariant(id = "v2", size = "L", color = "Black"),
        )

        val result = MerchandiseVariantResolver.resolveVariant(
            item = item,
            size = " m ",
            color = " black ",
        )

        assertTrue(result.isSuccess)
        assertEquals("v1", result.getOrNull()?.id)
    }

    @Test
    fun resolveVariant_returnsFailure_whenNoVariantMatches() {
        val item = merchandiseItemWithVariants(
            MerchandiseVariant(id = "v1", size = "S", color = "Blue"),
        )

        val result = MerchandiseVariantResolver.resolveVariant(
            item = item,
            size = "M",
            color = "Blue",
        )

        assertTrue(result.isFailure)
        assertEquals(
            SharedText.MERCH_VARIANT_NOT_FOUND.formatTemplate("M", " / Blue"),
            result.exceptionOrNull()?.message,
        )
    }

    private fun merchandiseItemWithVariants(vararg variants: MerchandiseVariant): MerchandiseItem {
        return MerchandiseItem(
            name = "Premium Hoodie",
            price = 69.0,
            description = "Heavyweight edition",
            imageUrls = emptyList(),
            available = true,
            variants = variants.toList(),
        )
    }

    private fun registrationInput(confirmPassword: String = "secret123"): RegistrationInput {
        return RegistrationInput(
            username = "sky",
            email = "sky@example.com",
            whatsApp = "+491234567",
            password = "secret123",
            confirmPassword = confirmPassword,
            consent = registrationConsent(),
        )
    }

    private fun registrationConsent(
        acceptedTerms: Boolean = true,
        acceptedPrivacyPolicy: Boolean = true,
        legalVersionLabel: String = "v1",
        consentSource: String = "signup-screen",
    ): RegistrationConsentInput {
        return RegistrationConsentInput(
            acceptedTerms = acceptedTerms,
            acceptedPrivacyPolicy = acceptedPrivacyPolicy,
            legalVersionLabel = legalVersionLabel,
            consentSource = consentSource,
        )
    }
}
