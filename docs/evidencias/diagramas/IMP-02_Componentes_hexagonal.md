# IMP-02 — Componentes del backend (arquitectura hexagonal implementada)

**Fuente:** el código en `backend/src/main/java/co/finanzas/`, con los nombres reales de paquetes, puertos y clases.

**Renderizado:** verificado. La imagen está en [img/IMP-02_Componentes_hexagonal.svg](img/IMP-02_Componentes_hexagonal.svg).

**Regla de dependencias:** apuntan siempre hacia el dominio. Las comprueba en cada `mvn test` la prueba de ArchUnit `ArquitecturaTest` (4 reglas).

```mermaid
flowchart LR
  subgraph cliente["Frontend React (frontend/src)"]
    UI["Páginas: Resumen, Historial,<br/>Presupuestos, Categorías,<br/>Reportes, Cuenta"]
    HTTP["api/ClienteHttp<br/>token en memoria + cookie"]
    UI --> HTTP
  end

  subgraph infra["infraestructura"]
    subgraph web["web"]
      CTRL["AuthController · MovimientoController<br/>ConsultaController · CategoriaController<br/>PresupuestoController · CuentaController"]
      ERR["ManejadorErrores<br/>ProblemDetail + codigo"]
    end
    subgraph seg["seguridad"]
      JWT["FiltroJwt · EmisorTokensJwt<br/>CifradorBCrypt"]
    end
    subgraph pers["persistencia"]
      ADP["Repositorio*Adaptador<br/>MongoTemplate + *Documento"]
    end
    NOT["notificacion/NotificadorSmtp"]
  end

  subgraph app["aplicacion"]
    PE["puertos/entrada<br/>IAutenticacion · IGestionMovimientos<br/>IConsultaFinanciera · IGestionPresupuestos<br/>IGestionCategorias · IGestionCuenta"]
    SRV["servicios<br/>Servicio* (casos de uso)"]
    PS["puertos/salida<br/>IRepositorio* · IEmisorTokens<br/>ICifradorContrasena · INotificador"]
    PE --> SRV --> PS
  end

  subgraph dom["dominio"]
    MOD["modelo: Usuario, Movimiento,<br/>Gasto, Ingreso, Categoria, Presupuesto"]
    VAL["valor: Dinero, Email"]
  end

  DB[("MongoDB<br/>base finanzas")]
  SMTP[["SMTP o consola (local)"]]

  HTTP -- "HTTPS /api/v1" --> CTRL
  CTRL --> PE
  JWT -.-> CTRL
  SRV --> MOD
  ADP -. implementa .-> PS
  JWT -. implementa .-> PS
  NOT -. implementa .-> PS
  ADP --> DB
  JWT --> DB
  NOT --> SMTP
```
