# DEV-01 Módulos y dependencias — paquetes con clases

Diagrama: `modelo/Finanzas_Personales_5_diagramas.vpp`, imagen en `modelo/imagenes/Finanzas_Personales_5_diagramas/DEV-01 Módulos y dependencias.png`.

## Estructura

Disposición horizontal: cuatro capas en columnas, de izquierda a derecha (Presentación → Infraestructura → Aplicación → Dominio), con las dependencias en el mismo sentido. Cada capa lleva el nombre de su paquete Java y, dentro, subpaquetes con sus clases, atributos y operaciones:

| Capa | Subpaquetes | Clases |
|---|---|---|
| PRESENTACIÓN · finanzas-web (TypeScript) | app, features, api, components | Enrutador, RutaProtegida; SesionStore «store» y 9 «page»/«component» (una por pantalla de los mockups); ClienteHttp, AlmacenTokens y 6 «api client» (uno por recurso REST); MenuUsuario, DialogoConfirmacion, Aviso |
| INFRAESTRUCTURA · `co.finanzas.infraestructura` | web, configuracion, seguridad, persistencia, notificacion | 7 «controller»/«controllerAdvice», ConfiguracionServicios, ConfiguracionSeguridad, FiltroJwt, EmisorTokensJwt, CifradorBCrypt, 4 Repositorio*Adaptador, NotificadorSmtp |
| APLICACIÓN · `co.finanzas.aplicacion` | dto, puertos.entrada, servicios, puertos.salida | 13 «dto», 7 puertos de entrada, 6 «service», 7 puertos de salida |
| DOMINIO · `co.finanzas.dominio` | modelo, valor, tipos, excepciones | Las 12 clases de LOG-01 y 2 excepciones |

## Decisiones de diseño

1. **Un solo modelo de dominio.** Las clases de `modelo`, `valor` y `tipos` son los mismos elementos de LOG-01 mostrados en otra vista: si se cambia un atributo en LOG-01, cambia también en DEV-01.
2. **Dependencias hacia adentro** (regla hexagonal): infraestructura → aplicación → dominio. Las antiguas *Realization* entre cajas (con la flecha al revés) se reemplazaron por dependencias rotuladas «implementa» desde el implementador hacia el puerto, que es lo válido en UML entre paquetes.
3. **Estereotipos de rol:** «controller», «controllerAdvice», «configuration», «service», «adapter», «dto», «Interface», «exception», «entity», «value object», «enumeration».
4. **Atributos de los servicios y adaptadores** = dependencias inyectadas por constructor (`final`).
5. **Cliente Web:** atributos = estado de la pantalla (campos del formulario, datos cargados); operaciones = acciones del usuario. Las páginas solo hablan con la API a través de `api` (`features → api → web`, «HTTPS / JSON»). Los montos viajan como `string` para no perder decimales en JavaScript; el token de renovación vive en una cookie `HttpOnly`, por eso `AlmacenTokens` solo guarda el de acceso.

## Origen de los miembros

| Elementos | Fuente |
|---|---|
| Puertos de entrada y salida, servicios, DTO principales | `docs/03-logica/VISTA_LOGICA.md` §3.5 (LOG-02) |
| Clases de dominio | LOG-01 (mismo modelo) |
| Controladores (rutas) | Columna *Interfaz* de los RF en `docs/08-requisitos/REQUISITOS_E_HISTORIAS_CONSOLIDADO.md` |
| `IGestionCuenta`, `ServicioCuenta`, `CuentaController` | RF-25 a RF-27 (`docs/08-requisitos/configuracion/`) |

## Agregados respecto a LOG-02 (propuestos)

- `IEmisorTokens.validarAcceso(token) : UUID`: el filtro JWT necesita validar el token de acceso, y LOG-02 solo tenía `validarRefresh`.
- `IEmisorTokens.revocarTodas(idUsuario) : void`: lo piden RF-08, RF-25 y RF-27 («revocar todas las sesiones»).
- Campos de `CategoriaDTO`, `PresupuestoDTO`, `UsuarioDTO`, `TokensDTO` y `RegistroCmd`: LOG-02 dice «campos equivalentes»; se tomaron de los atributos de dominio y de los RF.
- Todas las clases del Cliente Web: derivadas de las carpetas de `VISTA_DESARROLLO.md` §4.2, de los mockups y de las rutas de los RF.
- Firmas de controladores, `FiltroJwt`, `ConfiguracionSeguridad` y `ConfiguracionServicios`: derivadas de los puertos y de Spring Boot 3.3.

## Correspondencia con el código (2026-11-17)

DEV-01 se implementó en `backend/` (paquetes `co.finanzas.*`) y `frontend/src/` (`app`, `features`, `api`, `components`). Al implementar se ajustó el diagrama:

- Persistencia en MongoDB ([ADR-004](../../architecture/ADR-004-mongodb.md)): `*RepositorioJpa` → `*RepositorioMongo`.
- `IEmisorTokens` gana `crearTokenRecuperacion` y `usarTokenRecuperacion` (RF-07, RF-08).
- `IGestionCuenta.cambiarContrasena` devuelve `TokensDTO`, para que el dispositivo actual siga conectado (HU-22).
- `IRepositorioMovimientos.contarPorCategoria`, `IRepositorioPresupuestos.buscarPorId` y `eliminarPorCategoria` (RF-17, sin presupuestos huérfanos).
- Los servicios reciben `reloj : Clock` (fechas comprobables en pruebas). `ServicioCategorias` usa también `repoPresupuestos` y `ServicioPresupuestos` usa `repoCategorias` y `repoUsuarios`.
- `InicializadorCategorias` carga las categorías predeterminadas al arrancar (RF-14).

Las firmas del backend se comprueban al compilar y la regla de capas se verifica con ArchUnit en cada `./mvnw test` (`docs/evidencias/pruebas/backend-mvn-test.log`).
