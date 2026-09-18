# Cómo ver las señales desde el móvil

## La verdad primero

**La app de MetaTrader 5 para móvil no ejecuta indicadores ni robots.** iOS y Android no
permiten compilar ni cargar código MQL5: no existe la opción, ni pagando. Así que las flechas
del indicador, el panel de estado y las líneas de SL/TP **no se ven en el móvil**.

Lo que sí llega al móvil es el **resultado** de las señales, que es lo que realmente necesitas:
dirección, precio, stop y objetivos, en el momento en que se producen.

El esquema es siempre el mismo:

```
MetaTrader en PC o VPS  ──(calcula las señales)──►  push / Telegram / email  ──►  tu móvil
```

Alguien tiene que estar ejecutando MetaTrader en un ordenador encendido. El móvil es el
receptor, nunca el motor.

## Opción 1 — Notificaciones push (lo más rápido de montar)

Llegan a la propia app de MetaTrader 5, como una notificación del sistema.

1. **En el móvil:** abre MetaTrader 5 → *Configuración* → *Mensajes*. Ahí aparece tu
   **MetaQuotes ID**, 8 caracteres. Cópialo.
2. **En el PC:** *Herramientas → Opciones → Notificaciones* → marca *Habilitar notificaciones
   push*, pega el MetaQuotes ID y pulsa **Prueba**. Si no llega la prueba, revisa que las
   notificaciones de la app estén permitidas en los ajustes del teléfono.
3. **En el indicador o el EA:** `InpAlertPush = true`.

Recibirás algo así:

```
COMPRA XAUUSD PERIOD_M15 | pullback alcista a EMA | score 70
Entrada 2412.35 | SL 2406.10 | TP1 2418.60 | TP2 2424.85
ATR 4.17  RSI 58.3  ADX 26.4
```

Límite a tener en cuenta: MetaQuotes restringe la frecuencia de push. Con 2-3 señales al día
no es problema.

## Opción 2 — Telegram (la más cómoda de leer)

Solo la ofrece el EA (`GoldSignalsEA`), y no hace falta que opere: déjalo con
`InpEnableTrading = false` y actúa únicamente como emisor de señales.

Ventajas sobre el push: historial completo que puedes revisar después, buscador, y lo lees
desde el móvil, el PC o la tablet sin abrir MetaTrader.

Configuración en [`INSTALACION.md`](INSTALACION.md#6-alertas-por-telegram-solo-el-ea) — resumen:
token de [@BotFather](https://t.me/BotFather), tu chat id, y añadir `https://api.telegram.org`
en *Herramientas → Opciones → Asesores Expertos → Permitir WebRequest*.

## Opción 3 — Las operaciones, si dejas el EA operando

Con `InpEnableTrading = true`, las órdenes las abre el EA en tu cuenta. Como el móvil se conecta
a **la misma cuenta**, en la pestaña *Trade* verás en tiempo real la posición, el SL, el TP y el
beneficio flotante — y puedes cerrarla o moverle el stop desde el teléfono.

Esto no requiere ninguna configuración extra: es tu cuenta, vista desde otro dispositivo.

## El ordenador tiene que estar encendido

Es el punto que más gente pasa por alto. Si apagas el PC, se acaban las señales.

| Opción | Coste aprox. | Notas |
|---|---|---|
| PC propio encendido | 0 € | Válido para probar. Un reinicio de Windows o un corte de luz te deja sin señales |
| **MQL5 VPS** (integrado) | desde ~15 €/mes | Clic derecho sobre la cuenta en el Navegador → *Registrar un servidor virtual*. Migra el EA y los indicadores en un par de clics, latencia baja con el bróker |
| VPS Windows normal | 10-30 €/mes | Control total: instalas MetaTrader tú mismo. Más trabajo, más flexibilidad |

Sobre el MQL5 VPS: las **notificaciones push funcionan** sin problema. Para **Telegram**, añade
la URL a la lista de permitidas *antes* de migrar y comprueba que sigue enviando una vez
migrado; si tu versión lo bloquea, usa push en el VPS o pásate a un VPS Windows normal.

En un VPS, además, `Alert()` (la ventana emergente) no sirve de nada: nadie mira esa pantalla.
Deja `InpAlertPopup = false` y confía en push o Telegram.

## Recrear el contexto visual en el gráfico del móvil

No puedes tener las flechas, pero sí puedes ver **lo mismo que mira el sistema**, porque usa
solo indicadores estándar que el móvil sí incluye.

En la app, abre el gráfico de XAUUSD y toca el icono de indicadores (la `f` arriba):

**En el gráfico principal:**
- Moving Average, periodo **21**, método **Exponential**, aplicar a **Close**
- Moving Average, periodo **50**, método **Exponential**, aplicar a **Close**

**En ventana aparte:**
- RSI, periodo **14**
- ADX (Average Directional Movement Index), periodo **14**
- ATR, periodo **14** (opcional, para ver la volatilidad)

Y para el filtro de tendencia: cambia el gráfico a **H1** y añade una Moving Average de **200**
exponencial. Precio por encima = solo compras; por debajo = solo ventas.

Con eso, cuando te llegue el aviso al móvil puedes abrir el gráfico y comprobar la señal con tus
propios ojos antes de tocar nada.

## Resumen

| Quiero… | Cómo |
|---|---|
| El aviso en el móvil, ya | Push con MetaQuotes ID |
| Historial de señales consultable | Telegram |
| Ver y gestionar las operaciones | EA operando + pestaña *Trade* del móvil |
| Que funcione con el PC apagado | VPS |
| Ver las flechas del indicador | Solo en PC: el móvil no ejecuta MQL5 |
