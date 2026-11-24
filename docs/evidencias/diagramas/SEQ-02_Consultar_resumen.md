# SEQ-02 Consultar resumen financiero (CU-08, incluye CU-11)

Fuente: CU-08 (`docs/02-funcional/VISTA_FUNCIONAL.md:87-92,188-192`), RF-19, RF-23/24, RN-11, RN-13. Alineado el 22-11-2026 con el código:

- `ResumenPage.tsx` y `ResumenApi.ts`;
- `ConsultaController.resumen`;
- `ServicioConsultaFinanciera.obtenerResumen` y `FiltroMovimientos.validar`.

Sintaxis validada con `mermaid.parse` ([log](../pruebas/mermaid-arquitectura.log)).

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuario
    participant P as ResumenPage
    participant A as ResumenApi / ClienteHttp
    participant J as FiltroJwt
    participant C as ConsultaController
    participant S as ServicioConsultaFinanciera
    participant RM as IRepositorioMovimientos
    participant RP as IRepositorioPresupuestos

    U->>P: abre Resumen y elige el periodo
    P->>A: obtener(desde, hasta)
    A->>J: GET /api/v1/resumen?desde&hasta + Bearer
    J->>J: validarAcceso(token) da idUsuario
    J->>C: resumen(idUsuario, desde, hasta)
    C->>C: sin fechas, usa el mes actual
    C->>S: obtenerResumen(idUsuario, desde, hasta)
    S->>S: FiltroMovimientos.validar() RN-13
    alt falta una fecha, desde > hasta o rango > 24 meses
        S-->>C: ReglaNegocioException RANGO_INVALIDO
        C-->>A: 400 ProblemDetail (ManejadorErrores)
        A-->>P: ErrorApi
        P-->>U: muestra el error del filtro
    else rango valido
        S->>RM: sumarPorCategoria(idUsuario, desde, hasta)
        Note right of RM: agregacion $group en MongoDB
        RM-->>S: List TotalPorCategoria
        S->>S: ingresos, gastos, balance y porcentajes por categoria
        S->>RP: buscarPorPeriodo(idUsuario, YearMonth de hasta)
        RP-->>S: List Presupuesto
        loop cada presupuesto
            S->>RM: sumarGastos(idUsuario, idCategoria, periodo)
            RM-->>S: consumo, estado OK / ALERTA / EXCEDIDO (RN-11)
        end
        S->>RM: sumarPorCategoria(periodo anterior de igual duracion)
        RM-->>S: totales previos, variacion porcentual
        S->>RM: sumarPorDia(idUsuario, desde, hasta)
        RM-->>S: List TotalDiario, tendencia por semana o mes
        S-->>C: ResumenFinanciero
        C-->>A: 200 OK + JSON
        A-->>P: ResumenFinanciero
        P-->>U: tarjetas, grafico por categoria y presupuestos
    end
```

## Correcciones (22-11-2026, contra el código)

- **Excepción y código HTTP:** `RangoInvalidoException` no existe. El código lanza `ReglaNegocioException("RANGO_INVALIDO")` desde `FiltroMovimientos.validar()`, y `ManejadorErrores` lo asigna a **400**, no a 422.
- **Participante del frontend:** `ClienteWeb` no existe. Los participantes reales son `ResumenPage` y `ResumenApi.obtener`, que pasan por `ClienteHttp`. El `idUsuario` lo obtiene `FiltroJwt`.
- **Fechas por defecto:** si no llegan fechas, el controlador usa el mes actual.
- **Lo que el resumen calcula de verdad,** y que faltaba en el diagrama:
  - el consumo de cada presupuesto (`sumarGastos`);
  - la variación frente al periodo anterior (segundo `sumarPorCategoria`);
  - la tendencia (`sumarPorDia`).
- **Periodo de los presupuestos:** se toma de `hasta` (`YearMonth.from(hasta)`), no de `desde`.
