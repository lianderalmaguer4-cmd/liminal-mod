@echo off
setlocal
set GRADLE_VERSION=8.12.1
set ROOT=%~dp0
set CACHE=%ROOT%.gradle-local
set DIST=%CACHE%\gradle-%GRADLE_VERSION%
set ZIP=%CACHE%\gradle-%GRADLE_VERSION%-bin.zip
if not exist "%DIST%\bin\gradle.bat" (
  echo [Liminal Space] Descargando Gradle %GRADLE_VERSION%...
  if not exist "%CACHE%" mkdir "%CACHE%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP%'"
  if errorlevel 1 exit /b 1
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ZIP%' '%CACHE%'"
  if errorlevel 1 exit /b 1
)
call "%DIST%\bin\gradle.bat" %*
endlocal
