@echo off
setlocal
echo Comprobando los puertos del proyecto...
netstat -ano -p tcp | findstr /R /C:":8080 .*LISTENING" >nul
if errorlevel 1 (
    start "Backend - Finanzas" cmd.exe /d /k ""%~dp0inicio\iniciar-backend.cmd""
) else (
    echo El puerto 8080 ya esta ocupado. No se inicia otro backend.
)
netstat -ano -p tcp | findstr /R /C:":4200 .*LISTENING" >nul
if errorlevel 1 (
    start "Frontend - Finanzas" cmd.exe /d /k ""%~dp0inicio\iniciar-frontend.cmd""
) else (
    echo El puerto 4200 ya esta ocupado. No se inicia otro frontend.
)
echo.
echo Cuando Angular termine de compilar, abre http://localhost:4200/
echo Conserva abiertas las ventanas de los servidores.
pause
