# Changelog

## 1.0.0 - 2026-04-24

- Established SkyOS as the version 1 product identity across project metadata, Android versioning, iOS display metadata, and backend package metadata.
- Added a clean functions build script (`npm run build --prefix functions`) for server-side syntax validation.
- Kept existing Firebase callable names and mobile package identifiers for compatibility with the configured Firebase project.
- Added SkyOS-prefixed release signing environment variables while preserving legacy `SKYDOWN_UPLOAD_*` support for existing local setups.
- Stabilized Android lint for V1 by fixing code-level lint errors and treating the existing partial localization backlog as warnings.
- Documented V1 build commands and environment expectations, including release signing and backend secrets.
