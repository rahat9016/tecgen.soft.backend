# Package and Subscription Module Handoff

This document records the package/subscription implementation and the decisions needed to continue work on another computer.

## Naming and ownership

- Core service owns the package catalog, organization subscriptions, and entitlement decisions.
- Turf and Hotel are products represented by `ProductCode`; package features and limits are validated against their product.
- Package/subscription data lives in the Core PostgreSQL database.
- API paths and database names use `packages` and `subscriptions`; they do not include `saas`.
- An organization's user permissions remain separate from package entitlements. A subscribed feature does not grant a user role permission.
- The Kirja Swappi repository describes a book-exchange application and did not provide a matching package/subscription feature to copy. The implementation follows this repository's existing DAO/repository/mapper/service/controller conventions.

## Request flow

```text
Platform Super Admin
        |
        +--> Core package API --> packages table
        |
        +--> Core subscription API --> subscriptions table
                                      |
Turf service -- X-Service-Key --> Core entitlement API
                                      |
                                      +--> organization + subscription + package
                                           => active, features, limits
```

Each organization can have one active subscription per product. It can therefore subscribe to Turf and Hotel independently. Creating another package with the same code creates the next version; existing subscriptions retain their package version.

## API routes

Platform package/subscription APIs require the corresponding platform permission and a user Bearer token.

| Method | Route | Permission / purpose |
| --- | --- | --- |
| `GET` | `/api/v1/system/packages?product=TURF` | `PACKAGE_READ`; list packages, optional `TURF`/`HOTEL` filter |
| `POST` | `/api/v1/system/packages` | `PACKAGE_CREATE`; create a versioned package |
| `PATCH` | `/api/v1/system/packages/{packageId}/availability` | `PACKAGE_UPDATE`; update active/sale flags |
| `GET` | `/api/v1/system/organizations/{organizationId}/subscriptions` | `SUBSCRIPTION_READ`; list history |
| `POST` | `/api/v1/system/organizations/{organizationId}/subscriptions` | `SUBSCRIPTION_ASSIGN`; assign a package |
| `PATCH` | `/api/v1/system/organizations/{organizationId}/subscriptions/{subscriptionId}/status` | `SUBSCRIPTION_UPDATE`; activate, suspend, or cancel |
| `GET` | `/internal/v1/organizations/{organizationId}/entitlements?product=TURF` | Requires `X-Service-Key`; service-to-service entitlement lookup |

The internal response matches Turf's `CoreClient.Entitlements` fields: `organizationId`, `packageId`, `packageCode`, `version`, `subscriptionStatus`, `active`, `features`, and `limits`.

### Create package example

```json
{
  "code": "TURF_STANDARD",
  "displayName": "Turf Standard",
  "product": "TURF",
  "description": "Example turf package",
  "currency": "BDT",
  "price": 1000,
  "features": ["COURT_MANAGEMENT", "ONLINE_BOOKING"],
  "limits": {"MAX_COURTS": 3}
}
```

`active` and `availableForSale` start as `true`. Feature and limit keys are product-validated. The current supported Turf feature keys are `COURT_MANAGEMENT`, `BOOKING_MANAGEMENT`, `ONLINE_BOOKING`, `PAYMENT_MANAGEMENT`, `REFUND_MANAGEMENT`, `VENUE_SETTINGS`, `WEBSITE_CONTENT`, and `BASIC_REPORTS`; Turf supports `MAX_COURTS`.

The Hotel catalog currently accepts `HOTEL_MANAGEMENT`, `ROOM_MANAGEMENT`, `ROOM_AVAILABILITY`, `RESERVATION_MANAGEMENT`, `GUEST_MANAGEMENT`, `CHECK_IN_CHECK_OUT`, `HOTEL_PAYMENT_MANAGEMENT`, and `HOTEL_SETTINGS`; Hotel supports `MAX_ROOMS`. These are catalog keys only; a Hotel service is not present in this repository yet.

### Assign subscription example

```json
{
  "packageId": "PACKAGE_UUID",
  "billingPeriod": "MONTHLY",
  "startsAt": "2026-11-17T00:00:00Z",
  "endsAt": "2026-12-17T00:00:00Z",
  "agreedPrice": 1000
}
```

Subscription states are `ACTIVE`, `SUSPENDED`, `CANCELLED`, and system-managed `REPLACED`. Entitlement lookup also reports effective states such as `NOT_STARTED`, `EXPIRED`, or `INACTIVE`. A future-dated assignment is rejected if it would replace an effective active subscription. An active package replacement marks the old subscription `REPLACED`.

## File placement

All Java paths below are under `tecgensoft-core-service/src/main/java/com/tecgensoft/` unless otherwise stated.

### Controllers

- `http/controllers/SystemPackageController.java`
- `http/controllers/SystemSubscriptionController.java`
- `http/controllers/InternalEntitlementsController.java`

### Request and response DTOs

- `http/dtos/requests/CreatePackageRequest.java`
- `http/dtos/requests/UpdatePackageAvailabilityRequest.java`
- `http/dtos/requests/AssignSubscriptionRequest.java`
- `http/dtos/requests/UpdateSubscriptionStatusRequest.java`
- `http/dtos/responses/PackageResponse.java`
- `http/dtos/responses/SubscriptionResponse.java`
- `http/dtos/responses/EntitlementsResponse.java`

### Persistence and mapping

- `jpa/daos/PackageDao.java`
- `jpa/daos/SubscriptionDao.java`
- `jpa/repositories/PackageRepository.java`
- `jpa/repositories/SubscriptionRepository.java`
- `mapper/PackageMapper.java`
- `mapper/SubscriptionMapper.java`

### Service and domain types

- `service/PackageService.java`
- `service/SubscriptionService.java`
- `service/InternalServiceKeyValidator.java`
- `service/entities/SoftwarePackage.java`
- `service/entities/Subscription.java`
- `service/enums/ProductCode.java`
- `service/enums/PackageFeatureCode.java`
- `service/enums/BillingPeriod.java`
- `service/enums/SubscriptionStatus.java`

### Existing files updated

- `common/configs/SecurityConfig.java`: permits the internal route through the security filter chain; the controller still verifies the service key.
- `common/policy/PermissionPolicy.java`: platform permission policy includes package/subscription permissions.
- `service/enums/PermissionCode.java`: adds package/subscription permission codes; the existing permission seeder and Super Admin bootstrap pick them up.
- `src/main/resources/application.yml`: reads `INTERNAL_SERVICE_KEY` as `internal.service-key`.
- `tecgensoft-turf-service/src/main/java/com/tecgensoft/turf/service/CoreClient.java`: returns `403` for inactive entitlement and `503` for failed/malformed Core entitlement responses.
- `README.md`: documents routes, package payloads, limits, and known integration gaps.

### Database migration

- `tecgensoft-core-service/src/main/resources/db/migration/V4__packages_and_subscriptions.sql`
- Creates `packages` and `subscriptions`, indexes product lookup, enforces package version uniqueness and one active subscription per organization/product, and links subscription product to the package product.

## Configuration

The Core service needs the same `INTERNAL_SERVICE_KEY` value as the Turf service. Compose already maps this variable into both services. Do not put real secrets in this document or commit `.env`.

Flyway runs migrations at Core startup according to the current Flyway configuration. Before deploying to a database with data, take a backup and inspect its Flyway history. The V4 migration has not been applied to a live database as part of this work.

## Verification and current worktree

- Core: `./mvnw -q -DskipTests compile` succeeded.
- Turf: `./mvnw -q -DskipTests compile` succeeded.
- `git diff --check` succeeded.
- `docker compose config --quiet` succeeded.
- Tests were not run.
- Package/subscription changes are currently local, uncommitted work. To continue on another PC, commit and push this worktree (including untracked Java and migration files), then pull it there. `.env` is ignored and must be configured separately on the other PC.

## Known follow-up work

1. Core tenant resolution (`/internal/v1/tenants/resolve`) and delegated access-check (`/internal/v1/access/check`) controllers are not implemented in the current Core source. Turf `CoreClient` calls them, so Turf's tenant and admin authorization flows remain incomplete until those endpoints are added.
2. Add and validate the Hotel service before treating Hotel feature keys as active capabilities.
3. Add subscription billing automation/payment callbacks only after selecting the payment provider and billing rules.
4. Run migration V4 against a disposable PostgreSQL database and test package versioning, active-subscription uniqueness, status transitions, and tenant entitlements before production rollout.
