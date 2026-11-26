@echo off
setlocal
cd /d "%~dp0"
echo Preparando solamente la cuenta demo en el entorno local verificado...
python "%~dp0inicio\preparar-demo.py" --aplicar
if errorlevel 1 (
    echo No se completo la carga. Inicia FINDEX con INICIAR.cmd y revisa el error.
    pause
    exit /b 1
)
echo Carga verificada. Las claves no se muestran en este proceso.
pause
