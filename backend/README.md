# Polygraphic Centre Backend

This backend is a Spring Boot application generated with Spring Boot 4.0.5 and configured to use Java 25 via Gradle toolchains. The project uses the Gradle wrapper (`gradlew`) and Gradle 9.4.1.

## Requirements

- Java 25 SDK
- Git (optional, if you clone the repository)
- Internet access to download Gradle dependencies

## Recommended setup

The project is configured to use the Gradle wrapper, so you do not need to install Gradle globally.

### Linux

1. Install Java 25 JDK.
   - Ubuntu/Debian:
     ```bash
     sudo apt update
     sudo apt install openjdk-25-jdk
     ```
   - Fedora/RHEL:
     ```bash
     sudo dnf install java-25-openjdk-devel
     ```
2. Verify Java version:
   ```bash
   java -version
   ```
   It should report a Java 25 runtime.

### macOS

1. Install Java 25 JDK via Homebrew:
   ```bash
   brew install openjdk@25
   ```
2. Add Java 25 to your shell environment if needed:
   ```bash
   sudo ln -sfn /opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-25.jdk
   ```
3. Verify Java version:
   ```bash
   java -version
   ```
   It should report Java 25.

### Windows

1. Download and install a Java 25 JDK distribution from a trusted provider.
2. Set `JAVA_HOME` to the JDK install folder.
3. Add `%JAVA_HOME%\bin` to your `PATH`.
4. Verify the installation in PowerShell or Command Prompt:
   ```powershell
   java -version
   ```
   It should report Java 25.

## Build and Run

Open a terminal in the `backend` folder.

## Configuration

Spring Boot is configured here the standard way: through profile-specific properties and environment variables. For local development, copy [src/main/resources/application-local.properties.example](src/main/resources/application-local.properties.example) to `src/main/resources/application-local.properties`, then start the app with the `local` profile enabled.

The local properties file is the place for non-secret development defaults such as the PostgreSQL URL and the Firebase Admin service-account resource name. Keep the actual `firebase-service-account.json` file local only; it is ignored by Git.

For the local PostgreSQL instance launched from `db/scripts-podman/`, the backend expects the database to be reachable on `localhost:5433`.

If you prefer environment variables instead of a local properties file, Spring Boot can read them directly, for example `SPRING_DATASOURCE_URL` or `FIREBASE_ADMIN_SERVICE_ACCOUNT_RESOURCE`.

### Podman-based local database

If you use Podman instead of Docker, start the database with the mirrored scripts in `db/scripts-podman/`:

```bash
./db/scripts-podman/start.sh
```

The Podman scripts use the same database name, credentials, seed data, and dump/restore flow as the Docker scripts, but they connect through Podman.

### Linux / macOS

```bash
cd /path/to/Polygraphic-Centre/backend
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
SPRING_PROFILES_ACTIVE=local ./gradlew clean build
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

### Windows

```powershell
cd C:\path\to\Polygraphic-Centre\backend
Copy-Item src/main/resources/application-local.properties.example src/main/resources/application-local.properties
$env:SPRING_PROFILES_ACTIVE = "local"
.\gradlew.bat clean build
.\gradlew.bat bootRun
```

The application will start on `http://localhost:8080` by default.

## Common Gradle commands

- Build the project:
  ```bash
  ./gradlew build
  ```
- Run tests:
  ```bash
  ./gradlew test
  ```
- Start the application:
  ```bash
  ./gradlew bootRun
  ```

## Notes

- The project uses Java toolchains, so it requires a JDK 25 installation on the host machine.
- If you prefer not to install Gradle globally, use the included wrapper scripts (`gradlew` / `gradlew.bat`).
- If you see a Java version mismatch, confirm that the `java` command points to Java 25 and not an older version.
- Use `application-local.properties` for local-only defaults and environment variables for secrets or deployment-specific values.
- The Firebase Admin SDK service-account file is expected on the classpath by default as `firebase-service-account.json`.
