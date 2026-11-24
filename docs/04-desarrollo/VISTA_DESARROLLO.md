# 4. Vista de Desarrollo

## 4.1 Estilo arquitectónico

**Monolito modular con arquitectura hexagonal (puertos y adaptadores)** — ver ADR-001 y ADR-002.

- **Stack:** backend Java 21 + Spring Boot 3.3 (Maven); frontend React 18 + TypeScript + Vite;
  PostgreSQL 16; migraciones con Flyway.
- **Regla de dependencias:** `infraestructura → aplicacion → dominio`. El dominio no importa Spring,
  JPA ni nada de infraestructura. Corrige D4 (en v1.0 la lógica dependía de la persistencia y esta de
  una "Base de Datos" componente).

```
            ┌───────────────────── infraestructura ─────────────────────┐
            │  entrada (adaptadores primarios)   salida (secundarios)    │
            │  web.controladores, web.seguridad  persistencia.jpa,       │
            │                                    notificacion.smtp,      │
            │                                    seguridad.bcrypt/jwt    │
            └──────────────┬──────────────────────────────▲──────────────┘
                           │ usa                           │ implementa
            ┌──────────────▼──────────── aplicacion ───────┴──────────────┐
            │ puertos.entrada (IGestion*, IConsultaFinanciera, IAutentic.) │
            │ servicios (Servicio*)      puertos.salida (IRepositorio*, …) │
            └──────────────┬───────────────────────────────────────────────┘
                           │ usa
            ┌──────────────▼──────────── dominio ───────────────────────────┐
            │ Usuario, Movimiento, Gasto, Ingreso, Categoria, Presupuesto,  │
            │ Dinero, Email, enumeraciones, excepciones de dominio          │
            └───────────────────────────────────────────────────────────────┘
```

## 4.2 Estructura de paquetes / código (`DEV-01`)

```
finanzas-personales/
├── backend/                                   (módulo Maven: finanzas-api)
│   └── src/main/java/co/finanzas/
│       ├── dominio/
│       │   ├── modelo/        Usuario, Movimiento, Gasto, Ingreso, Categoria, Presupuesto
│       │   ├── valor/         Dinero, Email
│       │   ├── tipos/         TipoMovimiento, MetodoPago, EstadoCuenta, EstadoPresupuesto
│       │   └── excepciones/   ReglaNegocioException, RecursoNoEncontradoException
│       ├── aplicacion/
│       │   ├── puertos/entrada/   IAutenticacion, IGestionMovimientos, IGestionCategorias,
│       │   │                      IGestionPresupuestos, IConsultaFinanciera, IEvaluadorPresupuesto
│       │   ├── puertos/salida/    IRepositorio*, ICifradorContrasena, IEmisorTokens, INotificador
│       │   ├── servicios/         Servicio* (anotados @Transactional en la configuración, no en dominio)
│       │   └── dto/               *Cmd, *DTO, FiltroMovimientos, Pagina, ResumenFinanciero
│       └── infraestructura/
│           ├── web/               *Controller, ManejadorErrores (@RestControllerAdvice), mapeadores
│           ├── seguridad/         FiltroJwt, ConfiguracionSeguridad, EmisorTokensJwt, CifradorBCrypt
│           ├── persistencia/      *EntidadJpa, *RepositorioJpa (Spring Data), Repositorio*Adaptador
│           ├── notificacion/      NotificadorSmtp (con reintentos y @Async)
│           └── configuracion/     Beans: cablea servicios con sus puertos
│   └── src/main/resources/db/migration/   V1__esquema.sql, V2__categorias_predeterminadas.sql
├── frontend/                                  (finanzas-web)
│   └── src/
│       ├── features/   auth/, movimientos/, categorias/, presupuestos/, resumen/
│       ├── api/        cliente HTTP generado desde OpenAPI + interceptor de tokens
│       ├── components/ UI compartida
│       └── app/        rutas, estado global, guardas de sesión
├── deploy/        docker-compose.yml, nginx/finanzas.conf, scripts de respaldo
└── docs/          (esta documentación)
```

**Reglas verificables** (pruebas ArchUnit en `backend/src/test`):
1. `dominio..` no depende de `aplicacion..`, `infraestructura..`, `org.springframework..`, `jakarta.persistence..`.
2. `aplicacion..` no depende de `infraestructura..`.
3. Las clases de `infraestructura.web` solo invocan interfaces de `aplicacion.puertos.entrada`.
4. Sin ciclos entre paquetes de primer nivel bajo `aplicacion.servicios`.

## 4.3 Componentes (`DEV-02`)

Los nueve componentes de `FUN-02` se reutilizan aquí (mismo elemento del modelo); solo se añade
*Seguridad*, que provee `ICifradorContrasena` e `IEmisorTokens` (en `FUN-02` esas interfaces quedan como
requeridas sin proveedor visible).

| Componente | Estereotipo | Paquete | Provee | Requiere |
|---|---|---|---|---|
| Cliente Web | `«subsystem»` | `frontend` | — | `API REST` |
| API REST | `«component»` | `infraestructura.web` + `seguridad` (filtro) | `API REST` (OpenAPI 3, `/api/v1`) | `IAutenticacion`, `IGestionMovimientos`, `IGestionCategorias`, `IGestionPresupuestos`, `IConsultaFinanciera` |
| Identidad | `«component»` | `aplicacion` (ServicioAutenticacion) | `IAutenticacion` | `IRepositorioUsuarios`, `ICifradorContrasena`, `IEmisorTokens`, `INotificador` |
| Movimientos | `«component»` | `aplicacion` (ServicioMovimientos) | `IGestionMovimientos` | `IRepositorioMovimientos`, `IRepositorioCategorias`, `IEvaluadorPresupuesto` |
| Categorías | `«component»` | `aplicacion` (ServicioCategorias) | `IGestionCategorias` | `IRepositorioCategorias`, `IRepositorioMovimientos` |
| Presupuestos | `«component»` | `aplicacion` (ServicioPresupuestos) | `IGestionPresupuestos`, `IEvaluadorPresupuesto` | `IRepositorioPresupuestos`, `IRepositorioMovimientos`, `INotificador` |
| Consultas | `«component»` | `aplicacion` (ServicioConsultaFinanciera) | `IConsultaFinanciera` | `IRepositorioMovimientos`, `IRepositorioPresupuestos` |
| Persistencia | `«component»` | `infraestructura.persistencia` | `IRepositorioUsuarios`, `IRepositorioMovimientos`, `IRepositorioCategorias`, `IRepositorioPresupuestos` | JDBC (externo: PostgreSQL) |
| Seguridad | `«component»` | `infraestructura.seguridad` | `ICifradorContrasena`, `IEmisorTokens` | — |
| Notificaciones | `«component»` | `infraestructura.notificacion` | `INotificador` | SMTP (externo) |

El **dominio** no se dibuja como componente (no provee interfaces): es una biblioteca de clases usada
por los componentes de aplicación; en `DEV-02` se muestra como *Package* `dominio` con `«use»` desde
ellos.

Agrupa los componentes en un componente contenedor `finanzas-api` `«subsystem»` (todo excepto *Cliente
Web*) para que el diagrama coincida con el artefacto `finanzas-api.jar` de Despliegue.

### Tácticas de calidad en la implementación
| RNF | Táctica | Dónde |
|---|---|---|
| RNF-01 | BCrypt (12), JWT RS256 15 min, refresh rotativo en cookie `HttpOnly; Secure; SameSite=Strict`, CORS restringido al dominio. | `seguridad` |
| RNF-02 | `idUsuario` se toma **solo** del token, nunca del cuerpo/URL; repositorios filtran siempre por `idUsuario`. | `web`, `persistencia` |
| RNF-03 | `NUMERIC(14,2)` + `Dinero`; validación Bean Validation en DTO y reglas en dominio. | `dominio`, `dto` |
| RNF-04 | Índices `(id_usuario, fecha DESC)` y `(id_usuario, id_categoria, fecha)`; agregaciones SQL (`SUM … GROUP BY`); paginación. | `persistencia`, `V1__esquema.sql` |
| RNF-06 | Pruebas ArchUnit; cobertura ≥ 80 % en `dominio` y `aplicacion`. | `src/test` |
| Errores | `ManejadorErrores` → RFC 9457 *Problem Details*: 400 validación, 401, 403, 404, 409 duplicado, 422 regla de negocio. | `web` |

### Esquema relacional (resumen de `V1__esquema.sql`)
| Tabla | Columnas clave | Restricciones |
|---|---|---|
| `usuario` | `id UUID PK`, `correo VARCHAR(254) UNIQUE`, `hash_contrasena`, `estado`, `intentos_fallidos`, `bloqueado_hasta`, `fecha_registro` | |
| `categoria` | `id UUID PK`, `id_propietario UUID NULL FK→usuario ON DELETE CASCADE`, `nombre`, `tipo` | `UNIQUE (id_propietario, tipo, nombre)` |
| `movimiento` | `id UUID PK`, `id_usuario FK ON DELETE CASCADE`, `id_categoria FK`, `tipo`, `monto NUMERIC(14,2) CHECK (monto > 0)`, `moneda CHAR(3)`, `fecha`, `descripcion`, `metodo_pago NULL`, `fuente NULL` | Herencia *single table* con discriminador `tipo`; `CHECK` que `metodo_pago` solo con `GASTO` |
| `presupuesto` | `id UUID PK`, `id_usuario FK ON DELETE CASCADE`, `id_categoria FK`, `periodo DATE` (primer día del mes), `limite NUMERIC(14,2)`, `notificado` | `UNIQUE (id_usuario, id_categoria, periodo)` |
| `refresh_token` | `hash PK`, `id_usuario FK`, `expira`, `revocado` | |
| `token_recuperacion` | `hash PK`, `id_usuario FK`, `expira`, `usado` | |

## 4.4 Cómo modelarlo en Visual Paradigm

**DEV-01 Paquetes**
1. `4 Desarrollo` › *New Diagram › Package Diagram*.
2. Paquetes anidados: `infraestructura` (con `web`, `seguridad`, `persistencia`, `notificacion`,
   `configuracion`), `aplicacion` (con `puertos.entrada`, `puertos.salida`, `servicios`, `dto`), `dominio`.
3. *Dependency* con estereotipo `«use»` (estándar): `infraestructura → aplicacion`, `aplicacion → dominio`,
   `infraestructura.persistencia → dominio`. Y *Realization* (o `«use»` + nota) `infraestructura.persistencia ⇢ aplicacion.puertos.salida` para mostrar que la implementa.
4. Para enlazar con el modelo lógico: arrastra las clases existentes de `3 Lógica` a estos paquetes
   (*Model Explorer* › mover) — no las dupliques.
5. Añade una *Note* con las 4 reglas ArchUnit.

**DEV-02 Componentes**
1. *New Diagram › Component Diagram*.
2. Arrastra desde el *Model Explorer* los componentes creados en `FUN-02` (Identidad, Movimientos, …).
   Crea solo los nuevos: *Seguridad* y el contenedor `finanzas-api`
   (*Component* grande con estereotipo `«subsystem»`; arrastra los demás dentro).
3. Interfaces: arrastra las **Interface** de `LOG-02` (ya existen) — así las operaciones quedan
   enlazadas. Provista = *Realization* componente → interfaz, mostrada como *ball*; requerida = *Usage*
   componente → interfaz (*socket*). Para las interfaces externas (API REST, JDBC, SMTP) usa un *Port*
   en el borde de `finanzas-api` con la interfaz conectada al puerto (*delegation connector*).
4. Responsabilidad: escríbela en *Open Specification › Documentation* de cada componente, **no** como
   atributos ni notas duplicadas (error D2).
5. Realización por clases: clic derecho en el componente › *Open Specification › Realized Classifiers*
   (o *Realization* hacia la clase) → p. ej. *Movimientos* realiza `ServicioMovimientos`. Esto alimenta
   la matriz de trazabilidad.
6. Verifica que **no exista** un componente "Base de Datos" (error D1).
