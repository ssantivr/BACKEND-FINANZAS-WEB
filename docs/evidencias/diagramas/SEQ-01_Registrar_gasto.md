# SEQ-01 Registrar gasto (CU-03 + CU-10)

Fuente: CU-03 y CU-10 (`docs/02-funcional/VISTA_FUNCIONAL.md:160-186`), RF-09, RF-11, RF-20, RN-01..RN-04, RN-11, RN-12. Alineado el 22-11-2026 con el código:

- `FormularioMovimiento.tsx` y `MovimientosApi.ts`;
- `FiltroJwt`, `MovimientoController`, `ServicioMovimientos.registrarGasto` y `Movimiento.validar`;
- `ServicioPresupuestos.evaluar`, `NotificadorSmtp` (`@Async`) y `ManejadorErrores`.

Sintaxis validada con `mermaid.parse` ([log](../pruebas/mermaid-arquitectura.log)).

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuario
    participant F as FormularioMovimiento
    participant A as MovimientosApi / ClienteHttp
    participant J as FiltroJwt
    participant C as MovimientoController
    participant S as ServicioMovimientos
    participant RC as IRepositorioCategorias
    participant G as Gasto
    participant RM as IRepositorioMovimientos
    participant E as ServicioPresupuestos (IEvaluadorPresupuesto)
    participant RP as IRepositorioPresupuestos
    participant N as INotificador (NotificadorSmtp)

    U->>F: llena el formulario y pulsa Guardar
    F->>A: registrar(cmd)
    A->>J: POST /api/v1/movimientos (tipo GASTO) + Bearer
    J->>J: validarAcceso(token) da idUsuario
    J->>C: registrar(req)
    C->>S: registrarGasto(RegistrarGastoCmd)
    S->>RC: buscarDisponible(idCategoria, idUsuario)
    RC-->>S: Optional Categoria
    alt categoria vacia o de otro usuario
        S-->>C: ReglaNegocioException CATEGORIA_INVALIDA
        C-->>A: 422 ProblemDetail (ManejadorErrores)
        A-->>F: ErrorApi
        F-->>U: muestra "La categoria no existe"
    else categoria disponible
        S->>G: new Gasto(id, idUsuario, Dinero, fecha, descripcion, categoria, metodoPago)
        G->>G: validar(hoy) RN-01..RN-04
        alt monto, fecha, descripcion o tipo de categoria invalidos
            G-->>S: ReglaNegocioException MONTO_INVALIDO, FECHA_FUTURA, DESCRIPCION_LARGA o CATEGORIA_TIPO
            S-->>C: excepcion
            C-->>A: 422 ProblemDetail con el codigo
            A-->>F: ErrorApi
            F-->>U: marca el campo incorrecto
        else datos validos
            S->>RM: guardar(gasto)
            RM-->>S: ok
            S->>E: evaluar(gasto)
            E->>RP: buscar(idUsuario, idCategoria, periodo)
            RP-->>E: Optional Presupuesto
            opt existe presupuesto del mes
                E->>RM: sumarGastos(idUsuario, idCategoria, periodo)
                RM-->>E: consumo
                alt debeNotificar(consumo) y puedeEnviar() RN-11, RN-12
                    E->>E: presupuesto.marcarNotificado()
                    E->>RP: guardar(presupuesto)
                    E-)N: enviarAlertaPresupuesto(correo, PresupuestoDTO)
                    Note over N: @Async: se envia en otro hilo, si falla no revierte el gasto
                else reiniciarAvisoSiVolvioAlLimite(consumo)
                    E->>RP: guardar(presupuesto)
                end
            end
            E-->>S: fin
            S-->>C: MovimientoDTO
            C-->>A: 201 Created + JSON
            A-->>F: MovimientoDTO
            F-->>U: confirma y actualiza el historial
        end
    end
```

## Correcciones (22-11-2026, contra el código)

- **Excepciones:** `CategoriaNoDisponibleException` y `ValidacionException` no existen. El código lanza `ReglaNegocioException` con un código (`CATEGORIA_INVALIDA`, `MONTO_INVALIDO`, `FECHA_FUTURA`, `DESCRIPCION_LARGA`, `CATEGORIA_TIPO`), y `ManejadorErrores` lo convierte en `422` con `ProblemDetail`.
- **Participante del frontend:** `ClienteWeb` no existe. El participante real es `FormularioMovimiento`, que llama a `MovimientosApi.registrar`, que a su vez pasa por `ClienteHttp`. El mensaje `confirmarFormulario` tampoco existe.
- **Autenticación:** el `idUsuario` lo obtiene `FiltroJwt.validarAcceso(token)` antes de llegar al controlador.
- **Respuesta:** es `201 Created` con el JSON del movimiento. El código no envía cabecera `Location`.
- **Aviso de presupuesto:**
  - no hay `UPDATE … WHERE NOT notificado` ni transacción explícita (no hay `@Transactional`);
  - el servicio llama a `marcarNotificado()` y a `guardar(presupuesto)`;
  - la regla de "solo la primera vez" está en `debeNotificar(consumo)`, que mira el campo `notificado`;
  - el aviso se reinicia con `reiniciarAvisoSiVolvioAlLimite`.
- **Envío del correo:** es asíncrono porque `NotificadorSmtp` está anotado con `@Async`; no se debe a que vaya "después del commit".
- **Rama quitada:** la de "fallo de persistencia → 500" ya no está como flujo propio. Un error inesperado lo maneja `ManejadorErrores.inesperado`, que responde 500 de forma genérica.
