# Tecgen Soft Backend

Tecgen Soft is a Spring Boot SaaS backend for organizations, users, roles, permissions, subscription packages, and turf booking. This repository contains two runnable services: core and turf. Hotel booking is a future extension; there is no hotel service in this repository.

## Contents

- [Architecture and current scope](#architecture-and-current-scope)
- [Run with Docker Compose](#run-with-docker-compose)
- [Run locally with Java](#run-locally-with-java)
- [Environment variables](#environment-variables)
- [Subscription plans and entitlements](#subscription-plans-and-entitlements)
- [Tenant onboarding](#tenant-onboarding)
- [API reference](#api-reference)
- [Request examples](#request-examples)
- [Build and verification](#build-and-verification)
- [Implementation plan](#implementation-plan)
- [Troubleshooting](#troubleshooting)

## Architecture and current scope

| Component | Responsibility | Local address |
| --- | --- | --- |
| Core service | Authentication, organizations, branches, RBAC, packages, subscriptions, domain verification, internal authorization, file storage | `http://localhost:8080` |
| Turf service | Venue settings, courts, availability, holds, bookings, payments/refunds, blocks, website content, reports | `http://localhost:8081` |
| Core PostgreSQL | Core-owned data | `localhost:5433`, database `tecgensoft_core` |
| Turf PostgreSQL | Turf-owned data | `localhost:5434`, database `tecgensoft_turf` |
| MinIO | Local object storage / console | `http://localhost:9000` / `http://localhost:9001` |

Both modules use Java 26, Spring Boot 4.1.0, Spring Security, JPA, PostgreSQL, and Flyway. Core also provides JWT authentication, OpenAPI/Swagger UI, MinIO/S3 storage, and optional Redis caching in production.

Turf resolves the tenant through core using `X-Frontend-Host`. Administrative turf requests also delegate user authorization to core. The services share an internal service key, but use separate databases. Flyway applies migrations at startup; Hibernate validates the resulting schema.

```text
pom.xml                         Maven reactor for both services
compose.yml                     Local databases, MinIO, and applications
.env.example                    Compose environment template
tecgensoft-core-service/         Core application and migrations
tecgensoft-turf-service/         Turf application and migrations
scripts/http-smoke.py            HTTP integration smoke workflow
.github/workflows/main.yml       CI verification workflow
```

The root `src/` directory is not a module in the Maven reactor. Active service sources live under the two service directories.

## Run with Docker Compose

Requirements: Docker Engine/Desktop with Docker Compose v2. Docker builds provide the Java toolchain; host Java is unnecessary for this path.

1. Create the local configuration:

   ```sh
   cp .env.example .env
   ```

2. Edit `.env` and replace every `CHANGE_ME` value with independent secrets. Use at least 32 random characters for `JWT_SECRET` and `INTERNAL_SERVICE_KEY`. The template enables a local super administrator; set its email and password before startup. Do not commit `.env`.

3. Validate and start:

   ```sh
   docker compose config --quiet
   docker compose up --build -d
   docker compose ps
   docker compose logs -f tecgensoft-core-service tecgensoft-turf-service
   ```

4. Once core has started, open [Swagger UI](http://localhost:8080/swagger-ui/index.html) or [OpenAPI JSON](http://localhost:8080/v3/api-docs). Log in through `/api/v1/auth/login` using the bootstrap credentials from `.env`.

Core waits for PostgreSQL and MinIO health checks. Turf waits for its database and for core's container to start; core may still be initializing when turf starts. Wait for both application startup messages before sending requests.

Stop the stack with `docker compose down`. Named volumes retain database and object data. `docker compose down -v` deletes that local data permanently; use it only for an intentional reset.

## Run locally with Java

Requirements: JDK 26, Docker Compose for dependencies, and Maven or the included module Maven wrappers. Run the following from the repository root.

After configuring `.env`, start only the infrastructure:

```sh
docker compose up -d core-db turf-db minio
```

In each application terminal, load your trusted local configuration:

```sh
set -a
. ./.env
set +a
```

When sourcing `.env`, use shell-compatible assignments and quote values containing spaces or shell metacharacters. Spring Boot does not automatically load this root `.env` file.

Start core in the first terminal:

```sh
export SPRING_PROFILES_ACTIVE=local
export DATABASE_URL=jdbc:postgresql://localhost:5433/tecgensoft_core
export DATABASE_USERNAME=tecgensoft
export DATABASE_PASSWORD="$CORE_DB_PASSWORD"
export MINIO_URL=http://localhost:9000
sh tecgensoft-core-service/mvnw -f tecgensoft-core-service/pom.xml spring-boot:run
```

Start turf in a second terminal after loading `.env` there:

```sh
export DATABASE_URL=jdbc:postgresql://localhost:5434/tecgensoft_turf
export DATABASE_USERNAME=tecgensoft
export DATABASE_PASSWORD="$TURF_DB_PASSWORD"
export CORE_URL=http://localhost:8080
sh tecgensoft-turf-service/mvnw -f tecgensoft-turf-service/pom.xml spring-boot:run
```

On Windows use the corresponding `mvnw.cmd` scripts and your shell's environment-variable syntax. Each service also supports running its packaged JAR with `java -jar`, using the same environment.

## Environment variables

### Local Compose configuration

These variables are defined in [`.env.example`](.env.example) and consumed by [compose.yml](compose.yml).

| Variable | Purpose / template behavior |
| --- | --- |
| `CORE_DB_PASSWORD` | Core PostgreSQL password; passed to core as `DATABASE_PASSWORD` |
| `TURF_DB_PASSWORD` | Turf PostgreSQL password; passed to turf as `DATABASE_PASSWORD` |
| `JWT_SECRET` | Core user JWT signing secret |
| `INTERNAL_SERVICE_KEY` | Shared key for core/turf internal calls; keep separate from the JWT secret |
| `MINIO_ROOT_USER` | MinIO username; template uses `tecgensoft-local` |
| `MINIO_ROOT_PASSWORD` | MinIO password and core storage credential |
| `BRANCH_ADMIN_EMAIL_DOMAIN` | Generated legacy branch-admin email domain; template uses `legacy.local` |
| `BRANCH_ADMIN_COMMON_PASSWORD` | Legacy branch-admin initial/activation password |
| `SUPER_ADMIN_BOOTSTRAP_ENABLED` | Enables bootstrap account creation; template `true`, application/Compose fallback `false` |
| `SUPER_ADMIN_FIRST_NAME` | Bootstrap administrator first name |
| `SUPER_ADMIN_LAST_NAME` | Bootstrap administrator last name |
| `SUPER_ADMIN_EMAIL` | Bootstrap administrator login email |
| `SUPER_ADMIN_PASSWORD` | Bootstrap administrator password |
| `CORS_ALLOWED_ORIGINS` | Comma-separated frontend origins; Compose defaults to `http://localhost:3000` |

### Application and production configuration

| Variable | Service | Behavior |
| --- | --- | --- |
| `PORT` | Both | Core defaults to `8080`; turf to `8081` |
| `SPRING_PROFILES_ACTIVE` | Core | Defaults to `local`; `prod` enables production database/cache/storage settings |
| `DATABASE_URL` | Both | JDBC PostgreSQL URL; local defaults use the service-specific host ports above |
| `DATABASE_USERNAME` | Both | Local default `tecgensoft`; required explicitly for core production |
| `DATABASE_PASSWORD` | Both | Database password |
| `CORE_URL` | Turf | Core base URL; default `http://localhost:8080`, Compose uses the core service hostname |
| `MINIO_URL` | Core/local | Default `http://localhost:9000`; Compose uses `http://minio:9000` |
| `MINIO_BUCKET_NAME` | Core/local | Default `tecgensoft` |
| `STORAGE_TYPE` | Core | Default `minio` locally and `s3` in production |
| `REDIS_HOST` | Core/prod | Default `redis` |
| `REDIS_PORT` | Core/prod | Default `6379` |
| `REDIS_PASSWORD` | Core/prod | Redis authentication password |
| `S3_REGION` | Core/prod | Default `eu-north-1` |
| `S3_BUCKET_NAME` | Core/prod | Default `familie-munshi-storage`; set your deployment bucket |
| `S3_PUBLIC_BASE_URL` | Core/prod | Optional public URL base |

S3 uses the AWS SDK credential provider chain, such as a runtime IAM role or `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` (and `AWS_SESSION_TOKEN` for temporary credentials). The local Compose stack supplies neither Redis nor production S3 infrastructure. Changing `.env` alone does not forward extra production settings into containers; update deployment environment mappings accordingly.

Access tokens last 15 minutes; refresh tokens last 7 days in the checked-in core configuration. Core uploads allow `jpg,jpeg,png,webp,pdf`, up to 10 MB per file/request.

## Subscription plans and entitlements

Plans are API-managed product packages, not hardcoded Basic/Pro tiers. No fixed commercial tier prices are defined in this repository. Currently the accepted product is `TURF`.

A package contains `code`, `displayName`, `product`, `description`, three-letter uppercase `currency`, nonnegative `price`, `features`, `limits`, `active`, and `availableForSale`. Creating another package with the same code creates the next version. Existing subscriptions retain their package reference; assigning a new package is an explicit subscription operation.

| Feature | Enables |
| --- | --- |
| `COURT_MANAGEMENT` | Court administration and public court listing |
| `BOOKING_MANAGEMENT` | Administrative bookings, availability, cancellations, slot blocks |
| `ONLINE_BOOKING` | Public availability, holds, checkout, payment-reference submission |
| `PAYMENT_MANAGEMENT` | Payment recording and ledger access |
| `REFUND_MANAGEMENT` | Refund recording |
| `VENUE_SETTINGS` | Venue settings and public venue information |
| `WEBSITE_CONTENT` | Hero, about, gallery, service content |
| `BASIC_REPORTS` | Branch reports |

Supported limit keys are `MAX_BRANCHES` and `MAX_COURTS`, with nonnegative integer values. Branch creation checks `MAX_BRANCHES`; court creation checks `MAX_COURTS`. Include the limits your package needs; branch creation treats a missing branch limit as zero.

Subscriptions accept `ACTIVE`, `SUSPENDED`, or `CANCELLED`, a `MONTHLY` or `YEARLY` billing period, `startsAt`, `endsAt`, `packageId`, and `agreedPrice`. Dates use ISO-8601 instants. Entitlements account for organization activity, subscription status, and the subscription time window; responses can indicate `NOT_STARTED`, `EXPIRED`, or `INACTIVE`. Replacing an active subscription marks the previous active record `REPLACED` internally.

Feature entitlement and user permission are separate requirements. Buying a feature does not automatically grant every user permission to administer it. Platform super administrators manage SaaS provisioning; organization/branch role assignments control tenant operations.

## Tenant onboarding

1. Start core with bootstrap enabled and log in as the platform super administrator.
2. Register the intended organization administrator using signup; obtain the user's ID (for example, by logging in as that user and calling `/api/v1/users/me`).
3. As super administrator, create an organization with `name` and `adminUserId`.
4. Create a `TURF` package with the required features and limits, then assign a dated active subscription to the organization.
5. Add a tenant domain. Publish the returned `challenge` as a DNS TXT record at `_tecgen-verification.<hostname>`, then call its verification endpoint.
6. Create an organization branch and configure role permissions as needed.
7. As an authorized tenant user, send the verified hostname in `X-Frontend-Host` to turf. Create venue settings and courts, then publish website content.
8. Public clients can browse availability, create a hold, and check out using the hold ID and secret. Submit a payment reference for administrator verification; administrators record verified payments and any refunds.

A hosts-file entry does not satisfy DNS TXT verification. The smoke script bypasses DNS only by modifying its disposable test fixture; that is not the normal onboarding flow.

## API reference

Core base URL: `http://localhost:8080`. Turf base URL: `http://localhost:8081`. IDs are UUIDs. JSON APIs normally return `success`, `message`, and `data`; internal endpoints return their own DTOs. Use core Swagger for detailed core request schemas. Turf has no OpenAPI dependency; its request definitions are in [TurfRequests.java](tecgensoft-turf-service/src/main/java/com/tecgensoft/turf/http/dtos/TurfRequests.java).

### Core authentication and administration

Paths below are relative to `/api/v1`. Protected calls use `Authorization: Bearer <accessToken>` and may require additional permissions.

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/auth/signup` | Register user; public |
| POST | `/auth/login` | Obtain access and refresh tokens; public |
| POST | `/auth/refresh-token` | Exchange `refreshToken` for an access token; public |
| POST | `/auth/activate-branch-admin` | Activate legacy admin with `email`, `oldPassword`, `newPassword`; public |
| GET | `/users/me` | Current user |
| GET | `/users`, `/users/{id}` | List/read users |
| GET | `/organizations/me` | Current user's organizations |
| GET | `/permissions`, `/permissions/{id}` | List/read permissions |
| GET | `/permissions/tree`, `/permissions/me` | Permission hierarchy / current user's permissions |
| POST, GET | `/system/roles` | Create/list platform roles |
| GET | `/system/roles/{id}` | Read role |
| POST | `/system/roles/{id}/permissions` | Assign role permissions |
| POST | `/system/users/{userId}/roles` | Assign user platform role |
| DELETE | `/system/users/{userId}/roles/{roleId}` | Remove role assignment |
| POST, GET | `/branches` | Legacy branch creation/listing; creation uses multipart form data |
| GET, PATCH | `/branches/{id}` | Read/update legacy branch; updates use multipart form data |

Legacy list APIs generally use one-based `page` and `limit` (defaults `1` and `20`). SaaS lists use zero-based `page`; turf booking lists also default to `page=0`.

### Core SaaS provisioning

All paths below are relative to `/api/v1/system/saas` and require a platform super administrator. JSON request schemas are in [SaasRequests.java](tecgensoft-core-service/src/main/java/com/familiemunshi/http/dtos/requests/SaasRequests.java).

| Method | Path | Purpose |
| --- | --- | --- |
| GET, POST | `/organizations` | List/create organizations |
| GET, PATCH | `/organizations/{id}` | Read organization; change name, activity, or admin |
| GET, POST | `/packages` | List/create versioned packages |
| PATCH | `/packages/{id}/availability` | Change `active` / `availableForSale` |
| POST | `/organizations/{id}/subscriptions` | Assign subscription |
| PATCH | `/organizations/{id}/subscription` | Replace/update subscription through the same subscription operation |
| GET | `/organizations/{id}/entitlements` | Inspect TURF entitlements |
| POST | `/organizations/{id}/domains` | Register hostname and generate challenge |
| POST | `/organizations/{id}/domains/{domainId}/verify` | Verify DNS TXT challenge |
| POST | `/organizations/{id}/branches` | Create organization-owned branch |
| PATCH | `/organizations/{id}/role-permissions` | Set organization role permission codes |

### Internal core APIs

Require `X-Service-Key: <INTERNAL_SERVICE_KEY>`. These endpoints support service-to-service calls.

| Method | Path | Body / query |
| --- | --- | --- |
| POST | `/internal/v1/tenants/resolve` | `{"hostname":"booking.example.com"}` |
| POST | `/internal/v1/access/check` | `organizationId`, `branchId`, `product`, `permissionCode`; also requires user Bearer token |
| GET | `/internal/v1/organizations/{id}/entitlements` | `?product=TURF` |

### Turf administrative APIs

Prefix: `/api/v1/turf/branches/{branch}`. Every call requires a user Bearer token and `X-Frontend-Host: <verified tenant hostname>`. Core checks the relevant permission and subscription feature.

| Method | Suffix | Purpose |
| --- | --- | --- |
| GET, POST, PATCH | `/settings` | Read/create/update venue configuration |
| GET, POST | `/courts` | List/create courts |
| GET, PATCH, DELETE | `/courts/{id}` | Read/update/deactivate court |
| GET | `/availability` | Query `courtId`, `date`, optional `durationHours` (default `1`) |
| GET | `/bookings` | List with required `from`, `to` dates and optional `page` |
| POST | `/bookings` | Create phone/walk-in booking |
| GET | `/bookings/{id}` | Booking details |
| PATCH | `/bookings/{id}/cancellation` | Cancel with `reason` |
| GET, POST | `/bookings/{id}/payments` | Read ledger / record payment or refund |
| GET, POST | `/blocks` | List/create slot blocks |
| PATCH | `/blocks/{id}/release` | Release block |
| GET, POST | `/content` | List/create website content |
| PATCH, DELETE | `/content/{id}` | Update/delete content |
| GET | `/reports` | Report with required `from`, `to` dates |

Payment bodies contain `amount`, `method` (`BKASH`, `NAGAD`, `CARD`, `CASH`), `kind` (`ADVANCE`, `BALANCE`, `REFUND`), optional `reference`, `idempotencyKey`, and `verified`. Reuse the same idempotency key when retrying the same payment. A customer-submitted reference does not itself confirm payment.

### Turf public APIs

Prefix: `/api/v1/public/turf/branches/{branch}`. These calls require `X-Frontend-Host` and an eligible tenant subscription, but no user JWT. Hold/booking mutations use the returned secret as an ownership credential.

| Method | Suffix | Purpose |
| --- | --- | --- |
| GET | `/venue` | Venue information |
| GET | `/content` | Active website content |
| GET | `/courts` | Active courts |
| GET | `/availability` | `courtId`, `date`, optional `durationHours` |
| POST | `/holds` | Reserve `slot` temporarily |
| PATCH | `/holds/{id}/release` | Release with `secret` |
| POST | `/bookings` | Checkout with `holdId`, `secret`, customer details |
| PATCH | `/bookings/{id}/payment-reference` | Submit `secret` and `reference` |

Dates use `YYYY-MM-DD`. A slot contains `courtId`, `date`, integer `startHour` (0–23), and `durationHours` (1–24), subject to venue restrictions. Availability/pricing uses venue settings and regular/peak court prices.

## Request examples

Log in using the configured bootstrap credentials:

```sh
curl -sS http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.test","password":"YOUR_CONFIGURED_PASSWORD"}'
```

Copy `data.accessToken` from the response into `TOKEN`. The following is an example package definition, not a seeded commercial plan:

```sh
export TOKEN='YOUR_SUPER_ADMIN_ACCESS_TOKEN'
curl -sS http://localhost:8080/api/v1/system/saas/packages \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "code":"TURF_STANDARD",
    "displayName":"Turf Standard",
    "product":"TURF",
    "description":"Example full-feature turf package",
    "currency":"BDT",
    "price":1000,
    "features":["COURT_MANAGEMENT","BOOKING_MANAGEMENT","ONLINE_BOOKING","PAYMENT_MANAGEMENT","REFUND_MANAGEMENT","VENUE_SETTINGS","WEBSITE_CONTENT","BASIC_REPORTS"],
    "limits":{"MAX_BRANCHES":1,"MAX_COURTS":3},
    "active":true,
    "availableForSale":true
  }'
```

After tenant onboarding, query public availability using actual branch/court IDs and a date inside the venue booking window:

```sh
export BRANCH_ID='YOUR_BRANCH_UUID'
export COURT_ID='YOUR_COURT_UUID'
export BOOKING_DATE='YYYY-MM-DD'
curl -sS "http://localhost:8081/api/v1/public/turf/branches/$BRANCH_ID/availability?courtId=$COURT_ID&date=$BOOKING_DATE&durationHours=1" \
  -H 'X-Frontend-Host: booking.example.com'
```

## Build and verification

Use JDK 26. From the repository root, with Maven installed:

```sh
mvn -B verify
```

Alternatively, use the core wrapper to run the root reactor:

```sh
sh tecgensoft-core-service/mvnw -f pom.xml -B verify
```

Integration tests are gated by `TEST_CORE_DATABASE_URL` / `TEST_TURF_DATABASE_URL`; a successful build without those variables does not demonstrate database integration coverage. To enable it, create two dedicated disposable PostgreSQL databases and export:

```sh
export TEST_CORE_DATABASE_URL=jdbc:postgresql://localhost:5432/tecgen_core_test
export TEST_TURF_DATABASE_URL=jdbc:postgresql://localhost:5432/tecgen_turf_test
export TEST_DATABASE_USERNAME=postgres
export TEST_DATABASE_PASSWORD='YOUR_TEST_DATABASE_PASSWORD'
mvn -B verify
```

For the HTTP smoke workflow, point the same variables at a separate pair of disposable databases, then run:

```sh
python3 scripts/http-smoke.py
```

The script requires Python 3, `psql`, Java, and both packaged JARs from the build. It starts core/turf on ports `18080`/`18081`, creates fixtures, and exercises authorization, tenant access, pricing, checkout, payment confirmation/idempotency, reports, and competing holds. It terminates its application processes and prints log paths. It writes fixture data to the supplied databases; never use production or shared databases.

CI creates separate integration and smoke databases, runs `mvn -B verify`, executes the HTTP smoke script, and validates Compose configuration.

## Implementation plan

This is a repository-based status and proposed continuation plan, not a committed release schedule.

| Phase | Status | Scope / next work |
| --- | --- | --- |
| 1. Identity and access | Implemented | Signup/login/refresh, users, roles, permissions, legacy branch-admin activation, super-admin bootstrap |
| 2. SaaS foundation | Implemented | Organizations and ownership, versioned packages, subscriptions, entitlements, domain verification, branch provisioning |
| 3. Turf operations | Implemented | Venue settings, courts, availability, holds, public/admin bookings, blocks, payment/refund ledger, content, reports |
| 4. Local development and verification | Implemented | Compose, independent PostgreSQL databases, MinIO, Flyway, integration tests, HTTP smoke workflow, CI |
| 5. Product integration | Proposed | Build/connect frontend screens, agree commercial package tiers and prices, complete real-domain onboarding |
| 6. Payments and billing automation | Proposed | Payment gateway integration, callback verification, automated subscription billing/renewals and customer notifications |
| 7. Production operations | Proposed | Deployment configuration, managed database/storage/cache, monitoring, backups and restore validation, operational runbooks |
| 8. Additional products | Future | Define hotel domain and APIs, then add a separate service and product entitlement support |

## Troubleshooting

- **Missing environment placeholder:** configure `.env` for Compose; export variables explicitly when running Java. `CORE_DB_PASSWORD` / `TURF_DB_PASSWORD` must be mapped to the application's `DATABASE_PASSWORD` for host runs.
- **Database authentication fails after changing passwords:** PostgreSQL's existing volume retains initialized credentials. Update the database credentials deliberately or reset only disposable volumes.
- **Unsupported Java release:** both modules target Java 26. Check `java -version`, `JAVA_HOME`, and `mvn -version`.
- **Turf cannot reach core:** host runs need `CORE_URL=http://localhost:8080`; containers use the Compose service hostname. Match `INTERNAL_SERVICE_KEY` across both services.
- **Tenant resolution fails:** use a registered, DNS-verified hostname in `X-Frontend-Host`, and confirm organization/subscription activity.
- **403 on tenant operations:** check the user's organization/branch role, permission code, enabled package feature, subscription dates, and limits.
- **409 when reserving a slot:** an overlapping hold, booking, or block may already own it; refresh availability.

Licensed under [Apache License 2.0](LICENSE).
