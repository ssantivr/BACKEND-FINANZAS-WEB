# Comparación antes / después de los diagramas

Renderizado de todos los diagramas nuevos: NO VERIFICADO (no hay mermaid-cli). Los "antes" son los originales sin modificar.

| Diagrama | Antes | Después | Problemas resueltos |
|---|---|---|---|
| SEQ-01 Registrar gasto | No existía (solo texto en `docs/02-funcional/VISTA_FUNCIONAL.md:160-186`) | `docs/evidencias/diagramas/SEQ-01_Registrar_gasto.md` | P-01, P-12, P-19 |
| SEQ-02 Consultar resumen | No existía (texto en `VISTA_FUNCIONAL.md:188-192`) | `docs/evidencias/diagramas/SEQ-02_Consultar_resumen.md` | P-01, P-12 |
| SEQ-03 Iniciar sesión | No existía (texto en `VISTA_FUNCIONAL.md:194-198`) | `docs/evidencias/diagramas/SEQ-03_Iniciar_sesion.md` | P-01, P-13, P-18 |
| ACT-01 Registrar movimiento | `exportes/ACT-01 Registrar movimiento.png` | `docs/evidencias/diagramas/ACT-01_Registrar_movimiento.md` | P-09 (carriles, merge/decisión, 5b/6a/*a, Notificar) |
| ACT-02 Modificar o eliminar movimiento | No existía | `docs/evidencias/diagramas/ACT-02_Modificar_eliminar_movimiento.md` | P-08, P-10 |
| ACT-03 Iniciar sesión (bloqueo) | No existía | `docs/evidencias/diagramas/ACT-03_Iniciar_sesion.md` | P-08, P-13 |
| FUN-01 Casos de uso | `exportes/FUN-01 Casos de uso.png` | `docs/evidencias/diagramas/FUN-01_Casos_de_uso.md` | P-10, P-11, P-27 |
| ER | Tabla "Esquema relacional" en `docs/04-desarrollo/VISTA_DESARROLLO.md:108-114` (sin PNG verificado) | `docs/evidencias/diagramas/ER.md` | P-07, P-16, P-18, P-25 (ver `docs/architecture/arquitectura.md` §4) |

Los "antes" PNG no fueron abiertos en esta tarea: la descripción del defecto proviene de `docs/diagnostico/diagnostico.md` (P-09, P-10, P-11), no de una inspección visual propia (**NO VERIFICADO** visualmente).
