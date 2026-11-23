// Finanzas Personales - the queries the API runs, to try them by hand in mongosh or Atlas.
// Replace USUARIO with a real user id.
const USUARIO = "00000000-0000-0000-0000-000000000000";

// RF-21: history of the month, newest first, page 1 of 20 (uses ix_mov_usuario_fecha)
db.movimientos.find({ idUsuario: USUARIO, fecha: { $gte: "2026-11-01", $lte: "2026-11-30" } })
  .sort({ fecha: -1, fechaRegistro: -1 }).limit(20);

// RF-23/24: totals per category in a period
db.movimientos.aggregate([
  { $match: { idUsuario: USUARIO, fecha: { $gte: "2026-11-01", $lte: "2026-11-30" } } },
  { $group: { _id: "$idCategoria", total: { $sum: "$monto" } } },
  { $lookup: { from: "categorias", localField: "_id", foreignField: "_id", as: "categoria" } },
  { $sort: { total: -1 } }
]);

// RF-19: consumption of one budget
db.movimientos.aggregate([
  { $match: { idUsuario: USUARIO, tipo: "GASTO", idCategoria: "1c8868ac-9b89-4c04-a680-661c131c1165",
              fecha: { $gte: "2026-11-01", $lte: "2026-11-30" } } },
  { $group: { _id: null, total: { $sum: "$monto" } } }
]);
