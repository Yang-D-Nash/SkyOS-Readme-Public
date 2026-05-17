# ViewModel UiState Mutation Guidelines

Purpose: keep ViewModels predictable, testable, and easy to refactor by using one consistent pattern for `UiState` changes.

## Core Rules

1. Keep `UiState` updates inside small named helpers when logic repeats.
2. One helper = one intent (for example: loading, success toast, error toast, submit failure).
3. Prefer immutable full-state updates with `_uiState.update { state -> state.copy(...) }`.
4. Keep domain logic in use cases/services; ViewModel should orchestrate and map results to UI state.
5. Reset transient messages (`errorMessage`, `successMessage`, `toastMessage`) explicitly when user input changes.
6. Use one shared error-message resolver per flow (avoid duplicating fallback chains).

## Suggested Naming

- `setXInProgress(...)` for loading/submitting flags
- `postSuccessX(...)` and `postErrorX(...)` for user-visible feedback
- `finalizeXState(...)` for success/failure state completion after async calls
- `clearTransientMessages(...)` for clearing temporary UI feedback
- `resolveXErrorMessage(...)` for fallback message selection

## Anti-Patterns to Avoid

- Repeating the same `copy(...)` block in multiple methods
- Updating unrelated state fields in one mutation
- Mixing business validation rules directly in UI event handlers when a use case exists
- Implicitly relying on previous error/toast values instead of setting them intentionally

## Minimal Example

```kotlin
private fun setSyncInProgress(isCatalogLoading: Boolean, loadingToast: String? = null) {
    _uiState.update {
        it.copy(
            isSyncingCatalog = true,
            isCatalogLoading = isCatalogLoading,
            toastMessage = loadingToast ?: it.toastMessage,
            isErrorToast = false,
        )
    }
}

private fun resolveSyncErrorMessage(error: Throwable?): String {
    return error?.message ?: "Sync failed"
}
```

## PR Hygiene Checklist (ViewModels)

- [ ] Duplicate state-update blocks extracted to helper(s)
- [ ] Async flow has explicit start/fail/success/finalize states
- [ ] Message fields are intentionally cleared/set
- [ ] Compile check passed (`:androidApp:compileDebugKotlin`)
- [ ] Relevant tests or smoke checks executed
