# Guion de demostración — PulsoFinance (10 minutos)

## Antes de empezar (5 minutos antes)

1. El servicio **MongoDB** de Windows debe estar encendido (arranca solo con Windows).
2. Doble clic en **`iniciar.cmd`**. Espera a que se abra el navegador en `http://localhost:5173`.
3. Ten abiertos tres programas:
   - **MongoDB Compass**, conectado a `mongodb://localhost:27017`, con la base `finanzas` abierta.
   - **Postman**, con la colección *Finanzas Personales API* y el entorno **Finanzas - local** seleccionado arriba a la derecha.
   - Una terminal en la carpeta `frontend`, para las pruebas automáticas.
4. Cuenta demo: `demo@pulsofinance.local`. La contraseña está en `base-de-datos/05_datos_demo.mjs`.
5. Plan B si algo falla: las capturas de [evidencias/capturas/](evidencias/capturas/) y los resultados de [evidencias/pruebas/](evidencias/pruebas/).

## 1. El problema y la arquitectura (1,5 min)

- **Qué decir:** "Una app de finanzas personales para registrar ingresos y gastos, controlar presupuestos y ver reportes."
- **Mostrar** [IMP-02 Componentes](evidencias/diagramas/IMP-02_Componentes_hexagonal.md): arquitectura hexagonal en tres capas.
  - El dominio no depende de nada.
  - La aplicación define puertos.
  - La infraestructura los implementa con MongoDB, JWT y SMTP.
  - "ArchUnit comprueba esta regla en cada compilación."
- **Mostrar** [IMP-03 Despliegue](evidencias/diagramas/IMP-03_Despliegue.md): React en Vercel, Spring Boot en Render y MongoDB Atlas.

## 2. La aplicación (4 min)

1. **Inicio de sesión** con la cuenta demo. El Resumen saluda por nombre y los totales cuentan hacia arriba (HU-02 y HU-18).
2. **Nuevo movimiento:**
   - Intenta guardar un monto de **0**: la app muestra un error de validación.
   - Registra un gasto de **$ 45.000** en *Alimentación*, con la descripción "Almuerzo demo" (HU-06).
3. **Historial:** busca "almuerzo" y lo encuentra en todo el periodo, no solo en la página visible (HU-16 y HU-17).
4. **Compass:** recarga la colección `movimientos` y enseña el documento nuevo.
   - `monto` está guardado como **Decimal128**, nunca como `double`.
   - Pestaña **Indexes**: `ix_mov_usuario_fecha`.
   - Pestaña **Validation**: las reglas `$jsonSchema`.
5. **Presupuestos:** con los datos demo de noviembre de 2026, *Entretenimiento* aparece en rojo, "Excedido" al 120,6 %. Explica el aviso por correo (HU-14 y HU-15).
6. **Reportes:** dona y barras por categoría (HU-19).
7. **Configuración → Apariencia → Oscuro.** Luego reduce la ventana o usa las herramientas de desarrollo en modo móvil para mostrar la barra inferior.

## 3. La API con Postman (1,5 min)

1. Carpeta **1. Autenticación → Login**: enseña la respuesta `accessToken` y la cookie `refresh_token` (HttpOnly).
2. **"Renovar sin cabecera X-Requested-With → 403"**: "Protección CSRF: otro sitio no puede renovar la sesión con nuestra cookie."
3. Clic derecho en la colección → **Run collection**: 44 peticiones con 82 aserciones en verde.

## 4. Pruebas automáticas (1,5 min)

En la terminal, dentro de `frontend` y con la app encendida:

```bash
set "E2E_URL=http://localhost:5173" && npm run e2e
```

- **Qué decir:** "Edge recorre la vida completa de un usuario: registro, gasto, búsqueda, presupuesto, modo oscuro, móvil, sesión y borrado de cuenta. Son 12 pruebas."
- Menciona el resto de pruebas:
  - backend: 31 pruebas (dominio, servicios, ArchUnit e integración con MongoDB embebido);
  - frontend: 17 pruebas unitarias.
- Mostrar la [trazabilidad](evidencias/TRAZABILIDAD_IMPLEMENTACION.md): cada historia de usuario con su pantalla, su endpoint y sus pruebas.

## 5. Cierre (30 s)

- **Decisiones de diseño:**
  - MongoDB, documentada en [ADR-004](architecture/ADR-004-mongodb.md).
  - Token de acceso en memoria y cookie HttpOnly que rota, con 30 s de gracia.
  - Montos en Decimal128.
- **Pendiente honesto:**
  - verificar el sitio publicado;
  - pruebas de navegador para HU-05, 07, 08, 10, 11, 12, 21 y 22, que sí tienen pruebas de API.

## Preguntas probables

| Pregunta | Respuesta corta |
|---|---|
| ¿Por qué MongoDB y no SQL? | Lo pedía el proyecto. Cada agregado es una colección, con referencias por `_id`. La integridad se refuerza con `$jsonSchema`, índices únicos y reglas en el dominio ([ADR-004](architecture/ADR-004-mongodb.md)). |
| ¿Dónde se guarda el token? | El de acceso, en memoria (15 min). El de renovación, en una cookie HttpOnly (7 días) que rota en cada uso; en la base solo se guarda su hash SHA-256. |
| ¿Qué pasa si dos pestañas renuevan a la vez? | El token anterior sirve una vez más durante 30 s ([IMP-05](evidencias/diagramas/IMP-05_SEQ_Renovar_sesion.md)). |
| ¿Cómo evitan XSS y CSRF? | React escapa el contenido y el token de acceso no está en `localStorage`. Las rutas que usan la cookie exigen la cabecera `X-Requested-With`, que otro sitio no puede enviar. |
| ¿Y si no hay servidor de correo? | La alerta no se marca como enviada y sale cuando haya canal. En local, el correo se imprime en la consola de la API. |
| ¿Cómo sé que la arquitectura se respeta? | `ArquitecturaTest` (ArchUnit) falla la compilación si el dominio importa Spring o la infraestructura. |
