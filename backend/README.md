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

### Linux / macOS

```bash
cd /path/to/Polygraphic-Centre/backend
./gradlew clean build
./gradlew bootRun
```

### Windows

```powershell
cd C:\path\to\Polygraphic-Centre\backend
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
