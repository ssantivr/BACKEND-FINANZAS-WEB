# ACT-02 Modificar o eliminar movimiento (CU-04)

Fuente: CU-04 (catálogo `docs/02-funcional/VISTA_FUNCIONAL.md:22`), RF-12 y RF-13 (`docs/08-requisitos/REQUERIMIENTOS_FUNCIONALES.md:137-152`), RF-20, RN-05, RN-11, RN-12.

Justificación de la elección: CU-04 es el comportamiento con más lógica sin diagrama (P-08) y el que destapa P-10: RF-12 exige reevaluar el presupuesto del mes anterior y del nuevo, algo que el CU-10 original (solo extiende CU-03) no cubre. Aporta además el caso de propiedad ajena (404, RN-05). `VISTA_FUNCIONAL.md` no contiene la especificación textual de CU-04: el flujo de abajo sale de RF-12/RF-13 y `docs/architecture/arquitectura.md:71`; el comportamiento de eliminar frente a presupuesto es una inferencia (**NO VERIFICADO**).

Renderizado: NO VERIFICADO (no hay mermaid-cli). Es un `flowchart` con carriles (`subgraph`); rombos = decisión, círculos = merge.

```mermaid
flowchart TB
    subgraph LU["Usuario"]
        INI(["Inicio: abre un movimiento desde Historial"])
        ACC{"Accion elegida"}
        EDIT["Editar monto, fecha, descripcion o categoria"]
        CONF["Confirmar eliminacion"]
        FIN(["Fin"])
    end
    subgraph LW["Cliente Web"]
        ENV_PUT["PUT /api/v1/movimientos/id"]
        DLG["Pedir confirmacion"]
        ENV_DEL["DELETE /api/v1/movimientos/id"]
        ERR422["Mostrar errores de validacion"]
        ERR404["Mostrar movimiento no encontrado"]
        OK["Mostrar resultado y saldo actualizado"]
    end
    subgraph LA["API / Aplicacion"]
        BUS["Buscar movimiento del usuario"]
        DP{"Es del usuario"}
        DA{"Operacion"}
        DV{"Reglas cumplidas"}
        GUARDAR["Actualizar movimiento"]
        BORRAR["Eliminar movimiento"]
        DG{"Es gasto"}
        REEV1["Reevaluar presupuesto del mes anterior"]
        REEV2["Reevaluar presupuesto del mes nuevo"]
        REEV3["Reevaluar presupuesto del mes del gasto eliminado"]
        DX{"Excedido por primera vez"}
        CLAIM["Reclamar notificado atomico"]
        NOT["Notificar excedido (asincrono)"]
        MA(("merge"))
    end
    subgraph LB["Base de datos"]
        UPD["UPDATE movimiento"]
        DEL["DELETE movimiento"]
        UPN["UPDATE presupuesto SET notificado WHERE NOT notificado"]
    end
    subgraph LC["Servicio de correo"]
        MAIL["Enviar correo de alerta"]
    end

    INI --> ACC
    ACC -- "modificar" --> EDIT
    ACC -- "eliminar" --> CONF
    EDIT --> ENV_PUT
    CONF --> DLG
    DLG --> ENV_DEL
    ENV_PUT --> BUS
    ENV_DEL --> BUS
    BUS --> DP
    DP -- "no" --> ERR404
    DP -- "si" --> DA
    DA -- "modificar" --> DV
    DA -- "eliminar" --> BORRAR
    DV -- "no (5a)" --> ERR422
    DV -- "si" --> GUARDAR
    ERR422 --> EDIT
    ERR404 --> FIN
    GUARDAR --> UPD
    UPD --> DG
    BORRAR --> DEL
    DEL --> DG
    DG -- "no" --> MA
    DG -- "si, modificar" --> REEV1
    REEV1 --> REEV2
    REEV2 --> DX
    DG -- "si, eliminar" --> REEV3
    REEV3 --> MA
    DX -- "no" --> MA
    DX -- "si" --> CLAIM
    CLAIM --> UPN
    UPN --> NOT
    NOT -.-> MAIL
    NOT --> MA
    MA --> OK
    OK --> FIN
```

## Cambios respecto al original
- P-08: aporta un diagrama de comportamiento para CU-04 (antes no existía ninguno).
- P-10: refleja que al modificar un gasto se reevalúa el presupuesto del mes anterior y del nuevo (RF-12), y que CU-10 se dispara también desde CU-04 (ver FUN-01 corregido).
- RN-05 / RNF-02: rama 404 cuando el movimiento no es del usuario.
- P-19: reclamo atómico de `notificado` antes de llamar a `INotificador` (RN-12).
- Supuesto (**NO VERIFICADO**): al eliminar un gasto se reevalúa su mes (RF-13 no lo exige); se incluye por coherencia de saldos y se puede quitar si se rechaza. La reevaluación al eliminar solo puede bajar el consumo, así que no dispara alerta: por eso `REEV3` va directo al merge.
