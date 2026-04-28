import {
  Callout,
  Divider,
  Grid,
  H1,
  H2,
  H3,
  Pill,
  Row,
  Stack,
  Stat,
  Table,
  Text,
} from "cursor/canvas";

export default function SkyOsCtoDeepDive() {
  return (
    <Stack gap={20}>
      <H1>SkyOS: CTO Product Deep Dive</H1>
      <Text tone="secondary">
        Brutal ehrliche Analyse fuer Produkt, UX, Engineering, Business und
        Wachstum. Stand: aktuelles Repository.
      </Text>

      <Callout tone="warning" title="Radikal ehrliche Kurzdiagnose">
        Starkes Premium-Branding und ambitionierte Vision, aber aktuell zu viel
        Komplexitaet in Kernflaechen (v. a. Settings, AI, Rollenlogik). Das
        limitiert Retention, Skalierungsgeschwindigkeit und Team-Effizienz.
      </Callout>

      <Grid columns={5} gap={12}>
        <Stat value="7.5/10" label="Produktvision" />
        <Stat value="5.5/10" label="PMF-Reife heute" />
        <Stat value="4.5/10" label="UX-Klarheit" tone="warning" />
        <Stat value="4.0/10" label="Architekturfitness" tone="warning" />
        <Stat value="7.0/10" label="Monetarisierungspotenzial" />
      </Grid>

      <Divider />
      <H2>1) Produktanalyse</H2>
      <Table
        headers={["Dimension", "Score", "Befund", "Empfehlung"]}
        rows={[
          [
            "Was ist das Produkt?",
            "7/10",
            "Creator Super-App: Productivity + Media + Commerce + AI + Trust",
            "Kern-Job klarer fokussieren (Creator Ops + Revenue OS)",
          ],
          [
            "Welches Problem?",
            "6/10",
            "Fragmentierte Creator-Tools in einer App vereinen",
            "Weniger Feature-Breite, mehr Outcomes (mehr Umsatz/Woche)",
          ],
          [
            "Fuer wen?",
            "5/10",
            "Mischung aus Fan, Creator, Owner/Admin in einem Produkt",
            "Segmente trennen: Consumer App vs Creator Console",
          ],
          [
            "PMF",
            "5.5/10",
            "Gute Value-Hypothese, aber hohe Komplexitaetslast",
            "Retention-first Iterationen statt weiterer Surface-Ausbau",
          ],
          [
            "Fehlende Features",
            "6/10",
            "Klarer North-Star-Flow, KPI-Dashboard, guided onboarding",
            "Outcome-Onboarding + weekly success score",
          ],
          [
            "Unnoetige Features",
            "4/10",
            "Admin/Owner-Operations in Endnutzer-Settings",
            "Owner-Konsole separieren",
          ],
          [
            "10x Potenzial",
            "8/10",
            "Starke Basis fuer vertical Creator OS",
            "Nische dominieren: Musik+Merch+AI Operator fuer Mid-tier Creator",
          ],
        ]}
      />

      <Divider />
      <H2>2) UX / UI Analyse</H2>
      <Grid columns={2} gap={16}>
        <Stack gap={8}>
          <H3>Friction Points</H3>
          <Text>- Intro/Onboarding mit hoher kognitiver Last</Text>
          <Text>- Zu viele Top-Level-Flows gleichzeitig</Text>
          <Text>- Wiederholte Auth-Unterbrechungen im Hauptfluss</Text>
          <Text>- AI-Modell fuer neue User schwer verstaendlich</Text>
          <Text>- Settings massiv ueberladen (God-Screen)</Text>
        </Stack>
        <Stack gap={8}>
          <H3>Retention Risiken</H3>
          <Text>- Feature-Komplexitaet vor schnellem Success-Moment</Text>
          <Text>- Nutzenversprechen nicht flow-basiert gefuehrt</Text>
          <Text>- Mixed-language Copy reduziert Premium-Vertrauen</Text>
          <Text>- Zu wenig klare Habit-Loops pro Zielsegment</Text>
          <Text>- Value-Communication bei Paywall nicht scharf genug</Text>
        </Stack>
      </Grid>

      <Table
        headers={["Flow", "Score", "Hauptproblem", "Fix"]}
        rows={[
          ["Onboarding", "4/10", "Zu viele Optionen zu frueh", "2-stufiges Guided Setup"],
          ["Auth", "5/10", "Context-Spruenge", "Intent-preserving Auth Return"],
          ["Home", "5/10", "Zu dicht und heterogen", "Segmentierte Home-Personas"],
          ["Music", "7/10", "Relativ klar", "Mehr Conversion Hooks in Merch/AI"],
          ["AI", "4/10", "Bot/Agent/Workflow komplex", "Progressive Disclosure + Presets"],
          ["Settings", "2/10", "God-Surface", "Split: User Settings / Creator Ops / Owner Console"],
        ]}
        rowTone={["warning", "warning", "warning", undefined, "warning", "critical"]}
      />

      <Divider />
      <H2>3) Codebase Analyse</H2>
      <Table
        headers={["Bereich", "Score", "Harte Beobachtung", "Konkrete Aktion"]}
        rows={[
          [
            "Architektur",
            "4/10",
            "Uebergrosse Dateien (z. B. Settings 6k+ / 7k+, functions index 15k+)",
            "Modularisieren nach Bounded Context",
          ],
          [
            "Skalierbarkeit",
            "4/10",
            "Hohe Kopplung, breite Blast Radius",
            "Feature Slices + klare Layering-Grenzen",
          ],
          [
            "Code Quality",
            "5/10",
            "Uneinheitliche Guardrails je Plattform",
            "Gleiche Quality Gates fuer iOS/Android/Functions",
          ],
          [
            "Security",
            "3/10",
            "Owner-Identity mehrfach hart codiert",
            "Single source of truth + sichere Rotation",
          ],
          [
            "Performance",
            "5/10",
            "Mehrfach-Reads/Refresh-Muster in kritischen VM-Flows",
            "Caching, batched reads, event-driven updates",
          ],
          [
            "Duplicate Logic",
            "3/10",
            "Auth/Role/Quota/Artist-Logik plattformdoppelt",
            "Shared-Domain konsequent nutzen",
          ],
          [
            "Technical Debt",
            "3/10",
            "Service-locator + UI/Firebase Mischlayer",
            "UseCase-Schicht und Repo-Contracts erzwingen",
          ],
        ]}
      />

      <Divider />
      <H2>4) Business Analyse</H2>
      <Table
        headers={["Hebel", "Heute", "Chance", "Empfehlung"]}
        rows={[
          [
            "Monetarisierung",
            "AI + Membership + Merch vorhanden",
            "Sehr hoch",
            "Bundles: AI + Merch + Membership in Outcome-Paketen",
          ],
          [
            "Pricing",
            "Noch feature-zentriert",
            "Hoch",
            "Outcome-Tiers (Starter / Pro / Studio) mit ROI-Story",
          ],
          [
            "Conversion",
            "Viele Entry Points, aber nicht einheitlich",
            "Hoch",
            "Ein primarer Funnel mit persona-spezifischer Landing",
          ],
          [
            "Growth Loops",
            "Angedeutet, nicht systemisch",
            "Sehr hoch",
            "Creator share loops + referral + collaborative workflows",
          ],
          [
            "Revenue Uplift",
            "Substanziell moeglich",
            "Sehr hoch",
            "Upsell im Kontext des erreichten Outcomes statt Limit-Block",
          ],
        ]}
      />

      <Divider />
      <H2>5) Marktanalyse (2026 Sicht)</H2>
      <Text>
        Konkurrenzcluster: Bandcamp/Shopify/Patreon (Monetization), Notion/Trello
        (Ops), neue AI-Creator-Tools (CreatorFlo, CreatorSense, Alera). Vorteil
        von SkyOS: integrierter Stack. Schwaeche: Komplexitaet und Positioning-Blur.
      </Text>
      <Table
        headers={["Frage", "Antwort"]}
        rows={[
          ["Wo Vorteil?", "Ein Produkt fuer Music+Merch+AI+Ops statt Tool-Patchwork"],
          ["Wo schwach?", "Zu breite Zielgruppe und hohe Bedienkomplexitaet"],
          ["Dominierbare Nische?", "Mid-tier Musik-Creator mit 5k-500k Audience"],
          ["Wie Marktfuehrer?", "Best-in-class weekly revenue outcomes + simple UX"],
        ]}
      />

      <Divider />
      <H2>6) AI / Automation Potenzial</H2>
      <Table
        headers={["AI Feature", "Impact", "Aufwand", "Prioritaet"]}
        rows={[
          ["AI Revenue Copilot (wöchentlicher Plan)", "Sehr hoch", "Mittel", "P1"],
          ["Auto Campaign Builder (Music+Merch)", "Hoch", "Mittel", "P1"],
          ["Offer Optimizer (Preis/Bundle Tests)", "Sehr hoch", "Hoch", "P2"],
          ["Churn Predictor + Save Actions", "Hoch", "Mittel", "P1"],
          ["Inbox/Brief-to-Task Automation", "Hoch", "Mittel", "P1"],
          ["Ops Autopilot fuer Owner Tasks", "Mittel", "Mittel", "P2"],
        ]}
      />

      <Divider />
      <H2>7) Prioritaetenplan</H2>
      <H3>A) Top 10 kritischste Probleme</H3>
      <Table
        headers={["#","Problem","Severity","Time-to-fix"]}
        rows={[
          ["1","God-Screen Settings (iOS/Android)","Critical","2-4 Wochen"],
          ["2","Hard-coded Owner-Identity Pattern","Critical","1-2 Wochen"],
          ["3","Backend Monolith (functions/index.js)","High","3-6 Wochen"],
          ["4","Cross-platform Duplicate Domain Logic","High","4-8 Wochen"],
          ["5","AI UX Complexity (Bot/Agent/Workflow)","High","2-4 Wochen"],
          ["6","Inkonsequente Lokalisierung / harte Strings","High","1-3 Wochen"],
          ["7","UI-Layer mit Datenzugriff vermischt","Medium","2-5 Wochen"],
          ["8","Uneinheitliche QA-Gates","Medium","1-2 Wochen"],
          ["9","Onboarding ohne klare Outcome-Fuehrung","High","1-2 Wochen"],
          ["10","Zu viele gleichgewichtete Top-Level-Flaechen","High","2-3 Wochen"],
        ]}
        rowTone={["critical","critical","warning","warning","warning","warning",undefined,undefined,"warning","warning"]}
      />

      <H3>B) Top 10 groesste Chancen</H3>
      <Table
        headers={["#","Chance","Upside"]}
        rows={[
          ["1","Nische Mid-tier Music Creator OS dominieren","Sehr hoch"],
          ["2","AI Revenue Copilot als Kernprodukt","Sehr hoch"],
          ["3","Role-based Produkt-Splits fuer Klarheit","Hoch"],
          ["4","Outcome-basierte Pricing Tiers","Sehr hoch"],
          ["5","Merch+Content+AI Bundles","Hoch"],
          ["6","Referral Loops fuer Creator Teams","Hoch"],
          ["7","Automatisierte Campaign Templates","Hoch"],
          ["8","Retention Engine (churn prevention)","Sehr hoch"],
          ["9","Partner-Integrationen (Shopify/Stripe tiefer)","Mittel-Hoch"],
          ["10","Enterprise-lite fuer Agenturen/Labels","Mittel-Hoch"],
        ]}
      />

      <H3>C) Top 10 Quick Wins (7 Tage)</H3>
      <Table
        headers={["#","Quick Win","Impact"]}
        rows={[
          ["1","Settings in 3 sichtbare Bereiche splitten (IA-only first)","Hoch"],
          ["2","Onboarding: Skip + guided first value","Hoch"],
          ["3","AI Hub vereinfachen: 1 default mode + advanced toggle","Hoch"],
          ["4","Hardcoded user strings extrahieren","Hoch"],
          ["5","Owner-Konstanten zentralisieren","Hoch"],
          ["6","Paywall copy auf Outcome umstellen","Mittel-Hoch"],
          ["7","Auth return-to-intent sauber implementieren","Mittel-Hoch"],
          ["8","D1/D7 KPI dashboard in app/admin sichtbar","Mittel"],
          ["9","Duplicate cleanup scripts konsolidieren","Mittel"],
          ["10","Top-3 critical backend functions modular auslagern","Mittel"],
        ]}
      />

      <H3>D) Top 10 Wachstumshebel</H3>
      <Table
        headers={["#","Hebel","Mechanik"]}
        rows={[
          ["1","Weekly Revenue Plan","Habit + measurable ROI"],
          ["2","Creator Referral Program","Viral acquisition"],
          ["3","Collab Workspaces","Team retention"],
          ["4","Launch Templates","Faster success-time"],
          ["5","Creator Scoreboard","Gamified progress"],
          ["6","Cross-sell Merch in Music flow","Higher ARPU"],
          ["7","AI Campaign Auto-Builder","Increased activity"],
          ["8","Trial-to-paid optimization","Higher conversion"],
          ["9","Winback automations","Lower churn"],
          ["10","Localized growth pages","International conversion"],
        ]}
      />

      <H3>E) Roadmap 30 / 90 / 180 Tage</H3>
      <Table
        headers={["Zeitraum","Ziel","Deliverables"]}
        rows={[
          [
            "30 Tage",
            "Komplexitaet senken",
            "Settings split, onboarding reset, AI hub simplify, security constants cleanup",
          ],
          [
            "90 Tage",
            "Retention + Monetization heben",
            "Outcome pricing, churn model v1, referral loop, modular backend phase 1",
          ],
          [
            "180 Tage",
            "Kategorie-Fuehrerschaft in Nische",
            "Creator OS v2, partner ecosystem, scale architecture, category proof metrics",
          ],
        ]}
      />

      <Divider />
      <H2>8) Was loeschen / umbauen?</H2>
      <Row gap={8}>
        <Pill tone="critical">Loeschen</Pill>
        <Text>
          Ueberfluessige doppelte Script-Pfade und redundante Owner/Admin-UI in
          Endnutzeroberflaechen.
        </Text>
      </Row>
      <Row gap={8}>
        <Pill tone="warning">Umbauen</Pill>
        <Text>
          Functions-Monolith, Settings-God-Screens, cross-platform Duplikate von
          Role/Quota/Policy-Logik.
        </Text>
      </Row>
      <Row gap={8}>
        <Pill tone="success">Behalten/Staerken</Pill>
        <Text>
          Premium Brand-System, Security-Rules-Denke, AI + Commerce + Productivity
          als integriertes Grundgeruest.
        </Text>
      </Row>
    </Stack>
  );
}
