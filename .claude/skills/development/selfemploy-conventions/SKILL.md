---
name: selfemploy-conventions
description: "Repo conventions for the selfemploy-uk desktop app (Java 25, Maven multi-module, Quarkus 3.37 + JavaFX 25, SQLite via sqlite-jdbc-crypt). Load for ANY engineering work in this repository — coding, reviewing, testing, building, or CI changes. Carries the module map, build/test/profile conventions, storage rules (desktop SQLite vs server-profile H2/Flyway), and the HMRC-integration and encryption rules."
---

# selfemploy-uk — repository conventions

UK Self-Employment Manager: a privacy-first desktop app for UK self-employers to
manage accounting and submit HMRC Making Tax Digital reports. Java 25, Maven
multi-module, parent `uk.selfemploy:self-employment-parent`.

## 1. Module map

Desktop-default modules (built by every plain `mvn` invocation, in reactor order):

| Module | Role | Key dependency facts |
|---|---|---|
| `scss-compiler` | Build-time tool: compiles SCSS to JavaFX CSS via Dart Sass embedded (from Maven Central). Not shipped at runtime. | No project deps; consumed `provided`-scope by `ui`. |
| `common` | Shared domain entities, DTOs, enums. | Depends only on the parent. Guarded by ArchUnit (see §5). |
| `hmrc-api` | HMRC Making Tax Digital API integration, incl. `uk.selfemploy.hmrc.fraud.FraudPreventionService` and OAuth token storage. | Depends on `common`. Plain library — no Quarkus runtime deps. |
| `core` | Business logic, tax calculator, services. | Depends on `common`, `hmrc-api`, `plugin-api`, `plugin-runtime`. Must stay UI- and JDBC-free (ArchUnit-enforced). |
| `ui` | JavaFX 25 UI **and** the desktop persistence layer (Sqlite* repositories live here, not in `persistence`). | Depends on `common`, `core`, `hmrc-api`, `plugin-api`; `io.github.willena:sqlite-jdbc` 3.53.2.0 (sqlite-jdbc-crypt); BouncyCastle (Argon2id app-lock KDF); `scss-compiler` at build time only. |
| `app` | Application launcher and native packaging (jpackage input assembly). | Depends on `common`, `core`, `hmrc-api`, `ui`. |
| `plugin-api` | Plugin SDK, published to Maven Central for plugin developers. | Depends only on the parent — keep it dependency-clean. |
| `plugin-runtime` | Plugin loader/manager/runtime. | Depends on `plugin-api`. |

`server`-profile-only modules (NOT built by default — activate with `-Pserver`):

| Module | Role | Key dependency facts |
|---|---|---|
| `persistence` | Database entities, repositories, and Flyway migrations for the server stack. | H2 2.3.232 (pinned for on-disk format compatibility), quarkus-flyway, Panache. Depends on `common`. |
| `server` | Never-run server-side MTD submission and bank-import services (kept for a self-hostable direction). | Quarkus (arc, rest-client + jackson, micrometer/prometheus); depends on `common`, `core`, `hmrc-api`, `persistence`. |

`packaging/` is **not** a Maven module — it is a resources directory
(`icon.svg`, `logging.properties`, `SelfEmploy.desktop`) used by packaging.

## 2. Build & profiles

- **Default build = desktop.** Root `pom.xml` lists only the desktop modules;
  no Panache/H2 stack is compiled unless you ask for it.
- **`-Pserver`** adds `persistence` and `server` to the reactor. CI's
  build-and-test job runs with `-Pserver`, so server-module test breakage
  still fails PRs even though the desktop build ignores those modules.
- **`-Pnative`** (root pom, auto-activated by `-Dnative`) enables the Quarkus
  native build.
- **`-Ppackage`** (defined in `app/pom.xml`) assembles
  `app/target/jpackage-input/` (app jar + libs); the release workflow then runs
  `jpackage` per-OS to produce deb/rpm/AppImage/dmg/msi-style installers.
- **SCSS**: `ui` compiles `ui/src/main/scss/` to JavaFX CSS at
  `generate-resources` via exec-maven-plugin invoking the `scss-compiler`
  module. Edit the `.scss` sources, never the generated CSS.
- Versions come from the parent: Quarkus BOM 3.37.2 (also supplies JUnit and
  Flyway versions), JavaFX 25.0.3, Lombok 1.18.46.
- Compiler runs with `-parameters`; surefire pins locale to `en-GB` via
  `argLine` (assertions on formatted money/dates depend on it — do not remove).

## 3. Storage

Two independent stores; do not mix their code or migrations.

**Desktop (the real product): SQLite in the `ui` module.**
- Driver is `io.github.willena:sqlite-jdbc` (sqlite-jdbc-crypt), a drop-in
  fork of xerial sqlite-jdbc bundling SQLCipher/wxSQLite3 so the database file
  is encrypted at rest via `PRAGMA key`. **Never add `org.xerial:sqlite-jdbc`
  alongside it** — both register `org.sqlite.JDBC` (rule stated in `ui/pom.xml`).
- Repositories are hand-written JDBC classes in
  `ui/src/main/java/uk/selfemploy/ui/service/` (`SqliteIncomeRepository`,
  `SqliteExpenseRepository`, `SqliteBankTransactionRepository`,
  `SqliteSubmissionRepository`, …) around the `SqliteDataStore` singleton,
  with in-memory fakes beside them (`InMemoryExpenseRepository`,
  `InMemoryIncomeService`, …) for tests. No ORM, no Panache here.
- The DB encryption key is provisioned by the app-lock/unlock flow
  (`SqliteDataStore.provisionKey(DbKey)`); the store fails closed without it,
  and `lock()` closes connections and destroys the key.
- **Desktop migrations are NOT Flyway.** `SqliteDataStore.migrations()` holds
  6 ordered migrations applied once each by
  `uk.selfemploy.ui.service.db.SqliteMigrationRunner` (a `schema_version`
  ledger table): 3 SQL scripts in `ui/src/main/resources/db/migration-sqlite/`
  (`V1__baseline.sql`, `V4__import_audit.sql`, `V5__notification_state.sql`)
  plus 3 Java migrations. Migrations must be idempotent
  (`CREATE TABLE IF NOT EXISTS`, add-column-if-missing) because a
  pre-ledger database replays all of them. New schema change = append a new
  version; **never edit an applied migration**.
- `ui/src/main/resources/db/migration/V1__wizard_progress.sql` is a lone
  Flyway-named file no code references (the wizard table is created by
  `SqliteWizardProgressRepository` itself) — treat it as vestigial; do not add
  new files there.

**Server profile: H2 + Quarkus Panache in `persistence`.**
- 21 Flyway migrations in `persistence/src/main/resources/db/migration/`
  (`V1__create_initial_schema.sql` … `V21__add_business_use_pct_to_expenses.sql`),
  applied by quarkus-flyway to the H2 database only. Same rule: applied
  migrations are never edited — add a new `V<next>__*.sql`.
- H2 is version-pinned in the parent to preserve on-disk format compatibility.

## 4. Security & compliance

- **Secrets at rest**: `uk.selfemploy.ui.service.CredentialEncryption` —
  AES-256-GCM (`AES/GCM/NoPadding`, 128-bit tag, 12-byte IV, per-record salt)
  keyed from the installation's `MasterKeyProvider` secret. Used for HMRC API
  credentials and OAuth tokens; a `v2:` prefix marks the current layout and
  legacy values are still readable and re-encrypted on load (`isLegacy`).
- **NI number**: `SqliteWizardProgressRepository` encrypts the NINO column
  (AES-256-GCM) before it touches the database; `WizardProgressRepository`'s
  contract requires it of any implementation.
- **Whole-database encryption**: the app-lock passphrase (Argon2id KDF via
  BouncyCastle, `ui/.../service/security/PassphraseCrypto` and `Vault`)
  yields the sqlite-jdbc-crypt `PRAGMA key`; see also `BackupEncryption` and
  `AppLockService` in `ui/.../service/security/`.
- **Local-only stance**: all user data stays on the device (README:
  "Privacy-First"). DISCLAIMER.md defines the Pre-Submission Confirmation gate:
  an append-only local audit log records a salted SHA-256 of the NINO and a
  SHA-256 of the submitted tuple — **plaintext NINO and raw HMRC payloads are
  never persisted in the audit log**. Preserve both invariants in any change
  near submission or auditing.
- **Fraud-prevention headers**: HMRC's `DESKTOP_APP_DIRECT` spec is
  implemented by `hmrc-api`'s `FraudPreventionService` and contract-locked by
  `FraudPreventionHeadersHmrcContractTest` (tag `hmrc-sandbox`). The
  `nightly-fraud-headers.yml` workflow runs it daily: a deterministic
  shape-lock (every header HMRC lists as mandatory is emitted in the
  documented format, no network needed) plus an opt-in live POST to HMRC's
  `test-fraud-prevention-headers` validator when the
  `HMRC_FPH_VALIDATOR_TOKEN` secret is configured.

## 5. Testing

- Stack: JUnit 5 (version from the Quarkus BOM) + Mockito 5.23 + AssertJ
  3.27.7; TestFX 4.0.18 for JavaFX UI tests (CI runs them under Xvfb with
  `-Dtestfx.headless=true -Dprism.order=sw`).
- **ArchUnit 1.3.0** (declared in `core/pom.xml`): rules live in
  `core/src/test/java/uk/selfemploy/core/architecture/CoreBoundaryTest.java`
  and enforce the ports-and-adapters boundary — `core`/`common` must not
  depend on JavaFX (`javafx..`), raw JDBC (`java.sql..`/`javax.sql..`), or the
  `ui` module (`uk.selfemploy.ui..`). If it fails, put the dependency behind a
  port; do not relax the rule.
- **Tags**: `e2e` (slow UI suites) and `hmrc-sandbox` (depend on HMRC's spec /
  sandbox endpoint) are excluded by default via the parent property
  `surefire.excludedGroups=e2e,hmrc-sandbox`; the nightly workflows opt in
  with `-Dsurefire.excludedGroups= -Dgroups=<tag>`.
- Surefire runs unit tests; failsafe is wired for `integration-test`/`verify`
  (`*IT`-style integration tests) in the parent's pluginManagement.
- In-memory repository fakes (§3) are the standard substitute for SQLite in
  service-level tests; mock only at true boundaries.

## 6. CI (.github/workflows/)

- `ci.yml` — three gates on push/PR: **Content Lint**
  (`scripts/content-lint.sh`: raw `new Alert(` outside AppDialog, FXML
  bracket placeholders, enum tokens in user copy, hardcoded £-rate literals,
  TODO/FIXME in src/main; waivers in `scripts/content-lint-allow.txt`);
  **Build & Test** matrix (Java 25, `-Pserver`, Xvfb, e2e/hmrc-sandbox
  excluded); **Verify Full Build** (compile + package with the Quarkus build).
- `codeql.yml` — CodeQL Java analysis on push to main/develop, PRs to main,
  and a weekly cron.
- `dependency-review.yml` — blocks PRs introducing known-vulnerable
  dependencies.
- `nightly-e2e.yml` — 03:00 UTC: runs only `e2e`-tagged TestFX suites under
  Xvfb + openbox (24-bit depth), `--fail-never` plus a pass-rate ratchet
  script that enforces no regression.
- `nightly-fraud-headers.yml` — 03:00 UTC: the `hmrc-sandbox` fraud-header
  contract check described in §4 (`-pl hmrc-api -am`).
- `release.yml` — on version tags: `mvn package -Ppackage` then per-OS
  jpackage matrix (linux-deb, linux-rpm, linux-appimage, plus macOS/Windows
  jobs) and a GitHub Release from the installers.
- `semantic-version.yml` — on merged PRs to main: derives the suggested next
  semantic version from the current pom version.

## 7. Known gaps (flag in reviews; do not silently "fix")

- No checkstyle, spotbugs, PMD, or `.editorconfig` at repo level — code style
  and static-analysis are unenforced outside CodeQL and the content lint.
  Adopting the shared framework's build gates is a candidate follow-up; raise
  it as a ticket rather than bolting a linter onto an unrelated change.
- The `server`/`persistence` stack is explicitly "never-run" — don't grow it
  incidentally from desktop work.

## 8. Shared standards

General Java and SQL engineering standards live in the shared `~/.claude`
framework skills, not here: a Java language skill is planned; the SQL language
skill already exists and auto-loads — its SQLite dialect reference applies to
the desktop store (`ui`'s hand-written SQL and `db/migration-sqlite/`). This
file carries only what is specific to this repository.
