# Tecgen Soft Backend

Tecgen Soft is a Spring Boot SaaS backend for organizations, users, roles, permissions, subscription packages, and turf booking. This repository contains two services, core and turf, behind one API gateway. Hotel booking is a future extension; there is no hotel service in this repository.

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
| Core service | Authentication, organization management, organization-scoped roles and permissions, packages, subscriptions, domain verification, internal authorization, file storage | `http://localhost:8080` |
| Turf service | Venue settings, courts, availability, holds, bookings, payments/refunds, blocks, website content, reports | `http://localhost:8081` |
| Gateway | The only public entry point (`https://api.tecgensoft.com`): routing, CORS, tenant selection, rate limits, header cleaning | `http://localhost:8000` |
| Core PostgreSQL | Core-owned data | `localhost:5433`, database `tecgensoft_core` |
| Turf PostgreSQL | Turf-owned data | Direct local run: `localhost:5433/tecgensoft_turf`; Compose: `localhost:5434/tecgensoft_turf` |
| MinIO | Local object storage / console | `http://localhost:9000` / `http://localhost:9001` |

Both services use Java 26, Spring Boot 4.1.0, Spring Security, JPA, PostgreSQL, and Flyway. Core also provides JWT authentication, OpenAPI/Swagger UI, MinIO/S3 storage, and optional Redis caching in production.

For public requests Turf resolves the site hostname (`X-Tenant-Host`, set by the gateway, otherwise the request host) through Core's verified organization-domain mapping. Administrative turf requests take the organization from the path and delegate user authorization to Core. The services share an internal service key, but use separate databases. Flyway applies migrations at startup; Hibernate validates the resulting schema.

```text
pom.xml                         Maven reactor for all modules
compose.yml                     Local databases, MinIO, and applications
.env.example                    Compose environment template
tecgensoft-core-service/         Core application and migrations
tecgensoft-turf-service/         Turf application and migrations
tecgensoft-gateway/              API gateway (Spring Boot 4.2, Spring Cloud Gateway)
scripts/gateway-smoke.py         Core, Turf and gateway end-to-end smoke workflow
scripts/http-smoke.py            Older smoke workflow (targets APIs that no longer exist)
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

Requirements: JDK 26, PostgreSQL databases for Core and Turf, and Maven or the included module Maven wrappers. Docker Compose is optional if those dependencies are already running locally.

If using Docker Compose for infrastructure, configure `.env` and run:

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

Start turf in a second terminal after loading `.env` there. For your local PostgreSQL database, use port 5433 and database `tecgensoft_turf`:

```sh
export DATABASE_URL=jdbc:postgresql://localhost:5433/tecgensoft_turf
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
| `ORGANIZATION_ADMIN_EMAIL_DOMAIN` | Generated organization-admin email domain; template uses `legacy.local` |
| `ORGANIZATION_ADMIN_COMMON_PASSWORD` | Organization-admin initial/activation password |
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

## Packages, subscriptions, and entitlements

Packages are managed through the Core API, not hardcoded Basic/Pro tiers. No fixed commercial tier prices are defined in this repository. Supported products are `TURF` and `HOTEL`.

A package contains `code`, `displayName`, `product`, `description`, three-letter uppercase `currency`, nonnegative `price`, `features`, `limits`, `active`, and `availableForSale`. Creating another package with the same code creates the next version. Existing subscriptions retain their package-version reference; assigning a new package is an explicit subscription operation. Each organization may have one active subscription per product, so Turf and Hotel can be subscribed independently.

| Feature | Enables |
| --- | --- |
| `COURT_MANAGEMENT` | Court administration and public court listing |
| `BOOKING_MANAGEMENT` | Administrative bookings, availability, cancellations, slot blocks |
| `ONLINE_BOOKING` | Public availability, holds, checkout, payment-reference submission |
| `PAYMENT_MANAGEMENT` | Payment recording and ledger access |
| `REFUND_MANAGEMENT` | Refund recording |
| `VENUE_SETTINGS` | Venue settings and public venue information |
| `WEBSITE_CONTENT` | Hero, about, gallery, service content |
| `BASIC_REPORTS` | Organization reports |
| `HOTEL_MANAGEMENT`, `ROOM_MANAGEMENT`, `ROOM_AVAILABILITY`, `RESERVATION_MANAGEMENT`, `GUEST_MANAGEMENT`, `CHECK_IN_CHECK_OUT`, `HOTEL_PAYMENT_MANAGEMENT`, `HOTEL_SETTINGS` | Hotel configuration, rooms, reservations, guests, and hotel payments |

Supported product-specific limit keys are `MAX_COURTS` for Turf and `MAX_ROOMS` for Hotel, with nonnegative integer values. Turf data and permissions are scoped directly to an organization.

Subscriptions accept `ACTIVE`, `SUSPENDED`, or `CANCELLED`, a `MONTHLY` or `YEARLY` billing period, `startsAt`, `endsAt`, `packageId`, and `agreedPrice`. Dates use ISO-8601 instants. Entitlements account for organization activity, subscription status, and the subscription time window; responses can indicate `NOT_STARTED`, `EXPIRED`, or `INACTIVE`. Replacing an active subscription marks the previous active record `REPLACED` internally.

Feature entitlement and user permission are separate requirements. Buying a feature does not automatically grant every user permission to administer it. Platform super administrators manage packages and subscriptions; organization role assignments control tenant operations.

## Tenant onboarding

1. Start core with bootstrap enabled and log in as the platform super administrator.
2. Register the intended organization administrator using signup; obtain the user's ID (for example, by logging in as that user and calling `/api/v1/users/me`).
3. As super administrator, create an organization with `name` and `adminUserId`.
4. Create a `TURF` package with the required features and limits, then assign a dated active subscription to the organization.
5. Add a tenant domain. Publish the returned `challenge` as a DNS TXT record at `_tecgen-verification.<hostname>`, then call its verification endpoint.
6. Configure organization roles and permissions as needed.
7. As an authorized organization user, call the Turf administrative APIs. Create venue settings and courts, then publish website content.
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
| POST | `/auth/activate-organization-admin` | Activate organization admin with `email`, `oldPassword`, `newPassword`; public |
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
| POST, GET | `/organizations` | Create/list organizations |
| GET, PATCH | `/organizations/{id}` | Read/update an organization |
| POST, GET | `/organizations/{organizationId}/roles` | Create/list organization roles |
| POST | `/organizations/{organizationId}/roles/{roleId}/permissions` | Add permissions to an organization role |

Legacy list APIs generally use one-based `page` and `limit` (defaults `1` and `20`). SaaS lists use zero-based `page`; turf booking lists also default to `page=0`.

### Core package and subscription management

Catalog, package, subscription, and subscription payment routes require their corresponding Core platform permission. Request validation errors use the standard Core error envelope. Internal service endpoints require `X-Service-Key`; they are not public product APIs.

| Method | Path | Purpose |
| --- | --- | --- |
| GET, POST | `/api/v1/system/products` | List/create products (`PRODUCT_VIEW`, `PRODUCT_CREATE`) |
| PATCH | `/api/v1/system/products/{code}` | Update product (`PRODUCT_UPDATE`) |
| PATCH | `/api/v1/system/products/{code}/activation` | Activate/deactivate product (`PRODUCT_ACTIVATE`, `PRODUCT_DEACTIVATE`) |
| GET, POST | `/api/v1/system/products/{product}/features` | List/create product features (`FEATURE_VIEW`, `FEATURE_CREATE`) |
| PATCH | `/api/v1/system/features/{featureId}` | Update feature (`FEATURE_UPDATE`) |
| PATCH | `/api/v1/system/features/{featureId}/activation` | Activate/deactivate feature (`FEATURE_ACTIVATE`, `FEATURE_DEACTIVATE`) |
| GET, POST | `/api/v1/system/packages` | List/create versioned packages; filter by `?product=TURF` (`PACKAGE_VIEW`, `PACKAGE_CREATE`) |
| GET, PATCH | `/api/v1/system/packages/{packageId}` | Read/update package (`PACKAGE_VIEW`, `PACKAGE_UPDATE`) |
| PATCH | `/api/v1/system/packages/{packageId}/availability` | Change active/sale flags (`PACKAGE_UPDATE`) |
| PATCH | `/api/v1/system/packages/{packageId}/activation` | Activate/deactivate package (`PACKAGE_ACTIVATE`, `PACKAGE_DEACTIVATE`) |
| GET, PATCH, DELETE | `/api/v1/system/packages/{packageId}/features/{featureId}` | Read/set/remove typed package feature mapping (`PACKAGE_VIEW`, `PACKAGE_UPDATE`) |
| GET, POST | `/api/v1/system/organizations/{organizationId}/subscriptions` | Read history/create subscription (`SUBSCRIPTION_VIEW`, `SUBSCRIPTION_CREATE`) |
| GET | `/api/v1/system/organizations/{organizationId}/subscriptions/{subscriptionId}` | Read subscription details (`SUBSCRIPTION_VIEW`) |
| PATCH | `/api/v1/system/organizations/{organizationId}/subscriptions/{subscriptionId}/status` | Activate/suspend/cancel (`SUBSCRIPTION_ACTIVATE`, `SUBSCRIPTION_SUSPEND`, `SUBSCRIPTION_CANCEL`) |
| POST | `/api/v1/system/organizations/{organizationId}/subscriptions/{subscriptionId}/renewal` | Renew subscription (`SUBSCRIPTION_RENEW`) |
| POST | `/api/v1/system/organizations/{organizationId}/subscriptions/{subscriptionId}/trial-extension` | Extend trial (`SUBSCRIPTION_EXTEND_TRIAL`) |
| GET, POST | `/api/v1/system/organizations/{organizationId}/subscriptions/{subscriptionId}/payments` | Payment history/create (`SUBSCRIPTION_PAYMENT_VIEW`, `SUBSCRIPTION_PAYMENT_CREATE`) |
| PATCH | `/api/v1/system/organizations/{organizationId}/subscriptions/{subscriptionId}/payments/{paymentId}/refund` | Refund (`SUBSCRIPTION_PAYMENT_REFUND`) |
| GET, POST | `/api/v1/system/organizations/{organizationId}/domains` | List/register tenant domains; new domains are unverified (`ORGANIZATION_READ`, `ORGANIZATION_UPDATE`) |
| PATCH | `/api/v1/system/organizations/{organizationId}/domains/{domainId}/verification` | Verify/unverify a registered domain (`ORGANIZATION_UPDATE`) |

New organizations assigned `TURF_BASIC` receive the configurable first TURF trial (`TURF_BASIC_TRIAL_DAYS`, default 30). Trials carry no payment record; expiry is scheduled and entitlement lookups deny access after expiry. Package trial and feature configuration use the same `TURF_BASIC` package.

### Internal core APIs

All internal routes require `X-Service-Key: <INTERNAL_SERVICE_KEY>`. Entitlements return subscription state, trial days, boolean feature values, and limits. Tenant resolution only returns verified registered hostnames. Delegated access checks also validate the forwarded Bearer token and organization membership.

| Method | Path | Body / query |
| --- | --- | --- |
| GET | `/internal/v1/organizations/{organizationId}/entitlements` | `?product=TURF` or `?product=HOTEL`; returns the effective feature and limit entitlements |
| POST | `/internal/v1/tenants/resolve` | `{"hostname":"club.example.com"}`; resolve a verified tenant |
| POST | `/internal/v1/access/check` | Check an organization permission using service key plus user Bearer token |

### Turf administrative APIs

Prefix: `/api/v1/turf/organizations/{organizationId}`. Admin calls require a user Bearer token, membership of the organization in the path, the matching permission, and an active subscription. No tenant hostname is needed.

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

Admin bookings (`POST /bookings`, `POST /bookings/direct`) are confirmed immediately. They require a JWT, organization membership, `ADMIN_BOOKING`, and `TURF_BOOKING_CREATE`, and store the actor user ID.

Payment bodies contain `amount`, `method` (`BKASH`, `NAGAD`, `CARD`, `CASH`), `kind` (`ADVANCE`, `BALANCE`, `REFUND`), optional `reference`, `idempotencyKey`, and `verified`. Reuse the same idempotency key when retrying the same payment. A customer-submitted reference does not itself confirm payment.

### Turf public APIs

Prefix: `/api/v1/public/turf` (the older `/api/v1/public/turf/organizations/{organizationId}` form still works and must match the site). These calls resolve the site hostname against verified domains and require an eligible tenant subscription, but no user JWT. Hold/booking mutations use the returned secret as an ownership credential.

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

Checkout returns `CONFIRMED` when the venue needs no advance, otherwise `PENDING_PAYMENT`; an admin must verify the advance within `TURF_PAYMENT_WINDOW_MINUTES` (default 60, never past the slot start) or the slot is freed. Checkout counts toward `MAX_BOOKINGS`. There is no direct public booking.

`GET /api/v1/public/tenant` (Core) returns the site's organization ID, name, logo, product and enabled features.

Dates use `YYYY-MM-DD`. A slot contains `courtId`, `date`, integer `startHour` (0–23), and `durationHours` (1–24), subject to venue restrictions. Availability/pricing uses venue settings and regular/peak court prices.

## API gateway

| Path | Service | Accepted browser origins | Organization |
| --- | --- | --- | --- |
| `GET /api/v1/public/tenant` | Core | Organization sites, platform panels | Site hostname |
| `/api/v1/public/turf/**` | Turf | Organization sites, platform panels | Site hostname |
| `/api/v1/turf/organizations/{id}/**` | Turf | Platform panels | Path, with JWT |
| `/api/v1/{auth,users,organizations,permissions,system}/**` | Core | Platform panels | Core's own rules |
| Everything else, including `/internal/**` and Swagger | none | none | 404 |

A browser on an organization's verified domain needs no extra header: the gateway reads `Origin`, and that always wins over `X-Tenant-Host`. Server-side rendering and mobile clients send no `Origin`, so they must send `X-Tenant-Host: club.example.com`, and any response cache must include the host in its key. These values only select an organization; they never grant access.

Unknown origins get 403, a missing tenant 400, an unknown tenant 404, an unreachable service 503 and an exceeded limit 429 with `Retry-After`. Set `TRUSTED_PROXIES` to the reverse proxy's address, otherwise every client shares the proxy's rate limit. Rate limits are held in memory, so run one gateway instance until they move to Redis. Health is on port 8001 at `/actuator/health`. For local organization sites set `ALLOW_INSECURE_ORIGINS=true`.

Core's port 8080 and Turf's port 8081 must not be reachable from the internet.

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
curl -sS http://localhost:8080/api/v1/system/packages \
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
    "limits":{"MAX_COURTS":3}
  }'
```

After tenant onboarding, query public availability using the site hostname, the court ID and a date inside the venue booking window:

```sh
export COURT_ID='YOUR_COURT_UUID'
export BOOKING_DATE='YYYY-MM-DD'
curl -sS "http://localhost:8000/api/v1/public/turf/availability?courtId=$COURT_ID&date=$BOOKING_DATE&durationHours=1" \
  -H 'X-Tenant-Host: booking.example.com'
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

For the end-to-end smoke workflow, point the same variables at a separate pair of empty, disposable databases, then run:

```sh
python3 scripts/gateway-smoke.py
```

The script requires Python 3, `psql`, Java, and the three packaged JARs from the build. It starts core, turf and the gateway on ports `18080`, `18081` and `18000`, then checks onboarding, tenant isolation, booking, administration, CORS, concurrency, suspension and a core outage. It writes to the supplied databases; never use production or shared databases.

CI creates separate integration and smoke databases, runs `mvn -B verify`, executes the HTTP smoke script, and validates Compose configuration.

## Implementation plan

This is a repository-based status and proposed continuation plan, not a committed release schedule.

| Phase | Status | Scope / next work |
| --- | --- | --- |
| 1. Identity and access | Implemented | Signup/login/refresh, users, organization-scoped roles and permissions, organization-admin activation, super-admin bootstrap |
| 2. SaaS foundation | Implemented | Organizations, versioned packages, subscriptions, entitlements, domain verification |
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
- **Tenant resolution fails:** use a registered, verified hostname and confirm organization/subscription activity.
- **403 on tenant operations:** check the user's organization role, permission code, enabled package feature, subscription dates, and limits.
- **409 when reserving a slot:** an overlapping hold, booking, or block may already own it; refresh availability.

Licensed under [Apache License 2.0](LICENSE).
