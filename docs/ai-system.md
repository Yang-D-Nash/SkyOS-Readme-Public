# SkyOS AI System

SkyOS AI is not one button. It is a layered system that combines assistant behavior, FAQ response,
visual generation, agent execution, owner runtime controls, and plan-aware usage rules.

## 1. AI Surfaces

SkyOS currently ships or prepares for these AI surfaces:

- bot-style text generation
- FAQ and support-oriented answer generation
- visual generation
- agent-style execution for deeper structured work
- owner review intelligence for FAQ quality and support maintenance

The product intent is to make AI useful inside the SkyOS system, not to bolt a generic chatbot onto it.

## 2. Backend Execution Model

Critical AI callables include:

- `generateAiText`
- `generateAiVisual`
- `skydownAgent`
- `authorizeAiUsage`
- `reconcileAiUsageCost`
- `getAiFaqOwnerIntelligence`
- recommendation and review-loop callables for FAQ and membership ops

This means AI execution is part of the backend trust model. Usage, permission checks, and provider
selection are not left only to the client.

## 3. Provider Routing

The current backend uses Genkit with Vertex AI and Gemini as the default execution backbone.
The repo also includes owner-facing runtime settings for:

- primary and fallback agent providers
- hard daily caps
- global daily caps
- per-plan user limits
- timeout and retry behavior
- external automation provider order
- optional Manus BYOS

SkyOS therefore supports both direct AI execution and controlled bridge patterns into external systems.

## 4. External Automation

The agent layer can integrate with:

- a global owner-managed Activepieces flow
- optional user-owned Activepieces or `n8n` flows
- optional Manus BYOS for user-owned agent execution

Important boundary: these are optional execution routes. They should be:

- routed through the backend, never directly from the client
- governed by owner runtime settings and role/plan limits
- scoped per account when the user owns the external workflow
- transparent to the user when automation is triggered
- blocked when kill switches or confirmation policies require it

## 5. Agent Product Contract

The SkyOS Agent is a saved, account-scoped chatbot/workflow surface, not a disposable prompt box.
Clients store Agent sessions and entries locally and sync them to Firestore so users can create,
rename, reopen, continue, and delete conversations. Those conversations also feed the bounded
memory layer according to the active quota plan and retention window.

Live Agent modes:

- `release` - release planning, launch sequencing, assets, risks, next steps
- `briefing` - creative/operational briefings for owner, team, content, video, or partners
- `content` - channel plans, hooks, captions, scripts, CTAs, repurposing
- `merch` - drop planning, shop copy, product/content checklist
- `automation` in clients, shown as **Analyse** - social/profile analysis and workflow handoff

Social Analysis supports Instagram, TikTok, YouTube, Facebook/Meta, and Spotify. The backend only
uses live data when the relevant provider key/token and account permission are valid at request time.
If a platform rejects the token, lacks scope, rate-limits, or cannot expose a public profile, SkyOS
must say that clearly and continue with handle/prompt context. It must not invent reach,
impressions, follower counts, revenue, retention, or engagement metrics.

Owner-facing answers should be concise and shareable. Home `Me` / `Group` briefing buttons request
fresh server data before rendering. User-facing workflow outputs should be readable in the app and
can create Tasks, Notes, or Reminders through the documented workflow endpoints when automation is
enabled and permitted.

## 6. Safety and Trust Controls

SkyOS already contains several real safeguards:

- role-derived quota defaults
- daily limits for text, visuals, and agent requests
- AI consent fields in user state
- retention windows for AI history
- runtime kill switches
- App Check-aware callable handling
- owner confirmation policies for sensitive categories such as commerce or owner ops

AI should never silently overstep permission, billing, or privacy expectations.

## 7. FAQ and Support Intelligence

SkyOS includes a FAQ review loop, not only answer generation. This enables:

- owner review of FAQ quality
- preview and application of FAQ improvements
- rollback of recent FAQ changes
- intelligence surfaces for support blind spots or stale guidance

This is part of product trust, not only internal tooling.

## 8. Membership and AI Access

AI in SkyOS is plan-aware. The system can:

- change usage ceilings by quota plan
- gate deeper usage behind paid capability tiers
- surface restore and upgrade actions from AI-adjacent contexts
- block usage during budget or abuse incidents

The repo positions AI as capability-based membership, not a token shop.

## 9. What AI Is Not Allowed To Be

SkyOS AI should not:

- impersonate legal, medical, tax, financial, or safety advice
- silently execute owner or commerce actions without the expected confirmation path
- invent policy, shipping, or account rules that the product does not define
- encourage users to paste secrets into prompts

Use [legal/AI_USAGE_NOTICE.md](legal/AI_USAGE_NOTICE.md) and [faq.md](faq.md) as the trust-facing companions to this document.
