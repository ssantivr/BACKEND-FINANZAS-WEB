# SEQ-03 Iniciar sesión (CU-02)

Fuente: CU-02 (`docs/02-funcional/VISTA_FUNCIONAL.md:105,194-198`), RF-03, RF-04, RN-08 (`docs/08-requisitos/REQUERIMIENTOS_FUNCIONALES.md:28,57-72`). Alineado el 22-11-2026 con el código:

- `InicioSesionPage.tsx`, `SesionStore.tsx` y `AuthApi.ts`;
- `AuthController.iniciarSesion` y `CookieSesion`;
- `ServicioAutenticacion.iniciarSesion` y `buscarActivo`;
- `Usuario.estaBloqueado`, `registrarIntentoFallido` y `reiniciarIntentos`.

Sintaxis validada con `mermaid.parse` ([log](../pruebas/mermaid-arquitectura.log)).

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuario
    participant P as InicioSesionPage / SesionStore
    participant A as AuthApi / ClienteHttp
    participant C as AuthController
    participant S as ServicioAutenticacion
    participant RU as IRepositorioUsuarios
    participant US as Usuario
    participant H as ICifradorContrasena (BCrypt)
    participant T as IEmisorTokens (JWT)

    U->>P: escribe correo y clave
    P->>A: iniciarSesion(correo, clave)
    A->>C: POST /api/v1/auth/login (JSON)
    C->>S: iniciarSesion(correo, clave)
    S->>RU: buscarPorCorreo(Email)
    RU-->>S: Optional Usuario, filtrado por estaActiva()
    alt no existe o cuenta ELIMINADA
        S-->>C: ReglaNegocioException CREDENCIALES_INVALIDAS
        C-->>A: 401 mensaje generico
        A-->>P: ErrorApi
        P-->>U: "Correo o contrasena incorrectos"
    else cuenta activa
        S->>US: estaBloqueado(ahora)
        alt bloqueadoHasta > ahora
            S-->>C: ReglaNegocioException CUENTA_BLOQUEADA
            C-->>A: 423 Locked
            A-->>P: ErrorApi
            P-->>U: cuenta bloqueada 15 minutos
        else no bloqueada
            S->>H: coincide(clave, hashContrasena)
            H-->>S: boolean
            alt no coincide
                S->>US: registrarIntentoFallido(ahora)
                Note over US: al 5.o intento, bloqueadoHasta = ahora + 15 min (RN-08)
                S->>RU: guardar(usuario)
                S-->>C: CREDENCIALES_INVALIDAS, o CUENTA_BLOQUEADA si quedo bloqueada
                C-->>A: 401, o 423
                A-->>P: ErrorApi
                P-->>U: mensaje de error
            else coincide
                S->>US: reiniciarIntentos()
                S->>RU: guardar(usuario)
                S->>T: emitir(usuario)
                T-->>S: TokensDTO (acceso 15 min, refresh 7 dias)
                S-->>C: TokensDTO
                C-->>A: 200 token de acceso en JSON + Set-Cookie refresh_token HttpOnly
                A-->>P: guarda el token en AlmacenTokens (memoria)
                P-->>U: redirige a la pagina pedida o a /resumen
            end
        end
    end
```

## Correcciones (22-11-2026, contra el código)

- **Excepciones:** `CredencialesInvalidasException` y `CuentaBloqueadaException` no existen. El código lanza `ReglaNegocioException` con los códigos `CREDENCIALES_INVALIDAS` (401) y `CUENTA_BLOQUEADA` (423), definidos en `ManejadorErrores`.
- **Participante del frontend:** `ClienteWeb` no existe. Los participantes reales son `InicioSesionPage`, `SesionStore` y `AuthApi.iniciarSesion`. El token de acceso se guarda en memoria (`AlmacenTokens`).
- **Contador y bloqueo:** no hay "UPDATE atómico" en el repositorio. Las reglas de bloqueo viven en la entidad `Usuario`:
  - `estaBloqueado(ahora)`;
  - `registrarIntentoFallido(ahora)`, que bloquea al 5.º fallo;
  - `reiniciarIntentos()`.

  Después, el servicio llama a `guardar(usuario)`. Así se resuelve el antiguo supuesto sobre `IRepositorioUsuarios`.
- **Cuentas eliminadas:** `buscarActivo` filtra con `estaActiva()`, de modo que una cuenta `ELIMINADA` responde igual que un correo inexistente (401 genérico). Esto ya está verificado en el código.
- **Fallo que bloquea:** si el fallo que se acaba de registrar deja la cuenta bloqueada, la respuesta es **423** y no 401.
