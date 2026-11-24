# Trazabilidad requisito → diagrama → código → prueba → evidencia

Fuente RF→HU→CU: `docs/08-requisitos/REQUERIMIENTOS_FUNCIONALES.md:262-285`. Estado al cierre de la Fase 1.
Código, pruebas y evidencias: **no existen** en el repositorio (no hay código fuente). La correspondencia SEQ↔CU se deduce de `VISTA_FUNCIONAL.md:160-198`: **NO VERIFICADO**.

| RF | HU | CU | Diagramas | Código | Prueba | Evidencia |
|---|---|---|---|---|---|---|
| RF-01 | HU-01 | CU-01 | FUN-01, LOG-01, LOG-02 | — | — | — |
| RF-02 | HU-01 | CU-01 | FUN-01, LOG-02 | — | — | — |
| RF-03 | HU-02 | CU-02 | FUN-01, SEQ-03 (solo texto) | — | — | — |
| RF-04 | HU-02 | CU-02 | FUN-01 (SEQ-03 no lo cubre, P-13) | — | — | — |
| RF-05 | HU-03 | CU-02 | FUN-01 (sin flujo) | — | — | — |
| RF-06 | HU-04 | CU-02 | FUN-01 (sin flujo) | — | — | — |
| RF-07 | HU-05 | CU-09 | FUN-01 | — | — | — |
| RF-08 | HU-05 | CU-09 | FUN-01 | — | — | — |
| RF-09 | HU-06 | CU-03 | FUN-01, ACT-01, SEQ (solo texto) | — | — | — |
| RF-10 | HU-07 | CU-03 | FUN-01, ACT-01 | — | — | — |
| RF-11 | HU-06/07/08 | CU-03, CU-04 | FUN-01, ACT-01 | — | — | — |
| RF-12 | HU-08 | CU-04 | FUN-01 (sin extend a CU-10, P-10) | — | — | — |
| RF-13 | HU-09 | CU-04 | FUN-01 | — | — | — |
| RF-14 | HU-10 | CU-05 | FUN-01, LOG-01 | — | — | — |
| RF-15 | HU-11 | CU-05 | FUN-01 | — | — | — |
| RF-16 | HU-11 | CU-05 | FUN-01 | — | — | — |
| RF-17 | HU-12 | CU-05 | FUN-01 | — | — | — |
| RF-18 | HU-13 | CU-06 | FUN-01, LOG-01 | — | — | — |
| RF-19 | HU-14 | CU-06, CU-08 | FUN-01 | — | — | — |
| RF-20 | HU-15 | CU-10 | FUN-01, ACT-01, SEQ (solo texto) | — | — | — |
| RF-21 | HU-16 | CU-07 | FUN-01 | — | — | — |
| RF-22 | HU-17 | CU-07, CU-11 | FUN-01 («include») | — | — | — |
| RF-23 | HU-18 | CU-08 | FUN-01, SEQ (solo texto) | — | — | — |
| RF-24 | HU-19 | CU-08 | FUN-01 | — | — | — |

**Resumen:** 24/24 RF tienen caso de uso. 1/11 CU tiene diagrama de actividades (CU-03). Los diagramas de secuencia existen solo como texto. Hay 0 RF con código, prueba o evidencia. Componentes, puertos y nodos por CU: `docs/06-trazabilidad/TRAZABILIDAD.md:6-20` (original).
