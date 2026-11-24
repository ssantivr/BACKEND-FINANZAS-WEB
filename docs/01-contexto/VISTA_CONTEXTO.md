# 1. Vista de Contexto

## 1.1 Propósito y alcance

**Sistema:** *Finanzas Personales* — aplicación web para que una persona registre sus ingresos y
gastos, los clasifique por categoría, defina presupuestos mensuales y consulte su historial y un
resumen financiero.

**Dentro del alcance:** gestión de cuenta (registro, sesión, recuperación de contraseña), movimientos
(gastos/ingresos: alta, edición, baja), categorías personalizadas, presupuestos por categoría,
historial filtrable y resumen financiero.

**Fuera del alcance (v2.0):** conexión con bancos / pagos, multi-moneda con conversión, cuentas
compartidas entre usuarios, aplicaciones móviles nativas (la web es *responsive*).

## 1.2 Elementos del contexto

| Elemento | Tipo | Rol / interacción | Protocolo |
|---|---|---|---|
| **Finanzas Personales** | `«system»` (caja negra) | Sistema en diseño. | — |
| **Usuario** | Actor primario (persona) | Gestiona su cuenta, movimientos, categorías, presupuestos y consulta información. | HTTPS (navegador) |
| **Servicio de correo** | `«external system»` | Envía el enlace de recuperación de contraseña y la alerta de presupuesto excedido. | SMTP + STARTTLS (587) |
| **Almacenamiento de respaldos** | `«external system»` | Recibe copias diarias cifradas de la base de datos. | HTTPS (API S3-compatible) |

> La base de datos **no** aparece aquí: es parte interna del sistema (error C1 de v1.0).

## 1.3 Interacciones (una flecha = una responsabilidad)

| Desde → Hacia | Etiqueta del conector | CU de origen |
|---|---|---|
| Usuario → Finanzas Personales | Gestiona cuenta y sesión | CU-01, CU-02, CU-09 |
| Usuario → Finanzas Personales | Registra y mantiene movimientos | CU-03, CU-04 |
| Usuario → Finanzas Personales | Administra categorías y presupuestos | CU-05, CU-06 |
| Usuario → Finanzas Personales | Consulta historial y resumen | CU-07, CU-08 |
| Finanzas Personales → Servicio de correo | Solicita envío de correo | CU-09, CU-10 |
| Finanzas Personales → Almacenamiento de respaldos | Envía respaldo cifrado | (proceso programado) |

## 1.4 Atributos de calidad que condicionan el diseño

| ID | Atributo | Requisito medible |
|---|---|---|
| RNF-01 | Seguridad | Contraseñas con hash BCrypt (coste ≥ 12); sesión con JWT de 15 min + *refresh token* rotativo; todo el tráfico por TLS 1.2+. |
| RNF-02 | Privacidad | Un usuario solo puede leer o modificar sus propios datos (verificado en cada operación del servidor). |
| RNF-03 | Integridad | Montos con precisión decimal exacta (`NUMERIC(14,2)`), nunca punto flotante. |
| RNF-04 | Rendimiento | p95 < 500 ms para consultas de historial de hasta 10 000 movimientos (paginadas, 20 por página). |
| RNF-05 | Disponibilidad | 99,5 % mensual; RPO 24 h, RTO 4 h (respaldo diario). |
| RNF-06 | Mantenibilidad | Dominio sin dependencias de frameworks; reglas de capas verificadas en el *build*. |

## 1.5 Cómo modelarlo en Visual Paradigm

Diagrama recomendado: **Component Diagram** (UML) usado como diagrama de contexto. Es estándar, lo
soportan todas las ediciones de VP y permite reutilizar el componente del sistema en la Vista de
Desarrollo. Si tu edición incluye *C4 Model › System Context Diagram*, puedes usarlo con los mismos
elementos.

1. *Model Explorer* › clic derecho en `1 Contexto` › *Sub Diagrams › New Diagram › Component Diagram* →
   nombre `CTX-01 Contexto del sistema`.
2. **Sistema:** herramienta *Component* → `Finanzas Personales`. *Open Specification › Stereotypes* →
   crea y aplica `«system»`. Agrándalo y dale un color de relleno distinto.
3. **Actor:** desde la paleta *Use Case* (o arrastra *Actor* con la barra de búsqueda de la paleta) →
   `Usuario`. Este mismo actor se reutilizará en `FUN-01`.
4. **Sistemas externos:** dos *Component* con estereotipo `«external system»` (créalo una vez y reúsalo):
   `Servicio de correo`, `Almacenamiento de respaldos`. Colócalos alrededor del sistema.
5. **Interacciones:** herramienta *Dependency* (o *Association* dirigida) desde el iniciador al
   receptor; escribe la etiqueta de la tabla 1.3 en *Name*. No combines varias etiquetas en una
   flecha.
6. **Atributos de calidad:** una sola *Note* anclada al sistema con RNF-01…RNF-06 (o, mejor, créalos como
   *Requirement* en un *Requirement Diagram* y enlázalos con `«satisfy»`).
7. Verifica: *Diagram › Validate* no debe reportar elementos sin relaciones.

```
           ┌────────────┐
 [Usuario] │  «system»  │──Solicita envío de correo──▶ [«external system» Servicio de correo]
     │────▶│  Finanzas  │
 (4 conec- │ Personales │──Envía respaldo cifrado───▶ [«external system» Almacenamiento de respaldos]
  tores)   └────────────┘
```
