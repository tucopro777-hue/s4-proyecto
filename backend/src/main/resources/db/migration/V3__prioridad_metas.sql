-- Prioridad de cada meta para la asignación inteligente del dinero disponible (1 = baja, 10 = alta).
ALTER TABLE metas_ahorro ADD COLUMN prioridad INTEGER NOT NULL DEFAULT 5;
ALTER TABLE metas_ahorro ADD CONSTRAINT ck_meta_prioridad CHECK (prioridad BETWEEN 1 AND 10);
