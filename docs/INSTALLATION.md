# Installation Guide

## Download Pre-Built Installer (Recommended)

Download the latest installer for your platform from [GitHub Releases](https://github.com/olehsvyrydov/selfemploy-uk/releases/latest). No Java installation required — the JVM is bundled with the installer.

| Platform | File | Install Command |
|----------|------|-----------------|
| Windows | `.msi` | Double-click to install |
| macOS | `.dmg` | Drag to Applications |
| Linux (Debian/Ubuntu) | `.deb` | `sudo dpkg -i SelfEmploy-*.deb` |
| Linux (Fedora/RHEL) | `.rpm` | `sudo rpm -i SelfEmploy-*.rpm` |

### One-Line Install

```bash
# Linux / macOS
./install.sh --install

# Windows (PowerShell)
.\install.ps1 -Install
```

## Platform Notes

### Windows
If Windows SmartScreen shows a warning, click **"More info"** then **"Run anyway"**. The installer is not yet code-signed.

### macOS
If macOS Gatekeeper blocks the app, right-click the app and select **"Open"**, then click **"Open"** in the dialog. You only need to do this once.

### Linux
If `dpkg` reports missing dependencies: `sudo apt-get install -f`

## Build from Source (Developers)

### Prerequisites
- Java 21+ (JDK)
- Maven 3.6+

### Build & Run

```bash
git clone https://github.com/olehsvyrydov/selfemploy-uk.git
cd selfemploy-uk
./install.sh          # or: mvn clean install && mvn -pl app javafx:run
```

### Create Native Installer

```bash
./install.sh --package    # or: mvn -pl app -Ppackage package jpackage:jpackage
```

## Uninstall

| Platform | Method |
|----------|--------|
| Windows | Settings > Apps > SelfEmploy > Uninstall |
| macOS | Drag SelfEmploy from Applications to Trash |
| Linux (deb) | `sudo dpkg -r selfemploy` |
| Linux (rpm) | `sudo rpm -e selfemploy` |
