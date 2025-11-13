# DuckBot Java

DuckBot Java is a JavaFX desktop application for orchestrating LDPlayer-based automation bots. This repository contains a foundational implementation that follows the DuckBot master build specification.

## Features

- **Authentication** – Pluggable provider architecture with a local Argon2-backed implementation and a stubbed cloud provider.
- **Bot Profiles** – Domain model for multi-instance bot definitions, persisted as JSON files under `data/bots/`.
- **Script Engine** – Asynchronous engine with built-in step types (tap, swipe, scroll, wait, input, IF image, loops, OCR stub, log, custom JS stub, exit).
- **Runner Service** – Thread-safe runner that reserves LDPlayer instances and dispatches scripts.
- **Storage Helpers** – JSON persistence built on Gson, automatic data directory management, and daily log files.
- **JavaFX UI** – Login flow and a tabbed control center with themed styling.

## Project Structure

```
DuckBot-Java/
 ├─ src/main/java/com/duckbot/
 │   ├─ app/           # JavaFX entry point and UI composition
 │   ├─ core/          # Domain models (bot, config, user, run status)
 │   ├─ scripts/       # Script engine, context, steps
 │   ├─ adb/           # LDPlayer & ADB stubs
 │   ├─ ocr/           # OCR and image stubs
 │   ├─ services/      # Service interfaces
 │   ├─ services/impl/ # Service implementations
 │   ├─ store/         # JSON persistence helpers
 │   └─ util/          # Utility classes (data paths, templating)
 ├─ src/main/resources/
 │   └─ themes/        # CSS themes (black-blue default)
 └─ data/              # Runtime data directory (ignored by Git)
```

## Building

The project uses Maven with Java 17.

```
mvn clean package
```

A shaded JAR with the entry point `com.duckbot.app.DuckBotApp` will be produced under `target/`.

## Running

Ensure you are using JDK 17 or later and execute the shaded artifact:

```
java -jar target/duckbot-java-0.1.0-SNAPSHOT-shaded.jar
```

On first launch you will be prompted to create an admin account. All runtime files are created under the `data/` directory.