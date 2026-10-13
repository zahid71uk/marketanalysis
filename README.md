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

## Docker

Build and run the image from this directory after starting `PricePredictionModel` and `valueestimator` on the `property-estimator` Docker network:

```powershell
docker build -t market-analysis-api .
docker run -d --name marketanalysis --network property-estimator -p 8082:8082 -e DATASET_API_URL=http://price-prediction:8000 -e ESTIMATOR_API_URL=http://valueestimator:8001 market-analysis-api
```

The image builds the Spring Boot application with Java 21 and runs it on port 8082. Check `http://localhost:8082/actuator/health`. To enable cache invalidation after dataset uploads, also pass `-e DATASET_CACHE_INVALIDATION_TOKEN=$env:DATASET_CACHE_INVALIDATION_TOKEN`; use the same value for `MARKET_ANALYSIS_CACHE_INVALIDATION_TOKEN` in `PricePredictionModel`. Stop the container with `docker stop marketanalysis` and remove it with `docker rm marketanalysis`.

## Configuration

| Environment variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8082` | HTTP port |
| `DATASET_API_URL` | `http://127.0.0.1:8000` | `PricePredictionModel` base URL |
| `ESTIMATOR_API_URL` | `http://127.0.0.1:8001` | `valueestimator` base URL |
| `DATASET_CACHE_TTL` | `PT5M` | In-memory dataset cache duration (ISO-8601 duration) |
| `DATASET_CACHE_INVALIDATION_TOKEN` | unset | Required shared secret for the internal dataset-cache invalidation endpoint; keep this endpoint on a private service network |
| `UPSTREAM_CONNECT_TIMEOUT` | `PT2S` | Upstream connection timeout |
| `UPSTREAM_READ_TIMEOUT` | `PT8S` | Upstream response timeout |

## Endpoints

| Method | Path | Description |
| --- | --- | --- |
| GET | `/api/v1/market-analysis/summary` | Filtered sample size, mean/median/range price, and average features |
| GET | `/api/v1/market-analysis/properties` | Filtered, sortable, paginated source rows |
| POST | `/api/v1/market-analysis/what-if` | Validated property estimate through `valueestimator` |
| POST | `/api/v1/market-analysis/internal/dataset-cache` | Internal cache invalidation; requires the `X-Dataset-Cache-Token` header |
| GET | `/actuator/health` | Service health |

After a dataset promotion, `PricePredictionModel` invalidates the local dataset cache using the configured token. Set `MARKET_ANALYSIS_CACHE_INVALIDATION_TOKEN` in PricePredictionModel to the same value. If this service's token is unset, the endpoint returns HTTP 503; invalid tokens receive HTTP 401. Eviction completes before the endpoint returns HTTP 204, so the next summary or property request reloads the active dataset. The cache TTL remains a recovery fallback.

Summary and property endpoints accept `minSquareFootage`, `maxSquareFootage`, `minBedrooms`, `maxBedrooms`, `minYearBuilt`, and `maxYearBuilt`. Property results also accept `page` (zero-based), `size` (1–100), `sortBy` (`price`, `squareFootage`, `bedrooms`, `yearBuilt`), and `direction` (`asc` or `desc`).

The checked-in dataset has 50 rows, no dates or locations, and no documented measurement units. Responses are descriptive sample statistics only; they do not support market trends, geographic comparisons, or formal valuations. Dataset and estimator failures return HTTP 502 with a user-readable message.

## Tests

```powershell
mvn test
```