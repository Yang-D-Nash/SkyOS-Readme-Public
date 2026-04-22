# SkyOS Architecture

## App Overview

SkyOS besteht aus nativen iOS- und Android-Clients mit Firebase-zentriertem Backend und serverseitiger Access-/Entitlement-Logik.

## Zentrale Fluesse

- Client UI -> Firebase Functions -> Firestore/Services
- Membership/Billing -> Entitlement Sync -> Access Decisions
- AI Request -> Guardrails -> Provider Routing -> Usage Metering

## Entitlements

- Canonical Entitlement als Source of Truth fuer Serving.
- Event-basierte Nachvollziehbarkeit ueber Entitlement Events.
- Resolver mit canonical-first Ansatz.

## AI Pipeline

- Anfrageklassifizierung nach Feature-Klasse.
- Plan-/Limit-/Guardrail-Pruefung vor teuren Aktionen.
- Ergebnisvertrag mit ruhigen Deny/Degrade-Metadaten fuer UI.

## Analytics und Revenue Ops

- Membership Funnel Events in Analytics.
- Owner/Admin Callables fuer KPI, Trends, Recommendations, Impact und Lifecycle.
- Timeline/Audit Trail fuer operative Nachvollziehbarkeit.

## Membership System

- Capability-basierte Plaene (kein Token-Shop).
- Native Kaufpfade je Plattform.
- Restore/Refresh/Planstatus via Membership Coordinator und Sync-Flows.
