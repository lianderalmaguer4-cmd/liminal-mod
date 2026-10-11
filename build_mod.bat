@echo off
cd /d "%~dp0"
echo ============================================
echo   LIMINAL SPACE - Minecraft 1.21.4
 echo   Compilando mod Fabric...
echo ============================================
call gradlew.bat build
if errorlevel 1 (
  echo.
  echo ERROR: La compilacion fallo. Revisa el mensaje anterior.
  pause
  exit /b 1
)
echo.
echo LISTO. El mod esta en build\libs\
dir /b build\libs\*.jar
echo.
pause
