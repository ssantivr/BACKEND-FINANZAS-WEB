# Configuración de la cuenta — historias y requerimientos

Épica nueva **E6 Configuración de la cuenta**. Completa la pantalla *Configuración de la cuenta* (`/cuenta`), a la que ya se llega desde el menú de usuario (ver mockup HU-04, opciones *Configuración de la cuenta* y *Cambiar contraseña*), que hasta ahora no tenía historias ni requerimientos.

Estado: **propuesto**. Estas historias y requerimientos amplían [HISTORIAS_DE_USUARIO.md](../HISTORIAS_DE_USUARIO.md) y [REQUERIMIENTOS_FUNCIONALES.md](../REQUERIMIENTOS_FUNCIONALES.md) sin modificarlos. La versión con todos los RF, RNF y HU juntos está en [REQUISITOS_E_HISTORIAS_CONSOLIDADO.md](../REQUISITOS_E_HISTORIAS_CONSOLIDADO.md). Caso de uso nuevo: **CU-12 Configurar la cuenta** (actor: Usuario), aún sin diagrama.

| Archivo | Contenido |
|---|---|
| `CONFIGURACION_DE_LA_CUENTA.md` | Este documento |
| `mockups/HU-20.png` | Eliminar mi cuenta |
| `mockups/HU-21.png` | Actualizar mis datos personales |
| `mockups/HU-22.png` | Cambiar mi contraseña |

## Historias de usuario

### HU-20 Eliminar mi cuenta
**Como** usuario **quiero** eliminar mi cuenta **para** que la aplicación deje de guardar mis datos personales.

| Prioridad | Estimación | CU | RF | Mockup |
|---|---|---|---|---|
| M | 3 | CU-12 | RF-25 | `mockups/HU-20.png` |

**Criterios de aceptación**
1. **Dado** que estoy en *Configuración de la cuenta*, **cuando** pulso *Eliminar mi cuenta*, **entonces** veo un diálogo que explica las consecuencias y que la acción no se puede deshacer.
2. **Dado** el diálogo abierto, **cuando** pulso *Cancelar*, **entonces** no cambia nada.
3. **Dado** el diálogo abierto, **cuando** confirmo, **entonces** la cuenta pasa a `ELIMINADA` con `eliminadaEn`, se revocan todas mis sesiones y vuelvo al inicio de sesión con el aviso «Tu cuenta fue eliminada».
4. **Dado** una cuenta eliminada, **cuando** intento iniciar sesión con su correo, **entonces** recibo el mismo mensaje que con credenciales incorrectas.

### HU-21 Actualizar mis datos personales
**Como** usuario **quiero** ver y cambiar mi nombre **para** que la aplicación me muestre correctamente.

| Prioridad | Estimación | CU | RF | Mockup |
|---|---|---|---|---|
| S | 2 | CU-12 | RF-26 | `mockups/HU-21.png` |

**Criterios de aceptación**
1. **Dado** que abro *Configuración de la cuenta*, **entonces** veo mi nombre, mi correo y la fecha de registro.
2. **Dado** un nombre de 2 a 80 caracteres, **cuando** pulso *Guardar cambios*, **entonces** se guarda, veo «Datos actualizados» y el nombre cambia en el encabezado.
3. **Dado** un nombre vacío o de más de 80 caracteres, **cuando** intento guardar, **entonces** el Cliente Web lo rechaza e indica el límite.
4. El correo se muestra pero no se puede editar (es el identificador de inicio de sesión, RN-06).

### HU-22 Cambiar mi contraseña
**Como** usuario **quiero** cambiar mi contraseña escribiendo la actual **para** mantener segura mi cuenta.

| Prioridad | Estimación | CU | RF | Mockup |
|---|---|---|---|---|
| S | 3 | CU-12 | RF-27 | `mockups/HU-22.png` |

**Criterios de aceptación**
1. **Dado** la contraseña actual correcta y una nueva de al menos 10 caracteres distinta de la actual, **cuando** confirmo, **entonces** se guarda con BCrypt (RN-07) y veo «Contraseña actualizada».
2. **Dado** una contraseña actual incorrecta, **cuando** confirmo, **entonces** veo «La contraseña actual no es correcta» y no cambia nada.
3. **Dado** que la nueva y su confirmación no coinciden, **entonces** el Cliente Web lo indica antes de llamar a la API.
4. Al cambiarla, se revocan las sesiones de los demás dispositivos; la sesión actual sigue activa.

## Requerimientos funcionales

| ID | Requerimiento | Descripción | Interfaz | Componente | Origen | Prioridad |
|---|---|---|---|---|---|---|
| RF-25 | Eliminar la cuenta | Tras la confirmación del usuario, marca la cuenta como `ELIMINADA`, registra `eliminadaEn`, revoca todas sus sesiones y cierra la sesión actual (RNF-14). | `DELETE /api/v1/cuenta` | Identidad | HU-20 · CU-12 | M |
| RF-26 | Actualizar los datos personales | Devuelve nombre, correo y fecha de registro, y permite cambiar el nombre (2–80 caracteres). | `GET /api/v1/cuenta`, `PATCH /api/v1/cuenta` | Identidad | HU-21 · CU-12 | S |
| RF-27 | Cambiar la contraseña | Verifica la contraseña actual, aplica RN-07 a la nueva, la guarda con BCrypt y revoca las demás sesiones. | `PUT /api/v1/cuenta/contrasena` | Identidad, Seguridad | HU-22 · CU-12 | S |

Las tres operaciones se exponen por un puerto de entrada nuevo, **`IGestionCuenta`**, que provee *Identidad* (separado de `IAutenticacion`, que queda solo para registro, sesión y recuperación). Ver FUN-01 y DEV-01 en `modelo/Finanzas_Personales_5_diagramas.vpp`.

## Impacto en el diseño

| Vista | Cambio |
|---|---|
| CTX-01 | Los flujos del Usuario incluyen HU-20 a HU-22. |
| FUN-01 | Nueva interfaz `IGestionCuenta` (Identidad → API REST) con RF-25 a 27 y HU-20 a 22. |
| LOG-01 | `Usuario` gana `cambiarNombre(nombre)` y `eliminar(ahora)`. |
| DEV-01 | `CuentaController` (web), `IGestionCuenta` (puertos.entrada) y `ServicioCuenta` (servicios). |
