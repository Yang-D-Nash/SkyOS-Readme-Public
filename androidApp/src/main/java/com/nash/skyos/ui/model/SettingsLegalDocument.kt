package com.nash.skyos.ui.model

import com.nash.skyos.R
import com.nash.skyos.data.AppTextResolver
import com.nash.skyos.data.LegalContentSettings

enum class SettingsLegalDocumentType {
    ReadmeGuide,
    PrivacyPolicy,
    TermsAndConditions,
    TermsOfService,
    SubscriptionTerms,
    AiUsageNotice,
    ImprintInfo,
}

data class SettingsLegalDocument(
    val title: String,
    val updatedAt: String,
    val introduction: String,
    val sections: List<SettingsLegalSection>,
    val contactEmail: String,
)

data class SettingsLegalSection(
    val title: String,
    val body: String,
)

fun SettingsLegalDocumentType.resolve(
    legalContent: LegalContentSettings = LegalContentSettings(),
): SettingsLegalDocument {
    val brandName = legalContent.resolvedBrandName
    val operatorName = legalContent.resolvedOperatorName
    val rightsHolderName = legalContent.resolvedRightsHolderName
    val supportEmail = legalContent.resolvedSupportEmail
    val lastUpdatedLabel = legalContent.resolvedLastUpdatedLabel
    val imprintReference = legalContent.resolvedImprintReference
    val masterNumberMeaning = legalContent.resolvedMasterNumberMeaning
    val brandManifesto = legalContent.resolvedBrandManifesto
    val symbolicNumericCode = legalContent.resolvedSymbolicNumericCode
    val symbolicLeetCode = legalContent.resolvedSymbolicLeetCode
    val symbolicCodeExplanation = legalContent.resolvedSymbolicCodeExplanation
    fun s(resId: Int, vararg args: Any): String = AppTextResolver.string(resId, *args)

    return when (this) {
        SettingsLegalDocumentType.ReadmeGuide -> SettingsLegalDocument(
            title = s(R.string.legal_readme_title),
            updatedAt = lastUpdatedLabel,
            introduction = s(R.string.legal_readme_intro, brandName),
            sections = listOf(
                SettingsLegalSection(
                    title = s(R.string.legal_readme_s1_title),
                    body = s(R.string.legal_readme_s1_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_readme_s2_title),
                    body = s(R.string.legal_readme_s2_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_readme_s3_title),
                    body = s(R.string.legal_readme_s3_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_readme_s4_title),
                    body = s(R.string.legal_readme_s4_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_readme_s5_title),
                    body = s(R.string.legal_readme_s5_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_readme_s6_title),
                    body = s(R.string.legal_readme_s6_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_readme_s7_title),
                    body = s(R.string.legal_readme_s7_body, operatorName, imprintReference),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_readme_s8_title),
                    body = s(
                        R.string.legal_readme_s8_body,
                        masterNumberMeaning,
                        brandManifesto,
                        symbolicNumericCode,
                        symbolicLeetCode,
                        symbolicCodeExplanation,
                    ),
                ),
            ),
            contactEmail = supportEmail,
        )
        SettingsLegalDocumentType.PrivacyPolicy -> SettingsLegalDocument(
            title = s(R.string.legal_privacy_title),
            updatedAt = lastUpdatedLabel,
            introduction = s(R.string.legal_privacy_intro, brandName, brandName),
            sections = listOf(
                SettingsLegalSection(
                    title = s(R.string.legal_privacy_s1_title),
                    body = s(R.string.legal_privacy_s1_body, operatorName, supportEmail, imprintReference),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_privacy_s2_title),
                    body = s(R.string.legal_privacy_s2_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_privacy_s3_title),
                    body = s(R.string.legal_privacy_s3_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_privacy_s4_title),
                    body = s(R.string.legal_privacy_s4_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_privacy_s5_title),
                    body = s(R.string.legal_privacy_s5_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_privacy_s6_title),
                    body = s(R.string.legal_privacy_s6_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_privacy_s7_title),
                    body = s(R.string.legal_privacy_s7_body),
                ),
            ),
            contactEmail = supportEmail,
        )
        SettingsLegalDocumentType.TermsAndConditions -> SettingsLegalDocument(
            title = s(R.string.legal_tc_title),
            updatedAt = lastUpdatedLabel,
            introduction = s(R.string.legal_tc_intro, brandName),
            sections = listOf(
                SettingsLegalSection(
                    title = s(R.string.legal_tc_s1_title),
                    body = s(R.string.legal_tc_s1_body, operatorName, rightsHolderName, supportEmail, imprintReference),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_tc_s2_title),
                    body = s(R.string.legal_tc_s2_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_tc_s3_title),
                    body = s(R.string.legal_tc_s3_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_tc_s4_title),
                    body = s(R.string.legal_tc_s4_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_tc_s5_title),
                    body = s(R.string.legal_tc_s5_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_tc_s6_title),
                    body = s(R.string.legal_tc_s6_body),
                ),
            ),
            contactEmail = supportEmail,
        )
        SettingsLegalDocumentType.TermsOfService -> SettingsLegalDocument(
            title = s(R.string.legal_tos_title),
            updatedAt = lastUpdatedLabel,
            introduction = s(R.string.legal_tos_intro, brandName, brandName),
            sections = listOf(
                SettingsLegalSection(
                    title = s(R.string.legal_tos_s1_title),
                    body = s(R.string.legal_tos_s1_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_tos_s2_title),
                    body = s(R.string.legal_tos_s2_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_tos_s3_title),
                    body = s(R.string.legal_tos_s3_body, rightsHolderName, rightsHolderName),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_tos_s4_title),
                    body = s(R.string.legal_tos_s4_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_tos_s5_title),
                    body = s(R.string.legal_tos_s5_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_tos_s6_title),
                    body = s(R.string.legal_tos_s6_body, operatorName, supportEmail),
                ),
            ),
            contactEmail = supportEmail,
        )
        SettingsLegalDocumentType.SubscriptionTerms -> SettingsLegalDocument(
            title = s(R.string.legal_subscription_title),
            updatedAt = lastUpdatedLabel,
            introduction = s(R.string.legal_subscription_intro, brandName),
            sections = listOf(
                SettingsLegalSection(
                    title = s(R.string.legal_subscription_s1_title),
                    body = s(R.string.legal_subscription_s1_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_subscription_s2_title),
                    body = s(R.string.legal_subscription_s2_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_subscription_s3_title),
                    body = s(R.string.legal_subscription_s3_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_subscription_s4_title),
                    body = s(R.string.legal_subscription_s4_body),
                ),
            ),
            contactEmail = supportEmail,
        )
        SettingsLegalDocumentType.AiUsageNotice -> SettingsLegalDocument(
            title = s(R.string.legal_ai_notice_title),
            updatedAt = lastUpdatedLabel,
            introduction = s(R.string.legal_ai_notice_intro, brandName),
            sections = listOf(
                SettingsLegalSection(
                    title = s(R.string.legal_ai_notice_s1_title),
                    body = s(R.string.legal_ai_notice_s1_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_ai_notice_s2_title),
                    body = s(R.string.legal_ai_notice_s2_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_ai_notice_s3_title),
                    body = s(R.string.legal_ai_notice_s3_body),
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_ai_notice_s4_title),
                    body = s(R.string.legal_ai_notice_s4_body),
                ),
            ),
            contactEmail = supportEmail,
        )
        SettingsLegalDocumentType.ImprintInfo -> SettingsLegalDocument(
            title = s(R.string.legal_imprint_title),
            updatedAt = lastUpdatedLabel,
            introduction = s(R.string.legal_imprint_intro, brandName),
            sections = listOf(
                SettingsLegalSection(
                    title = s(R.string.legal_imprint_s1_title),
                    body = operatorName,
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_imprint_s2_title),
                    body = rightsHolderName,
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_imprint_s3_title),
                    body = imprintReference,
                ),
                SettingsLegalSection(
                    title = s(R.string.legal_imprint_s4_title),
                    body = s(R.string.legal_imprint_s4_body),
                ),
            ),
            contactEmail = supportEmail,
        )
    }
}
