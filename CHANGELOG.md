# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Native installers for Windows (.msi), macOS (.dmg), and Linux (.deb/.rpm/.AppImage) — no Java required
- GitHub Actions release workflow for automated installer builds
- `--install` mode for install.sh and `-Install` for install.ps1 (downloads pre-built installer)
- Linux AppImage support in release workflow and install script
- `packaging/SelfEmploy.desktop` for AppImage metadata
- `SHA256SUMS` generated for every release; `install.sh --install` and `install.ps1 -Install` verify
  the downloaded asset against it and refuse to install on a missing or mismatched checksum
- `RELEASE_CHECKLIST.md` and a `RELEASE_NOTES.md` template for cutting the first release
- `scripts/content-lint.sh` blocked-phrase check over the README and the wiki, backed by
  `scripts/content-lint-claims-blocklist.txt`, so a withdrawn claim cannot reappear in either

### Changed
- Fixed jpackage configuration (correct Quarkus main JAR and classloader entry point)
- Renamed native package from "UK Self-Employment Manager" to "SelfEmploy" for cross-platform compatibility
- Separated the website into a dedicated repository
- Migrated docs to Confluence (internal) and GitHub Wiki (client-facing); removed `docs/` directory
- Updated README links to point to GitHub Wiki
- Updated issue/PR templates to reference public roadmap
- Rewrote README and wiki claims to match verified behaviour: the annual submission flow is a
  guided multi-step wizard (not one-click), income/expense records have an optional invoice-number
  reference field (the app does not create or manage invoices), NI estimates cover Class 2 and
  Class 4, and every HMRC submission targets the sandbox API only. Added a "who can file with this"
  scope statement and removed the dead `selfemploy.uk` site references

### Fixed
- jpackage `mainJar` now correctly references `quarkus-run.jar` instead of non-existent artifact JAR
- jpackage `mainClass` now uses `QuarkusEntryPoint` for proper classloader bootstrapping
- jpackage `appVersion` strips `-SNAPSHOT` suffix for version compliance
- Removed a dead site link and a stale issue-tracker link from the project metadata and a test's
  Javadoc

## [0.1.0] - Unreleased

Initial release.

### Added
- Income and expense tracking with HMRC SA103 categories
- Real-time tax calculations (Income Tax, NI Class 2, NI Class 4)
- Bank statement import (CSV parser with SPI for custom formats)
- HMRC MTD API integration for Self Assessment submission
- Local SQLite database; HMRC credentials and National Insurance number encrypted (AES-256-GCM)
- JavaFX desktop UI with responsive layouts
- Plugin system with 9 extension points
- Cross-platform install scripts (bash + PowerShell)

[Unreleased]: https://github.com/olehsvyrydov/selfemploy-uk/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/olehsvyrydov/selfemploy-uk/releases/tag/v0.1.0
