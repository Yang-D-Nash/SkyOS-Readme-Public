//
//  PrivacyPolicy.swift
//  Skydown App
//
//  Created by Yang D. Nash on 29.07.25.
//

import Foundation

extension String {
    static let privacyPolicyText = """
    Datenschutzbestimmungen fuer Skydown x 22

    Zuletzt aktualisiert: 3. April 2026

    Diese Datenschutzbestimmungen informieren Sie ueber die Verarbeitung personenbezogener Daten bei der Nutzung von Skydown x 22. Sie beruecksichtigen insbesondere die DSGVO, die fuer Telemedien und Endgeraetezugriffe einschlaegigen Datenschutzvorgaben sowie den stufenweise geltenden EU AI Act, soweit KI-Funktionen in der App genutzt werden.

    1. Verantwortlicher
    Verantwortlich ist der Betreiber von Skydown x 22.
    Kontakt fuer Datenschutzanfragen: skydownent@gmail.com.
    Die vollstaendige Anbieterkennzeichnung einschliesslich ladungsfaehiger Anschrift ist im Impressum, Store-Eintrag oder einem gleichwertigen Anbieterprofil bereitzuhalten.

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
    Wenn Sie Profilangaben, Galerieinhalte, Artist-Seiten oder andere Inhalte zur oeffentlichen Darstellung freigeben, koennen diese fuer andere Nutzer oder fuer die Oeffentlichkeit sichtbar sein. Stellen Sie bitte nur Inhalte ein, fuer die Sie die erforderlichen Rechte besitzen und die keine Rechte Dritter verletzen. Oeffentlich sichtbare Inhalte koennen von Suchmaschinen, Plattformen oder Dritten wahrgenommen, gespeichert oder weiterverarbeitet werden.

    6. Bestellungen, Zahlungen und Fulfillment
    Fuer Merchandise, digitale Leistungen oder sonstige entgeltliche Angebote koennen externe Dienstleister eingebunden werden, insbesondere Shopify, Fulfillment-Partner, Zahlungsdienstleister sowie Versand- oder Kommunikationsdienste. Zahlungsdaten werden in der Regel direkt durch den jeweiligen Zahlungsdienstleister verarbeitet; wir erhalten typischerweise nur Status-, Referenz- und Abwicklungsdaten, soweit dies fuer Vertragserfuellung, Support oder Nachweise erforderlich ist.

    7. KI-Funktionen, DSGVO und EU AI Act
    Die in der App verfuegbaren KI-Funktionen sind als Assistenzfunktionen vorgesehen. KI-Ausgaben koennen unvollstaendig, fehlerhaft oder unpassend sein und muessen vor einer verbindlichen Nutzung geprueft werden. Bitte geben Sie keine geheimen, vertraulichen oder unnoetig sensiblen Daten Dritter ein. Soweit KI-Funktionen aktiv genutzt werden, koennen Prompts, hochgeladene Inhalte und Ausgaben an den jeweils eingebundenen KI-Dienst uebermittelt werden, soweit dies fuer die Anfrage technisch erforderlich ist. Die KI-Funktionen sind nicht dafuer bestimmt, ohne menschliche Pruefung rechtlich erhebliche oder in vergleichbarer Weise wesentliche Entscheidungen ueber Personen zu treffen. Zum Stand April 2026 wird der EU AI Act stufenweise anwendbar; bereits einschlaegige Verbote unzulaessiger Praktiken sowie Anforderungen an KI-Kompetenz und Transparenz werden bei der Gestaltung der KI-Funktionen risikobasiert beruecksichtigt.

    8. Workflow Automation und Webhook-Dienste
    Wenn der Owner Workflow-Automationen aktiviert, koennen serverseitig gepruefte Daten an den konfigurierten Workflow-Endpunkt, zum Beispiel n8n, uebermittelt werden. Dazu koennen insbesondere UID, E-Mail-Adresse, Benutzername, Rolle, Admin-/Owner-Status, Trigger, Zeitstempel und die fuer den jeweiligen Workflow uebergebenen Anfragedaten gehoeren. Die Uebermittlung erfolgt nur, wenn die Automation aktiv konfiguriert ist. Der Owner ist dafuer verantwortlich, den Ziel-Workflow datenschutzkonform zu konfigurieren und nur notwendige Daten verarbeiten zu lassen.

    9. Empfaenger und Drittstaatentransfers
    Daten koennen an technische und operative Empfaenger uebermittelt werden, soweit dies fuer die App erforderlich ist, etwa an Hosting-, Speicher-, Authentifizierungs-, Sicherheits-, Zahlungs-, Fulfillment-, Support-, KI- oder Workflow-Dienstleister. Sofern Empfaenger Daten ausserhalb des EWR verarbeiten, erfolgt dies nur auf Grundlage geeigneter Garantien, insbesondere Angemessenheitsbeschluessen, Standardvertragsklauseln oder sonstigen gesetzlich vorgesehenen Transfermechanismen, soweit erforderlich.

    10. Speicherdauer
    Wir speichern personenbezogene Daten nur so lange, wie dies fuer die jeweiligen Zwecke erforderlich ist. Kontodaten, Profilinhalte, Support-Kommunikation, Sicherheitslogs, Bestell- und Abwicklungsdaten koennen laenger gespeichert bleiben, soweit gesetzliche Aufbewahrungs-, Nachweis- oder Verjaehrungsfristen dies verlangen oder berechtigte Interessen an Missbrauchsabwehr, Recovery und Dokumentation bestehen.

    11. Ihre Rechte
    Sie haben nach Massgabe der gesetzlichen Voraussetzungen das Recht auf Auskunft, Berichtigung, Loeschung, Einschraenkung der Verarbeitung, Datenuebertragbarkeit sowie Widerspruch gegen bestimmte Verarbeitungen. Erteilte Einwilligungen koennen Sie mit Wirkung fuer die Zukunft widerrufen. Zudem besteht ein Beschwerderecht bei einer Datenschutz-Aufsichtsbehoerde.

    12. Datensicherheit und Aenderungen
    Wir setzen angemessene technische und organisatorische Massnahmen ein, um Daten gegen Verlust, Missbrauch, unbefugten Zugriff oder unzulaessige Veraenderung zu schuetzen. Dazu gehoeren unter anderem Rollen- und Rechtemodelle, Upload-Freigaben, Security Rules, App Check, Lockdown-Mechanismen und Kostenkontrolle. Eine vollstaendig risikofreie elektronische Verarbeitung kann dennoch nicht garantiert werden. Diese Datenschutzbestimmungen koennen aktualisiert werden; bei wesentlichen Aenderungen wird das Aktualisierungsdatum angepasst.
    """

    static let termsAndConditionsText = """
    Allgemeine Geschaeftsbedingungen (AGB) fuer Skydown x 22

    Zuletzt aktualisiert: 3. April 2026

    1. Geltungsbereich
    Diese AGB gelten fuer entgeltliche Leistungen, Bestellungen und vermittelte Kaeufe ueber Skydown x 22, insbesondere fuer Merchandise, digitale Inhalte, Creator-/Studio-Leistungen sowie sonstige App-Services, soweit im konkreten Angebot nichts Abweichendes geregelt ist.

    2. Vertragspartner
    Vertragspartner ist der Betreiber von Skydown x 22. Rueckfragen koennen an skydownent@gmail.com gerichtet werden. Die vollstaendige Anbieterkennzeichnung einschliesslich ladungsfaehiger Anschrift ist im Impressum oder Anbieterprofil bereitzuhalten.

    3. Vertragsschluss
    Die Darstellung von Produkten oder Leistungen in der App stellt noch kein verbindliches Angebot dar. Ein Vertrag kommt erst zustande, wenn Ihre Bestellung angenommen, Ihr Auftrag bestaetigt oder nach erfolgreicher Zahlung freigegeben wird. Fuer bestimmte Leistungen koennen zusaetzliche Briefings, Freigaben oder individuelle Abstimmungen erforderlich sein.

    4. Preise, Zahlung und Versand
    Alle in der App angezeigten Preise, Versandkosten und Zahlungsarten werden vor Abschluss des Kaufs angezeigt. Die verfuegbaren Zahlarten richten sich nach dem jeweils aktivierten Checkout und den hinterlegten Zahlungsdiensten. Bei grenzueberschreitenden Lieferungen koennen zusaetzliche Steuern, Zoelle oder Importgebuehren anfallen, die nicht von uns beeinflusst werden.

    5. Lieferung und Fulfillment
    Lieferzeiten koennen je nach Produkt, Zielland, Verfuegbarkeit, Produktionsdauer und Fulfillment-Partner variieren. Fuer bestimmte Bestellungen koennen externe Fulfillment-Dienste wie Shopify, PODpartner oder sonstige Versand- und Produktionspartner eingesetzt werden. Teillieferungen sind zulaessig, soweit dies fuer Sie zumutbar ist.

    6. Digitale Leistungen, KI und Workflow-Services
    Soweit ueber die App digitale Inhalte, Creator-Leistungen, KI-gestuetzte Unterstuetzung oder Workflow-Services angeboten werden, dienen diese regelmaessig der kreativen oder technischen Unterstuetzung. KI- oder Automationsausgaben koennen fachliche, rechtliche oder wirtschaftliche Pruefungen nicht ersetzen. Der konkrete Leistungsumfang ergibt sich aus der jeweiligen Leistungsbeschreibung, dem Checkout oder einer zusaetzlichen individuellen Vereinbarung.

    7. Eigentumsvorbehalt
    Gelieferte Ware bleibt bis zur vollstaendigen Bezahlung unser Eigentum, soweit gesetzlich zulaessig.

    8. Widerrufsrecht
    Verbrauchern steht bei Fernabsatzvertraegen grundsaetzlich ein gesetzliches Widerrufsrecht zu. Ausnahmen gelten insbesondere fuer nicht vorgefertigte oder personalisierte Waren sowie fuer digitale Inhalte oder digitale Dienstleistungen, wenn mit der Ausfuehrung erst begonnen wurde, nachdem Sie ausdruecklich zugestimmt und bestaetigt haben, dass Sie hierdurch Ihr Widerrufsrecht verlieren koennen.

    9. Gewaehrleistung, Produktsicherheit und Rueckrufe
    Es gelten die gesetzlichen Maengelrechte. Bitte pruefen Sie gelieferte Waren moeglichst zeitnah und kontaktieren Sie uns bei erkennbaren Problemen ueber die in der App oder im Support genannten Kanaele. Soweit fuer physische Produkte produkt- oder sicherheitsrechtliche Hinweise, Warnungen oder Rueckrufe erforderlich werden, koennen diese ueber die hinterlegten Kontaktwege kommuniziert werden.

    10. Haftung
    Wir haften unbeschraenkt bei Vorsatz, grober Fahrlaessigkeit, Verletzung von Leben, Koerper oder Gesundheit sowie in den gesetzlich zwingenden Faellen. Bei leicht fahrlaessiger Verletzung wesentlicher Vertragspflichten ist die Haftung auf den vorhersehbaren, vertragstypischen Schaden begrenzt. Im Uebrigen ist die Haftung ausgeschlossen, soweit gesetzlich zulaessig.

    11. Streitbeilegung
    Informationen zur alternativen Streitbeilegung richten sich nach dem jeweils anwendbaren Recht. Die fruehere EU-Online-Streitbeilegungsplattform wurde zum 20. Juli 2025 eingestellt. Eine Verpflichtung zur Teilnahme an Streitbeilegungsverfahren vor einer Verbraucherschlichtungsstelle besteht nur, wenn dies gesetzlich zwingend ist oder ausdruecklich erklaert wird.

    12. Schlussbestimmungen
    Es gilt deutsches Recht unter Ausschluss zwingender Verbraucherschutzvorschriften Ihres gewoehnlichen Aufenthaltsstaates, soweit diese anwendbar bleiben.
    """

    static let termsOfServiceText = """
    Nutzungsbedingungen fuer Skydown x 22

    Zuletzt aktualisiert: 3. April 2026

    Diese Nutzungsbedingungen regeln den Zugang zu Skydown x 22 und die Nutzung der Inhalte, Profil-, Medien-, Shop-, KI- und Workflow-Funktionen innerhalb der App.

    1. Annahme der Bedingungen
    Mit der Nutzung der App bestaetigen Sie, dass Sie diese Bedingungen gelesen haben und akzeptieren. Wenn Sie nicht einverstanden sind, duerfen Sie die App nicht nutzen.

    2. Aenderungen der Bedingungen
    Wir koennen diese Bedingungen aktualisieren. Die weitere Nutzung nach einer Aktualisierung gilt als Zustimmung zur dann gueltigen Fassung.

    3. Konto und Zugang
    Fuer bestimmte Funktionen ist ein Konto erforderlich. Sie sind fuer die Vertraulichkeit Ihrer Zugangsdaten verantwortlich und muessen uns ueber missbraeuchliche Nutzung oder Sicherheitsvorfaelle unverzueglich informieren.

    4. Zulaessige Nutzung
    Die App darf nur rechtmaessig und gemaess diesen Bedingungen genutzt werden. Untersagt sind insbesondere Manipulationen, automatisierter Missbrauch, Umgehung technischer Schutzmassnahmen, unbefugter Zugriff, rechtswidrige Uploads, der Einsatz von Bots gegen Schutzmechanismen, die Nutzung fuer Spam, Betrug oder Belaestigung sowie jede Nutzung in einer Weise, die Rechte Dritter verletzt.

    5. Profile, Uploads und Moderation
    Wenn Sie Profile, Galerien, Artist-Seiten oder andere Inhalte einstellen, sichern Sie zu, hierzu berechtigt zu sein. Oeffentlich sichtbare Inhalte muessen rechtmaessig, wahrheitsgemaess und frei von Rechten Dritter sein. Rechtswidrige, verletzende, betruegerische oder offensichtlich missbraeuchliche Inhalte koennen entfernt, eingeschraenkt, depubliziert oder anlassbezogen geprueft werden. Soweit die App als Hosting- oder Vermittlungsdienst im Sinne der Verordnung (EU) 2022/2065 einzuordnen ist, koennen Hinweise auf rechtswidrige Inhalte ueber die Support-Kontaktwege gemeldet werden. Betroffene Nutzer koennen Rueckfragen oder Gegendarstellungen an skydownent@gmail.com richten.

    6. Inhalte, Rechte und Marken
    Die App, ihre Marken, Designs, Texte, Bilder, Audio-, Video- und sonstigen Inhalte bleiben, soweit nicht anders angegeben, Eigentum von Skydown x 22, Skydown Entertainment, beteiligten Kuenstlern oder der jeweiligen Rechteinhaber. Ohne vorherige Erlaubnis duerfen Inhalte nicht ueber den privaten oder gesetzlich erlaubten Rahmen hinaus verwendet werden.

    7. KI-Funktionen und EU AI Act
    KI-Funktionen sind als Assistenzfunktionen gedacht. KI-Ausgaben koennen fehlerhaft, unvollstaendig oder ungeeignet sein und muessen vor jeder verbindlichen Nutzung eigenverantwortlich geprueft werden. Sie duerfen keine geheimen oder unnoetig sensiblen Daten Dritter eingeben und keine verbotenen, missbraeuchlichen oder rechtswidrigen KI-Nutzungen verfolgen. Die KI-Funktionen sind nicht fuer ausschliesslich automatisierte Entscheidungen mit rechtlicher oder vergleichbar erheblicher Wirkung vorgesehen. Soweit Vorgaben der Verordnung (EU) 2024/1689 bereits anwendbar sind, werden Transparenz, menschliche Kontrolle und risikobasierte Schutzmechanismen beruecksichtigt.

    8. Workflow Automation und externe Endpunkte
    Wenn Workflow-Automationen aktiv sind, werden Anfragen ueber serverseitig gepruefte Endpunkte an den vom Owner konfigurierten Workflow weitergeleitet. Diese Automationen dienen der technischen oder kreativen Unterstuetzung und duerfen nicht fuer Notfaelle, ausschliesslich automatisierte Rechtsentscheidungen oder sonstige hochriskante Einsaetze ohne angemessene menschliche Kontrolle genutzt werden. Externe Workflow-Ziele koennen dem technischen und organisatorischen Verantwortungsbereich des jeweiligen Betreibers unterliegen.

    9. Verfuegbarkeit und Aenderungen
    Wir bemuehen uns um eine moeglichst stabile Verfuegbarkeit, schulden jedoch keine jederzeit unterbrechungsfreie oder fehlerfreie Nutzung. Funktionen, Inhalte, KI-Modelle, Integrationen, Zahlarten oder externe Dienste koennen geaendert, eingeschraenkt, ersetzt oder eingestellt werden.

    10. Sperrung, Kuendigung und Recovery
    Wir koennen Konten, Inhalte oder Zugaenge sperren, beschraenken oder beenden, wenn gegen diese Bedingungen verstossen wird, Rechte Dritter betroffen sind, Sicherheits- oder Kostenrisiken bestehen, ein Lockdown- oder Recovery-Modus aktiviert wird oder gesetzliche Gruende dies erfordern.

    11. Haftung
    Es gelten die gesetzlichen Haftungsregeln. Fuer unentgeltliche Dienste, technisch bedingte Ausfaelle und die Nutzung von KI- oder Workflow-Ausgaben ist die Haftung im gesetzlich zulaessigen Umfang eingeschraenkt. Die Haftung fuer Vorsatz, grobe Fahrlaessigkeit, Verletzung von Leben, Koerper oder Gesundheit sowie zwingende gesetzliche Ansprueche bleibt unberuehrt.

    12. Geltendes Recht und Kontakt
    Es gilt deutsches Recht, soweit dem keine zwingenden Verbraucherschutzvorschriften entgegenstehen. Fragen zu diesen Bedingungen koennen an skydownent@gmail.com gerichtet werden.
    """
}
