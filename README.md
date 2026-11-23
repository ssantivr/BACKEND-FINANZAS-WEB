# Backend — finanzas-api

Java 21 · Spring Boot 3.5 · Spring Security (JWT) · Spring Data MongoDB. Arquitectura hexagonal con los paquetes de DEV-01:

```
co.finanzas
├── dominio          modelo/ valor/ tipos/ excepciones/   (Java puro, sin Spring)
├── aplicacion       puertos/entrada  puertos/salida  servicios/  dto/
└── infraestructura  web/ seguridad/ persistencia/ notificacion/ configuracion/
```

ArchUnit comprueba la regla de dependencias en cada `./mvnw test` (`ArquitecturaTest`).

## Endpoints (`/api/v1`)

| Método y ruta | RF | Auth |
|---|---|---|
| `POST /auth/registro`, `/auth/login`, `/auth/renovar`, `/auth/logout` | RF-01 a 06 | No |
| `POST /auth/recuperacion`, `/auth/restablecer` | RF-07, RF-08 | No |
| `GET/PATCH/DELETE /cuenta`, `PUT /cuenta/contrasena` | RF-25 a 27 | Bearer |
| `POST /movimientos`, `PUT/DELETE /movimientos/{id}` | RF-09 a 13 | Bearer |
| `GET /movimientos?desde&hasta&tipo&categoria&pagina&tamano`, `GET /movimientos/{id}` | RF-21, RF-22 | Bearer |
| `GET /resumen?desde&hasta` (totales, % por categoría, presupuestos, variación frente al periodo anterior, promedio diario y tendencia por semana o mes) | RF-23, RF-24 | Bearer |
| `GET/POST /categorias` (`color` opcional, `#RRGGBB`), `PATCH/DELETE /categorias/{id}?destino=` | RF-14 a 17 | Bearer |
| `PUT/GET /presupuestos`, `DELETE /presupuestos/{id}` | RF-18, RF-19 | Bearer |
| `GET /actuator/health` | — | No |

- **Sesión:** token de acceso JWT de 15 min en `Authorization: Bearer`, más un token de renovación de 7 días en la cookie `HttpOnly` `refresh_token`. Se rota en cada renovación y en la base solo se guarda su hash SHA-256.
- **Errores:** Problem Details (RFC 9457) en español, con un campo `codigo` (por ejemplo `CATEGORIA_TIPO`, `CORREO_DUPLICADO`).

## Variables de entorno

| Variable | Obligatoria | Ejemplo |
|---|---|---|
| `MONGODB_URI` | Sí (producción) | `mongodb+srv://usuario:clave@cluster.mongodb.net/finanzas` |
| `JWT_SECRET` | Sí | 32 caracteres o más, aleatorio |
| `FRONTEND_URL` | Sí | `https://finanzas-personales.vercel.app` |
| `CORS_ORIGINS` | No | `https://finanzas-personales.vercel.app` |
| `COOKIE_SECURE` / `COOKIE_SAMESITE` | No | `true` / `Lax` |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`… | No | Sin SMTP, los correos no se envían y queda una advertencia en el log |

## Comandos

```bash
./mvnw test
```

```bash
./mvnw spring-boot:run -Dspring-boot.run.useTestClasspath=true -Dspring-boot.run.profiles=local
```

```bash
./mvnw package -DskipTests
```

Con el MongoDB instalado en el equipo (servicio en `localhost:27017`), arranca sin la base embebida:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Las pruebas y el modo local usan un MongoDB 7.0 embebido (flapdoodle): la primera vez descarga el binario de MongoDB.

La imagen Docker (`Dockerfile`) es la que usa Render. Construirla está **NO VERIFICADO**: este equipo no tiene Docker.
