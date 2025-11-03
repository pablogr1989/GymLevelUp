@echo off
echo ================================================
echo        GymLog - Script de Compilacion          
echo ================================================
echo.

REM Verificar si existe gradlew
if not exist "gradlew.bat" (
    echo Generando wrapper de Gradle...
    gradle wrapper --gradle-version=8.2
)

REM Limpiar proyecto anterior
echo Limpiando compilaciones anteriores...
call gradlew.bat clean

REM Compilar el proyecto
echo.
echo Compilando aplicacion...
call gradlew.bat assembleDebug

REM Verificar si la compilacion fue exitosa
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ================================================
    echo      Compilacion exitosa!                     
    echo ================================================
    echo.
    echo APK generado en:
    echo app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo Para instalar en un dispositivo conectado, ejecuta:
    echo gradlew.bat installDebug
    echo.
) else (
    echo.
    echo ================================================
    echo      Error en la compilacion                   
    echo ================================================
    echo Revisa los mensajes de error anteriores
)
pause
