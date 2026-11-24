# 2. Vista Funcional

Contiene: el diagrama de casos de uso (`FUN-01`), las especificaciones de los CU clave, los
componentes funcionales con sus interfaces (`FUN-02`), y los diagramas de actividad (`ACT-*`) y
secuencia (`SEQ-*`).

## 2.1 Actores

| Actor | Tipo | Descripción |
|---|---|---|
| Usuario | Primario, humano | Persona registrada (o por registrarse) que gestiona sus finanzas. |
| Servicio de correo | Secundario, `«system»` | Sistema externo que entrega correos. Participa en CU-09 y CU-10. |
| Reloj del sistema | Secundario, `«system»` | Dispara el respaldo diario (no tiene CU de negocio; se documenta en Despliegue). |

## 2.2 Catálogo de casos de uso

| ID | Caso de uso | Actor | Prioridad | Relaciones |
|---|---|---|---|---|
| CU-01 | Registrarse | Usuario | Alta | — |
| CU-02 | Iniciar sesión | Usuario | Alta | — |
| CU-03 | Registrar movimiento | Usuario | Alta | ⟵ `«extend»` CU-10 (punto: *Gasto guardado*) |
| CU-04 | Modificar o eliminar movimiento | Usuario | Media | — |
| CU-05 | Gestionar categorías personalizadas | Usuario | Media | — |
| CU-06 | Gestionar presupuestos | Usuario | Media | — |
| CU-07 | Consultar historial de movimientos | Usuario | Alta | `«include»` CU-11 |
| CU-08 | Consultar resumen financiero | Usuario | Alta | `«include»` CU-11 |
| CU-09 | Recuperar contraseña | Usuario, Servicio de correo | Media | — |
| CU-10 | Notificar presupuesto excedido | Servicio de correo | Baja | `«extend»` → CU-03 |
| CU-11 | Seleccionar período y filtros | — (incluido) | — | incluido por CU-07, CU-08 |

**Reglas UML aplicadas (corrigen F1/F2):**
- El actor se une a los CU con **Association** (línea simple), nunca con `Dependency «use»`.
- **No** se usa `«include» Iniciar sesión` en cada CU (error frecuente): la autenticación es una
  **precondición**.
- `«include»`: la flecha va del CU base al incluido (CU-07 ⟶ CU-11). `«extend»`: la flecha va del CU que
  extiende al base (CU-10 ⟶ CU-03), con punto de extensión y condición.

## 2.3 Especificaciones de los casos de uso clave

### CU-03 Registrar movimiento

| Campo | Contenido |
|---|---|
| Actor primario | Usuario |
| Precondiciones | Sesión iniciada (JWT válido). Existe al menos una categoría disponible del tipo elegido. |
| Postcondición de éxito | El movimiento queda persistido, asociado al usuario y a una categoría compatible con su tipo; el resumen del período lo refleja. |
| Postcondición de fallo | No se persiste nada; el usuario ve el motivo. |
| Disparador | El usuario pulsa **Nuevo movimiento**. |

**Flujo básico**
1. El usuario elige el tipo: *Gasto* o *Ingreso*.
2. El sistema muestra las categorías disponibles de ese tipo (predeterminadas + personalizadas del usuario).
3. El usuario ingresa monto, fecha, descripción, categoría y, si es gasto, método de pago.
4. El usuario confirma.
5. El sistema valida los datos (RN-01…RN-04).
6. El sistema guarda el movimiento.
7. **Punto de extensión *Gasto guardado***.
8. El sistema confirma el registro y actualiza el saldo del período mostrado.

**Flujos alternativos**
- **5a. Datos inválidos:** el sistema marca los campos incorrectos y vuelve al paso 3.
- **5b. Categoría no disponible** (eliminada o de otro usuario): el sistema informa el error, recarga las categorías y vuelve al paso 2.
- **6a. Fallo de persistencia:** el sistema informa "No se pudo guardar, intenta de nuevo" y conserva los datos del formulario.
- **\*a. Sesión expirada:** el sistema intenta renovar con el *refresh token*; si falla, redirige a CU-02 conservando el borrador.

**Reglas de negocio**
- RN-01 `monto > 0` con máximo 2 decimales y ≤ 999 999 999 999,99.
- RN-02 `fecha` ≤ hoy + 1 día (tolerancia de zona horaria).
- RN-03 El tipo de la categoría debe coincidir con el tipo del movimiento.
- RN-04 La categoría debe ser predeterminada o pertenecer al usuario.

### CU-07 Consultar historial de movimientos

| Campo | Contenido |
|---|---|
| Precondición | Sesión iniciada. |
| Postcondición | Ninguna (solo lectura). |

1. El usuario abre **Historial**.
2. *Include CU-11:* el usuario elige período (por defecto, mes actual) y opcionalmente tipo y categoría.
3. El sistema devuelve la primera página (20 elementos) ordenada por fecha descendente, con el total del filtro.
4. El usuario navega entre páginas o abre un movimiento (→ CU-04).

- **3a. Sin resultados:** el sistema muestra "No hay movimientos para este filtro" y un acceso a CU-03.
- **2a. Rango inválido** (desde > hasta, o > 24 meses): el sistema rechaza el filtro.

### CU-08 Consultar resumen financiero

1. El usuario abre **Resumen**.
2. *Include CU-11.*
3. El sistema calcula: total de ingresos, total de gastos, balance (ingresos − gastos), gasto por categoría (monto y %) y, para cada presupuesto del período, consumo y estado (*OK*, *80 %*, *Excedido*).
4. El sistema muestra cifras y gráfico por categoría.

### CU-10 Notificar presupuesto excedido (extiende CU-03)

- **Condición de extensión:** `movimiento.tipo = GASTO` **y** existe un presupuesto de esa categoría para el mes de la fecha **y** el consumo, incluido este gasto, supera el límite por primera vez en el mes.
- 1. El sistema marca el presupuesto como notificado. 2. El sistema solicita al Servicio de correo enviar la alerta. 3. Regresa al paso 8 de CU-03.
- **2a. El correo falla:** se registra el error y se reintenta (3 intentos, *backoff* exponencial); **el registro del gasto no se revierte**.

### CU-01, CU-02, CU-09 (resumen)

| CU | Flujo básico | Alternativos clave |
|---|---|---|
| CU-01 | Ingresa nombre, correo y contraseña → valida formato y unicidad del correo → guarda con hash BCrypt → inicia sesión. | Correo ya registrado; contraseña < 10 caracteres. |
| CU-02 | Ingresa correo y contraseña → verifica hash → emite *access token* (15 min) y *refresh token* (7 días, cookie `HttpOnly`). | Credenciales inválidas (mensaje genérico); 5 intentos fallidos → bloqueo de 15 min. |
| CU-09 | Ingresa correo → genera token de un solo uso (30 min) → Servicio de correo envía enlace → define nueva contraseña → se invalidan todas las sesiones. | Correo inexistente: misma respuesta que éxito (evita enumerar usuarios). |

## 2.4 Componentes funcionales e interfaces (`FUN-02`)

Estos componentes son **los mismos elementos** que se detallan en la Vista de Desarrollo; aquí solo se
muestran sus responsabilidades e interfaces.

| Componente | Responsabilidad | Provee | Requiere | CU |
|---|---|---|---|---|
| Cliente Web (SPA) | Interfaz de usuario, validación de formularios, gestión de tokens. | — | API REST (HTTP) | Todos |
| API REST | Traducir HTTP ⇄ casos de uso; autenticar cada petición; mapear errores a códigos HTTP. | API REST `/api/v1` | IAutenticacion, IGestionMovimientos, IGestionCategorias, IGestionPresupuestos, IConsultaFinanciera | Todos |
| Identidad | Registro, credenciales, tokens, recuperación. | `IAutenticacion` | IRepositorioUsuarios, ICifradorContrasena, IEmisorTokens, INotificador | CU-01, 02, 09 |
| Movimientos | Alta/edición/baja de gastos e ingresos con sus reglas. | `IGestionMovimientos` | IRepositorioMovimientos, IRepositorioCategorias, IEvaluadorPresupuesto | CU-03, 04 |
| Categorías | Categorías predeterminadas y personalizadas. | `IGestionCategorias` | IRepositorioCategorias, IRepositorioMovimientos | CU-05 |
| Presupuestos | Presupuestos y evaluación de consumo. | `IGestionPresupuestos`, `IEvaluadorPresupuesto` | IRepositorioPresupuestos, IRepositorioMovimientos, INotificador | CU-06, 10 |
| Consultas | Historial paginado y resumen financiero. | `IConsultaFinanciera` | IRepositorioMovimientos, IRepositorioPresupuestos | CU-07, 08, 11 |
| Notificaciones | Envío de correos con reintentos. | `INotificador` | Servicio de correo (SMTP) | CU-09, 10 |
| Persistencia | Adaptador JPA de todos los repositorios. | `IRepositorio*` | PostgreSQL (JDBC) | — |

Dependencia acíclica: `Cliente → API → {Identidad, Movimientos, Categorías, Presupuestos, Consultas} →
puertos ← {Persistencia, Notificaciones}`. *Movimientos* usa a *Presupuestos* solo a través de
`IEvaluadorPresupuesto` (corrige la dependencia invertida L3 de v1.0).

## 2.5 Diagrama de actividad `ACT-01 Registrar movimiento`

**Particiones (swimlanes):** `Usuario` | `Cliente Web` | `API / Servicio de aplicación` | `Base de datos`

| # | Partición | Nodo | Tipo UML |
|---|---|---|---|
| 1 | Usuario | ● Inicio | Initial Node |
| 2 | Usuario | Seleccionar tipo de movimiento | Action |
| 3 | Cliente Web | Solicitar categorías del tipo | Action |
| 4 | API | Consultar categorías disponibles | Action |
| 5 | Base de datos | Leer categorías (predeterminadas + del usuario) | Action |
| 6 | Usuario | Completar formulario | Action |
| 7 | Cliente Web | Validar formato | Action |
| 8 | Cliente Web | ◇ ¿Formato válido? `[no]` → 6 · `[sí]` → 9 | Decision Node |
| 9 | Cliente Web | Enviar `POST /api/v1/movimientos` | Send Signal Action (opcional) / Action |
| 10 | API | Validar reglas RN-01…RN-04 | Action |
| 11 | API | ◇ ¿Reglas cumplidas? `[no]` → 12 · `[sí]` → 13 | Decision Node |
| 12 | Cliente Web | Mostrar errores → ◆ merge antes de 6 | Action + Merge Node |
| 13 | Base de datos | Insertar movimiento | Action |
| 14 | API | ◇ ¿Es gasto con presupuesto? `[no]` → 17 · `[sí]` → 15 | Decision Node |
| 15 | API | Evaluar consumo del presupuesto | Action |
| 16 | API | ◇ ¿Excedido por primera vez? `[sí]` → *Notificar* (acción asíncrona, sin esperar) · `[no]` → 17 | Decision Node |
| 17 | Cliente Web | ◆ Merge → Mostrar confirmación y saldo actualizado | Merge Node + Action |
| 18 | Usuario | ◉ Fin | Activity Final Node |

Notas de rigor: cada **Decision Node** tiene guardas mutuamente excluyentes en `[ ]`; cada rama que se
reúne pasa por un **Merge Node** (no por otra decisión); hay un único nodo final de actividad. La
notificación puede modelarse con un *Fork* hacia un *Flow Final Node* para indicar que no bloquea.

`ACT-02 Iniciar sesión` y `ACT-03 Recuperar contraseña` siguen el mismo patrón con los flujos de 2.3.

## 2.6 Diagrama de secuencia `SEQ-01 Registrar gasto` (CU-03 + CU-10)

**Lifelines (en orden):**
`:Usuario` (actor) · `:ClienteWeb` · `:MovimientoController` «boundary» · `:ServicioMovimientos` «control» ·
`:IRepositorioCategorias` · `:Gasto` «entity» · `:IRepositorioMovimientos` · `:IEvaluadorPresupuesto` ·
`:INotificador`

| # | Mensaje | Tipo |
|---|---|---|
| 1 | Usuario → ClienteWeb: `confirmarFormulario(datos)` | síncrono |
| 2 | ClienteWeb → MovimientoController: `POST /api/v1/movimientos {tipo:GASTO,…}` + `Authorization: Bearer` | síncrono |
| 3 | MovimientoController → MovimientoController: `idUsuario = token.sub` | self |
| 4 | MovimientoController → ServicioMovimientos: `registrarGasto(cmd: RegistrarGastoCmd)` | síncrono |
| 5 | ServicioMovimientos → IRepositorioCategorias: `buscarDisponible(idCategoria, idUsuario)` | síncrono |
| 6 | ↩ `Optional<Categoria>` | retorno |
| — | **`alt`** `[categoría vacía o tipo ≠ GASTO]` → 7a ServicioMovimientos ⟶ Controller: `CategoriaNoDisponibleException` ⟶ ClienteWeb: `422` · **`[else]`** → 7 | fragmento |
| 7 | ServicioMovimientos → `«create»` Gasto: `new Gasto(idUsuario, Dinero, fecha, descripcion, categoria, metodoPago)` | creación |
| 8 | Gasto → Gasto: `validar()` (RN-01, RN-02) | self |
| 9 | ServicioMovimientos → IRepositorioMovimientos: `guardar(gasto)` | síncrono |
| 10 | ServicioMovimientos → IEvaluadorPresupuesto: `evaluar(gasto)` | síncrono |
| — | **`opt`** `[presupuesto excedido por primera vez]` → 11 IEvaluadorPresupuesto ⟶ INotificador: `enviarAlertaPresupuesto(usuario, presupuesto)` (**asíncrono**, punta abierta) | fragmento |
| 12 | ↩ ServicioMovimientos ⟶ Controller: `MovimientoDTO` | retorno |
| 13 | ↩ Controller ⟶ ClienteWeb: `201 Created` + `Location` | retorno |
| 14 | ClienteWeb ⟶ Usuario: muestra confirmación | retorno |

Marca la transacción con un fragmento **`critical`** o una nota `{transaccional}` sobre los mensajes
5–10 (rollback si falla 9; el paso 11 queda fuera de la transacción).

### `SEQ-02 Consultar resumen financiero` (CU-08)

`ClienteWeb → ConsultaController: GET /api/v1/resumen?desde&hasta` → `ServicioConsultaFinanciera.obtenerResumen(idUsuario, periodo)` →
`IRepositorioMovimientos.sumarPorCategoria(idUsuario, periodo)` (agregación en la BD, **no** traer todos
los movimientos a memoria) → `IRepositorioPresupuestos.buscarPorPeriodo(idUsuario, mes)` → `«create» ResumenFinanciero` → `200 OK`.

### `SEQ-03 Iniciar sesión` (CU-02)

`ClienteWeb → AuthController: POST /api/v1/auth/login` → `ServicioAutenticacion.iniciarSesion(correo, clave)` →
`IRepositorioUsuarios.buscarPorCorreo` → `ICifradorContrasena.coincide(clave, hash)` →
**`alt`** `[no coincide]` `registrarIntentoFallido` → `401` · `[else]` `IEmisorTokens.emitir(usuario)` → `200 {accessToken}` + cookie refresh.

## 2.7 Cómo modelarlo en Visual Paradigm

**FUN-01 Casos de uso**
1. `2 Funcional` › *New Diagram › Use Case Diagram*.
2. Paleta *System* → rectángulo `Finanzas Personales` (frontera). Todos los CU van dentro.
3. Arrastra el actor `Usuario` desde el *Model Explorer* (el mismo de CTX-01). Crea `Servicio de correo` como *Actor* con estereotipo `«system»` a la derecha.
4. Une actor y CU con **Association** (no *Dependency*).
5. `«include»` y `«extend»`: usa las herramientas *Include* y *Extend* de la paleta (ya traen el estereotipo). En CU-03: *Open Specification › Extension Points › Add* → `Gasto guardado`; en la relación *Extend* selecciona ese punto y escribe la condición en *Condition*.
6. Escribe cada especificación en el CU: clic derecho › *Open Use Case Details* → pestañas *Info* (precondiciones), *Flow of Events* (flujos básicos/alternativos) y *Requirements*.

**FUN-02 Componentes funcionales**
1. *New Diagram › Component Diagram*. Crea los componentes de la tabla 2.4 **una sola vez** (luego se reutilizan en DEV-02).
2. Interfaces: herramienta **Interface** (no *Class*). Provista: *Realization* del componente a la interfaz → clic derecho en la interfaz › *Presentation Options › Display as Ball*. Requerida: *Usage* del componente consumidor a la interfaz (VP la dibuja como *socket*).
3. No uses puertos en esta vista; si los usas, toda relación debe salir del puerto.

**ACT-01**
1. *New Diagram › Activity Diagram*. Paleta *Swimlane* › *Vertical Swimlane* con 4 particiones.
2. Coloca los nodos de la tabla 2.5; guardas en *Control Flow › Guard*.
3. Desde CU-03: clic derecho › *Sub Diagrams › Add Existing Diagram* → enlaza ACT-01 para la trazabilidad.

**SEQ-01**
1. *New Diagram › Sequence Diagram*. Actor y *Lifeline*; en cada lifeline *Base Classifier* → selecciona la clase o interfaz real del modelo lógico (así los mensajes ofrecen las operaciones existentes).
2. Mensajes: *Message* (síncrono), *Asynchronous Message* para el paso 11, *Create Message* para el paso 7, *Return Message* para respuestas.
3. Fragmentos: paleta *Combined Fragment* → `alt`, `opt`, `critical`; agrega *Interaction Operand* con guardas.
4. Enlaza el diagrama a CU-03 (*Sub Diagrams*).
