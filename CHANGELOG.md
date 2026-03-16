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

### Changed
- Fixed jpackage configuration (correct Quarkus main JAR and classloader entry point)
- Renamed native package from "UK Self-Employment Manager" to "SelfEmploy" for cross-platform compatibility
- Separated website into dedicated repo ([selfemploy-website](https://github.com/olehsvyrydov/selfemploy-website))
- Migrated docs to Confluence (internal) and GitHub Wiki (client-facing); removed `docs/` directory
- Updated README links to point to GitHub Wiki
- Updated issue/PR templates to reference public roadmap

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

[Unreleased]: https://github.com/olehsvyrydov/selfemploy-uk/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/olehsvyrydov/selfemploy-uk/releases/tag/v0.1.0
