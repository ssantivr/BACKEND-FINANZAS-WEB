# ADR-002 — Arquitectura hexagonal (puertos y adaptadores)

- **Estado:** Aceptada
- **Contexto:** En v1.0 las capas dependían hacia abajo (Lógica → Persistencia → "Base de Datos"), con
  una interfaz `IAccesoDatos` indefinida. Las reglas de negocio quedaban atadas a la tecnología de
  almacenamiento y eran difíciles de probar.
- **Decisión:** El dominio y la aplicación definen **puertos** (interfaces); la infraestructura
  (web, JPA, SMTP, BCrypt/JWT) aporta **adaptadores** que los implementan. Dependencias:
  `infraestructura → aplicacion → dominio`, verificadas con ArchUnit.
- **Consecuencias:** (+) dominio verificable con pruebas unitarias sin BD; sustituir PostgreSQL o el proveedor
  de correo no toca el dominio. (−) más interfaces y mapeo entre entidades JPA y de dominio.
