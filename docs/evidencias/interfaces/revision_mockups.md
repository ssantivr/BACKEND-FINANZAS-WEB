# Revisión de mockups (F3): usabilidad, responsive y accesibilidad

**Norma:** WCAG 2.1 AA. **Fecha:** 2026-11-09.
**Método:** inspección visual de 5 mockups estáticos PNG (2560×1600) de `docs/08-requisitos/mockups/`. No es una app, así que no hubo pruebas reales.
**Herramienta:** la skill `frontend-design` no está instalada. Se usó en su lugar `design:accessibility-review`.

**Límites (NO VERIFICADO):**
- Contraste exacto: no se midieron píxeles; los ratios son estimados.
- Teclado, foco y lector de pantalla: no se pueden probar en una imagen.
- Los otros 14 mockups no se revisaron.

| Mockup | Pantalla | HU / RF |
|---|---|---|
| HU-01 | Registro | RF-01, RF-02 |
| HU-06 | Modal "Nuevo movimiento" (gasto) | RF-09, RF-11 |
| HU-13 | Modal "Definir presupuesto" | RF-18 |
| HU-16 | Historial con filtros y paginación | RF-21, RF-22 |
| HU-18 | Resumen (KPI, gráfico, presupuestos) | RF-19, RF-23, RF-24 |

## Resumen
Hay 13 hallazgos: 0 críticos, 7 mayores y 6 menores. La primera versión decía 14 y 7 menores por un error de conteo, corregido en F7.

**Puntos fuertes:**
- Etiquetas visibles encima de cada campo.
- Errores con icono y texto específico ("Mínimo 10 caracteres (llevas 6)", "El monto debe ser mayor que 0").
- El estado del presupuesto se muestra con texto (OK / ALERTA / EXCEDIDO), no solo con color.
- Los montos llevan signo (+/−) además de color.
- La paginación muestra el total.
- HU-13 avisa del upsert ("se actualizará el límite"), coherente con RN-10 y con `ON CONFLICT` en `arquitectura.md`.

## Hallazgos

### Perceptible
| # | Hallazgo | Mockup | WCAG | Gravedad | Recomendación |
|---|---|---|---|---|---|
| A-01 | Texto gris claro sobre blanco ("Opcional · máximo 140 caracteres", "Mostrando 1–8 de 8", "▲ 6,2 % vs. septiembre"). Contraste estimado < 4.5:1 (NO VERIFICADO) | HU-06, HU-16, HU-18 | 1.4.3 | 🟡 Mayor | Usar un gris ≥ #5F6B7A sobre blanco y medirlo con una herramienta de contraste |
| A-02 | Insignia "ALERTA" naranja sobre fondo naranja pálido y "91 %" naranja. Contraste probablemente bajo (NO VERIFICADO) | HU-13, HU-18 | 1.4.3 | 🟡 Mayor | Oscurecer el naranja del texto (≈ #9A4A00) |
| A-03 | El gráfico "Ingresos vs. gastos" no tiene eje Y, valores ni alternativa textual. Los meses anteriores salen con baja opacidad | HU-18 | 1.1.1, 1.4.11 | 🟡 Mayor | Añadir valores al pasar el cursor o enfocar, una tabla equivalente o `aria-describedby`, y opacidad ≥ 3:1 |
| A-04 | La barra de fortaleza de contraseña solo usa color (rojo) | HU-01 | 1.4.1 | 🟢 Menor | Añadir una etiqueta textual ("Débil") |
| A-05 | El tipo seleccionado ("Todos") va en rojo, el mismo color que gasto y error. Es semánticamente confuso | HU-16 | 1.4.1 (uso del color) | 🟢 Menor | Usar el color de marca (verde azulado) para el estado activo |

### Operable
| # | Hallazgo | Mockup | WCAG | Gravedad | Recomendación |
|---|---|---|---|---|---|
| O-01 | Las acciones editar y eliminar son solo iconos pequeños (≈ 20 px) y muy juntos | HU-16 | 2.5.5 / 2.5.8 (WCAG 2.2) | 🟡 Mayor | Área táctil ≥ 44×44 y `aria-label` "Editar movimiento Netflix" |
| O-02 | En los modales no hay botón de cerrar (×) visible; el comportamiento de foco y Escape no está especificado | HU-06, HU-13 | 2.1.2, 2.4.3 | 🟡 Mayor | Botón ×, foco atrapado dentro del modal, Escape para cerrar y foco devuelto al disparador |
| O-03 | Foco visible: solo se ve el anillo verde azulado en un campo. No se puede verificar en el resto de controles (NO VERIFICADO) | HU-06, HU-13 | 2.4.7 | 🟢 Menor | Definir un token de foco global de 2 px y contraste ≥ 3:1 |

### Comprensible
| # | Hallazgo | Mockup | WCAG | Gravedad | Recomendación |
|---|---|---|---|---|---|
| U-01 | El selector de periodo de la cabecera ("Octubre 2026") convive con Desde/Hasta del filtro. No queda claro cuál manda (RN-13 / CU-11) | HU-16 | 3.2.4 | 🟡 Mayor | Usar un único control de periodo, o sincronizar ambos y ocultar el de la cabecera en Movimientos |
| U-02 | El campo Monto no muestra moneda ni formato; en presupuesto sí aparece "$ 600.000". Es inconsistente con el supuesto COP | HU-06 | 3.3.2 | 🟢 Menor | Prefijo "$" y sufijo "COP", el mismo formato en todos los campos de dinero |
| U-03 | La regla de contraseña (mínimo 10, RN-07) solo aparece cuando ya hay error | HU-01 | 3.3.2 | 🟢 Menor | Mostrar la regla como ayuda antes de escribir y añadir el botón mostrar/ocultar |
| U-04 | El modal de presupuesto se abre con "Noviembre 2026" mientras la página muestra octubre | HU-13 | 3.2.4 | 🟢 Menor | Usar por defecto el periodo activo de la página |

### Robusto
| # | Hallazgo | Mockup | WCAG | Gravedad | Recomendación |
|---|---|---|---|---|---|
| R-01 | Los selectores personalizados (Categoría con punto de color, Método de pago, Mes) y el control segmentado Gasto/Ingreso no tienen rol ni estado definidos (NO VERIFICADO) | HU-06, HU-13, HU-16 | 4.1.2 | 🟡 Mayor | Usar `select` nativo o el patrón ARIA combobox / radiogroup |

## Responsive
| Tema | Observación |
|---|---|
| Cobertura | Los 5 mockups son solo de escritorio. No hay diseño móvil (390×844): **NO VERIFICADO** |
| Barra lateral | Ocupa ≈ 17 % del ancho y es fija. En móvil hace falta un menú hamburguesa o una barra inferior |
| Tablas | 5–6 columnas (HU-16, HU-18) no caben a 390 px. Propuesta: tarjetas por movimiento o desplazamiento horizontal con la primera columna fija |
| Filtros | En móvil, Desde/Hasta/Tipo/Categoría deberían apilarse o ir en un panel plegable |
| KPI | Las 3 tarjetas en fila deberían pasar a 1 columna |

## Otros
- El pie de la barra lateral dice "TypeScript · React". La documentación no confirma ese stack de cliente; `TRAZABILIDAD.md` solo menciona `finanzas-web.zip` (NO VERIFICADO).
- Los círculos rojos numerados son anotaciones del mockup, no parte de la UI.

## Prioridades
1. **O-02 y R-01:** modales y controles accesibles por teclado y lector de pantalla. Afectan a todos los flujos de alta (RF-09, RF-18).
2. **A-01, A-02, A-03:** contraste y alternativa textual del gráfico, para usuarios con baja visión.
3. **U-01:** un único control de periodo, porque evita resultados inconsistentes en historial y resumen.
4. **Responsive:** diseñar las variantes móviles de HU-16 y HU-18.
