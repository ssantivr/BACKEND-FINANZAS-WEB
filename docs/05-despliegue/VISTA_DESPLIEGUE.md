# 5. Vista de Despliegue

## 5.1 Topología de producción (`DEP-01`)

Infraestructura pequeña y realista para el volumen esperado (cientos a pocos miles de usuarios): dos
máquinas virtuales en un proveedor cloud, la BD en subred privada, más dos servicios externos.

| Nodo | Estereotipo | Especificación | Contiene |
|---|---|---|---|
| **Dispositivo del usuario** | `«device»` | PC o móvil | `«executionEnvironment» Navegador web` (Chrome/Firefox/Safari/Edge, 2 últimas versiones) → artefacto `finanzas-web` (descargado y ejecutado) |
| **vm-app** | `«device»` | VM 2 vCPU / 4 GB RAM / 40 GB SSD, Ubuntu Server 24.04 LTS, subred **pública** (solo 80/443 abiertos) | `«executionEnvironment» Docker Engine 27` → contenedores: |
| ↳ `nginx` | `«executionEnvironment» «container»` | Nginx 1.27 | `finanzas-web (dist/)` (estáticos), `finanzas.conf` (TLS Let's Encrypt, HSTS, proxy `/api` → `api:8080`, *rate limit* 10 r/s en `/api/v1/auth`) |
| ↳ `api` | `«executionEnvironment» «container»` | Eclipse Temurin JRE 21 | `finanzas-api.jar` (Spring Boot), `application-prod.yml` |
| **vm-bd** | `«device»` | VM 2 vCPU / 4 GB RAM / 80 GB SSD cifrado, Ubuntu 24.04, subred **privada** (5432 solo desde vm-app) | `«executionEnvironment» PostgreSQL 16` → `«artifact» finanzas_db` (esquema creado por `V1__esquema.sql`); `«artifact» respaldo.sh` (cron 02:00, `pg_dump` + cifrado `age`) |
| **Servicio de correo** | `«device» «external»` | Proveedor SMTP transaccional (p. ej. SendGrid/SES) | — |
| **Almacenamiento de respaldos** | `«device» «external»` | Bucket S3-compatible con versionado y retención 30 días | `«artifact» finanzas_db_AAAAMMDD.dump.age` |

> Por qué no "un artefacto por módulo" (error P2): el backend es un monolito modular; los componentes
> *Identidad, Movimientos, Categorías, Presupuestos, Consultas, Persistencia, Seguridad, Notificaciones* y
> *API REST* se empaquetan en **un** `finanzas-api.jar`.

## 5.2 Rutas de comunicación

| # | Extremo iniciador → receptor | Estereotipo | Puerto | Seguridad |
|---|---|---|---|---|
| CP1 | Navegador web → nginx | `«HTTPS»` | 443 (80 solo redirige) | TLS 1.2/1.3, HSTS |
| CP2 | nginx → api | `«HTTP»` | 8080 (red interna Docker `backend`, no publicada) | aislada en el host |
| CP3 | api → PostgreSQL 16 | `«JDBC/TLS»` | 5432 | `sslmode=verify-full`, usuario de BD con permisos mínimos (sin DDL en runtime; Flyway usa otro rol) |
| CP4 | api → Servicio de correo | `«SMTP»` | 587 STARTTLS | credencial en variable de entorno / *secret* |
| CP5 | vm-bd → Almacenamiento de respaldos | `«HTTPS»` | 443 | clave de acceso solo de escritura |

## 5.3 Artefactos y manifestaciones

| Artefacto | Tipo | `«manifest»` de | Desplegado en |
|---|---|---|---|
| `finanzas-web (dist/)` | bundle JS/CSS/HTML (Vite) | Cliente Web | nginx (servido) → Navegador web (ejecutado) |
| `finanzas-api.jar` | Spring Boot fat-jar | API REST, Identidad, Movimientos, Categorías, Presupuestos, Consultas, Persistencia, Seguridad, Notificaciones | contenedor `api` |
| `V1__esquema.sql`, `V2__categorias_predeterminadas.sql` | scripts Flyway | esquema de la Vista de Desarrollo §4.3 | ejecutados contra PostgreSQL 16 (producen `finanzas_db`) |
| `finanzas.conf` | configuración | — | contenedor `nginx` |
| `docker-compose.yml` | descriptor de despliegue | — | vm-app |

## 5.4 Variante de desarrollo local (`DEP-02`, opcional)

Un solo `«device» PC del desarrollador` con `«executionEnvironment» Docker Desktop` que levanta
`postgres:16`, `api` (o el IDE con `mvn spring-boot:run`) y `vite dev` en `localhost:5173` (proxy
`/api` → `localhost:8080`); correo con *Mailpit* en `localhost:8025`.

## 5.5 Cómo modelarlo en Visual Paradigm

1. `5 Despliegue` › *New Diagram › Deployment Diagram* → `DEP-01 Producción`.
2. **Nodos de hardware:** herramienta *Node* → `Dispositivo del usuario`, `vm-app`, `vm-bd`. Estereotipo
   `«device»`. En *Open Specification › Tagged Values* añade `so`, `cpu`, `ram`, `subred`.
3. **Entornos de ejecución:** herramienta *Node* **anidada** dentro del nodo padre (arrástrala dentro
   hasta que se resalte) con estereotipo `«executionEnvironment»`: `Navegador web` dentro del
   dispositivo; `Docker Engine` dentro de `vm-app` y, dentro de él, `nginx` y `api`; `PostgreSQL 16` dentro
   de `vm-bd`. (Corrige P1: el navegador no es `«device»`.)
4. **Sistemas externos:** *Node* con estereotipos `«device»` + `«external»` (créalo una vez), con borde
   punteado o color gris.
5. **Artefactos:** herramienta *Artifact* dentro del entorno de ejecución correspondiente (tabla 5.3).
6. **Manifestación:** herramienta *Manifestation* (o *Dependency* con estereotipo `«manifest»`) **desde el
   artefacto hacia el componente**; arrastra los componentes desde el *Model Explorer* (los de `DEV-02`)
   a un costado del diagrama, o crea un diagrama auxiliar `DEP-03 Manifestaciones` para no saturar.
7. **Rutas de comunicación:** herramienta *Communication Path* entre nodos (no *Association* genérica);
   nombre = protocolo de la tabla 5.2 y estereotipo `«HTTPS»`, `«JDBC/TLS»`, etc.; puerto en *Tagged
   Values*. Dibuja desde el iniciador hacia el receptor y activa la navegabilidad solo en el receptor
   (corrige P4).
8. **Zonas de red (opcional):** usa *Boundary* o un *Node* `«zone»` para "Subred pública" y "Subred
   privada", con una *Note* de reglas de firewall: `0.0.0.0/0 → vm-app:443`, `vm-app → vm-bd:5432`, nada
   más.
