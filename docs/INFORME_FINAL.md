# Informe final — Finanzas Personales (rediseño UML y base de datos)

Rama `ai/redesign` · 2026-11-09 · Fases 0 a 6 (ver `git log --oneline`).

> **Actualización 2026-11-21: la aplicación ya está implementada.** Las secciones 1 a 17 describen la fase de diseño (UML y esquema PostgreSQL propuesto) y se conservan como histórico. Por ejemplo, ya no es cierto que "no hay backend". El estado actual está en la sección 0.

## 0. Estado actual de la implementación (2026-11-21)

| Capa | Implementado | Verificación |
|---|---|---|
| Backend | Java 21 y Spring Boot 3.5, con arquitectura hexagonal (dominio, aplicación e infraestructura). Usa JWT con token de acceso en memoria y una cookie HttpOnly de renovación que rota en cada uso, con 30 s de gracia para pestañas paralelas. Los errores se devuelven como ProblemDetail con `codigo`. | `mvn test`: 31 pruebas, 0 fallos (dominio, servicios, ArchUnit e integración con MongoDB embebido). [backend-mvn-test.log](evidencias/pruebas/backend-mvn-test.log) |
| Base de datos | MongoDB (local 9.0.4, base `finanzas`) con validación `$jsonSchema`, montos `Decimal128`, índices compuestos e índices TTL para sesiones y tokens. Reemplaza al esquema PostgreSQL propuesto en la fase de diseño. | Scripts en `base-de-datos/` con su log en [mongodb-scripts.log](evidencias/pruebas/mongodb-scripts.log) |
| Frontend | React 18, TypeScript y Vite, con el diseño PulsoFinance de los mockups, modo claro, oscuro o del sistema, y diseño adaptable de 375 a 1320 px o más. | Build sin errores y 17 pruebas unitarias. [frontend-build-test.log](evidencias/pruebas/frontend-build-test.log) |
| API | Colección Postman con 44 peticiones en 7 carpetas. | newman: 82 aserciones, 0 fallos. [postman-newman.log](evidencias/pruebas/postman-newman.log) |
| Navegador | 12 pruebas Playwright en Microsoft Edge que recorren la vida completa de un usuario. Cubren HU-01, 02, 03, 04, 06, 09, 13, 14, 16, 17 y 20, más el modo oscuro y el móvil a 375 px. | 12 de 12 pasan. [frontend-e2e-playwright.log](evidencias/pruebas/frontend-e2e-playwright.log) |

Funciones añadidas en la revisión de noviembre:

- Búsqueda por texto en el historial (`GET /api/v1/movimientos?texto=`). Se hace en el servidor y abarca descripción, fuente y nombre de la categoría. El texto se trata como literal (`Pattern.quote`) y admite máximo 60 caracteres.
- RN-12: el aviso de presupuesto excedido solo se marca como enviado si hay un canal de correo. Si el gasto vuelve por debajo del límite, el aviso se rearma.
- En local, sin SMTP, el perfil `local` imprime en la consola de la API los correos de recuperación y de alerta (`app.correo.mostrar-en-consola`).
- Resumen: el promedio diario se calcula sobre los días transcurridos del periodo. El bloque de presupuestos indica a qué mes pertenece.
- Interfaz:
  - estados de carga y de error con opción de reintentar;
  - botones que se bloquean mientras guardan;
  - foco atrapado dentro de las ventanas emergentes;
  - saludo con el nombre del usuario;
  - animaciones de entrada y totales que cuentan hacia arriba, ambas desactivadas si el usuario pide reducir el movimiento.
- Seguridad (CSRF): `POST /auth/renovar` y `POST /auth/logout` se autentican solo con la cookie. Por eso exigen la cabecera `X-Requested-With: XMLHttpRequest`; sin ella responden 403 `ORIGEN_NO_PERMITIDO`. Lo prueban la integración y Postman.

Documentación de la implementación:

| Documento | Contenido |
|---|---|
| [IMP-01 ER MongoDB](evidencias/diagramas/IMP-01_ER_MongoDB.md) | Colecciones, campos, relaciones e índices reales. Reemplaza al ER relacional |
| [IMP-02 Componentes](evidencias/diagramas/IMP-02_Componentes_hexagonal.md) | Arquitectura hexagonal con los paquetes, puertos y clases del código |
| [IMP-03 Despliegue](evidencias/diagramas/IMP-03_Despliegue.md) | Vercel, Render y Atlas, y el entorno local con Postman y Compass |
| [IMP-04](evidencias/diagramas/IMP-04_SEQ_Buscar_historial.md), [IMP-05](evidencias/diagramas/IMP-05_SEQ_Renovar_sesion.md) | Secuencias de búsqueda en el historial y renovación de sesión con CSRF |
| [Trazabilidad](evidencias/TRAZABILIDAD_IMPLEMENTACION.md) | HU-01 a HU-22: pantalla, endpoint y pruebas de cada una |
| [Capturas](evidencias/capturas/) | 23 imágenes tomadas con Playwright: acceso y 6 páginas en claro y oscuro a 1366 px, el formulario y las 6 páginas en móvil a 390 px |
| [Guion de demostración](GUION_DEMO.md) | Demo de 10 minutos con preguntas probables |

Los 5 diagramas IMP se renderizaron con mermaid 11.4.1 en Edge, sin errores. El resultado está en [diagramas-render.log](evidencias/pruebas/diagramas-render.log) y las imágenes en `evidencias/diagramas/img/`. Los diagramas Mermaid de la fase de diseño (secciones 13 y 14) siguen sin renderizar.

Despliegue publicado (comprobado el 2026-11-21 con `curl`):

- **API en Render:** `https://finanzas-api.onrender.com/actuator/health` responde 404 con `x-render-routing: no-server`, así que **no hay ningún servicio en esa dirección**. Esa URL es la que aparece por defecto en `frontend/vercel.json`. Si la API se publicó con otro nombre, ese dato no está en el repositorio.
- **Web en Vercel:** el proyecto vinculado se llama `finanzas-personales`. `https://finanzas-personales.vercel.app` responde 200, pero con el título "Finanzas Personales" y no con el de esta versión ("PulsoFinance · Finanzas Personales"); además, `/api/v1/categorias` da 404. Que ese dominio sea el despliegue de este proyecto es **NO VERIFICADO**.
- **Conclusión:** la versión actual **no está publicada**. Para publicarla hay que crear el servicio en Render (`render.yaml`) con su cadena de MongoDB Atlas, poner su URL real en `frontend/vercel.json` y volver a desplegar en Vercel. Estos pasos usan las cuentas del autor y están en el README, en la sección "Desplegar".

## 1. Introducción

El proyecto contiene la documentación de diseño de una aplicación de finanzas personales (vistas 4+1, ADR y requisitos en `docs/00..08`). No existe código fuente ni aplicación ejecutable. Este trabajo audita los diagramas UML y el modelo de datos, propone correcciones y comprueba el esquema SQL propuesto en PostgreSQL.

Los originales no se modificaron: todo el trabajo nuevo está en `docs/diagnostico/`, `docs/architecture/` y `docs/evidencias/`. El backup íntegro está en `..\PROYECTO_FINAL_BACKUP_ORIGINAL`. La verificación por hash del backup **NO VERIFICADO** en este informe.

## 2. Objetivo

Según la decisión de alcance del 2026-11-09 ([diagnostico/diagnostico.md](diagnostico/diagnostico.md), §5):

- Auditar UML y base de datos frente a los requisitos.
- Dibujar SEQ-01..03 y el ER en Mermaid, y rehacer ACT-01 añadiendo ACT-02 y ACT-03.
- Entregar un DDL corregido, marcado como propuesta, con pruebas SQL.
- Fuera de alcance: backend Java/Spring y dependencias nuevas.

## 3. Diagnóstico

| Elemento | Estado (fuente: [diagnostico](diagnostico/diagnostico.md) §1) |
|---|---|
| Código fuente | No existe |
| Aplicación ejecutable | No existe: no hay capturas Playwright "antes" ni "después" |
| Modelos Visual Paradigm | 3 `.vpp` en `modelo/`; contenido **NO VERIFICADO** |
| PNG exportados | 10 en `exportes/`, con marca de agua "evaluation copy" (P-03) |
| Requisitos | RF-01..24, HU-01..19, RNF-01..15, RN-01..13 (originales); RF-25..27 y HU-20..22 propuestos en `docs/08-requisitos/configuracion/` |
| Mockups | 19 en `docs/08-requisitos/mockups/` y 3 en `docs/08-requisitos/configuracion/mockups/`; 5 revisados (ver §7) |

Diagramas revisados en la Fase 1: FUN-01, LOG-01, ACT-01 y DEP-01 (PNG). CTX-01, FUN-02, LOG-02, DEV-01, DEV-02 y DEP-03 son **NO VERIFICADO**. SEQ-01..03 solo existían como texto y el ER no existía.

Trazabilidad ([diagnostico/trazabilidad.md](diagnostico/trazabilidad.md)): 24 de 24 RF tienen caso de uso; 1 de 11 CU tiene diagrama de actividades; 0 RF tienen código, prueba o evidencia en el repositorio original.

## 4. Problemas (por gravedad)

Lista completa con evidencias en [diagnostico/diagnostico.md](diagnostico/diagnostico.md), §3.

### Alta

| ID | Problema |
|---|---|
| P-01 | SEQ-01..03 sin dibujar; el README los lista como existentes |
| P-02 | No hay diagrama ER |
| P-03 | PNG con marca de agua "evaluation copy" |
| P-04 | RN-04 (categoría propia o predeterminada) sin garantía en el esquema; el aislamiento depende solo del repositorio |
| P-05 | RN-03: la FK movimiento→categoría no incluye `tipo` |
| P-06 | Categorías predeterminadas duplicables (`NULL ≠ NULL` en la unicidad), sin CHECK de longitud, sin distinguir mayúsculas |
| P-07 | Borrado lógico (estado ELIMINADA) contradice `ON DELETE CASCADE` |

### Media (P-08..P-22)

- UML: P-08 (10 de 11 CU sin diagrama de comportamiento), P-09 (ACT-01 con defectos de notación), P-10 (CU-10 extiende solo CU-03), P-11 (actor Reloj no dibujado; estereotipo de correo inconsistente), P-12 (firmas SEQ distintas de LOG-02), P-13 (SEQ-03 sin comprobación de bloqueo), P-14 (puertos sin RF).
- Base de datos: P-15 (esquema y clases no coinciden), P-16 (`refresh_token` sin `reemplazado_por`), P-17 (faltan CHECK de RN-01 y RN-06), P-18 (bloqueo redundante), P-19 (presupuesto sin upsert ni control de concurrencia), P-20 (índices no cerrados).
- Documentación: P-21 (`AUDITORIA.md:19` declara correcciones no entregadas), P-22 (`diagnostico/db_audit.md` es de una ejecución previa y tiene errores; se conserva como histórico, ver [diagnostico/db_audit.md](diagnostico/db_audit.md)).

### Baja (P-23..P-27)

P-23 (LOG-01: asociaciones no dibujadas), P-24 (DEP-01: artefactos fuera de la tabla), P-25 (nomenclatura mixta de columnas), P-26 (README cita archivos inexistentes), P-27 (precondición de FUN-01 incompleta).

## 5. Auditoría UML

| Diagrama | Estado tras el trabajo | Fuente |
|---|---|---|
| FUN-01 Casos de uso | Corregido en Mermaid (aproximación; Mermaid no tiene diagrama de casos de uso) | [FUN-01](evidencias/diagramas/FUN-01_Casos_de_uso.md) |
| ACT-01 Registrar movimiento | Rehecho con carriles y flujos alternativos | [ACT-01](evidencias/diagramas/ACT-01_Registrar_movimiento.md) |
| ACT-02 Modificar o eliminar | Nuevo | [ACT-02](evidencias/diagramas/ACT-02_Modificar_eliminar_movimiento.md) |
| ACT-03 Iniciar sesión | Nuevo | [ACT-03](evidencias/diagramas/ACT-03_Iniciar_sesion.md) |
| SEQ-01, SEQ-02, SEQ-03 | Nuevos en Mermaid, con firmas canónicas | [SEQ-01](evidencias/diagramas/SEQ-01_Registrar_gasto.md), [SEQ-02](evidencias/diagramas/SEQ-02_Consultar_resumen.md), [SEQ-03](evidencias/diagramas/SEQ-03_Iniciar_sesion.md) |
| ER | Nuevo en Mermaid | [ER](evidencias/diagramas/ER.md) |

Todos los diagramas Mermaid: **sintaxis no verificada** (no hay mermaid-cli; renderizado NO VERIFICADO).

Supuestos y inferencias marcados en los diagramas:

- ACT-01: la rama de error 5b se evalúa en la API (supuesto).
- ACT-02: al eliminar un gasto se reevalúa su mes. Es una **inferencia**: RF-13 no lo exige.
- ACT-03: el bloqueo se decide al 5.º fallo (**NO VERIFICADO**; RN-08 dice "5 intentos fallidos seguidos").
- SEQ-02: `RangoInvalidoException` es un nombre **inventado** (no existe en LOG-02).
- SEQ-03: `CredencialesInvalidasException` y `CuentaBloqueadaException` son nombres **inventados**; las operaciones de actualización de intentos/bloqueo de `IRepositorioUsuarios` son **supuestos** (LOG-02 solo tiene `guardar` y `buscar*`).
- FUN-01: el caso "Respaldo diario" es una propuesta, no un CU del catálogo.

## 6. Arquitectura

Detalle en [architecture/arquitectura.md](architecture/arquitectura.md). Se mantienen ADR-001 (monolito modular), ADR-002 (hexagonal), ADR-003 (PostgreSQL 16) y JWT.

Puertos de entrada: IAutenticacion, IGestionMovimientos, IGestionCategorias, IGestionPresupuestos, IEvaluadorPresupuesto, IConsultaFinanciera. La concurrencia de presupuestos usa `INSERT … ON CONFLICT` y un `UPDATE … WHERE NOT notificado` atómico (P-19).

| # | Decisión | Estado |
|---|---|---|
| D-1 | Borrado lógico de usuario (`ELIMINADA` + `eliminada_en`) y `ON DELETE RESTRICT`. Purga física pendiente | APROBADA |
| D-2 | Firmas canónicas de P-12 (SEQ y LOG-02 alineados) | APROBADA |
| D-3a | Eliminar `IGestionPresupuestos.eliminar` | APROBADA |
| D-3b | Eliminar `IConsultaFinanciera.obtenerMovimiento` | **RECHAZADA**: se mantiene (lo usa CU-07 paso 4; falta su RF y su endpoint, pendientes) |
| D-4 | `BLOQUEADA` se deriva de `bloqueado_hasta`, no se guarda en `estado` | APROBADA |
| D-5 | Trigger PL/pgSQL para RN-04 | APROBADA |
| D-6 | `sesion_refresco` con `reemplazado_por` y `revocado_en` | APROBADA |
| D-7 | `token_recuperacion.usado_en` (timestamp) | APROBADA |
| D-8 | Moneda única COP | APROBADA (supuesto) |
| D-9 | No adoptar RLS | APROBADA |

## 7. Diseño (revisión de interfaz)

Fuente: [evidencias/interfaces/revision_mockups.md](evidencias/interfaces/revision_mockups.md). Método: inspección visual de 5 de 19 mockups estáticos (HU-01, HU-06, HU-13, HU-16, HU-18), norma WCAG 2.1 AA. No hubo pruebas en navegador.

- Los contrastes son **estimados**; no se midieron píxeles.
- En total hay 13 hallazgos: 0 críticos, 7 mayores y 6 menores, contados fila por fila en sus tablas. El resumen de la primera versión decía 14 por un error de conteo, que se corrigió en F7.
- Mayores principales: texto gris claro con contraste estimado bajo (A-01), naranja de "ALERTA" (A-02), gráfico sin alternativa textual (A-03), modales sin botón de cierre ni gestión de foco (O-02), selectores personalizados sin rol ARIA (R-01).
- Responsive (escritorio no verificado en móvil): **NO VERIFICADO**; la barra lateral fija y las tablas de 5-6 columnas no caben a 390 px.
- Puntos fuertes: etiquetas visibles, errores con texto específico, estado con texto además de color, signos en montos.

## 8. Tecnologías

| Tecnología | Situación |
|---|---|
| Java/Spring, PostgreSQL 16, JWT, nginx | Detectadas solo en la documentación (ADR-001..003 y `VISTA_DESARROLLO.md`). Ninguna implementada |
| PostgreSQL 18.6 | Usado en este trabajo para ejecutar el DDL y las pruebas |
| Mermaid | Usado para los diagramas; sin renderizar (**sintaxis no verificada**) |
| Git | Control de versiones; un commit por fase |
| Visual Paradigm | Herramienta de los originales; no se usó (los PNG son de evaluación) |

El pie de la barra lateral de un mockup dice "TypeScript · React"; la documentación no confirma ese stack de cliente (**NO VERIFICADO**).

## 9. Implementación

Solo se entregan artefactos de diseño: el DDL propuesto ([architecture/V1__esquema.propuesta.sql](architecture/V1__esquema.propuesta.sql)), los diagramas Mermaid y las pruebas SQL. **No hay backend**: decisión del usuario (fuera de alcance).

El DDL lleva la cabecera "PROPUESTA — no es un archivo original". Su comentario dice "syntax valid for PostgreSQL 16+ (tested on 18 only)"; la compatibilidad con PostgreSQL 16 es **NO VERIFICADO**.

## 10. Base de datos

Mecanismos por regla (detalle en [architecture/arquitectura.md](architecture/arquitectura.md), §4):

| Regla | Mecanismo |
|---|---|
| RN-01 | `NUMERIC(14,2) CHECK (monto>0 AND monto<=999999999999.99)`. Máximo 2 decimales se valida en el dominio (`NUMERIC` redondea en silencio) |
| RN-02 | Solo en el dominio (un CHECK no puede usar `now()`) |
| RN-03 | `UNIQUE(id,tipo)` en categoría; FK compuesta `(id_categoria,tipo)` |
| RN-04 | Trigger `BEFORE INSERT/UPDATE` en movimiento y presupuesto; el filtro `id_usuario` de la aplicación sigue siendo obligatorio |
| RN-06 | `UNIQUE(correo)`, `CHECK (correo=lower(correo))` |
| RN-09 | `CHECK (char_length(nombre) BETWEEN 1 AND 40)`; índices únicos parciales |
| RN-10 | `UNIQUE(id_usuario,id_categoria,periodo)`; `CHECK (periodo = date_trunc('month',periodo))` |
| RN-12 | Columna `notificado` con `UPDATE` atómico |
| RN-14 (**PROPUESTA**) | Trigger `fn_categoria_nombre_unico_global` con `pg_advisory_xact_lock`: una propia no puede repetir nombre normalizado y tipo de una predeterminada, ni al revés. Decisión del usuario; no está en los requisitos originales |
| Gasto/Ingreso | `CHECK ((tipo='GASTO' AND metodo_pago IS NOT NULL AND fuente IS NULL) OR (tipo='INGRESO' AND metodo_pago IS NULL))` |
| Moneda | `CHECK (moneda='COP')` (supuesto, D-8) |
| Borrado | `ON DELETE RESTRICT` en todas las FK (D-1). Purga física pendiente |

`db_audit.md` se conserva como histórico y lo sustituye el DDL propuesto (P-22; detalle en [diagnostico/diagnostico.md](diagnostico/diagnostico.md)).

## 11. Pruebas

Plan T-01..T-17 en [architecture/arquitectura.md](architecture/arquitectura.md) §6 y script [evidencias/pruebas/tests_esquema.sql](evidencias/pruebas/tests_esquema.sql). Cada caso corre en una transacción con `ROLLBACK`. La base temporal `fp_qa_test` se creó y se eliminó al terminar.

- **1.ª ejecución** (PostgreSQL 18.6): 3 FAIL en T-01c, T-07b y T-12a. Los códigos esperados eran incorrectos: el dato ya se rechazaba, con 22003, 22001 y 23001 respectivamente. La expectativa se corrigió **después de ejecutar**. El esquema no se modificó. Los logs de la 1.ª ejecución se conservaron sin cambios.
- **2.ª ejecución**: se corrigieron las tres expectativas (con comentario en el script) y se añadió T-17 (RN-14).
- T-16b es solo una observación: el planificador usó el índice del historial, no el de suma por categoría. Depende del planificador y del volumen de prueba (50 000 movimientos).
- Fuera de alcance: lógica Java, JWT, BCrypt, envío de correo, rendimiento y PostgreSQL 16 (**NO VERIFICADO**).

## 12. Resultados

| Ejecución | Log | PASS | FAIL | INFO |
|---|---|---|---|---|
| 1.ª | [F5_tests_esquema.log](evidencias/pruebas/F5_tests_esquema.log) | 26 | 3 | 1 |
| 2.ª | [F5_tests_esquema_v2.log](evidencias/pruebas/F5_tests_esquema_v2.log) | 32 | 0 | 1 |

Los recuentos son `grep` sobre cada log. La 2.ª ejecución incluye T-17 (RN-14): 3 casos PASS. Detalle caso por caso en [evidencias/pruebas/RESULTADOS.md](evidencias/pruebas/RESULTADOS.md).

Los logs de aplicación del DDL dan exit 0: [F4_ddl_apply.log](evidencias/pruebas/F4_ddl_apply.log), [F4_ddl_apply_v2.log](evidencias/pruebas/F4_ddl_apply_v2.log), [F5_ddl_apply.log](evidencias/pruebas/F5_ddl_apply.log), [F5_ddl_apply_v2.log](evidencias/pruebas/F5_ddl_apply_v2.log).

## 13. Antes y después

Comparación en [evidencias/comparaciones/antes_despues.md](evidencias/comparaciones/antes_despues.md).

| Diagrama | Antes | Después | Problemas |
|---|---|---|---|
| SEQ-01..03 | Solo texto | Mermaid | P-01, P-12, P-13, P-19 |
| ACT-01 | PNG | Mermaid con carriles | P-09 |
| ACT-02, ACT-03 | No existían | Mermaid | P-08, P-10, P-13 |
| FUN-01 | PNG | Mermaid (aproximación) | P-10, P-11, P-27 |
| ER | Tabla en `VISTA_DESARROLLO.md` | Mermaid | P-02, P-07, P-16, P-18 |

Los PNG "antes" no se inspeccionaron visualmente en la tarea de comparación (NO VERIFICADO). En la Fase 1 sí se revisaron ACT-01 y FUN-01. Capturas Playwright "antes"/"después": no aplicables (no hay aplicación).

## 14. Evidencias

Índice completo en [evidencias/INDICE_EVIDENCIAS.md](evidencias/INDICE_EVIDENCIAS.md).

- Diagramas: [SEQ-01](evidencias/diagramas/SEQ-01_Registrar_gasto.md), [SEQ-02](evidencias/diagramas/SEQ-02_Consultar_resumen.md), [SEQ-03](evidencias/diagramas/SEQ-03_Iniciar_sesion.md), [ACT-01](evidencias/diagramas/ACT-01_Registrar_movimiento.md), [ACT-02](evidencias/diagramas/ACT-02_Modificar_eliminar_movimiento.md), [ACT-03](evidencias/diagramas/ACT-03_Iniciar_sesion.md), [FUN-01](evidencias/diagramas/FUN-01_Casos_de_uso.md), [ER](evidencias/diagramas/ER.md), [cambios pendientes en originales](evidencias/diagramas/CAMBIOS_PENDIENTES_ORIGINALES.md).
- Comparación: [antes_despues.md](evidencias/comparaciones/antes_despues.md).
- Base de datos: [V1__esquema.propuesta.sql](architecture/V1__esquema.propuesta.sql), [F4_ddl_apply.log](evidencias/pruebas/F4_ddl_apply.log), [F4_ddl_apply_v2.log](evidencias/pruebas/F4_ddl_apply_v2.log), [F5_ddl_apply.log](evidencias/pruebas/F5_ddl_apply.log), [F5_ddl_apply_v2.log](evidencias/pruebas/F5_ddl_apply_v2.log).
- Pruebas: [tests_esquema.sql](evidencias/pruebas/tests_esquema.sql), [F5_tests_esquema.log](evidencias/pruebas/F5_tests_esquema.log), [F5_tests_esquema_v2.log](evidencias/pruebas/F5_tests_esquema_v2.log), [RESULTADOS.md](evidencias/pruebas/RESULTADOS.md).
- Interfaces: [revision_mockups.md](evidencias/interfaces/revision_mockups.md).
- Diagnóstico y arquitectura: [diagnostico.md](diagnostico/diagnostico.md), [trazabilidad.md](diagnostico/trazabilidad.md), [db_audit.md](diagnostico/db_audit.md), [arquitectura.md](architecture/arquitectura.md).
- PNG originales (con marca de agua): [ACT-01](../exportes/ACT-01%20Registrar%20movimiento.png), [FUN-01](../exportes/FUN-01%20Casos%20de%20uso.png).

## 15. Pendientes

- Aplicar a los originales los 19 cambios de [CAMBIOS_PENDIENTES_ORIGINALES.md](evidencias/diagramas/CAMBIOS_PENDIENTES_ORIGINALES.md). No se aplicaron; los `.vpp` no se inspeccionaron (**NO VERIFICADO**).
- Purga física de usuarios (D-1). La Ley 1581/2012 de supresión de datos es **NO VERIFICADO** como requisito; no aparece en los requisitos del proyecto.
- Renderizar los diagramas Mermaid (**sintaxis no verificada**; no hay mermaid-cli, que sería una dependencia nueva).
- Reexportar PNG sin marca de agua (requiere licencia de Visual Paradigm; herramienta no disponible).
- Compatibilidad con PostgreSQL 16 (**NO VERIFICADO**).
- Revisar los 14 mockups restantes y diseñar las variantes móviles (**NO VERIFICADO**).
- RF y endpoint `GET /api/v1/movimientos/{id}` para `obtenerMovimiento`.
- Definir las excepciones propuestas (`RangoInvalidoException`, `CredencialesInvalidasException`, `CuentaBloqueadaException`) y las operaciones de `IRepositorioUsuarios` de SEQ-03: son supuestos.
- Confirmar con el usuario la inferencia de ACT-02 (reevaluar presupuesto al eliminar).
- Decidir si el ambiguo RF-14/15 permite una categoría propia con el nombre de una predeterminada (**NO VERIFICADO**; RN-14 la bloquea).

## 16. Conclusiones

- La documentación original tenía 27 problemas identificados (7 altos), sin código que los contraste.
- Los diagramas de secuencia, el ER y los diagramas de actividad faltantes existen ahora en Mermaid. Su renderizado no está verificado.
- La 2.ª ejecución del DDL propuesto (PostgreSQL 18.6) dio 32 PASS, 0 FAIL y 1 INFO (T-16b, observación del planificador).
- Las tres expectativas erróneas de la 1.ª ejecución se corrigieron después de ver el resultado. Se documenta así para que sea trazable.
- El diseño de interfaz tiene problemas de accesibilidad mayores estimados; no hay evidencia de comportamiento en navegador.

## 17. Recomendaciones

1. Renderizar los diagramas Mermaid en un entorno con mermaid-cli o similar antes de darlos por válidos.
2. Ejecutar `V1__esquema.propuesta.sql` en PostgreSQL 16 para verificar la compatibilidad declarada.
3. Aprobar o rechazar los supuestos e inferencias marcados (§5 y §15) antes de aplicar cambios a los originales.
4. Aplicar los 19 cambios de `CAMBIOS_PENDIENTES_ORIGINALES.md` en una fase aparte, con aprobación del usuario.
5. Corregir los contrastes de la interfaz (texto gris, naranja de alerta) y medirlos con una herramienta antes de cerrar la revisión.
