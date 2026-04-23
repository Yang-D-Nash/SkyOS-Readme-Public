# SkyOS Owner Guide

The Owner role is responsible for product integrity. Owner access is powerful because it touches revenue, legal content, runtime controls, user trust, recommendations, experiments, and operational health.

## Owner Responsibilities

Owner work should prioritize:

- platform stability
- membership and billing correctness
- permissions and role integrity
- legal and support readiness
- revenue operations quality
- AI and entitlement guardrails
- clean data lifecycle
- release go/no-go decisions

Owner access should never be used as a shortcut for unreviewed live data edits.

## Membership Command Center

The Membership Command Center is the main operating surface for revenue and membership health.

Review:

- plan mix
- membership opens
- purchase starts and completions
- conversion rate
- restore behavior
- entitlement refresh signals
- alerts and anomalies
- recommendation state

Healthy behavior:

- metrics are clearly labelled
- empty states explain missing data
- recommendations have a lifecycle
- stale recommendations can be rejected or completed
- experiments record start, reject, complete, learnings, and timeline

## KPIs

Use KPIs to answer practical operating questions:

- Are users seeing membership at the right moments?
- Are upgrade hints creating confusion or useful context?
- Are purchases completing after store handoff?
- Are restores resolving support issues?
- Are alerts connected to real product risk?

Do not optimize a single metric at the expense of trust. A lower conversion rate with fewer confused billing/support cases can be healthier than aggressive prompt volume.

## Revenue Ops

Revenue Ops covers the connection between membership, AI usage, store purchases, recommendations, and operational response.

For each recommendation:

- confirm the underlying signal
- classify impact and risk
- decide whether to start, reject, or defer
- record rationale
- review after enough data
- complete with learnings when finished

Avoid starting experiments without a clear success criterion and rollback path.

## Experiments

Experiments should be small, observable, and reversible.

Before starting:

- define the affected surface
- document expected outcome
- confirm legal and billing copy is not misleading
- check that support can explain the change
- ensure analytics will capture the result

After completion:

- record the decision
- capture learnings
- clean up stale recommendation state
- update docs if behavior changed

## Alerts

Treat alerts as investigation prompts, not automatic truth.

Recommended triage:

1. Check whether the alert is current.
2. Confirm the affected area.
3. Compare with build, deploy, or rules changes.
4. Check whether support has related reports.
5. Decide whether to pause, roll back, or monitor.

Critical alerts around billing, membership, permissions, account access, AI availability, or data writes should be handled before visual polish work.

## Hygiene Controls

Hygiene controls exist to keep recommendation and analytics operations useful.

Use them to:

- reject stale or noisy recommendations
- complete outdated experiments
- review timeline integrity
- avoid duplicate operational actions
- keep the command center readable

Never delete live data blindly. Use audit, backup where appropriate, dry run, targeted cleanup, and verification.

## Legal Operations

Owner maintains legal content availability, not final legal approval.

Owner checks:

- Terms are reachable from Settings
- Privacy Policy is reachable from Settings
- Subscription Terms are reachable from Settings
- AI Usage Notice is reachable from Settings
- Impressum/Company Info is reachable from Settings
- Support route is visible
- last-updated labels are current

Before public release, external legal review is required.

## Owner Release Standard

Owner should approve release only when:

- iOS and Android builds pass
- Functions and rules tests pass
- membership and billing smoke tests pass on real platform paths
- live Firebase baseline is reviewed
- legal documents are approved
- crash/analytics monitoring is ready
- support knows the release state
- rollback path is known
