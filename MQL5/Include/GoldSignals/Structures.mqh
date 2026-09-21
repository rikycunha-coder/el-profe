//+------------------------------------------------------------------+
//|                                                    Structures.mqh |
//|   Deteccion de estructura de mercado: swings, tendencias, canales,|
//|   triangulos, cunas, dobles techos/suelos, y las senales que      |
//|   nacen de ellos (rupturas y rebotes).                            |
//|                                                                   |
//|   CLAVE ANTI-REPINTADO: Build() recibe `end_shift`, la vela que   |
//|   se considera "la ultima cerrada". Solo mira velas mas antiguas  |
//|   que esa. Asi, al recalcular el historico, cada senal se evalua  |
//|   con la informacion que existia en ese momento, igual que en     |
//|   vivo. Nada de dibujar figuras con el diario del dia siguiente.  |
//+------------------------------------------------------------------+
#ifndef GOLD_SIGNALS_STRUCTURES_MQH
#define GOLD_SIGNALS_STRUCTURES_MQH

#include <GoldSignals/Candles.mqh>

enum ENUM_STRUCT_TYPE
  {
   ST_NONE = 0,
   ST_TREND_UP,        // tendencia alcista (maximos y minimos crecientes)
   ST_TREND_DOWN,      // tendencia bajista
   ST_RANGE,           // rango sin direccion
   ST_CHANNEL_UP,      // canal alcista
   ST_CHANNEL_DOWN,    // canal bajista
   ST_CHANNEL_FLAT,    // canal lateral (rango entre paralelas)
   ST_TRI_SYM,         // triangulo simetrico
   ST_TRI_ASC,         // triangulo ascendente
   ST_TRI_DESC,        // triangulo descendente
   ST_WEDGE_RISING,    // cuna ascendente (sesgo bajista)
   ST_WEDGE_FALLING,   // cuna descendente (sesgo alcista)
   ST_DOUBLE_TOP,      // doble techo
   ST_DOUBLE_BOTTOM    // doble suelo
  };

enum ENUM_SIGNAL_KIND
  {
   SK_NONE = 0,
   SK_BREAKOUT,        // ruptura de linea
   SK_BOUNCE,          // rebote en linea
   SK_NECKLINE,        // ruptura del cuello de un doble techo/suelo
   SK_CANDLE           // patron de vela por si solo
  };

struct SwingPoint
  {
   int               shift;
   double            price;
   datetime          time;
  };

struct MarketStructure
  {
   ENUM_STRUCT_TYPE  type;
   string            name;
   int               bias;            // +1 alcista, -1 bajista, 0 neutro

   bool              has_high_line;
   int               h_shift0, h_shift1;
   double            h_price0, h_price1;
   datetime          h_time0, h_time1;
   double            slope_high;      // precio por vela

   bool              has_low_line;
   int               l_shift0, l_shift1;
   double            l_price0, l_price1;
   datetime          l_time0, l_time1;
   double            slope_low;

   bool              has_neck;
   double            neckline;
   double            pattern_height;
   int               apex_bars;       // velas hasta el vertice (-1 = no converge)
  };

struct PatternSignal
  {
   int               dir;             // +1 compra, -1 venta
   ENUM_SIGNAL_KIND  kind;
   ENUM_STRUCT_TYPE  structure;
   string            reason;          // texto de la senal
   string            candle;          // patron de vela que confirma (o "")
   int               score;           // 0..100
   double            entry, sl, tp1, tp2;
   double            rr;              // relacion riesgo/beneficio a TP1
   datetime          time;
  };

//+------------------------------------------------------------------+
string StructName(const ENUM_STRUCT_TYPE t)
  {
   switch(t)
     {
      case ST_TREND_UP:      return("Tendencia alcista");
      case ST_TREND_DOWN:    return("Tendencia bajista");
      case ST_RANGE:         return("Rango");
      case ST_CHANNEL_UP:    return("Canal alcista");
      case ST_CHANNEL_DOWN:  return("Canal bajista");
      case ST_CHANNEL_FLAT:  return("Canal lateral");
      case ST_TRI_SYM:       return("Triangulo simetrico");
      case ST_TRI_ASC:       return("Triangulo ascendente");
      case ST_TRI_DESC:      return("Triangulo descendente");
      case ST_WEDGE_RISING:  return("Cuna ascendente");
      case ST_WEDGE_FALLING: return("Cuna descendente");
      case ST_DOUBLE_TOP:    return("Doble techo");
      case ST_DOUBLE_BOTTOM: return("Doble suelo");
     }
   return("Sin estructura");
  }
//+------------------------------------------------------------------+
//| Escaner de estructura                                             |
//+------------------------------------------------------------------+
class CPatternScanner
  {
private:
   string            m_symbol;
   ENUM_TIMEFRAMES   m_tf;
   int               m_depth;      // velas a cada lado para confirmar un swing
   int               m_lookback;   // velas hacia atras al buscar swings
   int               m_loaded;
   int               m_h_atr;

   double            m_open[];
   double            m_high[];
   double            m_low[];
   double            m_close[];
   datetime          m_time[];
   double            m_atr[];

   SwingPoint        m_sh[];       // swings altos, del mas reciente al mas antiguo
   SwingPoint        m_sl[];
   int               m_n_sh;
   int               m_n_sl;

   MarketStructure   m_st;

   void              ResetStructure(void);
   void              FindSwings(const int end_shift);
   void              Classify(const int end_shift);
   int               Context(const int s);

public:
                     CPatternScanner(void);
                    ~CPatternScanner(void);

   bool              Init(const string symbol,const ENUM_TIMEFRAMES tf,
                          const int depth,const int lookback,const int atr_period);
   void              Deinit(void);
   bool              LoadData(const int bars);
   void              SetLookback(const int bars) { m_lookback = MathMax(60,bars); }
   void              Build(const int end_shift);
   bool              Signal(const int end_shift,const bool allow_breakout,
                            const bool allow_bounce,const bool allow_candle,
                            PatternSignal &sig);

   //--- acceso a datos
   int               Loaded(void) const { return m_loaded; }
   double            Open(const int s)  const { return((s>=0 && s<m_loaded) ? m_open[s]  : 0.0); }
   double            High(const int s)  const { return((s>=0 && s<m_loaded) ? m_high[s]  : 0.0); }
   double            Low(const int s)   const { return((s>=0 && s<m_loaded) ? m_low[s]   : 0.0); }
   double            Close(const int s) const { return((s>=0 && s<m_loaded) ? m_close[s] : 0.0); }
   double            Atr(const int s)   const { return((s>=0 && s<m_loaded) ? m_atr[s]   : 0.0); }
   datetime          Time(const int s)  const { return((s>=0 && s<m_loaded) ? m_time[s]  : 0); }
   int               MinBars(void) const { return(m_lookback+m_depth*2+30); }

   //--- estructura y swings
   MarketStructure   Structure(void) const { return m_st; }
   int               SwingHighs(void) const { return m_n_sh; }
   int               SwingLows(void)  const { return m_n_sl; }
   SwingPoint        SwingHigh(const int i) const;
   SwingPoint        SwingLow(const int i)  const;

   double            HighLineAt(const int s) const;
   double            LowLineAt(const int s) const;
   bool              CandleAtBar(const int s,CandleHit &hit);
  };
//+------------------------------------------------------------------+
CPatternScanner::CPatternScanner(void) : m_symbol(""),
                                         m_tf(PERIOD_CURRENT),
                                         m_depth(3),
                                         m_lookback(300),
                                         m_loaded(0),
                                         m_h_atr(INVALID_HANDLE),
                                         m_n_sh(0),
                                         m_n_sl(0)
  {
   ResetStructure();
  }
//+------------------------------------------------------------------+
CPatternScanner::~CPatternScanner(void)
  {
   Deinit();
  }
//+------------------------------------------------------------------+
void CPatternScanner::ResetStructure(void)
  {
   m_st.type           = ST_NONE;
   m_st.name           = StructName(ST_NONE);
   m_st.bias           = 0;
   m_st.has_high_line  = false;
   m_st.has_low_line   = false;
   m_st.has_neck       = false;
   m_st.h_shift0 = m_st.h_shift1 = 0;
   m_st.l_shift0 = m_st.l_shift1 = 0;
   m_st.h_price0 = m_st.h_price1 = 0.0;
   m_st.l_price0 = m_st.l_price1 = 0.0;
   m_st.h_time0  = m_st.h_time1  = 0;
   m_st.l_time0  = m_st.l_time1  = 0;
   m_st.slope_high = 0.0;
   m_st.slope_low  = 0.0;
   m_st.neckline   = 0.0;
   m_st.pattern_height = 0.0;
   m_st.apex_bars  = -1;
  }
//+------------------------------------------------------------------+
bool CPatternScanner::Init(const string symbol,const ENUM_TIMEFRAMES tf,
                           const int depth,const int lookback,const int atr_period)
  {
   Deinit();

   m_symbol   = (symbol=="" ? _Symbol : symbol);
   m_tf       = (tf==PERIOD_CURRENT ? (ENUM_TIMEFRAMES)Period() : tf);
   m_depth    = MathMax(2,depth);
   m_lookback = MathMax(60,lookback);

   m_h_atr = iATR(m_symbol,m_tf,MathMax(2,atr_period));
   if(m_h_atr==INVALID_HANDLE)
     {
      Print("PatternScanner: no se pudo crear el ATR (error ",GetLastError(),")");
      return(false);
     }

   ArraySetAsSeries(m_open,true);
   ArraySetAsSeries(m_high,true);
   ArraySetAsSeries(m_low,true);
   ArraySetAsSeries(m_close,true);
   ArraySetAsSeries(m_time,true);
   ArraySetAsSeries(m_atr,true);
   return(true);
  }
//+------------------------------------------------------------------+
void CPatternScanner::Deinit(void)
  {
   if(m_h_atr!=INVALID_HANDLE)
     {
      IndicatorRelease(m_h_atr);
      m_h_atr = INVALID_HANDLE;
     }
   m_loaded = 0;
  }
//+------------------------------------------------------------------+
bool CPatternScanner::LoadData(const int bars)
  {
   m_loaded = 0;

   int want  = MathMax(bars,MinBars());
   int avail = Bars(m_symbol,m_tf);
   if(avail<MinBars())
      return(false);
   if(want>avail)
      want = avail;

   if(CopyOpen(m_symbol,m_tf,0,want,m_open)   != want) return(false);
   if(CopyHigh(m_symbol,m_tf,0,want,m_high)   != want) return(false);
   if(CopyLow(m_symbol,m_tf,0,want,m_low)     != want) return(false);
   if(CopyClose(m_symbol,m_tf,0,want,m_close) != want) return(false);
   if(CopyTime(m_symbol,m_tf,0,want,m_time)   != want) return(false);
   if(CopyBuffer(m_h_atr,0,0,want,m_atr)      != want) return(false);

   m_loaded = want;
   return(true);
  }
//+------------------------------------------------------------------+
//| Swings tipo fractal: maximo/minimo rodeado de `depth` velas       |
//| menores a cada lado. Solo se aceptan los ya confirmados a la      |
//| derecha de `end_shift`.                                           |
//+------------------------------------------------------------------+
void CPatternScanner::FindSwings(const int end_shift)
  {
   m_n_sh = 0;
   m_n_sl = 0;
   ArrayResize(m_sh,0,128);   // se reserva memoria para no realojar en cada swing
   ArrayResize(m_sl,0,128);

   int first = end_shift+m_depth;                       // swing mas reciente posible
   int last  = MathMin(end_shift+m_lookback,m_loaded-m_depth-1);

   for(int s=first; s<=last; s++)
     {
      bool is_high = true;
      bool is_low  = true;

      for(int k=1; k<=m_depth; k++)
        {
         if(m_high[s]<=m_high[s-k] || m_high[s]<m_high[s+k]) is_high = false;
         if(m_low[s] >=m_low[s-k]  || m_low[s] >m_low[s+k])  is_low  = false;
         if(!is_high && !is_low)
            break;
        }

      if(is_high)
        {
         SwingPoint p;
         p.shift = s;
         p.price = m_high[s];
         p.time  = m_time[s];
         ArrayResize(m_sh,m_n_sh+1,128);
         m_sh[m_n_sh++] = p;
        }
      if(is_low)
        {
         SwingPoint p;
         p.shift = s;
         p.price = m_low[s];
         p.time  = m_time[s];
         ArrayResize(m_sl,m_n_sl+1,128);
         m_sl[m_n_sl++] = p;
        }
     }
  }
//+------------------------------------------------------------------+
//| Clasifica la figura a partir de los dos ultimos swings de cada    |
//| lado: pendientes, paralelismo y convergencia.                     |
//+------------------------------------------------------------------+
void CPatternScanner::Classify(const int end_shift)
  {
   ResetStructure();

   if(m_n_sh<2 || m_n_sl<2)
      return;

   double atr = m_atr[end_shift];
   if(atr<=0.0)
      return;

   //--- indice 0 = swing mas reciente, indice 1 = el anterior
   m_st.h_shift0 = m_sh[0].shift;  m_st.h_price0 = m_sh[0].price;  m_st.h_time0 = m_sh[0].time;
   m_st.h_shift1 = m_sh[1].shift;  m_st.h_price1 = m_sh[1].price;  m_st.h_time1 = m_sh[1].time;
   m_st.l_shift0 = m_sl[0].shift;  m_st.l_price0 = m_sl[0].price;  m_st.l_time0 = m_sl[0].time;
   m_st.l_shift1 = m_sl[1].shift;  m_st.l_price1 = m_sl[1].price;  m_st.l_time1 = m_sl[1].time;

   int span_h = m_st.h_shift1-m_st.h_shift0;
   int span_l = m_st.l_shift1-m_st.l_shift0;
   if(span_h<=0 || span_l<=0)
      return;

   m_st.slope_high = (m_st.h_price0-m_st.h_price1)/(double)span_h;
   m_st.slope_low  = (m_st.l_price0-m_st.l_price1)/(double)span_l;
   m_st.has_high_line = true;
   m_st.has_low_line  = true;

   double tol = 0.35*atr;
   bool up_h   = (m_st.h_price0 > m_st.h_price1+tol);
   bool down_h = (m_st.h_price0 < m_st.h_price1-tol);
   bool flat_h = (!up_h && !down_h);
   bool up_l   = (m_st.l_price0 > m_st.l_price1+tol);
   bool down_l = (m_st.l_price0 < m_st.l_price1-tol);
   bool flat_l = (!up_l && !down_l);

   //--- vertice de convergencia (en velas hacia el futuro)
   double denom = m_st.slope_low-m_st.slope_high;
   m_st.apex_bars = -1;
   if(MathAbs(denom)>1e-12)
     {
      double apex_shift = (m_st.l_price0+m_st.slope_low*m_st.l_shift0
                           -m_st.h_price0-m_st.slope_high*m_st.h_shift0)/denom;
      double bars_ahead = end_shift-apex_shift;
      if(bars_ahead>3.0 && bars_ahead<400.0)
         m_st.apex_bars = (int)MathRound(bars_ahead);
     }
   bool converging = (m_st.apex_bars>0);

   double line_h = HighLineAt(end_shift);
   double line_l = LowLineAt(end_shift);
   m_st.pattern_height = MathAbs(line_h-line_l);

   //--- 1) Doble techo / doble suelo: dos extremos al mismo nivel con
   //---    un valle (o pico) intermedio que hace de cuello
   if(flat_h && !up_l && MathAbs(m_st.h_price0-m_st.h_price1)<0.4*atr && span_h>=m_depth*3)
     {
      for(int i=0; i<m_n_sl; i++)
         if(m_sl[i].shift>m_st.h_shift0 && m_sl[i].shift<m_st.h_shift1)
           {
            m_st.type     = ST_DOUBLE_TOP;
            m_st.bias     = -1;
            m_st.has_neck = true;
            m_st.neckline = m_sl[i].price;
            m_st.pattern_height = MathAbs(m_st.h_price0-m_st.neckline);
            m_st.name     = StructName(m_st.type);
            return;
           }
     }
   if(flat_l && !down_h && MathAbs(m_st.l_price0-m_st.l_price1)<0.4*atr && span_l>=m_depth*3)
     {
      for(int i=0; i<m_n_sh; i++)
         if(m_sh[i].shift>m_st.l_shift0 && m_sh[i].shift<m_st.l_shift1)
           {
            m_st.type     = ST_DOUBLE_BOTTOM;
            m_st.bias     = 1;
            m_st.has_neck = true;
            m_st.neckline = m_sh[i].price;
            m_st.pattern_height = MathAbs(m_st.neckline-m_st.l_price0);
            m_st.name     = StructName(m_st.type);
            return;
           }
     }

   //--- 2) Triangulos (lineas que convergen)
   if(converging)
     {
      if(flat_h && up_l)   { m_st.type = ST_TRI_ASC;  m_st.bias =  1; }
      else if(down_h && flat_l) { m_st.type = ST_TRI_DESC; m_st.bias = -1; }
      else if(down_h && up_l)   { m_st.type = ST_TRI_SYM;  m_st.bias =  0; }
      else if(up_h && up_l && m_st.slope_low>m_st.slope_high)
                                { m_st.type = ST_WEDGE_RISING;  m_st.bias = -1; }
      else if(down_h && down_l && m_st.slope_high<m_st.slope_low)
                                { m_st.type = ST_WEDGE_FALLING; m_st.bias =  1; }

      if(m_st.type!=ST_NONE)
        {
         m_st.name = StructName(m_st.type);
         return;
        }
     }

   //--- 3) Canales (lineas aproximadamente paralelas)
   double max_slope = MathMax(MathAbs(m_st.slope_high),MathAbs(m_st.slope_low));
   bool parallel = (max_slope<1e-12) ||
                   (MathAbs(m_st.slope_high-m_st.slope_low)<0.4*max_slope);

   if(parallel)
     {
      if(up_h && up_l)     { m_st.type = ST_CHANNEL_UP;   m_st.bias =  1; }
      else if(down_h && down_l) { m_st.type = ST_CHANNEL_DOWN; m_st.bias = -1; }
      else if(flat_h && flat_l) { m_st.type = ST_CHANNEL_FLAT; m_st.bias =  0; }

      if(m_st.type!=ST_NONE)
        {
         m_st.name = StructName(m_st.type);
         return;
        }
     }

   //--- 4) Tendencia simple por maximos y minimos
   if(up_h && up_l)        { m_st.type = ST_TREND_UP;   m_st.bias =  1; }
   else if(down_h && down_l) { m_st.type = ST_TREND_DOWN; m_st.bias = -1; }
   else                    { m_st.type = ST_RANGE;      m_st.bias =  0; }

   m_st.name = StructName(m_st.type);
  }
//+------------------------------------------------------------------+
void CPatternScanner::Build(const int end_shift)
  {
   if(end_shift<0 || end_shift>=m_loaded)
     {
      ResetStructure();
      return;
     }
   FindSwings(end_shift);
   Classify(end_shift);
  }
//+------------------------------------------------------------------+
SwingPoint CPatternScanner::SwingHigh(const int i) const
  {
   SwingPoint empty;
   empty.shift = -1; empty.price = 0.0; empty.time = 0;
   if(i<0 || i>=m_n_sh)
      return(empty);
   return(m_sh[i]);
  }
//+------------------------------------------------------------------+
SwingPoint CPatternScanner::SwingLow(const int i) const
  {
   SwingPoint empty;
   empty.shift = -1; empty.price = 0.0; empty.time = 0;
   if(i<0 || i>=m_n_sl)
      return(empty);
   return(m_sl[i]);
  }
//+------------------------------------------------------------------+
double CPatternScanner::HighLineAt(const int s) const
  {
   if(!m_st.has_high_line)
      return(0.0);
   return(m_st.h_price0+m_st.slope_high*(m_st.h_shift0-s));
  }
//+------------------------------------------------------------------+
double CPatternScanner::LowLineAt(const int s) const
  {
   if(!m_st.has_low_line)
      return(0.0);
   return(m_st.l_price0+m_st.slope_low*(m_st.l_shift0-s));
  }
//+------------------------------------------------------------------+
//| Contexto de corto plazo, para leer bien las velas de giro         |
//+------------------------------------------------------------------+
int CPatternScanner::Context(const int s)
  {
   if(s+7>=m_loaded)
      return(0);

   double atr  = m_atr[s];
   double diff = m_close[s+1]-m_close[s+6];
   if(atr<=0.0)
      return(0);
   if(diff> 0.6*atr) return(1);
   if(diff<-0.6*atr) return(-1);
   return(0);
  }
//+------------------------------------------------------------------+
bool CPatternScanner::CandleAtBar(const int s,CandleHit &hit)
  {
   if(s<0 || s+3>=m_loaded)
     {
      CandleFill(hit,CP_NONE,0,0);
      return(false);
     }
   return(CandleAt(m_open,m_high,m_low,m_close,s,Context(s),m_atr[s],hit));
  }
//+------------------------------------------------------------------+
//| Evalua la senal de la vela `end_shift` con la estructura que ya   |
//| se construyo para esa misma vela.                                 |
//+------------------------------------------------------------------+
bool CPatternScanner::Signal(const int end_shift,const bool allow_breakout,
                             const bool allow_bounce,const bool allow_candle,
                             PatternSignal &sig)
  {
   sig.dir       = 0;
   sig.kind      = SK_NONE;
   sig.structure = m_st.type;
   sig.reason    = "";
   sig.candle    = "";
   sig.score     = 0;
   sig.entry = sig.sl = sig.tp1 = sig.tp2 = 0.0;
   sig.rr        = 0.0;
   sig.time      = 0;

   int s = end_shift;
   if(s<1 || s+3>=m_loaded)
      return(false);

   double atr = m_atr[s];
   if(atr<=0.0)
      return(false);

   sig.time = m_time[s];

   double c   = m_close[s];
   double c1  = m_close[s+1];
   double hi  = m_high[s];
   double lo  = m_low[s];
   double body= MathAbs(c-m_open[s]);

   CandleHit hit;
   bool has_candle = CandleAtBar(s,hit);

   double line_h  = HighLineAt(s);
   double line_h1 = HighLineAt(s+1);
   double line_l  = LowLineAt(s);
   double line_l1 = LowLineAt(s+1);

   int    dir    = 0;
   ENUM_SIGNAL_KIND kind = SK_NONE;
   string reason = "";
   double sl=0.0, tp1=0.0, tp2=0.0;
   int    base   = 0;

   //--- A) Cuello de doble techo / doble suelo
   if(m_st.has_neck)
     {
      if(m_st.type==ST_DOUBLE_TOP && c<m_st.neckline-0.1*atr && c1>=m_st.neckline)
        {
         dir=-1; kind=SK_NECKLINE; base=65;
         reason = "Doble techo: ruptura del cuello";
         sl  = MathMax(m_high[s],m_high[s+1])+0.3*atr;
         tp1 = m_st.neckline-0.6*m_st.pattern_height;
         tp2 = m_st.neckline-m_st.pattern_height;
        }
      else if(m_st.type==ST_DOUBLE_BOTTOM && c>m_st.neckline+0.1*atr && c1<=m_st.neckline)
        {
         dir=1; kind=SK_NECKLINE; base=65;
         reason = "Doble suelo: ruptura del cuello";
         sl  = MathMin(m_low[s],m_low[s+1])-0.3*atr;
         tp1 = m_st.neckline+0.6*m_st.pattern_height;
         tp2 = m_st.neckline+m_st.pattern_height;
        }
     }

   //--- B) Ruptura de la linea superior o inferior de la figura
   if(dir==0 && allow_breakout && m_st.has_high_line && m_st.has_low_line && body>0.35*atr)
     {
      double height = MathMax(MathAbs(line_h-line_l),1.0*atr);

      if(c>line_h+0.1*atr && c1<=line_h1+0.1*atr)
        {
         dir=1; kind=SK_BREAKOUT; base=60;
         reason = "Ruptura al alza de "+m_st.name;
         sl  = MathMin(line_l,lo)-0.3*atr;
         tp1 = c+height;
         tp2 = c+1.5*height;
        }
      else if(c<line_l-0.1*atr && c1>=line_l1-0.1*atr)
        {
         dir=-1; kind=SK_BREAKOUT; base=60;
         reason = "Ruptura a la baja de "+m_st.name;
         sl  = MathMax(line_h,hi)+0.3*atr;
         tp1 = c-height;
         tp2 = c-1.5*height;
        }
     }

   //--- C) Rebote dentro de la figura (comprar soporte / vender resistencia)
   if(dir==0 && allow_bounce && m_st.has_high_line && m_st.has_low_line && has_candle)
     {
      bool at_low  = (lo<=line_l+0.35*atr && c>line_l);
      bool at_high = (hi>=line_h-0.35*atr && c<line_h);

      if(at_low && hit.dir>0 && m_st.bias>=0)
        {
         dir=1; kind=SK_BOUNCE; base=55;
         reason = "Rebote en el soporte de "+m_st.name;
         sl  = MathMin(lo,line_l)-0.4*atr;
         tp1 = line_h;
         tp2 = c+2.0*(c-sl);
        }
      else if(at_high && hit.dir<0 && m_st.bias<=0)
        {
         dir=-1; kind=SK_BOUNCE; base=55;
         reason = "Rechazo en la resistencia de "+m_st.name;
         sl  = MathMax(hi,line_h)+0.4*atr;
         tp1 = line_l;
         tp2 = c-2.0*(sl-c);
        }
     }

   //--- D) Patron de vela fuerte a favor de la estructura
   if(dir==0 && allow_candle && has_candle && hit.strength>=3 &&
      hit.dir!=0 && (m_st.bias==0 || m_st.bias==hit.dir))
     {
      dir=hit.dir; kind=SK_CANDLE; base=45;
      reason = hit.name;
      if(dir>0)
        {
         sl  = lo-0.4*atr;
         tp1 = c+1.5*atr;
         tp2 = c+3.0*atr;
        }
      else
        {
         sl  = hi+0.4*atr;
         tp1 = c-1.5*atr;
         tp2 = c-3.0*atr;
        }
     }

   if(dir==0)
      return(false);

   //--- stop minimo razonable
   if(dir>0 && c-sl<0.5*atr) sl = c-0.5*atr;
   if(dir<0 && sl-c<0.5*atr) sl = c+0.5*atr;

   //--- los objetivos deben quedar del lado correcto de la entrada: si la
   //--- linea de destino ya estaba superada, se cae a objetivos por ATR
   if(dir>0)
     {
      if(tp1<c+0.3*atr) tp1 = c+1.5*atr;
      if(tp2<tp1)       tp2 = c+2.5*atr;
     }
   else
     {
      if(tp1>c-0.3*atr) tp1 = c-1.5*atr;
      if(tp2>tp1)       tp2 = c-2.5*atr;
     }

   //--- puntuacion
   int score = base;
   if(has_candle && hit.dir==dir)
     {
      score += 10;
      if(hit.strength>=3)
         score += 5;
      sig.candle = hit.name;
     }
   if(m_st.bias==dir)      score += 10;
   else if(m_st.bias==-dir) score -= 10;
   if(body>atr)            score += 5;

   double risk   = MathAbs(c-sl);
   double reward = MathAbs(tp1-c);
   if(risk>0.0)
     {
      sig.rr = reward/risk;
      if(sig.rr>=1.5)      score += 5;
      else if(sig.rr<0.8)  score -= 10;
     }

   sig.dir       = dir;
   sig.kind      = kind;
   sig.structure = m_st.type;
   sig.reason    = reason;
   sig.score     = (int)MathMax(0,MathMin(100,score));
   sig.entry     = c;
   sig.sl        = sl;
   sig.tp1       = tp1;
   sig.tp2       = tp2;
   return(true);
  }

#endif // GOLD_SIGNALS_STRUCTURES_MQH
