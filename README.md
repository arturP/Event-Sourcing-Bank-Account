# Event Sourcing Bank Account

An event-sourced banking domain written by hand on Java 21 and Spring Boot. There is no
event sourcing framework here: the aggregate, the event store, the replay and the read
models are all in this repository, which is the point of it. The same domain handed to a
framework instead lives in [reactive-event-sourcing](https://github.com/arturP/reactive-event-sourcing),
built on Akka persistence.

## Architecture

The code is organised as ports and adapters. The domain knows nothing about Spring, H2
or HTTP.

```
domain/                   account aggregate, events, value objects, invariants
application/ports/        incoming and outgoing interfaces, the only way in or out
application/commands/     write side: commands produce events
application/queries/      read side: projections and read models
infrastructure/           adapters: event store, cache, metrics, persistence, config
api/                      REST controllers and DTOs
```

Those boundaries are not a convention. `ArchitectureTest` fails the build when the domain
reaches for Spring or for a layer above it, when the application reaches past its ports,
and when new code in the presentation layer touches an aggregate directly. The controller
that does so today is listed in the rule by name, so the debt is visible and cannot grow.

Events are appended to an H2 table indexed by aggregate id, aggregate version, event type
and timestamp. Account snapshots are stored in a separate table, so rebuilding an account
does not have to replay its history from zero.

## What the domain does

Accounts open, take deposits and withdrawals against an overdraft limit, and transfer
between each other.

An account has a lifecycle rather than a boolean. It can be frozen, locked, suspended,
marked dormant, closed and reactivated, and every transition is an event. That makes two
questions answerable from the event history rather than from a status column: what has
happened to this account, and whether a given action is permitted right now.

The read side serves balance, status, account summary, search across accounts and
portfolio statistics, plus transaction history sliced by type, by recency, by day and by
month.

## Running it

```bash
mvn clean install
mvn spring-boot:run
```

The application starts on port 8080 with an in-memory H2 database, so state is gone when
it stops. Interactive API documentation is at `/swagger-ui.html`.

Spring Security is on the classpath without a configuration of its own, so the default
applies: HTTP Basic, with a generated password printed to the log at startup.

## API

One controller under `/api/accounts`. Rather than listing thirty endpoints, the shape is:

- account operations: create, deposit, withdraw, transfer, bulk operation
- lifecycle: freeze, unlock, lock, suspend, mark dormant, close, reactivate
- lifecycle queries: history, current restrictions, whether an action can be performed
- account reads: balance, status, summary, overdraft limit, search, statistics
- transaction reads: full history, recent, by type, today, monthly, statistics

The complete and current list is in Swagger UI. Actuator exposes `health`, `info` and
`metrics` under `/actuator`.

## Tests

```bash
mvn test
```

38 tests across seven files, covering the aggregate, the application service, async event
processing, the REST surface, two integration paths over the native event store, and the
architecture rules described above.

## Not in scope

Deliberately absent, so that reading the code does not raise the question:

- **Authentication.** Spring Security runs on its defaults and nothing more. An earlier
  attempt at JWT left dependencies and a token secret behind without a line of code to
  use them; both are gone.
- **A durable database.** H2 in memory only. There is no migration tooling.
- **Measured performance.** The cache and the metrics collector report their own numbers
  at runtime, but this repository contains no benchmark, so it makes no throughput claim.
- **Multi-currency, fraud detection, notifications.** Not attempted.

## Built with

Java 21, Spring Boot, H2, Caffeine, HikariCP, Dropwizard Metrics, springdoc-openapi.
