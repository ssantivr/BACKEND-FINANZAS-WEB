# Requerimientos funcionales — Finanzas Personales

| Capa | Tecnología |
|---|---|
| Frontend (Cliente Web) | **TypeScript + React**, compilado con Vite y servido por Nginx |
| Backend (API REST) | **Java 21 + Spring Boot 3.3**, arquitectura hexagonal, empaquetado como `finanzas-api.jar` |
| Persistencia | PostgreSQL 16 mediante Spring Data JPA y migraciones Flyway |

Cada requerimiento indica qué historia lo origina ([HISTORIAS_DE_USUARIO.md](HISTORIAS_DE_USUARIO.md)), su caso de uso y el componente de Visual Paradigm (FUN-01) que lo implementa.

**Prioridad:** **M** (imprescindible) · **S** (importante) · **C** (deseable).

---

## 1. Reglas de negocio

Los requerimientos citan estas reglas por su código.

| Código | Regla |
|---|---|
| RN-01 | El monto de un movimiento es mayor que 0, tiene como máximo 2 decimales y no supera 999 999 999 999,99. |
| RN-02 | La fecha de un movimiento no puede ser posterior a hoy + 1 día (tolerancia de zona horaria). |
| RN-03 | El tipo de la categoría (GASTO / INGRESO) coincide con el tipo del movimiento. |
| RN-04 | Un usuario solo puede usar categorías predeterminadas o categorías propias. |
| RN-05 | Un usuario solo puede leer, modificar o eliminar sus propios datos. El servidor toma el `idUsuario` del token, nunca del cuerpo ni de la URL. |
| RN-06 | El correo es único en el sistema y se guarda en minúsculas. |
| RN-07 | La contraseña tiene al menos 10 caracteres y se guarda con hash BCrypt (coste 12). |
| RN-08 | Tras 5 intentos de inicio de sesión fallidos seguidos, la cuenta se bloquea 15 minutos. |
| RN-09 | El nombre de una categoría tiene de 1 a 40 caracteres y es único por usuario y tipo. |
| RN-10 | Un presupuesto solo aplica a categorías de tipo GASTO y hay como máximo uno por usuario, categoría y mes. |
| RN-11 | Estado del presupuesto: OK si el consumo es menor al 80 %, ALERTA entre el 80 % y el 99 %, EXCEDIDO desde el 100 %. |
| RN-12 | La alerta de presupuesto excedido se envía solo una vez por presupuesto y mes. |
| RN-13 | Un rango de consulta tiene la fecha inicial menor o igual a la final y no supera 24 meses. |

---

## 2. Gestión de cuenta (componente Identidad)

### RF-01 Registrar usuario
| Campo | Detalle |
|---|---|
| Descripción | El sistema permite crear una cuenta con nombre, correo y contraseña. |
| Entradas | `nombre` (2–80 caracteres), `correo`, `clave` |
| Proceso | Valida los datos con RF-02, cifra la contraseña (RN-07), guarda el `Usuario` en estado ACTIVA e inicia sesión (RF-03). |
| Salidas | `201 Created` con `UsuarioDTO` y tokens de sesión. |
| Interfaz | `POST /api/v1/auth/registro` · vista *Registro* del Cliente Web |
| Origen | HU-01 · CU-01 · **M** |

### RF-02 Validar los datos de registro
| Campo | Detalle |
|---|---|
| Descripción | El sistema valida formato y unicidad del correo y la longitud de la contraseña. |
| Proceso | Cliente Web: valida el formato antes de enviar. Backend: aplica RN-06 y RN-07 con Bean Validation y reglas de dominio. |
| Salidas | `400` si el formato es inválido; `409` "Este correo ya está registrado". |
| Origen | HU-01 · CU-01 · **M** |

### RF-03 Iniciar sesión
| Campo | Detalle |
|---|---|
| Descripción | El sistema autentica al usuario con correo y contraseña. |
| Entradas | `correo`, `clave` |
| Proceso | Busca el usuario, comprueba el bloqueo (RF-04) y verifica el hash. Si es correcto, reinicia los intentos fallidos y emite un token de acceso JWT (15 min) y un token de renovación (7 días). |
| Salidas | `200` con `TokensDTO`; token de renovación en una cookie `HttpOnly; Secure; SameSite=Strict`. `401` con mensaje genérico si falla. |
| Interfaz | `POST /api/v1/auth/login` · vista *Inicio de sesión* |
| Origen | HU-02 · CU-02 · **M** |

### RF-04 Bloquear la cuenta por intentos fallidos
| Campo | Detalle |
|---|---|
| Descripción | El sistema cuenta los intentos fallidos y bloquea la cuenta según RN-08. |
| Salidas | `423 Locked` con el tiempo restante de bloqueo. |
| Origen | HU-02 · CU-02 · **M** |

### RF-05 Renovar la sesión
| Campo | Detalle |
|---|---|
| Descripción | El sistema entrega un token de acceso nuevo a partir de un token de renovación válido y rota este último. |
| Proceso | El interceptor HTTP del Cliente Web (TypeScript) detecta el `401`, llama a renovar y repite la petición original. |
| Interfaz | `POST /api/v1/auth/renovar` |
| Origen | HU-03 · CU-02 · **S** |

### RF-06 Cerrar sesión
| Campo | Detalle |
|---|---|
| Descripción | El sistema revoca el token de renovación y el Cliente Web elimina los tokens locales. |
| Interfaz | `POST /api/v1/auth/logout` |
| Origen | HU-04 · CU-02 · **M** |

### RF-07 Solicitar la recuperación de contraseña
| Campo | Detalle |
|---|---|
| Descripción | El sistema genera un token de un solo uso (30 min) y envía el enlace por correo (componente Notificaciones). |
| Salidas | Siempre `202 Accepted` con el mismo mensaje, exista o no el correo. |
| Interfaz | `POST /api/v1/auth/recuperacion` |
| Origen | HU-05 · CU-09 · **S** |

### RF-08 Restablecer la contraseña
| Campo | Detalle |
|---|---|
| Descripción | Con un token válido, el sistema guarda la nueva contraseña (RN-07), marca el token como usado y revoca todas las sesiones del usuario. |
| Salidas | `204`; `400` "El enlace no es válido" si el token venció o ya se usó. |
| Interfaz | `POST /api/v1/auth/restablecer` |
| Origen | HU-05 · CU-09 · **S** |

---

## 3. Movimientos (componente Movimientos)

### RF-09 Registrar un gasto
| Campo | Detalle |
|---|---|
| Descripción | El sistema registra un gasto del usuario autenticado. |
| Entradas | `monto`, `fecha`, `idCategoria`, `metodoPago` (EFECTIVO, TARJETA_DEBITO, TARJETA_CREDITO, TRANSFERENCIA, OTRO), `descripcion` opcional (≤ 140 caracteres) |
| Proceso | Valida con RF-11, crea el `Gasto`, lo guarda y pide la evaluación del presupuesto (RF-20). |
| Salidas | `201 Created` con `MovimientoDTO` y encabezado `Location`. |
| Interfaz | `POST /api/v1/movimientos` (tipo GASTO) · formulario *Nuevo movimiento* |
| Origen | HU-06 · CU-03 · **M** |

### RF-10 Registrar un ingreso
| Campo | Detalle |
|---|---|
| Descripción | El sistema registra un ingreso del usuario autenticado. |
| Entradas | `monto`, `fecha`, `idCategoria`, `fuente` opcional, `descripcion` opcional |
| Proceso | Valida con RF-11, crea el `Ingreso` y lo guarda. |
| Salidas | `201 Created` con `MovimientoDTO`. |
| Interfaz | `POST /api/v1/movimientos` (tipo INGRESO) |
| Origen | HU-07 · CU-03 · **M** |

### RF-11 Validar las reglas de un movimiento
| Campo | Detalle |
|---|---|
| Descripción | Antes de guardar o modificar un movimiento, el sistema aplica RN-01, RN-02, RN-03 y RN-04. |
| Proceso | Cliente Web: valida formato y rangos. Backend (Java): las reglas viven en la entidad `Movimiento.validar()` y en `ServicioMovimientos`. |
| Salidas | `400` por formato; `422` por regla de negocio, con el campo y el motivo. |
| Origen | HU-06, HU-07, HU-08 · CU-03, CU-04 · **M** |

### RF-12 Modificar un movimiento
| Campo | Detalle |
|---|---|
| Descripción | El sistema permite cambiar monto, fecha, descripción y categoría de un movimiento propio (RN-05). |
| Proceso | Revalida con RF-11; si es un gasto, reevalúa el presupuesto del mes anterior y del nuevo. |
| Salidas | `200` con `MovimientoDTO`; `404` si el movimiento no es del usuario. |
| Interfaz | `PUT /api/v1/movimientos/{id}` |
| Origen | HU-08 · CU-04 · **S** |

### RF-13 Eliminar un movimiento
| Campo | Detalle |
|---|---|
| Descripción | El sistema elimina un movimiento propio tras la confirmación del usuario en el Cliente Web. |
| Salidas | `204`; `404` si no es del usuario. |
| Interfaz | `DELETE /api/v1/movimientos/{id}` |
| Origen | HU-09 · CU-04 · **S** |

---

## 4. Categorías (componente Categorías)

### RF-14 Ofrecer categorías predeterminadas
| Campo | Detalle |
|---|---|
| Descripción | El sistema incluye categorías predeterminadas que se cargan con la migración Flyway `V2__categorias_predeterminadas.sql`, por ejemplo Alimentación, Transporte, Vivienda y Salud para gastos, y Salario y Otros ingresos para ingresos. |
| Proceso | Al listar, devuelve las predeterminadas del tipo pedido más las propias del usuario (RN-04). Las predeterminadas no se pueden modificar. |
| Interfaz | `GET /api/v1/categorias?tipo=GASTO` |
| Origen | HU-10 · CU-05 · **M** |

### RF-15 Crear una categoría propia
| Campo | Detalle |
|---|---|
| Entradas | `nombre`, `tipo` |
| Proceso | Aplica RN-09 y asigna como propietario al usuario autenticado. |
| Salidas | `201` con `CategoriaDTO`; `409` si el nombre ya existe. |
| Interfaz | `POST /api/v1/categorias` · vista *Categorías* |
| Origen | HU-11 · CU-05 · **S** |

### RF-16 Renombrar una categoría propia
| Campo | Detalle |
|---|---|
| Proceso | Aplica RN-09. Rechaza el cambio si la categoría es predeterminada o de otro usuario. |
| Salidas | `200`; `403` si es predeterminada; `404` si es de otro usuario. |
| Interfaz | `PATCH /api/v1/categorias/{id}` |
| Origen | HU-11 · CU-05 · **S** |

### RF-17 Eliminar una categoría con reasignación
| Campo | Detalle |
|---|---|
| Entradas | `idCategoria`, `idCategoriaDestino` (obligatorio si la categoría tiene movimientos) |
| Proceso | En una sola transacción, reasigna los movimientos a la categoría destino, que debe ser del mismo tipo, y elimina la categoría. |
| Salidas | `204`; `422` si falta el destino o es de otro tipo. |
| Interfaz | `DELETE /api/v1/categorias/{id}?destino={idDestino}` |
| Origen | HU-12 · CU-05 · **C** |

---

## 5. Presupuestos (componente Presupuestos)

### RF-18 Definir un presupuesto mensual
| Campo | Detalle |
|---|---|
| Entradas | `idCategoria`, `periodo` (año-mes), `limite` (> 0) |
| Proceso | Aplica RN-10. Si ya existe un presupuesto, actualiza el límite y reinicia la marca de notificado. |
| Salidas | `200` / `201` con `PresupuestoDTO`; `422` si la categoría no es de gasto. |
| Interfaz | `PUT /api/v1/presupuestos` · vista *Presupuestos* |
| Origen | HU-13 · CU-06 · **S** |

### RF-19 Calcular el estado de los presupuestos
| Campo | Detalle |
|---|---|
| Descripción | El sistema calcula, para un mes, el consumo de cada presupuesto con una suma en la base de datos, su porcentaje y su estado (RN-11). |
| Salidas | Lista de `PresupuestoDTO` con límite, consumo, porcentaje y estado. |
| Interfaz | `GET /api/v1/presupuestos?periodo=2026-10` |
| Origen | HU-14 · CU-06, CU-08 · **S** |

### RF-20 Notificar un presupuesto excedido
| Campo | Detalle |
|---|---|
| Descripción | Al registrar o modificar un gasto, el sistema evalúa el presupuesto con `IEvaluadorPresupuesto`. Si se excede por primera vez en el mes (RN-12), pide a `INotificador` que envíe un correo. |
| Proceso | El envío es asíncrono (`@Async`) y se reintenta 3 veces con espera exponencial. Si falla, el gasto no se revierte. |
| Origen | HU-15 · CU-10 · **C** |

---

## 6. Consultas (componente Consultas)

### RF-21 Consultar el historial paginado
| Campo | Detalle |
|---|---|
| Descripción | El sistema devuelve los movimientos del usuario ordenados por fecha descendente, en páginas de 20 (máximo 100). |
| Entradas | `pagina`, `tamano`, filtro (RF-22); por defecto, el mes actual. |
| Salidas | `Pagina<MovimientoDTO>` con `elementos`, `pagina`, `tamano` y `totalElementos`. |
| Interfaz | `GET /api/v1/movimientos?desde&hasta&tipo&categoria&pagina&tamano` · vista *Historial* |
| Origen | HU-16 · CU-07 · **M** |

### RF-22 Filtrar los movimientos
| Campo | Detalle |
|---|---|
| Descripción | El sistema filtra por rango de fechas, tipo y categoría (RN-13). El mismo filtro sirve para el historial y para el resumen (CU-11). |
| Salidas | `400` si el rango es inválido. |
| Origen | HU-17 · CU-07, CU-11 · **S** |

### RF-23 Generar el resumen financiero
| Campo | Detalle |
|---|---|
| Descripción | Para un período, el sistema calcula el total de ingresos, el total de gastos y el balance (ingresos − gastos). |
| Proceso | Hace la agregación en SQL (`SUM … GROUP BY`) y no carga todos los movimientos en memoria. Usa `BigDecimal` en Java. |
| Salidas | `ResumenFinanciero` con totales, balance y presupuestos del período (RF-19). |
| Interfaz | `GET /api/v1/resumen?desde&hasta` · vista *Resumen* |
| Origen | HU-18 · CU-08 · **M** |

### RF-24 Mostrar el gasto por categoría
| Campo | Detalle |
|---|---|
| Descripción | El resumen incluye, para cada categoría de gasto, el total y el porcentaje sobre el gasto del período, ordenados de mayor a menor. El Cliente Web los muestra en una lista y un gráfico. |
| Salidas | Lista de `TotalPorCategoria` dentro de `ResumenFinanciero`. |
| Origen | HU-19 · CU-08 · **S** |

---

## 7. Matriz de trazabilidad

| RF | Historia | Caso de uso | Componente (FUN-01) | Interfaz | Prioridad |
|---|---|---|---|---|---|
| RF-01 | HU-01 | CU-01 | Identidad | IAutenticacion | M |
| RF-02 | HU-01 | CU-01 | Identidad + Cliente Web | IAutenticacion | M |
| RF-03 | HU-02 | CU-02 | Identidad, Seguridad | IAutenticacion | M |
| RF-04 | HU-02 | CU-02 | Identidad | IAutenticacion | M |
| RF-05 | HU-03 | CU-02 | Identidad, Seguridad + Cliente Web | IAutenticacion | S |
| RF-06 | HU-04 | CU-02 | Identidad, Seguridad | IAutenticacion | M |
| RF-07 | HU-05 | CU-09 | Identidad, Notificaciones | IAutenticacion, INotificador | S |
| RF-08 | HU-05 | CU-09 | Identidad, Seguridad | IAutenticacion | S |
| RF-09 | HU-06 | CU-03 | Movimientos | IGestionMovimientos | M |
| RF-10 | HU-07 | CU-03 | Movimientos | IGestionMovimientos | M |
| RF-11 | HU-06, 07, 08 | CU-03, CU-04 | Movimientos + Cliente Web | IGestionMovimientos | M |
| RF-12 | HU-08 | CU-04 | Movimientos | IGestionMovimientos | S |
| RF-13 | HU-09 | CU-04 | Movimientos | IGestionMovimientos | S |
| RF-14 | HU-10 | CU-05 | Categorías | IGestionCategorias | M |
| RF-15 | HU-11 | CU-05 | Categorías | IGestionCategorias | S |
| RF-16 | HU-11 | CU-05 | Categorías | IGestionCategorias | S |
| RF-17 | HU-12 | CU-05 | Categorías | IGestionCategorias | C |
| RF-18 | HU-13 | CU-06 | Presupuestos | IGestionPresupuestos | S |
| RF-19 | HU-14 | CU-06, CU-08 | Presupuestos | IGestionPresupuestos | S |
| RF-20 | HU-15 | CU-10 | Presupuestos, Notificaciones | IEvaluadorPresupuesto, INotificador | C |
| RF-21 | HU-16 | CU-07 | Consultas | IConsultaFinanciera | M |
| RF-22 | HU-17 | CU-07, CU-11 | Consultas + Cliente Web | IConsultaFinanciera | S |
| RF-23 | HU-18 | CU-08 | Consultas | IConsultaFinanciera | M |
| RF-24 | HU-19 | CU-08 | Consultas + Cliente Web | IConsultaFinanciera | S |

Todos los requerimientos persisten a través del componente **Persistencia** (`IRepositorio*`, PostgreSQL 16) y entran por el componente **API REST** (Java · Spring Boot).
