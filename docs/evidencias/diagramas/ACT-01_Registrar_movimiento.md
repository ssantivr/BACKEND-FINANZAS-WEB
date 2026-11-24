# ACT-01 Registrar movimiento (CU-03)

Fuente: CU-03 flujo básico y alternativos 5a, 5b, 6a, *a (`docs/02-funcional/VISTA_FUNCIONAL.md:40-70,129-158`), RN-01..RN-04, RF-09, RF-11, RF-20.

Renderizado: NO VERIFICADO (no hay mermaid-cli). Mermaid no tiene diagrama de actividad UML: es un `flowchart` con `subgraph` por carril. Convención: rombo `{ }` = nodo de decisión (guardas en las flechas), círculo pequeño `(( ))` = nodo de unión (merge), `([ ])` = inicio/fin. Mermaid ubica los carriles según el motor de layout; el orden visual de carriles no está garantizado.

```mermaid
flowchart TB
    subgraph LU["Usuario"]
        direction TB
        INI(["Inicio"])
        A1["Seleccionar tipo de movimiento"]
        A6["Completar formulario"]
        FIN(["Fin"])
    end
    subgraph LW["Cliente Web"]
        direction TB
        A3["Solicitar categorias del tipo"]
        A7["Validar formato"]
        D8{"Formato valido"}
        A9["Enviar POST /api/v1/movimientos"]
        M1(("merge"))
        A12["Mostrar errores de campos"]
        A5b["Informar error y recargar categorias"]
        A6a["Mostrar No se pudo guardar y conservar datos"]
        AEX["Redirigir a Iniciar sesion conservando borrador"]
        A17["Mostrar confirmacion y saldo actualizado"]
        M2(("merge"))
    end
    subgraph LA["API / Aplicacion"]
        direction TB
        A4["Consultar categorias disponibles"]
        A10["Validar RN-01 a RN-04"]
        D11{"Reglas cumplidas"}
        D5b{"Categoria disponible"}
        A14["Persistir movimiento"]
        D14{"Gasto con presupuesto"}
        A15["Evaluar consumo del presupuesto"]
        D16{"Excedido por primera vez"}
        A16a["Reclamar notificado=true atomico"]
        D16b{"Reclamo afecto 1 fila"}
        D6a{"Persistencia exitosa"}
        A18["Registrar error y reintentar 3 veces backoff"]
        DS{"Sesion valida"}
        A_RF["Renovar con refresh token"]
        D_RF{"Renovacion exitosa"}
        NOT["Notificar excedido (asincrono)"]
    end
    subgraph LB["Base de datos"]
        direction TB
        A5["Leer categorias predeterminadas y del usuario"]
        A13["Insertar movimiento"]
        A16c["UPDATE presupuesto SET notificado WHERE NOT notificado"]
    end
    subgraph LC["Servicio de correo"]
        direction TB
        MAIL["Enviar correo de alerta"]
    end

    INI --> A1
    A1 --> A3
    A3 --> A4
    A4 --> A5
    A5 --> M1
    M1 --> A6
    A6 --> A7
    A7 --> D8
    D8 -- "no" --> M1
    D8 -- "si" --> DS
    DS -- "no" --> A_RF
    A_RF --> D_RF
    D_RF -- "no" --> AEX
    D_RF -- "si" --> A9
    DS -- "si" --> A9
    AEX --> FIN
    A9 --> A10
    A10 --> D5b
    D5b -- "no (5b)" --> A5b
    A5b --> A4
    D5b -- "si" --> D11
    D11 -- "no (5a)" --> A12
    A12 --> M1
    D11 -- "si" --> A13
    A13 --> D6a
    D6a -- "no (6a)" --> A6a
    A6a --> M1
    D6a -- "si" --> D14
    D14 -- "no" --> M2
    D14 -- "si" --> A15
    A15 --> D16
    D16 -- "no" --> M2
    D16 -- "si" --> A16a
    A16a --> A16c
    A16c --> D16b
    D16b -- "no" --> M2
    D16b -- "si" --> NOT
    NOT -.-> MAIL
    NOT --> M2
    MAIL -.-> A18
    M2 --> A17
    A17 --> FIN
```

Notas de lectura:
- El flujo 5b (categoría no disponible) vuelve al paso 2 (`A4`: recarga de categorías). El flujo 5a vuelve al paso 3 (`A6`) vía el merge `M1`. El flujo 6a conserva los datos y vuelve a `A6` vía `M1`.
- La comprobación de sesión (*a) se modela antes del envío; en la práctica ocurre en cualquier paso con una petición HTTP (la guarda `[*a]` se simplifica a un punto).
- La flecha punteada `NOT -.-> MAIL` es asíncrona: la actividad no espera el correo y el gasto no se revierte si falla (A18 es un flujo separado sin retorno al camino principal).

## Cambios respecto al original
- P-09: carriles reales (`subgraph`) en vez de columnas de tabla; se añade el carril **Servicio de correo** y **Notificar** está en el carril API (quien lo solicita), con el envío efectivo en el carril Servicio de correo y no en Base de datos.
- P-09: nodos de decisión (rombos) separados de los de unión (círculos `merge`); el original usaba un mismo nodo para ambos y una barra sin función.
- P-09: se agregan los flujos alternativos 5b (categoría no disponible), 6a (fallo de persistencia) y *a (sesión expirada con renovación y redirección a CU-02), ausentes del original.
- P-19: el reclamo atómico de `notificado` (`UPDATE … WHERE NOT notificado`) precede a la notificación (RN-12).
- Un solo nodo final `Fin` (activity final); el original tenía flujos sin destino claro.
- Supuesto: la rama de error 5b se evalúa en la API al buscar la categoría disponible (coincide con SEQ-01 `CategoriaNoDisponibleException`).
