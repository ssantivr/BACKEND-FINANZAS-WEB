# ADR-001 — Monolito modular en lugar de microservicios

- **Estado:** Aceptada
- **Contexto:** Un solo equipo pequeño, dominio acotado (movimientos, categorías, presupuestos,
  consultas), volumen bajo y fuerte acoplamiento transaccional entre registrar un gasto y evaluar su
  presupuesto. v1.0 desplegaba cada módulo lógico como artefacto separado sin justificar comunicación
  entre ellos.
- **Decisión:** Un único backend desplegable (`finanzas-api.jar`) dividido en módulos internos con
  interfaces explícitas (`IGestion*`, `IConsultaFinanciera`, `IEvaluadorPresupuesto`).
- **Consecuencias:** (+) transacciones locales, despliegue y operación simples, menor coste.
  (−) escala como un todo. Los límites por interfaz permiten extraer un módulo (p. ej. Notificaciones)
  si en el futuro hiciera falta.
