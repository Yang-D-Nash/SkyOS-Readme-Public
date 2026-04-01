package com.skydown.android.ui.model

enum class SettingsLegalDocumentType {
    PrivacyPolicy,
    TermsAndConditions,
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
            updatedAt = "2. April 2026",
            introduction = "Diese Datenschutzbestimmungen informieren darueber, wie Skydown x 22 personenbezogene Daten verarbeitet, wenn die App, das Konto, der Shop, Medienfunktionen, KI-Funktionen oder Support-Kanaele genutzt werden.",
            sections = listOf(
                SettingsLegalSection(
                    title = "1. Verantwortlicher",
                    body = "Verantwortlich fuer die Datenverarbeitung innerhalb der App ist Skydown x 22. Datenschutzanfragen koennen an skydownent@gmail.com gerichtet werden.",
                ),
                SettingsLegalSection(
                    title = "2. Welche Daten verarbeitet werden",
                    body = "Verarbeitet werden koennen insbesondere Kontodaten, freiwillige Profilangaben, technische Geraete- und Fehlerdaten, Nutzungsdaten zu Musik-, Video-, Shop-, KI- und Einstellungsbereichen sowie Bestell-, Versand- und Supportdaten.",
                ),
                SettingsLegalSection(
                    title = "3. Zwecke und Rechtsgrundlagen",
                    body = "Die Verarbeitung erfolgt insbesondere zur Bereitstellung der App und Ihres Kontos, zur Abwicklung von Bestellungen, Versand, Rechnungen und Support auf Grundlage von Art. 6 Abs. 1 lit. b DSGVO, zur Erfuellung gesetzlicher Pflichten auf Grundlage von Art. 6 Abs. 1 lit. c DSGVO, zur Systemsicherheit und Weiterentwicklung auf Grundlage von Art. 6 Abs. 1 lit. f DSGVO sowie bei freiwilligen Funktionen gegebenenfalls auf Grundlage einer Einwilligung gemaess Art. 6 Abs. 1 lit. a DSGVO.",
                ),
                SettingsLegalSection(
                    title = "4. Empfaenger und eingesetzte Dienste",
                    body = "Daten koennen an technische und operative Dienstleister uebermittelt werden, soweit dies fuer die App erforderlich ist, etwa an Firebase/Google Cloud, Shopify, PODpartner, angebundene Zahlungsdienste, E-Mail-Dienste sowie Hosting-, Speicher-, Analyse- oder Supportdienste.",
                ),
                SettingsLegalSection(
                    title = "5. Drittstaatentransfers und Speicherdauer",
                    body = "Sofern Dienstleister Daten ausserhalb des EWR verarbeiten, erfolgt dies nur auf Grundlage geeigneter Garantien, insbesondere Angemessenheitsbeschluessen oder Standardvertragsklauseln, soweit dies gesetzlich erforderlich ist. Personenbezogene Daten werden nur so lange gespeichert, wie dies fuer die jeweiligen Zwecke erforderlich ist oder gesetzliche Aufbewahrungsfristen bestehen.",
                ),
                SettingsLegalSection(
                    title = "6. Ihre Rechte",
                    body = "Sie haben im Rahmen der gesetzlichen Voraussetzungen das Recht auf Auskunft, Berichtigung, Loeschung, Einschraenkung der Verarbeitung, Datenuebertragbarkeit sowie Widerspruch gegen bestimmte Verarbeitungen. Erteilte Einwilligungen koennen mit Wirkung fuer die Zukunft widerrufen werden. Zudem besteht ein Beschwerderecht bei einer Datenschutz-Aufsichtsbehoerde.",
                ),
                SettingsLegalSection(
                    title = "7. Datensicherheit und Aenderungen",
                    body = "Es werden angemessene technische und organisatorische Massnahmen eingesetzt, um Daten gegen Verlust, Missbrauch oder unbefugten Zugriff zu schuetzen. Diese Datenschutzbestimmungen koennen aktualisiert werden; bei wesentlichen Aenderungen wird das Aktualisierungsdatum in der App angepasst.",
                ),
            ),
        )
        SettingsLegalDocumentType.TermsAndConditions -> SettingsLegalDocument(
            title = "AGB",
            updatedAt = "2. April 2026",
            introduction = "Diese Allgemeinen Geschaeftsbedingungen gelten fuer entgeltliche Leistungen, Bestellungen und vermittelte Kaeufe ueber die App, insbesondere fuer Merchandise und damit verbundene App-Services.",
            sections = listOf(
                SettingsLegalSection(
                    title = "1. Geltungsbereich und Vertragspartner",
                    body = "Vertragspartner ist Skydown x 22. Rueckfragen koennen an skydownent@gmail.com gerichtet werden. Die Darstellung von Produkten oder Leistungen in der App stellt noch kein verbindliches Angebot dar.",
                ),
                SettingsLegalSection(
                    title = "2. Vertragsschluss",
                    body = "Ein Vertrag kommt erst zustande, wenn eine Bestellung angenommen oder nach erfolgreicher Zahlung bestaetigt wird. Fuer einzelne Leistungen koennen zusaetzliche Hinweise direkt im Checkout oder in der jeweiligen Produktbeschreibung gelten.",
                ),
                SettingsLegalSection(
                    title = "3. Preise, Zahlung und Versand",
                    body = "Alle in der App angezeigten Preise und Versandkosten werden vor Abschluss des Kaufs angezeigt. Die verfuegbaren Zahlungsarten richten sich nach dem in der App aktivierten Checkout. Fuer Merchandise gelten die im Checkout ausgewiesenen Versandkosten und Lieferlaender.",
                ),
                SettingsLegalSection(
                    title = "4. Lieferung und Fulfillment",
                    body = "Lieferzeiten koennen je nach Produkt, Zielland, Verfuegbarkeit und Fulfillment-Partner variieren. Fuer Merchandise-Bestellungen koennen externe Fulfillment-Dienste wie Shopify oder PODpartner eingesetzt werden.",
                ),
                SettingsLegalSection(
                    title = "5. Widerruf und Ausschluesse",
                    body = "Verbrauchern steht bei Fernabsatzvertraegen grundsaetzlich ein gesetzliches Widerrufsrecht zu. Kein Widerrufsrecht besteht insbesondere bei Waren, die nicht vorgefertigt sind und fuer deren Herstellung eine individuelle Auswahl oder Bestimmung durch den Verbraucher massgeblich ist oder die eindeutig auf persoenliche Beduerfnisse zugeschnitten sind.",
                ),
                SettingsLegalSection(
                    title = "6. Gewaehrleistung und Haftung",
                    body = "Es gelten die gesetzlichen Maengelrechte. Wir haften unbeschraenkt bei Vorsatz, grober Fahrlaessigkeit, Verletzung von Leben, Koerper oder Gesundheit sowie in den gesetzlich zwingenden Faellen. Bei leicht fahrlaessiger Verletzung wesentlicher Vertragspflichten ist die Haftung auf den vorhersehbaren, vertragstypischen Schaden begrenzt.",
                ),
                SettingsLegalSection(
                    title = "7. Streitbeilegung und Schlussbestimmungen",
                    body = "Die fruehere EU-Online-Streitbeilegungsplattform ist seit dem 20. Juli 2025 eingestellt. Es gilt deutsches Recht, soweit dem keine zwingenden Verbraucherschutzvorschriften entgegenstehen.",
                ),
            ),
        )
        SettingsLegalDocumentType.TermsOfService -> SettingsLegalDocument(
            title = "Nutzungsbedingungen",
            updatedAt = "2. April 2026",
            introduction = "Diese Nutzungsbedingungen regeln den Zugriff auf Skydown x 22 und die Nutzung der Inhalte, Community-, Medien-, Shop- und KI-Funktionen innerhalb der App.",
            sections = listOf(
                SettingsLegalSection(
                    title = "1. Annahme und Aenderungen",
                    body = "Mit der Nutzung der App bestaetigen Sie, diese Bedingungen gelesen zu haben und zu akzeptieren. Die Bedingungen koennen aktualisiert werden; die weitere Nutzung nach einer Aktualisierung gilt als Zustimmung zur dann gueltigen Fassung.",
                ),
                SettingsLegalSection(
                    title = "2. Konto und Zugang",
                    body = "Fuer bestimmte Funktionen ist ein Konto erforderlich. Nutzer sind fuer die Vertraulichkeit ihrer Zugangsdaten verantwortlich und muessen missbraeuchliche Nutzung oder Sicherheitsvorfaelle unverzueglich melden.",
                ),
                SettingsLegalSection(
                    title = "3. Zulaessige Nutzung",
                    body = "Die App darf nur rechtmaessig und gemaess diesen Bedingungen genutzt werden. Untersagt sind insbesondere Manipulationen, automatisierter Missbrauch, Umgehung technischer Schutzmassnahmen, unbefugter Zugriff, rechtswidrige Uploads oder die Nutzung von Inhalten in einer Weise, die Rechte Dritter verletzt.",
                ),
                SettingsLegalSection(
                    title = "4. Inhalte, Rechte und Marken",
                    body = "Die App, ihre Marken, Designs, Texte, Bilder, Audio-, Video- und KI-Ausgaben sowie sonstige Inhalte bleiben, soweit nicht anders angegeben, Eigentum von Skydown x 22, Skydown Entertainment, beteiligten Kuenstlern oder der jeweiligen Rechteinhaber.",
                ),
                SettingsLegalSection(
                    title = "5. KI- und Upload-Funktionen",
                    body = "Wer Eingaben, Dateien oder Medien hochlaedt oder KI-Funktionen nutzt, sichert zu, hierzu berechtigt zu sein und keine rechtswidrigen oder fremden Inhalte einzuspeisen. KI-Ausgaben koennen fehlerhaft oder unvollstaendig sein und muessen vor verbindlicher Nutzung eigenverantwortlich geprueft werden.",
                ),
                SettingsLegalSection(
                    title = "6. Verfuegbarkeit, Sperrung und Haftung",
                    body = "Eine jederzeit unterbrechungsfreie oder fehlerfreie Nutzung wird nicht geschuldet. Funktionen, Inhalte oder Integrationen koennen geaendert, eingeschraenkt oder eingestellt werden. Konten oder Zugaenge koennen bei Verstoessen, Sicherheitsrisiken oder gesetzlichen Gruenden gesperrt werden. Fuer unentgeltliche Dienste und technisch bedingte Ausfaelle ist die Haftung im gesetzlich zulaessigen Umfang eingeschraenkt.",
                ),
                SettingsLegalSection(
                    title = "7. Geltendes Recht und Kontakt",
                    body = "Es gilt deutsches Recht, soweit keine zwingenden Verbraucherschutzvorschriften entgegenstehen. Fragen zu diesen Bedingungen koennen an skydownent@gmail.com gerichtet werden.",
                ),
            ),
        )
    }
}
