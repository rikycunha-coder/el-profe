# Herramientas de trading para MetaTrader 5

Indicadores y robot que generan **señales de compra y venta en tiempo real**, calculadas dentro
de tu MetaTrader con el feed de precios de tu propio bróker. Disponibles para **MetaTrader 5
(MQL5)** y **MetaTrader 4 (MQL4)**.

- **Señales de oro por tendencia** — medias, RSI, ADX y ATR sobre XAUUSD.
- **Escáner de patrones y figuras** — velas japonesas, canales, triángulos, cuñas y dobles techos.

## Qué es y qué no es

**Qué es:** un sistema de señales que corre en tu terminal, analiza cada vela de XAUUSD en
cuanto cierra y te avisa —ventana, sonido, notificación push al móvil, email, Telegram o CSV—
con la dirección, el precio de entrada, el stop loss y dos objetivos. Opcionalmente, el EA
puede ejecutar esas mismas señales por ti.

**Qué no es:** no es una bola de cristal ni una fuente externa de señales. Nadie te manda los
avisos desde fuera: los calcula tu propio MetaTrader, tick a tick, con reglas que puedes leer,
auditar y cambiar en `MQL5/Include/GoldSignals/SignalEngine.mqh`.

> ⚠️ **Aviso de riesgo.** Esto es software, no asesoramiento financiero. El oro es uno de los
> instrumentos más volátiles del mercado y el apalancamiento puede liquidar una cuenta en
> minutos. Pruébalo primero en **cuenta demo** y en el Probador de Estrategias durante varias
> semanas. Ninguna configuración por defecto garantiza resultados: el rendimiento pasado no
> predice el futuro.

## Contenido

| Archivo | Para qué sirve |
|---|---|
| `MQL5/Indicators/GoldSignalsRealtime.mq5` | Indicador de tendencia: flechas, panel en vivo y alertas al cierre de cada vela |
| `MQL5/Indicators/PriceActionPatterns.mq5` | Escáner de patrones de velas y figuras chartistas con señales |
| `MQL5/Experts/GoldSignalsEA.mq5` | Robot: mismas señales + envío a Telegram/push y ejecución automática opcional |
| `MQL5/Include/GoldSignals/SignalEngine.mqh` | Motor de señales de tendencia |
| `MQL5/Include/GoldSignals/Candles.mqh` | Biblioteca de 26 patrones de velas japonesas |
| `MQL5/Include/GoldSignals/Structures.mqh` | Swings, figuras chartistas y señales de ruptura/rebote |
| `docs/ESTRATEGIA.md` | Cómo decide el sistema, filtros, puntuación y cómo ajustarlo |
| `docs/INSTALACION.md` | Instalación paso a paso, alertas al móvil y Telegram |
| `docs/MOVIL.md` | Cómo recibir y seguir las señales desde el móvil |
| `docs/PATRONES.md` | Qué detecta el escáner de patrones y cómo leerlo |
| `docs/METATRADER4.md` | Versión MT4: instalación y diferencias con MT5 |
| `MQL4/` | Los mismos tres programas portados a MQL4 |
| `tools/instalar.ps1` | Instalador para Windows: copia y compila en MT4 y MT5 |

## Instalación rápida

**Un solo comando en Windows.** Abre PowerShell y pega esto:

```powershell
powershell -ExecutionPolicy Bypass -Command "irm https://raw.githubusercontent.com/rikycunha-coder/el-profe/claude/metatrader-gold-signals-realtime-y46qrf/tools/instalar.ps1 | iex"
```

Descarga los archivos, detecta tus MetaTrader (MT4 y MT5 a la vez) y los compila él solo. No
hace falta abrir MetaEditor.

Instala **solo los indicadores**: dibujan y avisan, no operan. Un indicador de MetaTrader no
puede abrir órdenes ni siquiera queriendo — la plataforma no se lo permite. Si además quieres el
robot, añade `-ConRobot` (ver [`docs/INSTALACION.md`](docs/INSTALACION.md)).

¿Prefieres hacerlo a mano? Sigue leyendo. ¿Usas MetaTrader 4? Ve directo a
[`docs/METATRADER4.md`](docs/METATRADER4.md).

1. En MetaTrader 5: **Archivo → Abrir carpeta de datos**.
2. Copia respetando las carpetas:
   - todo `MQL5/Include/GoldSignals/*.mqh` → `MQL5/Include/GoldSignals/`
   - `MQL5/Indicators/*.mq5` → `MQL5/Indicators/`
   - `MQL5/Experts/GoldSignalsEA.mq5` → `MQL5/Experts/`
3. Abre MetaEditor (F4), selecciona cada `.mq5` y pulsa **Compilar** (F7). Deben salir 0 errores.
4. Vuelve al terminal, abre un gráfico de **XAUUSD en M5 o M15** y arrastra
   `GoldSignalsRealtime` (indicador) o `GoldSignalsEA` (robot) sobre él.

Detalle completo, incluidas las notificaciones al móvil, en [`docs/INSTALACION.md`](docs/INSTALACION.md).

¿Solo quieres las señales en el teléfono? Empieza por [`docs/MOVIL.md`](docs/MOVIL.md): la app
de móvil no ejecuta MQL5, así que el PC o un VPS calcula y el móvil recibe.

## Los dos indicadores

**`GoldSignalsRealtime`** sigue la tendencia: pocas señales, todas a favor de la dirección
dominante. Es el que quieres si operas oro con una idea direccional clara.

**`PriceActionPatterns`** lee la estructura: marca patrones de velas, dibuja canales,
triángulos, cuñas y dobles techos, y avisa cuando el precio los rompe o rebota en ellos.
Detalle completo en [`docs/PATRONES.md`](docs/PATRONES.md).

Puedes usar los dos en el mismo gráfico. Cuando coinciden en dirección, la señal pesa más.

## Cómo funciona la señal de tendencia, en una frase

Opera **a favor de la tendencia de H1** (precio contra su EMA 200), entra cuando las EMAs 21/50
del gráfico operativo se cruzan o el precio hace un retroceso a la EMA 21 y rebota, y solo si
el ADX confirma fuerza, el RSI no está en extremo, la volatilidad (ATR) es razonable, el spread
es aceptable y estamos en horario de Londres/Nueva York.

El stop y los objetivos salen del ATR: SL = 1,5 × ATR, TP1 = 1,5 × ATR, TP2 = 3 × ATR.

Cada señal lleva una **puntuación 0-100**; por defecto solo se publican las de 60 o más.

Las señales se confirman **siempre al cierre de la vela**, nunca sobre la vela en formación:
por eso las flechas **no repintan** (no aparecen y desaparecen a posteriori).

## Uso típico

**Solo señales (recomendado para empezar):** pon el indicador en el gráfico, activa
`InpAlertPush` y recibirás cada señal en el móvil con entrada, SL y TP. Tú decides si la tomas.

**Señales + Telegram:** usa el EA con `InpEnableTrading = false` y rellena `InpTgToken` /
`InpTgChatId`.

**Automático:** EA con `InpEnableTrading = true`. Empieza con `InpRiskPercent = 0.25`, límite
de 2-3 operaciones al día y **siempre en demo** hasta tener al menos un mes de resultados.

## Antes de usarlo con dinero real

1. Pásalo por el **Probador de Estrategias** (EA, modelo "Cada tick basado en tick reales") con
   6-12 meses de histórico de oro.
2. Revisa drawdown máximo, no solo el beneficio.
3. Ajusta horario y filtros al **horario de tu bróker** (`InpSession*` usa la hora del servidor,
   que normalmente no es la tuya).
4. Comprueba el spread típico de oro en tu bróker y pon `InpMaxSpreadPts` acorde.

## Símbolo y timeframe

Pensado para `XAUUSD` (o como lo llame tu bróker: `GOLD`, `XAUUSD.m`, `XAUUSDm`…) en **M5 o
M15**, con filtro de tendencia en H1. Funciona en otros símbolos, pero los valores por defecto
están calibrados para el oro.
