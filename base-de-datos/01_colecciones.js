// Finanzas Personales - MongoDB collections with JSON Schema validation.
// Run with: mongosh "<MONGODB_URI>" base-de-datos/01_colecciones.js
// Idempotent: creates the collection or updates its validator (collMod) if it already exists,
// because the API also creates collections on start-up.

function asegurar(nombre, esquema) {
  const existe = db.getCollectionNames().includes(nombre);
  const opciones = { validator: { $jsonSchema: esquema }, validationLevel: "strict", validationAction: "error" };
  if (existe) {
    db.runCommand(Object.assign({ collMod: nombre }, opciones));
  } else {
    db.createCollection(nombre, opciones);
  }
  print("ok: " + nombre);
}

const uuid = { bsonType: "string", pattern: "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$" };
const fechaIso = { bsonType: "string", pattern: "^\\d{4}-\\d{2}-\\d{2}$" };

asegurar("usuarios", {
  bsonType: "object",
  required: ["_id", "correo", "nombre", "hashContrasena", "estado", "intentosFallidos", "fechaRegistro"],
  properties: {
    _id: uuid,
    correo: { bsonType: "string", maxLength: 254 },                     // RN-06, lower case
    nombre: { bsonType: "string", minLength: 2, maxLength: 80 },
    hashContrasena: { bsonType: "string", minLength: 59, maxLength: 60 }, // BCrypt (RN-07)
    estado: { enum: ["ACTIVA", "ELIMINADA"] },
    intentosFallidos: { bsonType: "int", minimum: 0, maximum: 5 },      // RN-08
    bloqueadoHasta: { bsonType: "date" },
    fechaRegistro: { bsonType: "date" },
    eliminadaEn: { bsonType: "date" }                                    // logical delete (D-1)
  }
});

asegurar("categorias", {
  bsonType: "object",
  required: ["_id", "nombre", "nombreClave", "tipo"],
  properties: {
    _id: uuid,
    nombre: { bsonType: "string", minLength: 1, maxLength: 40 },         // RN-09
    nombreClave: { bsonType: "string", minLength: 1, maxLength: 40 },
    tipo: { enum: ["GASTO", "INGRESO"] },
    icono: { bsonType: "string" },
    color: { bsonType: "string", pattern: "^#[0-9a-f]{6}$" },
    idPropietario: uuid                                                  // missing = predefined
  }
});

asegurar("movimientos", {
  bsonType: "object",
  required: ["_id", "idUsuario", "tipo", "monto", "moneda", "fecha", "idCategoria", "fechaRegistro"],
  properties: {
    _id: uuid,
    idUsuario: uuid,
    tipo: { enum: ["GASTO", "INGRESO"] },
    monto: { bsonType: "decimal" },                                      // Decimal128, never double (RNF-05)
    moneda: { bsonType: "string", minLength: 3, maxLength: 3 },
    fecha: fechaIso,
    descripcion: { bsonType: "string", maxLength: 140 },
    idCategoria: uuid,
    metodoPago: { enum: ["EFECTIVO", "TARJETA_DEBITO", "TARJETA_CREDITO", "TRANSFERENCIA", "OTRO"] },
    fuente: { bsonType: "string" },
    fechaRegistro: { bsonType: "date" }
  }
});

asegurar("presupuestos", {
  bsonType: "object",
  required: ["_id", "idUsuario", "idCategoria", "periodo", "limite", "moneda", "notificado"],
  properties: {
    _id: uuid,
    idUsuario: uuid,
    idCategoria: uuid,
    periodo: { bsonType: "string", pattern: "^\\d{4}-\\d{2}$" },
    limite: { bsonType: "decimal" },
    moneda: { bsonType: "string", minLength: 3, maxLength: 3 },
    notificado: { bsonType: "bool" }                                     // RN-12
  }
});

asegurar("sesiones", {
  bsonType: "object",
  required: ["_id", "idUsuario", "hash", "expiraEn"],
  properties: {
    _id: uuid,
    idUsuario: uuid,
    hash: { bsonType: "string", pattern: "^[0-9a-f]{64}$" },             // SHA-256 of the refresh token
    expiraEn: { bsonType: "date" },
    reemplazadaEn: { bsonType: "date" }   // rotated: valid until expiraEn (30 s grace)
  }
});

asegurar("tokens_recuperacion", {
  bsonType: "object",
  required: ["_id", "idUsuario", "hash", "expiraEn", "usado"],
  properties: {
    _id: uuid,
    idUsuario: uuid,
    hash: { bsonType: "string", pattern: "^[0-9a-f]{64}$" },
    expiraEn: { bsonType: "date" },
    usado: { bsonType: "bool" }
  }
});
