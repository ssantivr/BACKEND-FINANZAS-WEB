# 3. Vista Lógica

Modelo orientado a objetos en dos diagramas:
- `LOG-01 Dominio` — entidades, value objects, enumeraciones y sus relaciones.
- `LOG-02 Aplicación y puertos` — interfaces de entrada (casos de uso), servicios que las realizan y
  puertos de salida (repositorios, notificación, seguridad).

Paquetes lógicos: `dominio`, `aplicacion` (`aplicacion.puertos.entrada`, `aplicacion.puertos.salida`,
`aplicacion.servicios`, `aplicacion.dto`). Las clases de infraestructura (controladores, adaptadores JPA)
pertenecen a la Vista de Desarrollo.

## 3.1 Decisiones de diseño que corrigen v1.0

1. `Compra` se generaliza en **`Movimiento` abstracto** con subclases `Gasto` e `Ingreso` → el balance
   es calculable (L8).
2. **`Historial` deja de ser entidad**; es una consulta que devuelve `Pagina<MovimientoDTO>` (L1).
3. Un único camino de propiedad: **`Usuario 1 —— 0..* Movimiento`** (asociación; borrado lógico D-1) (L1).
4. Entidades con invariantes y sin lógica de casos de uso (L4); casos de uso en servicios con interfaz (L9).
5. Todas las operaciones de consulta exigen `idUsuario` y los ids son `UUID` (L7).
6. Dinero como value object `Dinero` (BigDecimal + moneda), nunca `double` (RNF-03).

## 3.2 Estereotipos usados

| Estereotipo | Aplica a | Significado |
|---|---|---|
| `«entity»` | Class | Objeto con identidad y ciclo de vida persistente. |
| `«value object»` | Class | Inmutable, igualdad por valor, sin identidad. |
| `«enumeration»` | Enumeration (elemento nativo VP) | Conjunto cerrado de valores. |
| `«service»` | Class | Implementa un caso de uso; sin estado. |
| `«dto»` | Class | Dato de entrada/salida de la capa de aplicación. |
| *(ninguno)* | Interface (elemento nativo VP) | Interfaces: no requieren estereotipo. |

## 3.3 `LOG-01 Dominio` — clases

> Notación: `-` privado, `+` público, `#` protegido; *cursiva* = abstracto; `{readOnly}` = no modificable
> tras la construcción; subrayado = estático.

### `«entity» Usuario`
| Miembro | Detalle |
|---|---|
| `- id : UUID {readOnly}` | |
| `- nombre : String` | 2..80 caracteres |
| `- correo : Email` | único |
| `- hashContrasena : String` | BCrypt; nunca la contraseña en claro |
| `- estado : EstadoCuenta` | |
| `- intentosFallidos : int` | |
| `- bloqueadoHasta : LocalDateTime [0..1]` | |
| `- fechaRegistro : LocalDateTime {readOnly}` | |
| `- eliminadaEn : LocalDateTime [0..1]` | borrado lógico (D-1) |
| `- /bloqueada : boolean` | derivado de `bloqueadoHasta` (D-4) |
| `+ Usuario(nombre : String, correo : Email, hashContrasena : String)` | constructor |
| `+ cambiarContrasena(nuevoHash : String) : void` | |
| `+ registrarIntentoFallido(ahora : LocalDateTime) : void` | bloquea 15 min al 5.º intento |
| `+ reiniciarIntentos() : void` | |
| `+ estaBloqueado(ahora : LocalDateTime) : boolean` | |
| `+ getId() : UUID` · `+ getNombre() : String` · `+ getCorreo() : Email` | |

### *`«entity» Movimiento`* {abstract}
| Miembro | Detalle |
|---|---|
| `# id : UUID {readOnly}` | |
| `# idUsuario : UUID {readOnly}` | propietario |
| `# monto : Dinero` | |
| `# fecha : LocalDate` | |
| `# descripcion : String [0..1]` | ≤ 140 caracteres |
| `# categoria : Categoria` | extremo navegable de la asociación |
| `# fechaRegistro : LocalDateTime {readOnly}` | |
| `# Movimiento(idUsuario : UUID, monto : Dinero, fecha : LocalDate, descripcion : String, categoria : Categoria)` | valida invariantes |
| `+ `*`getTipo() : TipoMovimiento`* | **abstracta** |
| `+ getSigno() : int` | `+1` ingreso, `-1` gasto (usa `getTipo()`) |
| `+ modificar(monto : Dinero, fecha : LocalDate, descripcion : String, categoria : Categoria) : void` | revalida |
| `+ perteneceA(idUsuario : UUID) : boolean` | |
| `# validar() : void` | RN-01, RN-02, RN-03 |
| getters públicos de todos los atributos | |

### `«entity» Gasto` (hereda de Movimiento)
| Miembro | Detalle |
|---|---|
| `- metodoPago : MetodoPago` | |
| `+ Gasto(idUsuario, monto, fecha, descripcion, categoria, metodoPago : MetodoPago)` | |
| `+ getTipo() : TipoMovimiento` | devuelve `GASTO` |
| `+ getMetodoPago() : MetodoPago` | |

### `«entity» Ingreso` (hereda de Movimiento)
| Miembro | Detalle |
|---|---|
| `- fuente : String [0..1]` | p. ej. "Salario" |
| `+ Ingreso(idUsuario, monto, fecha, descripcion, categoria, fuente : String)` | |
| `+ getTipo() : TipoMovimiento` | devuelve `INGRESO` |
| `+ getFuente() : String` | |

### `«entity» Categoria`
| Miembro | Detalle |
|---|---|
| `- id : UUID {readOnly}` | |
| `- nombre : String` | 1..40; único por usuario y tipo |
| `- tipo : TipoMovimiento {readOnly}` | |
| `- icono : String [0..1]` | |
| `- idPropietario : UUID [0..1]` | vacío ⇒ predeterminada |
| `+ esPredeterminada() : boolean` | |
| `+ estaDisponiblePara(idUsuario : UUID) : boolean` | predeterminada o propia (RN-04) |
| `+ renombrar(nombre : String) : void` | lanza excepción si es predeterminada |
| getters | |

### `«entity» Presupuesto`
| Miembro | Detalle |
|---|---|
| `- id : UUID {readOnly}` | |
| `- idUsuario : UUID {readOnly}` | |
| `- categoria : Categoria` | solo de tipo GASTO |
| `- periodo : YearMonth` | único por (usuario, categoría, periodo) |
| `- limite : Dinero` | > 0 |
| `- notificado : boolean` | |
| `+ calcularEstado(consumo : Dinero) : EstadoPresupuesto` | OK < 80 % ≤ ALERTA < 100 % ≤ EXCEDIDO |
| `+ debeNotificar(consumo : Dinero) : boolean` | excedido y `!notificado` |
| `+ marcarNotificado() : void` | |
| `+ cambiarLimite(limite : Dinero) : void` | reinicia `notificado` |

### `«value object» Dinero`
| Miembro | Detalle |
|---|---|
| `- cantidad : BigDecimal {readOnly}` | escala 2, `RoundingMode.HALF_EVEN` |
| `- moneda : Currency {readOnly}` | por defecto `COP` |
| `+ Dinero(cantidad : BigDecimal, moneda : Currency)` | |
| `+ sumar(otro : Dinero) : Dinero` | exige misma moneda |
| `+ restar(otro : Dinero) : Dinero` | |
| `+ porcentajeDe(total : Dinero) : BigDecimal` | |
| `+ esPositivo() : boolean` | |
| `+ `<u>`cero(moneda : Currency) : Dinero`</u> | estático |

### `«value object» Email`
`- valor : String {readOnly}` · `+ Email(valor : String)` (valida RFC 5322 simplificado y normaliza a minúsculas) · `+ getValor() : String`

### Enumeraciones
| Enumeración | Literales |
|---|---|
| `TipoMovimiento` | `INGRESO`, `GASTO` |
| `MetodoPago` | `EFECTIVO`, `TARJETA_DEBITO`, `TARJETA_CREDITO`, `TRANSFERENCIA`, `OTRO` |
| `EstadoCuenta` | `ACTIVA`, `ELIMINADA` |
| `EstadoPresupuesto` | `OK`, `ALERTA`, `EXCEDIDO` |

## 3.4 `LOG-01` — relaciones (exactas)

| # | Origen | Relación | Destino | Mult. origen | Mult. destino | Rol en destino / nombre | Navegable | Justificación |
|---|---|---|---|---|---|---|---|---|
| R1 | Gasto | **Generalización** | Movimiento | — | — | — | — | Gasto *es un* Movimiento. |
| R2 | Ingreso | **Generalización** | Movimiento | — | — | — | — | Ingreso *es un* Movimiento. Conjunto `{complete, disjoint}`. |
| R3 | Usuario | **Asociación** | Movimiento | 1 | 0..* | `movimientos` | Movimiento → Usuario por `idUsuario` | El usuario se elimina de forma lógica (D-1, `ON DELETE RESTRICT`); sus movimientos no se borran en cascada. |
| R4 | Usuario | **Asociación** | Categoria | 0..1 | 0..* | `categoriasPersonalizadas` | Categoria → Usuario por `idPropietario` | Con el borrado lógico (D-1) la categoría personalizada se conserva; `0..1` permite las predeterminadas sin dueño. |
| R5 | Usuario | **Asociación** | Presupuesto | 1 | 0..* | `presupuestos` | Presupuesto → Usuario | Igual que R3. |
| R6 | Movimiento | **Asociación** dirigida | Categoria | 0..* | 1 | `categoria` / "se clasifica en" | → Categoria | La categoría existe independientemente (predeterminada o reutilizada). |
| R7 | Presupuesto | **Asociación** dirigida | Categoria | 0..* | 1 | `categoria` / "limita" | → Categoria | Restricción `{categoria.tipo = GASTO}`. |
| R8 | Movimiento | **Composición** (◆ en Movimiento) | Dinero | 1 | 1 | `monto` | → Dinero | Value object embebido. (En VP basta mostrarlo como atributo tipado; dibuja la composición solo si quieres hacerla explícita.) |
| R9 | Presupuesto | **Composición** | Dinero | 1 | 1 | `limite` | → Dinero | Igual que R8. |
| R10 | Usuario | **Composición** | Email | 1 | 1 | `correo` | → Email | Igual que R8. |
| R11 | Movimiento | **Dependencia** `«use»` | TipoMovimiento | — | — | — | — | Retorno de `getTipo()`. |

Restricciones OCL (como *Constraint* en VP):
- `context Movimiento inv: self.categoria.tipo = self.getTipo()`
- `context Movimiento inv: self.monto.esPositivo()`
- `context Presupuesto inv: self.categoria.tipo = TipoMovimiento::GASTO`

## 3.5 `LOG-02 Aplicación y puertos`

### Interfaces de entrada (puertos primarios)
| Interface | Operaciones |
|---|---|
| `IAutenticacion` | `+ registrar(cmd : RegistroCmd) : UsuarioDTO` · `+ iniciarSesion(correo : String, clave : String) : TokensDTO` · `+ renovarSesion(refreshToken : String) : TokensDTO` · `+ cerrarSesion(refreshToken : String) : void` · `+ solicitarRecuperacion(correo : String) : void` · `+ restablecerContrasena(token : String, nuevaClave : String) : void` |
| `IGestionMovimientos` | `+ registrarGasto(cmd : RegistrarGastoCmd) : MovimientoDTO` · `+ registrarIngreso(cmd : RegistrarIngresoCmd) : MovimientoDTO` · `+ modificar(idUsuario : UUID, idMovimiento : UUID, cmd : ModificarMovimientoCmd) : MovimientoDTO` · `+ eliminar(idUsuario : UUID, idMovimiento : UUID) : void` |
| `IGestionCategorias` | `+ listarDisponibles(idUsuario : UUID, tipo : TipoMovimiento) : List<CategoriaDTO>` · `+ crear(idUsuario : UUID, nombre : String, tipo : TipoMovimiento) : CategoriaDTO` · `+ renombrar(idUsuario : UUID, idCategoria : UUID, nombre : String) : CategoriaDTO` · `+ eliminar(idUsuario : UUID, idCategoria : UUID, idCategoriaDestino : UUID) : void` (reasigna movimientos) |
| `IGestionPresupuestos` | `+ definir(idUsuario : UUID, idCategoria : UUID, periodo : YearMonth, limite : Dinero) : PresupuestoDTO` · `+ listar(idUsuario : UUID, periodo : YearMonth) : List<PresupuestoDTO>` · `+ eliminar(idUsuario : UUID, idPresupuesto : UUID) : void` |
| `IConsultaFinanciera` | `+ consultarHistorial(idUsuario : UUID, filtro : FiltroMovimientos, pagina : int, tamano : int) : Pagina<MovimientoDTO>` · `+ obtenerMovimiento(idUsuario : UUID, idMovimiento : UUID) : MovimientoDTO` · `+ obtenerResumen(idUsuario : UUID, desde : LocalDate, hasta : LocalDate) : ResumenFinanciero` |
| `IEvaluadorPresupuesto` | `+ evaluar(gasto : Gasto) : void` |

### Servicios (`«service»`) — cada uno **realiza** su interfaz
| Clase | Realiza | Atributos (inyectados, `- {readOnly}`) |
|---|---|---|
| `ServicioAutenticacion` | `IAutenticacion` | `repoUsuarios : IRepositorioUsuarios`, `cifrador : ICifradorContrasena`, `emisor : IEmisorTokens`, `notificador : INotificador` |
| `ServicioMovimientos` | `IGestionMovimientos` | `repoMovimientos : IRepositorioMovimientos`, `repoCategorias : IRepositorioCategorias`, `evaluador : IEvaluadorPresupuesto` |
| `ServicioCategorias` | `IGestionCategorias` | `repoCategorias : IRepositorioCategorias`, `repoMovimientos : IRepositorioMovimientos` |
| `ServicioPresupuestos` | `IGestionPresupuestos`, `IEvaluadorPresupuesto` | `repoPresupuestos : IRepositorioPresupuestos`, `repoMovimientos : IRepositorioMovimientos`, `notificador : INotificador` |
| `ServicioConsultaFinanciera` | `IConsultaFinanciera` | `repoMovimientos : IRepositorioMovimientos`, `repoPresupuestos : IRepositorioPresupuestos` |

Todos tienen un constructor público que recibe sus dependencias (inyección por constructor).

### Puertos de salida (interfaces)
| Interface | Operaciones |
|---|---|
| `IRepositorioUsuarios` | `+ guardar(u : Usuario) : void` · `+ buscarPorId(id : UUID) : Optional<Usuario>` · `+ buscarPorCorreo(correo : Email) : Optional<Usuario>` · `+ existeCorreo(correo : Email) : boolean` |
| `IRepositorioMovimientos` | `+ guardar(m : Movimiento) : void` · `+ eliminar(m : Movimiento) : void` · `+ buscarDeUsuario(idUsuario : UUID, id : UUID) : Optional<Movimiento>` · `+ buscar(idUsuario : UUID, filtro : FiltroMovimientos, pagina : int, tamano : int) : Pagina<Movimiento>` · `+ sumarPorCategoria(idUsuario : UUID, desde : LocalDate, hasta : LocalDate) : List<TotalPorCategoria>` · `+ sumarGastos(idUsuario : UUID, idCategoria : UUID, periodo : YearMonth) : Dinero` · `+ reasignarCategoria(idUsuario : UUID, origen : UUID, destino : UUID) : int` |
| `IRepositorioCategorias` | `+ guardar(c : Categoria) : void` · `+ eliminar(c : Categoria) : void` · `+ buscarDisponible(id : UUID, idUsuario : UUID) : Optional<Categoria>` · `+ listarDisponibles(idUsuario : UUID, tipo : TipoMovimiento) : List<Categoria>` |
| `IRepositorioPresupuestos` | `+ guardar(p : Presupuesto) : void` · `+ eliminar(p : Presupuesto) : void` · `+ buscar(idUsuario : UUID, idCategoria : UUID, periodo : YearMonth) : Optional<Presupuesto>` · `+ buscarPorPeriodo(idUsuario : UUID, periodo : YearMonth) : List<Presupuesto>` |
| `ICifradorContrasena` | `+ cifrar(clave : String) : String` · `+ coincide(clave : String, hash : String) : boolean` |
| `IEmisorTokens` | `+ emitir(u : Usuario) : TokensDTO` · `+ validarRefresh(token : String) : UUID` · `+ revocar(token : String) : void` |
| `INotificador` | `+ enviarRecuperacion(correo : Email, enlace : String) : void` · `+ enviarAlertaPresupuesto(correo : Email, p : PresupuestoDTO) : void` |

### DTO y objetos de consulta (`«dto»`, inmutables)
| Clase | Atributos (`- {readOnly}`) |
|---|---|
| `RegistrarGastoCmd` | `idUsuario : UUID`, `monto : BigDecimal`, `fecha : LocalDate`, `descripcion : String`, `idCategoria : UUID`, `metodoPago : MetodoPago` |
| `RegistrarIngresoCmd` | igual sin `metodoPago`, con `fuente : String` |
| `ModificarMovimientoCmd` | `monto`, `fecha`, `descripcion`, `idCategoria` |
| `FiltroMovimientos` | `desde : LocalDate`, `hasta : LocalDate`, `tipo : TipoMovimiento [0..1]`, `idCategoria : UUID [0..1]` · `+ validar() : void` (desde ≤ hasta, ≤ 24 meses) |
| `Pagina<T>` (clase plantilla, parámetro `T`) | `elementos : List<T>`, `pagina : int`, `tamano : int`, `totalElementos : long` |
| `MovimientoDTO` | `id`, `tipo`, `monto`, `fecha`, `descripcion`, `categoria : CategoriaDTO`, `metodoPago [0..1]`, `fuente [0..1]` |
| `CategoriaDTO` · `PresupuestoDTO` · `UsuarioDTO` · `TokensDTO` | campos públicos de lectura equivalentes |
| `ResumenFinanciero` | `desde`, `hasta`, `totalIngresos : Dinero`, `totalGastos : Dinero`, `balance : Dinero`, `porCategoria : List<TotalPorCategoria>`, `presupuestos : List<PresupuestoDTO>` |
| `TotalPorCategoria` | `categoria : CategoriaDTO`, `total : Dinero`, `porcentaje : BigDecimal` |

### Relaciones de `LOG-02`
| Origen | Relación | Destino |
|---|---|---|
| Cada `Servicio*` | **Realization** | su(s) interfaz(es) de entrada (tabla anterior) |
| Cada `Servicio*` | **Association** dirigida, mult. `1`, rol = nombre del atributo | cada puerto de salida que tiene como atributo |
| `ServicioMovimientos` | **Association** dirigida → `IEvaluadorPresupuesto` | (la realiza `ServicioPresupuestos`; nunca dependencia directa entre servicios) |
| `ResumenFinanciero` | **Composición** 1 ◆— 0..* | `TotalPorCategoria` |
| `ResumenFinanciero` | **Agregación** 1 ◇— 0..* (rol `presupuestos`) | `PresupuestoDTO` (los presupuestos existen y se consultan sin el resumen; el resumen solo los agrupa) |
| `TotalPorCategoria` | **Asociación** dirigida 0..* → 1 | `CategoriaDTO` |
| Interfaces de entrada / servicios | **Dependency `«use»`** | clases de `dominio` que manipulan (opcional: muéstralas solo si no saturan) |

> **Agregación vs composición, criterio aplicado:** composición cuando la parte no existe fuera del todo
> ni se comparte con otro todo (R8, R9, R10, `ResumenFinanciero ◆ TotalPorCategoria`). Agregación (◇)
> solo cuando el todo agrupa partes con ciclo de vida propio (`ResumenFinanciero ◇ PresupuestoDTO`).
> UML 2.5 deja la semántica de la agregación compartida abierta, así que no se usa como sinónimo de
> "contiene"; v1.0 la usaba en `Historial ◇ Compra`, que además era un camino redundante.

## 3.6 Referencia PlantUML (solo para revisar el resultado; el modelo se construye en VP)

```plantuml
@startuml LOG-01
skinparam classAttributeIconSize 0
enum TipoMovimiento { INGRESO
 GASTO }
enum MetodoPago { EFECTIVO
 TARJETA_DEBITO
 TARJETA_CREDITO
 TRANSFERENCIA
 OTRO }
class Usuario <<entity>> {
 - id : UUID {readOnly}
 - nombre : String
 - correo : Email
 - hashContrasena : String
 - estado : EstadoCuenta
 - intentosFallidos : int
 + cambiarContrasena(nuevoHash : String) : void
 + registrarIntentoFallido(ahora : LocalDateTime) : void
 + estaBloqueado(ahora : LocalDateTime) : boolean
}
abstract class Movimiento <<entity>> {
 # id : UUID {readOnly}
 # idUsuario : UUID {readOnly}
 # monto : Dinero
 # fecha : LocalDate
 # descripcion : String [0..1]
 + {abstract} getTipo() : TipoMovimiento
 + getSigno() : int
 + modificar(monto : Dinero, fecha : LocalDate, descripcion : String, categoria : Categoria) : void
 + perteneceA(idUsuario : UUID) : boolean
 # validar() : void
}
class Gasto <<entity>> { - metodoPago : MetodoPago
 + getTipo() : TipoMovimiento }
class Ingreso <<entity>> { - fuente : String [0..1]
 + getTipo() : TipoMovimiento }
class Categoria <<entity>> {
 - id : UUID {readOnly}
 - nombre : String
 - tipo : TipoMovimiento {readOnly}
 - idPropietario : UUID [0..1]
 + esPredeterminada() : boolean
 + estaDisponiblePara(idUsuario : UUID) : boolean
 + renombrar(nombre : String) : void
}
class Presupuesto <<entity>> {
 - id : UUID {readOnly}
 - periodo : YearMonth
 - limite : Dinero
 - notificado : boolean
 + calcularEstado(consumo : Dinero) : EstadoPresupuesto
 + debeNotificar(consumo : Dinero) : boolean
 + marcarNotificado() : void
}
class Dinero <<value object>> {
 - cantidad : BigDecimal {readOnly}
 - moneda : Currency {readOnly}
 + sumar(otro : Dinero) : Dinero
 + restar(otro : Dinero) : Dinero
 + porcentajeDe(total : Dinero) : BigDecimal
 + {static} cero(moneda : Currency) : Dinero
}
Movimiento <|-- Gasto
Movimiento <|-- Ingreso
Usuario "1" *-- "0..*" Movimiento : movimientos
Usuario "0..1" *-- "0..*" Categoria : categoriasPersonalizadas
Usuario "1" *-- "0..*" Presupuesto : presupuestos
Movimiento "0..*" --> "1" Categoria : categoria
Presupuesto "0..*" --> "1" Categoria : categoria
Movimiento ..> TipoMovimiento
@enduml
```

## 3.7 Cómo construirlo en Visual Paradigm

1. `3 Lógica` › *New Diagram › Class Diagram* → `LOG-01 Dominio`. Crea dentro un *Package* `dominio`.
2. **Clases:** herramienta *Class*. Atributos: clic derecho › *Add › Attribute* y escribe con sintaxis
   UML, p. ej. `-fecha : LocalDate` (VP interpreta visibilidad y tipo). Multiplicidad de atributo y
   `{readOnly}`: *Open Specification › Attributes › (atributo) › Multiplicity / Read only*.
3. **Abstracta:** en `Movimiento` › *Open Specification › General › Abstract* ✔ (el nombre pasa a cursiva).
   En `getTipo()` › *Abstract* ✔.
4. **Estático:** en `cero()` › *Scope: classifier* (aparece subrayado).
5. **Enumeraciones:** herramienta *Enumeration* (no Class); añade *Enumeration Literal*.
6. **Estereotipos:** *Open Specification › Stereotypes › Edit Stereotypes* → crea `entity`, `value object`,
   `service`, `dto` con base *Class*. No crees `interface` ni `realiza`/`usa` (error L6/T3).
7. **Generalización:** herramienta *Generalization* arrastrando **desde la subclase hacia la superclase**.
   Selecciona ambas generalizaciones › clic derecho › *Generalization Set* → `{complete, disjoint}`.
8. **Composición:** herramienta *Composition* arrastrando **desde el todo (Usuario) hacia la parte**; el
   rombo negro queda en el todo. Después: doble clic en cada extremo › *Multiplicity* (`1` / `0..*`),
   *Role name* (`movimientos`) y *Navigable* (deja navegable solo el lado indicado en R3–R10).
9. **Asociación dirigida:** herramienta *Association*; en el extremo destino *Navigable = True*, en el
   origen *Navigable = False* (o *Unspecified*).
10. **Restricciones:** clic derecho en la clase › *Add › Constraint* y pega la OCL de 3.4.
11. `LOG-02`: nuevo *Class Diagram*; paquetes `aplicacion.puertos.entrada`, `aplicacion.servicios`,
    `aplicacion.puertos.salida`, `aplicacion.dto`. Interfaces con la herramienta **Interface**; los
    servicios se unen con **Realization** (desde el servicio hacia la interfaz). Las clases del dominio se
    arrastran desde el *Model Explorer* si se necesitan (no se recrean).
12. `Pagina<T>`: *Open Specification › Template Parameters › Add* `T`.
13. Validación final: *Tools › Model Quality Checker* (o *Validate*) sin advertencias de tipos no
    resueltos: todos los tipos (`Dinero`, `Email`, DTO) deben ser clases del modelo, no texto libre —
    esto evita el error L2 (`IAccesoDatos` inexistente).
