@echo off
setlocal
set "PATH=%~dp0..\.tools\apache-maven-3.10.1\bin;%PATH%"
cd /d "%~dp0..\backend"
if errorlevel 1 goto fallo
echo La primera ejecucion necesita Internet para descargar dependencias.
call mvn.cmd "-Dmaven.repo.local=%~dp0..\.tools\maven-repository" "-Dspring-boot.run.profiles=demo-local" spring-boot:run
if errorlevel 1 goto fallo
exit /b 0
:fallo
echo No se pudo iniciar el backend. Revisa el error de arriba.
pause
exit /b 1
