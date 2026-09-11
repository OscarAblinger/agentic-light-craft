---
name: elevated-gradle-run
description: Run compilation, testing, and Gradle build workflows safely inside the restricted workspace-write sandbox without triggering C:\Users\Oscar\.gradle file locks.
---

# Run Gradle Task

Use this skill whenever you need to clean, build, compile, or test the Java/Kotlin application using Gradle. It prevents sandbox permission denials by isolating the Gradle home directory to the local workspace.

## Context & Constraints

The environment is strictly confined to `workspace-write` mode. Accessing or creating lock files (`.lck`) inside the host user profile `C:\Users\Oscar\.gradle\` is blocked by the runtime kernel.

To bypass this restriction, **you must explicitly configure the Gradle environment locally** during execution.

## Instructions

When the user requests a Gradle workflow (e.g., `build`, `test`, `clean`), choose the appropriate execution block for the active shell and execute it directly in the workspace root.

### 1. Windows PowerShell Execution
If running inside a PowerShell terminal (`dsh-tool-pwsh`), execute your command by prepending the local environment block exactly like this:

```powershell
\$env:GRADLE_USER_HOME =Join-Path (Get-Location) ".gradle_home"
\(env:ORG_GRADLE_PROJECT_gradleUserHome =\)env:GRADLE_USER_HOME
if (Test-Path ".\gradlew.bat") {
    .\gradlew.bat <TASK> <ARGUMENTS> --no-daemon
} else {
    Write-Error "gradlew.bat wrapper missing from workspace root."
}
```

### 2. Windows CMD Execution
If running inside a classic Windows Command Prompt tool, execute your command using this format:

```cmd
set GRADLE_USER_HOME=%CD%\.gradle_home
set ORG_GRADLE_PROJECT_gradleUserHome=%CD%\.gradle_home
if exist gradlew.bat (
    gradlew.bat <TASK> <ARGUMENTS> --no-daemon
} else (
    echo Error: gradlew.bat wrapper missing from workspace root.
    exit /b 1
)
```

### 3. Unix/Linux/macOS Bash Execution
If running inside a standard Unix terminal environment, execute using this block:

```bash
export GRADLE_USER_HOME="\$PWD/.gradle_home"
export ORG_GRADLE_PROJECT_gradleUserHome="\$PWD/.gradle_home"
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    ./gradlew <TASK> <ARGUMENTS> --no-daemon
else
    echo "Error: gradlew wrapper missing from workspace root."
    exit 1
fi
```

## Examples

### Example A: Running a standard build
* **Action:** Build the project while skipping tests.
* **Command (PowerShell):**
  ```powershell
  \$env:GRADLE_USER_HOME =Join-Path (Get-Location) ".gradle_home"; \(env:ORG_GRADLE_PROJECT_gradleUserHome =\)env:GRADLE_USER_HOME; .\gradlew.bat build -x test --no-daemon
  ```

### Example B: Cleaning the workspace
* **Action:** Reset previous compilation states.
* **Command (CMD):**
  ```cmd
  set GRADLE_USER_HOME=%CD%\.gradle_home && set ORG_GRADLE_PROJECT_gradleUserHome=%CD%\.gradle_home && gradlew.bat clean --no-daemon
  ```
