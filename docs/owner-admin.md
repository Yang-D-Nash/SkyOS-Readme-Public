# SkyOS Owner and Admin

SkyOS includes powerful owner and admin surfaces. They exist to operate a live product, not to
act as a hidden debug panel. This document defines what those surfaces are for and how to use them safely.

## 1. Roles

SkyOS distinguishes between:

- `user`
- `subadmin`
- `admin`
- `owner`

The owner role is special because it can influence revenue, runtime safety, legal content, role
assignment, and operational recovery. It should be treated as production authority.

## 2. What Owner/Admin Controls Cover

Owner/admin surfaces in the repo include:

- AI prompt and runtime settings
- FAQ review and intelligence loops
- user role and quota management
- membership and revenue operations
- legal content settings
- Shopify and payment configuration
- runtime lockdown and budget response
- upload and system write control

These are high-trust controls. They affect real users, cost, and legal posture.

## 3. AI Runtime Controls

Owner runtime controls can influence:

- primary and fallback provider choice
- quotas and history retention
- external automation policy
- confirmation requirements
- kill switches
- prompt and brand instructions

Best practice:

1. start conservative
2. observe real usage
3. widen capability only after cost, support, and trust impact are understood

## 4. Agent and External Workflow Setup

SkyOS can bridge into external systems. Owner/admin should understand the boundary between:

- platform-owned runtime settings
- user-owned automation configuration
- optional Manus BYOS configuration

Safe operating rules:

- keep external workflows scoped per account
- do not reuse one user's configuration for another user
- do not enable automation paths without clear support and privacy language
- require confirmation for sensitive categories such as commerce or owner actions

## 5. Membership and Revenue Ops

The repo includes a membership command and revenue operations layer with:

- dashboards
- time series
- recommendations
- experiment start and completion
- recommendation rejection
- learnings
- lifecycle timeline
- hygiene controls

This is a decision surface, not automatic truth. Owner should use it to review signals, not to
blindly optimize conversion at the expense of trust.

## 6. Kill Switches and Incident Response

Critical live controls include:

- runtime lockdown
- registration disable
- upload disable
- user-write disable
- budget lockdown
- agent and AI kill switches

Use these when:

- abuse risk spikes
- cost risk spikes
- a payment or role bug escapes
- legal or data handling issues require a temporary halt

The product should degrade clearly, not silently.

## 7. Safe Operations Checklist

Before changing live owner settings:

1. confirm the user impact
2. confirm whether support needs context
3. confirm whether legal or billing copy is affected
4. confirm whether analytics and release notes need an update
5. make the smallest reversible change possible

Never use owner power as a shortcut around missing process.

## 8. Release Controls

Owner release approval should require:

- green client builds
- green Functions and rules tests
- real-device smoke on current release candidate
- legal text status known
- billing and restore verified on intended live rails
- rollback and support paths known

The canonical launch gate lives in [release-checklist.md](release-checklist.md).
