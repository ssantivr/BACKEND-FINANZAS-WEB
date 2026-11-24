# Guía de cambios en Visual Paradigm

Archivo: `modelo/Finanzas_Personales_v2.vpp` · Referencia "después": `docs/evidencias/diagramas/` · ⚠ = NO VERIFICADO (no se leyó el `.vpp`): confirmar al abrir.

## SEQ-01 Registrar gasto (P0) · referencia: `diagramas/SEQ-01_Registrar_gasto.md`
| # | diagrama | elemento a cambiar | antes | después | prioridad | hecho |
|---|---|---|---|---|---|---|
| 1 | SEQ-01 | Diagrama completo | ⚠ No dibujado (solo texto `VISTA_FUNCIONAL.md:160-186`) | Secuencia completa CU-03 + CU-10 | P0 | [ ] |
| 2 | SEQ-01 | Mensaje de alerta | `enviarAlertaPresupuesto(usuario, presupuesto)` | `enviarAlertaPresupuesto(correo: Email, p: PresupuestoDTO)` | P0 | [ ] |
| 3 | SEQ-01 | Reclamo de alerta antes de notificar | "excedido por primera vez" | `UPDATE presupuesto SET notificado=true WHERE id=? AND NOT notificado`; `opt [filas=1]` → `INotificador` | P0 | [ ] |
| 4 | SEQ-01 | Fragmento `alt` categoría no disponible | Sin rama | `CategoriaNoDisponibleException` → 422 | P0 | [ ] |
| 5 | SEQ-01 | Fragmentos `alt` 5a / 6a | Sin ramas | 5a datos inválidos → 400; 6a fallo de persistencia → error | P0 | [ ] |
| 6 | SEQ-01 | Envío de correo | ⚠ No definido | Mensaje asíncrono, fuera de la transacción | P0 | [ ] |
| 7 | SEQ-01 | `sumarGastos` y detección de EXCEDIDO dentro de `evaluar` | No detallado | ⚠ Supuesto de diseño: confirmar | P0 | [ ] |

## SEQ-02 Consultar resumen (P0) · referencia: `diagramas/SEQ-02_Consultar_resumen.md`
| # | diagrama | elemento a cambiar | antes | después | prioridad | hecho |
|---|---|---|---|---|---|---|
| 8 | SEQ-02 | Diagrama completo | ⚠ No dibujado (texto `VISTA_FUNCIONAL.md:188-192`) | Secuencia CU-08 | P0 | [ ] |
| 9 | SEQ-02 | Mensaje de resumen | `obtenerResumen(idUsuario, periodo)` | `obtenerResumen(idUsuario, desde, hasta)` | P0 | [ ] |
| 10 | SEQ-02 | Mensaje de suma | `sumarPorCategoria(idUsuario, periodo)` | `sumarPorCategoria(idUsuario, desde, hasta)` | P0 | [ ] |
| 11 | SEQ-02 | Fragmento `alt` rango inválido (RN-13) | Sin rama | `RangoInvalidoException` (⚠ nombre inventado: confirmar) | P0 | [ ] |
| 12 | SEQ-02 | Nota "idUsuario sale de token.sub" | Ausente | Nota RN-05 / RNF-02 | P0 | [ ] |
| 13 | SEQ-02 | Mes de presupuestos | `mes` sin definir | `mes` derivado de `desde` (⚠ supuesto) | P0 | [ ] |

## SEQ-03 Iniciar sesión (P0) · referencia: `diagramas/SEQ-03_Iniciar_sesion.md`
| # | diagrama | elemento a cambiar | antes | después | prioridad | hecho |
|---|---|---|---|---|---|---|
| 14 | SEQ-03 | Diagrama completo | ⚠ No dibujado (texto `VISTA_FUNCIONAL.md:194-198`) | Secuencia CU-02 | P0 | [ ] |
| 15 | SEQ-03 | Comprobación de bloqueo | Ausente | `alt [bloqueado_hasta > now]` → 423 | P0 | [ ] |
| 16 | SEQ-03 | Intento fallido | `registrarIntentoFallido` → 401 | Incrementar `intentos_fallidos`; al 5.º fijar `bloqueado_hasta` (RN-08) | P0 | [ ] |
| 17 | SEQ-03 | Éxito | ⚠ Sin reinicio del contador | Reiniciar intentos (RF-03) | P0 | [ ] |
| 18 | SEQ-03 | Cuenta `ELIMINADA` | Sin rama | 401 genérico (⚠ supuesto) | P0 | [ ] |
| 19 | SEQ-03 | Excepciones | — | `CredencialesInvalidasException`, `CuentaBloqueadaException` (⚠ nombres inventados: confirmar) | P0 | [ ] |
| 20 | SEQ-03 | Operaciones de `IRepositorioUsuarios` | Solo `guardar` / `buscar*` | ⚠ Supuesto: usar `guardar(u)` u operación nueva; decidir | P0 | [ ] |

## ER (P0) · referencia: `diagramas/ER.md`, `architecture/V1__esquema.propuesta.sql`
| # | diagrama | elemento a cambiar | antes | después | prioridad | hecho |
|---|---|---|---|---|---|---|
| 21 | ER | Diagrama ER | ⚠ No existe en el `.vpp` | Crear con 6 tablas | P0 | [ ] |
| 22 | ER | `usuario` | `estado` con BLOQUEADA; sin `eliminada_en`; sin `nombre` | `estado` ∈ {ACTIVA, ELIMINADA}; `eliminada_en`; `nombre`; `intentos_fallidos` 0..5 | P0 | [ ] |
| 23 | ER | Todas las FK | `ON DELETE CASCADE` | `ON DELETE RESTRICT` | P0 | [ ] |
| 24 | ER | `movimiento` → `categoria` | FK simple `id_categoria` | FK compuesta `(id_categoria, tipo)` | P0 | [ ] |
| 25 | ER | `movimiento.monto` | `CHECK (monto > 0)` | `CHECK (monto > 0 AND monto <= 999999999999.99)`; `moneda='COP'` | P0 | [ ] |
| 26 | ER | `presupuesto` | FK simple | `tipo_categoria='GASTO'` + FK compuesta; periodo día 1 | P0 | [ ] |
| 27 | ER | `refresh_token` | `hash, id_usuario, expira, revocado` | `sesion_refresco` con `revocado_en` y `reemplazado_por` (auto-FK) | P0 | [ ] |
| 28 | ER | `token_recuperacion` | `usado` booleano | `usado_en` timestamptz | P0 | [ ] |
| 29 | ER | Nombres de FK | `id_categoria` / `categoria_id` mezclados | `id_<entidad>` | P0 | [ ] |
| 30 | ER | Nota de reglas | — | RN-01/03/04/06/09/10, RN-14 (propuesta) | P0 | [ ] |

## LOG-02 Aplicación y puertos · referencia: `architecture/arquitectura.md` §2-§3
| # | diagrama | elemento a cambiar | antes | después | prioridad | hecho |
|---|---|---|---|---|---|---|
| 31 | LOG-02 | `IGestionPresupuestos.eliminar` | Presente | Quitar (D-3a) | P0 | [ ] |
| 32 | LOG-02 | `IConsultaFinanciera.obtenerMovimiento` | Presente | Mantener; nota: falta RF y `GET /movimientos/{id}` | P0 | [ ] |
| 33 | LOG-02 | `sumarPorCategoria` en `IRepositorioMovimientos` | ⚠ Confirmar firma en el `.vpp` | `(idUsuario, desde, hasta): List<TotalPorCategoria>` | P0 | [ ] |
| 34 | LOG-02 | Excepción de aplicación | Ausente | `CategoriaNoDisponibleException` (422) | P0 | [ ] |
| 35 | LOG-02 | `IEvaluadorPresupuesto` | Invocado desde registrar | También desde `modificar` y `eliminar` | P0 | [ ] |

## ACT-01 Registrar movimiento (P1) · referencia: `diagramas/ACT-01_Registrar_movimiento.md`
| # | diagrama | elemento a cambiar | antes | después | prioridad | hecho |
|---|---|---|---|---|---|---|
| 36 | ACT-01 | Particiones | Cabeceras de texto | Swimlanes UML: Usuario, Cliente Web, API, Base de datos, Servicio de correo | P1 | [ ] |
| 37 | ACT-01 | Acción "Notificar" | En la columna Base de datos | Solicitud en API; envío en Servicio de correo | P1 | [ ] |
| 38 | ACT-01 | Barra azul sobre [excedido por primera vez] | Barra sin función | Eliminar | P1 | [ ] |
| 39 | ACT-01 | Decisión / merge | Mismo símbolo | Rombo de decisión y nodo merge separados | P1 | [ ] |
| 40 | ACT-01 | Flujo 5b categoría no disponible | Ausente | Recargar categorías → paso 2 | P1 | [ ] |
| 41 | ACT-01 | Flujo 6a fallo de persistencia | Ausente | Conservar datos → merge al formulario | P1 | [ ] |
| 42 | ACT-01 | Flujo *a sesión expirada | Ausente | Renovar; si falla → CU-02 conservando el borrador | P1 | [ ] |
| 43 | ACT-01 | Reclamo `notificado` | Ausente | Acción "Reclamar alerta" antes de notificar | P1 | [ ] |
| 44 | ACT-01 | Nodo final | Flujos sin destino claro | Un único activity final | P1 | [ ] |
| 45 | ACT-01 | Marca de agua del PNG | "evaluation copy" | Reexportar sin marca (⚠ requiere licencia) | P1 | [ ] |

## ACT-02 Modificar/eliminar movimiento (P1) · referencia: `diagramas/ACT-02_Modificar_eliminar_movimiento.md`
| # | diagrama | elemento a cambiar | antes | después | prioridad | hecho |
|---|---|---|---|---|---|---|
| 46 | ACT-02 | Diagrama completo | ⚠ No existe ("sigue el mismo patrón") | Actividad CU-04 con swimlanes | P1 | [ ] |
| 47 | ACT-02 | Reevaluar presupuesto al modificar | — | Mes anterior y nuevo (RF-12) | P1 | [ ] |
| 48 | ACT-02 | Movimiento ajeno | — | Rama 404 | P1 | [ ] |
| 49 | ACT-02 | Reevaluar al eliminar | — | ⚠ Inferencia, RF-13 no lo exige: confirmar o quitar | P1 | [ ] |

## ACT-03 Iniciar sesión (P1) · referencia: `diagramas/ACT-03_Iniciar_sesion.md`
| # | diagrama | elemento a cambiar | antes | después | prioridad | hecho |
|---|---|---|---|---|---|---|
| 50 | ACT-03 | Diagrama completo | ⚠ No existe | Actividad CU-02 con bloqueo (423) y conteo de intentos | P1 | [ ] |
| 51 | ACT-03 | Umbral de bloqueo | — | Bloqueo al 5.º fallo seguido (⚠ interpretación de RN-08) | P1 | [ ] |
| 52 | ACT-03 | Mensaje de error | — | Genérico para usuario inexistente, eliminado o clave errónea | P1 | [ ] |

## FUN-01 Casos de uso (P1) · referencia: `diagramas/FUN-01_Casos_de_uso.md`
| # | diagrama | elemento a cambiar | antes | después | prioridad | hecho |
|---|---|---|---|---|---|---|
| 53 | FUN-01 | «extend» de CU-10 | Solo desde CU-03 | Desde CU-03 y CU-04 (punto de extensión "Gasto modificado") | P1 | [ ] |
| 54 | FUN-01 | Actor "Reloj del sistema" | No dibujado | Actor `«system»` | P1 | [ ] |
| 55 | FUN-01 | Caso "Respaldo diario" | — | ⚠ Propuesta (no es CU del catálogo): confirmar o dejar el actor sin caso | P1 | [ ] |
| 56 | FUN-01 | Estereotipo "Servicio de correo" | `«system»` | `«system»` (mismo nombre que en CTX y DEP) | P1 | [ ] |
| 57 | FUN-01 | Nota de precondición | Omite CU-07 | CU-03..CU-08 requieren sesión | P1 | [ ] |
| 58 | FUN-01 | CU-02 | No detalla renovar/cerrar sesión | Nota RF-05 / RF-06 | P1 | [ ] |
| 59 | FUN-01 | Marca de agua del PNG | "evaluation copy" | Reexportar sin marca (⚠ requiere licencia) | P1 | [ ] |

## LOG-01 Dominio (P1-P2)
| # | diagrama | elemento a cambiar | antes | después | prioridad | hecho |
|---|---|---|---|---|---|---|
| 60 | LOG-01 | Enum `EstadoCuenta` | ACTIVA, BLOQUEADA, ELIMINADA | ACTIVA, ELIMINADA (BLOQUEADA derivada de `bloqueadoHasta`) | P1 | [ ] |
| 61 | LOG-01 | Composiciones R3-R5 | Borrado en cascada | Asociación con borrado restringido (D-1) | P1 | [ ] |
| 62 | LOG-01 | `Usuario.eliminadaEn` | Ausente | Añadir | P1 | [ ] |
| 63 | LOG-01 | Asociaciones R8-R10 | Descritas en el texto, no dibujadas | Dibujar | P2 | [ ] |
| 64 | LOG-01 | Atributos que duplican asociaciones (`categoria`, `correo`, `limite`) | Duplicados | Dejar solo la asociación | P2 | [ ] |
| 65 | LOG-01 | OCL | Sin RN-01 / RN-02 | Añadir restricciones | P2 | [ ] |
| 66 | LOG-01 | Trazado de asociaciones a `Categoria` | ⚠ Posible solapamiento | Reordenar | P2 | [ ] |

## CTX-01 Contexto (P2)
| # | diagrama | elemento a cambiar | antes | después | prioridad | hecho |
|---|---|---|---|---|---|---|
| 67 | CTX-01 | Estereotipo "Servicio de correo" | `«external system»` | Mismo nombre y estereotipo que FUN-01 | P2 | [ ] |
| 68 | CTX-01 | Revisión general | ⚠ PNG no revisado | Confirmar notación y marca de agua | P2 | [ ] |

## DEP-01 Producción (P2)
| # | diagrama | elemento a cambiar | antes | después | prioridad | hecho |
|---|---|---|---|---|---|---|
| 69 | DEP-01 | Estereotipo "Servicio de correo" | `«device» «external»` | Nombre unificado | P2 | [ ] |
| 70 | DEP-01 | Artefactos `application-prod.yml`, `finanzas_db`, `respaldo.sh`, dump | Fuera de la tabla 5.3 | Alinear el diagrama con la tabla 5.3 | P2 | [ ] |
| 71 | DEP-01 | Migraciones V1/V2 | Dentro del contenedor api | Alinear con 5.3 ("ejecutados contra PostgreSQL") | P2 | [ ] |
| 72 | DEP-01 | Artefacto de migración | `V1__esquema.sql` | `V1__esquema.sql` según `V1__esquema.propuesta.sql` | P2 | [ ] |

## FUN-02, DEV-01, DEV-02, DEP-03 (P2)
| # | diagrama | elemento a cambiar | antes | después | prioridad | hecho |
|---|---|---|---|---|---|---|
| 73 | FUN-02 / DEV-02 | Interfaz `IGestionPresupuestos` | ⚠ Confirmar si muestra `eliminar` | Sin `eliminar` (D-3a) | P2 | [ ] |
| 74 | FUN-02, DEV-01, DEV-02, DEP-03 | Revisión general | ⚠ PNG no revisados | Confirmar notación y marca de agua | P2 | [ ] |

## Proyecto `.vpp` (P2)
| # | diagrama | elemento a cambiar | antes | después | prioridad | hecho |
|---|---|---|---|---|---|---|
| 75 | — | `.vpp` antiguos (`_diagramas`, `_5_diagramas`) y `.lck` | ⚠ 3 `.vpp` + `.lck` | Decidir cuál es el vigente (borrar requiere aprobación) | P2 | [ ] |
| 76 | — | Reexportar los 10 PNG a `exportes/` | "evaluation copy" | PNG sin marca (⚠ requiere licencia) | P2 | [ ] |
