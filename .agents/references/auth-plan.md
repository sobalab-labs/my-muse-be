# Auth Layer Plan (Web App)

Snapshot of final decisions for implementing authentication and authorization across the Next.js frontend and Spring Boot API.

Target stack: **Spring Boot 4.0.x** (Spring Framework 7, Spring Security 7, Spring Session 4, Jackson 3), Java 21.

## 1. Scope

- Clients: Next.js web app only.
- Login: Google or Kakao only (OAuth2 Authorization Code + PKCE, performed by Spring). No password or other login methods.
- Session: server-side session, HttpOnly cookie, stored in Redis.
- Spring Boot API runs on AWS Lambda via Lambda Web Adapter. Cost minimization is a goal: no VPC attachment, no always-on infra.

## 2. Decisions

| Area | Decision |
|---|---|
| Login owner | Spring Boot (`oauth2Login`), BFF style. Next.js has no auth library |
| Browser credential | `HttpOnly; Secure; SameSite=Lax` session cookie. No JWT, no tokens in the browser |
| Resource server | Not used for user traffic |
| Session store | Redis via Spring Session, hosted on Upstash (serverless, outside VPC) |
| Topology | Same origin: one public host routes `/api/**`, `/oauth2/**`, `/login/**` to Spring and everything else to Next.js. No CORS |
| User identity key | `(provider, provider_subject)`. Never email |
| Account linking | No auto-merge by email |
| Authorization | Method security (`@PreAuthorize`) on top of authenticated session |
| CSRF | Enabled, SPA mode (`csrf.spa()`) |

## 3. Architecture

```
Browser ──► app.example.com
              ├─ /*                         → Next.js
              └─ /api/**, /oauth2/**,
                 /login/**                  → Spring Boot (Lambda)

Spring Boot ──► Google / Kakao   (OAuth2 code flow, server-side)
Spring Boot ──► Postgres         (users, user_identities)
Spring Boot ──► Upstash Redis    (sessions, over TLS, no VPC)
```

## 4. Login flow

1. Next.js login buttons are plain links: `/oauth2/authorization/google` and `/oauth2/authorization/kakao`.
2. Spring redirects to the provider, handles the callback at `/login/oauth2/code/{registrationId}`, and exchanges the code.
3. Custom user services resolve or create the app user (see section 7).
4. Session ID is rotated on login (Spring default). Session cookie is set.
5. Success handler redirects to a front-end URL validated against an allow-list.
6. Failure handler redirects to `/login?error=<code>`.
7. Next.js Server Components call `GET /api/me`, forwarding the incoming `Cookie` header. 401 means not logged in.
8. Logout: `POST /api/logout` returns 204 and invalidates the session.

Register redirect URIs with each provider: `https://<app-host>/login/oauth2/code/google` and `https://<app-host>/login/oauth2/code/kakao`.

## 5. Spring Boot 4 stack

Boot 4 uses modular starters. Use the technology-specific starters below. Do not use the `*-classic` starters.

```
spring-boot-starter-webmvc                      # was spring-boot-starter-web
spring-boot-starter-security
spring-boot-starter-security-oauth2-client      # was spring-boot-starter-oauth2-client
spring-boot-starter-session-data-redis          # Spring Session + Redis (replaces spring-session-data-redis as a direct dependency)
spring-boot-starter-data-redis
spring-boot-starter-data-jpa
spring-boot-starter-flyway
postgresql (driver)
spring-boot-starter-validation
spring-boot-starter-actuator
```

Test: add the matching `*-test` starters (e.g. `spring-boot-starter-security-test`, `spring-boot-starter-webmvc-test`).

Do not add `spring-boot-starter-security-oauth2-resource-server` or the authorization server starter for user traffic.

### Boot 4 / Security 7 rules the implementation must follow

- Lambda DSL only. `and()` chaining and `authorizeRequests()` are removed; use `authorizeHttpRequests`.
- `MvcRequestMatcher` and `AntPathRequestMatcher` are removed. `requestMatchers("/path/**")` uses path patterns automatically; for explicit matchers use `PathPatternRequestMatcher.withDefaults().matcher("/api/**")`.
- SPA CSRF is built in: `http.csrf(csrf -> csrf.spa())`. Do not hand-write a `CsrfTokenRequestHandler` unless `spa()` does not meet the need (verify it issues the readable `XSRF-TOKEN` cookie; otherwise configure `CookieCsrfTokenRepository.withHttpOnlyFalse()` explicitly).
- Jackson 3 is the default. Any JSON session serialization must use the Jackson 3 based serializer with Spring Security's Jackson 3 modules, not the Jackson 2 classes. If this proves problematic, use the default JDK serialization.
- Spring Retry and other previously separate utilities may have moved into core; do not add old dependencies by habit.
- Redirects to login are relative URIs in Security 7.

## 6. Beans and responsibilities

| Bean | Responsibility |
|---|---|
| `SecurityFilterChain` | `oauth2Login`, `/api/**` requires authentication, `/actuator/health`, `/oauth2/**`, `/login/**` public, logout, CSRF SPA mode |
| `OidcUserService` (custom) | Google: read `sub`, `email`, `email_verified`, name. Find or create user and identity. Return principal carrying the app user id |
| `OAuth2UserService<OAuth2UserRequest, OAuth2User>` (custom) | Kakao: parse numeric `id` and nested `kakao_account` (email, `is_email_verified`, profile). Same mapping logic as Google |
| `AuthenticationSuccessHandler` | Redirect to allow-listed front-end URL only (no open redirect) |
| `AuthenticationFailureHandler` | Redirect to `/login?error=...` |
| `AuthenticationEntryPoint` | `HttpStatusEntryPoint(401)` for `/api/**` so APIs never return a 302 to the provider |
| `DefaultCookieSerializer` | `SameSite=Lax`, `Secure`, `HttpOnly`, explicit max age |
| Lettuce / `RedisConnectionFactory` customization | TLS (`rediss://`), connect and command timeouts, auto-reconnect and validation for frozen Lambda connections |
| Session `RedisSerializer` | JSON (Jackson 3) preferred over JDK serialization, if it works with Security 7 modules |
| `@EnableMethodSecurity` | Method-level authorization |

Filter chain skeleton:

```java
@Bean
SecurityFilterChain web(HttpSecurity http, AppOidcUserService oidc,
                        AppOAuth2UserService oauth2, SuccessHandler ok,
                        FailureHandler fail) throws Exception {
  return http
    .authorizeHttpRequests(a -> a
      .requestMatchers("/actuator/health", "/oauth2/**", "/login/**").permitAll()
      .anyRequest().authenticated())
    .oauth2Login(o -> o
      .userInfoEndpoint(u -> u.oidcUserService(oidc).userService(oauth2))
      .successHandler(ok)
      .failureHandler(fail))
    .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
        PathPatternRequestMatcher.withDefaults().matcher("/api/**")))
    .csrf(csrf -> csrf.spa())
    .logout(l -> l.logoutUrl("/api/logout")
                  .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
    .build();
}
```

## 7. Data model

```
users(id, email, email_verified, display_name, created_at, ...)
user_identities(id, user_id → users.id, provider, provider_subject,
                UNIQUE(provider, provider_subject))
notification_preferences(user_id, channel, enabled, consented_at)   -- per channel: email, kakao
```

Rules:
- `provider_subject` = Google `sub`, or Kakao numeric `id` as string.
- First login for an unknown `(provider, subject)` creates a new user and identity.
- Do not attach a new identity to an existing user based on matching email. Linking, if ever needed, is an explicit authenticated action.
- Kakao may omit email or return it unverified. Store `email_verified` and treat unverified or missing email as unusable for notifications.

## 8. Configuration

```yaml
server:
  forward-headers-strategy: framework   # correct https redirect URIs behind proxy / Lambda adapter

spring:
  data:
    redis:
      url: ${REDIS_URL}                 # rediss://default:<password>@<host>.upstash.io:6379
  session:
    timeout: 14d
    redis:
      repository-type: default          # TTL-based; no keyspace events, pub/sub, or cleanup job (verify property name in Boot 4)
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: [openid, profile, email]
          kakao:
            client-id: ${KAKAO_REST_API_KEY}
            client-secret: ${KAKAO_CLIENT_SECRET}
            client-authentication-method: client_secret_post
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope: [profile_nickname, account_email]
            client-name: Kakao
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id
```

All secrets come from a secret manager or environment, never from the repo.

## 9. Session store (Upstash Redis)

- Single Upstash database, TLS endpoint, in the region closest to the Lambda.
- No VPC or NAT required. Lambda stays outside any VPC.
- Use the default (non-indexed) session repository because Lambda cannot run keyspace listeners or scheduled cleanup. Redis TTL handles expiry. Consequence: no lookup of sessions by username.
- Do not store provider access or refresh tokens in the session.
- Pay-as-you-go with a monthly budget cap once beyond the free tier.
- Lettuce: set connect timeout and command timeout, enable auto-reconnect.
- Enable Lambda SnapStart (or equivalent cold-start mitigation) because login redirects are latency visible. Confirm the chosen Java runtime version is supported by SnapStart.

## 10. Next.js responsibilities

- Render login page with two links to `/oauth2/authorization/{google|kakao}`.
- Server Components and route handlers fetch `/api/me` with the incoming `Cookie` header forwarded. Do not cache these responses across users.
- Mutating requests from the browser send the CSRF header from the readable CSRF cookie (`XSRF-TOKEN` → `X-XSRF-TOKEN`).
- Middleware may only check for the presence of the session cookie to redirect early. Real authorization is enforced by Spring.
- No tokens in localStorage, sessionStorage, or JS-readable cookies.
- Routing of `/api/**`, `/oauth2/**`, `/login/**` to Spring is done at the reverse proxy / CDN (preferred) or via Next.js `rewrites()`.

## 11. Notification worker (Go) and auth

- The worker does not use user sessions and does not authenticate as a user.
- Recipient data needed for sending (email address, channel preferences) is carried in the event payload or read by the worker directly from the data store it is allowed to access. The worker has no calls to user-facing Spring endpoints.
- Email is sent from the product's own sender (e.g. SES or SendGrid) to the stored verified email. No Google user tokens or Gmail API scopes are needed.
- Worker must be idempotent (dedupe key per notification) and must honor per-channel consent from `notification_preferences`.
- Events are published via an outbox table so they are not lost between DB commit and delivery.

## 12. Security checklist

- [ ] Redirect after login only to allow-listed URLs.
- [ ] `/api/**` returns 401 JSON, never a redirect.
- [ ] Session cookie is `HttpOnly`, `Secure`, `SameSite=Lax`, with bounded lifetime.
- [ ] Session ID rotates on login.
- [ ] CSRF enforced on all non-GET `/api/**` requests.
- [ ] Identity lookup uses `(provider, provider_subject)` only.
- [ ] No provider tokens or secrets in logs, session, or browser.
- [ ] Redis connection uses TLS; credentials in secret manager.
- [ ] `forward-headers-strategy: framework` set so generated redirect URIs use the public https host.
- [ ] Logout invalidates the server-side session and clears cookies.
- [ ] No removed Security 6 APIs (`and()`, `authorizeRequests`, `AntPathRequestMatcher`, `MvcRequestMatcher`) in the codebase.

## 13. Open items (need a decision before the notification part is built)

1. **Kakao message route:**
   - (a) Kakao Biz Message (alimtalk) through a Kakao Channel and authorized reseller. Requires a phone number and approved templates. No login-token dependency, so login scopes stay `profile_nickname`, `account_email`.
   - (b) Kakao Talk Message API ("send to me"). Requires the `talk_message` scope at login and encrypted storage and refresh of Kakao tokens. Adds token storage to the auth layer.

   Until decided, the auth layer implements only the scopes in section 8.
2. **Upstash region:** confirm availability near the Lambda region and choose it.
3. **Public hostname and routing layer** (CDN / reverse proxy) that provides the same-origin path routing in section 3.

## 14. Acceptance criteria

- Visiting a protected page while logged out leads to the login page; clicking Google or Kakao completes login and returns to the app with a valid session cookie.
- `GET /api/me` returns the app user for a valid session, and 401 without one.
- A second login with the same provider account resolves to the same app user; the same email via a different provider creates a separate user.
- A state-changing `/api/**` request without a valid CSRF token is rejected (403).
- Logout returns 204 and the old session cookie no longer authenticates.
- Sessions survive Lambda cold starts and are shared across concurrent Lambda instances.
