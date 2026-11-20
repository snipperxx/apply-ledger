# Roadmap

What I plan to build next, roughly in this order. Bigger items are also open as GitHub issues.

## v0.2: usable day to day

- **Login**: single-user login (form login or an API token from the environment) so the app can run on a home
  server instead of only on localhost.
- **Web UI**: a small server-rendered UI (Thymeleaf + a bit of htmx): application list with filters, detail page with
  the history timeline, buttons for the allowed next statuses, follow-up list on the start page.
- **Interviews as events**: date, time, kind (phone, technical, on-site) and notes per application; upcoming ones in
  the follow-ups; `.ics` export so they land in my calendar.
- **CSV import**: bring over the spreadsheet I used before; dry-run mode that reports problems per row.

## v0.3: better overview

- **Tags** (e.g. "Java", "relocation", "referral") with filter support.
- **Salary range** (min/max, currency) on applications and offers.
- **CV/cover letter versions**: record which version I sent with each application (file name or link, not the
  file itself at first).
- **Weekly summary**: applications sent, answers, interviews per week, as an API endpoint first, then a chart in
  the UI.
- **Stale handling**: suggest closing applications that have been quiet for a long time, with one call.

## Maintenance

- Optimistic locking on edits from the client side (send `version`, get 409 on conflicts).
- Backup and restore notes for the PostgreSQL volume (`pg_dump` script).
- Container image published from CI on tags.
- More test cases around time zones (what counts as "today").

## Not planned

- Storing contact people (recruiters, interviewers) or e-mail contents. This is a tracker for my own data only.
- Scraping job boards.
