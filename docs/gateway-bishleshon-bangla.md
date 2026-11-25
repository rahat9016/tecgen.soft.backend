# API Gateway — পূর্ণ বিশ্লেষণ (বাংলা)

এই নথি `tecgensoft-gateway`-এর বর্তমান কোড, Multi-Service Architecture Plan-এর সাথে এর মিল,
এবং multi-domain public booking flow — সব এক জায়গায় ব্যাখ্যা করে।

সোর্স ফাইল: `EdgeFilter` · `RouteConfig` · `ApiRoute` · `TenantDirectory` · `ClientIpResolver` · `RateLimiter` · `GatewayProperties` · `application.yml`

---

## ১. গেটওয়ে জিনিসটা কী, কেন

আলাদা একটা **deployable Spring Boot অ্যাপ** (`tecgensoft-gateway`, পোর্ট 8000)। সব frontend
(platform admin panel + প্রতিটা organization-এর সাইট) এর request এই গেটওয়ে দিয়ে ঢোকে,
তারপর Core বা Turf-এ যায়। কোনো browser সরাসরি service-এ hit করে না।

আলাদা রাখার কারণ (plan §2): গেটওয়ে Core-এর ভেতরে ঢুকিয়ে দিলে Core down হলে সব client API down।
তাই আলাদা।

গেটওয়ের কাজ = **edge policy** (route, limit, verify, log)। Business logic না, DB না,
package decision না — ওটা Core-এর কাজ।

---

## ২. একটা request-এর পুরো যাত্রা (EdgeFilter)

`EdgeFilter.java` = গেটওয়ের brain। প্রতিটা request এই order-এ check হয় (`doFilterInternal`, line 82):

```
১. Request ID         → X-Request-Id আছে? valid? না হলে generate (UUID)
২. Path reachable?    → ApiRoute.of(method, path); list-এ নাই হলে 404
৩. Body size check    → tenant site 64KB, admin 11MB; বেশি হলে 413
৪. Overall rate limit → client IP-তে 600/min; বেশি হলে 429
৫. Caller কে?         → Origin header দেখে: NoOrigin / Platform / Site
৬. CORS process       → browser হলে origin allowed কিনা
৭. Tenant host resolve→ site route হলে organization hostname বের করে
৮. Route rate limit   → read/hold/booking/login/admin bucket
৯. Forward            → SanitizedRequest বানিয়ে downstream-এ পাঠায়
```

যেকোনো step fail → `Rejection` → JSON error `{success:false, message}` + সঠিক status।
শেষে **সব সময়** access log: method, path, status, ms, client IP, tenant, request-id (line 124)।

### মূল nuance গুলো
- **Caller ৩ রকম** (line 62): `NoOrigin` (non-browser, mobile/SSR), `Platform` (tecgensoft admin panel),
  `Site` (organization-এর নিজস্ব domain)। Site route-এ Site origin **নিজেই win করে** —
  client-এর পাঠানো `X-Tenant-Host` override করতে পারে না (line 183)। এটা tenant spoofing ঠেকায়।
- **SSR multiplier** (line 97): নিজের SSR server (Next.js টাইপ) থেকে এলে rate limit 25x বড় —
  কারণ এক IP থেকে অনেক user।
- **Unreachable handling** (line 265): downstream down হলে 503, timeout হলে 504। Clean error, 500 না।

---

## ৩. ফাইল গুলো কী করে

| File | কাজ |
| --- | --- |
| `EdgeFilter` | পুরো request pipeline — verify, limit, log |
| `ApiRoute` | **Allowlist**। বাইরে থেকে reachable path: `/api/v1/public/tenant`, `/public/turf/**`, `/turf/organizations/**`, `/api/v1/{auth,users,organizations,permissions,system}`। বাকি সব 404 |
| `RouteConfig` | Allow হলে কোথায় পাঠাবে: `/public/turf` + `/turf` → Turf; বাকি `/api/v1/**` → Core |
| `TenantDirectory` | Organization hostname → Core `/internal/v1/tenants/resolve` call করে verify করে। 60s cache (found) / 15s (unknown) |
| `ClientIpResolver` | আসল client IP বের করে — শুধু configured trusted proxy-কে believe করে |
| `RateLimiter` | Token-bucket rate limit engine |
| `GatewayProperties` + `application.yml` | সব config: URL, limits, origins, service-key |

**Security defense:** `ApiRoute` কঠিন allowlist — percent-encoding, matrix param, empty/dot segment সব
reject (line 34)। মানে path-traversal টাইপ attack edge-এই মারা।

---

## ৪. Plan-এর সাথে মিল — প্রায় ৭০%

Plan §2-তে গেটওয়ের **১০টা কাজ** লিস্ট করা।

### ✅ করা (৭টা)
1. **Routing** — RouteConfig, Core+Turf
2. **Rate limit** — strong, per-route (overall/read/hold/booking/login/refresh/admin) + SSR 25x
3. **Request size limit** — 11MB/64KB
4. **Correlation ID** — X-Request-Id
5. **Error mapping** — consistent JSON
6. **Access log** — structured
7. **Origin/CORS** — code-এ আছে

### ◑ Partial (১টা)
8. **Org ID resolve/validate** — TenantDirectory করে, কিন্তু শুধু tenant-site route-এ।
   Admin route-এ org binding নাই।

### ❌ নাই (২টা) — এইটাই আসল gap
9. **Token / auth validation** — **বড় gap**। Plan §2 + §6 বলে: গেটওয়ে token validate করবে +
   organization-ID-কে authenticated user-এর সাথে মিলাবে। বর্তমান code token **validate করে না** —
   Authorization header শুধু forward করে দেয়, validation Core/Turf করে।
10. **TLS termination** — code http only; এটা ingress/infra (nginx/load balancer)-এর উপর assume করা।

### Plan "গেটওয়ের কাজ নয়" — সব মেনে চলে ✅
booking logic নাই, package catalog নাই, cross-service DB query নাই, business-permission-এর final decision নাই।

---

## ৫. Architecture-level mismatch (ছোট না, important)

- **gRPC নাই।** Plan §4 বলে — service-to-service internal communication, বিশেষ করে package access check,
  gRPC-তে হবে (`PackageAccessService.CheckOrganizationPackage`)। বর্তমান গেটওয়ে internal call **REST** দিয়ে
  করে (`/internal/v1/tenants/resolve` + `X-Service-Key`)। Transport choice plan থেকে আলাদা।
- **Dependency এখনো Core-এ নাই।** গেটওয়ে `/internal/v1/tenants/resolve` call করে, কিন্তু handoff doc-এর
  follow-up #1 বলে এই endpoint Core source-এ **নাই**। Core-এ add না করা পর্যন্ত tenant-site flow run-time-এ
  ভাঙবে (503)।
- **Hotel route নাই** — expected, Hotel service এখনো তৈরি হয়নি।

---

## ৬. Multi-domain public booking flow

### Setup
```
turf.com      ─┐
abc.com       ─┼─► api.tecgensoft.com (gateway) ─► Turf (public api)
hometurf.com  ─┘                                 ─► Core
                  প্রতিটা আলাদা repo/deploy
                  প্রতিটা আলাদা organization
```
প্রতিটা frontend domain = একটা **Site caller** = একটা **organization**। গেটওয়ে domain দেখে বুঝে এটা
কোন org, তারপর downstream-এ পাঠায়।

### turf.com থেকে একটা booking request-এর পুরো trace
turf.com browser hit করে: `POST https://api.tecgensoft.com/api/v1/public/turf/courts/123/bookings`

1. **Path check** — `/api/v1/public/turf/**` → `ApiRoute.PUBLIC_TURF`, `tenantSite=true` ✅
2. **Caller কে?** — `Origin: https://turf.com` আসে →
   - platformOrigin না (ওটা admin.tecgensoft.com)
   - `siteHost("https://turf.com")` → `"turf.com"` → `find("turf.com")` → **Core-এ জিজ্ঞেস করে** এই domain কোন org
   - Core বলে org-X → `Caller.Site("turf.com")` ✅
3. **Site route?** — route tenantSite হ্যাঁ, তাই Site origin allowed। (Admin route হলে এখানে **403**)
4. **CORS + rate limit** — origin echo করে allow; booking bucket `booking|clientIP|turf.com` (10/min)
5. **Tenant host** — Site caller, তাই `tenantHost = turf.com` (origin নিজেই, client override করতে পারে না)
6. **Forward** — Turf-এ পাঠায় `X-Tenant-Host: turf.com` + `X-Request-Id`

Turf তখন জানে এটা org-X-এর booking, সেই org-এর court-এ লেখে।
**abc.com, hometurf.com** — একই flow, শুধু domain আলাদা → org আলাদা।

### এটা কাজ করার জন্য যা লাগবে
1. **প্রতিটা domain Core-এ register থাকতে হবে** — গেটওয়ে hardcode করে না। `turf.com → org-X`,
   `abc.com → org-Y` mapping Core-এর tenant directory-তে থাকবে। গেটওয়ে প্রতি নতুন domain-এর জন্য
   Core-কে জিজ্ঞেস করে, উত্তর 60s cache করে।
2. **⚠ ঐ Core endpoint এখনো নাই** — `/internal/v1/tenants/resolve` Core source-এ implement নাই
   (handoff follow-up #1)। **এই মুহূর্তে** turf.com থেকে hit করলে → **503 / "Organization not found"**।
   পুরো public flow চালু করতে হলে **সবার আগে এই endpoint Core-এ বানাতে হবে।**
3. **Public API শুধু এই ২টা route** — Site domain শুধু tenantSite route hit করতে পারবে:
   `/api/v1/public/tenant` (Core) + `/api/v1/public/turf/**` (Turf — booking, availability)।
   বাকি সব থেকে Site origin → **403**। শুধু platform panel (admin.tecgensoft.com) পারবে।
4. **HTTPS লাগবে** — Site origin অবশ্যই `https://` (line 177)। Local dev-এ `allow-insecure-origins=true`।
5. **Mobile app (no Origin)** — Origin header থাকে না → `Caller.NoOrigin`। তখন app-কে নিজে
   `X-Tenant-Host: turf.com` header পাঠাতে হবে (line 188)। গেটওয়ে সেটাও Core-এ verify করে।

---

## ৭. সারসংক্ষেপ

গেটওয়ে একটা **ভালো বানানো edge proxy** — route, rate-limit, size-limit, request-id, error, log,
tenant-resolve সব clean, security tight (allowlist + anti-spoof + trusted-proxy IP)। **Match ≈৭০%।**

বাকি ৩০%:
- গেটওয়ে-level token auth + org binding
- gRPC internal transport
- TLS termination (infra)
- Core-এ `/internal/v1/tenants/resolve` endpoint add করা (**এটা না হলে multi-domain flow এখন চলবে না**)
