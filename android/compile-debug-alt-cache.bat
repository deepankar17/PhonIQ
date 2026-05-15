@echo off
REM Use when Gradle executionHistory lock blocks default .gradle (e.g. another IDE/Gradle PID).
cd /d "%~dp0"
call gradlew.bat ":app:compileDebugKotlin" "-Pkotlin.incremental=false" "--project-cache-dir=%~dp0.gradle-build-alt" --no-daemon %*
