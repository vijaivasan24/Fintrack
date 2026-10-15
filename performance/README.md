# FinTrack Performance Testing

This folder contains the performance test suite, JMeter test plan, seed dataset, and helper scripts for FinTrack.

## Overview

The load test simulates 500 concurrent users performing typical workflows against the FinTrack REST API on port 8089.
Each user thread logs in once to obtain a JWT token, then executes 10 iterations of the following endpoints with a 1000 ms timer:
- GET /api/transactions?page=0&size=20 (paginated transactions)
- GET /api/transactions with date and type filters
- GET /api/reports/summary for the current year
- GET /api/reports/monthly for the current year
- GET /api/budgets/status for the current month
- POST /api/transactions (creates a new valid expense)
- GET /api/categories (user categories list)

## Test Environment

- CPU: 12th Gen Intel(R) Core(TM) i5-12450H (8 cores, 12 logical processors)
- RAM: 8 GB
- Operating System: Microsoft Windows 11 Home Single Language (Version 10.0.26200)
- Java Version: 21.0.10 LTS
- JMeter Version: Apache JMeter 5.6.3
- Note: The load generator (JMeter), application backend, and MySQL server all ran concurrently on the same laptop.

## Fake Credentials and Seed Data

- Seed accounts: perf1@example.com through perf500@example.com
- Seed password: PerfPass123!
- Seed dataset contents: 500 users, 3,000 categories (6 per user), 50,000 transactions (100 per user over 12 months), and 1,500 budgets (3 per user for the current month).

## Step-by-Step Instructions

### 1. Seed Database

Run the seeder with the local, perf, and seed profiles using the non-web execution flag:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local,perf,seed" "-Dspring-boot.run.arguments=--spring.main.web-application-type=none"
cd ..
```

The seeder validates that the datasource URL contains `fintrack_perf` before seeding. If the users table already contains rows, it skips insertion.

### 2. Build and Start Backend

Build the application package and launch the backend on port 8089:

```powershell
cd backend
.\mvnw.cmd package -DskipTests
cd ..

# Start backend process in background on port 8089
java "-Dspring.profiles.active=local,perf" "-Dserver.port=8089" -jar backend\target\fintrack-backend-0.0.1-SNAPSHOT.jar
```

Wait until the application is ready:

```powershell
powershell -ExecutionPolicy Bypass -File performance/wait-backend.ps1
```

Record the process ID to stop it after testing:

```powershell
powershell -ExecutionPolicy Bypass -File performance/record-pid.ps1
```

### 3. Run Load Test

Run JMeter in non-GUI mode with 500 threads, 60 seconds ramp-up, and 10 loops:

```powershell
C:\Users\vijai\Documents\apache-jmeter-5.6.3\bin\jmeter.bat -n -t performance/fintrack-load-test.jmx -Jthreads=500 -Jrampup=60 -Jloops=10 -l performance/results/run1.jtl -e -o performance/report/run1
```

### 4. Summarize Results

Generate the summary table from the JTL result file:

```powershell
powershell -ExecutionPolicy Bypass -File performance/summarize.ps1 -JtlPath performance/results/run1.jtl
```

### 5. Stop Backend

Stop the running backend process using its recorded PID:

```powershell
powershell -ExecutionPolicy Bypass -File performance/stop-backend.ps1
```

## Results

### Run 1 Results (500 threads, 60s ramp-up, 10 loops)

| Metric | Login | All API Labels Combined |
|---|---|---|
| Sample Count | 500 | 35,000 |
| Average (ms) | 139.27 | 12.25 |
| Median (ms) | 127.00 | 11.00 |
| P90 (ms) | 179.00 | 20.00 |
| P95 (ms) | 206.00 | 23.00 |
| P99 (ms) | 278.00 | 33.00 |
| Max (ms) | 424.00 | 193.00 |
| Error Rate | 0.00% | 0.00% |
| Throughput | 8.4/s | 270.2/s |

### Per-Endpoint Breakdown (Run 1)

| Sampler Label | Samples | Average Response Time (ms) | Error Rate |
|---|---|---|---|
| api:categories | 5,000 | 6.38 | 0.00% |
| api:reports_monthly | 5,000 | 7.64 | 0.00% |
| api:reports_summary | 5,000 | 10.28 | 0.00% |
| api:budgets_status | 5,000 | 13.02 | 0.00% |
| api:transactions_filtered | 5,000 | 13.71 | 0.00% |
| api:transactions_paged | 5,000 | 14.89 | 0.00% |
| api:transactions_create | 5,000 | 19.86 | 0.00% |
