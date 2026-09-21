# Versión para MetaTrader 4

Todo el sistema está portado a MQL4. Mismas señales, mismas reglas, misma puntuación: lo que
cambia es la plataforma por debajo.

```
MQL4/Include/GoldSignals/SignalEngine.mqh    motor de señales de tendencia
MQL4/Include/GoldSignals/Candles.mqh         26 patrones de velas
MQL4/Include/GoldSignals/Structures.mqh      swings y figuras chartistas
MQL4/Indicators/GoldSignalsRealtime.mq4      indicador de tendencia
MQL4/Indicators/PriceActionPatterns.mq4      escáner de patrones y figuras
MQL4/Experts/GoldSignalsEA.mq4               robot / emisor de señales
```

## Instalación

1. En MetaTrader 4: **Archivo → Abrir carpeta de datos**.
2. Copia respetando la estructura (fíjate: aquí la carpeta es `MQL4`, no `MQL5`):

```
MQL4\Include\GoldSignals\SignalEngine.mqh
MQL4\Include\GoldSignals\Candles.mqh
MQL4\Include\GoldSignals\Structures.mqh
MQL4\Indicators\GoldSignalsRealtime.mq4
MQL4\Indicators\PriceActionPatterns.mq4
MQL4\Experts\GoldSignalsEA.mq4
```

3. Abre MetaEditor con **F4**, selecciona cada `.mq4` y compila con **F7** (los `.mqh` no se
   compilan sueltos: son bibliotecas).
4. Reinicia MetaTrader o pulsa clic derecho en el Navegador → *Actualizar*.

El resto —gráfico, parámetros, alertas push, Telegram, VPS— funciona igual que en MT5. Sigue
[`INSTALACION.md`](INSTALACION.md) y [`MOVIL.md`](MOVIL.md) a partir del paso 3.

## Qué cambió al portarlo

| Tema | MetaTrader 5 | MetaTrader 4 |
|---|---|---|
| Valores de indicadores | Handle + `CopyBuffer` | Llamada directa `iMA/iRSI/iATR/iADX` con el índice de vela |
| Series de precios | `CopyOpen/High/Low/Close` | Igual (MT4 build 600+ las tiene) |
| Búferes del indicador | `SetIndexBuffer` + `PlotIndexSetInteger` | `SetIndexBuffer` + `SetIndexStyle`/`SetIndexArrow` |
| Órdenes | `CTrade`, posición neta | `OrderSend`/`OrderModify` con ticket por orden |
| Información del símbolo | `SymbolInfoDouble` | `MarketInfo` |
| Agrupar parámetros | `input group` | No existe: son comentarios `//---` |
| Nombre del timeframe | `EnumToString` | Función `TfName()` propia |

## Tres detalles que importan en la práctica

**1. Se recalcula una vez por vela, no en cada tick.** En MT4 cada valor de indicador es una
llamada a función, no una lectura de búfer. Pedir 1000 velas × 5 indicadores en cada tick
congelaría el gráfico, así que el trabajo pesado se hace al abrir una vela nueva. Las señales
salen igual de rápido: se confirman justo al cierre de la vela.

**2. Brókers ECN y el error 130.** Muchos brókers de MT4 rechazan una orden que ya lleva SL y TP
puestos. El EA lo detecta: si `OrderSend` devuelve el error 130, reabre la orden limpia y coloca
los niveles inmediatamente después con `OrderModify`. No tienes que configurar nada.

**3. El trailing respeta la zona prohibida.** Antes de mover un stop, el EA comprueba el
`STOPLEVEL` del bróker. Sin esa comprobación, MT4 devuelve error 130 una y otra vez en cada tick
y llena el log.

## Probador de estrategias de MT4

Funciona, con una advertencia: el probador de MT4 es bastante peor que el de MT5. Modela ticks
interpolando dentro de cada barra de M1 y no reproduce el spread variable.

- Modelo: **Todos los ticks**, y mira que la *calidad de modelado* salga por encima del 90 %.
- Para eso necesitas historial de M1 descargado (Herramientas → Centro de historiales).
- Un resultado bueno en el probador de MT4 es una pista, no una prueba. Confirma en demo.

El indicador `PriceActionPatterns` no se puede pasar por el probador como tal (es indicador, no
EA): pruébalo visualmente en demo o en el modo visual con un EA cargado.

## ¿Cuál uso, MT4 o MT5?

Si puedes elegir, **MT5**: el probador es mucho mejor, el cálculo de indicadores es más rápido y
la gestión de posiciones es más simple. La versión MT4 está para quien ya tiene ahí su cuenta o
su bróker no ofrece MT5.

Las señales son las mismas en ambas. Si ves diferencias entre una plataforma y otra con el mismo
símbolo, mira primero el **historial descargado** y el **horario del servidor**: suelen ser dos
brókers distintos, no dos códigos distintos.
