# DuckBot Java - Copilot Instructions

## Project Overview
DuckBot is a **JavaFX desktop application** for orchestrating LDPlayer-based mobile device automation. It manages multiple bot instances running scripts concurrently, with pluggable authentication, JSON-based persistence, and an async script execution engine.

**Key Tech Stack:** Java 17, JavaFX 21, Maven, Gson, Argon2

## Architecture

### Core Components

1. **`app/DuckBotApp.java`** – JavaFX entry point. Bootstraps services, handles login flow, creates 6-tab UI (Bots, Script Builder, Live Runner, Logs, Settings, Updates).

2. **Service Layer** (`services/` and `services/impl/`)
   - **AuthService + AuthProvider** – Pluggable auth (LocalAuthProvider with Argon2, CloudAuthProvider stubbed)
   - **BotService** – Persists bot profiles (file-based)
   - **ConfigService** – Manages `data/config.json`
   - **LogService** – Daily rotating file logs
   - **RunnerService** – Orchestrates bot script execution with instance reservation
   - **InstanceRegistry** – Thread-safe tracking of LDPlayer instance availability

3. **Script Engine** (`scripts/`)
   - **ScriptEngine (DefaultScriptEngine)** – Async executor using cached thread pool
   - **Step** – Interface for composable automation steps (Tap, Swipe, Scroll, Wait, Input, IfImage, Loop, OcrRead, Log, CustomJs, Exit)
   - **ScriptContext** – Per-run state: runId, botId, variables, ADB client, logger, screenshot supplier
   - **Script** – Container of ordered steps; executed sequentially per instance

4. **Domain Models** (`core/`)
   - **BotProfile** – High-level bot definition with instances, scripts, run mode (sequential/parallel), cooldown
   - **BotInstanceBinding** – Maps instance name to bot
   - **BotScriptRef** – Script reference with enabled flag
   - **RunStatus** – Real-time run state (botId, instanceName, scriptName, state, message)
   - **Config** – App settings (auth mode, LDPlayer paths, theme, OCR config)

### Data Flow

1. **User logs in** → LocalAuthProvider validates credentials against `data/auth/users.json` (Argon2 hashed)
2. **User starts a bot** → RunnerService reserves instances, schedules scripts via ScriptEngine
3. **ScriptEngine** executes script steps asynchronously, each Step updates ScriptContext vars and triggers ADB/OCR
4. **Updates** propagate via RunStatus objects to UI for real-time display
5. **Persistence** – All JSON via JsonStore (Gson with pretty printing)

## Key Patterns

### Step Implementation Pattern
Steps implement `Step` interface with `type()` and `execute(ScriptContext)`. See `TapStep` and `IfImageStep` as examples:
- **Variable resolution** via `StringTemplate.resolve(input, vars)` 
- **Null-safe dependencies** – ADB and logging are optional
- **Exception handling** – `ScriptExitException` for intentional exits, propagates as normal completion

```java
// Example: TapStep resolves X/Y coordinates from variables
int resolvedX = Integer.parseInt(StringTemplate.resolve(x, vars));
ctx.adb.tap(ctx.instanceName, resolvedX, resolvedY);
```

### Service Dependency Injection
No DI framework. Services are manually wired in `DuckBotApp.start()` and passed to dependents:
```java
AuthProvider provider = createAuthProvider(store, config);
authService = new AuthService(provider);
```

### Concurrent Execution
- **ConcurrentHashMap** used in DefaultScriptEngine and DefaultRunnerService for thread-safe run tracking
- **ExecutorService.newCachedThreadPool()** dispatches script tasks
- **AtomicBoolean** for graceful stop requests

### JSON Persistence with Gson
`JsonStore` handles all file I/O with automatic directory creation:
```java
store.read(path, Class<T>) → Optional<T>
store.write(path, data) → Creates parent dirs, writes JSON
```
Use `@SerializedName` for snake_case JSON fields (see `LocalAuthProvider.UsersFile`).

## Build & Workflow

### Build
```
mvn clean package
```
Produces JAR: `target/duckbot-java-0.1.0-SNAPSHOT.jar` with all dependencies

### Run (Development)
**Option 1: Maven with JavaFX runtime (recommended)**
```
mvn javafx:run
```

**Option 2: Batch file (Windows)**
```
./run.bat
```

**Option 3: PowerShell (Windows)**
```
./run.ps1
```

**Option 4: JAR (requires Maven modules in classpath - not recommended for end users)**
```
java -jar target/duckbot-java-0.1.0-SNAPSHOT.jar
```

### Login Credentials
- **Username:** Duck
- **Password:** Aedyn2013

Auto-initialized on first launch; all data stored in `data/` directory.

### Key Maven Plugins
- **maven-compiler-plugin** – Java 17 source/target
- **javafx-maven-plugin** – Handles JavaFX runtime (`mvn javafx:run`)
- **maven-assembly-plugin** – Bundles all dependencies
- **maven-surefire-plugin** – Runs JUnit 5 tests with `useModulePath=false`

## Important Conventions

1. **Package structure mirrors domain** – `services/` interfaces in parent, `services/impl/` implementations
2. **All domain objects are mutable POJOs** – No getters/setters; direct field access for Gson compatibility
3. **Variables in scripts** – `ScriptContext.vars` is a `Map<String, Object>`; steps resolve via StringTemplate
4. **Logging** – Use `LogService` (optional in context); SLF4J backend configured for simple output to file
5. **Error handling** – Step execution failures propagate; RunStatus updated with error message
6. **Instance reservation** – Checked via `InstanceRegistry.reserve(name, runId)` before scheduling; released on stop

## Critical Files to Understand First
- `src/main/java/com/duckbot/app/DuckBotApp.java` – UI + bootstrap
- `src/main/java/com/duckbot/scripts/DefaultScriptEngine.java` – Async execution model
- `src/main/java/com/duckbot/services/impl/DefaultRunnerService.java` – Bot orchestration
- `src/main/java/com/duckbot/scripts/steps/TapStep.java` – Step pattern template
- `src/main/java/com/duckbot/services/impl/LocalAuthProvider.java` – JSON persistence + Argon2

## Testing
JUnit 5 tests run via `mvn test`. Test files follow `*Test.java` convention. Use Gson directly in tests for mock data serialization.

