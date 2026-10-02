# Despiece de Hierro · app Android

App para hacer el **despiece de ferralla** (planilla de corte y doblado, plan de corte y pedido)
de **losas, vigas, muros y pilares** de hormigón armado. Funciona sin conexión y guarda el trabajo en
el teléfono.

La app muestra la página «Despiece de ferralla» (`app/src/main/assets/web/index.html`) con sus
tipografías incluidas, y Android se encarga de guardar o compartir los archivos, copiar el resumen y
el botón «atrás».

## Instalar en el móvil

1. Desde el móvil, abre la página de **Releases** del repositorio y entra en
   **«Despiece de Hierro · última compilación»**.
2. Descarga **`DespieceHierro.apk`** y ábrelo.
3. Si Android lo pide, permite **«Instalar apps desconocidas»** para el navegador o gestor de
   archivos que estés usando.

Requiere Android 8.0 o superior. Las versiones nuevas se instalan encima de la anterior.

## Cómo se usa

Cuatro pestañas:

- **Datos**: ajustes de cálculo y los elementos de la obra. Cada elemento tiene su esquema dibujado
  y sus capas de armado con interruptor:
  - **Losa**: parrillas inferior y superior (X e Y), patillas y pates (caballetes) con sus medidas.
    Puede ser un **rectángulo** o de **forma libre**: vértices en metros, lados curvos (flecha en cm)
    y huecos (rectángulos, círculos o polígonos del plano). Las barras se cortan en los huecos y en
    los bordes curvos, y se agrupan por largos redondeados.
  - **Viga**: armaduras inferior y superior, estribos con zona de apoyo.
  - **Muro**: caras exterior e interior, extremos de las barras y arranques en la zapata.
  - **Pilar**: barras por cara, **piel** (intermedias) con su propio Ø, cercos con cercos
    interiores y trabas, y esperas en la zapata.
  - **Refuerzos** en cualquier elemento y **barras sobre comanda** sueltas.
- **Desde un plano** (tarjeta en Datos):
  - **Importar DXF**: lee los contornos cerrados del dibujo (polilíneas con arcos, líneas, arcos,
    círculos y splines), deja elegir cuál es la losa y qué huecos tiene, y crea la losa de forma libre.
    Sin conexión. Un DWG hay que exportarlo a DXF de texto desde el CAD.
  - **Leer plano con IA**: envía una foto o un PDF del plano a la API de Claude y propone losas con
    contorno, huecos, canto, recubrimiento y parrillas, para revisarlas antes de crearlas. Necesita
    internet y una clave de API (console.anthropic.com → API keys) que se guarda solo en el teléfono;
    cada lectura cuesta unos céntimos. Es una propuesta: hay que comprobarla con el plano.
- **Planilla**: cada posición con su marca, croquis acotado, largo de corte, unidades y peso.
- **Cortes**: plan de corte por diámetro dibujado sobre la barra comercial, con retales útiles.
- **Pedido**: peso de planilla, barras a comprar, aprovechamiento, cuantía y, si se pide, el margen
  de compra aparte. Desde aquí se exporta:
  - **PDF**: planilla, cortes y pedido para imprimir.
  - **Excel (.xlsx)**: hojas «Pedido», «Planilla» y «Plan de corte», con fórmulas.
  - **Copiar resumen** para WhatsApp o correo.

  Al exportar, la app pregunta si **guardar en el teléfono** (Descargas, Drive…) o **compartir**.

## Ajustes de cálculo

| Ajuste | Qué hace |
|---|---|
| Barra comercial | Largo de la barra que se compra (p. ej. 6 o 12 m) |
| Solape | Largo de solape en diámetros; aparte, uno distinto desde Ø22 si hace falta |
| Retal útil desde | Sobrantes a partir de este largo se cuentan como aprovechables |
| Norma | **ACI 318** (solape 52Ø hasta Ø20 y 65Ø desde Ø22, ganchos 6Ø mín. 7,5 cm), **Eurocódigo 2** (solape 60Ø, ganchos 10Ø mín. 7 cm) o **propia** |
| Ganchos de cercos | Largo de cada gancho de cercos, estribos y trabas en diámetros |
| Margen de compra | % opcional que solo se suma a las barras a comprar y se muestra aparte del neto |
| Descontar doblado | Resta 2Ø por cada doblez a 90° al largo de corte (apagado = medidas exteriores) |

Los valores de cada norma son orientativos (fy 420 MPa / f'c 25 MPa para ACI; B500S y C25/30 para
Eurocódigo). Comprueba solapes, patillas y anclajes con el proyecto de estructura.

## Estructura

| Ruta | Contenido |
|---|---|
| `app/src/main/assets/web/index.html` | La página: motor de cálculo, interfaz, PDF y Excel |
| `app/src/main/assets/web/fuentes/` | Barlow, Barlow Condensed e IBM Plex Mono (SIL OFL) |
| `app/src/main/java/.../MainActivity.kt` | WebView sin conexión, selector de archivos y puente para guardar, compartir y copiar |
| `pruebas/web.test.mjs` | Pruebas del motor de cálculo y del Excel con Node |

## Compilar

Cada cambio en esta carpeta se compila solo en GitHub Actions
(`.github/workflows/despiece-hierro.yml`): pasa las pruebas, genera el APK firmado y lo publica en la
release `despiece-hierro-latest`.

En local hace falta Node y, para el APK, Android Studio (o el SDK de Android) con JDK 17:

```bash
cd DespieceHierro
node pruebas/web.test.mjs       # pruebas del cálculo y del Excel
./gradlew :app:assembleRelease  # APK en app/build/outputs/apk/release/
```

La página también se puede abrir directamente en un navegador para probarla.

El APK se firma con `app/despiece.keystore`, incluida en el repositorio para que todas las
compilaciones tengan la misma firma y se puedan instalar encima. Si algún día publicas la app en
Google Play, crea una clave propia y no la subas al repositorio.
