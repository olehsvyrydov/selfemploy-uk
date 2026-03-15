# Release Process

## Overview

Releases are automated via GitHub Actions. Pushing a tag matching `v*` triggers the release workflow, which builds native installers for all platforms and creates a GitHub Release with the artifacts attached.

## Creating a Release

### 1. Prepare the release

```bash
# Ensure you're on main with latest changes
git checkout main
git pull

# Update version from SNAPSHOT to release
mvn versions:set -DnewVersion=0.1.0
mvn versions:commit

# Update CHANGELOG.md: move [Unreleased] items to [0.1.0] with today's date

# Commit the version change
git add pom.xml */pom.xml CHANGELOG.md
git commit -m "Release v0.1.0"
```

### 2. Tag and push

```bash
git tag v0.1.0
git push origin main
git push origin v0.1.0
```

This triggers the release workflow which:
1. Builds the application on all platforms
2. Runs `jpackage` to create native installers (.msi, .dmg, .deb, .rpm)
3. Generates SHA-256 checksums
4. Creates a GitHub Release with all artifacts attached

### 3. Post-release

```bash
# Bump to next development version
mvn versions:set -DnewVersion=0.2.0-SNAPSHOT
mvn versions:commit

git add pom.xml */pom.xml
git commit -m "Bump version to 0.2.0-SNAPSHOT"
git push origin main
```

## Version Scheme

- **Release versions**: `MAJOR.MINOR.PATCH` (e.g., `0.1.0`)
- **Development versions**: `MAJOR.MINOR.PATCH-SNAPSHOT` (e.g., `0.2.0-SNAPSHOT`)
- **Git tags**: `vMAJOR.MINOR.PATCH` (e.g., `v0.1.0`)

jpackage requires clean version numbers without suffixes. The `build-helper-maven-plugin` strips `-SNAPSHOT` automatically for local builds. The CI workflow extracts the version from the git tag.

## What Gets Built

| Platform | Runner | Installer | Notes |
|----------|--------|-----------|-------|
| Windows | windows-latest | `.msi` | WiX Toolset included on runner |
| macOS | macos-latest | `.dmg` | ARM64 (Apple Silicon) |
| Linux | ubuntu-latest | `.deb` | Debian/Ubuntu |
| Linux | ubuntu-latest | `.rpm` | Fedora/RHEL (needs `rpm` tools) |

## Unsigned Installers

Current installers are unsigned. Users will see OS warnings:
- **Windows**: SmartScreen warning — click "More info" then "Run anyway"
- **macOS**: Gatekeeper warning — right-click the app and select "Open"

Code signing is planned for a future sprint.
