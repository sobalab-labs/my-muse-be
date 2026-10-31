# Plan: Migrate Notification Preferences from PUT to PATCH

## Goal

Let the UI toggle individual notification settings without sending the whole object, avoiding lost updates between tabs/devices and breakage when new settings are added.

## Decisions

- **Method:** `PATCH` with JSON Merge Patch (RFC 7396), `Content-Type: application/merge-patch+json`.
- **Scope:** Global per-user toggles (one row per user). No per-artist preferences.
- **Request DTO:** Nested (`channels` / `topics`) with all fields nullable. `null` or absent means "unchanged".
- **Storage model:** Flat, matching the existing record.
- **Removed:** `artistSubscriptions` list and the `discography` toggle (not in the current model).

## 1. Data model

One table, `notification_preferences`, one row per user.

| Column | Type | Default |
|---|---|---|
| `id` | UUID (PK) | |
| `user_id` | UUID (FK -> users.id, **UNIQUE**) | |
| `channel_email_enabled` | boolean | false |
| `channel_kakao_enabled` | boolean | false |
| `general_weekly_updates` | boolean | false |
| `artist_new_concert_enabled` | boolean | false |
| `bookmarked_event_ticket_open_enabled` | boolean | false |
| `created_at`, `updated_at` | timestamp | |

Notes:

- Add `UNIQUE(user_id)` to prevent duplicate rows.
- Rows are created lazily: no row means all false, and the first PATCH upserts.
- Add `@DynamicUpdate` on the JPA entity so concurrent PATCHes on different toggles write only their own columns.

## 2. Stored/response model (existing)

```java
record NotificationPreferencesDto(
        boolean channelEmailEnabled,
        boolean channelKakaoEnabled,
        boolean generalWeeklyUpdates,
        boolean artistNewConcertEnabled,
        boolean bookmarkedEventTicketOpenEnabled
) {}
```

Primitives are correct here: a stored value is always true or false. Because it is a record (immutable), the merge returns a **new** instance instead of mutating.

## 3. Patch request DTO (new)

```java
public record NotificationPreferencesPatchDto(
        @Valid ChannelsPatch channels,
        @Valid TopicsPatch topics
) {
    public record ChannelsPatch(Boolean email, Boolean kakao) {}

    public record TopicsPatch(
            Boolean generalWeeklyUpdates,
            Boolean artistNewConcert,
            Boolean bookmarkedEventTicketOpen
    ) {}

    @JsonIgnore
    public boolean isEmpty() {
        boolean noChannels = channels == null
                || (channels.email() == null && channels.kakao() == null);
        boolean noTopics = topics == null
                || (topics.generalWeeklyUpdates() == null
                    && topics.artistNewConcert() == null
                    && topics.bookmarkedEventTicketOpen() == null);
        return noChannels && noTopics;
    }
}
```

Rules:

- Wrapper `Boolean` everywhere. Primitives would turn a missing key into `false`.
- No `@NotNull` on any field.
- `@Valid` on nested records so validation cascades if constraints are added later.

Example request:

```http
PATCH /users/me/notification-preferences
Content-Type: application/merge-patch+json

{ "topics": { "bookmarkedEventTicketOpen": false } }
```

## 4. Merge logic

```java
static NotificationPreferencesDto merge(NotificationPreferencesDto cur,
                                         NotificationPreferencesPatchDto p) {
    var c = p.channels();
    var t = p.topics();
    return new NotificationPreferencesDto(
            c != null && c.email() != null ? c.email() : cur.channelEmailEnabled(),
            c != null && c.kakao() != null ? c.kakao() : cur.channelKakaoEnabled(),
            t != null && t.generalWeeklyUpdates() != null ? t.generalWeeklyUpdates() : cur.generalWeeklyUpdates(),
            t != null && t.artistNewConcert() != null ? t.artistNewConcert() : cur.artistNewConcertEnabled(),
            t != null && t.bookmarkedEventTicketOpen() != null ? t.bookmarkedEventTicketOpen() : cur.bookmarkedEventTicketOpenEnabled()
    );
}
```

Service flow (`@Transactional`):

1. Load by `user_id`, or build defaults (all false) if no row exists.
2. `merge(current, patch)`.
3. Run business-rule validation on the **merged** result.
4. Save and return the full updated resource.

## 5. Controller

```java
@PatchMapping(
        path = "/users/me/notification-preferences",
        consumes = "application/merge-patch+json",
        produces = MediaType.APPLICATION_JSON_VALUE)
public NotificationPreferencesDto patch(
        @AuthenticationPrincipal UserPrincipal user,
        @Valid @RequestBody NotificationPreferencesPatchDto body) {
    if (body.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Patch must contain at least one field");
    }
    return service.patch(user.id(), body);
}
```

Return `200` with the full updated resource so the UI can sync without a second GET.

## 6. Validation

| Concern | Where |
|---|---|
| Unknown or misspelled keys | Jackson `fail-on-unknown-properties: true` (off by default in Spring Boot) |
| Wrong types (e.g. `"true"` as a string) | Jackson, returns 400 |
| Empty patch (`{}`) | Controller `isEmpty()` check |
| Cross-field business rules | Service, after merge, against the resulting state |

Config:

```yaml
spring:
  jackson:
    deserialization:
      fail-on-unknown-properties: true
```

## 7. OpenAPI changes

- Rename path to `/users/me/notification-preferences`.
- Replace `put` with `patch`; request content type `application/merge-patch+json`.
- Add `NotificationPreferencesPatch` schema: all properties optional, `additionalProperties: false`.
- Keep `get` unchanged; `patch` returns `200` with the full resource.
- Remove `put` after clients migrate (or mark it `deprecated: true` first).

## 8. Rollout steps

1. Add migration: rename/create table, add `UNIQUE(user_id)`, drop unused columns (`discography`, per-artist data).
2. Implement patch DTO, merge function, service method, controller endpoint.
3. Enable `fail-on-unknown-properties`. Check other endpoints for typos it would now reject.
4. Update OpenAPI spec and regenerate clients.
5. Switch the UI to send single-toggle PATCH requests.
6. Deprecate and then remove `PUT`.

## 9. Tests

- Single toggle changes only that field; all others are untouched.
- `{}` returns 400.
- Unknown key returns 400.
- Wrong type returns 400.
- First PATCH with no existing row creates a row with defaults plus the change.
- Two concurrent PATCHes on different fields both persist.
- Nested partial: `{ "channels": { "email": true } }` leaves `kakao` untouched.

## Open items

- **Consent audit:** If any toggle covers marketing messages under Korean law (정보통신망법 광고성 정보 수신동의), add a consent log recording when and how consent was given or withdrawn.
- **Optimistic concurrency:** Add `ETag` / `If-Match` only if conflicting edits matter in practice.
- **Future "reset" fields:** If a nullable field is added where `null` means clear, use `JsonNullable<T>` for that field only.