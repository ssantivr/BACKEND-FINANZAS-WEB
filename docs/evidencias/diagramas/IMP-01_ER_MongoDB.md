# IMP-01 — Modelo de datos implementado (MongoDB)

Reemplaza al ER relacional de [ER.md](ER.md), que se conserva como histórico de la fase de diseño ([ADR-004](../../architecture/ADR-004-mongodb.md)).

**Fuentes:**
- Campos y reglas: [`base-de-datos/01_colecciones.js`](../../../base-de-datos/01_colecciones.js), que define la validación `$jsonSchema`.
- Índices: [`base-de-datos/02_indices.js`](../../../base-de-datos/02_indices.js).

**Renderizado:** verificado con mermaid y Playwright. La imagen está en [img/IMP-01_ER_MongoDB.svg](img/IMP-01_ER_MongoDB.svg).

Cada colección es un agregado. Las relaciones son referencias por `_id` (UUID en texto), no documentos embebidos. Así, al renombrar o reasignar una categoría (RF-16 y RF-17), todos los movimientos ven el nombre nuevo.

```mermaid
erDiagram
  usuarios ||--o{ categorias : "idPropietario (vacío = predeterminada)"
  usuarios ||--o{ movimientos : idUsuario
  usuarios ||--o{ presupuestos : idUsuario
  usuarios ||--o{ sesiones : idUsuario
  usuarios ||--o{ tokens_recuperacion : idUsuario
  categorias ||--o{ movimientos : idCategoria
  categorias ||--o{ presupuestos : idCategoria

  usuarios {
    string _id PK "UUID"
    string correo UK "minúsculas, RN-06"
    string nombre "2 a 80"
    string hashContrasena "BCrypt, RN-07"
    string estado "ACTIVA | ELIMINADA"
    int intentosFallidos "0 a 5, RN-08"
    date bloqueadoHasta
    date fechaRegistro
    date eliminadaEn "borrado lógico"
  }
  categorias {
    string _id PK
    string nombre "1 a 40, RN-09"
    string nombreClave UK "minúsculas"
    string tipo "GASTO | INGRESO"
    string icono
    string color "#rrggbb"
    string idPropietario FK
  }
  movimientos {
    string _id PK
    string idUsuario FK
    string tipo "GASTO | INGRESO"
    decimal monto "Decimal128, RNF-05"
    string moneda "COP"
    string fecha "yyyy-MM-dd"
    string idCategoria FK
    string descripcion "máx 140"
    string metodoPago "solo gastos"
    string fuente "solo ingresos"
    date fechaRegistro
  }
  presupuestos {
    string _id PK
    string idUsuario FK
    string idCategoria FK
    string periodo "yyyy-MM"
    decimal limite "Decimal128"
    string moneda
    bool notificado "RN-12"
  }
  sesiones {
    string _id PK
    string idUsuario FK
    string hash UK "SHA-256 del token"
    date expiraEn "TTL"
    date reemplazadaEn "rotado, 30 s de gracia"
  }
  tokens_recuperacion {
    string _id PK
    string idUsuario FK
    string hash UK "SHA-256"
    date expiraEn "TTL, 30 min"
    bool usado "un solo uso"
  }
```

## Índices

| Colección | Índice | Campos | Para qué |
|---|---|---|---|
| usuarios | `uq_usuario_correo` (único) | correo | RN-06: un correo por cuenta |
| categorias | `uq_categoria_nombre` (único) | idPropietario, tipo, nombreClave | RN-09: sin nombres repetidos |
| movimientos | `ix_mov_usuario_fecha` | idUsuario, fecha ↓, fechaRegistro ↓ | Historial paginado |
| movimientos | `ix_mov_usuario_categoria_fecha` | idUsuario, idCategoria, fecha ↓, fechaRegistro ↓ | Filtro por categoría y consumo de presupuestos |
| presupuestos | `uq_presupuesto` (único) | idUsuario, idCategoria, periodo | RN-10: un límite por categoría y mes |
| presupuestos | `ix_presupuesto_periodo` | idUsuario, periodo | Presupuestos del mes |
| sesiones | `uq_sesion_hash`, `ix_sesion_usuario`, `ttl_sesion` | hash / idUsuario / expiraEn | Renovar, cerrar todas, caducidad automática |
| tokens_recuperacion | `uq_recuperacion_hash`, `ttl_recuperacion` | hash / expiraEn | Enlace de un solo uso que caduca solo |
