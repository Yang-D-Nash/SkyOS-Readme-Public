# AI Membership Funnel Metrics

This dashboard spec uses Firebase Analytics event export in BigQuery.

## Event Contract

Required events:

- `membership_open`
- `membership_reason`
- `plan_selected`
- `annual_toggle_changed`
- `purchase_started`
- `purchase_success`
- `purchase_cancelled`
- `restore_success`
- `upgrade_after_warning`
- `upgrade_after_deny`

Required params:

- `platform`
- `reason`
- `plan`
- `annual`
- `surface`
- `currentPlan`

## Core KPI Definitions

- `open_to_purchase_cvr` = `purchase_success / membership_open`
- `cvr_by_reason` = `purchase_success(reason=x) / membership_open(reason=x)`
- `annual_share` = `purchase_success(annual=true) / purchase_success`
- `creator_take_rate` = `purchase_success(plan=creator) / purchase_success`
- `cancel_rate` = `purchase_cancelled / purchase_started`
- `restore_rate` = `restore_success / membership_open`
- `time_to_purchase_seconds` = median seconds between `membership_open` and `purchase_success` for same user/session window

## BigQuery Starter Query (Open -> Purchase CVR)

```sql
WITH base AS (
  SELECT
    user_pseudo_id,
    event_name,
    event_timestamp,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'reason') AS reason,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'plan') AS plan,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'annual') AS annual,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'surface') AS surface,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'currentPlan') AS currentPlan
  FROM `YOUR_PROJECT.analytics_XXXXXXXX.events_*`
  WHERE event_name IN ('membership_open', 'purchase_success')
)
SELECT
  COUNTIF(event_name = 'membership_open') AS opens,
  COUNTIF(event_name = 'purchase_success') AS purchases,
  SAFE_DIVIDE(COUNTIF(event_name = 'purchase_success'), COUNTIF(event_name = 'membership_open')) AS open_to_purchase_cvr
FROM base;
```
