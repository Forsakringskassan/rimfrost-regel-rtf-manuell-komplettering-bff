# rimfrost-regel-rtf-manuell-komplettering-bff

Backend-for-frontend for the RTF manuell komplettering micro-frontend. Proxies komplettering data
to and from the `rimfrost-regel-rtf-manuell-komplettering` rule service.

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_** Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:9004/q/dev/>.

The application runs on port **9004** by default.

To run the full build locally (mirrors CI, skips Docker):

```shell script
./mvnw verify -Dquarkus.container-image.build=false
```

## Environment variables

| Variable | Default | Description |
|---|---|---|
| `BACKEND_URL` | `http://localhost:8080/regel/rtf-manuell-komplettering` | Full base URL of the komplettering rule service, including the rule path. No trailing slash. |
| `CORS_ORIGINS` | `http://localhost:3000,http://localhost:3030` | Comma-separated list of allowed CORS origins. |

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

Docker image build is disabled by default (`quarkus.container-image.build=false` in `application.properties`), so this command only produces the JAR.

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it's not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

## Packaging and running as Docker

Build a Docker image _rimfrost/rimfrost-regel-rtf-manuell-komplettering-bff:latest_:

```shell script
./mvnw clean package -Dquarkus.container-image.build=true
```

Launch container:

```shell script
docker run -p 9004:9004 \
  -e BACKEND_URL=http://host.docker.internal:8080/regel/rtf-manuell-komplettering \
  rimfrost/rimfrost-regel-rtf-manuell-komplettering-bff
```

## REST endpoints

| Method | Path | Backend call | Description |
|---|---|---|---|
| `GET` | `/api/{handlaggningId}/komplettering` | `GET /{handlaggningId}` | Fetches the current komplettering data (`personnummer`, `avsikt`). |
| `PATCH` | `/api/{handlaggningId}/komplettering` | `PATCH /{handlaggningId}` | Registers `personnummer` and `avsikt`. Both are required; a missing field is rejected with 400. |
| `POST` | `/api/{handlaggningId}/komplettering/done` | `POST /{handlaggningId}/done` | Completes the komplettering and closes the OUL task. Forwards the backend status: 204 done, 409 correlation state already cleared, 422 yrkande still incomplete. |
| `GET` | `/api/uppgiftsbeskrivning/{uppgiftstyp}` | `GET /utokadUppgiftsbeskrivning` | Fetches the task help text from the rule's `config.yaml`. |
| `GET` | `/q/health` | — | Health check provided by `quarkus-smallrye-health`. |

The `Authorization` header is passed through unchanged on every call that carries one.


Health: <http://localhost:9004/q/health>
