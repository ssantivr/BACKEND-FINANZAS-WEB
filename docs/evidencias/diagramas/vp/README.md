# Diagramas en Visual Paradigm (5 vistas del profesor)

Los diagramas de `modelo/Finanzas_Personales_5_diagramas.vpp` se actualizaron el 21-11-2026 para que coincidan con el código real (Spring Boot + MongoDB + React). Siguen la plantilla del profesor (`Modelo de presentación de diseño.pptx`), con una vista por diapositiva: contexto, funcional, lógica, desarrollo y despliegue.

## Respaldo y verificación

- **Respaldo** del original, idéntico byte a byte: `..\RESPALDO_VPP\Finanzas_Personales_5_diagramas_antes_de_mejorar_2026-11-21.vpp`. El original también está en git.
- **Verificación:** [../../pruebas/vp-diagramas.log](../../pruebas/vp-diagramas.log) guarda:
  - los hashes del respaldo y del archivo mejorado;
  - la comparación de elementos entre ambos;
  - la lista de imágenes exportadas.

## Diagramas

| Diagrama | Vista del profesor | Qué muestra | Sale de |
|---|---|---|---|
| [CTX-01 Contexto del sistema](CTX-01%20Contexto%20del%20sistema.png) | Vista de contexto | Diagrama de contexto (DFD de Yourdon/DeMarco): el sistema en el centro; el Usuario y el Servicio de correo alrededor, con el dato que viaja en cada flujo | `NotificadorSmtp.java` (correo de recuperación y de alerta) |
| [FUN-01 Componentes funcionales](FUN-01%20Componentes%20funcionales.png) | Vista funcional | Componentes UML con interfaces provistas y requeridas que llevan el nombre real de los puertos | `aplicacion/puertos/entrada` y `salida`; `infraestructura/*` |
| [LOG-01 Modelo de dominio](LOG-01%20Modelo%20de%20dominio.png) | Vista lógica | Clases del dominio con atributos, métodos, multiplicidades, herencia `Movimiento → Gasto/Ingreso`, objetos de valor y enumeraciones | `dominio/modelo`, `valor`, `tipos` |
| [LOG-02 Modelo de datos (MongoDB)](LOG-02%20Modelo%20de%20datos%20(MongoDB).png) | Vista lógica (datos) | Las 6 colecciones de la base `finanzas` con sus campos y tipos BSON (`[0..1]` = opcional), sus índices (`unique`, `index`, `ttl`) y sus referencias por UUID con multiplicidad | `base-de-datos/01_colecciones.js`, `02_indices.js`, `*Documento.java` |
| [DEV-01 Capas y dependencias](DEV-01%20Capas%20y%20dependencias.png) | Vista de desarrollo | Al estilo del PDF de API REST del profesor, de punta a punta: frontend `features` (páginas) → `api` (clientes HTTP) → `web` (Controller) → `servicios` (Service) → `persistencia` (Repository) → `modelo` (entidades). Cada clase muestra solo sus métodos públicos; las flechas dicen qué hace cada capa con la siguiente | `infraestructura/web`, `aplicacion/servicios`, `infraestructura/persistencia`, `dominio/modelo` |
| [DEV-02 Detalle de clases (anexo)](DEV-02%20Detalle%20de%20clases%20por%20paquete%20(anexo).png) | Vista de desarrollo (anexo) | Todos los paquetes y clases con atributos y firmas completas, frontend incluido. Es para consulta, no para explicar en clase | `backend/src/main/java/co/finanzas/**`, `frontend/src/**` |
| [DEP-01 Despliegue local](DEP-01%20Despliegue%20local%20(verificado).png) | Vista de despliegue | Lo que corre hoy en el computador: Navegador → Vite (:5173) → JVM (:8080) → MongoDB (:27017). Las herramientas de prueba (Postman, Compass) no se dibujan porque no son parte del sistema | `iniciar.cmd`, `vite.config.ts`, `application.yml` |
| [DEP-02 Producción planificada](DEP-02%20Despliegue%20en%20producci%C3%B3n%20(planificado,%20NO%20VERIFICADO).png) | Vista de despliegue | Navegador → Vercel (`dist/` y reescritura `/api/*`) → Render (contenedor Docker ⊃ JVM ⊃ `finanzas-api.jar`). Desde la JVM: MongoDB Atlas (protocolo MongoDB sobre TLS) y un servidor SMTP opcional | `frontend/vercel.json`, `render.yaml`, `backend/Dockerfile` |

## Proyecto solo de despliegues: `modelo/Finanzas_Personales_despliegues.vpp`

Es un `.vpp` aparte que reúne **solo DEP-01 y DEP-02**, para presentar la vista de despliegue por separado. Se generó desde `Finanzas_Personales_5_diagramas.vpp` con `ExportXML -diagram` e `ImportXML` en un proyecto nuevo, y sus imágenes son idénticas a las del proyecto principal.

Los diagramas muestran solo lo esencial:

- nodos;
- entornos de ejecución;
- artefactos;
- protocolo y puerto de cada conexión.

Los detalles de configuración (versiones, variables de entorno, cabeceras) están en `render.yaml`, `vercel.json`, `backend/Dockerfile` y `application.yml`, y se resumen en [docs/arquitectura/vista-despliegue.md](../../../arquitectura/vista-despliegue.md).

### Cómo se aplicaron las reglas del profesor (PDF de despliegue)

- Notación: `«device»` es un equipo, `«executionEnvironment»` una plataforma (Navegador, Node.js, JVM, MongoDB) y `«node»` un servicio administrado.
- Los artefactos son unidades desplegables, no clases.
- Cada conexión indica su protocolo y su puerto.
- No se dibujó infraestructura que no existe. MongoDB nunca aparece dentro de la JVM.
- Las conexiones salen del entorno que de verdad se comunica (por ejemplo, la JVM habla con MongoDB Atlas), no solo de la caja exterior.
- Colores: «device» en blanco, «executionEnvironment» en azul claro y «node» (servicio administrado) en gris claro.

## Qué cambió respecto al original

- **CTX-01:** se quitó "Almacenamiento de respaldos (pg_dump)", porque el código no implementa respaldos. El correo pasa a ser "SMTP con STARTTLS, opcional".
- **FUN-01:**
  - las interfaces llevan el nombre real de los puertos (`IGestionMovimientos`, `IRepositorioCategorias`, `INotificador`…);
  - PostgreSQL y JDBC `:5432` se reemplazaron por MongoDB `:27017`;
  - el frontend aparece como React + TypeScript.
- **LOG-01:** se añadió el atributo `color : String` a `Categoria`, que existe en `Categoria.java`.
- **LOG-01, segunda revisión** ([correcciones](../../../arquitectura/correcciones-vpp.md), hechas con [`corregir_log01_vpp.py`](corregir_log01_vpp.py)):
  - se quitó `Usuario.bloqueada`, que no existe en el código;
  - se añadieron `id`, `idUsuario` e `idPropietario`;
  - se añadieron 6 métodos que faltaban;
  - las asociaciones con `Usuario` apuntan hacia él ("pertenece a"), porque cada hijo guarda el UUID de su dueño.

  El anexo DEV-02 se ajustó para mostrar los mismos cambios.
- **DEP-01 y DEP-02:** son nuevos y reemplazan el despliegue con PostgreSQL, Docker Compose, nginx y máquinas virtuales.
- **DEV-01:** el diagrama anterior tenía unas 100 clases con todos sus atributos y no se podía leer en una diapositiva. Pasó a ser el anexo **DEV-02**, y **DEV-01** es ahora la vista por capas, con unas 20 clases y solo sus métodos.
- **FUN-01, DEP-01 y DEP-02, segunda revisión** (filas 11 a 15 de [correcciones](../../../arquitectura/correcciones-vpp.md), hechas con [`corregir_fun_dep_vpp.py`](corregir_fun_dep_vpp.py)):
  - en FUN-01 se añadieron 3 dependencias que faltaban y se quitaron los códigos RF/HU que tapaban las líneas;
  - los despliegues quedaron sin valores etiquetados, sin texto repetido y sin herramientas de prueba;
  - las conexiones de DEP-02 indican su puerto.
- **Tercera revisión: frontend, backend y base de datos completos** (filas 16 a 19 de [correcciones](../../../arquitectura/correcciones-vpp.md), hechas con [`completar_vpp.py`](completar_vpp.py)):
  - nuevo LOG-02 con el modelo de datos de MongoDB;
  - DEV-01 incluye el frontend;
  - DEV-02 tiene las 33 clases que faltaban;
  - las operaciones de `MovimientosApi` y `ClienteHttp` son las del código.
- **Diagramas obsoletos:** se **eliminaron** del `.vpp` los antiguos CTX-01 (con respaldos) y DEP-01 (PostgreSQL y Docker). También se fueron los elementos que solo usaban ellos y una clase vacía sin nombre.
  - Se comprobó que los 6 diagramas vigentes se ven **idénticos píxel a píxel** antes y después de la limpieza.
  - Siguen disponibles en el respaldo y en el historial de git.
- **Restricciones OCL:** las 3 reglas del dominio (`tipo coherente` y `monto positivo` en `Movimiento`, `solo gastos` en `Presupuesto`) se conservan como restricciones de esas clases.

## NO VERIFICADO

- **DEP-02 Producción:** es el escenario planificado. El sitio publicado no corresponde a esta versión (ver `docs/INFORME_FINAL.md`, sección 0).
- **Servidor SMTP:** opcional. En local, los correos se imprimen en la consola de la API.

## Cómo se hizo

La licencia de evaluación de la interfaz de Visual Paradigm 18.1 venció. Por eso se usaron sus herramientas de línea de comandos, que sí funcionan:

1. **ExportXML** exportó el proyecto a XML.
2. [`mejorar_vpp.py`](mejorar_vpp.py) editó el XML, clonando las figuras existentes como plantilla.
3. **ImportXML** fusionó el XML editado en una copia del `.vpp`.
4. **ExportDiagramImage** generó estos PNG.
5. Para el proyecto de despliegues: **ExportXML -diagram** con DEP-01 y DEP-02, seguido de **ImportXML** en un proyecto nuevo.
6. Limpieza y vista por capas:
   - **ExportXML -diagram** con los diagramas vigentes;
   - [`vista_capas_vpp.py`](vista_capas_vpp.py) añade DEV-01 (atributos ocultos y métodos sin parámetros) y restaura las restricciones OCL;
   - **ImportXML** crea un proyecto nuevo.

Las imágenes llevan la marca de agua "evaluation copy" que pone Visual Paradigm.
