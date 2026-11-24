# Vista funcional y escenarios (modelo 4+1 de Kruchten)

**Qué responde:** quién usa el sistema, qué puede hacer y cómo colaboran los objetos en los flujos más importantes. Los casos de uso salen de los puertos de entrada (`aplicacion/puertos/entrada/*.java`) y de las rutas REST de los controllers.

## 1. Diagrama de casos de uso

**Leyenda:**

- figura con 👤: actor;
- óvalo: caso de uso;
- línea continua: el actor participa;
- `«include»`: el caso base **siempre** ejecuta al incluido;
- `«extend»`: el caso extensor **a veces** se añade al base, si se cumple una condición.

Mermaid no tiene un diagrama de casos de uso nativo, así que se dibuja con un `flowchart`.

```mermaid
---
title: Vista funcional - casos de uso de Finanzas Personales
---
flowchart LR
    visitante(["👤 Visitante"])
    usuario(["👤 Usuario registrado"])
    correo(["👤 Servicio de correo<br/>(sistema externo)"])
    subgraph SIS["Sistema Finanzas Personales"]
        direction TB
        CU01(["CU-01 Registrarse"])
        CU02(["CU-02 Iniciar sesión"])
        CU03(["CU-03 Recuperar contraseña"])
        CU04(["CU-04 Registrar gasto"])
        CU05(["CU-05 Registrar ingreso"])
        CU06(["CU-06 Modificar o eliminar movimiento"])
        CU07(["CU-07 Consultar historial y resumen"])
        CU08(["CU-08 Gestionar categorías"])
        CU09(["CU-09 Definir presupuesto mensual"])
        CU10(["CU-10 Gestionar cuenta"])
        CU11(["Evaluar presupuesto"])
        CU12(["Enviar alerta de presupuesto"])
        CU13(["Bloquear cuenta por intentos fallidos"])
        CU14(["Enviar enlace de recuperación"])
        CU15(["Reasignar movimientos a otra categoría"])
    end
    visitante --- CU01
    visitante --- CU02
    visitante --- CU03
    usuario --- CU04
    usuario --- CU05
    usuario --- CU06
    usuario --- CU07
    usuario --- CU08
    usuario --- CU09
    usuario --- CU10
    CU04 -. "«include»" .-> CU11
    CU12 -. "«extend» (consumo ≥ límite)" .-> CU11
    CU13 -. "«extend» (5 intentos fallidos)" .-> CU02
    CU03 -. "«include»" .-> CU14
    CU08 -. "«include» (al eliminar)" .-> CU15
    CU12 --- correo
    CU14 --- correo
```

| Caso de uso | Puerto y método | Endpoint REST |
|---|---|---|
| CU-01 Registrarse | `IAutenticacion.registrar` | `POST /api/v1/auth/registro` → 201 |
| CU-02 Iniciar sesión | `IAutenticacion.iniciarSesion` | `POST /api/v1/auth/login` → 200 |
| CU-03 Recuperar contraseña | `solicitarRecuperacion` y `restablecerContrasena` | `POST /auth/recuperacion` y `POST /auth/restablecer` |
| CU-04 y CU-05 Registrar gasto o ingreso | `IGestionMovimientos.registrarGasto` y `registrarIngreso` | `POST /api/v1/movimientos` → 201 |
| CU-06 Modificar o eliminar movimiento | `modificar` y `eliminar` | `PUT` y `DELETE /api/v1/movimientos/{id}` (el borrado responde 204) |
| CU-07 Consultar historial y resumen | `IConsultaFinanciera` | `GET /movimientos`, `GET /movimientos/{id}` y `GET /resumen` |
| CU-08 Gestionar categorías | `IGestionCategorias` | `GET`, `POST`, `PATCH` y `DELETE /api/v1/categorias` |
| CU-09 Definir presupuesto mensual | `IGestionPresupuestos` | `PUT`, `GET` y `DELETE /api/v1/presupuestos` |
| CU-10 Gestionar cuenta | `IGestionCuenta` | `GET`, `PATCH`, `PUT /contrasena` y `DELETE /api/v1/cuenta` |
| Evaluar presupuesto | `IEvaluadorPresupuesto.evaluar` | Ninguno: es interno y lo llama CU-04 |

**Decisiones de diseño:**

- "Evaluar presupuesto" es un `«include»` porque `ServicioMovimientos.registrarGasto` **siempre** llama a `evaluador.evaluar(g)`. Enviar la alerta es un `«extend»` porque solo ocurre si `debeNotificar(consumo)` es verdadero y hay SMTP configurado.
- El "Servicio de correo" es un actor secundario: el sistema lo usa, pero él nunca inicia un caso de uso.
- Los casos de uso del Usuario registrado exigen un token JWT válido, que comprueba `FiltroJwt`. Por eso se separaron del Visitante.

## 2. Secuencia 1: registrar un gasto (CU-04)

**Leyenda:**

- `->>`: mensaje síncrono;
- `-->>`: respuesta;
- `alt` / `else`: alternativas;
- `opt`: fragmento opcional.

Los números son el orden de los mensajes.

```mermaid
---
title: Secuencia CU-04 Registrar gasto (con evaluación de presupuesto)
---
sequenceDiagram
    autonumber
    actor U as Usuario
    participant SPA as SPA (MovimientosApi)
    participant F as FiltroJwt
    participant C as MovimientoController
    participant S as ServicioMovimientos
    participant RC as IRepositorioCategorias
    participant RM as IRepositorioMovimientos
    participant E as ServicioPresupuestos (IEvaluadorPresupuesto)
    participant N as INotificador
    participant DB as MongoDB
    U->>SPA: llena el formulario y pulsa Guardar
    SPA->>F: POST /api/v1/movimientos (Bearer token, JSON)
    F->>F: validarAcceso(token) obtiene idUsuario
    F->>C: registrar(req)
    C->>S: registrarGasto(RegistrarGastoCmd)
    S->>RC: buscarDisponible(idCategoria, idUsuario)
    RC->>DB: find categorias
    DB-->>RC: documento
    alt la categoría no existe o no es del usuario
        RC-->>S: Optional.empty
        S-->>C: ReglaNegocioException CATEGORIA_INVALIDA
        C-->>SPA: 422 Unprocessable Entity (ManejadorErrores)
        SPA-->>U: muestra "La categoría no existe"
    else categoría válida
        RC-->>S: Categoria
        S->>S: new Gasto(...) valida monto > 0 y fecha
        S->>RM: guardar(gasto)
        RM->>DB: insert movimientos (tipo = GASTO, monto Decimal128)
        DB-->>RM: ok
        S->>E: evaluar(gasto)
        E->>RM: sumarGastos(idUsuario, idCategoria, periodo)
        RM-->>E: consumo : Dinero
        opt debeNotificar(consumo) y hay SMTP configurado
            E->>N: enviarAlertaPresupuesto(correo, presupuesto)
            N-->>E: enviado
        end
        E-->>S: fin de la evaluación
        S-->>C: MovimientoDTO
        C-->>SPA: 201 Created + JSON del movimiento
        SPA-->>U: muestra el gasto en el historial
    end
```

**Decisiones de diseño:**

- El controlador no sabe de presupuestos. Es el servicio quien encadena la evaluación, y así la regla RF-20 vive en la capa de negocio.
- La categoría se busca con el `idUsuario` del token, nunca con uno enviado en el JSON. Así se evita que un usuario use las categorías de otro.
- Un error de regla de negocio sube como excepción y `ManejadorErrores` lo convierte en `422`, con un cuerpo `ProblemDetail` y el código `CATEGORIA_INVALIDA`.

## 3. Secuencia 2: iniciar sesión (CU-02)

```mermaid
---
title: Secuencia CU-02 Iniciar sesión (con bloqueo por intentos)
---
sequenceDiagram
    autonumber
    actor V as Visitante
    participant SPA as SPA (AuthApi)
    participant C as AuthController
    participant S as ServicioAutenticacion
    participant RU as IRepositorioUsuarios
    participant CF as ICifradorContrasena (BCrypt)
    participant T as IEmisorTokens (JWT)
    V->>SPA: escribe correo y contraseña
    SPA->>C: POST /api/v1/auth/login (JSON)
    C->>S: iniciarSesion(correo, clave)
    S->>RU: buscarPorCorreo(correo)
    RU-->>S: Usuario activo
    alt cuenta bloqueada (estaBloqueado)
        S-->>C: ReglaNegocioException CUENTA_BLOQUEADA
        C-->>SPA: 423 Locked
        SPA-->>V: "Cuenta bloqueada 15 minutos"
    else contraseña incorrecta
        S->>CF: coincide(clave, hash)
        CF-->>S: false
        S->>S: registrarIntentoFallido(ahora)
        S->>RU: guardar(usuario)
        S-->>C: CREDENCIALES_INVALIDAS (o CUENTA_BLOQUEADA en el 5.º intento)
        C-->>SPA: 401 Unauthorized (423 Locked si quedó bloqueada)
        SPA-->>V: "Correo o contraseña incorrectos"
    else contraseña correcta
        S->>CF: coincide(clave, hash)
        CF-->>S: true
        S->>S: reiniciarIntentos()
        S->>RU: guardar(usuario)
        S->>T: emitir(usuario)
        T-->>S: TokensDTO (acceso 15 min, refresh 7 días)
        S-->>C: TokensDTO
        C-->>SPA: 200 OK, token de acceso en el JSON y Set-Cookie refresh_token (HttpOnly)
        SPA-->>V: redirige a /resumen
    end
```

**Decisiones de diseño:**

- El mensaje de error es el mismo si el correo no existe o si la clave es incorrecta. Así no se revela qué correos están registrados.
- El token de acceso vive solo en memoria (`AlmacenTokens.ts`) y el de renovación en una cookie `HttpOnly`, que JavaScript no puede leer. Esto reduce el riesgo de robo por XSS.
- El bloqueo (RN-08: 5 intentos fallidos, 15 minutos) es una regla de `Usuario`, no del controlador. Por eso se puede probar con una prueba unitaria del dominio.

## Supuestos

- Los textos que muestra la SPA en cada error son aproximados. Los códigos HTTP y los códigos de error (`CATEGORIA_INVALIDA`, `CREDENCIALES_INVALIDAS`, `CUENTA_BLOQUEADA`) sí son los del código.
- Si el correo no existe, `buscarActivo(correo)` lanza `CREDENCIALES_INVALIDAS` (401) sin consultar BCrypt. Esto se verificó en `ServicioAutenticacion.java`, pero no se dibujó como rama aparte para no recargar el diagrama.
- La numeración CU-01 a CU-10 es de este documento. Su correspondencia con RF y HU está en FUN-01 y en `docs/06-trazabilidad`.
