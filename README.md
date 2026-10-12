# Market Analysis API

An independently runnable Java service for descriptive analysis of the housing sample and what-if estimates through the existing Value Estimator API. It fetches typed rows over HTTP; it does not read or package the Python service's CSV file.

## Requirements

- Java 21
- Maven 3.6.3 or newer

## Start

Start `PricePredictionModel` on port 8000 and `valueestimator` on port 8001 first. Then, from this folder:

```powershell
$env:JAVA_HOME = "$env:LOCALAPPDATA\Programs\Java\temurin-21-jdk"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn test
mvn spring-boot:run
```

The environment assignments select the user-local Temurin JDK for this PowerShell session only. If Java 21 is already selected in your terminal, those two lines are not needed.

The service listens on `http://127.0.0.1:8082`. Its health endpoint is `http://127.0.0.1:8082/actuator/health`.

## Configuration

| Environment variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8082` | HTTP port |
| `DATASET_API_URL` | `http://127.0.0.1:8000` | `PricePredictionModel` base URL |
| `ESTIMATOR_API_URL` | `http://127.0.0.1:8001` | `valueestimator` base URL |
| `DATASET_CACHE_TTL` | `PT5M` | In-memory dataset cache duration (ISO-8601 duration) |
| `UPSTREAM_CONNECT_TIMEOUT` | `PT2S` | Upstream connection timeout |
| `UPSTREAM_READ_TIMEOUT` | `PT8S` | Upstream response timeout |

## Endpoints

| Method | Path | Description |
| --- | --- | --- |
| GET | `/api/v1/market-analysis/summary` | Filtered sample size, mean/median/range price, and average features |
| GET | `/api/v1/market-analysis/properties` | Filtered, sortable, paginated source rows |
| POST | `/api/v1/market-analysis/what-if` | Validated property estimate through `valueestimator` |
| GET | `/actuator/health` | Service health |

Summary and property endpoints accept `minSquareFootage`, `maxSquareFootage`, `minBedrooms`, `maxBedrooms`, `minYearBuilt`, and `maxYearBuilt`. Property results also accept `page` (zero-based), `size` (1–100), `sortBy` (`price`, `squareFootage`, `bedrooms`, `yearBuilt`), and `direction` (`asc` or `desc`).

The checked-in dataset has 50 rows, no dates or locations, and no documented measurement units. Responses are descriptive sample statistics only; they do not support market trends, geographic comparisons, or formal valuations. Dataset and estimator failures return HTTP 502 with a user-readable message.

## Tests

```powershell
mvn test
```