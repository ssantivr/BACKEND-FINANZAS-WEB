# ACT-03 Iniciar sesión con bloqueo (CU-02)

Fuente: CU-02 (`docs/02-funcional/VISTA_FUNCIONAL.md:105`), RF-03, RF-04, RN-08 (`docs/08-requisitos/REQUERIMIENTOS_FUNCIONALES.md:28,57-72`), arquitectura §4 P-18 y D-4.

Justificación: CU-02 es la segunda lógica con más reglas sin diagrama (P-08): bloqueo a 5 intentos y 15 min (RN-08), 423 con tiempo restante y mensaje genérico anti-enumeración. Complementa a SEQ-03 (mecanismo) con la vista de flujo de decisiones. Se prefiere sobre ACT de CU-09 (recuperar contraseña) porque el bloqueo es un requisito Must (M) y P-13 lo detectó como omisión.

Renderizado: NO VERIFICADO (no hay mermaid-cli). `flowchart` con carriles; rombos = decisión, círculos = merge.

```mermaid
flowchart TB
    subgraph LU["Usuario"]
        INI(["Inicio"])
        ING["Ingresar correo y clave"]
        FIN(["Fin"])
    end
    subgraph LW["Cliente Web"]
        ENV["POST /api/v1/auth/login"]
        MGEN["Mostrar credenciales invalidas (mensaje generico)"]
        MBLQ["Mostrar cuenta bloqueada y tiempo restante"]
        MOK["Guardar access token y abrir sesion"]
        M1(("merge"))
    end
    subgraph LA["API / Aplicacion"]
        BUS["Buscar usuario por correo"]
        D1{"Existe y no esta ELIMINADA"}
        D2{"bloqueado_hasta mayor que ahora"}
        VER["Verificar hash de la clave"]
        D3{"Clave coincide"}
        INC["Incrementar intentos_fallidos"]
        D4{"intentos_fallidos = 5"}
        BLQ["Fijar bloqueado_hasta = ahora + 15 min"]
        RST["Reiniciar intentos_fallidos y bloqueado_hasta"]
        TOK["Emitir access token 15 min y refresh token 7 dias"]
        M2(("merge"))
    end
    subgraph LB["Base de datos"]
        RD["Leer usuario"]
        WR1["UPDATE usuario SET intentos_fallidos"]
        WR2["UPDATE usuario SET bloqueado_hasta"]
        WR3["UPDATE usuario reinicia contadores"]
        WS["INSERT sesion_refresco (hash)"]
    end

    INI --> ING
    ING --> ENV
    ENV --> BUS
    BUS --> RD
    RD --> D1
    D1 -- "no" --> MGEN
    D1 -- "si" --> D2
    D2 -- "si: 423" --> MBLQ
    D2 -- "no" --> VER
    VER --> D3
    D3 -- "no" --> INC
    INC --> WR1
    WR1 --> D4
    D4 -- "si" --> BLQ
    BLQ --> WR2
    WR2 --> M2
    D4 -- "no" --> M2
    M2 --> MGEN
    D3 -- "si" --> RST
    RST --> WR3
    WR3 --> TOK
    TOK --> WS
    WS --> MOK
    MGEN --> M1
    MBLQ --> M1
    MOK --> M1
    M1 --> FIN
```

Nota: tras `MGEN` el usuario puede reintentar (volver a `ING`); se omite el ciclo para mantener un solo nodo final. Con cuenta bloqueada, un intento adicional no incrementa el contador (se rechaza antes de verificar la clave).

## Cambios respecto al original
- P-08: aporta el diagrama de comportamiento de CU-02 (el original solo tenía ACT-01, y ACT-02 "Iniciar sesión" estaba solo mencionado, `VISTA_FUNCIONAL.md:158`).
- P-13: incluye comprobación de bloqueo (423) y conteo de intentos (RN-08).
- D-4 / P-18: el bloqueo se deriva de `bloqueado_hasta`, no de un estado `BLOQUEADA` almacenado.
- Mensaje genérico para usuario inexistente, eliminado o clave errónea (CU-02 alternativo).
- Supuesto (**NO VERIFICADO**): el bloqueo se decide al 5.º fallo; RN-08 dice "tras 5 intentos fallidos seguidos" y el CHECK `intentos_fallidos BETWEEN 0 AND 5` lo respalda. Renovar/cerrar sesión (RF-05/06, P-27) quedan fuera de este diagrama.
