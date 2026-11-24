# Cambios pendientes en los originales (NO aplicados)

Edits que `docs/*/VISTA_*.md` y `modelo/*.vpp` necesitarían para alinearse con las decisiones aprobadas (`docs/architecture/arquitectura.md` §7). Ninguno se aplicó. Los números de línea son del estado actual de la rama `ai/redesign`. Visual Paradigm (`.vpp`) es un formato binario/propietario: no se inspeccionó, edición **NO VERIFICADO**.

| # | Archivo:línea | Edit necesario | Decisión / problema |
|---|---|---|---|
| 1 | `docs/03-logica/VISTA_LOGICA.md:169` | Quitar `eliminar(idUsuario, idPresupuesto)` de `IGestionPresupuestos` | D-3a, P-14 |
| 2 | `docs/03-logica/VISTA_LOGICA.md:170` | Mantener `obtenerMovimiento`; añadir nota de RF y endpoint `GET /api/v1/movimientos/{id}` pendientes | D-3b rechazada, P-14 |
| 3 | `docs/03-logica/VISTA_LOGICA.md:166-193` | Añadir `CategoriaNoDisponibleException` (422) como excepción de aplicación | D-2, P-12 |
| 4 | `docs/03-logica/VISTA_LOGICA.md:137` | `EstadoCuenta`: quitar `BLOQUEADA` (se deriva de `bloqueado_hasta`) | D-4, P-18 |
| 5 | `docs/02-funcional/VISTA_FUNCIONAL.md:160-198` | Reemplazar texto de SEQ-01..03 por las firmas canónicas y referenciar los diagramas de `docs/evidencias/diagramas/` | D-2, P-01, P-12 |
| 6 | `docs/02-funcional/VISTA_FUNCIONAL.md:175` | Mantener 422; añadir ramas 5a/6a | P-12 |
| 7 | `docs/02-funcional/VISTA_FUNCIONAL.md:180` | `enviarAlertaPresupuesto(correo, p)` y reclamo atómico `UPDATE … WHERE NOT notificado` antes de notificar | D-2, P-12, P-19 |
| 8 | `docs/02-funcional/VISTA_FUNCIONAL.md:190-192` | `obtenerResumen(idUsuario, desde, hasta)`, `sumarPorCategoria(idUsuario, desde, hasta)` | D-2, P-12 |
| 9 | `docs/02-funcional/VISTA_FUNCIONAL.md:198` | Añadir comprobación `bloqueado_hasta > now` → 423 e incremento de `intentos_fallidos` | P-13 |
| 10 | `docs/02-funcional/VISTA_FUNCIONAL.md:21,28,94-96` | CU-10 `«extend»` también a CU-04 (punto de extensión *Gasto modificado*) | P-10 |
| 11 | `docs/02-funcional/VISTA_FUNCIONAL.md:76` | Precondición de FUN-01 debe incluir CU-07 (P-27) y detallar CU-02 renovar/cerrar sesión (`:105`) | P-27 |
| 12 | `docs/02-funcional/VISTA_FUNCIONAL.md:12,13,205` | Unificar estereotipo de "Servicio de correo" (`«system»` aquí, `«external system»` en `VISTA_CONTEXTO.md:22`, `«device» «external»` en `VISTA_DESPLIEGUE.md:15`) y dibujar "Reloj del sistema" | P-11 |
| 13 | `docs/02-funcional/VISTA_FUNCIONAL.md:131-156` | Carriles reales (+ Servicio de correo), decisión vs merge, flujos 5b/6a/*a, "Notificar" fuera del carril BD | P-09 |
| 14 | `docs/02-funcional/VISTA_FUNCIONAL.md:158` | Sustituir "ACT-02/ACT-03 siguen el mismo patrón" por los ACT-02 (CU-04) y ACT-03 (CU-02) nuevos | P-08 |
| 15 | `docs/04-desarrollo/VISTA_DESARROLLO.md:112-114` | `ON DELETE CASCADE` → `ON DELETE RESTRICT`; añadir `eliminada_en`, `CHECK (intentos_fallidos BETWEEN 0 AND 5)`, `bloqueado_hasta` | D-1, D-4, P-07, P-18 |
| 16 | `docs/04-desarrollo/VISTA_DESARROLLO.md` (esquema) | Añadir tablas `sesion_refresco` y `token_recuperacion` (`usado_en`) | D-6, D-7 |
| 17 | `docs/README.md:51,64` | Dejar de listar SEQ-01..03 como existentes hasta que se exporten | P-01 |
| 18 | `modelo/*.vpp` y `exportes/*.png` | Redibujar SEQ-01..03, ACT-01, FUN-01, ER en Visual Paradigm y re-exportar | todos; **NO VERIFICADO** (no inspeccionado) |
| 19 | `docs/04-desarrollo/VISTA_DESARROLLO.md:113` | `monto CHECK (monto > 0)` → `CHECK (monto > 0 AND monto <= 999999999999.99)` (RN-01); FK compuesta `(id_categoria, tipo)` (revisión R-07) | RN-01, RN-03 |
