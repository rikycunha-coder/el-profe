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
const D = (() => {
  const codigo = bloques.find(b => b.includes('const DXF ='));
  const module = { exports: {} };
  vm.runInNewContext(codigo, { module, Engine: E, Math });
  return module.exports;
})();
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

prueba('pates: medidas a mano y alto automático', () => {
  const conPates = pates => Object.assign({}, losa, { canto: '20', sup: capa(true, 10, 20, 'no', 15), pates: Object.assign({ on: true, d: '12', dens: '1' }, pates) });
  const auto = E.calcular({ cfg: cfg(), elems: [conPates({ pie: '10', alto: '', cab: '25' })] }).pos.find(p => p.forma === 'pate');
  // Hueco: 200 − 2 × 30 − (12 + 12 + 10 + 10) = 96 mm → 90 mm
  assert.equal(auto.h, 90); assert.equal(auto.f, 100); assert.equal(auto.t, 250);
  assert.equal(auto.len, 2 * 100 + 2 * 90 + 250);
  assert.equal(auto.uds, 20);   // 5 × 4 m² × 1 ud/m²
  const fijo = E.calcular({ cfg: cfg(), elems: [conPates({ pie: '10', alto: '12', cab: '25' })] });
  const p2 = fijo.pos.find(p => p.forma === 'pate');
  assert.equal(p2.h, 120); assert.equal(p2.len, 200 + 240 + 250);
  assert.ok(fijo.elems[0].avisos.some(a => a.includes('mayor que el hueco')));
  const viejo = E.calcular({ cfg: cfg(), elems: [conPates({})] }).pos.find(p => p.forma === 'pate');
  assert.equal(viejo.f, 150); assert.equal(viejo.t, 200);   // sin medidas: pie 15 y cabeza 20 cm
  const mal = E.calcular({ cfg: cfg(), elems: [conPates({ pie: 'x' })] });
  assert.ok(mal.elems[0].errores.some(e => e.includes('Pates')));
});

const libre = (pts, huecos = [], extra = {}) => Object.assign({}, losa, { forma: { modo: 'libre', red: '1', pts, huecos } }, extra);
const v = (x, y, f = '') => ({ x: String(x), y: String(y), f: String(f) });

prueba('losa libre rectangular = losa rectangular', () => {
  const rect = E.calcular({ cfg: cfg(), elems: [losa] });
  const lib = E.calcular({ cfg: cfg(), elems: [libre([v(0, 0), v(5, 0), v(5, 4), v(0, 4)])] });
  assert.equal(lib.errores.length, 0);
  assert.equal(lib.elems[0].errores.length, 0, lib.elems[0].errores.join('; '));
  const px = lib.pos.filter(p => p.titulo.includes('(X)')), py = lib.pos.filter(p => p.titulo.includes('(Y)'));
  assert.equal(px.length, 1); assert.equal(px[0].uds, 21); assert.equal(px[0].len, 4940);
  assert.equal(py.length, 1); assert.equal(py[0].uds, 26); assert.equal(py[0].len, 3940);
  assert.ok(Math.abs(lib.tot.kg - rect.tot.kg) < 1e-6);
  assert.ok(Math.abs(lib.elems[0].geo.area - 20) < 1e-9);
});

prueba('hueco: corta las barras que lo cruzan y descuenta área', () => {
  // Hueco de 1 × 1 m centrado en x = 2…3, y = 1…2 en una losa de 5 × 4 m
  const R = E.calcular({ cfg: cfg(), elems: [libre([v(0, 0), v(5, 0), v(5, 4), v(0, 4)], [{ tipo: 'rect', x: '2', y: '1', w: '1', h: '1' }])] });
  const o = R.elems[0];
  assert.equal(o.errores.length, 0, o.errores.join('; '));
  assert.ok(Math.abs(o.geo.area - 19) < 1e-9);
  const px = R.pos.filter(p => p.titulo.includes('(X)'));
  const largos = px.map(p => p.len).sort((a, b) => b - a);
  // Barras a y = 3, 23, …, 203 cm: las de y = 103 a 183 cruzan el hueco (5 barras) y se parten en 1,94 + 1,94 m
  assert.deepEqual([...new Set(largos)], [4940, 1940]);
  assert.equal(px.find(p => p.len === 4940).uds, 16);
  assert.equal(px.find(p => p.len === 1940).uds, 10);
  const total = R.pos.filter(p => p.titulo.includes('(X)')).reduce((a, p) => a + p.uds, 0);
  assert.equal(total, 26);
});

prueba('lado curvo: barras más cortas hacia el borde curvo', () => {
  // Lado derecho curvado hacia fuera 50 cm (flecha +): la losa es más ancha en el centro
  const R = E.calcular({ cfg: cfg(), elems: [libre([v(0, 0), v(5, 0, 50), v(5, 4), v(0, 4)])] });
  const o = R.elems[0];
  assert.equal(o.errores.length, 0, o.errores.join('; '));
  assert.ok(o.geo.area > 20 && o.geo.area < 21.5, 'área ' + o.geo.area);
  const px = R.pos.filter(p => p.titulo.includes('(X)'));
  const maxLen = Math.max(...px.map(p => p.len)), minLen = Math.min(...px.map(p => p.len));
  assert.ok(maxLen > 5300 && maxLen <= 5440, 'máx ' + maxLen);   // en el centro llega a 5,50 − 0,06
  assert.ok(minLen >= 4940 && minLen < maxLen, 'mín ' + minLen);
  // Lado curvado hacia dentro (flecha −): área menor que el rectángulo
  const R2 = E.calcular({ cfg: cfg(), elems: [libre([v(0, 0), v(5, 0, -50), v(5, 4), v(0, 4)])] });
  assert.ok(R2.elems[0].geo.area < 20);
});

prueba('losa libre: redondeo de largos y errores', () => {
  const R = E.calcular({ cfg: cfg(), elems: [libre([v(0, 0), v(5, 0, 50), v(5, 4), v(0, 4)], [], { forma: { modo: 'libre', red: '10', pts: [v(0, 0), v(5, 0, 50), v(5, 4), v(0, 4)], huecos: [] } })] });
  for (const p of R.pos.filter(p => p.capa === 'inf')) assert.equal(p.len % 100, 0, 'múltiplo de 10 cm: ' + p.len);
  const mal = E.calcular({ cfg: cfg(), elems: [libre([v(0, 0), v(5, 0)])] });
  assert.ok(mal.elems[0].errores.some(e => e.includes('3 vértices')));
  const fuera = E.calcular({ cfg: cfg(), elems: [libre([v(0, 0), v(5, 0), v(5, 4), v(0, 4)], [{ tipo: 'circ', x: '6', y: '2', d: '0,5' }])] });
  assert.ok(fuera.elems[0].avisos.some(a => a.includes('fuera del contorno')));
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

/* DXF sintético: marco de la hoja, losa con un lado curvo, hueco rectangular hecho con líneas, círculo y una semicircunferencia suelta. */
const dxf = (unidades, esc = 1) => {
  const L = [];
  const push = (...xs) => L.push(...xs);
  push('0', 'SECTION', '2', 'HEADER');
  if (unidades !== null) push('9', '$INSUNITS', '70', String(unidades));
  push('0', 'ENDSEC', '0', 'SECTION', '2', 'ENTITIES');
  const poly = (capa, pts, cerrada = true) => { push('0', 'LWPOLYLINE', '8', capa, '90', String(pts.length), '70', cerrada ? '1' : '0'); for (const [x, y, b] of pts) { push('10', String(x * esc), '20', String(y * esc)); if (b) push('42', String(b)); } };
  const line = (capa, x1, y1, x2, y2) => push('0', 'LINE', '8', capa, '10', String(x1 * esc), '20', String(y1 * esc), '11', String(x2 * esc), '21', String(y2 * esc));
  poly('MARCO', [[-1, -1], [20, -1], [20, 14], [-1, 14]]);
  poly('LOSA', [[0, 0], [6.4, 0, 0.1], [6.4, 5.1], [0, 5.1]]);
  line('HUECOS', 0.3, 2, 2.9, 2); line('HUECOS', 2.9, 3.1, 0.3, 3.1); line('HUECOS', 2.9, 2, 2.9, 3.1); line('HUECOS', 0.3, 3.1, 0.3, 2);
  push('0', 'CIRCLE', '8', 'HUECOS', '10', String(5.2 * esc), '20', String(1.0 * esc), '40', String(0.2 * esc));
  push('0', 'ARC', '8', 'OTRA', '10', String(10 * esc), '20', String(10 * esc), '40', String(1 * esc), '50', '0', '51', '180');
  line('OTRA', 9, 10, 11, 10);
  push('0', 'LINE', '8', 'SUELTA', '10', '30', '20', '30', '11', '31', '21', '31');
  push('0', 'ENDSEC', '0', 'EOF');
  return L.join('\n') + '\n';
};

prueba('DXF: contornos, arcos, huecos encadenados y unidades', () => {
  const r = D.leer(dxf(6), '');
  assert.equal(r.unidad, 'm');
  assert.equal(r.sueltas, 1);
  assert.equal(r.anillos.length, 5);
  const [marco, losa, hueco, semi, circ] = r.anillos;
  assert.equal(marco.capa, 'MARCO');
  assert.ok(Math.abs(marco.area / 1e6 - 315) < 1e-6);
  assert.equal(losa.capa, 'LOSA');
  assert.ok(losa.curvo);
  // Flecha del lado curvo: bulge 0,1 × cuerda 5,10 m / 2 = 25,5 cm hacia fuera
  const v = losa.verts.find(q => Math.abs(q.f) >= 1);
  assert.ok(Math.abs(v.f - 255) < 0.5, 'flecha ' + v.f);
  assert.ok(losa.area / 1e6 > 32.64 && losa.area / 1e6 < 33.6, 'área losa ' + losa.area / 1e6);
  assert.equal(hueco.capa, 'HUECOS'); assert.equal(hueco.verts.length, 4);
  assert.ok(Math.abs(hueco.area / 1e6 - 2.86) < 1e-6);
  assert.ok(Math.abs(semi.area / 1e6 - Math.PI / 2) < 0.01, 'semicírculo ' + semi.area / 1e6);
  assert.ok(Math.abs(circ.area / 1e6 - Math.PI * 0.04) < 0.002);
  const h = D.huecosDe(r.anillos, losa);
  assert.equal(JSON.stringify(h.map(a => a.capa).sort()), JSON.stringify(['HUECOS', 'HUECOS']));
  assert.equal(D.huecosDe(r.anillos, marco).length, 2); // losa y semicírculo; los huecos de la losa quedan anidados
});

prueba('DXF: unidades deducidas y forzadas', () => {
  const mm = D.leer(dxf(null, 1000), '');
  assert.equal(mm.unidad, 'mm');
  assert.ok(Math.abs(mm.anillos[1].area / 1e6 - D.leer(dxf(6), '').anillos[1].area / 1e6) < 1e-3);
  const cm = D.leer(dxf(null, 100), '');
  assert.equal(cm.unidad, 'cm');
  // Forzar «m» sobre un dibujo en mm multiplica las medidas por 1000
  const forzado = D.leer(dxf(null, 1000), 'm');
  assert.equal(forzado.unidad, 'm');
  assert.ok(Math.abs(forzado.anillos[1].area / 1e12 - mm.anillos[1].area / 1e6) < 0.05);
  assert.throws(() => D.leer('AutoCAD Binary DXF\r\n', ''), /binario/);
});

prueba('DXF: la losa importada se calcula con huecos', () => {
  const r = D.leer(dxf(6), '');
  const losa = r.anillos[1], x0 = losa.xmin, y0 = losa.ymin;
  const m = v => String(Math.round(v) / 1000);
  const pts = losa.verts.map(q => ({ x: m(q.x - x0), y: m(q.y - y0), f: Math.abs(q.f) >= 1 ? String(q.f / 10) : '' }));
  const huecos = D.huecosDe(r.anillos, losa).map(h => ({ tipo: 'poly', pts: h.verts.map(q => ({ x: m(q.x - x0), y: m(q.y - y0), f: Math.abs(q.f) >= 1 ? String(q.f / 10) : '' })) }));
  const R = E.calcular({ cfg: cfg(), elems: [libre(pts, huecos)] });
  const o = R.elems[0];
  assert.equal(o.errores.length, 0, o.errores.join('; '));
  assert.ok(o.geo.area > 29.5 && o.geo.area < 30.8, 'área ' + o.geo.area);
  assert.ok(R.pos.some(p => p.titulo.includes('(X)') && p.len < 3000), 'hay barras partidas por el hueco');
});

if (fallos) { console.log(`\n${fallos} prueba(s) fallida(s)`); process.exit(1); }
console.log('\nTodas las pruebas pasan');
