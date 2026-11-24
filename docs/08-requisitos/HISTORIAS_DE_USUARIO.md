# Historias de usuario — Finanzas Personales

Producto: aplicación web de finanzas personales.
Frontend: **TypeScript + React** (Cliente Web). Backend: **Java 21 + Spring Boot 3.3** (API REST). Base de datos: PostgreSQL 16.

## Convenciones

| Campo | Significado |
|---|---|
| **ID** | `HU-nn`. Cada historia se traza a un caso de uso (`CU-nn`) y a requerimientos funcionales (`RF-nn`, ver [REQUERIMIENTOS_FUNCIONALES.md](REQUERIMIENTOS_FUNCIONALES.md)). |
| **Prioridad** | MoSCoW: **M** (Must, imprescindible), **S** (Should, importante), **C** (Could, deseable). |
| **Estimación** | Puntos de historia (escala Fibonacci: 1, 2, 3, 5, 8). |
| **Criterios de aceptación** | Formato Gherkin: *Dado / Cuando / Entonces*. |
| **Definición de terminado** | Ver la sección final; aplica a todas las historias. |

## Épicas

| Épica | Objetivo | Historias |
|---|---|---|
| E1 Cuenta y acceso | Que cada persona tenga una cuenta propia y segura. | HU-01 a HU-05 |
| E2 Movimientos | Registrar y mantener gastos e ingresos. | HU-06 a HU-09 |
| E3 Categorías | Clasificar los movimientos a la medida de cada usuario. | HU-10 a HU-12 |
| E4 Presupuestos | Controlar el gasto mensual por categoría. | HU-13 a HU-15 |
| E5 Consultas | Entender en qué se va el dinero. | HU-16 a HU-19 |

---

## E1 — Cuenta y acceso

### HU-01 Registrarme
**Como** visitante **quiero** crear una cuenta con mi nombre, correo y contraseña **para** guardar mis finanzas de forma privada.

| Prioridad | Estimación | CU | RF |
|---|---|---|---|
| M | 3 | CU-01 | RF-01, RF-02 |

**Criterios de aceptación**
1. **Dado** que el correo no está registrado, **cuando** envío nombre, correo válido y una contraseña de al menos 10 caracteres, **entonces** se crea la cuenta y quedo con la sesión iniciada.
2. **Dado** que el correo ya existe, **cuando** intento registrarme, **entonces** veo "Este correo ya está registrado" y no se crea otra cuenta.
3. **Dado** una contraseña de menos de 10 caracteres, **cuando** envío el formulario, **entonces** el Cliente Web lo rechaza antes de llamar a la API e indica el mínimo.
4. La contraseña nunca se guarda en texto plano: se almacena con hash BCrypt (coste 12).

### HU-02 Iniciar sesión
**Como** usuario registrado **quiero** iniciar sesión con mi correo y contraseña **para** acceder a mis datos.

| Prioridad | Estimación | CU | RF |
|---|---|---|---|
| M | 3 | CU-02 | RF-03, RF-04 |

**Criterios de aceptación**
1. **Dado** credenciales correctas, **cuando** inicio sesión, **entonces** recibo un token de acceso válido por 15 minutos y una cookie de renovación `HttpOnly` válida por 7 días.
2. **Dado** credenciales incorrectas, **cuando** inicio sesión, **entonces** veo "Correo o contraseña incorrectos", sin indicar cuál de los dos falló.
3. **Dado** 5 intentos fallidos seguidos, **cuando** hago el sexto, **entonces** la cuenta queda bloqueada 15 minutos y se me informa.

### HU-03 Mantener la sesión activa
**Como** usuario **quiero** que mi sesión se renueve sola mientras uso la aplicación **para** no tener que volver a escribir la contraseña cada 15 minutos.

| Prioridad | Estimación | CU | RF |
|---|---|---|---|
| S | 2 | CU-02 | RF-05 |

**Criterios de aceptación**
1. **Dado** un token de acceso vencido y una cookie de renovación válida, **cuando** el Cliente Web hace una petición, **entonces** obtiene un token nuevo y repite la petición sin que yo lo note.
2. **Dado** que la cookie de renovación venció o fue revocada, **entonces** se me lleva a iniciar sesión y se conserva lo que tenía escrito en el formulario.

### HU-04 Cerrar sesión
**Como** usuario **quiero** cerrar sesión **para** que nadie más use mi cuenta en este dispositivo.

| Prioridad | Estimación | CU | RF |
|---|---|---|---|
| M | 1 | CU-02 | RF-06 |

**Criterios de aceptación**
1. **Cuando** cierro sesión, **entonces** el token de renovación queda revocado en el servidor y el Cliente Web borra los tokens locales.
2. **Dado** que cerré sesión, **cuando** intento usar el token anterior, **entonces** la API responde `401`.

### HU-05 Recuperar mi contraseña
**Como** usuario que olvidó su contraseña **quiero** recibir un enlace por correo para crear una nueva **para** recuperar el acceso a mi cuenta.

| Prioridad | Estimación | CU | RF |
|---|---|---|---|
| S | 5 | CU-09 | RF-07, RF-08 |

**Criterios de aceptación**
1. **Cuando** solicito la recuperación con mi correo, **entonces** recibo un enlace de un solo uso válido por 30 minutos.
2. **Dado** un correo que no existe, **cuando** solicito la recuperación, **entonces** veo el mismo mensaje que si existiera, para no revelar qué correos están registrados.
3. **Cuando** defino la nueva contraseña con un enlace válido, **entonces** se cierran todas mis sesiones abiertas y el enlace deja de servir.
4. **Dado** un enlace vencido o ya usado, **entonces** veo "El enlace no es válido" y puedo pedir otro.

---

## E2 — Movimientos

### HU-06 Registrar un gasto
**Como** usuario **quiero** registrar un gasto con monto, fecha, categoría, método de pago y descripción **para** llevar el control de lo que gasto.

| Prioridad | Estimación | CU | RF |
|---|---|---|---|
| M | 5 | CU-03 | RF-09, RF-11 |

**Criterios de aceptación**
1. **Dado** que tengo sesión iniciada, **cuando** registro un gasto con monto mayor que 0, fecha no futura y una categoría de tipo GASTO, **entonces** se guarda y el saldo del mes se actualiza.
2. **Dado** un monto de 0, negativo o con más de 2 decimales, **entonces** el formulario marca el campo y no se guarda nada.
3. **Dado** una categoría de tipo INGRESO, **cuando** la elijo para un gasto, **entonces** la API responde `422` ("La categoría no corresponde al tipo de movimiento").
4. **Dado** una categoría de otro usuario, **entonces** la API responde `422` y no revela que esa categoría existe.

### HU-07 Registrar un ingreso
**Como** usuario **quiero** registrar un ingreso con monto, fecha, categoría y fuente **para** conocer mi balance real.

| Prioridad | Estimación | CU | RF |
|---|---|---|---|
| M | 3 | CU-03 | RF-10, RF-11 |

**Criterios de aceptación**
1. **Cuando** registro un ingreso válido con una categoría de tipo INGRESO, **entonces** se guarda y se suma al balance del mes.
2. Las validaciones de monto y fecha son las mismas de HU-06.

### HU-08 Modificar un movimiento
**Como** usuario **quiero** corregir un gasto o ingreso que registré mal **para** que mis cifras sean correctas.

| Prioridad | Estimación | CU | RF |
|---|---|---|---|
| S | 3 | CU-04 | RF-12 |

**Criterios de aceptación**
1. **Cuando** cambio monto, fecha, descripción o categoría de un movimiento propio, **entonces** se guardan los cambios y se vuelven a aplicar todas las reglas de registro.
2. **Dado** un movimiento de otro usuario, **cuando** intento modificarlo, **entonces** la API responde `404`.
3. Al cambiar la fecha o la categoría de un gasto, se recalcula el presupuesto afectado.

### HU-09 Eliminar un movimiento
**Como** usuario **quiero** eliminar un movimiento **para** quitar registros duplicados o equivocados.

| Prioridad | Estimación | CU | RF |
|---|---|---|---|
| S | 2 | CU-04 | RF-13 |

**Criterios de aceptación**
1. **Cuando** elimino un movimiento propio, **entonces** el Cliente Web pide confirmación y, tras confirmar, el movimiento desaparece del historial y de los totales.
2. **Dado** un movimiento de otro usuario, **entonces** la API responde `404`.

---

## E3 — Categorías

### HU-10 Usar categorías predeterminadas
**Como** usuario nuevo **quiero** tener categorías listas (Alimentación, Transporte, Salario…) **para** empezar a registrar sin configurar nada.

| Prioridad | Estimación | CU | RF |
|---|---|---|---|
| M | 2 | CU-05 | RF-14 |

**Criterios de aceptación**
1. **Cuando** registro un movimiento, **entonces** veo las categorías predeterminadas del tipo elegido y, además, las mías.
2. Las categorías predeterminadas no se pueden renombrar ni eliminar.

### HU-11 Crear y renombrar mis categorías
**Como** usuario **quiero** crear y renombrar categorías propias **para** clasificar mis movimientos a mi manera.

| Prioridad | Estimación | CU | RF |
|---|---|---|---|
| S | 3 | CU-05 | RF-15, RF-16 |

**Criterios de aceptación**
1. **Cuando** creo una categoría con nombre de 1 a 40 caracteres y un tipo (GASTO o INGRESO), **entonces** queda disponible solo para mí.
2. **Dado** que ya tengo una categoría con ese nombre y ese tipo, **entonces** veo "Ya existe una categoría con ese nombre" (`409`).

### HU-12 Eliminar una categoría propia
**Como** usuario **quiero** eliminar una categoría que ya no uso, pasando sus movimientos a otra **para** no perder información.

| Prioridad | Estimación | CU | RF |
|---|---|---|---|
| C | 3 | CU-05 | RF-17 |

**Criterios de aceptación**
1. **Dado** que la categoría tiene movimientos, **cuando** la elimino, **entonces** debo elegir una categoría destino del mismo tipo y todos sus movimientos se reasignan.
2. **Dado** que la categoría no tiene movimientos, **entonces** se elimina directamente.

---

## E4 — Presupuestos

### HU-13 Definir un presupuesto mensual
**Como** usuario **quiero** fijar un límite de gasto mensual para una categoría **para** controlar cuánto gasto en ella.

| Prioridad | Estimación | CU | RF |
|---|---|---|---|
| S | 3 | CU-06 | RF-18 |

**Criterios de aceptación**
1. **Cuando** defino un presupuesto con límite mayor que 0 para una categoría de tipo GASTO y un mes, **entonces** se guarda.
2. **Dado** que ya existe un presupuesto para esa categoría y ese mes, **entonces** se actualiza el límite en lugar de crear otro.
3. **Dado** una categoría de tipo INGRESO, **entonces** la API responde `422`.

### HU-14 Ver el estado de mis presupuestos
**Como** usuario **quiero** ver cuánto llevo gastado de cada presupuesto **para** saber si debo frenar el gasto.

| Prioridad | Estimación | CU | RF |
|---|---|---|---|
| S | 3 | CU-06, CU-08 | RF-19 |

**Criterios de aceptación**
1. **Cuando** abro mis presupuestos de un mes, **entonces** veo para cada uno el límite, el consumo, el porcentaje y el estado: **OK** (menos del 80 %), **ALERTA** (80 % a 99 %) o **EXCEDIDO** (100 % o más).

### HU-15 Recibir una alerta al exceder un presupuesto
**Como** usuario **quiero** recibir un correo la primera vez que supero un presupuesto en el mes **para** enterarme a tiempo.

| Prioridad | Estimación | CU | RF |
|---|---|---|---|
| C | 5 | CU-10 | RF-20 |

**Criterios de aceptación**
1. **Dado** un gasto que hace superar el límite por primera vez en el mes, **cuando** se guarda, **entonces** se envía un correo de alerta.
2. **Dado** que ya se envió la alerta ese mes, **cuando** registro otro gasto en la categoría, **entonces** no se envía otro correo.
3. **Dado** que el servicio de correo falla, **entonces** el gasto igual queda guardado y el envío se reintenta hasta 3 veces.

---

## E5 — Consultas

### HU-16 Consultar mi historial
**Como** usuario **quiero** ver mis movimientos ordenados del más reciente al más antiguo **para** revisar en qué gasté.

| Prioridad | Estimación | CU | RF |
|---|---|---|---|
| M | 3 | CU-07 | RF-21 |

**Criterios de aceptación**
1. **Cuando** abro el historial, **entonces** veo los movimientos del mes actual en páginas de 20, ordenados por fecha descendente.
2. **Dado** que no hay movimientos, **entonces** veo "No hay movimientos para este filtro" y un acceso para registrar uno.

### HU-17 Filtrar el historial
**Como** usuario **quiero** filtrar por período, tipo y categoría **para** encontrar movimientos concretos.

| Prioridad | Estimación | CU | RF |
|---|---|---|---|
| S | 3 | CU-07, CU-11 | RF-22 |

**Criterios de aceptación**
1. **Cuando** aplico un rango de fechas, un tipo o una categoría, **entonces** el historial y su total muestran solo los movimientos que cumplen el filtro.
2. **Dado** un rango en el que la fecha inicial es posterior a la final, o un rango mayor a 24 meses, **entonces** el filtro se rechaza con un mensaje claro.

### HU-18 Ver mi resumen financiero
**Como** usuario **quiero** ver mis ingresos, gastos y balance de un período **para** saber si estoy ahorrando.

| Prioridad | Estimación | CU | RF |
|---|---|---|---|
| M | 5 | CU-08 | RF-23 |

**Criterios de aceptación**
1. **Cuando** abro el resumen de un período, **entonces** veo total de ingresos, total de gastos y balance (ingresos − gastos).
2. Los montos se calculan con precisión decimal exacta (`BigDecimal` en Java, `NUMERIC(14,2)` en la base de datos).

### HU-19 Ver el gasto por categoría
**Como** usuario **quiero** ver cuánto gasté en cada categoría, con su porcentaje y un gráfico **para** identificar dónde puedo recortar.

| Prioridad | Estimación | CU | RF |
|---|---|---|---|
| S | 3 | CU-08 | RF-24 |

**Criterios de aceptación**
1. **Cuando** abro el resumen, **entonces** veo una lista y un gráfico con el total y el porcentaje de cada categoría de gasto, de mayor a menor.
2. Los porcentajes suman 100 %, con un margen de redondeo de ±0,1 %.

---

## Definición de terminado (aplica a todas las historias)

- Criterios de aceptación cubiertos por pruebas automatizadas:
  - **backend (Java):** JUnit 5 para las reglas de dominio y pruebas de integración del endpoint;
  - **frontend (TypeScript):** pruebas de componente del formulario o de la vista.
- El usuario solo puede leer o modificar sus propios datos; esto está verificado con una prueba que usa un segundo usuario.
- Los errores de la API siguen el formato *Problem Details* (RFC 9457) y el Cliente Web los muestra en español.
- El contrato OpenAPI está actualizado.
- Revisión de código aprobada y diagramas de Visual Paradigm actualizados si cambió el diseño.

## Resumen del backlog

| Prioridad | Historias | Puntos |
|---|---|---|
| M (imprescindible) | HU-01, 02, 04, 06, 07, 10, 16, 18 | 25 |
| S (importante) | HU-03, 05, 08, 09, 11, 13, 14, 17, 19 | 27 |
| C (deseable) | HU-12, 15 | 8 |
| **Total** | **19 historias** | **60** |
