# ADR-003 — PostgreSQL 16 como base de datos

- **Estado:** Aceptada
- **Contexto:** Datos fuertemente relacionales (usuario → movimientos → categoría), necesidad de
  precisión decimal exacta, restricciones de integridad (`CHECK`, `UNIQUE`, `FK ON DELETE CASCADE`) y
  agregaciones por período/categoría.
- **Decisión:** PostgreSQL 16 con `NUMERIC(14,2)` para montos, `UUID` como claves, migraciones Flyway e
  índices compuestos por `id_usuario`.
- **Alternativas descartadas:** MongoDB (pierde integridad referencial y transacciones simples entre
  colecciones para este caso); SQLite (no apto para acceso concurrente desde servidor).
- **Consecuencias:** (+) integridad garantizada en la BD además del dominio; consultas de resumen en SQL.
  (−) requiere administración de respaldos (resuelto con `respaldo.sh` + almacenamiento externo).
