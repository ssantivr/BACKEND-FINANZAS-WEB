# Finanzas Personales — Documentación de Arquitectura (v2.0)

Rediseño completo del proyecto `Finanzas_Personales.vpp` (v1.0). Esta carpeta reemplaza el modelo
anterior: corrige los errores detectados en la auditoría y alinea las cinco vistas entre sí.

## Estructura de la carpeta

```
Finanzas_Personales/
├── modelo/
│   └── Finanzas_Personales_v2.vpp        ← UN solo proyecto VP con un Model por vista
├── docs/                                 ← esta carpeta
│   ├── README.md                         ← índice, convenciones y glosario
│   ├── 00-auditoria/AUDITORIA.md         ← errores de v1.0 y cómo se corrigieron
│   ├── 01-contexto/VISTA_CONTEXTO.md
│   ├── 02-funcional/VISTA_FUNCIONAL.md   ← casos de uso, especificaciones, actividad, secuencia
│   ├── 03-logica/VISTA_LOGICA.md         ← diagrama de clases completo
│   ├── 04-desarrollo/VISTA_DESARROLLO.md ← capas, paquetes y componentes
│   ├── 05-despliegue/VISTA_DESPLIEGUE.md
│   ├── 06-trazabilidad/TRAZABILIDAD.md   ← matriz CU ↔ componente ↔ clase ↔ artefacto ↔ nodo
│   ├── 08-requisitos/
│   │   ├── HISTORIAS_DE_USUARIO.md       ← 19 historias en 5 épicas, con criterios de aceptación
│   │   └── REQUERIMIENTOS_FUNCIONALES.md ← RF-01..RF-24, reglas de negocio y trazabilidad
│   └── 07-decisiones/                    ← ADR (Architecture Decision Records)
│       ├── ADR-001-monolito-modular.md
│       ├── ADR-002-arquitectura-hexagonal.md
│       └── ADR-003-postgresql.md
└── exportes/                             ← PNG/SVG/PDF exportados desde VP (no se editan a mano)
    ├── 01-contexto.svg  02-casos-uso.svg  02-act-registrar-movimiento.svg  02-seq-registrar-movimiento.svg
    ├── 03-clases-dominio.svg  03-clases-aplicacion.svg  04-componentes.svg  04-paquetes.svg
    └── 05-despliegue.svg
```

Regla: **los diagramas se editan solo en el `.vpp`**; `exportes/` se regenera con
*Project › Export › Diagrams as Image* antes de cada entrega.

## Contenido del proyecto `modelo/Finanzas_Personales_v2.vpp`

Generado con las herramientas de importación XML de Visual Paradigm 18.1 (`modelo/gen_full.py` +
`modelo/vpgen.py` regeneran el XML). Incluye 10 diagramas:

| Model | Diagramas en el `.vpp` |
|---|---|
| 1 Contexto | CTX-01 Contexto del sistema |
| 2 Funcional | FUN-01 Casos de uso · FUN-02 Componentes funcionales · ACT-01 Registrar movimiento |
| 3 Lógica | LOG-01 Dominio · LOG-02 Aplicación y puertos |
| 4 Desarrollo | DEV-01 Paquetes y capas · DEV-02 Componentes (las clases viven en el paquete `co.finanzas`) |
| 5 Despliegue | DEP-01 Producción · DEP-03 Manifestaciones |

**Pendiente de dibujar a mano en VP (el importador no los genera de forma fiable):**
- `SEQ-01..03`: el importador no ubica bien los mensajes de secuencia. Están especificados paso a paso en `02-funcional` §2.6.
- Particiones (swimlanes) de `ACT-01`: los nodos ya están en 4 columnas con su título; basta con *Swimlane › Vertical Swimlane* (4 carriles) y arrastrar cada columna a su carril.
- `ACT-02`, `ACT-03` y `DEP-02` (opcionales).

Las imágenes de `exportes/` salieron de la herramienta de línea de comandos, que corre en modo de evaluación y por eso llevan la marca de agua "evaluation copy". Si las exportas desde tu VP, saldrán sin ella.

## Organización del proyecto en Visual Paradigm

En el *Model Explorer* crea un `Model` raíz por vista, con el mismo número que el documento:

| Model VP | Diagramas que contiene |
|---|---|
| `1 Contexto` | `CTX-01 Contexto del sistema` |
| `2 Funcional` | `FUN-01 Casos de uso`, `FUN-02 Componentes funcionales`, `ACT-01..03`, `SEQ-01..03` |
| `3 Lógica` | `LOG-01 Dominio`, `LOG-02 Aplicación y puertos` |
| `4 Desarrollo` | `DEV-01 Paquetes/capas`, `DEV-02 Componentes` |
| `5 Despliegue` | `DEP-01 Producción`, `DEP-02 Desarrollo local` (opcional) |

**Regla de oro de consistencia (corrige el error más grave de v1.0):** cada elemento existe **una sola
vez** en el modelo. Para mostrarlo en otro diagrama, arrástralo desde el *Model Explorer* (crea una
*vista* del mismo elemento) en lugar de dibujar uno nuevo con el mismo nombre. En v1.0 había, por
ejemplo, dos componentes distintos llamados *Gestión de Compras* y un artefacto homónimo sin relación
entre ellos.

## Convenciones

| Aspecto | Convención |
|---|---|
| Idioma | Español sin tildes en identificadores de código (`Categoria`, `ServicioMovimientos`); con tildes en textos de diagramas. |
| Clases | `PascalCase`, sustantivo singular (`Movimiento`, no `Compras`). |
| Interfaces | Prefijo `I` + sustantivo/rol (`IGestionMovimientos`, `IRepositorioCategorias`). Se modelan con el elemento **Interface** de VP, nunca como `Class` con estereotipo escrito a mano. |
| Operaciones | `camelCase`, verbo en infinitivo (`registrarGasto`). |
| Casos de uso | `CU-nn` + verbo + objeto (`CU-03 Registrar movimiento`). |
| Visibilidad | `+` público, `-` privado, `#` protegido, `~` paquete. Atributos de entidades siempre `-`. |
| Estereotipos | Solo estándar UML o los definidos en la tabla de la Vista Lógica (`«entity»`, `«value object»`, `«service»`, `«repository»`, `«enumeration»`). |
| Dinero | Nunca `double`/`float`: `BigDecimal` encapsulado en el value object `Dinero`. |
| Identificadores | `UUID` (no `int` autoincremental expuesto en la API). |

## Glosario

| Término | Definición |
|---|---|
| Movimiento | Registro de dinero que entra (Ingreso) o sale (Gasto) de las finanzas del usuario. Generaliza la "Compra" de v1.0. |
| Categoría | Clasificación de un movimiento. Puede ser **predeterminada** (del sistema, compartida) o **personalizada** (de un usuario). |
| Presupuesto | Límite de gasto mensual para una categoría. |
| Resumen financiero | Vista calculada (no persistida) de totales, balance y gasto por categoría en un período. |
| Historial | Resultado de consultar movimientos con filtros y paginación. **No es una entidad** (en v1.0 sí lo era, error corregido). |
