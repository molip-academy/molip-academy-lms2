# molip-academy-lms2

## Repository layout

| Directory     | Contents                                          |
| ------------- | ------------------------------------------------- |
| `back/`       | Backend code                                      |
| `front-react/`| Web frontend (React)                              |
| `infra/`      | Infrastructure — Terraform and friends            |
| `docs/`       | Domain docs (`adr/`) and agent config (`agents/`) |
| `.scratch/`   | Specs and issues (this repo's issue tracker)      |

## Agent skills

### Issue tracker

Issues live as markdown files under `.scratch/<feature-slug>/`. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical triage roles, used as-is. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `CONTEXT.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.
