// Pruebas del motor de cálculo y del generador de Excel de assets/web/index.html.
// Uso: node pruebas/web.test.mjs
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const html = fs.readFileSync(new URL('../app/src/main/assets/web/index.html', import.meta.url), 'utf8');
const bloques = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)].map(m => m[1]);
const cargar = (marca, nombre) => {
  const codigo = bloques.find(b => b.includes(marca));
  assert.ok(codigo, `no se encontró el bloque ${nombre}`);
  const module = { exports: {} };
  vm.runInNewContext(codigo, { module, TextEncoder, Math, Date });
  return module.exports;
};
const E = cargar('const Engine =', 'Engine');
const X = cargar('const XLSX =', 'XLSX');

let fallos = 0;
const prueba = (nombre, fn) => {
  try { fn(); console.log('ok  ', nombre); } catch (e) { fallos++; console.log('FALLA', nombre, '\n     ', e.message); }
};
const cfg = extra => Object.assign({ lcom: '12', solape: '50', retal: '1', norma: 'propia', solapeg: '', gancho: '10', margen: '0', doblado: false }, extra);
const capa = (on, d, s, pat, patcm) => ({ on, dx: String(d), sx: String(s), dy: String(d), sy: String(s), pat, patcm: String(patcm) });
const losa = { tipo: 'losa', nombre: 'L', n: '1', lx: '5,00', ly: '4,00', canto: '20', rec: '3', inf: capa(true, 12, 20, 'no', 15), sup: capa(false, 10, 20, 'no', 15), pates: { on: false }, refs: [] };
const viga = { tipo: 'viga', nombre: 'V', n: '1', l: '5,00', b: '30', h: '50', rec: '3', inf: { on: true, nb: '3', d: '16', pat: 'fija', patcm: '20' }, sup: { on: false }, est: { on: false }, refs: [] };
const pilar = { tipo: 'pilar', nombre: 'P', n: '1', b: '40', h: '60', alto: '3,00', rec: '3',
  lon: { on: true, nx: '3', ny: '4', d: '20', dp: '16', abajo: 'solape', arriba: 'espera', espcm: '' },
  cer: { on: true, d: '8', s1: '10', zona: '0,60', s2: '15', ini: '5', int: 'auto' }, esp: { on: false }, refs: [] };

prueba('losa: nº de barras y largo de corte', () => {
  const R = E.calcular({ cfg: cfg(), elems: [losa] });
  assert.equal(R.errores.length, 0, R.errores.join('; '));
  const x = R.pos.find(p => p.titulo.includes('(X)'));
  assert.equal(x.uds, 21);          // (4000 − 60) / 200 → 19,7 → 20 huecos + 1
  assert.equal(x.len, 4940);        // 5000 − 2 × 30
  assert.equal(x.forma, 'recta');
});

prueba('pilar: esquinas y piel con su propio Ø y su espera', () => {
  const R = E.calcular({ cfg: cfg(), elems: [pilar] });
  const esq = R.pos.find(p => p.titulo.startsWith('Longitudinales'));
  const piel = R.pos.find(p => p.titulo.startsWith('Piel'));
  assert.equal(esq.d, 20); assert.equal(esq.uds, 4); assert.equal(esq.len, 4000);   // 3000 + solape 50 × 20
  assert.equal(piel.d, 16); assert.equal(piel.uds, 6); assert.equal(piel.len, 3800); // 3000 + solape 50 × 16
});

prueba('pilar sin piel distinta: un solo grupo longitudinal', () => {
  const R = E.calcular({ cfg: cfg(), elems: [Object.assign({}, pilar, { lon: Object.assign({}, pilar.lon, { dp: '' }) })] });
  assert.equal(R.pos.filter(p => p.capa === 'lon').length, 1);
  assert.equal(R.pos.find(p => p.capa === 'lon').uds, 10);
});

prueba('ACI 318: solape por diámetro y ganchos con mínimo', () => {
  const R = E.calcular({ cfg: cfg({ norma: 'aci', solape: '52', solapeg: '65', gancho: '6' }), elems: [losa] });
  assert.equal(E.solapeMM(12, R.cfg), 630);   // 52 × 12 = 624 → 630
  assert.equal(E.solapeMM(25, R.cfg), 1630);  // 65 × 25 = 1625 → 1630
  assert.equal(E.ganchoMM(8, R.cfg), 75);     // 6 × 8 = 48 → mínimo 75 mm
  assert.equal(E.ganchoMM(16, R.cfg), 96);
});

prueba('descuento por doblado: 2Ø por doblez a 90°', () => {
  const sin = E.calcular({ cfg: cfg(), elems: [viga] }).pos[0];
  const con = E.calcular({ cfg: cfg({ doblado: true }), elems: [viga] }).pos[0];
  assert.equal(sin.len, 5340);              // 4940 + 2 patillas de 200
  assert.equal(con.len, 5340 - 2 * 32);     // U de Ø16: 2 dobleces × 32 mm
  assert.equal(con.desc, 64);
});

prueba('margen de compra aparte del neto', () => {
  const sin = E.calcular({ cfg: cfg(), elems: [losa, viga, pilar] });
  const con = E.calcular({ cfg: cfg({ margen: '5' }), elems: [losa, viga, pilar] });
  assert.equal(con.tot.kg, sin.tot.kg);
  assert.equal(con.tot.barras, sin.tot.barras);
  for (const e of con.porD) assert.equal(e.barrasM, Math.ceil(e.barras * 1.05 - 1e-9));
  assert.ok(con.tot.brutoKgM > con.tot.brutoKg);
});

prueba('ajustes fuera de rango dan error', () => {
  assert.ok(E.calcular({ cfg: cfg({ gancho: '1' }), elems: [losa] }).errores.length > 0);
  assert.ok(E.calcular({ cfg: cfg({ margen: '80' }), elems: [losa] }).errores.length > 0);
});

prueba('Excel: zip válido con las partes y CRC correctos', () => {
  const libro = new X.Libro();
  const h = libro.hoja('Prueba');
  h.fila(X.t('Título', 'TITULO'));
  h.fila(X.n(1.5), X.i(2), X.f('A2*B2', 3));
  const b = libro.bytes();
  const dv = new DataView(b.buffer, b.byteOffset, b.byteLength);
  let p = 0; const partes = {};
  while (dv.getUint32(p, true) === 0x04034b50) {
    const crc = dv.getUint32(p + 14, true), tam = dv.getUint32(p + 18, true), ln = dv.getUint16(p + 26, true);
    const nombre = new TextDecoder().decode(b.subarray(p + 30, p + 30 + ln));
    const datos = b.subarray(p + 30 + ln, p + 30 + ln + tam);
    assert.equal(X.crc32(datos), crc, `CRC de ${nombre}`);
    partes[nombre] = new TextDecoder().decode(datos);
    p += 30 + ln + tam;
  }
  for (const n of ['[Content_Types].xml', 'xl/workbook.xml', 'xl/styles.xml', 'xl/sharedStrings.xml', 'xl/worksheets/sheet1.xml']) assert.ok(n in partes, n);
  assert.ok(partes['xl/worksheets/sheet1.xml'].includes('<f>A2*B2</f><v>3</v>'));
  assert.ok(partes['xl/sharedStrings.xml'].includes('Título'));
});

if (fallos) { console.log(`\n${fallos} prueba(s) fallida(s)`); process.exit(1); }
console.log('\nTodas las pruebas pasan');
