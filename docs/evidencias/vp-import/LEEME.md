# Importar en Visual Paradigm

⚠ Importación en VP: **NO VERIFICADO**. Trabaja sobre una **copia** del `.vpp` (`modelo/Finanzas_Personales_v2.vpp`), nunca sobre el original.

## Archivos
| Archivo | Contenido | Validación real |
|---|---|---|
| `modelo_corregido.xmi` | Modelo UML (XMI 2.1): 32 clases, 13 interfaces, 89 operaciones, 10 constraints (RN-01..14, RN-14 PROPUESTA) | XML bien formado, 653 `xmi:id` sin repetir, 0 referencias rotas (`validacion.log`) |
| `puml/*.puml` | 16 diagramas PlantUML corregidos | Solo comprobación estática (`@startuml`/`@enduml`, bloques). **Render NO VERIFICADO** |
| `ddl_recheck.log` | `V1__esquema.propuesta.sql` aplicado en PostgreSQL 18.6 (exit 0) | Ejecutado |
| `*.svg` | No generados: faltan `plantuml.jar` y Graphviz (`dot`). Java 21 sí está instalado | — |

## Orden
1. **Modelo:** `File > Import > XMI…` → `modelo_corregido.xmi`. Así se crean clases, interfaces, enums, excepciones y constraints en el Model Explorer, pero **sin diagramas**.
2. **Comportamiento (P0):** SEQ-01, SEQ-02, SEQ-03.
   - Si tu edición de VP importa PlantUML, usa `File > Import > PlantUML` (**NO VERIFICADO** que exista).
   - Si no, dibuja a mano siguiendo el `.puml`. Los lifelines se arrastran desde las clases importadas.
3. **ER (P0):** `Tools > DB > Reverse Database` contra una BD local con el DDL aplicado. Si no puedes, dibújalo a mano desde `puml/ER.puml`.
4. **Estructura:** LOG-02 y luego LOG-01. Crea un diagrama vacío y arrastra las clases e interfaces desde el Model Explorer; las relaciones ya vienen del XMI.
5. **Actividad (P1):** ACT-01, ACT-02, ACT-03, a mano (swimlanes) desde los `.puml`.
6. **Funcional (P1):** FUN-01 y luego CTX-01.
7. **P2:** FUN-02, DEV-01, DEV-02, DEP-01, DEP-03.
8. Marca cada fila de `../GUIA_VISUAL_PARADIGM.md` como `[x]` y reexporta los PNG.

## Qué hacer a mano
| Qué | Por qué |
|---|---|
| Layout de todos los diagramas | El XMI no lleva información gráfica |
| Swimlanes y merges de ACT | PlantUML usa `repeat` para los bucles 5a/5b/6a |
| Puntos de extensión de FUN-01 ("Gasto modificado") | Comprobar que el `«extend»` quede enlazado |
| Componentes, nodos y artefactos (FUN-02, DEV, DEP, CTX) | No están en el XMI; vienen solo del texto, sin comparar con el PNG (⚠) |

## Elementos ⚠ que debes confirmar
- **Excepciones inventadas:** `RangoInvalidoException`, `CredencialesInvalidasException`, `CuentaBloqueadaException`. Su clase padre `ReglaNegocioException` también es un supuesto.
- **SEQ-01, caso 5a:** ¿400 o 422? La guía y el Mermaid no coinciden.
- **ACT-02:** volver a evaluar el presupuesto al eliminar un gasto es una inferencia.
- **FUN-01:** el caso "Respaldo diario" es una propuesta.
- **CTX-01:** el actor "Reloj del sistema" no aparece en `VISTA_CONTEXTO.md`.
- **XMI:** `IRepositorioMovimientos.buscar` devuelve `Pagina<MovimientoDTO>`, pero el original dice `Pagina<Movimiento>`. Los genéricos `List`, `Optional` y `Pagina` se modelaron con multiplicidad.
- **XMI:** el paquete `dto` (13 clases) se añadió porque los puertos lo referencian.
