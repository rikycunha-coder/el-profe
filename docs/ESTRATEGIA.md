# Cómo decide el sistema

Toda la lógica vive en `MQL5/Include/GoldSignals/SignalEngine.mqh`, en el método
`CGoldSignalEngine::Evaluate()`. El indicador y el EA solo la consumen: si cambias una regla
ahí, cambia en los dos a la vez.

## Principio: confirmar al cierre, nunca repintar

Cada vela se evalúa cuando **ya ha cerrado** (`shift >= 1`). La vela en formación no genera
flechas ni órdenes. Esto evita el problema clásico de los indicadores de señales que "aciertan"
en el histórico porque se redibujan: aquí lo que ves en el pasado es exactamente lo que habrías
visto en vivo.

Coste: pierdes los primeros segundos del movimiento. Beneficio: la señal es real.

## Las cuatro capas

### 1. Sesgo de fondo (¿hacia dónde opero?)

- **Filtro de tendencia H1:** el precio debe estar por encima de la EMA 200 de H1 para comprar,
  y por debajo para vender. Es el filtro que evita operar contra la corriente dominante.
- **Alineación de EMAs:** EMA 21 por encima de EMA 50 para comprar (y al revés para vender).

Si ambas condiciones no coinciden, no hay señal posible: el sistema se queda plano.

### 2. Gatillo (¿cuándo entro?)

Dos patrones, ambos a favor del sesgo:

- **Cruce de EMAs** — la EMA 21 cruza la 50 en esa vela. Captura el arranque de un tramo nuevo.
- **Retroceso a la EMA 21** (*pullback*) — el precio toca la EMA 21 y cierra de vuelta a favor,
  con vela de continuación. Suele dar mejor relación riesgo/beneficio, por eso puntúa más alto.

### 3. Filtros de calidad (¿merece la pena?)

Cualquiera de estos bloquea la señal, y el motivo queda registrado en el log:

| Filtro | Qué evita |
|---|---|
| `ADX >= 20` | Rangos laterales, donde los cruces de medias se despedazan |
| `RSI <= 72` para comprar / `>= 28` para vender | Entrar justo en el clímax de un movimiento |
| ATR mínimo / máximo | Sesiones muertas y picos de noticias ingobernables |
| Spread máximo | Momentos en que el bróker ensancha y se come el objetivo |
| Horario (8-12 y 13-19 hora del servidor) | La sesión asiática y el arrastre nocturno |
| Cierre del viernes | Huecos de fin de semana |

### 4. Puntuación (¿cuánta confianza?)

Cada señal parte de 50 puntos y suma:

- +10 si el ADX supera el mínimo, +10 más si lo supera por 10 puntos.
- +10 si el gatillo es *pullback* (continuación) en lugar de cruce puro.
- +10 si el precio está a más de 1 ATR de la EMA de tendencia (tendencia ya desarrollada).
- +10 si el RSI acompaña sin estar en extremo.

Por defecto solo se publican señales con **60 o más** (`InpMinScore`). Subir a 70-80 da muchas
menos señales y, en general, mejor calidad.

## Niveles operativos

Todo se deriva del ATR de la vela de señal, así que se adaptan solos a la volatilidad del oro:

- **SL** = el más lejano entre el mínimo/máximo de las dos últimas velas y `1,5 × ATR`.
- **TP1** = `1,5 × ATR` (relación 1:1 aproximada).
- **TP2** = `3 × ATR` (relación 2:1 aproximada).

El EA abre en TP2 por defecto y, si lo activas, mueve a **break-even** al alcanzar la distancia
de TP1 y luego aplica un **trailing de 1,5 × ATR**.

## Tamaño de posición

`InpRiskPercent` define cuánto arriesgas por operación. El lotaje se calcula desde la distancia
real al stop y el valor del tick del símbolo, no con una cifra fija:

```
lotes = (balance × riesgo%) / (distancia_al_SL / tick_size × tick_value)
```

Es decir: stop más ancho → lote más pequeño. El riesgo en euros es constante. Si el resultado
queda por debajo del lote mínimo del bróker, el EA no abre la operación en vez de arriesgar de más.

## Cómo ajustarlo

Un parámetro cada vez, y validando en el Probador antes de tocar el siguiente:

| Quieres… | Toca esto |
|---|---|
| Menos señales, más fiables | `InpMinScore` 70-80, `InpAdxMin` 25 |
| Más señales | `InpMinScore` 50, `InpUseSession = false` (ojo con el ruido asiático) |
| Stops más amplios en oro volátil | `InpSlAtrMult` 2.0 |
| Operar solo Nueva York | `InpSession1Start = 13`, `InpSession2Start = -1` |
| Cambiar de M5 a M15 | Nada: el ATR y las EMAs se recalculan solos al timeframe |

## Limitaciones conocidas, dichas claras

- **Es un sistema de seguimiento de tendencia.** En lateral pierde, y el oro pasa semanas
  lateral. Los filtros reducen el daño, no lo eliminan.
- **No sabe de noticias.** Un dato de inflación o una comparecencia de la Fed rompe cualquier
  nivel técnico. Si operas datos, desactiva el EA en esos minutos.
- **Depende de tu bróker.** Spread, slippage y hora del servidor cambian los resultados entre
  brókers con el mismo código.
- **El backtest es optimista.** Ticks reales ayudan, pero no reproducen el ensanchamiento de
  spread ni los requotes de los momentos malos.
