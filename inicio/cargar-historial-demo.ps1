# Compatibilidad: la carga anterior ahora pasa por las comprobaciones de entorno.
# No contiene claves y solo actúa sobre la cuenta demo existente.
$ErrorActionPreference = 'Stop'
python "$PSScriptRoot\preparar-demo.py" --aplicar
if ($LASTEXITCODE -ne 0) { throw 'No se completó la carga demo. Revisa el entorno y el mensaje anterior.' }
