//
//  PrivacyPolicy.swift
//  Skydown App
//
//  Created by Yang D. Nash on 29.07.25.
//

import Foundation

enum LegalTextTemplateFactory {
    static func privacyPolicy(using settings: LegalContentSettings) -> String {
        let brand = settings.resolvedBrandName
        let operatorName = settings.resolvedOperatorName
        let supportEmail = settings.resolvedSupportEmail
        let updatedAt = settings.resolvedLastUpdatedLabel
        let imprintReference = settings.resolvedImprintReference

        return """
        Datenschutzbestimmungen fuer \(brand)

        Zuletzt aktualisiert: \(updatedAt)

        Diese Datenschutzbestimmungen informieren Sie ueber die Verarbeitung personenbezogener Daten bei der Nutzung von \(brand). Sie beruecksichtigen insbesondere die DSGVO, die fuer Telemedien und Endgeraetezugriffe einschlaegigen Datenschutzvorgaben sowie den stufenweise geltenden EU AI Act, soweit KI-Funktionen in der App genutzt werden.

        1. Verantwortlicher
        Verantwortlich ist voruebergehend \(operatorName).
        Kontakt fuer Datenschutzanfragen: \(supportEmail).
        \(imprintReference)

        2. Welche Daten verarbeitet werden
        Je nach Nutzung verarbeiten wir insbesondere:
        - Kontodaten wie E-Mail-Adresse, Benutzername, Rolle, Kontingent, Login- und Sitzungsinformationen
        - freiwillige Profildaten wie Profilbild, Kurzinfo, Bio, Instagram, WhatsApp, Galerieinhalte und oeffentlich sichtbare Artist- oder Creator-Angaben
        - technische Daten wie App-Version, Betriebssystem, Geraetetyp, Crash- und Fehlerdaten, Security- und Abuse-Logs
        - Nutzungsdaten zu Musik, Video, Shop, Bestellungen, Profilen, KI-Funktionen, Zahlarten und Systemeinstellungen
        - Bestell-, Versand- und Fulfillmentdaten wie Name, Lieferadresse, Warenkorb, Zahlungsstatus, Referenzen und Versandinformationen
        - Inhalte, die Sie aktiv hochladen, veroeffentlichen oder anfragen, zum Beispiel Bilder, Texte, Support-Nachrichten, KI-Prompts oder Workflow-Anfragen

        3. Zwecke und Rechtsgrundlagen
        Die Verarbeitung erfolgt insbesondere:
        - zur Bereitstellung Ihres Kontos und der App-Funktionen auf Grundlage von Art. 6 Abs. 1 lit. b DSGVO
        - zur Abwicklung von Bestellungen, Fulfillment, Versand, Rechnungen und Support auf Grundlage von Art. 6 Abs. 1 lit. b DSGVO
        - zur Erfuellung gesetzlicher Aufbewahrungs-, Steuer-, Nachweis- und Compliance-Pflichten auf Grundlage von Art. 6 Abs. 1 lit. c DSGVO
        - zur Systemsicherheit, Betrugs- und Missbrauchsabwehr, App-Check-, Rollen-, Rechte- und Kostenkontrolle sowie Weiterentwicklung der App auf Grundlage von Art. 6 Abs. 1 lit. f DSGVO
        - bei optionalen Funktionen, besonderen Freigaben oder gesonderten Zustimmungen auf Grundlage von Art. 6 Abs. 1 lit. a DSGVO
        Besondere Kategorien personenbezogener Daten sollen grundsaetzlich nicht uebermittelt werden. Falls Nutzer solche Daten dennoch freiwillig eingeben, erfolgt eine Verarbeitung nur, soweit hierfuer eine tragfaehige Rechtsgrundlage besteht.

        4. Konten, Sicherheit und Firebase
        Fuer Authentifizierung, Datenbank, Storage, serverseitige Logik sowie Security- und Abuse-Schutz koennen Dienste von Firebase und Google Cloud eingesetzt werden, etwa fuer Firebase Auth, Cloud Firestore, Cloud Storage, Cloud Functions, App Check, Logging und Kostenkontrolle. Diese Verarbeitung dient der sicheren Bereitstellung der App, der Rollen- und Rechtepruefung, der Upload-Freigabe und der Missbrauchsabwehr.

        5. Oeffentliche Profile, Uploads und Artist-Seiten
        Wenn Sie Profilangaben, Galerieinhalte, Artist-Seiten oder andere Inhalte zur oeffentlichen Darstellung freigeben, koennen diese fuer andere Nutzer oder fuer die Oeffentlichkeit sichtbar sein. Stellen Sie bitte nur Inhalte ein, fuer die Sie die erforderlichen Rechte besitzen und die keine Rechte Dritter verletzen.

        6. Bestellungen, Zahlungen und Fulfillment
        Fuer Merchandise, digitale Leistungen oder sonstige entgeltliche Angebote koennen externe Dienstleister eingebunden werden, insbesondere Shopify, Fulfillment-Partner, Zahlungsdienstleister sowie Versand- oder Kommunikationsdienste.

        7. KI-Funktionen, DSGVO und EU AI Act
        Die in der App verfuegbaren KI-Funktionen sind als Assistenzfunktionen vorgesehen. KI-Ausgaben koennen unvollstaendig, fehlerhaft oder unpassend sein und muessen vor einer verbindlichen Nutzung geprueft werden. Bitte geben Sie keine geheimen, vertraulichen oder unnoetig sensiblen Daten Dritter ein.

        8. Workflow Automation und Webhook-Dienste
        Wenn Workflow-Automationen aktiviert sind, koennen serverseitig gepruefte Daten an den konfigurierten Workflow-Endpunkt, zum Beispiel n8n, uebermittelt werden.

        9. Empfaenger und Drittstaatentransfers
        Daten koennen an technische und operative Empfaenger uebermittelt werden, soweit dies fuer die App erforderlich ist, etwa an Hosting-, Speicher-, Authentifizierungs-, Sicherheits-, Zahlungs-, Fulfillment-, Support-, KI- oder Workflow-Dienstleister.

        10. Speicherdauer
        Wir speichern personenbezogene Daten nur so lange, wie dies fuer die jeweiligen Zwecke erforderlich ist.

        11. Ihre Rechte
        Sie haben nach Massgabe der gesetzlichen Voraussetzungen das Recht auf Auskunft, Berichtigung, Loeschung, Einschraenkung der Verarbeitung, Datenuebertragbarkeit sowie Widerspruch gegen bestimmte Verarbeitungen.

        12. Datensicherheit und Aenderungen
        Wir setzen angemessene technische und organisatorische Massnahmen ein, um Daten gegen Verlust, Missbrauch, unbefugten Zugriff oder unzulaessige Veraenderung zu schuetzen. Dazu gehoeren unter anderem Rollen- und Rechtemodelle, Upload-Freigaben, Security Rules, App Check, Lockdown-Mechanismen und Kostenkontrolle.
        """
    }

    static func termsAndConditions(using settings: LegalContentSettings) -> String {
        let brand = settings.resolvedBrandName
        let operatorName = settings.resolvedOperatorName
        let rightsHolder = settings.resolvedRightsHolderName
        let supportEmail = settings.resolvedSupportEmail
        let updatedAt = settings.resolvedLastUpdatedLabel
        let imprintReference = settings.resolvedImprintReference

        return """
        Allgemeine Geschaeftsbedingungen (AGB) fuer \(brand)

        Zuletzt aktualisiert: \(updatedAt)

        1. Geltungsbereich
        Diese AGB gelten fuer entgeltliche Leistungen, Bestellungen und vermittelte Kaeufe ueber \(brand), insbesondere fuer Merchandise, digitale Inhalte, Creator-/Studio-Leistungen sowie sonstige App-Services, soweit im konkreten Angebot nichts Abweichendes geregelt ist.

        2. Vertragspartner
        Vertragspartner ist voruebergehend \(operatorName).
        Rechteinhaber der App ist voruebergehend \(rightsHolder).
        Rueckfragen koennen an \(supportEmail) gerichtet werden.
        \(imprintReference)

        3. Vertragsschluss
        Die Darstellung von Produkten oder Leistungen in der App stellt noch kein verbindliches Angebot dar. Ein Vertrag kommt erst zustande, wenn Ihre Bestellung angenommen, Ihr Auftrag bestaetigt oder nach erfolgreicher Zahlung freigegeben wird.

        4. Preise, Zahlung und Versand
        Alle in der App angezeigten Preise, Versandkosten und Zahlungsarten werden vor Abschluss des Kaufs angezeigt.

        5. Lieferung und Fulfillment
        Lieferzeiten koennen je nach Produkt, Zielland, Verfuegbarkeit, Produktionsdauer und Fulfillment-Partner variieren.

        6. Digitale Leistungen, KI und Workflow-Services
        Soweit ueber die App digitale Inhalte, Creator-Leistungen, KI-gestuetzte Unterstuetzung oder Workflow-Services angeboten werden, dienen diese regelmaessig der kreativen oder technischen Unterstuetzung.

        7. Eigentumsvorbehalt
        Gelieferte Ware bleibt bis zur vollstaendigen Bezahlung unser Eigentum, soweit gesetzlich zulaessig.

        8. Widerrufsrecht
        Verbrauchern steht bei Fernabsatzvertraegen grundsaetzlich ein gesetzliches Widerrufsrecht zu.

        9. Gewaehrleistung, Produktsicherheit und Rueckrufe
        Es gelten die gesetzlichen Maengelrechte.

        10. Haftung
        Wir haften unbeschraenkt bei Vorsatz, grober Fahrlaessigkeit, Verletzung von Leben, Koerper oder Gesundheit sowie in den gesetzlich zwingenden Faellen.

        11. Streitbeilegung
        Informationen zur alternativen Streitbeilegung richten sich nach dem jeweils anwendbaren Recht.

        12. Schlussbestimmungen
        Es gilt deutsches Recht unter Ausschluss zwingender Verbraucherschutzvorschriften Ihres gewoehnlichen Aufenthaltsstaates, soweit diese anwendbar bleiben.
        """
    }

    static func termsOfService(using settings: LegalContentSettings) -> String {
        let brand = settings.resolvedBrandName
        let operatorName = settings.resolvedOperatorName
        let rightsHolder = settings.resolvedRightsHolderName
        let supportEmail = settings.resolvedSupportEmail
        let updatedAt = settings.resolvedLastUpdatedLabel

        return """
        Nutzungsbedingungen fuer \(brand)

        Zuletzt aktualisiert: \(updatedAt)

        Diese Nutzungsbedingungen regeln den Zugang zu \(brand) und die Nutzung der Inhalte, Profil-, Medien-, Shop-, KI- und Workflow-Funktionen innerhalb der App.

        1. Annahme der Bedingungen
        Mit der Nutzung der App bestaetigen Sie, dass Sie diese Bedingungen gelesen haben und akzeptieren.

        2. Aenderungen der Bedingungen
        Wir koennen diese Bedingungen aktualisieren.

        3. Konto und Zugang
        Fuer bestimmte Funktionen ist ein Konto erforderlich.

        4. Zulaessige Nutzung
        Die App darf nur rechtmaessig und gemaess diesen Bedingungen genutzt werden.

        5. Profile, Uploads und Moderation
        Wenn Sie Profile, Galerien, Artist-Seiten oder andere Inhalte einstellen, sichern Sie zu, hierzu berechtigt zu sein.

        6. Inhalte, Rechte und Marken
        Rechteinhaber der App ist voruebergehend \(rightsHolder). Die App, ihre Marken, Designs, Texte, Bilder, Audio-, Video- und sonstigen Inhalte bleiben, soweit nicht anders angegeben, Eigentum von \(rightsHolder), beteiligten Kuenstlern oder der jeweiligen Rechteinhaber.

        7. KI-Funktionen und EU AI Act
        KI-Funktionen sind als Assistenzfunktionen gedacht. KI-Ausgaben koennen fehlerhaft, unvollstaendig oder ungeeignet sein und muessen vor jeder verbindlichen Nutzung eigenverantwortlich geprueft werden.

        8. Workflow Automation und externe Endpunkte
        Wenn Workflow-Automationen aktiv sind, werden Anfragen ueber serverseitig gepruefte Endpunkte an den konfigurierten Workflow weitergeleitet.

        9. Verfuegbarkeit und Aenderungen
        Wir bemuehen uns um eine moeglichst stabile Verfuegbarkeit, schulden jedoch keine jederzeit unterbrechungsfreie oder fehlerfreie Nutzung.

        10. Sperrung, Kuendigung und Recovery
        Wir koennen Konten, Inhalte oder Zugaenge sperren, beschraenken oder beenden, wenn gegen diese Bedingungen verstossen wird.

        11. Haftung
        Es gelten die gesetzlichen Haftungsregeln.

        12. Geltendes Recht und Kontakt
        Vertragspartner und Betreiber ist voruebergehend \(operatorName). Fragen zu diesen Bedingungen koennen an \(supportEmail) gerichtet werden.
        """
    }

    static func appGuide(using settings: LegalContentSettings) -> String {
        let brand = settings.resolvedBrandName
        let operatorName = settings.resolvedOperatorName
        let supportEmail = settings.resolvedSupportEmail
        let updatedAt = settings.resolvedLastUpdatedLabel
        let masterNumberMeaning = settings.resolvedMasterNumberMeaning
        let brandManifesto = settings.resolvedBrandManifesto
        let symbolicNumericCode = settings.resolvedSymbolicNumericCode
        let symbolicLeetCode = settings.resolvedSymbolicLeetCode
        let symbolicCodeExplanation = settings.resolvedSymbolicCodeExplanation

        return """
        README / App Guide fuer \(brand)

        Zuletzt aktualisiert: \(updatedAt)

        Dieser Guide gibt dir einen schnellen und praxisnahen Einstieg in die App.

        1. Schnellstart
        - Konto erstellen oder einloggen
        - Profil mit Bild, Bio und Links vervollstaendigen
        - Home, Music, Video, Shop und AI als Kernmodule nutzen

        2. Rollen im System
        - Owner: volle Steuerung (Rollen, Limits, Einstellungen, Rechtliches)
        - Admin/Subadmin: operative Bereiche je nach Freigabe
        - User: eigenes Profil, eigene Inhalte, KI innerhalb der freigegebenen Kontingente

        3. KI Bot nutzen
        - Text-Modus: Caption-Ideen, Hooks, Konzepte, Copy
        - Visual-Modus: Key-Visual-Ideen und Creative-Richtungen
        - KI-Ausgaben immer vor verbindlicher Nutzung pruefen

        4. Agent und Workflows
        - Agent dient fuer umsetzungsorientierte Aufgaben statt nur Chat-Antworten
        - Optional kannst du eigene Workflow-Dienste (z. B. n8n/Webhook) hinterlegen
        - Je nach Setup koennen externe Services und eigene API-Kosten entstehen

        5. Kostenkontrolle
        - Kontingente, Hard Caps und globale Tageslimits begrenzen das Risiko
        - Bei deaktiviertem Zugriff oder erreichten Limits werden Aktionen blockiert

        6. Content und Commerce
        - Music/Video fuer Releases, Artist-Pages und Collabo-Inhalte
        - Shop fuer Merchandise und Checkout-Faelle gemaess aktivierter Zahlarten

        7. Support und Troubleshooting
        - Bei Login-, Rollen- oder Upload-Problemen App neu anmelden und Rechte pruefen
        - Fuer Hilfe: \(supportEmail)
        - Betreiber der App ist voruebergehend \(operatorName)

        8. Brand-DNA und Bedeutung von 22
        - Meisterzahl 22: \(masterNumberMeaning)
        - Wer wir sind:
        \(brandManifesto)
        - Symbolcode: \(symbolicNumericCode)
        - Leet-Code: \(symbolicLeetCode)
        - Deutung:
        \(symbolicCodeExplanation)
        """
    }
}
