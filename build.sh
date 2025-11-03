#!/bin/bash

echo "================================================"
echo "       GymLog - Script de Compilación          "
echo "================================================"
echo ""

# Verificar si existe gradlew
if [ ! -f "./gradlew" ]; then
    echo "Generando wrapper de Gradle..."
    gradle wrapper --gradle-version=8.2
fi

# Dar permisos de ejecución
chmod +x ./gradlew

# Limpiar proyecto anterior
echo "Limpiando compilaciones anteriores..."
./gradlew clean

# Compilar el proyecto
echo ""
echo "Compilando aplicación..."
./gradlew assembleDebug

# Verificar si la compilación fue exitosa
if [ $? -eq 0 ]; then
    echo ""
    echo "================================================"
    echo "     ¡Compilación exitosa!                     "
    echo "================================================"
    echo ""
    echo "APK generado en:"
    echo "app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "Para instalar en un dispositivo conectado, ejecuta:"
    echo "./gradlew installDebug"
    echo ""
else
    echo ""
    echo "================================================"
    echo "     Error en la compilación                   "
    echo "================================================"
    echo "Revisa los mensajes de error anteriores"
fi
