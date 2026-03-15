# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Native installers for Windows (.msi), macOS (.dmg), and Linux (.deb/.rpm) — no Java required
- GitHub Actions release workflow for automated installer builds
- Interactive app walkthrough on the website with 5-tab demo tour
- Smart OS detection on website highlighting the recommended download
- Dynamic download links fetched from GitHub Releases API
- Installation guide page with per-OS instructions
- `--install` mode for install.sh and `-Install` for install.ps1 (downloads pre-built installer)
- Visual "How It Works" section with mini-mockup screenshots

### Changed
- Redesigned website download section with OS auto-detection
- Updated "How It Works" from 3 text-only steps to 4 visual step cards
- Fixed jpackage configuration (correct Quarkus main JAR and classloader entry point)
- Fixed GitHub repository URLs across the website (selfemploy-uk/self-employment -> olehsvyrydov/selfemploy-uk)
- Renamed native package from "UK Self-Employment Manager" to "SelfEmploy" for cross-platform compatibility

### Fixed
- jpackage `mainJar` now correctly references `quarkus-run.jar` instead of non-existent artifact JAR
- jpackage `mainClass` now uses `QuarkusEntryPoint` for proper classloader bootstrapping
- jpackage `appVersion` strips `-SNAPSHOT` suffix for version compliance

## [0.1.0] - Unreleased

Initial release.

### Added
- Income and expense tracking with HMRC SA103 categories
- Real-time tax calculations (Income Tax, NI Class 2, NI Class 4)
- Bank statement import (CSV parser with SPI for custom formats)
- HMRC MTD API integration for Self Assessment submission
- Local H2 database with AES-256 encryption
- JavaFX desktop UI with responsive layouts
- Plugin system with 9 extension points
- Cross-platform install scripts (bash + PowerShell)
- Landing page website with download cards
- Comprehensive Playwright E2E test suite
- Privacy policy, terms of service, and legal disclaimers

[Unreleased]: https://github.com/olehsvyrydov/selfemploy-uk/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/olehsvyrydov/selfemploy-uk/releases/tag/v0.1.0
