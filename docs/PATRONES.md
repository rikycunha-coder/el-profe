# Escáner de patrones y figuras (`PriceActionPatterns`)

Indicador que marca en el gráfico **patrones de velas japonesas** y **figuras chartistas**
(tendencias, canales, triángulos, cuñas, dobles techos y suelos), y genera **señales de compra
y venta** a partir de ellas.

Funciona en cualquier símbolo y timeframe: todas las medidas se normalizan con el ATR, así que
los mismos ajustes valen para oro en M5 y para el Dax en H4.

## Lo que detecta

### Patrones de velas — 26 en total

| Grupo | Patrones |
|---|---|
| Giro mayor (3 velas) | Estrella del amanecer, estrella del atardecer, tres soldados blancos, tres cuervos negros |
| Envolventes | Envolvente alcista, envolvente bajista |
| Penetración | Línea penetrante, nube oscura |
| Sombra larga | Martillo, hombre colgado, estrella fugaz, martillo invertido, pin bar alcista/bajista |
| Doji | Doji, libélula, lápida |
| Indecisión | Harami alcista/bajista, vela interior |
| Rechazo doble | Pinzas superiores, pinzas inferiores |
| Continuación | Marubozu alcista/bajista, exterior alcista/bajista |

Cada patrón lleva una **fuerza de 1 a 3**. `InpMinCandleTag` controla cuáles se etiquetan en el
gráfico: con `2` desaparece el ruido de dojis y velas interiores.

Detalle importante: el mismo dibujo de vela se lee distinto según el contexto. Una vela con
sombra inferior larga es **martillo** (giro alcista) si viene de una caída, y **hombre colgado**
(aviso bajista) si viene de una subida. El indicador mira las velas previas antes de nombrarla.

### Figuras chartistas

Se construyen desde los **swings** (máximos y mínimos tipo fractal, `InpSwingDepth` velas a cada
lado). Con los dos últimos swings altos se traza la línea superior y con los dos últimos bajos la
inferior; la relación entre ambas pendientes decide la figura:

| Figura | Cómo se reconoce | Sesgo |
|---|---|---|
| Tendencia alcista / bajista | Máximos y mínimos crecientes (o decrecientes) | Direccional |
| Canal alcista / bajista / lateral | Las dos líneas casi paralelas | Direccional / neutro |
| Triángulo ascendente | Resistencia plana + mínimos crecientes | Alcista |
| Triángulo descendente | Soporte plano + máximos decrecientes | Bajista |
| Triángulo simétrico | Máximos bajando y mínimos subiendo | Neutro |
| Cuña ascendente | Ambas líneas suben, el soporte más rápido | Bajista |
| Cuña descendente | Ambas bajan, la resistencia más rápido | Alcista |
| Doble techo / doble suelo | Dos extremos al mismo nivel con un cuello en medio | De giro |
| Rango | Nada de lo anterior | Neutro |

En triángulos y cuñas se calcula el **vértice**: cuántas velas faltan para que las dos líneas se
crucen. Aparece en el panel y en la etiqueta de la figura. Si el precio llega al vértice sin
romper, la figura ha caducado.

## Las señales

Cuatro tipos, de mayor a menor prioridad:

1. **Cuello** (`SK_NECKLINE`) — el precio cierra al otro lado del cuello de un doble techo o
   doble suelo. Objetivo: la altura de la figura proyectada desde el cuello. Base 65 puntos.
2. **Ruptura** (`SK_BREAKOUT`) — cierre fuera de una de las dos líneas, con cuerpo de vela
   superior a 0,35 × ATR. Objetivo: la altura de la figura en el punto de ruptura. Base 60.
3. **Rebote** (`SK_BOUNCE`) — el precio toca una línea por dentro, la respeta y cierra con un
   patrón de vela a favor. Objetivo: la línea contraria. Base 55.
4. **Vela suelta** (`SK_CANDLE`) — patrón de fuerza 3 alineado con el sesgo de la figura.
   Desactivado por defecto (`InpSignalCandle`). Base 45.

### Puntuación

Sobre la base de cada tipo:

- **+10** si un patrón de vela confirma la dirección, **+5** más si es de fuerza 3.
- **+10** si la señal va a favor del sesgo de la figura, **−10** si va en contra.
- **+5** si el cuerpo de la vela supera 1 ATR.
- **+5** si la relación riesgo/beneficio a TP1 llega a 1,5; **−10** si no llega a 0,8.

Se publican las que superen `InpMinScore` (60 por defecto) **y** `InpMinRR` (1,0). Subir el
score a 75 deja solo las rupturas limpias con vela de confirmación.

## Qué ves en el gráfico

- **Flechas azules y rojas**: señales de compra y venta.
- **Líneas gruesas** roja y verde: resistencia y soporte de la figura vigente, proyectadas hacia
  la derecha.
- **Etiqueta** con el nombre de la figura y las velas que faltan para el vértice.
- **Línea dorada discontinua**: el cuello de un doble techo/suelo.
- **Línea punteada** roja y verde: último máximo y mínimo relevantes (soporte/resistencia horizontal).
- **`v` y `^`**: los swings que el indicador está usando para trazar las líneas.
- **Etiquetas de texto** con el patrón de cada vela (ENVOLV+, MARTILLO, FUGAZ…).
- **Panel** arriba a la izquierda: figura actual, niveles, patrón de la última vela y última señal.
- **Líneas punteadas** SL/TP1/TP2 de la última señal.

Si se llena demasiado: `InpShowCandleTags = false`, `InpShowSwings = false` y
`InpCandleTagBars = 40` dejan el gráfico limpio manteniendo las señales.

## Por qué el histórico es honesto

Casi todos los indicadores de figuras hacen trampa sin querer: recalculan el pasado con los
swings de hoy, y el resultado es un gráfico lleno de flechas perfectas que en vivo nunca
existieron.

Aquí el escáner recibe un parámetro `end_shift` — "haz como si esta fuera la última vela
cerrada" — y **solo mira velas anteriores a esa**. El indicador recorre el histórico vela a
vela llamando a `Build(s)` y `Signal(s)` con cada una. Cuesta más cálculo, pero la flecha que
ves en una vela de hace tres días es la que habrías recibido ese día.

## Limitaciones, dichas claras

- **Los swings llegan con retraso.** Un máximo no se confirma hasta que pasan `InpSwingDepth`
  velas por su derecha. Es inherente al método: no existe forma de saber que un máximo era un
  máximo sin ver lo que vino después. Con `InpSwingDepth = 3` el retraso es de 3 velas.
- **Las figuras se redibujan.** Cuando se confirma un swing nuevo, las líneas se recalculan y la
  figura puede cambiar de nombre. Las **flechas ya emitidas no se mueven**, pero el dibujo sí.
- **Dos puntos hacen una línea, no una verdad.** El indicador usa los dos últimos swings de cada
  lado. Un chartista humano a veces prefiere otros. No hay una única lectura correcta de un
  gráfico, y este código elige una.
- **No valida el volumen** de la ruptura: en Forex y CFDs el volumen del bróker es de ticks, no
  real, y añadía más ruido que señal.
- **Las rupturas fallan mucho.** Los falsos quiebres son la norma, no la excepción. Por eso hay
  filtro de cuerpo mínimo, puntuación y R:R — para descartar las peores, no para eliminarlas.

## Parámetros que más cambian el resultado

| Parámetro | Efecto |
|---|---|
| `InpSwingDepth` | 2 = muchos swings, figuras pequeñas y nerviosas. 5 = pocas figuras, más significativas |
| `InpLookback` | Velas que mira para construir la figura. 300 en M5 ≈ un día; en H1 ≈ dos semanas |
| `InpMinScore` | El filtro que de verdad decide cuántas señales ves |
| `InpMinRR` | Descarta señales con el objetivo demasiado cerca del stop |
| `InpSignalCandle` | Actívalo solo si quieres muchas señales y vas a filtrarlas tú |

## Relación con el otro indicador del repositorio

`GoldSignalsRealtime` busca **continuación de tendencia** con medias, RSI y ADX: pocas señales,
muy direccionales. `PriceActionPatterns` busca **estructura y giro**: rupturas, rebotes y figuras.

Se complementan bien en el mismo gráfico — y cuando los dos coinciden en dirección, la señal
merece bastante más atención que por separado. Pero son dos lecturas independientes: ninguno
valida al otro automáticamente.
