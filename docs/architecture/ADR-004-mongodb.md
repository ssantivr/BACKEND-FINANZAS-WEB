# ADR-004 — MongoDB como base de datos (reemplaza a ADR-003)

- **Estado:** aceptada (pedido del usuario, 2026-11-17).
- **Reemplaza a:** [ADR-003 PostgreSQL](../07-decisiones/ADR-003-postgresql.md). El ADR-003 original no se modifica.

## Contexto

El proyecto pide implementar el backend en Java con **MongoDB**, el frontend en TypeScript desplegado en **Vercel** y las pruebas de la API en **Postman**. El diseño hexagonal (ADR-002) aísla la persistencia detrás de los puertos `IRepositorio*`, así que el cambio de motor solo afecta a los adaptadores de `infraestructura.persistencia`.

## Decisión

- **MongoDB 7** con Spring Data MongoDB; en producción, **MongoDB Atlas** M0.
- Una colección por agregado: `usuarios`, `categorias`, `movimientos`, `presupuestos`, más `sesiones` y `tokens_recuperacion` para la seguridad.
- Referencias por `_id` (UUID en texto), no documentos embebidos: las categorías se renombran y se reasignan (RF-16, RF-17), y cada movimiento debe ver el nombre actual.
- Montos en **Decimal128** (`@Field(targetType = DECIMAL128)`), nunca `double` (RNF-05).
- Fechas de movimientos como texto ISO `AAAA-MM-DD`: se ordenan y filtran bien y no dependen de la zona horaria del servidor.
- Unicidad e integridad con índices únicos (correo, nombre de categoría, presupuesto por mes) y validación `$jsonSchema` (`base-de-datos/01_colecciones.js`).
- Sesiones y enlaces vencidos se borran con índices **TTL**.

## Consecuencias

| Tema | Con PostgreSQL (ADR-003) | Con MongoDB |
|---|---|---|
| Integridad referencial | Claves foráneas `ON DELETE RESTRICT` | La garantizan los servicios: reasignar antes de borrar una categoría y borrar sus presupuestos (RNF-05) |
| Transacciones | ACID en cada caso de uso | Cada escritura es atómica por documento. Eliminar una categoría hace 3 escrituras sin transacción: si falla a la mitad, se puede repetir sin efectos nuevos (idempotente) |
| Migraciones | Flyway `V1__`, `V2__` | `base-de-datos/*.js` (idempotentes) y el arranque de la API, que crea índices y categorías |
| Agregaciones | `GROUP BY` | Pipeline `$match` → `$group` (`RepositorioMovimientosAdaptador`) |

La propuesta `docs/architecture/V1__esquema.propuesta.sql` queda como referencia histórica; ya no se usa.
