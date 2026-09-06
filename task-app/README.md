# task-app — WeeklyCompass backend

Ruby on Rails 8 API-only backend for WeeklyCompass. See the root [CLAUDE.md](../CLAUDE.md) and [startup-guide.md](../startup-guide.md) for the project overview, architecture, and how to run the whole stack.

- Ruby: 3.3.11
- Rails: 8.1 (`--api`)
- DB: PostgreSQL (`task_app` dev, `task_app_test` test)

```bash
bundle install
bin/rails db:migrate   # or db:schema:load
bin/rails server -p 8080
bin/rails test
```
