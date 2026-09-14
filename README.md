# StayLanka
Java 21 · Spring Boot 4.1 · MVC · Security · JPA · Thymeleaf · MySQL · Flyway

Updated from the supplied source ZIP using the proposal and the pasted implementation prompt.
The existing modular monolith and working guest experience are preserved.

## Verification status — read first
This delivery has NOT been certified production-ready. Source checks and JavaScript syntax checks
passed here, but Java compilation, integration tests, real MySQL migrations, application startup,
and browser testing could not be executed to completion. The available runtime is Java 17 (not
a Java 21 JDK), and Maven Central fails DNS resolution. The attempted Maven build failed while
resolving the Spring Boot 4.1.0 parent POM, before compilation. No test pass count is claimed.
Run the commands below on Java 21 with Maven Central access before using real guest data.

See REQUIREMENTS-CHECKLIST.md for proposal mapping and acceptance gates.

## Prerequisites and database
- JDK 21; check `java -version` and `javac -version`.
- MySQL 8 with an isolated database and an application account.
- Internet for the first Maven Wrapper dependency download.
- Existing frontend Bootstrap and icons use pinned CDN URLs; their availability must be checked
  for your deployment, or vendor those assets locally before an offline demo.

Create the database through your MySQL administration tool:
```sql
CREATE DATABASE staylanka CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
Create a dedicated application user with a password supplied privately. Give the migration
account DDL privileges on this database; use a least-privilege runtime account in production.

Set environment variables in your shell or IDE; .env.example is documentation, not an auto-loaded file:
```bash
export DB_URL='jdbc:mysql://localhost:3306/staylanka?serverTimezone=Asia/Colombo'
export DB_USERNAME='staylanka'
export DB_PASSWORD='<your-local-database-password>'
export UPLOAD_DIR='./uploads'
```
Use TLS-enabled MySQL connectivity in production. Do not use the local-development URL's
SSL/public-key shortcuts on an untrusted network. Keep database passwords outside Git.

## Start and build
```bash
chmod +x mvnw
./mvnw clean test
./mvnw clean verify
./mvnw spring-boot:run
```
Windows: use `mvnw.cmd`. After packaging:
```bash
java -jar target/staylanka-0.0.1-SNAPSHOT.jar
```
Open [StayLanka locally](http://localhost:8080).
Flyway applies V1 through V6; Hibernate uses `ddl-auto: validate`, never create/drop.
Do not modify already-applied migrations or enable destructive schema recreation.

## Initial administrator and university demo
The default profile inserts no accounts. For an EMPTY LOCAL development database, explicitly
supply `DEV_ADMIN_PASSWORD` and `DEV_STAFF_PASSWORD`, then run:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
Administrator username defaults to `admin@staylanka.lk` (`DEV_ADMIN_EMAIL` overrides it).
The six module usernames are the student IDs below. Their initial local password comes from
`DEV_STAFF_PASSWORD`; there are no built-in passwords. Existing accounts are never overwritten.
Never use the dev profile or a shared module password for a real deployment. Provision distinct
staff accounts via the administrator, then use the normal profile. The seed rooms and offer are
sample development data, not real hotel inventory.

| Owner | Username | Role / demo |
|---|---|---|
| Abeyrathna A.H.M.P.M. | IT25103762 | ROOM_MANAGER: room/type creation, pricing, images, maintenance and availability |
| Wimukthi A.K.A. | IT25103763 | RESERVATION_MANAGER: confirm, amend and cancel guest bookings |
| Silva Y.H.S.D. | IT25103764 | PROFILE_MANAGER: walk-in creation, profile update, deactivation/anonymisation |
| Parindya R.K.M. | IT25103765 | STAY_MANAGER: identity verification, arrival, room move, extension, checkout/void |
| Gunaseela Y.P.S. | IT25103767 | PROMOTION_REVIEW_MANAGER: valid offers, completed-stay reviews and moderation |
| Kasthuriarachchi K.A.M.H.N. | IT25103768 | INQUIRY_REQUEST_MANAGER: assign, respond, progress, resolve, close/reopen/archive |

CUSTOMER owns their own profiles/bookings/stays/reviews/requests.
ADMIN manages all modules, staff, audit, recovery and purge.
Legacy STAFF has overview access but cannot mutate module-owned routes.
Cross-module managers have limited read access; edit endpoints and UI controls stay module-scoped.

## Six-member demonstration
1. Room manager adds an active room type and room, uploads an image, checks date filters and
   schedules a maintenance block. Confirm that those dates cannot be booked.
2. Register a customer. Book future dates, inspect the reference/price and inbox, amend the booking.
   Reservation manager confirms it; attempt an overlapping reservation and observe rejection.
3. Profile manager creates a walk-in profile. Customer updates preferences and communication consent.
4. For a reservation beginning today, stay manager verifies identity, checks in, moves to an available
   room, extends non-conflicting dates, adds charges and checks out.
5. Customer reviews the completed stay. Marketing moderates it and checks the rating summary.
6. Customer submits a request; customer relations assigns it, responds, starts, resolves, closes,
   reopens where permitted and archives a terminal request.
7. Demonstrate lifecycle retention and a separate ADMIN purge on disposable records. Inspect audit history.

## Lifecycle versus permanent delete
Normal actions retain database rows: room deactivation, reservation cancellation, profile
deactivation/anonymisation, erroneous-stay void, promotion deactivation, review hiding, and
request cancellation/archive. Guest review withdrawal now hides rather than physically deletes.

ADMIN uses Operations → Permanent deletion (`/admin/purge`). Select a module and type the exact
`DELETE EntityType ID` phrase. The action also presents:
“This permanently removes data from the database and cannot be undone.”

The purge service deletes the actual row transactionally and keeps a generic, FK-independent
HARD_DELETE audit event. Active records must first undergo their lifecycle action.
Critical dependent history causes refusal with an explanation. Eligible images' metadata,
maintenance blocks, promotion usages, stay charges and request response/history children are
removed in safe order for their corresponding purge. Room image bytes are deliberately not
automatically erased; include unreferenced-media review in the approved retention procedure.
Customer purge additionally removes its login, recovery and notification rows, only when
booking, request, review and authored operational history no longer depend on it.

## Integrity and security
- Half-open date ranges: arrival inclusive, departure exclusive.
- Room-level pessimistic locks serialize allocations, maintenance and room assignment.
- Terminal cancellations/rejections/no-shows/completed reservations release booking dates.
- Pending and confirmed bookings remain date conflicts. Physical occupancy is separate from
  future availability. Completed checkout returns operational room status to available.
- Room movement retains the contracted rate; extensions add nights at the original rate,
  preserving the original discount without discounting additional nights.
- Check-in requires a confirmed eligible reservation and explicit identity verification.
- A void is for an erroneous OPEN stay without charges; it cancels the associated reservation
  and retains the stay. Create a new booking for a corrected arrival.
- Only completed, non-void stays can produce reviews; a stay has at most one review.
- BCrypt passwords, CSRF-protected POST mutations, validated form DTOs and customer ownership
  checks. Password reset, account deactivation or role changes invalidate existing real login
  sessions on their next request.
- Audit summaries intentionally omit credentials, identity documents and free-text guest notes.
- Uploads retain existing JPEG/PNG/WebP signature checks and configured size limits.

## Open proposal policies and conservative defaults
- Guest amendments are allowed for PENDING/CONFIRMED bookings before the arrival date.
  Cancellations are allowed until physical check-in; there is no fee/payment implementation.
- No-show action is disabled until approved. Enable with
  `STAYLANKA_POLICIES_NO_SHOW_ENABLED=true`; only authorized staff can apply it on/after arrival.
- No automatic profile erasure. Manual anonymisation clears profile identity/contact/preferences
  and disables login. It does NOT scrub guest text from historical requests, booking notes or
  retained audits. Approve a wider retention policy before treating that history as anonymous.
- Marketing managers approve/hide reviews; only ADMIN physically purges.
- Notifications are persisted IN-APP. No email or SMS sending is claimed.
  Arrival reminders default to one day before arrival, checked hourly:
  `STAYLANKA_REMINDERS_ENABLED`, `STAYLANKA_REMINDERS_DAYS_BEFORE`,
  `STAYLANKA_REMINDERS_INTERVAL_MS`. Unique reminder keys prevent duplicate delivery.
- Unassigned requests belong to the Customer Relations queue until a named staff owner is
  assigned. Processing requires an assignee. Closed requests may be reopened by authorized
  request managers; archived requests are final.
- Password recovery uses administrator-assisted, independently verified token delivery.
  The public request response does not disclose account existence. Admin issues a random
  single-use token shown once; only its SHA-256 hash is stored, with 30-minute expiry.
  Guest enters the token at /reset-password. No automatic email is sent.
- Online payments, paid notification vendors, exceptional rate overrides, cancellation fees,
  automatic no-show jobs and automatic retention deletion are not enabled.

## Backups and recovery
Back up MySQL AND UPLOAD_DIR together; restrict and encrypt backups. Never pass a database
password on a command line. Example using a configured MySQL login path:
```bash
mysqldump --login-path=staylanka --single-transaction --routines --triggers staylanka > staylanka-backup.sql
```
Test restoration into a separate database, restore matching media, set DB_URL to that database,
and verify Flyway checksums, row counts and a complete booking journey. Never restore over live
data without an approved recovery window. Define retention, backup frequency and recovery
targets with stakeholders; the proposal does not establish numeric targets.

## Tests and local structural checks
`ModuleLifecycleIntegrationTests` covers all major entity CRUD/lifecycle/purge operations,
flush/clear/existsById deletion assertions, dependency refusal, identity checks, maintenance
and recovery tokens. Existing workflow and RBAC tests are retained and updated.
`NewRoutesIntegrationTests` checks new pages, purge roles/CSRF and cross-module edit boundaries.

```bash
node scripts/check-source.mjs
node --check src/main/resources/static/js/app.js
./mvnw clean verify
```
The source checker catches unbalanced Java delimiters, duplicate GET/POST mappings and
missing fragment files. It is NOT a Java compiler or a Thymeleaf renderer.

## Release gates still required
On an approved Java 21 environment: pass Maven verify; run migrations against clean and
existing MySQL databases; exercise concurrent booking transactions; inspect all populated
Thymeleaf pages; test 360px/tablet/desktop layouts, keyboard/focus/contrast and browser console;
verify backup restoration and recovery token delivery policy. No production-ready claim
should be made before these gates pass.
