//+------------------------------------------------------------------+
//|                                          PriceActionPatterns.mq5  |
//|   Escaner de accion del precio: patrones de velas, tendencias,    |
//|   canales, triangulos, cunas y dobles techos/suelos, con senales  |
//|   de compra y venta derivadas de esas figuras.                    |
//|                                                                   |
//|   Las flechas del historico se calculan vela a vela, con la       |
//|   informacion disponible en cada momento: lo que ves en el pasado |
//|   es lo que habrias visto en vivo.                                |
//+------------------------------------------------------------------+
#property copyright "el-profe"
#property version   "1.00"
#property strict
#property description "Patrones de velas + figuras chartistas (canales, triangulos, dobles techos) con senales."
#property description "Version para MetaTrader 4."
#property indicator_chart_window
#property indicator_buffers 2

#property indicator_color1  clrDodgerBlue
#property indicator_width1  3

#property indicator_color2  clrOrangeRed
#property indicator_width2  3

#include <GoldSignals/Structures.mqh>

//--- Deteccion
//--- Deteccion
input int     InpSwingDepth       = 3;      // Profundidad del swing (velas a cada lado)
input int     InpLookback         = 300;    // Velas analizadas para la figura
input int     InpAtrPeriod        = 14;     // Periodo ATR (escala de los patrones)
input int     InpMaxSignalBars    = 300;    // Velas de historial con flechas
//--- Que mostrar
//--- Que dibujar
input bool    InpShowStructure    = true;   // Lineas de la figura (tendencia/canal/triangulo)
input bool    InpShowNeckline     = true;   // Cuello de dobles techos/suelos
input bool    InpShowSwings       = true;   // Marcas en maximos y minimos relevantes
input bool    InpShowSR           = true;   // Soportes y resistencias horizontales
input bool    InpShowCandleTags   = true;   // Etiquetas de patrones de velas
input int     InpCandleTagBars    = 120;    // Velas con etiqueta de patron
input int     InpMinCandleTag     = 2;      // Fuerza minima para etiquetar (1-3)
input bool    InpShowPanel        = true;   // Panel de estado
//--- Senales
//--- Senales
input bool    InpSignalBreakout   = true;   // Rupturas de figura
input bool    InpSignalBounce     = true;   // Rebotes en soporte/resistencia
input bool    InpSignalCandle     = false;  // Patrones de vela por si solos
input int     InpMinScore         = 60;     // Puntuacion minima (0-100)
input double  InpMinRR            = 1.0;    // Relacion riesgo/beneficio minima a TP1
//--- Avisos
//--- Avisos
input bool    InpAlertPopup       = true;   // Ventana de alerta
input bool    InpAlertSound       = true;   // Sonido
input bool    InpAlertPush        = false;  // Push al movil
input bool    InpAlertEmail       = false;  // Email
input bool    InpDrawSignalLevels = true;   // Dibujar SL/TP de la ultima senal
//--- Colores
//--- Colores
input color   InpColorRes         = clrTomato;      // Linea superior (resistencia)
input color   InpColorSup         = clrMediumSeaGreen; // Linea inferior (soporte)
input color   InpColorNeck        = clrGold;        // Cuello
input color   InpColorTagBull     = clrDodgerBlue;  // Etiqueta alcista
input color   InpColorTagBear     = clrOrangeRed;   // Etiqueta bajista
input color   InpColorTagNeutral  = clrSilver;      // Etiqueta neutra

double          g_buy[];
double          g_sell[];
CPatternScanner g_scan;
string          g_prefix   = "PAP_";
datetime        g_last_bar = 0;
datetime        g_last_alert_bar = 0;
int             g_rates_total = 0;      // ultimo tamano de la serie
bool            g_ready       = false;  // ya se ha podido calcular al menos una vez
int             g_lookback    = 0;      // lookback realmente usado
string          g_last_aviso  = "";
PatternSignal   g_last_sig;
bool            g_has_sig  = false;

//+------------------------------------------------------------------+
//| Nombre del timeframe (MQL4 no tiene EnumToString para periodos)   |
//+------------------------------------------------------------------+
string TfName(const int tf)
  {
   switch(tf)
     {
      case PERIOD_M1:  return("M1");
      case PERIOD_M5:  return("M5");
      case PERIOD_M15: return("M15");
      case PERIOD_M30: return("M30");
      case PERIOD_H1:  return("H1");
      case PERIOD_H4:  return("H4");
      case PERIOD_D1:  return("D1");
      case PERIOD_W1:  return("W1");
      case PERIOD_MN1: return("MN1");
     }
   return("TF"+IntegerToString(tf));
  }
//+------------------------------------------------------------------+
int OnInit(void)
  {
   IndicatorBuffers(2);
   SetIndexBuffer(0,g_buy);
   SetIndexBuffer(1,g_sell);

   SetIndexStyle(0,DRAW_ARROW,EMPTY,3,clrDodgerBlue);
   SetIndexStyle(1,DRAW_ARROW,EMPTY,3,clrOrangeRed);
   SetIndexArrow(0,233);
   SetIndexArrow(1,234);
   SetIndexEmptyValue(0,EMPTY_VALUE);
   SetIndexEmptyValue(1,EMPTY_VALUE);
   SetIndexLabel(0,"Compra");
   SetIndexLabel(1,"Venta");

   IndicatorShortName("Patrones y figuras");
   IndicatorDigits(_Digits);

   if(!g_scan.Init(_Symbol,(ENUM_TIMEFRAMES)Period(),InpSwingDepth,InpLookback,InpAtrPeriod))
      return(INIT_FAILED);

   g_lookback = InpLookback;
   EventSetTimer(2);   // reintento por reloj: con el mercado cerrado no hay ticks

   return(INIT_SUCCEEDED);
  }
//+------------------------------------------------------------------+
void OnDeinit(const int reason)
  {
   EventKillTimer();
   g_scan.Deinit();
   ObjectsDeleteAll(0,g_prefix,-1,-1);
   Comment("");
   ChartRedraw();
  }
//+------------------------------------------------------------------+
string DirText(const int dir)
  {
   if(dir>0) return("COMPRA");
   if(dir<0) return("VENTA");
   return("-");
  }
//+------------------------------------------------------------------+
string KindText(const ENUM_SIGNAL_KIND k)
  {
   switch(k)
     {
      case SK_BREAKOUT: return("ruptura");
      case SK_BOUNCE:   return("rebote");
      case SK_NECKLINE: return("cuello");
      case SK_CANDLE:   return("vela");
     }
   return("-");
  }
//+------------------------------------------------------------------+
void MakeTrendLine(const string name,const datetime t1,const double p1,
                   const datetime t2,const double p2,const color clr,
                   const int style,const int width)
  {
   if(t1==0 || t2==0 || p1<=0.0 || p2<=0.0)
      return;

   ObjectDelete(0,name);
   if(!ObjectCreate(0,name,OBJ_TREND,0,t1,p1,t2,p2))
      return;

   ObjectSetInteger(0,name,OBJPROP_COLOR,clr);
   ObjectSetInteger(0,name,OBJPROP_STYLE,style);
   ObjectSetInteger(0,name,OBJPROP_WIDTH,width);
   ObjectSetInteger(0,name,OBJPROP_RAY_RIGHT,true);
   ObjectSetInteger(0,name,OBJPROP_BACK,true);
   ObjectSetInteger(0,name,OBJPROP_SELECTABLE,false);
  }
//+------------------------------------------------------------------+
void MakeText(const string name,const datetime t,const double p,const string text,
              const color clr,const int anchor)
  {
   ObjectDelete(0,name);
   if(!ObjectCreate(0,name,OBJ_TEXT,0,t,p))
      return;

   ObjectSetString(0,name,OBJPROP_TEXT,text);
   ObjectSetString(0,name,OBJPROP_FONT,"Arial");
   ObjectSetInteger(0,name,OBJPROP_FONTSIZE,8);
   ObjectSetInteger(0,name,OBJPROP_COLOR,clr);
   ObjectSetInteger(0,name,OBJPROP_ANCHOR,anchor);
   ObjectSetInteger(0,name,OBJPROP_SELECTABLE,false);
  }
//+------------------------------------------------------------------+
void MakeHLine(const string name,const double price,const color clr,const int style)
  {
   ObjectDelete(0,name);
   if(!ObjectCreate(0,name,OBJ_HLINE,0,0,price))
      return;

   ObjectSetInteger(0,name,OBJPROP_COLOR,clr);
   ObjectSetInteger(0,name,OBJPROP_STYLE,style);
   ObjectSetInteger(0,name,OBJPROP_WIDTH,1);
   ObjectSetInteger(0,name,OBJPROP_BACK,true);
   ObjectSetInteger(0,name,OBJPROP_SELECTABLE,false);
  }
//+------------------------------------------------------------------+
//| Dibuja la figura vigente: lineas, cuello, swings y etiqueta       |
//+------------------------------------------------------------------+
void DrawStructure(void)
  {
   ObjectsDeleteAll(0,g_prefix+"st_",-1,-1);
   ObjectsDeleteAll(0,g_prefix+"sw_",-1,-1);
   ObjectsDeleteAll(0,g_prefix+"sr_",-1,-1);

   MarketStructure st = g_scan.Structure();
   if(st.type==ST_NONE)
      return;

   if(InpShowStructure)
     {
      if(st.has_high_line)
         MakeTrendLine(g_prefix+"st_res",st.h_time1,st.h_price1,st.h_time0,st.h_price0,
                       InpColorRes,STYLE_SOLID,2);
      if(st.has_low_line)
         MakeTrendLine(g_prefix+"st_sup",st.l_time1,st.l_price1,st.l_time0,st.l_price0,
                       InpColorSup,STYLE_SOLID,2);

      //--- nombre de la figura junto al swing mas reciente
      datetime t_lab = (st.h_time0>st.l_time0 ? st.h_time0 : st.l_time0);
      double   p_lab = g_scan.High(1)+1.5*g_scan.Atr(1);
      string   lab   = st.name;
      if(st.apex_bars>0)
         lab += StringFormat("  (vertice en ~%d velas)",st.apex_bars);
      MakeText(g_prefix+"st_name",t_lab,p_lab,lab,
               (st.bias>0 ? InpColorTagBull : (st.bias<0 ? InpColorTagBear : InpColorTagNeutral)),
               ANCHOR_LEFT_LOWER);
     }

   if(InpShowNeckline && st.has_neck)
      MakeHLine(g_prefix+"st_neck",st.neckline,InpColorNeck,STYLE_DASH);

   if(InpShowSwings)
     {
      int n = MathMin(g_scan.SwingHighs(),6);
      for(int i=0;i<n;i++)
        {
         SwingPoint p = g_scan.SwingHigh(i);
         MakeText(g_prefix+"sw_h"+(string)i,p.time,p.price,"v",InpColorRes,ANCHOR_LOWER);
        }
      n = MathMin(g_scan.SwingLows(),6);
      for(int i=0;i<n;i++)
        {
         SwingPoint p = g_scan.SwingLow(i);
         MakeText(g_prefix+"sw_l"+(string)i,p.time,p.price,"^",InpColorSup,ANCHOR_UPPER);
        }
     }

   if(InpShowSR)
     {
      if(g_scan.SwingHighs()>0)
         MakeHLine(g_prefix+"sr_r",g_scan.SwingHigh(0).price,InpColorRes,STYLE_DOT);
      if(g_scan.SwingLows()>0)
         MakeHLine(g_prefix+"sr_s",g_scan.SwingLow(0).price,InpColorSup,STYLE_DOT);
     }
  }
//+------------------------------------------------------------------+
//| Etiquetas de patrones de velas en las ultimas barras              |
//+------------------------------------------------------------------+
void DrawCandleTags(void)
  {
   ObjectsDeleteAll(0,g_prefix+"cd_",-1,-1);
   if(!InpShowCandleTags)
      return;

   int bars = MathMin(InpCandleTagBars,g_scan.Loaded()-5);
   CandleHit hit;

   for(int s=1;s<=bars;s++)
     {
      if(!g_scan.CandleAtBar(s,hit))
         continue;
      if(hit.strength<InpMinCandleTag)
         continue;

      double atr = g_scan.Atr(s);
      color  clr = (hit.dir>0 ? InpColorTagBull : (hit.dir<0 ? InpColorTagBear : InpColorTagNeutral));

      if(hit.dir>0)
         MakeText(g_prefix+"cd_"+(string)s,g_scan.Time(s),g_scan.Low(s)-0.5*atr,
                  hit.tag,clr,ANCHOR_UPPER);
      else
         MakeText(g_prefix+"cd_"+(string)s,g_scan.Time(s),g_scan.High(s)+0.5*atr,
                  hit.tag,clr,ANCHOR_LOWER);
     }
  }
//+------------------------------------------------------------------+
void DrawSignalLevels(const PatternSignal &sig)
  {
   ObjectsDeleteAll(0,g_prefix+"lv_",-1,-1);
   if(!InpDrawSignalLevels || sig.dir==0)
      return;

   MakeHLine(g_prefix+"lv_sl", sig.sl, clrCrimson,  STYLE_DOT);
   MakeHLine(g_prefix+"lv_tp1",sig.tp1,clrSeaGreen, STYLE_DOT);
   MakeHLine(g_prefix+"lv_tp2",sig.tp2,clrSeaGreen, STYLE_DOT);
  }
//+------------------------------------------------------------------+
void FireAlert(const PatternSignal &sig)
  {
   string head = StringFormat("%s %s %s | %s | score %d",
                              DirText(sig.dir),_Symbol,
                              TfName(Period()),
                              sig.reason,sig.score);
   string body = head;
   if(sig.candle!="")
      body += "\nVela: "+sig.candle;
   body += StringFormat("\nEntrada %s | SL %s | TP1 %s | TP2 %s | R:R %.2f",
                        DoubleToString(sig.entry,_Digits),
                        DoubleToString(sig.sl,_Digits),
                        DoubleToString(sig.tp1,_Digits),
                        DoubleToString(sig.tp2,_Digits),
                        sig.rr);

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

   MarketStructure st = g_scan.Structure();
   double atr = g_scan.Atr(1);

   string txt = StringFormat("PATRONES Y FIGURAS - %s %s\n",
                             _Symbol,TfName(Period()));
   txt += "Figura: "+st.name;
   if(st.bias>0)      txt += "  (sesgo alcista)";
   else if(st.bias<0) txt += "  (sesgo bajista)";
   if(st.apex_bars>0) txt += StringFormat("  vertice ~%d velas",st.apex_bars);
   txt += "\n";

   if(st.has_high_line && st.has_low_line)
      txt += StringFormat("Resistencia %s | Soporte %s | ATR %s\n",
                          DoubleToString(g_scan.HighLineAt(0),_Digits),
                          DoubleToString(g_scan.LowLineAt(0),_Digits),
                          DoubleToString(atr,_Digits));
   if(st.has_neck)
      txt += "Cuello: "+DoubleToString(st.neckline,_Digits)+"\n";

   CandleHit hit;
   if(g_scan.CandleAtBar(1,hit))
      txt += "Ultima vela: "+hit.name+"\n";
   else
      txt += "Ultima vela: sin patron\n";

   if(g_has_sig)
     {
      txt += StringFormat("Senal: %s (%s) %s | score %d | R:R %.2f\n",
                          DirText(g_last_sig.dir),KindText(g_last_sig.kind),
                          TimeToString(g_last_sig.time,TIME_DATE|TIME_MINUTES),
                          g_last_sig.score,g_last_sig.rr);
      txt += StringFormat("Entrada %s | SL %s | TP1 %s | TP2 %s\n",
                          DoubleToString(g_last_sig.entry,_Digits),
                          DoubleToString(g_last_sig.sl,_Digits),
                          DoubleToString(g_last_sig.tp1,_Digits),
                          DoubleToString(g_last_sig.tp2,_Digits));
     }
   else
      txt += "Senal: ninguna todavia\n";

   txt += "Confirmacion al cierre de vela.";
   Comment(txt);
  }
//+------------------------------------------------------------------+
bool SignalPasses(const PatternSignal &sig)
  {
   if(sig.dir==0)                  return(false);
   if(sig.score<InpMinScore)       return(false);
   if(InpMinRR>0.0 && sig.rr<InpMinRR) return(false);
   return(true);
  }
//+------------------------------------------------------------------+
//+------------------------------------------------------------------+
//| Mensaje en pantalla cuando todavia no se puede calcular.          |
//| Un grafico en blanco no explica nada: siempre hay que decir que   |
//| esta pasando.                                                     |
//+------------------------------------------------------------------+
void Aviso(const string texto)
  {
   Comment("PATRONES Y FIGURAS - "+_Symbol+" "+TfName(Period())+"\n"+texto);
   if(texto!=g_last_aviso)
     {
      Print(texto);
      g_last_aviso = texto;
     }
  }
//+------------------------------------------------------------------+
//| Todo el trabajo, llamado tanto desde OnCalculate como del reloj   |
//+------------------------------------------------------------------+
void Procesar(const int rates_total,const bool first)
  {
   //--- ajustar el analisis al historial que realmente hay
   int lookback = MathMin(InpLookback,MathMax(60,rates_total/2));
   if(lookback!=g_lookback)
     {
      g_lookback = lookback;
      g_scan.SetLookback(lookback);
     }

   if(rates_total<g_scan.MinBars())
     {
      Aviso(StringFormat("Hacen falta %d velas y el grafico tiene %d.\nPulsa Inicio o arrastra el grafico a la izquierda para descargar mas historial.",
                         g_scan.MinBars(),rates_total));
      return;
     }

   datetime bar_time = iTime(_Symbol,Period(),0);
   bool     new_bar  = (bar_time!=g_last_bar);

   //--- el trabajo pesado solo una vez por vela
   if(!first && !new_bar && g_ready)
     {
      ShowPanel();
      return;
     }
   g_last_bar = bar_time;

   if(!g_scan.LoadData(g_lookback+InpMaxSignalBars+50))
     {
      Aviso("Descargando el historial del simbolo...\nSi tarda, abre Herramientas > Centro de historiales y descarga este simbolo.");
      return;
     }
   g_ready = true;

   if(first)
     {
      ArrayInitialize(g_buy,EMPTY_VALUE);
      ArrayInitialize(g_sell,EMPTY_VALUE);
     }

   int limit = MathMin(InpMaxSignalBars,g_scan.Loaded()-g_scan.MinBars());
   if(limit<1)
      limit = 1;

   //--- recorrido vela a vela: cada barra se evalua con su propia
   //--- estructura, sin mirar el futuro
   PatternSignal sig;
   for(int s=limit; s>=1; s--)
     {
      g_buy[s]  = EMPTY_VALUE;
      g_sell[s] = EMPTY_VALUE;

      g_scan.Build(s);
      if(!g_scan.Signal(s,InpSignalBreakout,InpSignalBounce,InpSignalCandle,sig))
         continue;
      if(!SignalPasses(sig))
         continue;

      double pad = 0.8*g_scan.Atr(s);
      if(sig.dir>0) g_buy[s]  = g_scan.Low(s)-pad;
      else          g_sell[s] = g_scan.High(s)+pad;
     }

   g_buy[0]  = EMPTY_VALUE;
   g_sell[0] = EMPTY_VALUE;

   //--- la estructura vigente es la de la ultima vela cerrada
   g_scan.Build(1);
   DrawStructure();
   DrawCandleTags();

   if(g_scan.Signal(1,InpSignalBreakout,InpSignalBounce,InpSignalCandle,sig) &&
      SignalPasses(sig) && sig.time!=g_last_alert_bar)
     {
      g_last_alert_bar = sig.time;
      g_last_sig       = sig;
      g_has_sig        = true;
      FireAlert(sig);
      DrawSignalLevels(sig);
     }

   ShowPanel();
   ChartRedraw();
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
   Procesar(rates_total,(prev_calculated<=0));
   return(rates_total);
  }
//+------------------------------------------------------------------+
//| Con el mercado cerrado no llegan ticks y OnCalculate no se vuelve |
//| a llamar: el reloj reintenta hasta que haya datos suficientes.    |
//+------------------------------------------------------------------+
void OnTimer(void)
  {
   if(g_rates_total>0 && !g_ready)
      Procesar(g_rates_total,true);
  }
//+------------------------------------------------------------------+
