-- Bloqueo temporal de la cuenta tras intentos de login fallidos.
ALTER TABLE usuarios ADD COLUMN intentos_fallidos INTEGER NOT NULL DEFAULT 0;
ALTER TABLE usuarios ADD COLUMN bloqueado_hasta TIMESTAMP;
