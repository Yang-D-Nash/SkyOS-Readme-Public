package com.skydown.android.ui.model

enum class SettingsLegalDocumentType {
    PrivacyPolicy,
    TermsOfService,
}

data class SettingsLegalDocument(
    val title: String,
    val updatedAt: String,
    val introduction: String,
    val sections: List<SettingsLegalSection>,
)

data class SettingsLegalSection(
    val title: String,
    val body: String,
)

fun SettingsLegalDocumentType.resolve(): SettingsLegalDocument {
    return when (this) {
        SettingsLegalDocumentType.PrivacyPolicy -> SettingsLegalDocument(
            title = "Datenschutzbestimmungen",
            updatedAt = "29. Juli 2025",
            introduction = "Diese Datenschutzbestimmungen erklaeren, wie Skydown x 22 Informationen von und ueber Nutzer verarbeitet, wenn die App verwendet wird.",
            sections = listOf(
                SettingsLegalSection(
                    title = "1. Informationen, die wir erfassen",
                    body = "Wir erfassen Kontoinformationen wie E-Mail-Adresse und Passwort, technische Geraetedaten sowie Nutzungsdaten zu Musik, Shop, Beat Hub, Einstellungen und KI-Bereichen.",
                ),
                SettingsLegalSection(
                    title = "2. Wie wir Ihre Informationen verwenden",
                    body = "Die Daten werden fuer Konto- und Authentifizierungsprozesse, den Betrieb der App, Personalisierung, Support, Bestellungen, Sicherheitspruefungen und die Weiterentwicklung der App verwendet.",
                ),
                SettingsLegalSection(
                    title = "3. Weitergabe Ihrer Informationen",
                    body = "Informationen werden nur weitergegeben, wenn dies fuer den Betrieb der App erforderlich ist, wenn Sie eingewilligt haben oder wenn wir gesetzlich dazu verpflichtet sind. Dazu koennen Hosting-, Datenbank-, Speicher-, Analyse- und Support-Dienste gehoeren.",
                ),
                SettingsLegalSection(
                    title = "4. Datensicherheit",
                    body = "Wir setzen angemessene technische und organisatorische Massnahmen ein, um persoenliche Daten vor unbefugtem Zugriff, Veraenderung oder Verlust zu schuetzen. Eine vollstaendig risikofreie digitale Uebertragung kann jedoch nicht garantiert werden.",
                ),
                SettingsLegalSection(
                    title = "5. Ihre Rechte",
                    body = "Je nach Wohnsitz koennen Sie Auskunft, Berichtigung, Loeschung oder Widerspruch verlangen. Anfragen dazu koennen an skydownent@gmail.com gerichtet werden.",
                ),
                SettingsLegalSection(
                    title = "6. Aenderungen",
                    body = "Wesentliche Aenderungen dieser Datenschutzbestimmungen werden in der App kenntlich gemacht und mit einem aktualisierten Datum versehen.",
                ),
            ),
        )
        SettingsLegalDocumentType.TermsOfService -> SettingsLegalDocument(
            title = "Nutzungsbedingungen",
            updatedAt = "29. Juli 2025",
            introduction = "Diese Nutzungsbedingungen regeln den Zugriff auf Skydown x 22 und die Nutzung der Inhalte, Services und App-Funktionen.",
            sections = listOf(
                SettingsLegalSection(
                    title = "1. Annahme der Bedingungen",
                    body = "Mit der Nutzung der App bestaetigen Sie, dass Sie diese Bedingungen gelesen haben und akzeptieren.",
                ),
                SettingsLegalSection(
                    title = "2. Aenderungen der Bedingungen",
                    body = "Wir koennen diese Bedingungen aktualisieren. Die weitere Nutzung der App nach einer Aktualisierung gilt als Zustimmung zur aktuellen Version.",
                ),
                SettingsLegalSection(
                    title = "3. Zugriff und Nutzung der App",
                    body = "Bestimmte Funktionen erfordern ein Konto. Sie sind fuer Ihre Zugangsdaten verantwortlich. Die App darf nur rechtmaessig und ohne Missbrauch oder Manipulation verwendet werden.",
                ),
                SettingsLegalSection(
                    title = "4. Geistiges Eigentum",
                    body = "Die App sowie Inhalte wie Marken, Designs, Texte, Bilder, Audio- und Videoinhalte bleiben Eigentum von Skydown x 22, Skydown Entertainment, beteiligten Kuenstlern oder den jeweiligen Rechteinhabern.",
                ),
                SettingsLegalSection(
                    title = "5. Haftungsausschluss und Haftungsbeschraenkung",
                    body = "Die App wird in ihrer aktuellen Form bereitgestellt. Soweit gesetzlich zulaessig, haften wir nicht fuer direkte oder indirekte Schaeden, die aus der Nutzung oder Nichtverfuegbarkeit der App entstehen.",
                ),
                SettingsLegalSection(
                    title = "6. Sperrung und geltendes Recht",
                    body = "Wir koennen Konten oder Zugriffe einschraenken, wenn gegen diese Bedingungen verstossen wird. Es gilt das Recht der Bundesrepublik Deutschland.",
                ),
            ),
        )
    }
}
