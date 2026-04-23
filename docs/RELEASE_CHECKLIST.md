# SkyOS Release Checklist

Use this checklist for every release candidate. A release is not ready because the UI looks good; it is ready when the product, backend, legal, data, and support paths can be trusted.

## 1. Build And Tooling

- [ ] iOS Debug build passes.
- [ ] Android Debug build passes.
- [ ] Functions tests pass.
- [ ] Firestore/Storage rules tests pass.
- [ ] Localization audit has been run.
- [ ] Dependency audit has been reviewed.
- [ ] No unexpected generated files are staged.
- [ ] No secrets or private exports are staged.

Commands:

```bash
xcodebuild -project "Skydown App.xcodeproj" -scheme "Skydown App" -configuration Debug -destination "generic/platform=iOS Simulator" build
./gradlew :androidApp:assembleDebug
npm test --prefix functions
./scripts/localization_audit.sh
npm audit --prefix functions --omit=dev
```

## 2. Core Smoke Test

- [ ] App starts.
- [ ] Login works.
- [ ] Logout works.
- [ ] Session restore works.
- [ ] Main tab navigation works.
- [ ] Sheets and dialogs open and close.
- [ ] Background/foreground does not break state.
- [ ] Offline state is understandable.
- [ ] Retry state is visible where needed.
- [ ] Empty states are intentional.
- [ ] Error states do not expose raw internals.

## 3. CRUD And Backend

- [ ] Profile create/read/update paths work.
- [ ] Settings reads and writes allowed user data only.
- [ ] AI/Agent usage reads current plan/usage.
- [ ] Membership status refresh works.
- [ ] Orders create through Functions.
- [ ] Users read only own orders.
- [ ] Owner can manage global orders.
- [ ] Admin/owner paths are hidden from normal users.
- [ ] Firestore rules match UI expectations.
- [ ] Storage upload/delete rules match UI expectations.

## 4. Membership And Billing

- [ ] Membership opens from Settings.
- [ ] Membership opens from AI/Agent hints.
- [ ] Upgrade flow opens native store path.
- [ ] Restore path is visible.
- [ ] Restore refreshes status.
- [ ] Back/cancel does not leave stuck loading.
- [ ] Plan label matches entitlement state.
- [ ] Limit warnings are calm and accurate.
- [ ] Support path is visible for billing issues.

## 5. AI And Agent

- [ ] AI opens.
- [ ] Agent opens.
- [ ] Prompt send state is visible.
- [ ] Successful response renders.
- [ ] Error state is readable.
- [ ] Retry does not duplicate stale warnings.
- [ ] Limit/membership hints are understandable.
- [ ] Result cards do not break layout.
- [ ] Progress states are not abrupt.

## 6. Home, Profile, Settings

- [ ] Home signals load.
- [ ] Utility actions open correct destinations.
- [ ] Profile opens.
- [ ] Profile dashboard state is current.
- [ ] Settings opens.
- [ ] Account actions are clear.
- [ ] Legal Center opens.
- [ ] Support route is visible.
- [ ] No broken localization keys are visible.

## 7. Music, Video, Shop, Orders

- [ ] Music opens.
- [ ] Video opens.
- [ ] Player/preview surfaces open or fail safely.
- [ ] Shop opens.
- [ ] Product detail opens.
- [ ] Cart updates item quantity and remove actions.
- [ ] Checkout/order submission path is tested.
- [ ] Orders reload after submission.
- [ ] Normal users do not see owner management actions.
- [ ] Owner can confirm payment, toggle completion, and delete when appropriate.

## 8. Owner/Admin

- [ ] Membership Command Center opens for Owner.
- [ ] KPI cards render.
- [ ] Recommendations load.
- [ ] Experiment start/reject/complete flows work.
- [ ] Learnings and timeline are visible.
- [ ] Hygiene controls do not affect unrelated data.
- [ ] Admin user management is role-gated.
- [ ] Normal users cannot access owner/admin callables.

## 9. Legal And Trust

- [ ] Terms are visible.
- [ ] Privacy Policy is visible.
- [ ] Subscription Terms are visible.
- [ ] AI Usage Notice is visible.
- [ ] Impressum/Company Info is visible.
- [ ] Support is visible.
- [ ] Delete account path is reviewed.
- [ ] Billing restore copy is reviewed.
- [ ] Final legal review is documented.

## 10. Live Data Hygiene

- [ ] No live cleanup is performed without approval.
- [ ] Test users are clearly prefixed.
- [ ] Test docs are clearly prefixed.
- [ ] Cleanup dry run is reviewed.
- [ ] Backup/export exists when useful.
- [ ] Targeted deletion is verified.
- [ ] Remaining test data is documented.

Allowed test prefixes:

- `qa_`
- `test_`
- `releasecheck_`
- `deviceqa_`

## 11. Store Readiness

- [ ] App Store / Play Store metadata matches product behavior.
- [ ] Subscription descriptions match in-app wording.
- [ ] Privacy labels/data safety answers match actual data use.
- [ ] Screenshots are current.
- [ ] Support URL/contact is valid.
- [ ] Legal URLs are valid.
- [ ] Crash monitoring is active.
- [ ] Rollback or hotfix path is known.

## 12. Go / No-Go

Release can proceed only when:

- no P0 remains
- P1 items are fixed or explicitly accepted by Owner
- legal review status is known
- billing/store smoke is complete
- live data baseline is clean enough for launch
- support knows the release state
- final commit/tag baseline is documented
