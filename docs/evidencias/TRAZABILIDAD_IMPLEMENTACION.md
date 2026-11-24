# Trazabilidad: historia de usuario → pantalla → API → pruebas

Une cada historia de usuario ([REQUISITOS_E_HISTORIAS_CONSOLIDADO.md](../08-requisitos/REQUISITOS_E_HISTORIAS_CONSOLIDADO.md)) con lo implementado y con las pruebas que la ejecutan.

Los nombres de las pruebas son los reales:
- **Backend:** métodos de `ServiciosTest` (S), `DominioTest` (D) y `ApiIntegracionTest` (I).
- **Postman:** carpeta y nombre de la petición.
- **Playwright:** título de la prueba en `frontend/e2e/flujo-usuario.spec.ts`.

Resultados en [pruebas/](pruebas/):
- backend: 31 de 31;
- Postman: 44 peticiones y 82 aserciones;
- Playwright: 12 de 12.

Capturas en [capturas/](capturas/).

| HU | Historia | Pantalla (ruta) | API | Backend (S/D/I) | Postman | Playwright |
|---|---|---|---|---|---|---|
| HU-01 | Registrarme | Registro `/registro` | `POST /auth/registro` | S `registroRechazaCorreoDuplicadoYClaveCorta` · I `registroDuplicadoYClaveCorta`, `flujoCompletoDeUnUsuario` | 1. Registro; duplicado → 409; clave corta → 400 | HU-01 registro (2 pruebas) |
| HU-02 | Iniciar sesión | Inicio `/login` | `POST /auth/login` | S `loginBloqueaTrasCincoFallosYNoRevelaElCampo` · D `usuarioSeBloqueaAlQuintoIntento` | 1. Login; login incorrecto → 401 | HU-02/04 sesión |
| HU-03 | Mantener la sesión | (todas) `ClienteHttp` | `POST /auth/renovar` + cabecera CSRF | S `renovarRotaElTokenYCerrarSesionLoRevoca` · I `flujoCompletoDeUnUsuario` (403 sin cabecera; rotación con 30 s de gracia) | 1. Renovar sin cabecera → 403; Renovar con la cookie | HU-03 sesión (recargar) |
| HU-04 | Cerrar sesión | Menú de usuario y menú lateral | `POST /auth/logout` | S `renovarRotaElTokenYCerrarSesionLoRevoca` | 7. Cerrar sesión; renovar tras cerrar → 401 | HU-02/04 sesión |
| HU-05 | Recuperar contraseña | `/recuperar`, `/restablecer` | `POST /auth/recuperacion`, `POST /auth/restablecer` | S `recuperacionNoRevelaCorreosYElEnlaceEsDeUnSoloUso` | 7. Recuperación con correo inexistente → 202 | — (en local el enlace sale en la consola de la API) |
| HU-06 | Registrar gasto | Formulario "Nuevo movimiento" | `POST /movimientos` | S `gastoConCategoriaDeIngresoODeOtroUsuarioFalla` · D `dineroEsExactoYRechazaMasDeDosDecimales`, `gastoNoAceptaCategoriaDeOtroUsuario` | 3. Registrar gasto; RN-01, RN-02 y RN-03 → 422 | HU-06 registrar gasto |
| HU-07 | Registrar ingreso | Formulario "Nuevo movimiento" | `POST /movimientos` | I `flujoCompletoDeUnUsuario` | 3. Registrar ingreso | — |
| HU-08 | Modificar movimiento | Historial → editar | `PUT /movimientos/{id}` | I `flujoCompletoDeUnUsuario` | 3. Modificar gasto | — |
| HU-09 | Eliminar movimiento | Historial → eliminar (confirmación) | `DELETE /movimientos/{id}` | I `flujoCompletoDeUnUsuario` (solo que otro usuario recibe 404) | 6. Eliminar movimiento; eliminado → 404 | HU-09 eliminar movimiento |
| HU-10 | Categorías predeterminadas | Categorías `/categorias` | `GET /categorias` | D `categoriaPredeterminadaNoSeRenombra` | 2. Categorías de gasto y de ingreso | — |
| HU-11 | Crear y renombrar categorías | Categorías → "Nueva categoría" o "Editar" | `POST /categorias`, `PATCH /categorias/{id}` | D `categoriaSinColorRecibeUnoDeLaPaletaYValidaElFormato` | 2. Crear; color inválido; duplicada → 409; renombrar | — |
| HU-12 | Eliminar categoría propia | Categorías → eliminar con destino | `DELETE /categorias/{id}?destino=` | S `eliminarCategoriaReasignaMovimientosYBorraSusPresupuestos` | 6. Sin destino → 422; reasignando; el movimiento pasó a Alimentación | — |
| HU-13 | Definir presupuesto | Presupuestos `/presupuestos` | `PUT /presupuestos` | D `presupuestoSoloParaGastos` | 5. Definir; categoría de ingreso → 422 | HU-13/14 presupuesto |
| HU-14 | Estado de presupuestos | Presupuestos; bloque en Resumen | `GET /presupuestos?periodo=` | D `presupuestoCalculaEstadoYNotificaUnaSolaVez` | 5. Listar presupuestos del mes | HU-13/14 presupuesto ("Excedido" al 112,5 %) |
| HU-15 | Alerta al exceder | Mensaje en Presupuestos y correo | (efecto de registrar o eliminar gastos) | S `alertaDePresupuestoSeEnviaSoloLaPrimeraVez`, `sinCanalDeCorreoLaAlertaNoSeDaPorEnviada` · D `presupuestoVuelveAAvisarSiElGastoBajaDelLimite` | — | HU-09 (vuelve a "En control") |
| HU-16 | Consultar historial | Historial `/movimientos` | `GET /movimientos` | S `historialValidaRangoYTamano` | 4. Historial del mes; rango inválido → 400 | HU-16 historial |
| HU-17 | Filtrar el historial | Historial: tipo, periodo, categoría y búsqueda | `GET /movimientos?tipo&categoria&texto` | I `flujoCompletoDeUnUsuario` (5 casos de texto) | 4. Filtrado por tipo; buscar por texto | HU-17 búsqueda |
| HU-18 | Resumen financiero | Resumen `/resumen` | `GET /resumen` | S `resumenCalculaBalanceYPorcentajes`, `tendenciaDeVariosMesesSeAgrupaPorMes` | 4. Resumen del mes | HU-01 (entra al Resumen) |
| HU-19 | Gasto por categoría | Reportes `/reportes` | `GET /resumen` (porCategoria) | S `resumenCalculaBalanceYPorcentajes` | 4. Resumen del mes (RF-24) | Móvil 375 px (abre Reportes) |
| HU-20 | Eliminar mi cuenta | Cuenta → Zona de peligro | `DELETE /cuenta` | S `cuentaCambiaContrasenaYSeEliminaLogicamente` · D `usuarioEliminadoQuedaInactivo` | 7. Eliminar mi cuenta; login tras eliminar → 401 | HU-20 eliminar cuenta |
| HU-21 | Actualizar mis datos | Cuenta → Datos personales | `GET /cuenta`, `PATCH /cuenta` | — | 7. Ver mi cuenta; actualizar nombre | — |
| HU-22 | Cambiar contraseña | Cuenta → Seguridad | `PUT /cuenta/contrasena` | S `cuentaCambiaContrasenaYSeEliminaLogicamente` | 7. Con la actual mal → 422; cambiar; login con la nueva | — |

## Requisitos no funcionales comprobados

| RNF | Cómo se comprueba |
|---|---|
| Seguridad de la API (RNF-01) | Postman "Sin token → 401" e integración `sinTokenResponde401EnFormatoProblemDetails` |
| CSRF en la cookie de sesión | `renovar` y `logout` exigen `X-Requested-With`. Lo prueban la integración (403) y Postman |
| Montos exactos (RNF-05) | `Decimal128` en MongoDB y D `dineroEsExactoYRechazaMasDeDosDecimales` |
| Arquitectura hexagonal | `ArquitecturaTest` (ArchUnit, 4 reglas) en cada `mvn test` |
| Diseño adaptable | Playwright "Móvil (375 px)" en las 6 páginas y capturas `movil-*.png` |
| Accesibilidad | Roles `dialog` y `alertdialog`, etiquetas y anuncios de error que usan las pruebas de Playwright. El contraste se revisó a ojo en las capturas; con una herramienta es **NO VERIFICADO** |

## Huecos (sin prueba automática en navegador)

Las historias **HU-05, 07, 08, 10, 11, 12, 21 y 22** no tienen prueba en navegador. Sí tienen pruebas de API (Postman) y casi todas de backend.

HU-21 es la única sin prueba de backend propia: solo la cubre Postman.
