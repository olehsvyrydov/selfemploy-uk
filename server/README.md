# Self-Employment :: Server (quarantined)

Server-side MTD submission and bank-import services, backed by the Panache persistence module.
**Not part of the desktop runtime.** The module builds only under the `server` Maven profile
(`mvn -Pserver ...`) and ships in no desktop artifact.

It is a plain library module today — there is no `quarkus-maven-plugin`, so ArC never validates
the CDI bean graph and the services are exercised only through Mockito unit tests.

## Before reactivating as a Quarkus application

Assembling these services into a real Quarkus app (adding the build plugin, or combining `core` +
`server` in a runnable module) requires resolving the following, which the current library build
does not surface:

- **Unsatisfied `IncomeService` / `ExpenseService` beans.** The desktop refactor made these two
  `core` services abstract, non-CDI contracts; their only concrete implementations are the
  SQLite/in-memory subclasses in the `ui` module, which `server` does not depend on. `CsvImportService`
  still `@Inject`s both. Provide server-side implementations (or `@Produces` methods) before ArC
  augmentation, or the build fails with an unsatisfied-dependency error.
- **Split packages across `core` and `server`.** Several packages (`uk.selfemploy.core.audit`,
  `.bankimport`, `.dedup`, `.service`, `.undo`) now span both jars. Quarkus does not support packages
  split across archives, and JPMS/jlink rejects two modules exporting the same package. Rename the
  server-owned classes to a distinct package (for example `uk.selfemploy.server.*`) before enabling
  the Quarkus build plugin.

## CI

No CI job activates the `server` profile, so these modules and their tests are not continuously
verified and can drift as `core`/`hmrc-api` signatures change. A cheap
`mvn -B -Pserver -pl persistence,server -am test-compile` step keeps the quarantine compiling.
