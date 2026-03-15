# Security Policy

This document outlines security practices for the UK Self-Employment Manager project, with particular focus on HMRC API credential handling.

## Table of Contents

- [Security Overview](#security-overview)
- [HMRC Credential Handling](#hmrc-credential-handling)
- [Reporting Vulnerabilities](#reporting-vulnerabilities)
- [Data Security](#data-security)
- [Supported Versions](#supported-versions)

---

## Security Overview

The UK Self-Employment Manager is designed with a **privacy-first** approach:

- **Local-only storage**: All financial data remains on your computer
- **Encrypted database**: H2 database with AES-256 encryption
- **No telemetry**: No data is sent to third-party servers
- **Open source**: Full transparency of code handling your data

---

## HMRC Credential Handling

### CRITICAL: Never Commit Credentials

**WARNING**: HMRC API credentials (Client ID and Client Secret) must **NEVER** be committed to version control.

Committing credentials to Git:
- Exposes your HMRC Developer Hub application
- Could allow unauthorized access to taxpayer data
- Violates HMRC API terms of service
- May result in your application being revoked

### How to Register Your Own HMRC Application

Since this is open-source software, you must register your own HMRC application:

1. **Create an HMRC Developer Hub account**
   - Visit: https://developer.service.hmrc.gov.uk/developer/registration
   - Sign up with your Government Gateway credentials

2. **Create a new application**
   - Go to: https://developer.service.hmrc.gov.uk/developer/applications
   - Click "Add an application"
   - Select "Sandbox" for testing (recommended first)
   - Name your application (e.g., "My Self-Employment Manager")

3. **Subscribe to APIs**
   - Subscribe to these APIs for full functionality:
     - Self Assessment (MTD)
     - Self Assessment Accounts (MTD)
     - Individual Calculations (MTD)
     - Obligations (MTD)

4. **Get your credentials**
   - After creating the application, you'll receive:
     - **Client ID**: Your unique application identifier
     - **Client Secret**: Your secret key (shown once - save it securely!)

5. **Configure the application**
   - Copy `.env.example` to `.env` in the project root
   - Add your credentials to the `.env` file
   - **NEVER commit the `.env` file**

### Sandbox vs Production

| Environment | Purpose | URL |
|-------------|---------|-----|
| **Sandbox** | Development/testing | https://test-api.service.hmrc.gov.uk |
| **Production** | Live tax submissions | https://api.service.hmrc.gov.uk |

**Important**: Start with Sandbox credentials. Production access requires HMRC approval.

### Production Application Approval

To use this application with real HMRC data:

1. Complete sandbox testing
2. Apply for production credentials on Developer Hub
3. HMRC reviews your application (can take several weeks)
4. Once approved, update your `.env` with production values

### Environment Variable Configuration

Set these environment variables (see `.env.example`):

```bash
# Required for HMRC API access
HMRC_CLIENT_ID=your_client_id_here
HMRC_CLIENT_SECRET=your_client_secret_here

# Optional: Switch to production (default is sandbox)
HMRC_API_BASE_URL=https://api.service.hmrc.gov.uk
HMRC_AUTHORIZE_URL=https://www.tax.service.gov.uk/oauth/authorize
HMRC_TOKEN_URL=https://api.service.hmrc.gov.uk/oauth/token
```

### OAuth Token Storage

OAuth tokens (after user authorization) are:
- Stored in an encrypted local file
- Never logged or transmitted externally
- Automatically refreshed when expired
- Cleared when user disconnects

---

## Reporting Vulnerabilities

### Private Disclosure

If you discover a security vulnerability, please **DO NOT** open a public issue.

Instead, report privately:

1. **Email**: [Create a security advisory](https://github.com/olehsvyrydov/selfemploy-uk/security/advisories/new) on GitHub
2. **Include**:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Any suggested fixes

### Response Timeline

- **Acknowledgment**: Within 48 hours
- **Initial assessment**: Within 7 days
- **Fix timeline**: Depends on severity (critical: ASAP, high: 2 weeks, medium: 30 days)

### What We Consider Security Issues

- Credential exposure
- Authentication bypass
- Data encryption weaknesses
- HMRC API misuse potential
- SQL injection vulnerabilities
- Cross-site scripting (XSS) in local UI
- Insecure data storage

---

## Data Security

### Local Database Encryption

The H2 database uses AES-256 encryption:

```properties
jdbc:h2:file:${user.home}/.selfemploy/data;CIPHER=AES
```

### What Is Stored Locally

- Income and expense records
- Tax calculations
- HMRC submission history
- OAuth tokens (encrypted)
- User preferences

### What Is NOT Stored

- HMRC Client ID/Secret (environment variables only)
- Raw passwords (OAuth flow only)
- Government Gateway credentials

### Secure Development Practices

Contributors should:

1. **Never hardcode secrets** in source code
2. **Never log sensitive data** (tokens, credentials)
3. **Use parameterized queries** for database access
4. **Validate all user input** before processing
5. **Keep dependencies updated** for security patches

---

## Supported Versions

| Version | Supported |
|---------|-----------|
| 1.x.x   | Yes       |
| 0.x.x   | Development only |

Security updates are backported to the latest stable release.

---

## Plugin Security

The plugin system implements multiple layers of security to protect users from malicious or poorly-written plugins.

### Permission System

Plugins must declare their required permissions, which are enforced at runtime:

| Permission | Sensitivity | Description |
|------------|-------------|-------------|
| `UI_EXTENSION` | LOW | Add navigation items and widgets |
| `DATA_READ` | LOW | Read transaction data |
| `SETTINGS_READ` | LOW | Read user settings |
| `CLIPBOARD_ACCESS` | LOW | Access system clipboard |
| `FILE_ACCESS` | MEDIUM | Access file system beyond plugin directory |
| `NETWORK_ACCESS` | MEDIUM | Make network connections |
| `DATA_WRITE` | MEDIUM | Create/modify transactions |
| `SETTINGS_WRITE` | MEDIUM | Modify user settings |
| `HMRC_API` | HIGH | Interact with HMRC APIs |
| `SYSTEM_EXEC` | HIGH | Execute system commands |

### Sensitivity Levels

| Level | User Experience | Requirements |
|-------|-----------------|--------------|
| **LOW** | Auto-granted on install | No special requirements |
| **MEDIUM** | User prompted on first use | User approval required |
| **HIGH** | Explicit approval dialog | JAR signing required |

### JAR Signing Requirements

Plugins requesting HIGH sensitivity permissions (`HMRC_API`, `SYSTEM_EXEC`) must be signed by a trusted publisher:

1. **Self-signed certificates** - Acceptable for development/testing
2. **CA-signed certificates** - Required for Plugin Marketplace submission

The application verifies signatures at plugin load time:
- Unsigned plugins requesting HIGH permissions are rejected
- Invalid signatures cause plugin rejection
- Users are prompted before trusting new publishers

### Plugin Isolation

Each plugin runs in a secure sandbox:

- **ClassLoader isolation**: Plugins cannot access internal application classes
- **Data isolation**: Each plugin has its own encrypted data directory
- **No direct database access**: All data operations go through controlled APIs
- **Permission enforcement**: Runtime checks prevent unauthorized operations

### User Security Controls

Users can manage plugin security through Settings:

- View installed plugins and their permissions
- Revoke permissions for any plugin
- Disable or uninstall plugins
- View trusted publishers list
- Report suspicious plugins

### Reporting Plugin Security Issues

If you discover a security vulnerability in a plugin:

1. Do NOT install or continue using the plugin
2. Report to the plugin author
3. If unresponsive, email security@selfemploy.uk
4. Include plugin name, version, and vulnerability details

For detailed security documentation, see [docs/plugin-development/security.md](docs/plugin-development/security.md).

---

## Third-Party Dependencies

We use CodeQL and Dependabot to monitor for vulnerabilities in dependencies:

- Automated security scanning on every PR
- Weekly dependency vulnerability checks
- Critical vulnerabilities addressed within 48 hours

---

## Additional Resources

- [HMRC Developer Hub](https://developer.service.hmrc.gov.uk/)
- [HMRC Fraud Prevention Headers](https://developer.service.hmrc.gov.uk/guides/fraud-prevention/)
- [OAuth 2.0 Security Best Practices](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-security-topics)

---

## Questions?

For security-related questions that are not vulnerabilities, open a [Discussion](https://github.com/olehsvyrydov/selfemploy-uk/discussions) with the "security" tag.
