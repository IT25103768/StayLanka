# StayLanka hardening notes

This revision focuses on business-rule integrity and regression prevention rather than adding new features.

## Applied fixes

- Room status is no longer part of the normal room edit form.
- `OCCUPIED` can only be produced by the check-in workflow and cleared by check-out.
- Maintenance/deactivation is blocked when a room has active or upcoming reservations.
- Changing a booked room to a different room type is blocked until the blocking reservations are resolved.
- Room-type capacity cannot be reduced below the guest count of active/upcoming reservations.
- Future reservations cannot be marked `NO_SHOW`.
- Reservations whose checkout date has already passed cannot be confirmed.
- Actual check-in/check-out timestamps are revalidated in the service layer.
- A database check now enforces checkout-after-check-in for stays.
- Image uploads validate JPEG/PNG/WebP signatures instead of trusting the browser MIME type.
- Uploaded files are cleaned up when the surrounding database transaction rolls back.
- Room-list primary-image loading is batched instead of issuing one query per room.
- Browser date constraints and the application JVM use `Asia/Colombo` hotel time.
- Oversized uploads receive a controlled 413 error page.
- GitHub Actions CI runs the Maven verification lifecycle.
- Regression tests were added for room lifecycle, room-type capacity, future no-show, and spoofed image uploads.

## Verification command

Run from the project root with Java 21 and network access for Maven dependencies:

```bash
./mvnw clean verify
```

For the real deployment database, also start the application against a fresh MySQL 8 database so Flyway applies `V1` and `V2` from scratch.

## Admin development login fix

- Added `application-dev.yml` so the `dev` profile actually enables the existing development seeder.
- Fresh dev databases now get local-only default accounts unless environment variables override them:
  - `admin@staylanka.lk` / `Admin@12345`
  - `staff@staylanka.lk` / `Staff@12345`
- Added `scripts/dev-reset-admin.sql` as a development-only recovery script for an existing database whose admin password is unknown or stale.
