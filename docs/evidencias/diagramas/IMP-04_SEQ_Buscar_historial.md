# IMP-04 — Secuencia: buscar en el historial (HU-16 y HU-17)

**Fuente:** las clases y métodos del código:
- `HistorialPage.tsx` (300 ms de espera antes de buscar)
- `ConsultaController.historial`
- `ServicioConsultaFinanciera.consultarHistorial`
- `RepositorioMovimientosAdaptador.buscar` y `coincideTexto`

**Renderizado:** verificado. La imagen está en [img/IMP-04_SEQ_Buscar_historial.svg](img/IMP-04_SEQ_Buscar_historial.svg).

**Pruebas que la cubren:** `ApiIntegracionTest` (5 casos de texto), Postman "Buscar en el historial por texto" y Playwright "HU-17 búsqueda".

```mermaid
sequenceDiagram
  autonumber
  actor U as Usuario
  participant H as HistorialPage
  participant C as ConsultaController
  participant S as ServicioConsultaFinanciera
  participant R as RepositorioMovimientosAdaptador
  participant M as MongoDB

  U->>H: escribe "café"
  Note over H: espera 300 ms sin teclear<br/>y vuelve a la página 1
  H->>C: GET /api/v1/movimientos?desde&hasta&texto=café&pagina=0
  C->>S: consultarHistorial(idUsuario, FiltroMovimientos)
  S->>S: filtro.validar()<br/>rango ≤ 24 meses, texto ≤ 60
  alt filtro inválido
    S-->>C: ReglaNegocioException
    C-->>H: 400 ProblemDetail (codigo)
  else filtro válido
    S->>R: buscar(idUsuario, filtro, página, tamaño)
    R->>M: find categorias {nombre ~ /café/i} (propias y predeterminadas)
    M-->>R: ids de categorías que coinciden
    R->>M: count + find movimientos {idUsuario, fecha en rango,<br/>$or: descripcion ~, fuente ~, idCategoria ∈ ids}
    Note over R,M: el texto va con Pattern.quote:<br/>literal, nunca como expresión regular
    M-->>R: documentos ordenados por fecha ↓
    R-->>S: Pagina<Movimiento>
    S-->>C: Pagina<MovimientoDTO>
    C-->>H: 200 {elementos, totalElementos}
    H-->>U: tabla filtrada o "Ningún movimiento coincide"
  end
```
