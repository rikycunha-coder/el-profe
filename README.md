# Señales de oro (XAUUSD) en tiempo real para MetaTrader 5

Indicador + robot (EA) en MQL5 que generan **señales de compra y venta de oro en tiempo real**,
calculadas dentro de tu MetaTrader con el feed de precios de tu propio bróker.

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
| `MQL5/Indicators/GoldSignalsRealtime.mq5` | Indicador: flechas en el gráfico, panel en vivo y alertas al cierre de cada vela |
| `MQL5/Experts/GoldSignalsEA.mq5` | Robot: mismas señales + envío a Telegram/push y ejecución automática opcional |
| `MQL5/Include/GoldSignals/SignalEngine.mqh` | Motor de señales compartido (toda la lógica está aquí) |
| `docs/ESTRATEGIA.md` | Cómo decide el sistema, filtros, puntuación y cómo ajustarlo |
| `docs/INSTALACION.md` | Instalación paso a paso, alertas al móvil y Telegram |
| `docs/MOVIL.md` | Cómo recibir y seguir las señales desde el móvil |

## Instalación rápida

1. En MetaTrader 5: **Archivo → Abrir carpeta de datos**.
2. Copia respetando las carpetas:
   - `MQL5/Include/GoldSignals/SignalEngine.mqh` → `MQL5/Include/GoldSignals/`
   - `MQL5/Indicators/GoldSignalsRealtime.mq5` → `MQL5/Indicators/`
   - `MQL5/Experts/GoldSignalsEA.mq5` → `MQL5/Experts/`
3. Abre MetaEditor (F4), selecciona cada `.mq5` y pulsa **Compilar** (F7). Deben salir 0 errores.
4. Vuelve al terminal, abre un gráfico de **XAUUSD en M5 o M15** y arrastra
   `GoldSignalsRealtime` (indicador) o `GoldSignalsEA` (robot) sobre él.

Detalle completo, incluidas las notificaciones al móvil, en [`docs/INSTALACION.md`](docs/INSTALACION.md).

¿Solo quieres las señales en el teléfono? Empieza por [`docs/MOVIL.md`](docs/MOVIL.md): la app
de móvil no ejecuta MQL5, así que el PC o un VPS calcula y el móvil recibe.

## Cómo funciona la señal, en una frase

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
