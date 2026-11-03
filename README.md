# Subscription Finder

I wanted to know what I'm actually paying for every month without going through a whole statement line by line, so I built this. You upload a CSV from your bank or card and it finds the charges that repeat. It tells you what each one costs per month and per year, and points out the ones that went up in price or look like they stopped.

The sample file in here is fake. I made it up so there's nothing real in the repo. It's built with Java 21, Spring Boot, PostgreSQL, Docker, and GitHub Actions.

## Running it

You'll need Docker Desktop. Set a database password and start everything:

```bash
export POSTGRES_PASSWORD='pick-something-long'
docker compose up --build
```

Once it's up, upload the sample file:

```bash
curl -F "file=@samples/sample-bank.csv" http://localhost:8080/api/imports
```

That gives you back an id. Use it to look at the results (swap in your id for the 1):

```bash
curl http://localhost:8080/api/imports/1/subscriptions
curl http://localhost:8080/api/imports/1/summary
```

On the sample it finds Netflix and notices the price went up about 16%, plus Spotify, the gym, Adobe, Apple, and rent. It marks Hulu as probably cancelled because the charges stopped in July, and it leaves the groceries and coffee alone.

## Using your own statement

Your CSV needs a header row with columns called date, description, and amount. Other columns are ignored and the order doesn't matter. Dates can look like 2026-03-15 or 3/15/2026. If some of the amounts are negative, those are treated as the charges, which is how most bank exports work. If none are negative, the positive ones are the charges, which is how most card exports work. If a row has a problem, the error tells you which row it is.

## How it decides what counts as a subscription

A merchant has to show up at least three times. The charges have to land about a week, a month, three months, or a year apart, and a few days early or late is fine. The amounts also have to stay within about 10% of each other. That last part is what keeps groceries and coffee out, since you go to those on a regular schedule too, but the totals are all over the place. It also allows for one price change, so something that went from $15.49 to $17.99 still counts.

Before any of that, the descriptions get cleaned up so that NETFLIX.COM 866-579-7172 CA and the same thing with a different reference number end up as the same merchant.

A couple of limits: a yearly charge needs three years of history before it shows up, and rent and other bills show up too, since they do repeat.

## Tests

`mvn test` runs the fast unit tests and doesn't need Docker. `mvn verify` also runs the API tests against a real PostgreSQL in Docker. The unit tests cover the CSV reader, the description cleanup, and the detection rules, including price changes, cancelled subscriptions, and look-alikes that shouldn't be flagged.

If `mvn verify` fails with "client version 1.32 is too old" on a very new Docker, run `echo "api.version=1.44" > ~/.docker-java.properties` and try again. It's a mismatch between new Docker versions and the older Testcontainers library that Spring Boot 3.3 uses.

## CI/CD

The GitHub Actions workflow builds and tests everything on each push and pull request. When something is pushed to main and the tests pass, it also publishes the Docker image to GitHub Container Registry.

## Where things are

```
src/main/java/.../domain       CSV reader, cleanup, and detection rules (plain Java)
src/main/java/.../api          REST endpoints
src/main/java/.../persistence  database entities and repositories
src/main/resources/db          database migrations (Flyway)
samples/                       fake bank export
```

## What I'd add next

A login so everyone only sees their own uploads, a simple page for uploading a file, categories like streaming and fitness, and batch inserts for very large files.
