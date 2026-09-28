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
  pata y espera para traslapo.
- **Barras adicionales** en cualquier elemento: suples, bastones, esquineros o estribos especiales.
- **Planilla** con marca, forma dibujada con sus cotas, Ø, medidas, largo de corte, cantidad y peso.
- **Resumen por diámetro** y **plan de corte** en barras comerciales de 12 m con el desperdicio.
- **Exportar** la planilla a **PDF** (con croquis) o **CSV** para Excel, y compartirla por
  WhatsApp, correo, Drive…

## Criterios de cálculo

| Concepto | Criterio por defecto |
|---|---|
| Medidas | Exteriores, redondeadas al cm; no se descuenta el alargamiento por doblado |
| Recubrimiento | Se descuenta en cada extremo y en cada cara |
| Nº de barras | `huecos para no superar la separación + 1` |
| Peso | π/4 · Ø² · 7.850 kg/m³ (Ø8 0,395 · Ø10 0,617 · Ø12 0,888 · Ø16 1,578 kg/m) |
| Pata automática | 12Ø (el campo de pata vacío = automática; 0 = sin pata) |
| Ganchos de estribos y trabas | 10Ø cada uno, mínimo 7,5 cm |
| Estribo | 2·(b − 2r) + 2·(h − 2r) + 2 ganchos |
| Zuncho circular | π·(D − 2r) + 2 ganchos |
| Traslapo | 50Ø: en esperas de muros y pilares y al cortar barras de más de 12 m |
| Plan de corte | Mejor ajuste decreciente: primero las piezas largas, cada una en la barra que deja menos sobrante |

Todos estos valores se cambian por obra en **Ajustes** (icono de engranaje). Son orientativos:
comprueba anclajes, traslapos y ganchos con la norma que aplique (ACI 318 / NCh 430, Código
Estructural / EHE, CIRSOC 201, NSR-10, E.060…) y con los planos del calculista.

## Estructura

| Carpeta | Contenido |
|---|---|
| `core/` | Motor de cálculo en Kotlin puro: modelo, reglas de despiece, plan de corte, CSV y pruebas |
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
