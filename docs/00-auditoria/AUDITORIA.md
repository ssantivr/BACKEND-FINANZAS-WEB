# Auditoría del modelo v1.0 (`Finanzas_Personales.vpp`)

Fuente auditada: proyecto VP con 5 diagramas — `1 Vista de Contexto` (DFD), `2 Vista Funcional`
(componentes), `3 Vista Lógica` (clases), `4 Vista de Desarrollo` (componentes) y
`5 Vista de Despliegue`. Los hallazgos se extrajeron leyendo directamente los elementos y relaciones
del archivo, no las imágenes.

Severidad: **A** = error UML o de arquitectura que invalida el diagrama · **M** = inconsistencia
entre vistas o mala práctica relevante · **B** = calidad/presentación.

## 1. Errores transversales (todas las vistas)

| # | Sev. | Hallazgo en v1.0 | Corrección en v2.0 |
|---|---|---|---|
| T1 | A | **Elementos duplicados en lugar de reutilizados.** *Gestión de Compras*, *Gestión de Categorías* e *Información Financiera* existen como componentes distintos en la Vista Funcional y en la de Desarrollo, y otra vez como artefactos sin relación en Despliegue. El modelo no puede validar consistencia. | Un único elemento por concepto, reutilizado por arrastre desde el *Model Explorer*; artefactos vinculados con `«manifest»` a los componentes. |
| T2 | M | **Nombres distintos para lo mismo:** `Consulta de Historial` (funcional) vs `Gestión de Historial` (desarrollo); `Almacenamiento de Información` vs `Persistencia`; `RepositorioCompras` (lógica) vs `IRepositorioCompras` (desarrollo); `ServicioCompras` vs `IGestionCompras`. | Glosario y matriz de trazabilidad (`06-trazabilidad`) con un nombre canónico por elemento. |
| T3 | M | **Dos estereotipos `use`** (uno para `Usage`, otro para `Dependency`), más `«usa»` y `«realiza»` inventados, y `«Interface»`/`«interface»` con mayúscula y minúscula. | Solo estereotipos estándar; las realizaciones y usos se expresan con el tipo de relación, no con un estereotipo. |
| T4 | A | **Alcance funcional incompleto:** existe un *Almacén de Usuarios* y una clase `Usuario`, pero ningún caso de uso de registro, autenticación o cierre de sesión. Tampoco hay forma de editar/eliminar compras ni de crear categorías (aunque *Gestión de Categorías* dice "administrar"). | Casos de uso CU-01…CU-09 que cubren el ciclo de vida completo (cuenta, movimientos, categorías, presupuestos, consultas). |
| T5 | M | **No existen diagramas de comportamiento** (actividad/secuencia): ninguna vista explica cómo colaboran los componentes en tiempo de ejecución. | ACT-01..03 y SEQ-01..03 en la Vista Funcional. |
| T6 | M | **Sin requisitos de calidad** (seguridad, validación, errores, rendimiento). | Atributos de calidad explícitos en Contexto y tácticas en Desarrollo/Despliegue. |
| T7 | B | Notas vacías (5 en la Vista Funcional) y notas que solo repiten el nombre del elemento (*Usuario*, *Almacén de…*). | Eliminadas; las notas solo documentan restricciones o supuestos. |

## 2. Vista de Contexto

| # | Sev. | Hallazgo | Corrección |
|---|---|---|---|
| C1 | A | Se usó un **DFD** y los tres **almacenes de datos internos** (Categorías, Compras, Usuarios) aparecen **fuera** del sistema, como si fueran sistemas externos. Un diagrama de contexto muestra el sistema como caja negra; su persistencia es interna. | El sistema es una sola caja `«system»`; la base de datos aparece solo en Desarrollo y Despliegue. |
| C2 | M | Flujos con **cuatro datos en una sola flecha** ("Confirmación de registro / Historial de compras / Compras por categoría / Información financiera"). Ilegible y no trazable. | Una interacción por responsabilidad, etiquetada con el CU que la origina. |
| C3 | M | **No hay límite del sistema** dibujado ni sistemas externos (correo para recuperación de contraseña, respaldos). | Frontera explícita, actor secundario *Servicio de correo* y *Almacenamiento de respaldos*. |
| C4 | B | Los nombres de elementos se duplican con notas superpuestas. | Eliminado. |

## 3. Vista Funcional

| # | Sev. | Hallazgo | Corrección |
|---|---|---|---|
| F1 | A | **Las "interfaces" son verbos** (`obtener categoría`, `guardar compra`, `registrar compra`…): mezclan casos de uso con interfaces de software. (Usar la herramienta *Interface* de VP, que crea una `Class «Interface»`, es correcto; el error es el nombre y el nivel de abstracción.) | Casos de uso en un diagrama de casos de uso; interfaces con nombre sustantivo (`IGestionMovimientos`). |
| F2 | A | El **actor `Usuario` tiene dependencias `«use»` hacia clases-interfaz**. En UML un actor se asocia con casos de uso (asociación), no depende de clases. | Actor ↔ casos de uso mediante *Association*. |
| F3 | M | **Puertos colgantes:** 3 de 6 puertos (en *Gestión de Compras*, *Consulta de Historial*, *Información Financiera*) no tienen ninguna relación; otros vínculos salen del componente y no del puerto. Uso inconsistente. | Se eliminan los puertos (no aportan en este nivel); interfaces provistas/requeridas en notación *ball-and-socket*. |
| F4 | M | `Almacenamiento de Información` es un **componente técnico en la vista funcional**, y *Consulta de Historial* usa `listar categorías` sin necesitarlo según su responsabilidad. | La persistencia pasa a ser un puerto (`IRepositorio*`) de la capa de aplicación; solo se muestran dependencias justificadas. |
| F5 | B | Números rojos "1…5" sueltos sin leyenda. | Pasos numerados dentro de las especificaciones de CU. |

## 4. Vista Lógica (diagrama de clases)

| # | Sev. | Hallazgo | Corrección |
|---|---|---|---|
| L1 | A | **`Historial` modelado como entidad persistente** (`idHistorial`) con composición 1-1 desde `Usuario` y agregación hacia `Compra`. A la vez existe `Usuario —realiza→ Compra`. Hay **dos caminos** Usuario→Compra que pueden contradecirse; el historial es solo una consulta. | Se elimina `Historial` como entidad; `Usuario 1 ◆— 0..* Movimiento`; el historial es el resultado `Pagina<Movimiento>` de `IConsultaFinanciera`. |
| L2 | A | **Tipo inexistente:** `RepositorioComprasImpl.acceso : IAccesoDatos` y `RepositorioCategoriasImpl.acceso : IAccesoDatos`, pero `IAccesoDatos` no está definido en el modelo (solo como texto "SUPOSICIÓN" en Desarrollo). | Los adaptadores usan JPA (`EntityManager`/Spring Data); no se inventa una interfaz genérica de acceso a datos. |
| L3 | A | **Dependencia invertida entre vistas:** en Lógica `ServicioHistorial → ServicioInformacionFinanciera`, pero en Funcional y Desarrollo es *Información Financiera → Historial*. Además `InformacionFinanciera → Historial`: riesgo de ciclo. | Grafo de dependencias acíclico y único: `ServicioConsultaFinanciera → IRepositorioMovimientos`. |
| L4 | A | **Lógica de servicio en la entidad:** `Usuario.registrarCompra(c: Categoria): Compra` y `Usuario.consultarHistorial()` duplican `ServicioCompras.registrarCompra(...)` y `ServicioHistorial.consultarHistorial(u)` con firmas distintas (la de `Usuario` ni siquiera recibe monto ni fecha). | Las entidades encapsulan invariantes; los casos de uso viven en servicios de aplicación. |
| L5 | M | **`InformacionFinanciera` mezcla DTO y calculadora**: tiene totales como atributos y a la vez `calcularTotal(h: Historial)`. | `ResumenFinanciero` inmutable (resultado) + `ServicioConsultaFinanciera` (cálculo). |
| L6 | M | En la Vista Lógica las interfaces usan un estereotipo **creado a mano `«interface»` (minúscula)**, distinto del `«Interface»` nativo de VP que sí se usó en la Vista Funcional; además las realizaciones llevan `«realiza»`. VP no trata esas clases como interfaces (no se muestran como *ball*). | Herramienta *Interface* de VP (`«Interface»` nativo) + *Realization* sin estereotipo. |
| L7 | A | **Seguridad (IDOR):** `consultarCompra(id: int)` y `buscarPorId(id: int)` no reciben el usuario; cualquiera puede leer compras ajenas conociendo el id. IDs `int` secuenciales lo facilitan. | Todas las consultas reciben `idUsuario`; ids `UUID`. |
| L8 | M | **Sin herencia, sin enumeraciones, sin value objects.** Solo se registran compras: no hay ingresos, por lo que "información financiera" no puede calcular un balance. | `Movimiento` abstracto ← `Gasto`, `Ingreso`; `Dinero` y `Email` como value objects; `TipoMovimiento`, `MetodoPago`. |
| L9 | M | Servicios concretos sin interfaz mientras la Vista de Desarrollo declara `IGestionCompras`, etc. | Cada servicio realiza su interfaz de entrada (`IGestion*`). |
| L10 | B | Asociaciones sin nombres de rol ni navegabilidad (solo `pertenece a` es navegable). Atributos sin visibilidad explícita. | Roles, navegabilidad y visibilidad en todas las relaciones y miembros. |
| L11 | B | `Map<Categoria, BigDecimal>` como atributo: dependencia oculta a `Categoria` y difícil de serializar. | `List<TotalPorCategoria>`. |

## 5. Vista de Desarrollo

| # | Sev. | Hallazgo | Corrección |
|---|---|---|---|
| D1 | A | **La base de datos como componente de software** (`Base de Datos` provee `IAccesoDatos`). Un SGBD es un nodo/entorno de ejecución, no un componente de la aplicación. | La BD solo aparece en Despliegue; el componente `Persistencia` es un adaptador que implementa los puertos `IRepositorio*`. |
| D2 | M | **Atributos usados como texto libre** ("Responsabilidad: …", "Interfaces requeridas: …") dentro de los componentes, y además duplicados en 21 notas. | Responsabilidad en el campo *Documentation*; interfaces como elementos reales. |
| D3 | M | Interfaces expresadas solo como **nombre de la relación `Usage`**, no como elementos *Interface*. No se puede ver qué componente las provee. | Notación *ball-and-socket* con *Interface* + *Realization* (provista) y *Usage* (requerida). |
| D4 | M | **Dependencias de capa en dirección incorrecta:** Lógica de negocio → Persistencia → Base de datos (el dominio depende de la infraestructura). | Arquitectura hexagonal: Infraestructura → Aplicación → Dominio (inversión de dependencias). |
| D5 | B | No hay diagrama de paquetes ni regla de dependencias entre capas. | `DEV-01` paquetes + reglas verificables (ArchUnit). |

## 6. Vista de Despliegue

| # | Sev. | Hallazgo | Corrección |
|---|---|---|---|
| P1 | A | **Todos los nodos con estereotipo `«device»`, incluido `Navegador Web`**, que es software. | `«device»` para hardware/VM; `«executionEnvironment»` para navegador, Docker, JVM, PostgreSQL. |
| P2 | A | **Artefactos = componentes lógicos** (5 artefactos "Gestión de …" en el servidor). Un monolito se despliega como **un** artefacto (`finanzas-api.jar`) que manifiesta varios componentes. | `finanzas-api.jar`, `finanzas-web.zip`, `V1__esquema.sql` con `«manifest»`. |
| P3 | M | La UI se ejecuta en el navegador pero **ningún nodo la sirve**; no hay proxy inverso/TLS, red privada para la BD, puertos ni respaldos. | Nginx (TLS + estáticos + proxy `/api`), subred privada para PostgreSQL, puertos y protocolos etiquetados, respaldo diario. |
| P4 | B | Ruta de comunicación `HTTPS` dibujada desde el servidor hacia el navegador (el cliente inicia la conexión). | Rutas `«HTTPS»`, `«HTTP»`, `«JDBC/TLS»`, `«SMTP»` con extremo iniciador en el cliente. |

## 7. Resumen

- Hallazgos totales: **36** (A: 13 · M: 16 · B: 7).
- Causa raíz común: cada diagrama se construyó aislado, con elementos nuevos, en vez de derivarse de un
  único modelo. v2.0 se construye en el orden **Contexto → Casos de uso → Dominio → Componentes →
  Despliegue**, reutilizando elementos, y se valida con la matriz de `06-trazabilidad`.
