# Getting Started

This guide will help you install and run the UK Self-Employment Manager.

## Installation Options

### Option 1: Download Installer (Recommended)

Download the latest installer for your platform from [GitHub Releases](https://github.com/olehsvyrydov/selfemploy-uk/releases/latest). **No Java installation required** — the JVM is bundled with the installer.

| Platform | File | Install |
|----------|------|---------|
| Windows | `SelfEmploy-x.x.x.msi` | Double-click to install |
| macOS | `SelfEmploy-x.x.x.dmg` | Drag to Applications |
| Linux (Debian/Ubuntu) | `SelfEmploy-x.x.x.deb` | `sudo dpkg -i SelfEmploy-*.deb` |
| Linux (Fedora/RHEL) | `SelfEmploy-x.x.x.rpm` | `sudo rpm -i SelfEmploy-*.rpm` |

#### One-Line Install

```bash
# Linux / macOS
./install.sh --install

# Windows (PowerShell)
.\install.ps1 -Install
```

### Option 2: Build from Source (Developers)

**Prerequisites:**
- **Java 21** or later (JDK, not JRE) — [Adoptium](https://adoptium.net/) recommended
- **Maven 3.6+** (for building from source)
- **Git** (for cloning the repository)

```bash
# Clone the repository
git clone https://github.com/olehsvyrydov/selfemploy-uk.git
cd selfemploy-uk

# Build and run
./install.sh              # Linux/macOS
.\install.ps1             # Windows

# Or manually:
mvn clean install
mvn -pl app javafx:run
```

---

## First Run

1. **Launch the application**
2. **Accept the Terms of Service** (one-time)
3. **Create your business profile**:
   - Business name
   - UTR (Unique Taxpayer Reference) - optional initially
   - Accounting period start date

---

## Project Structure

```
selfemploy-uk/
├── common/          # Domain entities, DTOs, enums
├── persistence/     # Database layer
├── hmrc-api/        # HMRC MTD API integration
├── core/            # Business logic, tax calculator
├── ui/              # JavaFX user interface
└── app/             # Application launcher
```

---

## Configuration

Configuration is stored in:

| Platform | Location |
|----------|----------|
| Windows | `%APPDATA%\selfemploy-uk\` |
| macOS | `~/Library/Application Support/selfemploy-uk/` |
| Linux | `~/.config/selfemploy-uk/` |

### Database Location

Your encrypted database is stored at:
```
{config-dir}/data/tax.mv.db
```

---

## Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| New Income | `Ctrl+I` |
| New Expense | `Ctrl+E` |
| Dashboard | `Ctrl+D` |
| Settings | `Ctrl+,` |
| Backup Data | `Ctrl+B` |

---

## Troubleshooting

### Application won't start

1. Check Java version: `java -version`
2. Ensure Java 21+ is installed
3. Check for conflicting processes

### Database errors

1. Check disk space
2. Verify file permissions in config directory
3. Try restoring from backup

### HMRC connection issues

1. Check internet connection
2. Verify HMRC Gateway credentials
3. Check HMRC API status at [HMRC Developer Hub](https://developer.service.hmrc.gov.uk/)

---

## Next Steps

- [Add your first income](Income-Tracking)
- [Record expenses](Expense-Tracking)
- [View tax estimates](Tax-Calculator)
- [Connect to HMRC](HMRC-Integration)
