# apply-ledger

A small self-hosted tracker for my own job applications. Work in progress.

Built with Java 21, Spring Boot 4 and PostgreSQL.

## Run locally

```bash
cp .env.example .env          # then set DB_PASSWORD
docker compose up -d postgres
./mvnw spring-boot:run
```

## License

MIT, see [LICENSE](LICENSE).
