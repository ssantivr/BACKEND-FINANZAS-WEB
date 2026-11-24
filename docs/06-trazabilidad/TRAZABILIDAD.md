# 6. Matriz de trazabilidad entre vistas

Comprueba que cada caso de uso tiene interfaz, componente, clases, artefacto y nodo — la verificación que
faltaba en v1.0 (errores T1/T2).

| CU | Interfaz de entrada | Componente | Servicio / clases de dominio | Puertos de salida | Artefacto | Nodo / entorno |
|---|---|---|---|---|---|---|
| CU-01 Registrarse | `IAutenticacion.registrar` | Identidad | `ServicioAutenticacion` / `Usuario`, `Email` | `IRepositorioUsuarios`, `ICifradorContrasena` | `finanzas-api.jar` | vm-app › api |
| CU-02 Iniciar sesión | `IAutenticacion.iniciarSesion` | Identidad | `ServicioAutenticacion` / `Usuario` | `IRepositorioUsuarios`, `ICifradorContrasena`, `IEmisorTokens` | `finanzas-api.jar` | vm-app › api |
| CU-03 Registrar movimiento | `IGestionMovimientos.registrarGasto/Ingreso` | Movimientos | `ServicioMovimientos` / `Gasto`, `Ingreso`, `Categoria`, `Dinero` | `IRepositorioMovimientos`, `IRepositorioCategorias`, `IEvaluadorPresupuesto` | `finanzas-api.jar` | vm-app › api |
| CU-04 Modificar/eliminar | `IGestionMovimientos.modificar/eliminar` | Movimientos | `ServicioMovimientos` / `Movimiento` | `IRepositorioMovimientos`, `IRepositorioCategorias` | `finanzas-api.jar` | vm-app › api |
| CU-05 Gestionar categorías | `IGestionCategorias` | Categorías | `ServicioCategorias` / `Categoria` | `IRepositorioCategorias`, `IRepositorioMovimientos` | `finanzas-api.jar` | vm-app › api |
| CU-06 Gestionar presupuestos | `IGestionPresupuestos` | Presupuestos | `ServicioPresupuestos` / `Presupuesto` | `IRepositorioPresupuestos` | `finanzas-api.jar` | vm-app › api |
| CU-07 Consultar historial | `IConsultaFinanciera.consultarHistorial` | Consultas | `ServicioConsultaFinanciera` / `FiltroMovimientos`, `Pagina` | `IRepositorioMovimientos` | `finanzas-api.jar` | vm-app › api |
| CU-08 Resumen financiero | `IConsultaFinanciera.obtenerResumen` | Consultas | `ServicioConsultaFinanciera` / `ResumenFinanciero`, `TotalPorCategoria` | `IRepositorioMovimientos`, `IRepositorioPresupuestos` | `finanzas-api.jar` | vm-app › api |
| CU-09 Recuperar contraseña | `IAutenticacion.solicitarRecuperacion/restablecer` | Identidad, Notificaciones | `ServicioAutenticacion` / `Usuario` | `INotificador`, `IRepositorioUsuarios` | `finanzas-api.jar` | vm-app › api → Servicio de correo |
| CU-10 Notificar presupuesto | `IEvaluadorPresupuesto.evaluar` | Presupuestos, Notificaciones | `ServicioPresupuestos` / `Presupuesto` | `INotificador` | `finanzas-api.jar` | vm-app › api → Servicio de correo |
| CU-11 Período y filtros | (parte de `IConsultaFinanciera`) | Cliente Web, Consultas | `FiltroMovimientos` | — | `finanzas-web (dist/)`, `finanzas-api.jar` | Navegador, vm-app |
| Todos | API REST `/api/v1` | Cliente Web, API REST | `*Controller` | — | `finanzas-web (dist/)`, `finanzas-api.jar` | Navegador › nginx › api |
| Persistencia de todos | `IRepositorio*` | Persistencia | `Repositorio*Adaptador` | JDBC | `finanzas-api.jar` + `V1__esquema.sql` | api → vm-bd › PostgreSQL 16 |

## Lista de verificación antes de entregar

- [ ] Ningún nombre de elemento aparece dos veces en el *Model Explorer* (busca con *Ctrl+F*).
- [ ] Cada CU de `FUN-01` tiene especificación (*Use Case Details*) y al menos un ACT o SEQ enlazado.
- [ ] Cada *Interface* tiene exactamente un componente que la realiza y al menos uno que la usa.
- [ ] En `LOG-01/02` no hay tipos escritos como texto libre que no existan como clase/enum/interface.
- [ ] Ningún componente se llama "Base de Datos"; la BD solo aparece en Despliegue.
- [ ] Cada artefacto de `DEP-01` tiene `«manifest»` y cada componente de `DEV-02` está manifestado por algún artefacto.
- [ ] Ninguna relación usa estereotipos inventados (`«usa»`, `«realiza»`).
- [ ] `Tools › Model Quality Checker` sin errores; exportes regenerados en `exportes/`.
