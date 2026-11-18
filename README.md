# Transporter Assignment on Lanes

Spring Boot service that assigns transporters to lanes across India at the **lowest total cost**, using **at most N
transporters**, with **every lane covered**.

| | |
|---|---|
| **Live app / Swagger UI** | <LIVE_URL> (the root URL opens Swagger) |
| **Postman collection** | [`postman/Transporter-Assignment.postman_collection.json`](postman/Transporter-Assignment.postman_collection.json) |
| **Stack** | Java 17, Spring Boot 4.1, Spring Data JPA, H2, springdoc-openapi, JUnit 5, JaCoCo |

> **Note on the brief's sample answers.** Both sample responses in the PDF are inconsistent with their own inputs
> (one says 75,000 where its lanes add up to 69,001 and use 4 transporters under a limit of 3). The real optimum is
> **60,139**. Details in [the section below](#the-briefs-sample-answers-do-not-add-up).

## Run it

Needs Java 17. Nothing else: the Maven wrapper is included.

```bash
./mvnw spring-boot:run          # http://localhost:8080  (opens Swagger UI)
./mvnw verify                   # all tests + 85% coverage gate
```

Or with Docker (the same image the live app runs):

```bash
docker build -t transporter-lane-assignment . && docker run -p 8080:8080 transporter-lane-assignment
```

## API

Swagger UI has a drop-down of ready examples (the brief's Test Case 1 and 2, the provided CSV, the brief's first
example) so each case runs in one click. Raw spec: `/v3/api-docs`.

### `POST /api/v1/transporters/input` — submit lanes and quotes

Stores the data **in place of** whatever was submitted before. Body as in the brief:

```json
{
  "lanes": [{"id": 1, "origin": "Mumbai", "destination": "Delhi"}],
  "transporters": [{"id": 1, "name": "Transporter T1", "laneQuotes": [{"laneId": 1, "quote": 5000}]}]
}
```

```json
{"status": "success", "message": "Input data saved successfully."}
```

### `POST /api/v1/transporters/assignment` — cheapest assignment

```bash
curl -X POST $BASE/api/v1/transporters/assignment -H 'Content-Type: application/json' -d '{"maxTransporters": 3}'
```

```json
{
  "status": "success",
  "totalCost": 60139,
  "assignments": [
    {"laneId": 1, "transporterId": 5}, {"laneId": 2, "transporterId": 1}, {"laneId": 3, "transporterId": 4},
    {"laneId": 4, "transporterId": 4}, {"laneId": 5, "transporterId": 4}
  ],
  "selectedTransporters": [1, 4, 5],
  "optimal": true
}
```

`optimal` is an addition to the brief's shape: `true` means the search finished and no cheaper answer exists. It is
`false` only if the search budget ran out on a very large input, in which case the answer is valid but possibly not
the cheapest.

### Errors

Every failure has one shape: `{"status": "error", "code": "...", "message": "...", "errors": [...]}`.

| HTTP | `code` | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Missing or blank fields, quote ≤ 0, `maxTransporters` < 1. `errors` lists each field. |
| 400 | `INVALID_INPUT` | Duplicate lane or transporter ids, a quote for a lane that is not in the request, the same lane quoted twice, size caps exceeded. |
| 400 | `MALFORMED_REQUEST` | Not JSON, wrong type, or a decimal quote such as `5000.75` (rejected, never rounded). Names the bad field. |
| 409 | `NO_INPUT_DATA` | Assignment requested before any input. |
| 422 | `NO_FEASIBLE_ASSIGNMENT` | No set of ≤ N transporters covers every lane. Says how many are needed, or which lane nobody quoted. |
| 405 / 415 / 404 | `METHOD_NOT_ALLOWED` ... | Framework errors use the same shape. |

A rejected input never touches stored data: validation runs before the old input is replaced.

## How the optimizer works

Choosing which ≤ N transporters to use, then giving each lane to the cheapest of them, is a k-median style problem.
It is NP-hard, so there is no known shortcut that is always fast, but the instances in the brief are small and the
exact answer is cheap to find with a good search.

1. **Fixed picks.** If a lane has only one bidder, that transporter must be in every valid answer. If those
   transporters already exceed N (or a lane has no quote at all) the request is infeasible, proven without searching.
2. **Seed** (`GreedyLocalSearchSolver`). Greedily add the transporter that helps most, then swap transporters while
   swaps improve the cost. Gives a strong first answer to prune against.
3. **Exact search** (`BranchAndBoundSolver`). Depth-first over "take this transporter / skip it". At each node the
   lower bound gives every lane its cheapest quote still available deeper in the tree; branches that cannot beat the
   best answer are cut. A transporter that would win no lane is never taken.
4. **Budget.** The search stops after 50 million lane evaluations (about 1–3 s on the largest input the API accepts,
   measured in `LargestAllowedProblemTest`). If it stops early the response says `"optimal": false`.

**Rules**, applied the same way everywhere (and tested):
- each lane goes to the cheapest chosen transporter that quoted it; an equal quote goes to the lower transporter id;
- lowest total cost wins; on equal cost, **more transporters used** wins (the brief asks to "utilize the maximum number
  of transporters up to the limit"); then the smaller list of ids. The answer is therefore deterministic.

Correctness is checked against a brute-force solver on 1,000 random problems, including heavy ties and lanes with
no quotes (`BranchAndBoundSolverTest`, `CoverageAnalyzerTest`).

### The brief's sample answers do not add up

| Brief says | What the data says |
|---|---|
| Test Case 1, limit 3: cost **75,000**, transporters 4, 6, 2, 3 | Those lanes sum to **69,001**, and that is **4** transporters, over the limit. The true optimum for limit 3 is **60,139** with transporters 1, 4, 5. |
| First example (10 lanes), limit 3: cost 29,000, transporters 1, 2, 3 | Lanes 7 and 8 are only quoted by transporter 4, lanes 9 and 10 only by 5, so **5 transporters are needed**. With limit 3 the API returns 422 saying so; with 5 the cost is 48,000. |

Optimal costs for Test Case 1 by limit: 1 → 96,940, 2 → 62,938, 3 → 60,139, 4 or more → 59,896. Test Case 2 (sparse
quotes): limit 2 is infeasible, 3 → 7,500, 4 → 7,450. The provided CSV (9 lanes): 3 → 134,876, 4 → 127,844,
5 → 125,045. All are asserted in the tests.

## Design

```
api/        controller, request/response DTOs (records + Bean Validation), global error handler
service/    TransporterInputService (validate + store), AssignmentService (load + solve + explain failures),
            InputValidator (whole-payload rules), InputDataStore (transaction + read/write lock)
optimizer/  pure Java, no Spring: AssignmentProblem (builder), AssignmentSolver (strategy interface),
            GreedyLocalSearchSolver, BranchAndBoundSolver, CoverageAnalyzer
domain/ repository/   JPA entities and Spring Data repositories
config/     solver wiring, tunable limits (application.properties), OpenAPI metadata
```

- **Strategy**: `AssignmentSolver` is an interface; the exact and heuristic solvers are interchangeable and the
  service never knows which one it has. **Builder**: `AssignmentProblem`. **Repository**, **DTO**, and one
  `@RestControllerAdvice` for errors.
- The optimizer package has no Spring or database dependency, so it is tested in milliseconds and could be lifted
  out unchanged.
- **Efficient storage.** Each input replaces the last with three bulk deletes and batched inserts. Entities have
  client-assigned ids and implement `Persistable`, so Spring Data does not run a `SELECT` before every insert.
- **Concurrency.** A read-write lock keeps an assignment request from reading half of a replacement and keeps two
  simultaneous submissions from colliding on primary keys. It is enough for one instance on an in-memory database; a
  shared database would use a database lock instead.

## Testing

`./mvnw verify` runs **115 tests** and fails the build if line coverage is below 85% (it is about 97%).

- **Optimizer**: the brief's cases, the CSV, tie-breaks, node-budget behaviour, brute-force comparison on random
  problems, and the largest allowed input under a time limit.
- **API** (`MockMvc` against the real app and H2): both endpoints with the brief's cases, every error path in the
  table above, replacement semantics, and that a rejected input leaves the old data intact.
- **Docs**: Swagger UI is served, `/` redirects to it, and the OpenAPI document carries the examples.
- **Postman**: import the collection, set `baseUrl`, run it; each request has assertions.

CI (`.github/workflows/ci.yml`) runs the same `./mvnw verify` on every push.

## Deploy (Render, free)

`render.yaml` is a Blueprint: in Render choose **New → Blueprint**, pick this repo, and it builds the `Dockerfile`.
Health check: `/actuator/health`. The container runs with a 320 MB heap for the 512 MB free plan and trusts the
proxy's forwarded headers so Swagger's "Try it out" calls the `https` origin.

The free plan sleeps after 15 idle minutes (first request then takes about a minute). `.github/workflows/keep-alive.yml`
pings the health URL every 10 minutes once you set the repository variable `LIVE_URL`.

## Assumptions and limits

- Quotes are **whole rupees** above zero; decimals are rejected. Ids are positive integers.
- Data is **in memory** (H2): it resets when the server restarts, and each new input replaces the previous one. A
  reviewer calls the input API first, then the assignment API. Swapping H2 for PostgreSQL is a datasource change
  (`spring.datasource.*`) plus the driver dependency; no code depends on H2.
- A transporter may quote any subset of lanes. A lane may only be served by a transporter that quoted it.
- `maxTransporters` larger than the number of transporters is fine.
- Size caps (configurable, `transporter.limits.*`): 10,000 lanes, 1,000 transporters, 200,000 quotes, and 2 million
  lanes × transporters. They keep a small free server safe from a huge request.
- No authentication or UI, as the brief specifies.
