// Finanzas Personales - demo data for a LOCAL environment, loaded through the API (so passwords are hashed
// and every business rule applies).
// Local:     node base-de-datos/05_datos_demo.mjs [http://localhost:8080/api/v1]
// Published: node base-de-datos/05_datos_demo.mjs https://<your-domain>/api/v1
//            Optional DEMO_CORREO / DEMO_CLAVE; without DEMO_CLAVE a random password is generated and printed
//            only in your terminal (it is never written to a file).
import { randomBytes } from 'node:crypto';

const API = process.argv[2] ?? 'http://localhost:8080/api/v1';
const local = /localhost|127\.0\.0\.1/.test(API);
const claveGenerada = !local && !process.env.DEMO_CLAVE;

// Local test account (documented in base-de-datos/README.md). A published server never gets this known password.
const DEMO = local
  ? { nombre: 'Usuario Demo', correo: 'demo@pulsofinance.local', clave: 'DemoLocal2026!' }
  : {
      nombre: 'Usuario Demo',
      correo: process.env.DEMO_CORREO ?? 'demo@pulsofinance.app',
      clave: process.env.DEMO_CLAVE ?? `Demo-${randomBytes(9).toString('base64url')}`,
    };

async function llamar(metodo, ruta, cuerpo, token) {
  const r = await fetch(API + ruta, {
    method: metodo,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: cuerpo ? JSON.stringify(cuerpo) : undefined,
  });
  const texto = await r.text();
  const datos = texto ? JSON.parse(texto) : null;
  if (!r.ok) throw Object.assign(new Error(`${metodo} ${ruta} -> ${r.status} ${datos?.detail ?? ''}`), { status: r.status });
  return datos;
}

let sesion;
try {
  sesion = await llamar('POST', '/auth/registro', DEMO);
  console.log('cuenta demo creada');
} catch (e) {
  if (e.status !== 409) throw e;
  if (claveGenerada) throw new Error(`${DEMO.correo} ya existe: entra con su clave o usa otro correo con DEMO_CORREO`);
  sesion = await llamar('POST', '/auth/login', { correo: DEMO.correo, clave: DEMO.clave });
  console.log('cuenta demo existente: se agregan datos');
}
const t = sesion.accessToken;

const cats = await llamar('GET', '/categorias', null, t);
const id = (nombre) => cats.find((c) => c.nombre === nombre)?.id;
async function propia(nombre, tipo, color) {
  return cats.find((c) => c.nombre === nombre)?.id ?? (await llamar('POST', '/categorias', { nombre, tipo, color }, t)).id;
}
const mascotas = await propia('Mascotas', 'GASTO', '#a855f7');
const freelance = await propia('Freelance', 'INGRESO', '#0ea5e9');

const hoy = new Date();
const dia = (d) => {
  const f = new Date(hoy.getFullYear(), hoy.getMonth(), Math.min(d, hoy.getDate()));
  return `${f.getFullYear()}-${String(f.getMonth() + 1).padStart(2, '0')}-${String(f.getDate()).padStart(2, '0')}`;
};
const mesAnterior = (d) => {
  const f = new Date(hoy.getFullYear(), hoy.getMonth() - 1, d);
  return `${f.getFullYear()}-${String(f.getMonth() + 1).padStart(2, '0')}-${String(f.getDate()).padStart(2, '0')}`;
};

const movimientos = [
  ['INGRESO', '4200000', dia(1), 'Salario', 'Salario del mes', { fuente: 'Empresa' }],
  ['INGRESO', '650000', dia(9), freelance, 'Diseño de página web', { fuente: 'Cliente' }],
  ['GASTO', '1200000', dia(2), 'Vivienda', 'Arriendo apartamento', { metodoPago: 'TRANSFERENCIA' }],
  ['GASTO', '186400', dia(3), 'Alimentación', 'Mercado semanal', { metodoPago: 'TARJETA_DEBITO' }],
  ['GASTO', '52300', dia(5), 'Alimentación', 'Almuerzo con el equipo', { metodoPago: 'EFECTIVO' }],
  ['GASTO', '120000', dia(6), 'Transporte', 'Gasolina', { metodoPago: 'EFECTIVO' }],
  ['GASTO', '44900', dia(7), 'Entretenimiento', 'Netflix', { metodoPago: 'TARJETA_CREDITO' }],
  ['GASTO', '210000', dia(10), 'Alimentación', 'Mercado quincenal', { metodoPago: 'TARJETA_DEBITO' }],
  ['GASTO', '136000', dia(12), 'Entretenimiento', 'Cine y cena', { metodoPago: 'TARJETA_CREDITO' }],
  ['GASTO', '98000', dia(13), 'Servicios públicos', 'Energía', { metodoPago: 'TRANSFERENCIA' }],
  ['GASTO', '85000', dia(14), 'Salud', 'Consulta médica', { metodoPago: 'TARJETA_DEBITO' }],
  ['GASTO', '75000', dia(15), mascotas, 'Concentrado para Max', { metodoPago: 'EFECTIVO' }],
  ['GASTO', '64000', dia(16), 'Transporte', 'Recargas transporte', { metodoPago: 'TARJETA_DEBITO' }],
  ['GASTO', '159900', dia(17), 'Ropa', 'Chaqueta', { metodoPago: 'TARJETA_CREDITO' }],
  ['INGRESO', '4200000', mesAnterior(1), 'Salario', 'Salario mes anterior', { fuente: 'Empresa' }],
  ['GASTO', '1200000', mesAnterior(2), 'Vivienda', 'Arriendo mes anterior', { metodoPago: 'TRANSFERENCIA' }],
  ['GASTO', '420000', mesAnterior(8), 'Alimentación', 'Mercado mes anterior', { metodoPago: 'TARJETA_DEBITO' }],
  ['GASTO', '180000', mesAnterior(20), 'Transporte', 'Transporte mes anterior', { metodoPago: 'EFECTIVO' }],
];
for (const [tipo, monto, fecha, cat, descripcion, extra] of movimientos) {
  await llamar('POST', '/movimientos', { tipo, monto, fecha, descripcion, idCategoria: id(cat) ?? cat, ...extra }, t);
}
console.log(`movimientos registrados: ${movimientos.length}`);

const periodo = dia(1).slice(0, 7);
for (const [cat, limite] of [['Alimentación', '600000'], ['Transporte', '300000'], ['Entretenimiento', '150000'], ['Salud', '200000']]) {
  await llamar('PUT', '/presupuestos', { idCategoria: id(cat), periodo, limite }, t);
}
console.log(`presupuestos definidos para ${periodo}: 4`);

console.log(`
Inicia sesión con:
  correo:     ${DEMO.correo}
  contraseña: ${DEMO.clave}`);
if (claveGenerada) console.log('Guárdala ahora: no queda escrita en ningún archivo.');
