# IMP-03 — Despliegue (producción y entorno local)

**Fuentes:**
- [`frontend/vercel.json`](../../../frontend/vercel.json): reescritura de `/api` y cabeceras de seguridad.
- [`render.yaml`](../../../render.yaml): servicio `finanzas-api`, plan gratuito, con comprobación de salud en `/actuator/health`.
- [`iniciar.cmd`](../../../iniciar.cmd): arranque local.

**Renderizado:** verificado. La imagen está en [img/IMP-03_Despliegue.svg](img/IMP-03_Despliegue.svg).

Que el sitio publicado funcione con esta versión es **NO VERIFICADO**; el detalle está en la sección 0 del [informe final](../../INFORME_FINAL.md).

```mermaid
flowchart TB
  subgraph prod["Producción"]
    NAV["Navegador del usuario"]
    subgraph vercel["Vercel (CDN)"]
      SPA["React compilado (dist/)<br/>rewrite /api/* → Render"]
    end
    subgraph render["Render · finanzas-api (Docker)"]
      API["Spring Boot 3.5 · Java 21<br/>health: /actuator/health"]
    end
    subgraph atlas["MongoDB Atlas"]
      DBP[("finanzas")]
    end
    NAV -- "HTTPS (mismo dominio:<br/>cookie de sesión first-party)" --> SPA
    SPA -- "HTTPS /api/*" --> API
    API -- "MONGODB_URI (TLS)" --> DBP
  end

  subgraph local["Equipo local (iniciar.cmd)"]
    NL["Navegador"]
    VITE["Vite :5173<br/>proxy /api"]
    APIL["API :8080<br/>perfil local"]
    DBL[("Servicio MongoDB :27017<br/>finanzas")]
    PM["Postman / newman"]
    CP["MongoDB Compass"]
    NL --> VITE --> APIL --> DBL
    PM --> APIL
    CP --> DBL
  end
```

| Variable (Render) | Uso |
|---|---|
| `MONGODB_URI` | Cadena de MongoDB Atlas con el nombre de la base |
| `JWT_SECRET` | Firma de los tokens, mínimo 32 caracteres |
| `FRONTEND_URL` | Enlaces de recuperación de contraseña |
| `CORS_ORIGINS` | Dominio de Vercel |
| `COOKIE_SECURE`, `COOKIE_SAMESITE` | Cookie de sesión `Secure` y `Lax`, porque es del mismo sitio gracias a la reescritura |
| `MAIL_FROM` (+ `SPRING_MAIL_*`) | Remitente y servidor de correo |
