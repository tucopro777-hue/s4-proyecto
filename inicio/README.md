# Inicio y preparación local

- `iniciar-backend.cmd`: inicia Spring Boot con el perfil demo-local y Maven local.
- `iniciar-frontend.cmd`: inicia Angular utilizando Node local.
- `preparar-demo.py`: comprueba el entorno y prepara la cuenta demo con `--aplicar`.
- `cargar-historial-demo.ps1`: acceso compatible al preparador de demo.
- `demo-completa-periodo.json`: período ancla de la demo.
- `demo-completa-reporte.json` y `demo-completa-primera-carga.json`: reportes de referencia de la preparación.
- `.demo-completa.lock`: control utilizado por el preparador.

Los archivos de estado y referencia se mantienen aquí porque los scripts los utilizan. Los logs de servidores activos permanecen aquí mientras están abiertos. Las evidencias históricas se encuentran en `../evidencias/`.
