-- Quita únicamente la palabra DEMO de movimientos y metas de la cuenta de ejemplo.
BEGIN;

UPDATE transacciones
SET descripcion = btrim(regexp_replace(descripcion, '\mdemo\M[[:blank:]]*', '', 'gi'))
WHERE usuario_id = (SELECT id FROM usuarios WHERE email = 'demo@finanzas.test')
  AND descripcion ~* '\mdemo\M';

UPDATE metas_ahorro
SET nombre = btrim(regexp_replace(nombre, '\mdemo\M[[:blank:]]*', '', 'gi'))
WHERE usuario_id = (SELECT id FROM usuarios WHERE email = 'demo@finanzas.test')
  AND nombre ~* '\mdemo\M';

COMMIT;
