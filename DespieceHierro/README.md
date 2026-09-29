# Despiece de Hierro · app Android

App para hacer el **despiece de armaduras** (planilla de doblado y corte) de **losas, vigas,
muros y pilares** de hormigón armado. Funciona sin conexión y guarda las obras en el teléfono.

## Instalar en el móvil

1. Desde el móvil, abre la página de **Releases** del repositorio y entra en
   **«Despiece de Hierro · última compilación»**.
2. Descarga **`DespieceHierro.apk`** y ábrelo.
3. Si Android lo pide, permite **«Instalar apps desconocidas»** para el navegador o gestor de
   archivos que estés usando.

Requiere Android 8.0 o superior. Las versiones nuevas se instalan encima de la anterior sin
perder las obras.

## Qué hace

- **Obras** con cualquier número de elementos. Cada elemento puede repetirse (p. ej. 12 pilares iguales).
- **Losa**: malla inferior y, opcional, superior en dirección X e Y (Ø, separación y patas).
- **Viga**: armadura inferior, superior y de piel; estribos con zonas de confinamiento en los
  extremos, varios vanos y trabas.
- **Muro**: simple o doble malla, verticales con anclaje, pata y espera para traslapo,
  horizontales con patas y trabas por m².
- **Pilar**: rectangular (estribos y trabas) o circular (zunchos); longitudinales con anclaje,
  pata y espera para traslapo, y armadura de piel (barras intermedias en las caras) con su propio
  diámetro.
- **Barras adicionales** en cualquier elemento: suples, bastones, esquineros o estribos especiales.
- **Planilla** con marca, forma dibujada con sus cotas, Ø, medidas, largo de corte, cantidad y peso.
- **Resumen por diámetro** y **plan de corte** en barras comerciales de 12 m con el desperdicio.
- **Exportar a Excel (.xlsx) y PDF**: botón «Exportar a Excel o PDF» en cada obra (o el icono de
  compartir). Cada formato se puede **guardar en el teléfono** (Descargas, Drive…) o **compartir**
  por WhatsApp, correo, etc.
  - Excel: hojas «Planilla», «Resumen» y «Plan de corte»; cantidades totales, largos y pesos son
    fórmulas, así que se recalculan si cambias un dato.
  - PDF: planilla para imprimir con el dibujo acotado de cada barra, resumen y plan de corte.

## Criterios de cálculo

Cada obra tiene una **norma de referencia** (Ajustes, icono de engranaje). Las obras nuevas
empiezan con ACI 318:

| Concepto | ACI 318 | Eurocódigo 2 |
|---|---|---|
| Pata automática (gancho/patilla a 90°) | 12Ø | 10Ø |
| Ganchos de estribos y trabas (135°) | 6Ø, mín. 7,5 cm | 10Ø, mín. 7,5 cm |
| Traslapo | 52Ø hasta Ø20 · 65Ø desde Ø22 (clase B, fy 420 MPa, f'c 25 MPa) | 60Ø (B500S, C25/30, α6 = 1,5) |
| Material por defecto | Grado 60 (fy 420 MPa) | B500S |

Si cambias a mano alguno de esos valores, la obra pasa a «criterio propio».

| Concepto | Criterio |
|---|---|
| Medidas | Exteriores, redondeadas al cm |
| Doblado | Opcional: descuenta 2Ø por cada doblez a 90° (patas y 3 esquinas de estribos). Apagado = conservador |
| Recubrimiento | Se descuenta en cada extremo y en cada cara |
| Nº de barras | `huecos para no superar la separación + 1` |
| Peso | π/4 · Ø² · 7.850 kg/m³ (Ø8 0,395 · Ø10 0,617 · Ø12 0,888 · Ø16 1,578 kg/m) |
| Estribo | 2·(b − 2r) + 2·(h − 2r) + 2 ganchos |
| Zuncho circular | π·(D − 2r) + 2 ganchos |
| Barra comercial | 6 o 12 m (u otro largo); las barras más largas se cortan en piezas con traslapo |
| Plan de corte | Mejor ajuste decreciente: primero las piezas largas, cada una en la barra que deja menos sobrante |
| Margen de seguridad | Opcional (p. ej. 3–5 %). Solo se suma a la lista de compra y se informa aparte del neto |

Son valores orientativos para acero y hormigón habituales: comprueba anclajes, traslapos y
ganchos con los planos del calculista.

## Informes (Excel y PDF)

1. **Resumen del proyecto**: tipo de estructura, material, norma, barra comercial, desperdicio
   total estimado (% y metros sobrantes) y margen de seguridad.
2. **Tabla de despiece (corte neto)**: elemento, Ø, cantidad, largo unitario y total, peso, forma y
   dobleces.
3. **Plan de corte** por diámetro: cuántas barras comerciales y qué piezas salen de cada una, con su
   sobrante.
4. **Lista de compra consolidada**: barras comerciales y kg por diámetro; con margen, en columnas aparte.

## Estructura

| Carpeta | Contenido |
|---|---|
| `core/` | Motor de cálculo en Kotlin puro: modelo, reglas de despiece, plan de corte, Excel y pruebas |
| `app/` | App Android con Jetpack Compose: pantallas, guardado y exportación a PDF |

## Compilar

Cada cambio en esta carpeta se compila solo en GitHub Actions
(`.github/workflows/despiece-hierro.yml`): pasa las pruebas del motor, genera el APK firmado y lo
publica en la release `despiece-hierro-latest`.

Para compilar en tu ordenador necesitas Android Studio (o el SDK de Android) y JDK 17:

```bash
cd DespieceHierro
./gradlew :core:test            # pruebas del motor de cálculo
./gradlew :app:assembleRelease  # APK en app/build/outputs/apk/release/
```

El APK se firma con `app/despiece.keystore`, incluida en el repositorio para que todas las
compilaciones tengan la misma firma y se puedan instalar encima. Si algún día publicas la app en
Google Play, crea una clave propia y no la subas al repositorio.
