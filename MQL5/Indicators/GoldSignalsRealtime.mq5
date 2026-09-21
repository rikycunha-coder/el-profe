//+------------------------------------------------------------------+
//|                                         GoldSignalsRealtime.mq5   |
//|   Indicador de senales de compraventa de ORO (XAUUSD) en vivo.    |
//|   Dibuja flechas en las velas con senal y avisa por popup,        |
//|   sonido, push al movil y/o email en cuanto la vela cierra.       |
//+------------------------------------------------------------------+
#property copyright "el-profe"
#property version   "1.00"
#property description "Senales XAUUSD en tiempo real: tendencia H1 + EMAs + RSI + ADX + ATR."
#property indicator_chart_window
#property indicator_buffers 2
#property indicator_plots   2

#property indicator_label1  "Compra"
#property indicator_type1   DRAW_ARROW
#property indicator_color1  clrDodgerBlue
#property indicator_width1  2

#property indicator_label2  "Venta"
#property indicator_type2   DRAW_ARROW
#property indicator_color2  clrOrangeRed
#property indicator_width2  2

#include <GoldSignals/SignalEngine.mqh>

//--- Tendencia y gatillos
input group                "Tendencia y gatillos"
input int              InpEmaFast        = 21;          // EMA rapida
input int              InpEmaSlow        = 50;          // EMA lenta
input ENUM_TIMEFRAMES  InpTrendTF        = PERIOD_H1;   // Timeframe de tendencia
input int              InpTrendEma       = 200;         // EMA de tendencia
//--- Filtros
input group                "Filtros de calidad"
input int              InpRsiPeriod      = 14;          // Periodo RSI
input double           InpRsiMaxBuy      = 72.0;        // No comprar con RSI por encima de
input double           InpRsiMinSell     = 28.0;        // No vender con RSI por debajo de
input int              InpAdxPeriod      = 14;          // Periodo ADX
input double           InpAdxMin         = 20.0;        // ADX minimo (fuerza de tendencia)
input int              InpAtrPeriod      = 14;          // Periodo ATR
input double           InpMinAtrPoints   = 0.0;         // ATR minimo en puntos (0 = off)
input double           InpMaxAtrPoints   = 0.0;         // ATR maximo en puntos (0 = off)
input double           InpMaxSpreadPts   = 0.0;         // Spread maximo en puntos (0 = off)
input int              InpMinScore       = 60;          // Puntuacion minima (0-100)
//--- Horario
input group                "Horario (hora del servidor)"
input bool             InpUseSession     = true;        // Filtrar por sesion
input int              InpSession1Start  = 8;           // Sesion 1: hora inicio
input int              InpSession1End    = 12;          // Sesion 1: hora fin
input int              InpSession2Start  = 13;          // Sesion 2: hora inicio (-1 = off)
input int              InpSession2End    = 19;          // Sesion 2: hora fin
input bool             InpSkipFriday     = true;        // Evitar cierre del viernes
//--- Gestion
input group                "Niveles sugeridos"
input double           InpSlAtrMult      = 1.5;         // Stop loss = N x ATR
input double           InpTp1AtrMult     = 1.5;         // Objetivo 1 = N x ATR
input double           InpTp2AtrMult     = 3.0;         // Objetivo 2 = N x ATR
//--- Avisos
input group                "Avisos"
input bool             InpAlertPopup     = true;        // Ventana de alerta
input bool             InpAlertSound     = true;        // Sonido
input bool             InpAlertPush      = false;       // Push al movil (MetaQuotes ID)
input bool             InpAlertEmail     = false;       // Email
input bool             InpDrawLevels     = true;        // Dibujar SL/TP de la ultima senal
input bool             InpShowPanel      = true;        // Panel de estado en el grafico
input bool             InpLogCsv         = false;       // Guardar senales en CSV
input int              InpBarsToScan     = 1000;        // Velas de historial a analizar

double            g_buy[];
double            g_sell[];
CGoldSignalEngine g_engine;
datetime          g_last_alert_bar = 0;
int               g_rates_total    = 0;
bool              g_ready          = false;
string            g_last_aviso     = "";
GSResult          g_last_signal;
bool              g_has_signal     = false;
string            g_prefix         = "GSRT_";

//+------------------------------------------------------------------+
void BuildSettings(GSSettings &s)
  {
   GSDefaults(s);
   s.ema_fast          = InpEmaFast;
   s.ema_slow          = InpEmaSlow;
   s.trend_tf          = InpTrendTF;
   s.trend_ema         = InpTrendEma;
   s.rsi_period        = InpRsiPeriod;
   s.rsi_max_buy       = InpRsiMaxBuy;
   s.rsi_min_sell      = InpRsiMinSell;
   s.atr_period        = InpAtrPeriod;
   s.adx_period        = InpAdxPeriod;
   s.adx_min           = InpAdxMin;
   s.min_atr_points    = InpMinAtrPoints;
   s.max_atr_points    = InpMaxAtrPoints;
   s.sl_atr_mult       = InpSlAtrMult;
   s.tp1_atr_mult      = InpTp1AtrMult;
   s.tp2_atr_mult      = InpTp2AtrMult;
   s.max_spread_points = InpMaxSpreadPts;
   s.use_session       = InpUseSession;
   s.session1_start    = InpSession1Start;
   s.session1_end      = InpSession1End;
   s.session2_start    = InpSession2Start;
   s.session2_end      = InpSession2End;
   s.skip_friday_close = InpSkipFriday;
   s.min_score         = InpMinScore;
  }
//+------------------------------------------------------------------+
int OnInit(void)
  {
   SetIndexBuffer(0,g_buy,INDICATOR_DATA);
   SetIndexBuffer(1,g_sell,INDICATOR_DATA);
   ArraySetAsSeries(g_buy,true);
   ArraySetAsSeries(g_sell,true);

   PlotIndexSetInteger(0,PLOT_ARROW,233);
   PlotIndexSetInteger(1,PLOT_ARROW,234);
   PlotIndexSetDouble(0,PLOT_EMPTY_VALUE,EMPTY_VALUE);
   PlotIndexSetDouble(1,PLOT_EMPTY_VALUE,EMPTY_VALUE);
   PlotIndexSetInteger(0,PLOT_ARROW_SHIFT,10);
   PlotIndexSetInteger(1,PLOT_ARROW_SHIFT,-10);

   IndicatorSetString(INDICATOR_SHORTNAME,"Senales Oro");
   IndicatorSetInteger(INDICATOR_DIGITS,_Digits);

   GSSettings cfg;
   BuildSettings(cfg);
   if(!g_engine.Init(_Symbol,(ENUM_TIMEFRAMES)Period(),cfg))
      return(INIT_FAILED);

   EventSetTimer(2);   // reintento por reloj: con el mercado cerrado no hay ticks

   return(INIT_SUCCEEDED);
  }
//+------------------------------------------------------------------+
void OnDeinit(const int reason)
  {
   EventKillTimer();
   g_engine.Deinit();
   ObjectsDeleteAll(0,g_prefix);
   Comment("");
  }
//+------------------------------------------------------------------+
string DirText(const ENUM_GS_DIR d)
  {
   if(d==GS_BUY)  return("COMPRA");
   if(d==GS_SELL) return("VENTA");
   return("-");
  }
//+------------------------------------------------------------------+
void DrawLevels(const GSResult &r)
  {
   ObjectsDeleteAll(0,g_prefix+"lvl_");
   if(!InpDrawLevels || r.dir==GS_NONE)
      return;

   string names[3]  = {"lvl_sl","lvl_tp1","lvl_tp2"};
   double prices[3];          // MQL exige valores constantes al inicializar un
   prices[0] = r.sl;          // array en la declaracion: estos no lo son
   prices[1] = r.tp1;
   prices[2] = r.tp2;
   color  colors[3] = {clrCrimson,clrSeaGreen,clrSeaGreen};
   string labels[3] = {"SL","TP1","TP2"};

   for(int i=0;i<3;i++)
     {
      string name = g_prefix+names[i];
      ObjectCreate(0,name,OBJ_HLINE,0,0,prices[i]);
      ObjectSetDouble(0,name,OBJPROP_PRICE,prices[i]);
      ObjectSetInteger(0,name,OBJPROP_COLOR,colors[i]);
      ObjectSetInteger(0,name,OBJPROP_STYLE,STYLE_DOT);
      ObjectSetInteger(0,name,OBJPROP_WIDTH,1);
      ObjectSetInteger(0,name,OBJPROP_BACK,true);
      ObjectSetInteger(0,name,OBJPROP_SELECTABLE,false);
      ObjectSetString(0,name,OBJPROP_TEXT,labels[i]+" "+DoubleToString(prices[i],_Digits));
     }
  }
//+------------------------------------------------------------------+
void LogCsv(const GSResult &r)
  {
   if(!InpLogCsv)
      return;

   string file = "GoldSignals_"+_Symbol+".csv";
   bool   fresh = !FileIsExist(file);
   int    h = FileOpen(file,FILE_READ|FILE_WRITE|FILE_CSV|FILE_ANSI,';');
   if(h==INVALID_HANDLE)
      return;

   FileSeek(h,0,SEEK_END);
   if(fresh)
      FileWrite(h,"fecha","simbolo","tf","direccion","setup","score","entrada","sl","tp1","tp2","atr","rsi","adx");
   FileWrite(h,
             TimeToString(r.bar_time,TIME_DATE|TIME_MINUTES),
             _Symbol,
             EnumToString((ENUM_TIMEFRAMES)Period()),
             DirText(r.dir),
             r.setup,
             (string)r.score,
             DoubleToString(r.price,_Digits),
             DoubleToString(r.sl,_Digits),
             DoubleToString(r.tp1,_Digits),
             DoubleToString(r.tp2,_Digits),
             DoubleToString(r.atr,_Digits),
             DoubleToString(r.rsi,1),
             DoubleToString(r.adx,1));
   FileClose(h);
  }
//+------------------------------------------------------------------+
void FireAlert(const GSResult &r)
  {
   string head = StringFormat("%s %s  %s  (%s, score %d)",
                              DirText(r.dir),_Symbol,
                              EnumToString((ENUM_TIMEFRAMES)Period()),
                              r.setup,r.score);
   string body = StringFormat("%s\nEntrada %s | SL %s | TP1 %s | TP2 %s\nATR %s  RSI %.1f  ADX %.1f",
                              head,
                              DoubleToString(r.price,_Digits),
                              DoubleToString(r.sl,_Digits),
                              DoubleToString(r.tp1,_Digits),
                              DoubleToString(r.tp2,_Digits),
                              DoubleToString(r.atr,_Digits),
                              r.rsi,r.adx);

   if(InpAlertPopup) Alert(head);
   if(InpAlertSound) PlaySound("alert.wav");
   if(InpAlertPush)  SendNotification(body);
   if(InpAlertEmail) SendMail("Senal "+_Symbol,body);
   Print(body);
  }
//+------------------------------------------------------------------+
void ShowPanel(void)
  {
   if(!InpShowPanel)
      return;

   double bid = SymbolInfoDouble(_Symbol,SYMBOL_BID);
   double ask = SymbolInfoDouble(_Symbol,SYMBOL_ASK);
   long   spr = SymbolInfoInteger(_Symbol,SYMBOL_SPREAD);

   string txt = StringFormat("SENALES ORO - %s %s\n",_Symbol,EnumToString((ENUM_TIMEFRAMES)Period()));
   txt += StringFormat("Bid %s / Ask %s   spread %d pts\n",
                       DoubleToString(bid,_Digits),DoubleToString(ask,_Digits),(int)spr);
   txt += StringFormat("Sesgo %s   ATR %s\n",
                       g_engine.Bias(1),DoubleToString(g_engine.Atr(1),_Digits));

   if(g_has_signal)
     {
      txt += StringFormat("Ultima senal: %s  %s\n",DirText(g_last_signal.dir),
                          TimeToString(g_last_signal.bar_time,TIME_DATE|TIME_MINUTES));
      txt += StringFormat("Entrada %s | SL %s | TP1 %s | TP2 %s | score %d\n",
                          DoubleToString(g_last_signal.price,_Digits),
                          DoubleToString(g_last_signal.sl,_Digits),
                          DoubleToString(g_last_signal.tp1,_Digits),
                          DoubleToString(g_last_signal.tp2,_Digits),
                          g_last_signal.score);
     }
   else
      txt += "Ultima senal: ninguna todavia\n";

   txt += "Las senales se confirman al cierre de cada vela.";
   Comment(txt);
  }
//+------------------------------------------------------------------+
//+------------------------------------------------------------------+
//| Mensaje en pantalla cuando todavia no se puede calcular           |
//+------------------------------------------------------------------+
void Aviso(const string texto)
  {
   Comment("SENALES ORO - "+_Symbol+" "+EnumToString((ENUM_TIMEFRAMES)Period())+"\n"+texto);
   if(texto!=g_last_aviso)
     {
      Print(texto);
      g_last_aviso = texto;
     }
  }
//+------------------------------------------------------------------+
void Procesar(const int rates_total,const int prev_calculated)
  {

   if(rates_total < g_engine.MinBars()+5)
     {
      Aviso(StringFormat("Hacen falta %d velas y el grafico tiene %d.\nPulsa Inicio o arrastra el grafico a la izquierda para descargar mas historial.",
                         g_engine.MinBars()+5,rates_total));
      return;
     }

   int scan = MathMin(InpBarsToScan,rates_total-2);
   if(!g_engine.Refresh(scan+5))
     {
      Aviso("Descargando el historial del simbolo...\nSi tarda, abre Herramientas > Centro de historiales y descarga este simbolo.");
      return;
     }
   g_ready = true;

   int limit;
   if(prev_calculated<=0)
     {
      ArrayInitialize(g_buy,EMPTY_VALUE);
      ArrayInitialize(g_sell,EMPTY_VALUE);
      limit = MathMin(scan,g_engine.Loaded()-3);
     }
   else
      limit = MathMin(MathMax(rates_total-prev_calculated+1,2),g_engine.Loaded()-3);

   GSResult r;
   for(int s=limit; s>=1; s--)
     {
      g_buy[s]  = EMPTY_VALUE;
      g_sell[s] = EMPTY_VALUE;

      if(!g_engine.Evaluate(s,r) || r.dir==GS_NONE)
         continue;

      double pad = 0.6*r.atr;                       // separacion de la flecha a la vela
      if(r.dir==GS_BUY)  g_buy[s]  = g_engine.Low(s)-pad;
      if(r.dir==GS_SELL) g_sell[s] = g_engine.High(s)+pad;
     }

   //--- la vela 1 acaba de cerrar: avisar una sola vez
   if(g_engine.Evaluate(1,r) && r.dir!=GS_NONE && r.bar_time!=g_last_alert_bar)
     {
      g_last_alert_bar = r.bar_time;
      g_last_signal    = r;
      g_has_signal     = true;
      FireAlert(r);
      LogCsv(r);
      DrawLevels(r);
     }

   //--- la vela en formacion nunca lleva flecha
   g_buy[0]  = EMPTY_VALUE;
   g_sell[0] = EMPTY_VALUE;

   ShowPanel();
   return;
  }
//+------------------------------------------------------------------+
int OnCalculate(const int rates_total,
                const int prev_calculated,
                const datetime &time[],
                const double &open[],
                const double &high[],
                const double &low[],
                const double &close[],
                const long &tick_volume[],
                const long &volume[],
                const int &spread[])
  {
   g_rates_total = rates_total;
   Procesar(rates_total,prev_calculated);
   return(rates_total);
  }
//+------------------------------------------------------------------+
//| Con el mercado cerrado no llegan ticks: el reloj reintenta        |
//+------------------------------------------------------------------+
void OnTimer(void)
  {
   if(g_rates_total>0 && !g_ready)
      Procesar(g_rates_total,0);
  }
//+------------------------------------------------------------------+
