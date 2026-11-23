// Finanzas Personales - predefined categories (RF-14). Same ids the API seeds on start-up
// (InicializadorCategorias), so running both is safe.
// Run with: mongosh "<MONGODB_URI>" base-de-datos/03_categorias_predeterminadas.js

const categorias = [
  { _id: "1c8868ac-9b89-4c04-a680-661c131c1165", nombre: "Alimentación", nombreClave: "alimentación", tipo: "GASTO", icono: "cubiertos", color: "#14b8a6" },
  { _id: "1792c240-47cf-45c2-843f-09ca29df786b", nombre: "Transporte", nombreClave: "transporte", tipo: "GASTO", icono: "bus", color: "#f59e0b" },
  { _id: "f9097551-ede3-492e-bfcd-98cdbbd1b176", nombre: "Vivienda", nombreClave: "vivienda", tipo: "GASTO", icono: "casa", color: "#0f766e" },
  { _id: "1a529716-8f75-4d3d-9903-a13bb5e41f62", nombre: "Servicios públicos", nombreClave: "servicios públicos", tipo: "GASTO", icono: "bombilla", color: "#0ea5e9" },
  { _id: "13a77e6f-b534-438a-9e69-e7b8164e5bb7", nombre: "Salud", nombreClave: "salud", tipo: "GASTO", icono: "corazon", color: "#6366f1" },
  { _id: "020efdd0-59c1-45c9-8d3e-9a5cc7578f5b", nombre: "Educación", nombreClave: "educación", tipo: "GASTO", icono: "libro", color: "#3b82f6" },
  { _id: "fd75338b-1b48-4b24-aa73-e0db9f6f1304", nombre: "Entretenimiento", nombreClave: "entretenimiento", tipo: "GASTO", icono: "estrella", color: "#e11d48" },
  { _id: "b0f172cd-4eb8-4e87-a6da-e475fea185cb", nombre: "Ropa", nombreClave: "ropa", tipo: "GASTO", icono: "camisa", color: "#a855f7" },
  { _id: "1e77b3ed-ce26-4b46-860a-19f3f7de417c", nombre: "Otros gastos", nombreClave: "otros gastos", tipo: "GASTO", icono: "puntos", color: "#64748b" },
  { _id: "7b49081f-0a70-49bb-8ba4-0d3391bbe5e2", nombre: "Salario", nombreClave: "salario", tipo: "INGRESO", icono: "maletin", color: "#10b981" },
  { _id: "87aefb28-a696-420c-83d3-cc34129d3d1e", nombre: "Honorarios", nombreClave: "honorarios", tipo: "INGRESO", icono: "documento", color: "#22c55e" },
  { _id: "cfbe1122-611f-4ead-b332-244752f4073a", nombre: "Inversiones", nombreClave: "inversiones", tipo: "INGRESO", icono: "grafica", color: "#0d9488" },
  { _id: "6adecdf1-8aaa-460d-8f5b-2ed0165f7624", nombre: "Regalos", nombreClave: "regalos", tipo: "INGRESO", icono: "regalo", color: "#ec4899" },
  { _id: "6a6be415-ca63-494c-b4fc-ed22ba3d2b7b", nombre: "Otros ingresos", nombreClave: "otros ingresos", tipo: "INGRESO", icono: "puntos", color: "#16a34a" }
];

for (const c of categorias) {
  const { color, ...resto } = c;   // color is also set on rows created by older versions
  db.categorias.updateOne({ _id: c._id }, { $setOnInsert: resto, $set: { color } }, { upsert: true });
}
print("categorías predeterminadas: " + db.categorias.countDocuments({ idPropietario: { $exists: false } }));
