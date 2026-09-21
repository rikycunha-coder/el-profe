# Instalación y puesta en marcha

## Opción rápida: instalador automático (Windows)

`tools/instalar.ps1` hace por ti lo que explica el resto de esta página: localiza tus terminales
de MetaTrader, copia los archivos en la carpeta correcta de cada uno y **los compila llamando a
MetaEditor por línea de comandos**, para que no tengas ni que abrir el editor.

Abre PowerShell (tecla Windows → escribe `powershell` → Enter) y pega esta línea:

```powershell
powershell -ExecutionPolicy Bypass -Command "irm https://raw.githubusercontent.com/rikycunha-coder/el-profe/claude/metatrader-gold-signals-realtime-y46qrf/tools/instalar.ps1 | iex"
```

No necesitas descargar nada antes: el propio comando trae los archivos. Detecta MT4 y MT5 a la
vez, incluidas las instalaciones *portable*.

**Instala solo los indicadores.** No se instala el robot salvo que lo pidas: los indicadores
dibujan y avisan, y no pueden operar (MetaTrader no permite a un indicador abrir órdenes).

Si ya tienes el repositorio descargado, puedes ejecutarlo en local con opciones:

```powershell
powershell -ExecutionPolicy Bypass -File tools\instalar.ps1 -ConRobot
```

| Opción | Para qué |
|---|---|
| `-ConRobot` | Instalar también el robot (EA), además de los indicadores |
| `-Origen C:\ruta\al\repositorio` | Usar archivos ya descargados en vez de bajarlos de GitHub |
| `-Plataforma mt4` / `mt5` | Instalar solo en una plataforma |
| `-SinCompilar` | Copiar sin compilar |

Al terminar, reinicia MetaTrader (o clic derecho en el Navegador → *Actualizar*) y arrastra el
indicador al gráfico.

Si el script no encuentra MetaEditor, te lo dice y solo tienes que compilar a mano con F4 y F7.
Si algo falla, sigue los pasos manuales de abajo: funcionan siempre.

---

## 1. Copiar los archivos

En MetaTrader 5: **Archivo → Abrir carpeta de datos**. Se abre el explorador en una ruta tipo
`C:\Users\TU_USUARIO\AppData\Roaming\MetaQuotes\Terminal\<ID>\`.

Copia manteniendo la estructura:

```
MQL5\Include\GoldSignals\SignalEngine.mqh
MQL5\Indicators\GoldSignalsRealtime.mq5
MQL5\Experts\GoldSignalsEA.mq5
```

La carpeta `MQL5\Include\GoldSignals\` no existe: créala.

## 2. Compilar

1. Abre MetaEditor con **F4** (o botón "IDE").
2. En el Navegador de MetaEditor, doble clic en `GoldSignalsRealtime.mq5` → **Compilar (F7)**.
3. Repite con `GoldSignalsEA.mq5`.
4. La pestaña *Errores* debe decir `0 errors, 0 warnings`. Si aparece
   `cannot open "GoldSignals/SignalEngine.mqh"`, el `.mqh` no está en `MQL5\Include\GoldSignals\`.

El `.mqh` no se compila por separado: es una librería que incluyen los otros dos.

## 3. Abrir el gráfico correcto

- Símbolo: el oro de tu bróker (`XAUUSD`, `GOLD`, `XAUUSD.m`, `XAUUSDm`…). Si no aparece en
  *Observación del mercado*, clic derecho → *Símbolos* → búscalo y actívalo.
- Timeframe: **M5** (más señales, más ruido) o **M15** (menos señales, más limpias).

## 4. Poner el indicador

Arrastra `GoldSignalsRealtime` desde el Navegador al gráfico. En la pestaña *Parámetros de
entrada* ajusta lo que necesites y acepta.

Verás:
- **Flechas azules** (compra) y **rojas** (venta) en las velas con señal.
- Un **panel arriba a la izquierda** con bid/ask, spread, sesgo de tendencia, ATR y la última señal.
- **Líneas punteadas** con SL, TP1 y TP2 de la última señal.

## 5. Alertas al móvil (push)

1. Instala **MetaTrader 5** en el móvil y entra en *Configuración → Mensajes* para ver tu
   **MetaQuotes ID**.
2. En el PC: **Herramientas → Opciones → Notificaciones**, marca *Habilitar notificaciones push*
   y pega el MetaQuotes ID. Pulsa *Prueba*.
3. En el indicador o el EA, pon `InpAlertPush = true`.

## 6. Alertas por Telegram (solo el EA)

1. Habla con [@BotFather](https://t.me/BotFather) en Telegram → `/newbot` → te da un **token**.
2. Escribe un mensaje a tu bot y abre
   `https://api.telegram.org/bot<TU_TOKEN>/getUpdates` en el navegador para ver tu **chat id**.
3. En MetaTrader: **Herramientas → Opciones → Asesores Expertos** → marca
   *Permitir WebRequest para las siguientes URL* y añade `https://api.telegram.org`.
4. En el EA, rellena `InpTgToken` y `InpTgChatId`.

Si algo falla, el EA lo dice en la pestaña *Expertos* con el código HTTP.

## 7. Modo robot (opcional)

Arrastra `GoldSignalsEA` al gráfico y en la pestaña *Común* marca *Permitir trading algorítmico*.
Además, el botón **Algo Trading** de la barra superior del terminal debe estar en verde.

Parámetros clave de ejecución:

| Parámetro | Recomendación inicial |
|---|---|
| `InpEnableTrading` | `false` hasta validar en demo |
| `InpRiskPercent` | `0.25` – `0.5` |
| `InpMaxTradesDay` | `2` – `3` |
| `InpMaxSpreadPts` | el spread típico de tu bróker en oro × 2 |
| `InpUseTp2` | `true` (deja correr con trailing) |

## 8. Probar en el Probador de Estrategias

1. **Ver → Probador de estrategias** (Ctrl+R).
2. Experto: `GoldSignalsEA`. Símbolo: tu oro. Periodo: M5 o M15.
3. Modelado: *Cada tick basado en ticks reales*. Rango: 6-12 meses.
4. `InpEnableTrading = true` **dentro del probador** (ahí no hay dinero real).
5. Mira el informe: beneficio neto, **drawdown máximo**, factor de recuperación y número de
   operaciones (menos de 30 operaciones no permite concluir nada).

## Problemas frecuentes

| Síntoma | Causa habitual |
|---|---|
| No aparece ninguna flecha | Histórico insuficiente (baja y sube el timeframe para forzar la carga) o filtros demasiado estrictos |
| "Simbolo no disponible" en el log | El símbolo no está activo en *Observación del mercado* |
| No entra nunca aunque hay flechas | `InpMaxSpreadPts` demasiado bajo, o fuera del horario `InpSession*` |
| Señales fuera de tu horario | `InpSession*` usa la **hora del servidor del bróker**, no la tuya: compárala con el reloj de *Observación del mercado* |
| El EA no opera | Botón *Algo Trading* apagado o `InpEnableTrading = false` |
