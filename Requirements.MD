# Polygraphic Centre - Requirements and Development Guide

## 1. Project Overview

Polygraphic Centre is a web-based client-server system for managing print orders, files, pickup times, pricing, notifications, and staff workflows for one or more print shops.

The project is intended to replace manual and fragmented order intake methods such as e-mail and physical media. The system uses a React frontend and a Spring Boot backend, with PostgreSQL for persistent data, Google Cloud Storage for file storage, Firebase Authentication for identity, and Google Pub/Sub for asynchronous event handling.

## 2. Goals

- Provide a simple and reliable ordering flow for clients.
- Give print shop staff a fast operational dashboard.
- Allow shop managers to configure prices, opening hours, and capacity limits.
- Allow system administrators to manage shops, users, and global operations.
- Keep file uploads and notifications resilient by using cloud storage and asynchronous messaging.
- Make the system testable, deployable, and maintainable by a small team.

## 3. Roles and Access Model

The requirements documents use different labels for similar roles. For implementation, the project should standardize the following roles:

- Guest: can browse print shops and estimate prices, but cannot place an order.
- Client: can register, log in, upload files, place orders, track orders, and cancel orders before processing starts.
- Worker / Operator: can view assigned orders, download files, update order status, and mark problematic orders.
- Manager: can manage pricing, opening hours, print options, and capacity rules for a print shop.
- Administrator: can manage print shops, staff accounts, and global system configuration.

Access rules:

- Clients can only access their own orders and files.
- Privileged staff can access operational data only within their authorized scope.
- Administrative functions require explicit authorization.

## 4. Functional Requirements

### 4.1 Identity and Access

- Registration and login must use Firebase Authentication.
- JWT token claims must be used to determine user role and permissions.
- Unauthorized access must be blocked and handled with clear error responses.
- Ownership checks must prevent access to orders that do not belong to the current user.

### 4.2 Guest Functions

- Guests can view available print shops and locations without logging in.
- Guests can estimate print costs before creating an account.
- Guests must be redirected to login or registration when they try to finalize an order.

### 4.3 Client Functions

- Clients can upload supported document formats to cloud storage.
- Clients can choose print parameters such as color, format, number of copies, and page range.
- Clients can select a pickup time from available slots.
- Clients can see the order cost before confirmation.
- Clients can view order history.
- Clients can modify or cancel an order until the shop accepts it.
- Clients must be informed when an order becomes ready for pickup.

### 4.4 Worker Functions

- Workers can view active orders sorted by pickup time.
- Workers can download uploaded source files.
- Workers can change order status to in progress, ready, or collected.
- Workers can mark a damaged file or problematic order and trigger a client notification.

### 4.5 Manager Functions

- Managers can edit pricing and opening hours for their shop.
- Managers can define capacity limits for time slots.
- Managers can configure available print types and pickup availability.

### 4.6 Administrator Functions

- Administrators can add print shops to the system.
- Administrators can manage staff accounts and permissions.
- Administrators can generate daily and monthly reports.
- Administrators can monitor platform health and operational integrity.

### 4.7 Notifications and Events

- The system must notify the print shop when a file has been uploaded.
- The system must notify the client when order status changes.
- The system must notify the client when the order is ready for pickup.
- Messaging must continue to work even if the e-mail provider is temporarily unavailable.

## 5. Nonfunctional Requirements

### 5.1 Performance and Scalability

- API CRUD responses should usually stay under 500 ms for 95 percent of cases, excluding file transfer time.
- Large file uploads must not block the main backend request path.
- The system should scale through cloud services and asynchronous processing.

### 5.2 Reliability

- Financial or balance-related operations must remain transactional.
- If e-mail delivery fails, the order flow must continue and messages must be retried.
- The system must not lose order data if a downstream service is temporarily unavailable.

### 5.3 Security

- All state-changing operations must require a valid JWT.
- Role claims must be enforced server-side.
- File access must be limited to authorized users only.

## 6. Target Architecture

The project should follow this high-level structure:

- Frontend: React SPA.
- Backend: Spring Boot REST API.
- Database: PostgreSQL.
- File storage: Google Cloud Storage.
- Messaging: Google Pub/Sub.
- Authentication: Firebase Authentication.
- Notification processing: separate worker or lightweight service.

The preferred file upload flow is direct-to-cloud storage using signed upload URLs so the backend does not become the bottleneck.

## 7. Implementation Roadmap

This roadmap is organized so each phase can be split further into subtasks.

### Phase 1: Foundations

- Normalize the business vocabulary and canonical role model.
- Define the order lifecycle and main business rules.
- Define the API style and error response format.
- Establish project conventions for naming, package layout, and configuration.
- Prepare environment documentation for the whole team.

### Phase 2: Backend Foundation

- Add persistence and database integration.
- Add core entities, repositories, and service layer structure.
- Add validation and global error handling.
- Add security setup and token verification.
- Add OpenAPI or similar API documentation.

### Phase 3: Frontend Foundation

- Replace the starter app with the real application shell.
- Add routing and page structure.
- Add authentication state handling.
- Add a reusable API client layer.
- Add form patterns and validation.

### Phase 4: Core Client Flow

- Add print shop browsing.
- Add order creation and cost estimation.
- Add supported file upload flow.
- Add order history and order update/cancel rules.

### Phase 5: Staff Operations

- Add order queue views.
- Add sorting and filtering by pickup time.
- Add order detail and file download support.
- Add status updates and problem handling.

### Phase 6: Configuration and Administration

- Add shop configuration screens.
- Add pricing, capacity, and availability management.
- Add staff and shop administration.
- Add report generation and export.

### Phase 7: Messaging and Notifications

- Add event publishing for important order changes.
- Add asynchronous e-mail notifications.
- Add retry and dead-letter behavior.

### Phase 8: Quality and Release Readiness

- Add unit, integration, and end-to-end tests.
- Add linting and formatting checks.
- Add deployment configuration and release checklists.
- Add backup, monitoring, and recovery guidance.

## 8. Team Work Breakdown

The project has five contributors, so the work should be divided into five streams that can be further split when needed.

1. Backend platform and data layer.
   - Database schema.
   - Entities, repositories, services.
   - Validation and business rules.

2. Authentication and security.
   - Firebase integration.
   - JWT validation.
   - Role and ownership checks.

3. Client-facing frontend.
   - Public browsing.
   - Client order flow.
   - File upload and order history screens.

4. Staff and admin frontend.
   - Queue dashboard.
   - Order detail views.
   - Manager and administrator configuration views.

5. Integrations, notifications, and quality.
   - Cloud storage integration.
   - Pub/Sub and e-mail worker.
   - Testing, CI, deployment notes, and environment setup.

## 9. Development Guidelines

### 9.1 Working Principles

- Build the simplest version that satisfies the requirement first.
- Keep frontend and backend contracts aligned.
- Prefer small vertical slices over isolated technical work when possible.
- Do not implement features before the data model and access rules are clear.
- Use shared terminology in code, docs, tests, and UI text.

### 9.2 Definition of Done

A task is complete only when:

- the feature works end to end,
- errors are handled,
- tests are added or updated,
- documentation is updated if needed,
- the feature is verified locally.

### 9.3 What Should Be Standardized Early

- Role names.
- Order statuses.
- File type and size limits.
- API paths and response shapes.
- Date and time handling.
- Configuration keys for local and cloud environments.

## 10. Local Development Setup

This section is meant for contributors who may have no prior programming experience.

### 10.1 What to Install

- Git.
- Java 25 for the backend.
- Node.js 18 or newer for the frontend.
- VS Code.
- A terminal application.
- Optional: Docker Desktop or Docker Engine if the team wants a shared local database later.

### 10.1.1 Local PostgreSQL via Docker (Team Standard)

For consistency across machines, local PostgreSQL should run in Docker using a simple script-based flow.

Minimum expected commands:

- Start local database container.
- Stop and remove local database container.
- Create a database dump file.
- Restore database state from a dump file.

The script(s) should be versioned in the repository so every contributor uses the same database image, port, credentials, and database name.

### 10.2 First-Time Setup Steps

1. Clone the repository.
2. Open the project in VS Code.
3. Open two terminals: one for backend, one for frontend.
4. Install frontend dependencies.
5. Verify Java and Node versions.
6. Create local environment files for backend and frontend.
7. Start the backend using the run instructions in [backend/README.md](backend/README.md).
8. Start the frontend using the run instructions in [frontend/README.md](frontend/README.md).
9. Confirm both services open in the browser without errors.

### 10.3 Environment Variables To Expect

- Backend database connection values.
- Firebase project and credential values.
- Google Cloud Storage bucket or upload configuration.
- Pub/Sub topic and subscription names.
- E-mail provider settings.
- Frontend backend URL.

### 10.3.1 Database Backup and Restore Requirement

The local and non-production environments must support state export and import.

- Export: ability to create a SQL dump from PostgreSQL.
- Import: ability to restore the dump into another PostgreSQL instance.
- Dumps should be shareable within the team for reproducible testing and debugging.

### 10.4 Beginner-Friendly Rules

- If a command fails, read the error before trying random fixes.
- Change one thing at a time.
- Keep the terminal output when asking for help.
- Do not edit source code just to make a build pass without understanding why it failed.

## 11. Testing Strategy

### 11.1 Backend Tests

- Unit tests for pricing, validation, and status transitions.
- Repository tests for database interactions.
- Security tests for role and ownership checks.
- Integration tests for order creation, file metadata, and reporting.

### 11.2 Frontend Tests

- Component tests for forms and views.
- Routing tests for role-based navigation.
- API integration tests for main flows where practical.

### 11.3 End-to-End Scenarios

- Guest browsing and estimation.
- Client registration, file upload, and order submission.
- Staff review, status updates, and file download.
- Manager configuration changes.
- Administrator account and shop management.

## 12. Deployment and Operations

### 12.1 Required Deployment Pieces

- Backend service.
- Frontend build deployment.
- PostgreSQL database.
- Google Cloud Storage bucket.
- Pub/Sub configuration.
- E-mail provider integration.

### 12.2 Operational Concerns

- Environment-specific configuration.
- Secrets handling.
- Backup and recovery.
- Logging and monitoring.
- Retry and dead-letter handling for async messages.

### 12.3 Release Checklist

- Local tests pass.
- Build succeeds.
- Environment variables are defined.
- Cloud resources are reachable.
- Main user flows are smoke-tested.

## 13. Initial Constraints and Assumptions

- The current repository is still a skeleton and most features must be built from scratch.
- The architecture should remain cloud-friendly and event-driven.
- The first priority should be establishing clear foundations rather than polishing UI or adding advanced features too early.
- Docker should be optional for onboarding, not a blocker for new contributors.

Note: for this project, local PostgreSQL in Docker is adopted as the team standard for consistency, while application runtime remains local (backend/frontend started from host machine).

## 14. Suggested First Tasks

1. Normalize the project terminology and scope.
2. Add the backend and frontend environment instructions in more detail.
3. Define the initial data model and order lifecycle.
4. Set up backend security and API contracts.
5. Replace the starter frontend with the real application shell.

## 15. Open Questions

The following decisions are now fixed for the current implementation scope:

- Local database: use PostgreSQL in Docker with repository scripts for start, stop, dump, and restore.
- Authorization model: use a hybrid approach.
  - Firebase handles authentication identity and token issuance.
  - Minimal custom claims are allowed for coarse-grained role hints.
  - Backend database remains the source of truth for role scope and authorization details.
  - Backend enforces permissions server-side on every protected operation.
- Reports: PDF is mandatory for the first release; CSV is optional and may be added later.
- Release scope: first release includes manager and administrator panels, even if some views are stubbed or placeholder-only initially.
