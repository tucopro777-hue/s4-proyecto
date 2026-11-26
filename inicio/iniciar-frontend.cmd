@echo off
setlocal
set "PATH=%~dp0..\.tools\node-v20.20.2-win-x64;%PATH%"
cd /d "%~dp0..\frontend"
if errorlevel 1 goto fallo
if not exist node_modules\@angular\cli\bin\ng.js (
    call npm.cmd ci --no-audit --no-fund
    if errorlevel 1 goto fallo
)
echo Abre http://localhost:4200 cuando termine de compilar.
call npm.cmd start
if errorlevel 1 goto fallo
exit /b 0
:fallo
echo No se pudo iniciar el frontend. Revisa el error de arriba.
pause
exit /b 1
