# apply-ledger

A small self-hosted tracker for my own job applications, as an alternative to a spreadsheet: which companies I
applied to, where each application stands, what I have to do next and when, and how the search is going overall.

It is a REST API (Java 21, Spring Boot 4, PostgreSQL). There is no web UI yet; use the Swagger UI, curl or any
HTTP client. Plans are in [ROADMAP.md](ROADMAP.md).

## What it does

- **Companies and applications**: role, link to the posting, location, on-site/hybrid/remote, where I found it,
  free-text notes.
- **Status workflow with history**: `WISHLIST → APPLIED → SCREENING → INTERVIEWING → OFFER → ACCEPTED`, plus
  `REJECTED`, `WITHDRAWN` and `DECLINED`. Only sensible moves are allowed (an application cannot jump from
  `APPLIED` to `OFFER` or come back from `REJECTED`), and every change is stored with its date and an optional note.
  Each response lists the statuses the application can move to next.
- **Follow-ups**: a planned next action with a date, plus a list of applications that went quiet (applied more than
  14 days ago, no answer, nothing planned).
- **Stats**: counts per status and a simple funnel for a date range: applied, got an answer, interviews, offers,
  response rate and median days until the first answer.
- **Search and export**: filter by status, company, open/closed or text; export the same selection as CSV.

It deliberately stores no contact people (recruiters, interviewers), only companies and my own notes.

## Run it

You need Docker. Copy the settings file and set a database password:

```bash
cp .env.example .env    # then edit DB_PASSWORD
```

**Everything in containers** (app on http://localhost:8080):

```bash
docker compose up -d --build
```

**Database in Docker, app from the IDE or Maven** (Java 21):

```bash
docker compose up -d postgres
DB_PASSWORD=... ./mvnw spring-boot:run
```

Add `SPRING_PROFILES_ACTIVE=demo` (in `.env`, or `--spring.profiles.active=demo`) to fill an empty database with a
few fictional companies and applications.

- API docs (Swagger UI): http://localhost:8080/swagger-ui/index.html
- Health: http://localhost:8080/actuator/health

The ports are bound to `127.0.0.1` only, because there is no login yet.

## Configuration

| Variable | Default | Meaning |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/applyledger` | JDBC URL |
| `DB_USER` | `applyledger` | database user |
| `DB_PASSWORD` | none, required | database password |
| `APP_ZONE` | `Europe/Berlin` | time zone that decides what "today" is |
| `APP_STALE_AFTER_DAYS` | `14` | when an unanswered application counts as stale |

## API overview

| Method and path | What it does |
|---|---|
| `POST /api/companies`, `GET /api/companies`, `GET/PUT/DELETE /api/companies/{id}` | companies; names are unique ignoring case; a company with applications cannot be deleted |
| `POST /api/applications` | new application, starts as `APPLIED` (default, date defaults to today) or `WISHLIST` |
| `GET /api/applications?status=&companyId=&open=&q=&page=&size=&sort=` | paged search |
| `GET/PUT/DELETE /api/applications/{id}` | read, edit fields (not the status), delete |
| `POST /api/applications/{id}/status` | move to another status: `{"status": "INTERVIEWING", "changedOn": "2026-11-12", "note": "..."}` |
| `GET /api/applications/{id}/history` | all status changes, oldest first |
| `GET /api/follow-ups?within=7` | due and overdue next actions, stale applications |
| `GET /api/stats?from=&to=` | status counts and funnel |
| `GET /api/applications/export.csv` | CSV export, same filters as the search |

Errors use RFC 9457 problem details; validation errors include a field-by-field `errors` map. Illegal status moves
and duplicate company names are `409 Conflict`.

Example:

```bash
curl -s -X POST localhost:8080/api/companies -H 'Content-Type: application/json' \
  -d '{"name": "Example Software GmbH", "website": "https://example.com"}'

curl -s -X POST localhost:8080/api/applications -H 'Content-Type: application/json' \
  -d '{"companyId": 1, "roleTitle": "Backend Developer", "workMode": "HYBRID", "nextAction": "Follow up", "nextActionOn": "2026-12-01"}'

curl -s -X POST localhost:8080/api/applications/1/status -H 'Content-Type: application/json' \
  -d '{"status": "SCREENING", "note": "Call with HR on Monday"}'
```

## Development

```bash
./mvnw verify
```

runs the unit tests and the API tests against a real PostgreSQL started by Testcontainers (Docker must be running).
The tests use a fixed clock, so date rules are deterministic. CI runs the same command and builds the image.

Code layout (`src/main/java/io/github/snipperxx/applyledger`):

- `company/`: company entity, service, REST controller
- `application/`: applications, the status workflow (`ApplicationStatus`), history, search
- `report/`: follow-ups, stats, CSV export
- `common/`: error handling, paging, clock and settings

The schema is managed by Flyway (`src/main/resources/db/migration`).

## License

MIT, see [LICENSE](LICENSE).
