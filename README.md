# Career Domain Intelligence Platform using Knowledge Graphs and Machine Learning

This final-year project helps students compare their skills with career roles, understand missing skills, follow a prerequisite-aware learning path, and explore an actual Neo4j career knowledge graph.

## Architecture

```text
React / Vite frontend
        |
Spring Boot REST API
  |-- MySQL / JPA: accounts, profiles, user skills, jobs, assessments
  |-- Neo4j: domains, roles, skills, technologies, resources and relationships
  |-- Explainable career-intelligence services
  `-- ESCO Web Service import (administrator initiated)
```

## Features

- Secure registration and sign-in with BCrypt password hashes and signed bearer tokens.
- Profile and normalized skill management with duplicate prevention.
- Job-description ingestion with deterministic skill extraction and generated assessments.
- Career matches based on TF-IDF weighted skill vectors and cosine similarity. This is an explainable deterministic recommendation method; the application does **not** claim a trained Random Forest, XGBoost, deep-learning model, or LLM recommender.
- Career Domain Explorer for Software Engineering, Data & Artificial Intelligence, Cybersecurity, Information Technology, and Cloud & DevOps.
- Prerequisite-aware learning paths with mapped resources.
- Neo4j-backed knowledge-graph views plus Breadth-First Search, Depth-First Search, shortest learning route, skill neighbours, and skill-gap analysis.
- ESCO occupation and skill refresh, protected for the configured administrator only.

## Prerequisites

- Java 21+
- Node.js 20+ and npm
- MySQL 8+ running on `localhost:3306`
- Neo4j 5+ running on `bolt://localhost:7687`

The database name is `career_domain_intelligence_platform`. Create a MySQL application user with access to that database. Neo4j authentication remains enabled: the actual password is never stored in this repository.

## Configuration

Set these values in your shell or deployment environment before starting the backend:

```bash
export DB_URL='jdbc:mysql://localhost:3306/career_domain_intelligence_platform?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata'
export DB_USERNAME='your_mysql_application_user'
export DB_PASSWORD='your_mysql_application_password'
export NEO4J_URI='bolt://localhost:7687'
export NEO4J_USERNAME='neo4j'
export NEO4J_PASSWORD='your_actual_neo4j_password'
export APP_AUTH_SECRET='a-unique-random-secret-of-at-least-32-characters'
```

Optional values are `APP_AUTH_EXPIRATION_HOURS` (default `24`), `APP_CORS_ALLOWED_ORIGINS` (defaults to the Vite local origins), and `APP_ADMIN_EMAIL` (defaults to `admin@careerdomain.local`). Assign `APP_ADMIN_EMAIL` to an account you control before using an administrative ESCO endpoint.

`docker-compose.yml` is an optional local-service setup. It exposes MySQL on `3306`, Neo4j Browser on `7474`, and Neo4j Bolt on `7687`; it deliberately requires `DB_USERNAME`, `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`, and `NEO4J_PASSWORD` from your environment.

## Run

Start the Spring API:

```bash
./mvnw spring-boot:run
```

In another terminal, start the frontend:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. The API listens on `http://localhost:8080`; the Vite proxy forwards `/api` requests to it.

## API overview

Public endpoints:

- `POST /auth/register`
- `POST /auth/login`
- `GET /actuator/health`

Authenticated endpoints include profiles and skills (`/users/me/*`), jobs (`/jobs`), recommendations (`/recommendations/me`), learning paths (`/learning-path/jobs/{id}`), graph operations (`/graph/*`), assessments, insights, domains, roles, and skills. Send `Authorization: Bearer <token>` for protected requests.

Administrative knowledge-base endpoints are additionally restricted to `APP_ADMIN_EMAIL`:

- `POST /admin/knowledge-base/reclassify-domains`
- `POST /admin/knowledge-base/refresh-esco`

## Graph algorithms

- **Explore Skills Level by Level** — Breadth-First Search (BFS), O(V + E) over the explored graph.
- **Explore One Skill Path Deeply** — Depth-First Search (DFS), O(V + E) over the explored graph.
- **Find the Shortest Learning Route** — shortest-path search between selected skills, O(V + E) for an unweighted graph.
- **Skill neighbours** — related skills reached through role, technology, learning-resource, and prerequisite relationships.
- **Skill Gap** — comparison of current skills against a target requirement set.

The startup knowledge-base seed is idempotent: domains, roles, graph nodes, and relationships use stable identities and `MERGE`; it does not delete user accounts, profiles, or user skills. Neo4j is required because graph-backed features are not silently simulated. If its credential is absent or incorrect, startup or graph API responses clearly direct you to `NEO4J_URI`, `NEO4J_USERNAME`, and `NEO4J_PASSWORD`.

## Verification

```bash
./mvnw clean test
./mvnw package
cd frontend && npm run build
```

## Troubleshooting

- **Neo4j authentication / unavailable graph:** verify Neo4j is running at `bolt://localhost:7687`, then export the actual `NEO4J_USERNAME` and `NEO4J_PASSWORD`. Do not disable Neo4j security or use a guessed password.
- **MySQL connection error:** confirm MySQL is on port `3306`, the database exists or can be created by the application user, and `DB_*` values match that user.
- **CORS error in local development:** use the Vite server at `http://localhost:5173`, or add the exact frontend origin to `APP_CORS_ALLOWED_ORIGINS`.
