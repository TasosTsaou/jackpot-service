API Documentation

- Live Swagger UI: http://localhost:8080/swagger-ui
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Export the spec locally:

- Using Make: `make openapi-export` (writes `docs/api/openapi.json`).
- Script: `./scripts/export_openapi.sh`.

Conventions

- Versioned paths exposed alongside legacy prefix: `/api` and `/api/v1`.
- Errors follow RFC 7807 (`application/problem+json`).
- Every operation documents success and 400 error responses with examples.

