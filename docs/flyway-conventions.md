# Flyway Migration Conventions

## Naming Pattern
`V{sequential_number}__{description}.sql`

Examples:
- V1__create_users_table.sql
- V2__add_email_to_users.sql
- V3__create_orders_table.sql

## Rules
1. Version numbers must be sequential integers (1, 2, 3, ...)
2. Use double underscores (__) between version and description
3. Description should be snake_case
4. Never reuse version numbers
5. Never modify already-applied migrations

## Commands
- `mvn flyway:info` - Show migration status
- `mvn flyway:repair` - Fix history table issues
- `mvn flyway:migrate` - Apply pending migrations
- `mvn flyway:validate` - Check for problems

## Configuration
- `validate-on-migrate: true` - Always validate before migrating
- `clean-on-validation-error: false` - Never auto-clean on errors
- `outOfOrder: false` - Enforce sequential execution