# Diagnóstico — Finanzas Personales (Fase 1)

Fecha: 2026-11-09 · Rama `ai/redesign` · Fuentes: auditorías de subagentes Explore (inventario, UML, BD) sobre textos originales y 4 PNG (FUN-01, LOG-01, ACT-01, DEP-01).

## 1. Stack detectado

| Elemento | Estado |
|---|---|
| Código fuente | **No existe** (sin package.json, pom.xml, *.java, *.py, *.sql, *.js) |
| App ejecutable | **No existe** → capturas Playwright "antes" no aplicables |
| Modelos | 3 `.vpp` en `modelo/` (principal `Finanzas_Personales_v2.vpp`) + `.lck` obsoleto; no parseados |
| Exportes | 10 PNG en `exportes/` con marca "evaluation copy"; sin XMI/XML ni carpeta `vp-export` |
| Stack planificado (solo diseño) | Java/Spring, PostgreSQL 16, JWT, nginx (`docs/04-desarrollo/VISTA_DESARROLLO.md`, ADR-001..003) |
| Requisitos | RF-01..24, HU-01..19, RNF-01..06 (dispersos en 01-contexto y 04-desarrollo), RN-01..12 |
| Mockups | 19 en `docs/08-requisitos/` (no revisados: NO VERIFICADO) |

## 2. Inventario de diagramas

| ID | Tipo | Estado |
|---|---|---|
| CTX-01 | Contexto | PNG (no revisado: NO VERIFICADO) |
| FUN-01 | Casos de uso | PNG revisado |
| FUN-02, LOG-02, DEV-02 | Componentes | PNG (no revisados: NO VERIFICADO) |
| ACT-01 | Actividades | PNG revisado |
| ACT-02, ACT-03 | Actividades | **No dibujados** |
| LOG-01 | Clases | PNG revisado |
| DEV-01 | Paquetes | PNG (NO VERIFICADO) |
| DEP-01, DEP-03 | Despliegue | DEP-01 revisado; DEP-03 NO VERIFICADO |
| SEQ-01..03 | Secuencia | **Solo texto** (`VISTA_FUNCIONAL.md:160-198`), sin PNG |
| ER | Entidad-relación | **No existe**; solo tablas en `VISTA_DESARROLLO.md:108-116` |

## 3. Problemas por gravedad

### Alta
| ID | Problema | Evidencia |
|---|---|---|
| P-01 | Diagramas de secuencia SEQ-01..03 sin dibujar; README los lista como existentes | `VISTA_FUNCIONAL.md:160-198`; `docs/README.md:51,64` |
| P-02 | No hay diagrama ER | `VISTA_DESARROLLO.md:108-116` |
| P-03 | PNG con marca de agua "evaluation copy" | `docs/README.md:55`; `exportes/*.png` |
| P-04 | RN-04 (categoría propia o predeterminada) sin garantía en el esquema; el aislamiento depende solo del repositorio (crítico para RNF-02) | `REQUERIMIENTOS_FUNCIONALES.md:24`; `VISTA_DESARROLLO.md:102` |
| P-05 | RN-03: la FK de movimiento→categoría no incluye `tipo` (gasto con categoría de ingreso posible) | `VISTA_DESARROLLO.md:113` |
| P-06 | Categorías predeterminadas duplicables. En las predeterminadas `id_propietario` es NULL y en PostgreSQL `NULL ≠ NULL`, así que `UNIQUE(id_propietario,tipo,nombre)` admite dos "Comida/GASTO" predeterminadas. Tampoco distingue "comida" de "Comida" y no hay CHECK de longitud 1-40 (RN-09). Además, nada impide crear una propia con el mismo nombre que una predeterminada (ambigüedad en RF-14/15: NO VERIFICADO si está permitido) | `VISTA_DESARROLLO.md:112`; `REQUERIMIENTOS_FUNCIONALES.md:29` |
| P-07 | Estado ELIMINADA (soft delete) contradice `ON DELETE CASCADE` | `VISTA_LOGICA.md:137`; `VISTA_DESARROLLO.md:111` |

### Media
| ID | Problema | Evidencia |
|---|---|---|
| P-08 | 10 de 11 CU sin diagrama de comportamiento (solo CU-03→ACT-01) | `TRAZABILIDAD.md:25` |
| P-09 | ACT-01: particiones no son swimlanes UML; "Notificar" en columna BD; barra sin función; decisión=merge; faltan flujos 5b, 6a, *a | ACT-01 png; `VISTA_FUNCIONAL.md:61-64,131` |
| P-10 | FUN-01: CU-10 extiende solo CU-03; RF-12/RF-20 exigen reevaluar al modificar (CU-04) | `VISTA_FUNCIONAL.md:94-96`; `REQUERIMIENTOS_FUNCIONALES.md:141,216` |
| P-11 | Actor "Reloj del sistema" no dibujado; estereotipo del servicio de correo distinto en FUN/CTX/DEP | `VISTA_FUNCIONAL.md:12-13`; `VISTA_CONTEXTO.md:22`; `VISTA_DESPLIEGUE.md:15` |
| P-12 | Firmas incoherentes SEQ vs LOG-02 (`enviarAlertaPresupuesto`, `obtenerResumen`, `sumarPorCategoria`); `CategoriaNoDisponibleException` inexistente en LOG | `VISTA_FUNCIONAL.md:175-192`; `VISTA_LOGICA.md:170-193` |
| P-13 | SEQ-03 (login) sin comprobación de bloqueo (RF-03/04) | `VISTA_FUNCIONAL.md:198` |
| P-14 | Puertos sin RF: `obtenerMovimiento`, `IGestionPresupuestos.eliminar` | `VISTA_LOGICA.md:169-170` |
| P-15 | Esquema vs clases: faltan `nombre` (usuario), `fecha_registro`, `icono`; FK no NOT NULL pese a multiplicidad 1 | `VISTA_DESARROLLO.md:111-114`; `VISTA_LOGICA.md:41,94,146-149` |
| P-16 | `refresh_token` sin `reemplazado_por` (detección de reutilización RF-05); `revocado` booleano | `VISTA_DESARROLLO.md:115-116` |
| P-17 | Faltan CHECK de RN-01 (monto máx.) y RN-06 (correo en minúsculas); moneda mono/multi sin decidir en ADR | `VISTA_DESARROLLO.md:111,113`; ADR-003 |
| P-18 | Bloqueo redundante (`estado=BLOQUEADA` + `bloqueadoHasta`); tipos temporales sin declarar | `VISTA_LOGICA.md:45-48` |
| P-19 | Presupuesto: sin estrategia upsert ni control de concurrencia de `notificado` (RN-12) | `REQUERIMIENTOS_FUNCIONALES.md:200` |
| P-20 | Índices no cerrados; falta índice para SUM por categoría (RF-19) | `VISTA_DESARROLLO.md:104` |
| P-21 | `AUDITORIA.md:19` declara ACT/SEQ corregidos sin entregarlos | `docs/00-auditoria/AUDITORIA.md:19` |
| P-22 | `db_audit.md` viene de una ejecución previa y no leyó `VISTA_DESARROLLO.md` §4.3 (l. 108-116). Errores: **DB-01** dice que faltan tablas de sesión y recuperación, pero existen `refresh_token` y `token_recuperacion` (l. 115-116). **DB-02** dice que no se sabe dónde va la moneda, pero está `moneda CHAR(3)` (l. 113). **DB-05** dice que no se decidió el mapeo de herencia, pero §4.3 fija tabla única con discriminador `tipo` (l. 113). **DB-11** dice que falta el UNIQUE de presupuesto, pero existe (l. 114). Además, el DDL que propone (l. 228-231) tiene `CHECK (tipo='GASTO' AND metodo_pago IN (...))`. Con `metodo_pago` NULL, `IN` da NULL y el CHECK pasa, así que se aceptan gastos sin método de pago. Además, CASCADE (usuario→categoria) junto con RESTRICT (movimiento→categoria) puede hacer fallar el borrado de un usuario (NO VERIFICADO, sin ejecutar). Decisión: se conserva como histórico y lo sustituye un DDL nuevo marcado "propuesta" | `docs/diagnostico/db_audit.md:36-38,201,226,228-231` |

### Baja
| ID | Problema | Evidencia |
|---|---|---|
| P-23 | LOG-01: asociaciones R8-R10 no dibujadas; atributos duplican asociaciones; OCL sin RN-01/02; tipos de librería sin modelar | `VISTA_LOGICA.md:61-64,109,121,151-153` |
| P-24 | DEP-01: artefactos fuera de la tabla 5.3; ubicación de migraciones V1/V2 incoherente | `VISTA_DESPLIEGUE.md:14-41` |
| P-25 | Nomenclatura de columnas mixta (`id_categoria` / `categoria_id`) | `VISTA_DESARROLLO.md:104,113` |
| P-26 | README cita `gen_full.py`, `vpgen.py` y SVG inexistentes; `modelo/README` ausente; 3 `.vpp` + `.lck`; imágenes legacy con FUN-01/02 intercambiados | `docs/README.md:26-40` |
| P-27 | Precondición de FUN-01 omite CU-07; CU-02 no detalla renovar/cerrar sesión (RF-05/06) | `VISTA_FUNCIONAL.md:76,105` |

**NO VERIFICADO:** contenido de los `.vpp`; PNG CTX-01, FUN-02, LOG-02, DEV-01, DEV-02, DEP-03; `.docx` y mockups de requisitos; comportamiento real del DDL (nunca ejecutado).

## 4. Plan

| Prioridad | Acción | Problemas |
|---|---|---|
| P0 | Dibujar SEQ-01..03 (Mermaid) con firmas alineadas a LOG-02 y comprobación de bloqueo | P-01, P-12, P-13 |
| P0 | Crear el diagrama ER (Mermaid) y un DDL de referencia `docs/architecture/V1__esquema.propuesta.sql`, marcado "PROPUESTA (no es original)", con estas restricciones: **RN-01** `monto NUMERIC(14,2) CHECK (monto > 0 AND monto <= 999999999999.99)`. **RN-03** `UNIQUE(id, tipo)` en categoria y FK compuesta `movimiento(id_categoria, tipo) → categoria(id, tipo)`. **RN-04** FK compuesta `(id_categoria, propietario)` o trigger `BEFORE INSERT/UPDATE` que exija `categoria.id_propietario IS NULL OR = movimiento.id_usuario`. **RN-06** `CHECK (correo = lower(correo))` y `UNIQUE(correo)`. **RN-09** `CHECK (char_length(nombre) BETWEEN 1 AND 40)` e índices únicos parciales `(id_propietario, tipo, lower(nombre)) WHERE id_propietario IS NOT NULL` y `(tipo, lower(nombre)) WHERE id_propietario IS NULL`. **Moneda** `CHECK (moneda = 'COP')`. **Gasto** `CHECK ((tipo='GASTO' AND metodo_pago IS NOT NULL) OR (tipo='INGRESO' AND metodo_pago IS NULL))` | P-02, P-04..P-07, P-15, P-17 |
| P0 | Corregir o sustituir `db_audit.md`; marcar AUDITORIA:19 como desactualizado (en doc nueva, sin tocar original) | P-21, P-22 |
| P1 | Rehacer ACT-01 con swimlanes reales y flujos alternativos; añadir ACT-02/03 | P-08, P-09 |
| P1 | Corregir FUN-01 (extend CU-04, actor Reloj, estereotipos) en Mermaid | P-10, P-11, P-27 |
| P1 | Sesiones, upsert de presupuesto, índices, bloqueo | P-16, P-18..P-20 |
| P1 | Reexportar PNG sin marca de agua (requiere licencia VP: **falta herramienta**) o sustituir por Mermaid | P-03 |
| P2 | Limpiar LOG-01, DEP-01, nomenclatura, README | P-14, P-23..P-26 |

## 5. Decisiones del usuario (aprobación 2026-11-09)
| Tema | Decisión |
|---|---|
| Alcance | Auditoría UML/BD; SEQ-01..03 y ER en Mermaid; DDL corregido marcado "propuesta"; rehacer ACT-01 y añadir ACT-02/03; plan de pruebas y pruebas SQL del esquema (se ejecutan solo si hay PostgreSQL o SQLite) |
| Fuera de alcance | Backend Java/Spring y cualquier dependencia nueva |
| **Supuesto: moneda** | COP única: `moneda CHAR(3) DEFAULT 'COP' CHECK (moneda='COP')` |
| Diagramas | Mermaid en `docs/evidencias/diagramas/` y `docs/evidencias/comparaciones/` |
| Mockups | Muestreo de 5 de 19 |
| Marca de agua VP | Se sustituye por Mermaid; no hay reexportación |
