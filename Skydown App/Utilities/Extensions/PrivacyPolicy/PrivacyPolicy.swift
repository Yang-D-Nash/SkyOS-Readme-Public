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
        let rightsHolder = settings.resolvedRightsHolderName
        let supportEmail = settings.resolvedSupportEmail
        let updatedAt = settings.resolvedLastUpdatedLabel
        let imprintReference = settings.resolvedImprintReference

        return """
        Datenschutz fuer \(brand)

        Stand: \(updatedAt)

        Hinweise zur Verarbeitung personenbezogener Daten bei \(brand) (Creator-/Workflow-App: Profile, Medien, Commerce, KI, Automation). Massgeblich: DSGVO, Telemedien-/Endgeraeteregeln; bei KI zusaetzlich der EU AI Act nach jeweils geltendem Stand.

        1. Verantwortlicher
        Verantwortlich: \(operatorName). Rechteinhaber: \(rightsHolder). Datenschutz: \(supportEmail).
        \(imprintReference)

        2. Datenarten
        u.a. Kontodaten (E-Mail, Nutzername, Rolle, Kontingent, Login/Sitzung); freiwillige Profildaten inkl. oeffentlicher Creator-/Artist-Angaben; technische Daten (App-Version, OS, Geraet, Crashes, Security-/Abuse-Logs); Nutzung (Musik, Video, Shop, Bestellungen, KI, Zahlarten, Einstellungen); Bestell-/Versand-/Fulfillmentdaten; von Ihnen aktive Uploads, Veroeffentlichungen, Prompts und Support-/Workflow-Anfragen.

        3. Zwecke und Rechtsgrundlagen
        App und Konto: Art. 6 Abs. 1 lit. b DSGVO. Bestellungen, Versand, Support: lit. b. Rechtspflichten: lit. c. Sicherheit, Missbrauchsabwehr, App Check, Rollen, Weiterentwicklung: lit. f. Optionale Zustimmungen/Freigaben: lit. a. Besondere Datenkategorien: moeglichst nicht; freiwillige Eingaben nur mit tragfaehiger Grundlage.

        4. Firebase / Google Cloud
        Auth, Firestore, Storage, Functions, App Check, Logging, Kostenkontrolle koennen fuer sichere Bereitstellung, Rechtepruefung, Upload-Freigaben und Abuse-Schutz genutzt werden.

        5. Oeffentliche Inhalte
        Freigegebene Profile, Galerien und Artist-Seiten koennen fuer andere oder oeffentlich sichtbar sein — nur Inhalte mit eigener Berechtigung.

        6. Zahlung, Fulfillment
        Externe Anbieter (z. B. Shopify, Zahlung, Versand, Fulfillment) erhalten nur fuer Abwicklung, Zustellung, Support und Nachweis noetige Daten.

        7. KI und EU AI Act
        KI ist Assistenz; Ausgaben vor verbindlicher Nutzung pruefen. Keine unnoetig sensiblen oder fremden Geheimnisse in Prompts.

        8. Workflows / Webhooks
        Aktivierte Automationen: serverseitig gepruefte Uebermittlung an den konfigurierten Endpunkt (z. B. n8n). BYOS: externer Zugang in Ihrer Verantwortung. Konfigurationen sind pro Konto getrennt (Rechte, Kosten).

        9. Empfaenger, Drittstaaten
        Hosting, Auth, Zahlung, Fulfillment, Support, KI, Workflow — soweit fuer den Betrieb erforderlich.

        10. Speicherdauer
        Nur solange fuer die Zwecke erforderlich.

        11. Betroffenenrechte
        Nach Massgabe des Gesetzes: Auskunft, Berichtigung, Loeschung, Einschraenkung, Datenportabilitaet, Widerspruch.

        12. Sicherheit
        TOM inkl. Rollenmodelle, Security Rules, App Check, Upload-Freigaben und Kostenkontrolle.
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
        AGB fuer \(brand)

        Stand: \(updatedAt)

        1. Geltungsbereich
        Fuer entgeltliche Leistungen, Bestellungen und vermittelte Kaeufe ueber \(brand) (Merch, digitale Inhalte, Creator-/KI-/Workflow-Services), soweit im Angebot nicht anders geregelt.

        2. Vertragspartner
        \(operatorName). Rechteinhaber App: \(rightsHolder). Kontakt: \(supportEmail).
        \(imprintReference)

        3. Vertragsschluss
        App-Darstellung ist unverbindlich. Vertrag erst bei Annahme, Auftragsbestaetigung oder Zahlungsfreigabe.

        4. Preise, Zahlung, Versand
        Preise, Versand und Zahlungsarten vor Kauf sichtbar; massgeblich der Checkout unmittelbar vor Abschluss.

        5. Lieferung
        Lieferzeiten variieren je nach Produkt, Land, Verfuegbarkeit und Fulfillment-Partner.

        6. Digitale Leistungen, KI, Workflows
        Unterstuetzung, keine Rechts-/Steuer-/Medizinberatung. KI-Ergebnisse vor verbindlicher Nutzung pruefen.

        7. Eigentumsvorbehalt
        Bis zur vollen Bezahlung, soweit zulaessig.

        8. Widerruf
        Fernabsatz: gesetzliches Widerrufsrecht fuer Verbraucher, soweit anwendbar.

        9. Gewaehrleistung, Sicherheit, Rueckrufe
        Gesetzliche Maengelrechte; Hinweise ueber geeignete Kanaele.

        10. Haftung
        Unbeschraenkt bei Vorsatz, grober Fahrlaessigkeit, Leben/Koerper/Gesundheit und gesetzlich zwingend.

        11. Streitbeilegung
        Nach anwendbarem Recht.

        12. Schluss
        Deutsches Recht; zwingender Verbraucherschutz des Aufenthaltsstaates bleibt unberuehrt, soweit einschlaegig.
        """
    }

    static func termsOfService(using settings: LegalContentSettings) -> String {
        let brand = settings.resolvedBrandName
        let operatorName = settings.resolvedOperatorName
        let rightsHolder = settings.resolvedRightsHolderName
        let supportEmail = settings.resolvedSupportEmail
        let updatedAt = settings.resolvedLastUpdatedLabel

        return """
        Nutzungsbedingungen \(brand)

        Stand: \(updatedAt)

        Zugang und Nutzung von Profil, Medien, Shop, KI und Workflows in \(brand) — einheitlich fuer alle Produktbereiche.

        1. Annahme
        Nutzung der App = Kenntnisnahme und Zustimmung zu diesen Bedingungen.

        2. Aenderungen
        Bedingungen koennen aktualisiert werden.

        3. Konto
        Wo erforderlich: korrekte Angaben, sichere Zugangsdaten, rechtmassige Nutzung.

        4. Zulaessige Nutzung
        Nur im Rahmen dieser Bedingungen und des geltenden Rechts.

        5. Profile, Uploads, Moderation
        Nur berechtigte Inhalte. Eingriff bei Rechtsverletzung, Risiko, Missbrauch oder Verstoss moeglich.

        6. Rechte
        Rechteinhaber App: \(rightsHolder). Marken, Designs, Medien: \(rightsHolder), Kuenstler oder jeweilige Inhaber, soweit nicht anders gekennzeichnet.

        7. KI, EU AI Act
        Assistenz; Ausgaben vor verbindlicher Nutzung pruefen.

        8. Workflows
        Aktive Automationen: Weiterleitung ueber gepruefte Endpunkte an den konfigurierten Dienst.

        9. Verfuegbarkeit
        Kein Anspruch auf permanente, fehlerfreie Nutzung; Aenderungen an Features und Preisen vorbehalten.

        10. Sperrung / Ende
        Bei Verstoss: Sperre, Beschraenkung oder Beendigung moeglich.

        11. Haftung
        Gesetzliche Regeln.

        12. Kontakt
        Betreiber: \(operatorName). \(supportEmail)
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
        SkyOS Guide · \(brand)

        Stand: \(updatedAt)

        1. Start
        Konto → Profil (Bild, Bio, Links) → Home, dann Music, Video, Shop, AI.

        2. Rollen
        Owner: Rollen, Limits, Settings, Legal. Admin/Subadmin: nach Freigabe. User: eigenes Profil, Inhalte, KI im Kontingent.

        3. KI
        Text: Copy, Hooks, Konzepte. Visual: Richtungen. Immer vor Nutzung pruefen.

        4. Agent / Workflows
        Umsetzung statt nur Chat. Optional n8n/Webhook — externe Kosten moeglich. Konfiguration pro Konto getrennt.

        5. Limits
        Kontingente und Caps; bei Limit oder Sperre: Aktionen blockiert.

        6. Content / Shop
        Music/Video, Artist-Pages, Merch-Checkout nach aktivierten Zahlarten.

        7. Hilfe
        Login/Rollen/Upload: neu anmelden, Rechte pruefen. \(supportEmail) · \(operatorName)

        8. Brand
        22: \(masterNumberMeaning)
        \(brandManifesto)
        Code: \(symbolicNumericCode) · \(symbolicLeetCode)
        \(symbolicCodeExplanation)
        """
    }
}
