# Requirement-to-code checklist
Baseline: supplied StayLanka proposal plus the implementation prompt. “Yes” below means
implementation is present in source, NOT runtime verification. All Java integration tests
remain unexecuted here because the Java 21/dependency prerequisites are unavailable.

## Six owners
| Module | Owner / ID |
|---|---|
| Room & availability | Abeyrathna A.H.M.P.M. / IT25103762 |
| Reservation | Wimukthi A.K.A. / IT25103763 |
| Customer profile | Silva Y.H.S.D. / IT25103764 |
| Check-in/out & stay | Parindya R.K.M. / IT25103765 |
| Promotion & review | Gunaseela Y.P.S. / IT25103767 |
| Inquiry & special request | Kasthuriarachchi K.A.M.H.N. / IT25103768 |

## All 18 proposal functional requirements
Paths below are beneath src/main/java/com/staylanka unless prefixed otherwise.
Tests: CW = CriticalWorkflowIntegrationTests; ML = ModuleLifecycleIntegrationTests;
ST = SecurityAndTemplateIntegrationTests; NR = NewRoutesIntegrationTests.

| # | Requirement | Implemented | Module / relevant files | Test | Notes |
|---|---|---|---|---|---|
| 1 | Date/type/price room search | Yes | room/RoomService, RoomRepository, RoomSearchForm; room/search.html | CW roomSearchExcludesOperationallyUnavailableAndOverlappingRooms; ML scheduledMaintenanceBlocksSearchAndReservation | Half-open ranges; capacity filter |
| 2 | Details, images, facilities and rates | Yes | room/RoomController, FileStorageService; room/detail.html | CW imageUploadRejectsSpoofedContentType | Existing image assets retained |
| 3 | Register, login, profile | Yes | auth/*, security/*, customer/* | ML profileCreateReadUpdateAnonymiseAndPurge; ST | BCrypt; guest ownership |
| 4 | Create/modify/cancel bookings | Yes | reservation/ReservationService, ReservationOperationsController | ML reservationCreateReadUpdateCancelAndPurge | Guest confirmed amendments before arrival; staff edits/cancellation |
| 5 | History and references | Yes | ReservationRepository, ReservationController; reservation/customer-list.html | CW; ML reservation test | Reference search via staff queue |
| 6 | Reviews and special requests | Yes | review/ReviewService, request/GuestRequestService | CW requestWorkflowRecordsAssignmentResponsesTransitionsAndOwnership; ML | Completed stays only for reviews |
| 7 | Rooms/types/prices/status | Yes | room/RoomService, MaintenanceService, AvailabilityController | CW room lifecycle/capacity tests; ML room/maintenance tests | Scheduled blocks + operational maintenance |
| 8 | Staff bookings and profiles | Yes | ReservationOperationsController, ProfileOperationsService | ML reservation/profile; NR | Walk-in profiles use verified recovery for login |
| 9 | Check-in/assignment/checkout | Yes | stay/StayService, StayOperationsController | CW completeGuestJourneyChecksInAddsChargesChecksOutAndAllowsOneReview; ML stay test | Identity verification recorded |
| 10 | Occupancy/arrivals/departures | Yes | common/ReportingService, DashboardController, StayService | NR renders pages; CW stay workflow | Database metrics; browser validation pending |
| 11 | Offers and moderation | Yes | promotion/PromotionService, review/ReviewService | CW percentageAndFixedPromotionStrategiesEnforceEligibilityAndZeroFloor; ML | Approved-review rating summary |
| 12 | Assign/resolve inquiries | Yes | request/GuestRequestService, RequestHistoryListener | CW request workflow; ML request test | Queue owns new unassigned items |
| 13 | Prevent overlapping reservations | Yes | RoomRepository pessimistic lock, ReservationRepository overlap query | CW overlap test | Concurrent MySQL execution remains an acceptance gate |
| 14 | Block maintenance bookings | Yes | MaintenanceService, RoomRepository, ReservationService | CW maintenance test; ML scheduled maintenance | Same rule applied to moves/extensions |
| 15 | Valid reservation for check-in | Yes | StayService, CheckInForm | ML checkInRequiresVerifiedIdentity; CW journey | Date/status/identity checks |
| 16 | Safe lifecycle retention | Yes | RoomService, ReservationService, ProfileOperationsService, StayService, PromotionService, ReviewService, GuestRequestService | ML all entity tests | No normal review hard delete |
| 17 | Key-change audit | Yes | common/AuditService; V4 migration; control/audit.html | ML auditExists after every purge | Generic table has no deleted-entity FK |
| 18 | Role-limited actions | Yes | security/SecurityConfig, AccountStatusFilter; template sec:authorize | ST, NR, ML moduleManagerCannotPurge | ADMIN-only purge service as well as route |

## Additional module and cross-cutting requirements
| Requirement | Implemented | Relevant files | Relevant test / verification | Notes |
|---|---|---|---|---|
| Separate hard database delete for every major entity | Yes | PurgeService, ControlController, control/purge.html | Seven ML CRUD tests flush/clear and assert existsById false | Typed confirmation; dependency refusal; generic audit |
| Room availability calendar | Yes | AvailabilityController; room/calendar.html | Structural checks only | 31 nightly dates; no private booking details |
| Scheduled maintenance windows | Yes | MaintenanceBlock, MaintenanceService; V6 | ML scheduledMaintenanceBlocksSearchAndReservation | Released blocks retain history |
| Profile preferences and consent | Yes | CustomerProfile/Form; profile templates; V4 | ML profile test | Optional marketing opt-in |
| Staff-created walk-in profile | Yes | WalkInForm, ProfileOperationsService | ML profile test | Random inaccessible initial password |
| Deactivation / anonymisation | Yes | CustomerService, ProfileOperationsService | ML profile test | Profile-only anonymisation; historical text retention explicitly documented |
| Secure password recovery | Yes | RecoveryService/Controller, AccountPrincipal; V4 | ML recoveryTokenIsHashedExpiringAndSingleUse | Manual verified delivery; no email/SMS claim |
| Room move, extension and void | Yes | StayService, StayOperationsController | ML stayCreateReadMoveExtendVoidAndPurge | Stable room lock order; original rate policy |
| Verified review editing and hiding | Yes | ReviewService | ML verifiedReviewCreateReadUpdateHideAndPurge | Non-void completed stay; approved edits return to moderation |
| Request assignment, priority, responses/history | Yes | GuestRequestService, RequestHistoryListener | CW request workflow | Updates/responses now in history |
| Close/reopen/archive | Yes | GuestRequestService, request/detail.html | CW request; ML request test | Archived records cannot progress |
| In-app confirmation/change/cancel/service notices | Yes | NotificationService, ControlController | Source review; new-route tests | Guest and responsible staff recipients |
| Upcoming arrival reminders | Yes | ReminderService; V5 | Source review only | Configurable; unique reminder key |
| Configurable no-show control | Yes | ReservationService | CW futureConfirmedReservationCannotBeMarkedNoShow | Disabled pending policy approval |
| No online payments | Yes | No payment integration introduced | Source review | Proposal decision remains open |
| Migrations and safe startup | Yes | V1–V6; application.yml | Existing Flyway-enabled H2 suite, NOT run here | Hibernate validate only |
| Secret-free configuration defaults | Yes | application*.yml, DevDataSeeder, .env.example | Source inspection | Git history omitted from delivery ZIP; rotate any previously exposed secrets |
| Premium shared UI and staff shell | Yes | fragments/layout, refinement.css, existing app.css | Structural/JS checks | Visual quality not certified without browser testing |
| Safe business/integrity errors | Yes | GlobalExceptionHandler, form BindingResult handling | CW and NR where applicable | No SQL/stack trace details in responses |
| Comprehensive runtime test pass | No | All test sources present | Maven blocked before compilation | Requires Java 21 and Maven Central |
| Verified responsive / WCAG 2.1 AA | No | Responsive CSS, focus/skip/reduced-motion controls present | Browser and accessibility audit not executed | Do not claim conformance |
| Validated MySQL migrations / recovery | No | Flyway migrations and README backup procedure | Not executed against MySQL here | Required release gate |
| Production deployment verification | No | Not requested or attempted | None | Deliverable is the updated source project |

## Non-functional requirements
| Requirement | Implementation / status | Acceptance still needed |
|---|---|---|
| Security and privacy | Ownership, roles, CSRF, BCrypt, input rules, active-session revocation and generic safe errors | Run role matrix, IDOR and upload adversarial tests |
| Reliability/recovery | Transactional operations, foreign keys, safe lifecycle actions, durable audit and backup instructions | Restore exercise and concurrent real-DB workflow tests |
| Usability/access | Date search, guest cards, module work queues, shared navigation and accessible-control improvements | Populated browser checks at 360px/tablet/desktop; keyboard/contrast review |
| Performance/quality | Indexed room/date queries, paginated primary queues, bounded calendar and audit pages | Agree numeric targets and measure with realistic data |
| Notifications/continuity | Persisted in-app updates and scheduled arrival reminders | Verify reminders in deployment; approve external channels if desired |

## Exact verification performed in this environment
- node scripts/check-source.mjs: PASS (structural source checks; not a compiler).
- node --check src/main/resources/static/js/app.js: PASS.
- git diff --check with CRLF handling: PASS.
- Maven clean test: FAILED before compilation while resolving Spring Boot parent POM.
  Error: repo.maven.apache.org: Temporary failure in name resolution.
- Available launcher: OpenJDK 17, no javac; project requires JDK 21.
- No claim is made that Java tests, Thymeleaf rendering, MySQL migrations, browser console
  checks, responsive layouts or production readiness passed.
