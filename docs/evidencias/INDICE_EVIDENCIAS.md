# Índice de evidencias (F6)

Rama `ai/redesign` · 2026-11-09. Todas las evidencias son reales y se generaron en esta rama. Lo que no se pudo comprobar se marca **NO VERIFICADO**.

## 1. Diagramas corregidos (Mermaid)
Ninguno se ha comprobado renderizándolo: **sintaxis no verificada** (no hay `mermaid-cli`, que sería una dependencia nueva).

| Diagrama | Archivo | Antes |
|---|---|---|
| SEQ-01 Registrar gasto | [SEQ-01](diagramas/SEQ-01_Registrar_gasto.md) | No existía; solo texto |
| SEQ-02 Consultar resumen | [SEQ-02](diagramas/SEQ-02_Consultar_resumen.md) | No existía; solo texto |
| SEQ-03 Iniciar sesión | [SEQ-03](diagramas/SEQ-03_Iniciar_sesion.md) | No existía; solo texto |
| ACT-01 Registrar movimiento | [ACT-01](diagramas/ACT-01_Registrar_movimiento.md) | [PNG original](../../exportes/ACT-01%20Registrar%20movimiento.png) |
| ACT-02 Modificar/eliminar movimiento | [ACT-02](diagramas/ACT-02_Modificar_eliminar_movimiento.md) | No existía |
| ACT-03 Iniciar sesión | [ACT-03](diagramas/ACT-03_Iniciar_sesion.md) | No existía |
| FUN-01 Casos de uso (aproximación) | [FUN-01](diagramas/FUN-01_Casos_de_uso.md) | [PNG original](../../exportes/FUN-01%20Casos%20de%20uso.png) |
| ER | [ER](diagramas/ER.md) | Tabla en `docs/04-desarrollo/VISTA_DESARROLLO.md:108-116` |
| Módulos y puertos | [arquitectura.md §2](../architecture/arquitectura.md) | LOG-02, DEV-02 (PNG) |

- Comparación antes/después: [antes_despues.md](comparaciones/antes_despues.md). El agente de diagramas no abrió los PNG "antes", pero el UML Analyst inspeccionó ACT-01 y FUN-01 en la Fase 1.
- Cambios que necesitarían los originales (no aplicados): [CAMBIOS_PENDIENTES_ORIGINALES.md](diagramas/CAMBIOS_PENDIENTES_ORIGINALES.md).

## 2. Base de datos
| Evidencia | Archivo |
|---|---|
| DDL propuesto (no original) | [V1__esquema.propuesta.sql](../architecture/V1__esquema.propuesta.sql) |
| Aplicación del DDL, v1 (exit 0) | [F4_ddl_apply.log](pruebas/F4_ddl_apply.log) |
| Aplicación del DDL, v2 con RN-14 (exit 0) | [F4_ddl_apply_v2.log](pruebas/F4_ddl_apply_v2.log) |

## 3. Pruebas (PostgreSQL 18.6, base temporal que se borra al terminar)
| Ejecución | Script | Log | Resultado (grep sobre el log) |
|---|---|---|---|
| 1.ª | `tests_esquema.sql` en el commit `7db79c4` | [F5_tests_esquema.log](pruebas/F5_tests_esquema.log), [F5_ddl_apply.log](pruebas/F5_ddl_apply.log) | 26 PASS / 3 FAIL / 1 INFO |
| 2.ª | [tests_esquema.sql](pruebas/tests_esquema.sql) | [F5_tests_esquema_v2.log](pruebas/F5_tests_esquema_v2.log), [F5_ddl_apply_v2.log](pruebas/F5_ddl_apply_v2.log) | 32 PASS / 0 FAIL / 1 INFO |

En la 2.ª ejecución se corrigió lo que esperaban las pruebas T-01c, T-07b y T-12a, no el esquema, y se añadió T-17 (RN-14). Detalle completo en [RESULTADOS.md](pruebas/RESULTADOS.md).

## 4. Interfaces
| Evidencia | Archivo |
|---|---|
| Revisión de 5 mockups (contraste **estimado**) | [revision_mockups.md](interfaces/revision_mockups.md) |
| Capturas Playwright "antes"/"después" | **No aplicable**: no hay una app que ejecutar |

## 5. Diagnóstico y arquitectura
[diagnostico.md](../diagnostico/diagnostico.md) · [trazabilidad.md](../diagnostico/trazabilidad.md) · [arquitectura.md](../architecture/arquitectura.md) · [db_audit.md](../diagnostico/db_audit.md) (de una ejecución previa; contiene errores documentados en P-22)
