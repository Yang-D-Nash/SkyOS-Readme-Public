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
            updatedAt = "3. April 2026",
            introduction = "Diese Datenschutzbestimmungen informieren ueber die Verarbeitung personenbezogener Daten bei Skydown x 22. Sie beruecksichtigen insbesondere die DSGVO, einschlaegige Datenschutzvorgaben fuer Telemedien und Endgeraetezugriffe sowie den stufenweise geltenden EU AI Act, soweit KI-Funktionen genutzt werden.",
            sections = listOf(
                SettingsLegalSection(
                    title = "1. Verantwortlicher",
                    body = "Verantwortlich ist der Betreiber von Skydown x 22. Datenschutzanfragen koennen an skydownent@gmail.com gerichtet werden. Die vollstaendige Anbieterkennzeichnung einschliesslich ladungsfaehiger Anschrift ist im Impressum, Store-Eintrag oder einem gleichwertigen Anbieterprofil bereitzuhalten.",
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
                    body = "Fuer Authentifizierung, Datenbank, Storage, serverseitige Logik und Security-Schutz koennen Dienste von Firebase und Google Cloud eingesetzt werden, etwa Firebase Auth, Cloud Firestore, Cloud Storage, Cloud Functions, App Check, Logging und Kostenkontrolle. Diese Verarbeitung dient der sicheren Bereitstellung der App, der Rollen- und Rechtepruefung sowie der Missbrauchsabwehr.",
                ),
                SettingsLegalSection(
                    title = "5. Oeffentliche Profile, Uploads und Artist-Seiten",
                    body = "Wenn Profilangaben, Galerieinhalte, Artist-Seiten oder andere Inhalte zur oeffentlichen Darstellung freigegeben werden, koennen diese fuer andere Nutzer oder fuer die Oeffentlichkeit sichtbar sein. Stellen Sie bitte nur Inhalte ein, fuer die Sie die erforderlichen Rechte besitzen und die keine Rechte Dritter verletzen.",
                ),
                SettingsLegalSection(
                    title = "6. Bestellungen, Zahlungen und Fulfillment",
                    body = "Fuer Merchandise, digitale Leistungen oder sonstige entgeltliche Angebote koennen externe Dienstleister eingebunden werden, insbesondere Shopify, Fulfillment-Partner, Zahlungsdienstleister sowie Versand- oder Kommunikationsdienste. Zahlungsdaten werden in der Regel direkt durch den jeweiligen Zahlungsdienstleister verarbeitet; wir erhalten typischerweise Status-, Referenz- und Abwicklungsdaten.",
                ),
                SettingsLegalSection(
                    title = "7. KI-Funktionen, DSGVO und EU AI Act",
                    body = "Die KI-Funktionen sind als Assistenzfunktionen vorgesehen. KI-Ausgaben koennen unvollstaendig, fehlerhaft oder unpassend sein und muessen vor verbindlicher Nutzung geprueft werden. Bitte geben Sie keine geheimen, vertraulichen oder unnoetig sensiblen Daten Dritter ein. Soweit KI-Funktionen genutzt werden, koennen Prompts, hochgeladene Inhalte und Ausgaben an eingebundene KI-Dienste uebermittelt werden. Die KI-Funktionen sind nicht dafuer bestimmt, ohne menschliche Pruefung rechtlich erhebliche oder vergleichbar wesentliche Entscheidungen ueber Personen zu treffen. Zum Stand April 2026 wird der EU AI Act stufenweise anwendbar; bereits einschlaegige Verbote unzulaessiger Praktiken sowie Anforderungen an KI-Kompetenz und Transparenz werden risikobasiert beruecksichtigt.",
                ),
                SettingsLegalSection(
                    title = "8. Workflow Automation und Webhook-Dienste",
                    body = "Wenn der Owner Workflow-Automationen aktiviert, koennen serverseitig gepruefte Daten an den konfigurierten Workflow-Endpunkt, zum Beispiel n8n, uebermittelt werden. Dazu koennen insbesondere UID, E-Mail-Adresse, Benutzername, Rolle, Admin-/Owner-Status, Trigger, Zeitstempel und die fuer den jeweiligen Workflow uebergebenen Anfragedaten gehoeren. Die Uebermittlung erfolgt nur bei aktiver Konfiguration.",
                ),
                SettingsLegalSection(
                    title = "9. Empfaenger, Drittstaatentransfers und Speicherdauer",
                    body = "Daten koennen an technische und operative Empfaenger uebermittelt werden, soweit dies fuer die App erforderlich ist, etwa an Hosting-, Speicher-, Authentifizierungs-, Sicherheits-, Zahlungs-, Fulfillment-, Support-, KI- oder Workflow-Dienstleister. Sofern Empfaenger Daten ausserhalb des EWR verarbeiten, erfolgt dies nur auf Grundlage geeigneter Garantien. Personenbezogene Daten werden nur so lange gespeichert, wie dies fuer die jeweiligen Zwecke erforderlich ist oder gesetzliche Aufbewahrungs-, Nachweis- oder Verjaehrungsfristen bestehen.",
                ),
                SettingsLegalSection(
                    title = "10. Ihre Rechte",
                    body = "Sie haben nach Massgabe der gesetzlichen Voraussetzungen das Recht auf Auskunft, Berichtigung, Loeschung, Einschraenkung der Verarbeitung, Datenuebertragbarkeit sowie Widerspruch gegen bestimmte Verarbeitungen. Erteilte Einwilligungen koennen mit Wirkung fuer die Zukunft widerrufen werden. Zudem besteht ein Beschwerderecht bei einer Datenschutz-Aufsichtsbehoerde.",
                ),
                SettingsLegalSection(
                    title = "11. Datensicherheit und Aenderungen",
                    body = "Es werden angemessene technische und organisatorische Massnahmen eingesetzt, um Daten gegen Verlust, Missbrauch oder unbefugten Zugriff zu schuetzen. Dazu gehoeren unter anderem Rollen- und Rechtemodelle, Upload-Freigaben, Security Rules, App Check, Lockdown-Mechanismen und Kostenkontrolle. Diese Datenschutzbestimmungen koennen aktualisiert werden; bei wesentlichen Aenderungen wird das Aktualisierungsdatum in der App angepasst.",
                ),
            ),
        )
        SettingsLegalDocumentType.TermsAndConditions -> SettingsLegalDocument(
            title = "AGB",
            updatedAt = "3. April 2026",
            introduction = "Diese Allgemeinen Geschaeftsbedingungen gelten fuer entgeltliche Leistungen, Bestellungen und vermittelte Kaeufe ueber die App, insbesondere fuer Merchandise, digitale Inhalte, Creator-/Studio-Leistungen und damit verbundene App-Services.",
            sections = listOf(
                SettingsLegalSection(
                    title = "1. Geltungsbereich und Vertragspartner",
                    body = "Vertragspartner ist der Betreiber von Skydown x 22. Rueckfragen koennen an skydownent@gmail.com gerichtet werden. Die vollstaendige Anbieterkennzeichnung einschliesslich ladungsfaehiger Anschrift ist im Impressum oder Anbieterprofil bereitzuhalten. Die Darstellung von Produkten oder Leistungen in der App stellt noch kein verbindliches Angebot dar.",
                ),
                SettingsLegalSection(
                    title = "2. Vertragsschluss",
                    body = "Ein Vertrag kommt erst zustande, wenn eine Bestellung angenommen, ein Auftrag bestaetigt oder nach erfolgreicher Zahlung freigegeben wird. Fuer einzelne Leistungen koennen zusaetzliche Hinweise direkt im Checkout, in der Produktbeschreibung oder in einer individuellen Abstimmung gelten.",
                ),
                SettingsLegalSection(
                    title = "3. Preise, Zahlung und Versand",
                    body = "Alle in der App angezeigten Preise, Versandkosten und Zahlungsarten werden vor Abschluss des Kaufs angezeigt. Die verfuegbaren Zahlarten richten sich nach dem aktivierten Checkout und den hinterlegten Zahlungsdiensten. Bei grenzueberschreitenden Lieferungen koennen zusaetzliche Steuern, Zoelle oder Importgebuehren anfallen.",
                ),
                SettingsLegalSection(
                    title = "4. Lieferung und Fulfillment",
                    body = "Lieferzeiten koennen je nach Produkt, Zielland, Verfuegbarkeit, Produktionsdauer und Fulfillment-Partner variieren. Fuer Bestellungen koennen externe Fulfillment-Dienste wie Shopify, PODpartner oder sonstige Versand- und Produktionspartner eingesetzt werden. Teillieferungen sind zulaessig, soweit dies zumutbar ist.",
                ),
                SettingsLegalSection(
                    title = "5. Digitale Leistungen, KI und Workflow-Services",
                    body = "Soweit ueber die App digitale Inhalte, Creator-Leistungen, KI-gestuetzte Unterstuetzung oder Workflow-Services angeboten werden, dienen diese regelmaessig der kreativen oder technischen Unterstuetzung. KI- oder Automationsausgaben koennen fachliche, rechtliche oder wirtschaftliche Pruefungen nicht ersetzen. Der konkrete Leistungsumfang ergibt sich aus dem jeweiligen Angebot.",
                ),
                SettingsLegalSection(
                    title = "6. Widerruf und Ausschluesse",
                    body = "Verbrauchern steht bei Fernabsatzvertraegen grundsaetzlich ein gesetzliches Widerrufsrecht zu. Ausnahmen gelten insbesondere fuer nicht vorgefertigte oder personalisierte Waren sowie fuer digitale Inhalte oder digitale Dienstleistungen, wenn mit der Ausfuehrung erst begonnen wurde, nachdem Sie ausdruecklich zugestimmt und bestaetigt haben, dass Sie hierdurch Ihr Widerrufsrecht verlieren koennen.",
                ),
                SettingsLegalSection(
                    title = "7. Gewaehrleistung, Produktsicherheit und Rueckrufe",
                    body = "Es gelten die gesetzlichen Maengelrechte. Bitte pruefen Sie gelieferte Waren moeglichst zeitnah. Soweit fuer physische Produkte produkt- oder sicherheitsrechtliche Hinweise, Warnungen oder Rueckrufe erforderlich werden, koennen diese ueber die hinterlegten Kontaktwege kommuniziert werden.",
                ),
                SettingsLegalSection(
                    title = "8. Haftung",
                    body = "Wir haften unbeschraenkt bei Vorsatz, grober Fahrlaessigkeit, Verletzung von Leben, Koerper oder Gesundheit sowie in den gesetzlich zwingenden Faellen. Bei leicht fahrlaessiger Verletzung wesentlicher Vertragspflichten ist die Haftung auf den vorhersehbaren, vertragstypischen Schaden begrenzt. Im Uebrigen ist die Haftung ausgeschlossen, soweit gesetzlich zulaessig.",
                ),
                SettingsLegalSection(
                    title = "9. Streitbeilegung und Schlussbestimmungen",
                    body = "Die fruehere EU-Online-Streitbeilegungsplattform wurde zum 20. Juli 2025 eingestellt. Eine Verpflichtung zur Teilnahme an Streitbeilegungsverfahren vor einer Verbraucherschlichtungsstelle besteht nur, wenn dies gesetzlich zwingend ist oder ausdruecklich erklaert wird. Es gilt deutsches Recht, soweit dem keine zwingenden Verbraucherschutzvorschriften entgegenstehen.",
                ),
            ),
        )
        SettingsLegalDocumentType.TermsOfService -> SettingsLegalDocument(
            title = "Nutzungsbedingungen",
            updatedAt = "3. April 2026",
            introduction = "Diese Nutzungsbedingungen regeln den Zugriff auf Skydown x 22 und die Nutzung der Inhalte, Profil-, Medien-, Shop-, KI- und Workflow-Funktionen innerhalb der App.",
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
                    body = "Die App darf nur rechtmaessig und gemaess diesen Bedingungen genutzt werden. Untersagt sind insbesondere Manipulationen, automatisierter Missbrauch, Umgehung technischer Schutzmassnahmen, unbefugter Zugriff, rechtswidrige Uploads, der Einsatz von Bots gegen Schutzmechanismen, die Nutzung fuer Spam, Betrug oder Belaestigung sowie jede Nutzung in einer Weise, die Rechte Dritter verletzt.",
                ),
                SettingsLegalSection(
                    title = "4. Profile, Uploads und Moderation",
                    body = "Wenn Profile, Galerien, Artist-Seiten oder andere Inhalte eingestellt werden, sichern Nutzer zu, hierzu berechtigt zu sein. Rechtswidrige, verletzende, betruegerische oder offensichtlich missbraeuchliche Inhalte koennen entfernt, eingeschraenkt oder depubliziert werden. Soweit die App als Hosting- oder Vermittlungsdienst im Sinne der Verordnung (EU) 2022/2065 einzuordnen ist, koennen Hinweise auf rechtswidrige Inhalte ueber skydownent@gmail.com gemeldet werden.",
                ),
                SettingsLegalSection(
                    title = "5. Inhalte, Rechte und Marken",
                    body = "Die App, ihre Marken, Designs, Texte, Bilder, Audio-, Video- und sonstigen Inhalte bleiben, soweit nicht anders angegeben, Eigentum von Skydown x 22, Skydown Entertainment, beteiligten Kuenstlern oder der jeweiligen Rechteinhaber.",
                ),
                SettingsLegalSection(
                    title = "6. KI-Funktionen und EU AI Act",
                    body = "KI-Funktionen sind als Assistenzfunktionen gedacht. KI-Ausgaben koennen fehlerhaft, unvollstaendig oder ungeeignet sein und muessen vor jeder verbindlichen Nutzung eigenverantwortlich geprueft werden. Es duerfen keine geheimen oder unnoetig sensiblen Daten Dritter eingegeben und keine verbotenen, missbraeuchlichen oder rechtswidrigen KI-Nutzungen verfolgt werden. Die KI-Funktionen sind nicht fuer ausschliesslich automatisierte Entscheidungen mit rechtlicher oder vergleichbar erheblicher Wirkung vorgesehen. Soweit Vorgaben der Verordnung (EU) 2024/1689 bereits anwendbar sind, werden Transparenz, menschliche Kontrolle und risikobasierte Schutzmechanismen beruecksichtigt.",
                ),
                SettingsLegalSection(
                    title = "7. Workflow Automation und externe Endpunkte",
                    body = "Wenn Workflow-Automationen aktiv sind, werden Anfragen ueber serverseitig gepruefte Endpunkte an den vom Owner konfigurierten Workflow weitergeleitet. Diese Automationen dienen der technischen oder kreativen Unterstuetzung und duerfen nicht fuer Notfaelle, ausschliesslich automatisierte Rechtsentscheidungen oder sonstige hochriskante Einsaetze ohne angemessene menschliche Kontrolle genutzt werden.",
                ),
                SettingsLegalSection(
                    title = "8. Verfuegbarkeit und Aenderungen",
                    body = "Eine jederzeit unterbrechungsfreie oder fehlerfreie Nutzung wird nicht geschuldet. Funktionen, Inhalte, KI-Modelle, Integrationen, Zahlarten oder externe Dienste koennen geaendert, eingeschraenkt, ersetzt oder eingestellt werden.",
                ),
                SettingsLegalSection(
                    title = "9. Sperrung, Kuendigung und Recovery",
                    body = "Konten, Inhalte oder Zugaenge koennen bei Verstoessen, Rechten Dritter, Sicherheits- oder Kostenrisiken, aktiviertem Lockdown- oder Recovery-Modus oder aufgrund gesetzlicher Gruende gesperrt, beschraenkt oder beendet werden.",
                ),
                SettingsLegalSection(
                    title = "10. Haftung, Recht und Kontakt",
                    body = "Fuer unentgeltliche Dienste, technisch bedingte Ausfaelle und die Nutzung von KI- oder Workflow-Ausgaben ist die Haftung im gesetzlich zulaessigen Umfang eingeschraenkt. Die Haftung fuer Vorsatz, grobe Fahrlaessigkeit, Verletzung von Leben, Koerper oder Gesundheit sowie zwingende gesetzliche Ansprueche bleibt unberuehrt. Es gilt deutsches Recht, soweit keine zwingenden Verbraucherschutzvorschriften entgegenstehen. Fragen koennen an skydownent@gmail.com gerichtet werden.",
                ),
            ),
        )
    }
}
