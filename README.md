# Subscription Finder

Upload a bank or card export and it finds your recurring charges: what each one costs per month and per year, which ones went up in price, and which ones seem to have stopped.

I wanted something that could look through my own statements and tell me what I'm paying for without me going line by line. The sample file here is **made up**, so there's nothing real in the repo.

Built with Java 21, Spring Boot, PostgreSQL, Docker, and GitHub Actions.

## Try it

You need [Docker Desktop](https://www.docker.com/products/docker-desktop/). Then:

```bash
export POSTGRES_PASSWORD='pick-something-long'
docker compose up --build
```

Upload the sample file:

```bash
curl -F "file=@samples/sample-bank.csv" http://localhost:8080/api/imports
```

That returns an id. Use it to see the results (swap in your id for the `1`):

```bash
curl http://localhost:8080/api/imports/1/subscriptions
curl http://localhost:8080/api/imports/1/summary
```

On the sample file it finds Netflix (and that its price went up 16%), Spotify, the gym, Adobe, and Apple, flags Hulu as probably cancelled, and skips the groceries and coffee.

## Your own statement

The CSV needs a header row with columns named `date`, `description` and `amount`. Other columns are fine and the order doesn't matter. Dates can be `2026-03-15` or `3/15/2026`.

If any amounts are negative, the negative ones are treated as charges (the usual bank export). If none are negative, every positive amount is a charge (the usual card export). Bad rows get a message that says which row is wrong.

## How it decides something is a subscription

A merchant counts when it has at least 3 charges and:

1. **The timing fits.** The gaps between charges match weekly, monthly, quarterly, or yearly billing (a few days early or late is fine).
2. **The amounts stay close.** Within about 10% of each other, or steady, then one price change, then steady again.

The second rule is what keeps groceries and coffee out. They show up on a regular pace too, but the amounts jump around.

Descriptions get cleaned up first, so `NETFLIX.COM 866-579-7172 CA` and `SPOTIFY USA 8841 NY` match the same merchant every month even when the reference numbers change.

Limits worth knowing: a once-a-year charge needs three years of history to be found. Rent and other bills show up as recurring charges too, since they are. And the sample shows what the rules catch, not what they'd catch on every real statement.

## Tests

```bash
mvn test      # fast unit tests, no Docker needed
mvn verify    # also runs the API tests against a real PostgreSQL in Docker
```

The unit tests cover the CSV reader, the description cleanup, and the detection rules, including price changes, cancelled subscriptions, and look-alikes that should *not* be flagged. The API tests start the whole app against PostgreSQL.

## CI/CD

`.github/workflows/ci.yml` runs on every push and pull request: it builds the project and runs all the tests. When something is pushed to `main` and the tests pass, it builds the Docker image and publishes it to GitHub Container Registry.

## Project layout

```
src/main/java/.../domain       the CSV reader, cleanup, and detection rules (plain Java, no framework)
src/main/java/.../api          the REST endpoints
src/main/java/.../persistence  database entities and repositories
src/main/resources/db          database migrations (Flyway)
samples/                       fake bank export
```

## What I'd add next

- Login, so each person only sees their own uploads
- A simple page for uploading a file and seeing the results
- Categories (streaming, fitness, software) and a way to mark something as not a subscription
- Batch inserts for very large files
