# IMP-05 — Secuencia: renovar la sesión (HU-03, RF-05)

**Fuente:** las clases y métodos del código:
- `ClienteHttp.renovarConResultado`
- `AuthController.renovar` y `exigirPeticionDeLaApp`
- `ServicioAutenticacion.renovarSesion`
- `EmisorTokensJwt.retirar` y `GRACIA_ROTACION`

**Renderizado:** verificado. La imagen está en [img/IMP-05_SEQ_Renovar_sesion.svg](img/IMP-05_SEQ_Renovar_sesion.svg).

**Pruebas que la cubren:**
- `ApiIntegracionTest`: 403 sin cabecera; con el token rotado, 200 una vez y después 401.
- Postman: carpeta 1.
- Playwright: "HU-03 sesión".

```mermaid
sequenceDiagram
  autonumber
  participant N as Navegador (ClienteHttp)
  participant A as AuthController
  participant S as ServicioAutenticacion
  participant E as EmisorTokensJwt
  participant M as MongoDB (sesiones)

  Note over N: al cargar la página o cuando una petición da 401<br/>(una sola renovación a la vez)
  N->>A: POST /api/v1/auth/renovar<br/>cookie refresh_token (HttpOnly)<br/>X-Requested-With: XMLHttpRequest
  alt sin la cabecera (petición desde otro sitio)
    A-->>N: 403 ORIGEN_NO_PERMITIDO (protección CSRF)
  else con la cabecera
    A->>S: renovarSesion(refresh)
    S->>E: validarRefresh(refresh)
    E->>M: buscar por hash SHA-256
    alt no existe, caducado o ya usado dentro de la gracia
      E-->>S: TOKEN_INVALIDO
      S-->>A: excepción
      A-->>N: 401, el cliente cierra la sesión
    else válido
      S->>E: retirar(refresh)
      E->>M: 1.er uso: reemplazadaEn = ahora,<br/>expiraEn = mín(expiraEn, ahora + 30 s)<br/>2.º uso: se borra
      S->>E: emitir(usuario)
      E->>M: insertar sesión nueva (hash, expiraEn 7 días)
      E-->>S: accessToken (15 min) + refresh nuevo
      A-->>N: 200 {accessToken}<br/>Set-Cookie refresh_token nuevo
      Note over N: el access token solo vive en memoria
    end
  end
  Note over N: si falla la red o hay un 5xx, la sesión NO se cierra:<br/>"No hay conexión con el servidor"
```

Los 30 s de gracia evitan que dos pestañas que renuevan a la vez se cierren la sesión entre sí. El token anterior sirve una sola vez más dentro de esa ventana.
