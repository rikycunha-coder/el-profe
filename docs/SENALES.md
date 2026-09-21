# Señales de compra y venta (`SenalesCompraVenta`)

El indicador directo: dice **COMPRA**, **VENTA** o **ESPERAR**, con el precio de entrada, el
stop y el objetivo. Nada más. Si lo que quieres es saber cuándo entrar, este es el tuyo.

## Qué ves

Un panel fijo en la esquina superior izquierda:

```
XAUUSD  M15
COMPRA
Entrada 2412.35
SL 2406.10   TP 2418.60
Fuerza 75/100  -  pullback alcista a EMA + Rebote en el soporte de Canal alcista
```

Cuando no hay entrada clara pone **ESPERAR** y te dice en qué estado está el mercado
(tendencia y figura detectada), para que sepas que está funcionando y no colgado.

Además: flecha azul o roja en la vela de la señal, líneas punteadas de SL y TP, y aviso por
ventana, sonido o móvil en cuanto la vela cierra.

## De dónde sale la señal

Combina los dos análisis del repositorio:

1. **Tendencia** — medias 21/50, tendencia de fondo en H1, RSI y ADX.
2. **Estructura** — figuras chartistas (canales, triángulos, dobles techos) y patrones de velas.

Y aplica tres reglas:

- **Si los dos coinciden** → señal con fuerza extra (+15 puntos). Es la mejor que da el sistema.
  El stop se pone en el más lejano de los dos y el objetivo en el más cercano: menos sustos,
  objetivo realista.
- **Si solo uno detecta algo** → señal con su fuerza normal. Puedes exigir que coincidan los dos
  con `InpCombinar = Avisar solo si los dos coinciden`.
- **Si se contradicen** → no hay señal. Cuando los dos análisis discrepan, lo rentable es
  quedarse fuera.

## Los ajustes

Solo hay uno que importe de verdad:

| `InpSensibilidad` | Qué hace |
|---|---|
| **Baja** | Pocas señales, muy filtradas (ADX 25, fuerza mínima 75, R:R 1,5) |
| **Media** | Equilibrado. El de por defecto |
| **Alta** | Muchas más señales, también más falsas (ADX 15, fuerza 45, incluye patrones de vela sueltos) |

Si estás viendo pocas señales, sube a **Alta** unos días para entender cómo se comporta, y luego
baja a Media o Baja para operar de verdad. Más señales no es mejor: es más ruido.

El resto:

| Ajuste | Por defecto |
|---|---|
| `InpCombinar` | Avisar con cualquiera de los dos análisis |
| `InpSoloSesion` | `false` — si lo pones en `true`, solo avisa en horario de Londres y Nueva York |
| `InpAlertaMovil` | `false` — actívalo y te llega al teléfono ([`MOVIL.md`](MOVIL.md)) |
| `InpVelasHistorial` | 300 velas con flechas hacia atrás |

## Lo que debes saber antes de usarlo

- **La señal se confirma al cierre de la vela**, no antes. En M15 eso significa que puede tardar
  hasta 15 minutos en aparecer desde que empieza el movimiento. Es el precio de que la señal sea
  real y no cambie después.
- **No es una orden.** Es una lectura técnica con su stop y su objetivo. La decisión y el riesgo
  son tuyos.
- **Un indicador no puede operar.** MetaTrader no se lo permite. No tocará tu cuenta.
- **Prueba en demo** hasta que entiendas cuándo acierta y cuándo falla. Ninguna configuración
  gana siempre, y el oro se mueve rápido.
