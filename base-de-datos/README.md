# Base de datos — MongoDB

La aplicación guarda los datos en **MongoDB** (ver [ADR-004](../docs/architecture/ADR-004-mongodb.md)). En producción se usa **MongoDB Atlas** (plan gratuito M0).

## Colecciones

| Colección | Contenido | Índices |
|---|---|---|
| `usuarios` | Cuenta, hash BCrypt, intentos fallidos, borrado lógico | `uq_usuario_correo` (único) |
| `categorias` | Predeterminadas (sin `idPropietario`) y propias; cada una con `color` `#rrggbb` para la interfaz | `uq_categoria_nombre` (único por propietario, tipo y nombre) |
| `movimientos` | Gastos e ingresos; `monto` en Decimal128, `fecha` en texto `AAAA-MM-DD` | `ix_mov_usuario_fecha`, `ix_mov_usuario_categoria_fecha` |
| `presupuestos` | Límite por usuario, categoría y mes (`AAAA-MM`) | `uq_presupuesto` (único), `ix_presupuesto_periodo` |
| `sesiones` | Hash SHA-256 de cada token de renovación; al rotarlo, el anterior queda 30 s (`reemplazadaEn`) para un solo uso más | `uq_sesion_hash`, `ix_sesion_usuario`, TTL `ttl_sesion` |
| `tokens_recuperacion` | Hash de los enlaces de recuperación (30 min, un uso) | `uq_recuperacion_hash`, TTL `ttl_recuperacion` |

Las referencias entre colecciones son por `_id` (UUID en texto). No se guardan contraseñas ni tokens en claro.

## Scripts

| Archivo | Qué hace |
|---|---|
| `01_colecciones.js` | Crea las colecciones con validación `$jsonSchema` (o la actualiza si ya existen) |
| `02_indices.js` | Crea los índices, incluidos los TTL que borran sesiones y enlaces vencidos |
| `03_categorias_predeterminadas.js` | Carga las 14 categorías predeterminadas (RF-14) |
| `04_consultas_de_ejemplo.js` | Las consultas del historial, el resumen y el presupuesto, para probarlas a mano |
| `05_datos_demo.mjs` | Solo en local: crea por la API una cuenta demo con 18 movimientos, 2 categorías propias y 4 presupuestos |

```bash
mongosh "mongodb+srv://USUARIO:CLAVE@CLUSTER.mongodb.net/finanzas" base-de-datos/01_colecciones.js
mongosh "mongodb+srv://USUARIO:CLAVE@CLUSTER.mongodb.net/finanzas" base-de-datos/02_indices.js
mongosh "mongodb+srv://USUARIO:CLAVE@CLUSTER.mongodb.net/finanzas" base-de-datos/03_categorias_predeterminadas.js
```

La API también crea los índices y las categorías predeterminadas al arrancar, así que una base vacía funciona sin correr los scripts. Los scripts agregan la validación de esquema, que la API no crea.

## Usar MongoDB en este equipo

MongoDB Server 9.0, `mongosh`, MongoDB Compass y Postman están instalados con `winget`. MongoDB corre como servicio de Windows (`MongoDB`, inicio automático) en `mongodb://localhost:27017`.

```bash
mongosh mongodb://localhost:27017/finanzas base-de-datos/01_colecciones.js
```

```bash
node base-de-datos/05_datos_demo.mjs
```

- Corre los scripts `01` a `03` igual que el primero.
- `05_datos_demo.mjs` necesita la API encendida en `http://localhost:8080`. La cuenta demo (`demo@pulsofinance.local`) y su contraseña de prueba están al inicio del script. Es solo para tu equipo.
- Para ver los datos en **Compass**: *New connection* → `mongodb://localhost:27017` → base `finanzas`.

## Crear la base en MongoDB Atlas

1. En [cloud.mongodb.com](https://cloud.mongodb.com) crea un clúster **M0 (gratis)**.
2. *Database Access*: crea un usuario con rol `readWrite` sobre la base `finanzas`.
3. *Network Access*: permite `0.0.0.0/0` (Render no tiene IP fija en el plan gratuito).
4. *Connect → Drivers*: copia la URI y agrégale el nombre de la base: `mongodb+srv://USUARIO:CLAVE@CLUSTER.mongodb.net/finanzas?retryWrites=true&w=majority`.
5. Esa URI va en la variable `MONGODB_URI` del backend. **No la subas al repositorio.**

## Estado de verificación

- La API contra MongoDB 7.0 (embebido) está probada: `docs/evidencias/pruebas/backend-mvn-test.log`.
- Los scripts `01` a `04` corrieron sin errores en MongoDB 9.0.4 local con `mongosh` 2.14.1: `docs/evidencias/pruebas/mongodb-scripts.log`.
- La colección de Postman pasó completa contra la API conectada a ese MongoDB, con la validación `$jsonSchema` activa: `docs/evidencias/pruebas/postman-newman.log`.
- Datos demo y documentos de muestra: `docs/evidencias/pruebas/datos-demo.log`, `mongodb-conteos.log`, `mongodb-datos-demo.log`.
- Atlas **NO VERIFICADO**: falta crear el clúster.

> En este repositorio público no se incluye `05_datos_demo.mjs`, porque guarda la contraseña de la cuenta demo. Está en el repositorio del proyecto completo.
