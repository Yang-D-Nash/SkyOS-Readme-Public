fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android validate_android_internal

```sh
[bundle exec] fastlane android validate_android_internal
```

Validate SkyOS Android AAB upload for Google Play internal testing

### android upload_android_internal

```sh
[bundle exec] fastlane android upload_android_internal
```

Upload SkyOS Android AAB to Google Play internal testing

### android validate_android_production

```sh
[bundle exec] fastlane android validate_android_production
```

Validate SkyOS Android AAB for Google Play **production** (no upload).

Overrides (optional): ANDROID_PLAY_TRACK (default production), ANDROID_PLAY_RELEASE_STATUS (default draft).
For phased release after rollout: ANDROID_PLAY_RELEASE_STATUS=completed ANDROID_PLAY_ROLLOUT=0.1


### android upload_android_production

```sh
[bundle exec] fastlane android upload_android_production
```

Upload SkyOS Android AAB to Google Play **production** track.

Default release_status is **draft**: review/publish in Play Console as usual — safer soft launch.

Env: SUPPLY_JSON_KEY (required), optional ANDROID_PLAY_TRACK, ANDROID_PLAY_RELEASE_STATUS, ANDROID_PLAY_ROLLOUT.


----


## iOS

### ios precheck_ios_ipa

```sh
[bundle exec] fastlane ios precheck_ios_ipa
```

Only checks that IOS_IPA_PATH exists (no App Store Connect access).


### ios upload_ios_app_store_connect

```sh
[bundle exec] fastlane ios upload_ios_app_store_connect
```

Upload an App Store IPA to App Store Connect (metadata/screenshots unchanged here).

Required env:
- IOS_IPA_PATH (relative to repo root or absolute path to .ipa)
- ASC_KEY_ID, ASC_ISSUER_ID, ASC_KEY_PATH (path to AuthKey_XXXXXXXX.p8)

Uses API key upload only; submit_for_review and automatic_release are **false**.
Listing + screenshots still happen in App Store Connect.


### ios precheck_ios_asc_credentials

```sh
[bundle exec] fastlane ios precheck_ios_asc_credentials
```



----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
