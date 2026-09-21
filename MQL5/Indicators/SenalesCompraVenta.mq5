//+------------------------------------------------------------------+
//|                                            SenalesCompraVenta.mq5 |
//|                                            SenalesCompraVenta.mq4 |
//|                                                                   |
//|   Un solo indicador con un solo trabajo: decir COMPRA, VENTA o    |
//|   ESPERAR, con el precio de entrada, el stop y el objetivo.       |
//|                                                                   |
//|   Combina los dos analisis del repositorio:                       |
//|     - tendencia (medias + RSI + ADX)                              |
//|     - estructura (figuras chartistas y patrones de velas)         |
//|                                                                   |
//|   El mismo archivo vale para MetaTrader 4 y 5.                    |
//+------------------------------------------------------------------+
#property copyright "el-profe"
#property version   "1.00"
#property description "Senales claras de compra y venta: panel, flechas y avisos."
#property indicator_chart_window
#property indicator_buffers 2
#property indicator_plots   2
#property indicator_label1  "Compra"
#property indicator_type1   DRAW_ARROW
#property indicator_color1  clrDodgerBlue
#property indicator_width1  4
#property indicator_label2  "Venta"
#property indicator_type2   DRAW_ARROW
#property indicator_color2  clrOrangeRed
#property indicator_width2  4

#include <GoldSignals/SignalEngine.mqh>
#include <GoldSignals/Structures.mqh>

enum ENUM_SENSIBILIDAD
  {
   SENS_BAJA   = 0,   // Pocas senales, muy filtradas
   SENS_MEDIA  = 1,   // Equilibrado
   SENS_ALTA   = 2    // Muchas senales, mas ruido
  };

enum ENUM_COMBINAR
  {
   COMBI_CUALQUIERA = 0,   // Avisar con cualquiera de los dos analisis
   COMBI_AMBOS      = 1    // Avisar solo si los dos coinciden
  };

input ENUM_SENSIBILIDAD InpSensibilidad = SENS_MEDIA;      // Sensibilidad
input ENUM_COMBINAR     InpCombinar     = COMBI_CUALQUIERA;// Cuando avisar
input bool   InpSoloSesion    = false;   // Operar solo en horario Londres/NY
input bool   InpMostrarPanel  = true;    // Panel con la senal
input bool   InpMostrarNiveles= true;    // Lineas de SL y objetivo
input bool   InpAlertaVentana = true;    // Aviso en ventana
input bool   InpAlertaSonido  = true;    // Aviso con sonido
input bool   InpAlertaMovil   = false;   // Aviso al movil (MetaQuotes ID)
input int    InpVelasHistorial= 300;     // Velas de historial con flechas

//--- la senal ya resuelta, lista para mostrar
struct Senal
  {
   int       dir;        // +1 compra, -1 venta, 0 esperar
   double    entrada;
   double    sl;
   double    tp;
   int       fuerza;     // 0..100
   string    motivo;
   datetime  hora;
  };

double            g_buy[];
double            g_sell[];
CGoldSignalEngine g_tend;
CPatternScanner   g_fig;
string            g_pfx = "SCV_";

int      g_min_score   = 60;
double   g_min_rr      = 1.0;
bool     g_velas_solas = false;

int      g_rates_total = 0;
bool     g_listo       = false;
datetime g_ultima_vela = 0;
datetime g_ultimo_aviso= 0;
string   g_texto_aviso = "";
Senal    g_senal;

//+------------------------------------------------------------------+
string TfNombre(const int tf)
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
//| La sensibilidad traduce a los filtros de los dos analisis         |
//+------------------------------------------------------------------+
void Configurar(GSSettings &cfg)
  {
   GSDefaults(cfg);

   if(InpSensibilidad==SENS_BAJA)
     {
      cfg.adx_min   = 25.0;
      cfg.min_score = 75;
      g_min_score   = 75;
      g_min_rr      = 1.5;
      g_velas_solas = false;
     }
   else if(InpSensibilidad==SENS_ALTA)
     {
      cfg.adx_min   = 15.0;
      cfg.min_score = 45;
      g_min_score   = 45;
      g_min_rr      = 0.8;
      g_velas_solas = true;
     }
   else
     {
      cfg.adx_min   = 20.0;
      cfg.min_score = 60;
      g_min_score   = 60;
      g_min_rr      = 1.0;
      g_velas_solas = false;
     }

   cfg.use_session = InpSoloSesion;
  }
//+------------------------------------------------------------------+
int OnInit(void)
  {
#ifdef __MQL5__
   SetIndexBuffer(0,g_buy,INDICATOR_DATA);
   SetIndexBuffer(1,g_sell,INDICATOR_DATA);
   ArraySetAsSeries(g_buy,true);
   ArraySetAsSeries(g_sell,true);
   PlotIndexSetInteger(0,PLOT_ARROW,233);
   PlotIndexSetInteger(1,PLOT_ARROW,234);
   PlotIndexSetDouble(0,PLOT_EMPTY_VALUE,EMPTY_VALUE);
   PlotIndexSetDouble(1,PLOT_EMPTY_VALUE,EMPTY_VALUE);
   IndicatorSetString(INDICATOR_SHORTNAME,"Senales de entrada");
#else
   IndicatorBuffers(2);
   SetIndexBuffer(0,g_buy);
   SetIndexBuffer(1,g_sell);
   SetIndexStyle(0,DRAW_ARROW,EMPTY,4,clrDodgerBlue);
   SetIndexStyle(1,DRAW_ARROW,EMPTY,4,clrOrangeRed);
   SetIndexArrow(0,233);
   SetIndexArrow(1,234);
   SetIndexEmptyValue(0,EMPTY_VALUE);
   SetIndexEmptyValue(1,EMPTY_VALUE);
   SetIndexLabel(0,"Compra");
   SetIndexLabel(1,"Venta");
   IndicatorShortName("Senales de entrada");
#endif

   GSSettings cfg;
   Configurar(cfg);

   if(!g_tend.Init(_Symbol,(ENUM_TIMEFRAMES)Period(),cfg))
      return(INIT_FAILED);
   if(!g_fig.Init(_Symbol,(ENUM_TIMEFRAMES)Period(),3,200,14))
      return(INIT_FAILED);

   g_senal.dir = 0;
   EventSetTimer(2);
   return(INIT_SUCCEEDED);
  }
//+------------------------------------------------------------------+
void OnDeinit(const int reason)
  {
   EventKillTimer();
   g_tend.Deinit();
   g_fig.Deinit();
   ObjectsDeleteAll(0,g_pfx,-1,-1);
   Comment("");
   ChartRedraw();
  }
//+------------------------------------------------------------------+
//| Dibujo del panel                                                  |
//+------------------------------------------------------------------+
void Fondo(const string nombre,const int x,const int y,const int ancho,const int alto,const color bg)
  {
   if(ObjectFind(0,nombre)<0)
      ObjectCreate(0,nombre,OBJ_RECTANGLE_LABEL,0,0,0);
   ObjectSetInteger(0,nombre,OBJPROP_CORNER,CORNER_LEFT_UPPER);
   ObjectSetInteger(0,nombre,OBJPROP_XDISTANCE,x);
   ObjectSetInteger(0,nombre,OBJPROP_YDISTANCE,y);
   ObjectSetInteger(0,nombre,OBJPROP_XSIZE,ancho);
   ObjectSetInteger(0,nombre,OBJPROP_YSIZE,alto);
   ObjectSetInteger(0,nombre,OBJPROP_BGCOLOR,bg);
   ObjectSetInteger(0,nombre,OBJPROP_BORDER_TYPE,BORDER_FLAT);
   ObjectSetInteger(0,nombre,OBJPROP_COLOR,clrDimGray);
   ObjectSetInteger(0,nombre,OBJPROP_BACK,false);
   ObjectSetInteger(0,nombre,OBJPROP_SELECTABLE,false);
  }
//+------------------------------------------------------------------+
void Texto(const string nombre,const int x,const int y,const string txt,
           const color clr,const int tam,const string fuente)
  {
   if(ObjectFind(0,nombre)<0)
      ObjectCreate(0,nombre,OBJ_LABEL,0,0,0);
   ObjectSetInteger(0,nombre,OBJPROP_CORNER,CORNER_LEFT_UPPER);
   ObjectSetInteger(0,nombre,OBJPROP_XDISTANCE,x);
   ObjectSetInteger(0,nombre,OBJPROP_YDISTANCE,y);
   ObjectSetString(0,nombre,OBJPROP_TEXT,txt);
   ObjectSetString(0,nombre,OBJPROP_FONT,fuente);
   ObjectSetInteger(0,nombre,OBJPROP_FONTSIZE,tam);
   ObjectSetInteger(0,nombre,OBJPROP_COLOR,clr);
   ObjectSetInteger(0,nombre,OBJPROP_BACK,false);
   ObjectSetInteger(0,nombre,OBJPROP_SELECTABLE,false);
  }
//+------------------------------------------------------------------+
void Linea(const string nombre,const double precio,const color clr,const string etiqueta)
  {
   if(precio<=0.0)
      return;
   if(ObjectFind(0,nombre)<0)
      ObjectCreate(0,nombre,OBJ_HLINE,0,0,precio);
   ObjectSetDouble(0,nombre,OBJPROP_PRICE,precio);
   ObjectSetInteger(0,nombre,OBJPROP_COLOR,clr);
   ObjectSetInteger(0,nombre,OBJPROP_STYLE,STYLE_DOT);
   ObjectSetInteger(0,nombre,OBJPROP_WIDTH,1);
   ObjectSetInteger(0,nombre,OBJPROP_BACK,true);
   ObjectSetInteger(0,nombre,OBJPROP_SELECTABLE,false);
   ObjectSetString(0,nombre,OBJPROP_TEXT,etiqueta+" "+DoubleToString(precio,_Digits));
  }
//+------------------------------------------------------------------+
void Panel(const string estado,const color clr_estado,
           const string l1,const string l2,const string l3)
  {
   if(!InpMostrarPanel)
      return;

   Fondo(g_pfx+"bg",8,16,268,116,C'25,25,30');
   Texto(g_pfx+"t0",18,24,_Symbol+"  "+TfNombre(Period()),clrSilver,9,"Arial");
   Texto(g_pfx+"t1",18,40,estado,clr_estado,20,"Arial Black");
   Texto(g_pfx+"t2",18,72,l1,clrWhiteSmoke,9,"Arial");
   Texto(g_pfx+"t3",18,88,l2,clrWhiteSmoke,9,"Arial");
   Texto(g_pfx+"t4",18,104,l3,clrSilver,8,"Arial");
  }
//+------------------------------------------------------------------+
void Aviso(const string texto)
  {
   Panel("ESPERANDO",clrGoldenrod,texto,"","");
   if(texto!=g_texto_aviso)
     {
      Print(texto);
      g_texto_aviso = texto;
     }
   ChartRedraw();
  }
//+------------------------------------------------------------------+
//| Nucleo: combina los dos analisis en una sola decision             |
//+------------------------------------------------------------------+
bool Decidir(const int shift,Senal &s)
  {
   s.dir     = 0;
   s.entrada = 0.0;
   s.sl      = 0.0;
   s.tp      = 0.0;
   s.fuerza  = 0;
   s.motivo  = "";
   s.hora    = 0;

   //--- analisis 1: tendencia
   GSResult r;
   bool hay_tend = (g_tend.Evaluate(shift,r) && r.dir!=GS_NONE);

   //--- analisis 2: estructura y velas
   g_fig.Build(shift);
   PatternSignal p;
   bool hay_fig = g_fig.Signal(shift,true,true,g_velas_solas,p);
   if(hay_fig && (p.dir==0 || p.score<g_min_score || p.rr<g_min_rr))
      hay_fig = false;

   int dir_t = (hay_tend ? (r.dir==GS_BUY ? 1 : -1) : 0);
   int dir_f = (hay_fig  ? p.dir : 0);

   //--- si se contradicen, no hay senal: mejor quedarse fuera
   if(dir_t!=0 && dir_f!=0 && dir_t!=dir_f)
      return(false);

   if(InpCombinar==COMBI_AMBOS && (dir_t==0 || dir_f==0))
      return(false);

   int dir = (dir_t!=0 ? dir_t : dir_f);
   if(dir==0)
      return(false);

   s.dir  = dir;
   s.hora = (dir_t!=0 ? r.bar_time : p.time);

   if(dir_t!=0 && dir_f!=0)
     {
      //--- los dos coinciden: la senal mas fiable que da el sistema
      s.fuerza  = (int)MathMin(100,MathMax(r.score,p.score)+15);
      s.motivo  = r.setup+" + "+p.reason;
      s.entrada = r.price;
      //--- stop en el mas lejano de los dos y objetivo en el mas cercano:
      //--- menos sustos y objetivo realista
      s.sl = (dir>0 ? MathMin(r.sl,p.sl) : MathMax(r.sl,p.sl));
      s.tp = (dir>0 ? MathMin(r.tp1,p.tp1) : MathMax(r.tp1,p.tp1));
     }
   else if(dir_t!=0)
     {
      s.fuerza  = r.score;
      s.motivo  = r.setup;
      s.entrada = r.price;
      s.sl      = r.sl;
      s.tp      = r.tp1;
     }
   else
     {
      s.fuerza  = p.score;
      s.motivo  = p.reason;
      if(p.candle!="")
         s.motivo += " ("+p.candle+")";
      s.entrada = p.entry;
      s.sl      = p.sl;
      s.tp      = p.tp1;
     }

   return(s.fuerza>=g_min_score);
  }
//+------------------------------------------------------------------+
void Alertar(const Senal &s)
  {
   string cab = StringFormat("%s %s %s  entrada %s  SL %s  TP %s",
                             (s.dir>0 ? "COMPRA" : "VENTA"),
                             _Symbol,TfNombre(Period()),
                             DoubleToString(s.entrada,_Digits),
                             DoubleToString(s.sl,_Digits),
                             DoubleToString(s.tp,_Digits));
   string cuerpo = cab+"\nMotivo: "+s.motivo+StringFormat("\nFuerza %d/100",s.fuerza);

   if(InpAlertaVentana) Alert(cab);
   if(InpAlertaSonido)  PlaySound("alert.wav");
   if(InpAlertaMovil)   SendNotification(cuerpo);
   Print(cuerpo);
  }
//+------------------------------------------------------------------+
void MostrarEstado(void)
  {
   MarketStructure st = g_fig.Structure();
   string figura = st.name;
   string tend   = g_tend.Bias(1);

   if(g_senal.dir!=0)
     {
      string estado = (g_senal.dir>0 ? "COMPRA" : "VENTA");
      color  clr    = (g_senal.dir>0 ? clrDeepSkyBlue : clrTomato);

      Panel(estado,clr,
            "Entrada "+DoubleToString(g_senal.entrada,_Digits),
            "SL "+DoubleToString(g_senal.sl,_Digits)+"   TP "+DoubleToString(g_senal.tp,_Digits),
            StringFormat("Fuerza %d/100  -  %s",g_senal.fuerza,g_senal.motivo));

      if(InpMostrarNiveles)
        {
         Linea(g_pfx+"sl",g_senal.sl,clrCrimson,"SL");
         Linea(g_pfx+"tp",g_senal.tp,clrMediumSeaGreen,"TP");
        }
     }
   else
     {
      Panel("ESPERAR",clrGray,
            "Tendencia: "+tend,
            "Figura: "+figura,
            "Sin entrada clara ahora mismo");
     }
   ChartRedraw();
  }
//+------------------------------------------------------------------+
void Procesar(const int rates_total,const bool primera)
  {
   if(rates_total<g_fig.MinBars() || rates_total<g_tend.MinBars()+5)
     {
      Aviso(StringFormat("Faltan velas: hay %d.\nPulsa Inicio para cargar historial.",rates_total));
      return;
     }

   datetime vela = iTime(_Symbol,Period(),0);
   bool nueva = (vela!=g_ultima_vela);
   if(!primera && !nueva && g_listo)
     {
      MostrarEstado();
      return;
     }
   g_ultima_vela = vela;

   int velas = MathMin(InpVelasHistorial,rates_total-50);
   if(velas<20)
      velas = 20;

   if(!g_tend.Refresh(velas+g_tend.MinBars()+10) || !g_fig.LoadData(velas+250))
     {
      Aviso("Descargando historial del simbolo...");
      return;
     }
   g_listo = true;

   if(primera)
     {
      ArrayInitialize(g_buy,EMPTY_VALUE);
      ArrayInitialize(g_sell,EMPTY_VALUE);
     }

   int limite = MathMin(velas,MathMin(g_tend.Loaded(),g_fig.Loaded())-g_fig.MinBars());
   if(limite<1)
      limite = 1;

   Senal s;
   for(int i=limite;i>=1;i--)
     {
      g_buy[i]  = EMPTY_VALUE;
      g_sell[i] = EMPTY_VALUE;

      if(!Decidir(i,s))
         continue;

      double pad = 0.8*g_fig.Atr(i);
      if(s.dir>0) g_buy[i]  = g_fig.Low(i)-pad;
      else        g_sell[i] = g_fig.High(i)+pad;
     }
   g_buy[0]  = EMPTY_VALUE;
   g_sell[0] = EMPTY_VALUE;

   //--- la vela 1 acaba de cerrar: esa es la senal viva
   g_fig.Build(1);
   if(Decidir(1,s))
     {
      g_senal = s;
      if(s.hora!=g_ultimo_aviso)
        {
         g_ultimo_aviso = s.hora;
         Alertar(s);
        }
     }
   else
      g_senal.dir = 0;

   MostrarEstado();
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
void OnTimer(void)
  {
   if(g_rates_total>0 && !g_listo)
      Procesar(g_rates_total,true);
  }
//+------------------------------------------------------------------+
