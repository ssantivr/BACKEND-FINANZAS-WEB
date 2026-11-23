// Finanzas Personales - indexes (same names as the @Indexed/@CompoundIndex annotations of the API).
// Run with: mongosh "<MONGODB_URI>" base-de-datos/02_indices.js

db.usuarios.createIndex({ correo: 1 }, { name: "uq_usuario_correo", unique: true });

db.categorias.createIndex({ idPropietario: 1, tipo: 1, nombreClave: 1 }, { name: "uq_categoria_nombre", unique: true });

db.movimientos.createIndex({ idUsuario: 1, fecha: -1, fechaRegistro: -1 }, { name: "ix_mov_usuario_fecha" });     // history (RNF-06)
// history filtered by category, sorted by date without an in-memory SORT; its prefix also serves budgets
if (db.movimientos.getIndexes().some((i) => i.name === "ix_mov_usuario_categoria")) db.movimientos.dropIndex("ix_mov_usuario_categoria");
db.movimientos.createIndex({ idUsuario: 1, idCategoria: 1, fecha: -1, fechaRegistro: -1 }, { name: "ix_mov_usuario_categoria_fecha" });

db.presupuestos.createIndex({ idUsuario: 1, idCategoria: 1, periodo: 1 }, { name: "uq_presupuesto", unique: true }); // RN-10
db.presupuestos.createIndex({ idUsuario: 1, periodo: 1 }, { name: "ix_presupuesto_periodo" });                      // budgets of a month

db.sesiones.createIndex({ hash: 1 }, { name: "uq_sesion_hash", unique: true });
db.sesiones.createIndex({ idUsuario: 1 }, { name: "ix_sesion_usuario" });
db.sesiones.createIndex({ expiraEn: 1 }, { name: "ttl_sesion", expireAfterSeconds: 0 });

db.tokens_recuperacion.createIndex({ hash: 1 }, { name: "uq_recuperacion_hash", unique: true });
db.tokens_recuperacion.createIndex({ expiraEn: 1 }, { name: "ttl_recuperacion", expireAfterSeconds: 0 });

print("índices creados");
