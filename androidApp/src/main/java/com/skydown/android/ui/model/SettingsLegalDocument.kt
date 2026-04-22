package com.skydown.android.ui.model

import com.skydown.android.data.LegalContentSettings

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

    return when (this) {
        SettingsLegalDocumentType.ReadmeGuide -> SettingsLegalDocument(
            title = "README / SkyOS Guide",
            updatedAt = lastUpdatedLabel,
            introduction = "Dieser Guide hilft dir beim schnellen Einstieg in $brandName als zusammenhaengendes Creator Operating System. Du findest hier die wichtigsten Flows fuer Rollen, KI, Agenten, Workflows, Content und Support.",
            sections = listOf(
                SettingsLegalSection(
                    title = "1. Schnellstart",
                    body = "Konto erstellen oder einloggen, Profil mit Bild/Bio vervollstaendigen und danach Home als Orientierung nutzen, bevor Music, Video, Shop und AI Schritt fuer Schritt erschlossen werden.",
                ),
                SettingsLegalSection(
                    title = "2. Rollen",
                    body = "Owner verwaltet Systemfunktionen wie Rollen, Limits, Rechtliches und Runtime-Konfigurationen. Admin/Subadmin arbeiten in freigegebenen Bereichen. User nutzen die App inklusive KI innerhalb der zugewiesenen Kontingente.",
                ),
                SettingsLegalSection(
                    title = "3. KI Bot",
                    body = "Der Bot unterstuetzt bei Text- und Visual-Workflows, z. B. Captions, Hooks, Konzepte und Creative-Richtungen. Ergebnisse sollten vor produktiver Nutzung immer geprueft werden.",
                ),
                SettingsLegalSection(
                    title = "4. Agent und Workflow-Trigger",
                    body = "Der Agent ist fuer umsetzungsorientierte Aufgaben gedacht und kann optional eigene Workflow-Services (z. B. n8n/Webhook) triggern. Bei externen Services koennen je nach Setup eigene API-Kosten entstehen. SkyOS trennt persoenliche Workflow-Konfigurationen pro Konto, damit Verantwortung, Rechte und Kosten klar bleiben.",
                ),
                SettingsLegalSection(
                    title = "5. Kostenkontrolle",
                    body = "Kontingente, Hard Caps und globale Tageslimits helfen, Kostenrisiken zu begrenzen. Aktionen werden bei deaktiviertem Zugriff oder ueberschrittenen Limits blockiert.",
                ),
                SettingsLegalSection(
                    title = "6. Content und Commerce",
                    body = "Music/Video decken Releases, Artist-Pages und Collabo-Inhalte ab. Shop und Checkout basieren auf den aktivierten Commerce- und Zahlungs-Setups. Das Ziel ist ein Systemgefuehl: entdecken, verstehen, handeln und wieder sauber zurueckfinden.",
                ),
                SettingsLegalSection(
                    title = "7. Rechtliches und Support",
                    body = "Aktuelle AGB, Datenschutz und Nutzungsbedingungen sind direkt in den Einstellungen abrufbar. Betreiber ist $operatorName. $imprintReference",
                ),
                SettingsLegalSection(
                    title = "8. Brand-DNA und Bedeutung von 22",
                    body = "Meisterzahl 22: $masterNumberMeaning\n\nWer wir sind:\n$brandManifesto\n\nSymbolcode: $symbolicNumericCode\nLeet-Code: $symbolicLeetCode\n\nDeutung:\n$symbolicCodeExplanation",
                ),
            ),
            contactEmail = supportEmail,
        )
        SettingsLegalDocumentType.PrivacyPolicy -> SettingsLegalDocument(
            title = "Datenschutzbestimmungen",
            updatedAt = lastUpdatedLabel,
            introduction = "Diese Datenschutzbestimmungen informieren ueber die Verarbeitung personenbezogener Daten bei $brandName. $brandName ist als zusammenhaengendes Creator- und Workflow-System konzipiert und verbindet Profile, Medien, Commerce, KI und Automation innerhalb einer App.",
            sections = listOf(
                SettingsLegalSection(
                    title = "1. Verantwortlicher",
                    body = "Verantwortlich ist $operatorName. Datenschutzanfragen koennen an $supportEmail gerichtet werden. $imprintReference",
                ),
                SettingsLegalSection(
                    title = "2. Welche Daten verarbeitet werden",
                    body = "Je nach Nutzung werden insbesondere Kontodaten, Rollen und Kontingente, freiwillige Profilangaben, Galerie- und Upload-Inhalte, technische Geraete-, Crash- und Fehlerdaten, Nutzungsdaten zu Musik-, Video-, Shop-, KI-, Workflow- und Einstellungsbereichen sowie Bestell-, Versand-, Fulfillment- und Supportdaten verarbeitet.",
                ),
                SettingsLegalSection(
                    title = "3. Zwecke und Rechtsgrundlagen",
                    body = "Die Verarbeitung erfolgt insbesondere zur Bereitstellung der App und Ihres Kontos auf Grundlage von Art. 6 Abs. 1 lit. b DSGVO, zur Vertragsabwicklung von Bestellungen und Support ebenfalls nach Art. 6 Abs. 1 lit. b DSGVO, zur Erfuellung gesetzlicher Pflichten nach Art. 6 Abs. 1 lit. c DSGVO, zur Systemsicherheit, Abuse-Abwehr, Rollen-, Rechte- und Kostenkontrolle sowie Weiterentwicklung nach Art. 6 Abs. 1 lit. f DSGVO und bei optionalen Funktionen gegebenenfalls auf Grundlage einer Einwilligung nach Art. 6 Abs. 1 lit. a DSGVO.",
                ),
                SettingsLegalSection(
                    title = "4. Konten, Sicherheit und Firebase",
                    body = "Fuer Authentifizierung, Datenbank, Storage, serverseitige Logik und Security-Schutz koennen Dienste von Firebase und Google Cloud eingesetzt werden, etwa Firebase Auth, Cloud Firestore, Cloud Storage, Cloud Functions, App Check, Logging und Kostenkontrolle.",
                ),
                SettingsLegalSection(
                    title = "5. KI, Workflow und Drittanbieter",
                    body = "KI-Funktionen sind Assistenzfunktionen. Je nach Nutzung koennen Prompts, Uploads und Ausgaben an eingebundene KI- oder Workflow-Dienste uebermittelt werden. Bei BYOS-Setups mit n8n oder Manus verwaltet der Nutzer seinen externen Dienstzugang eigenverantwortlich. SkyOS trennt persoenliche Konfigurationen pro Konto, damit Verantwortung, Rechte und Kosten nachvollziehbar bleiben. KI-Ausgaben muessen vor verbindlicher Nutzung geprueft werden.",
                ),
                SettingsLegalSection(
                    title = "6. Rechte der Nutzer",
                    body = "Sie haben nach Massgabe der gesetzlichen Voraussetzungen das Recht auf Auskunft, Berichtigung, Loeschung, Einschraenkung der Verarbeitung, Datenuebertragbarkeit sowie Widerspruch gegen bestimmte Verarbeitungen.",
                ),
                SettingsLegalSection(
                    title = "7. Datensicherheit und Aenderungen",
                    body = "Es werden angemessene technische und organisatorische Massnahmen eingesetzt, um Daten gegen Verlust, Missbrauch oder unbefugten Zugriff zu schuetzen. Dazu gehoeren unter anderem Rollen- und Rechtemodelle, Upload-Freigaben, Security Rules, App Check, Lockdown-Mechanismen und Kostenkontrolle.",
                ),
            ),
            contactEmail = supportEmail,
        )
        SettingsLegalDocumentType.TermsAndConditions -> SettingsLegalDocument(
            title = "AGB",
            updatedAt = lastUpdatedLabel,
            introduction = "Diese Allgemeinen Geschaeftsbedingungen gelten fuer entgeltliche Leistungen, Bestellungen und vermittelte Kaeufe ueber $brandName, insbesondere fuer Merchandise, digitale Inhalte, Creator-/Studio-Leistungen, AI-gestuetzte Unterstuetzung, Workflow-nahe Services und damit verbundene App-Services.",
            sections = listOf(
                SettingsLegalSection(
                    title = "1. Geltungsbereich und Vertragspartner",
                    body = "Vertragspartner ist $operatorName. Rechteinhaber der App ist $rightsHolderName. Rueckfragen koennen an $supportEmail gerichtet werden. $imprintReference",
                ),
                SettingsLegalSection(
                    title = "2. Vertragsschluss",
                    body = "Die Darstellung von Produkten oder Leistungen in der App stellt noch kein verbindliches Angebot dar. Ein Vertrag kommt erst zustande, wenn eine Bestellung angenommen, ein Auftrag bestaetigt oder nach erfolgreicher Zahlung freigegeben wird.",
                ),
                SettingsLegalSection(
                    title = "3. Preise, Zahlung und Versand",
                    body = "Alle in der App angezeigten Preise, Versandkosten und Zahlungsarten werden vor Abschluss des Kaufs angezeigt. Massgeblich sind die Angaben, die im Checkout unmittelbar vor Abschluss dargestellt werden. Die verfuegbaren Zahlarten richten sich nach dem aktivierten Checkout und den hinterlegten Zahlungsdiensten.",
                ),
                SettingsLegalSection(
                    title = "4. Lieferung und Fulfillment",
                    body = "Lieferzeiten koennen je nach Produkt, Zielland, Verfuegbarkeit, Produktionsdauer und Fulfillment-Partner variieren. Fuer Bestellungen koennen externe Fulfillment-Dienste wie Shopify, PODpartner oder sonstige Versand- und Produktionspartner eingesetzt werden.",
                ),
                SettingsLegalSection(
                    title = "5. Digitale Leistungen, KI und Workflow",
                    body = "Soweit ueber die App digitale Inhalte, Creator-Leistungen, KI-gestuetzte Unterstuetzung oder Workflow-Services angeboten werden, dienen diese regelmaessig der kreativen oder technischen Unterstuetzung. KI- oder Automationsausgaben koennen fachliche, rechtliche oder wirtschaftliche Pruefungen nicht ersetzen und muessen vor verbindlicher Nutzung eigenverantwortlich geprueft werden.",
                ),
                SettingsLegalSection(
                    title = "6. Haftung und Schlussbestimmungen",
                    body = "Es gelten die gesetzlichen Haftungsregeln. Es gilt deutsches Recht, soweit dem keine zwingenden Verbraucherschutzvorschriften entgegenstehen.",
                ),
            ),
            contactEmail = supportEmail,
        )
        SettingsLegalDocumentType.TermsOfService -> SettingsLegalDocument(
            title = "Nutzungsbedingungen",
            updatedAt = lastUpdatedLabel,
            introduction = "Diese Nutzungsbedingungen regeln den Zugriff auf $brandName und die Nutzung der Inhalte, Profil-, Medien-, Shop-, KI- und Workflow-Funktionen innerhalb der App. $brandName ist als zusammenhaengendes System gedacht; diese Bedingungen gelten fuer alle darin bereitgestellten Produktbereiche.",
            sections = listOf(
                SettingsLegalSection(
                    title = "1. Annahme und Aenderungen",
                    body = "Mit der Nutzung der App bestaetigen Sie, diese Bedingungen gelesen zu haben und zu akzeptieren. Die Bedingungen koennen aktualisiert werden; die weitere Nutzung nach einer Aktualisierung gilt als Zustimmung zur dann gueltigen Fassung.",
                ),
                SettingsLegalSection(
                    title = "2. Konto und zulaessige Nutzung",
                    body = "Fuer bestimmte Funktionen ist ein Konto erforderlich. Nutzer sind fuer die Richtigkeit ihrer Kontoangaben, den Schutz ihrer Zugangsdaten und die rechtmaessige Nutzung ihres Kontos verantwortlich. Untersagt sind insbesondere Manipulationen, automatisierter Missbrauch, Umgehung technischer Schutzmassnahmen, unbefugter Zugriff und rechtswidrige Uploads.",
                ),
                SettingsLegalSection(
                    title = "3. Inhalte, Rechte und Marken",
                    body = "Rechteinhaber der App ist $rightsHolderName. Die App, ihre Marken, Designs, Texte, Bilder, Audio-, Video- und sonstigen Inhalte bleiben, soweit nicht anders angegeben, Eigentum von $rightsHolderName, beteiligten Kuenstlern oder der jeweiligen Rechteinhaber.",
                ),
                SettingsLegalSection(
                    title = "4. KI und Workflow",
                    body = "KI-Funktionen sind Assistenzfunktionen. KI-Ausgaben koennen fehlerhaft, unvollstaendig oder ungeeignet sein und muessen vor verbindlicher Nutzung geprueft werden. Workflow-Automationen dienen der technischen oder kreativen Unterstuetzung.",
                ),
                SettingsLegalSection(
                    title = "5. Verfuegbarkeit, Sperrung und Haftung",
                    body = "Eine jederzeit unterbrechungsfreie oder fehlerfreie Nutzung wird nicht geschuldet. Produktbereiche, Designs, Rollenlogiken, KI-Funktionen, Preise oder Integrationen koennen im Rahmen der Weiterentwicklung angepasst werden. Konten oder Inhalte koennen bei Verstoessen, Sicherheits- oder Kostenrisiken eingeschraenkt oder gesperrt werden. Die Haftung richtet sich nach den gesetzlichen Vorgaben.",
                ),
                SettingsLegalSection(
                    title = "6. Recht und Kontakt",
                    body = "Betreiber und Vertragspartner ist $operatorName. Fragen koennen an $supportEmail gerichtet werden. Es gilt deutsches Recht, soweit keine zwingenden Verbraucherschutzvorschriften entgegenstehen.",
                ),
            ),
            contactEmail = supportEmail,
        )
        SettingsLegalDocumentType.SubscriptionTerms -> SettingsLegalDocument(
            title = "Subscription Terms",
            updatedAt = lastUpdatedLabel,
            introduction = "Diese Hinweise regeln Laufzeit, Verlaengerung, Kuendigung und Wiederherstellung von Mitgliedschaften innerhalb von $brandName.",
            sections = listOf(
                SettingsLegalSection(
                    title = "1. Plaene und Laufzeiten",
                    body = "Mitgliedschaften koennen je nach Angebot monatlich und/oder jaehrlich verfuegbar sein. Preise und Laufzeiten werden vor Kauf im jeweiligen Store angezeigt.",
                ),
                SettingsLegalSection(
                    title = "2. Store-Abrechnung",
                    body = "Abrechnung und Verlaengerung erfolgen ueber Apple App Store oder Google Play Store gemaess deren Bedingungen.",
                ),
                SettingsLegalSection(
                    title = "3. Auto-Renew und Kuendigung",
                    body = "Abos verlaengern sich automatisch, wenn sie nicht vor Ende des laufenden Zeitraums im jeweiligen Store gekuendigt werden.",
                ),
                SettingsLegalSection(
                    title = "4. Restore und Refunds",
                    body = "Bereits gekaufte Abos koennen ueber Restore synchronisiert werden. Erstattungen richten sich nach den Richtlinien des jeweiligen Stores.",
                ),
            ),
            contactEmail = supportEmail,
        )
        SettingsLegalDocumentType.AiUsageNotice -> SettingsLegalDocument(
            title = "AI Usage Notice",
            updatedAt = lastUpdatedLabel,
            introduction = "KI in $brandName ist Assistenz, nicht Autoritaet. Ergebnisse muessen vor verbindlicher Nutzung geprueft werden.",
            sections = listOf(
                SettingsLegalSection(
                    title = "1. KI kann Fehler machen",
                    body = "Ausgaben koennen unvollstaendig, veraltet oder ungeeignet sein. Nutzer tragen die Verantwortung fuer die finale Bewertung und Nutzung.",
                ),
                SettingsLegalSection(
                    title = "2. Keine Fachberatung",
                    body = "Es erfolgt keine Rechts-, Medizin-, Steuer-, Finanz- oder Sicherheitsberatung durch KI-Ausgaben.",
                ),
                SettingsLegalSection(
                    title = "3. Missbrauch verboten",
                    body = "Verboten sind rechtswidrige Nutzung, Umgehung von Schutzmechanismen, Rechteverletzungen, Abuse und tauschende oder schaedliche Inhalte.",
                ),
                SettingsLegalSection(
                    title = "4. Limits und Schutz",
                    body = "Zur Sicherheit und Kostenkontrolle koennen Limits, Blockierungen oder Degradierungen aktiv sein.",
                ),
            ),
            contactEmail = supportEmail,
        )
        SettingsLegalDocumentType.ImprintInfo -> SettingsLegalDocument(
            title = "Impressum / Company Info",
            updatedAt = lastUpdatedLabel,
            introduction = "Unternehmens- und Anbieterinformationen fuer $brandName.",
            sections = listOf(
                SettingsLegalSection(
                    title = "1. Betreiber",
                    body = operatorName,
                ),
                SettingsLegalSection(
                    title = "2. Rechteinhaber",
                    body = rightsHolderName,
                ),
                SettingsLegalSection(
                    title = "3. Anbieterkennzeichnung",
                    body = imprintReference,
                ),
                SettingsLegalSection(
                    title = "4. Hinweis",
                    body = "Regionale Pflichtangaben koennen je nach Rechtsraum variieren und sollten vor Launch rechtlich final geprueft werden.",
                ),
            ),
            contactEmail = supportEmail,
        )
    }
}
