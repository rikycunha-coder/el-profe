//+------------------------------------------------------------------+
//|                                                  SignalEngine.mqh |
//|   Motor de senales XAUUSD (oro) - logica compartida indicador/EA  |
//|                                                                   |
//|   Diseno: las senales se confirman SIEMPRE sobre vela cerrada     |
//|   (shift >= 1) para que no repinten. El shift 0 solo se usa para  |
//|   el panel "en formacion".                                        |
//+------------------------------------------------------------------+
#ifndef GOLD_SIGNALS_SIGNAL_ENGINE_MQH
#define GOLD_SIGNALS_SIGNAL_ENGINE_MQH

//--- direccion de la senal
enum ENUM_GS_DIR
  {
   GS_NONE =  0,  // sin senal
   GS_BUY  =  1,  // compra
   GS_SELL = -1   // venta
  };

//--- parametros de configuracion del motor
struct GSSettings
  {
   int               ema_fast;            // EMA rapida (timeframe de operativa)
   int               ema_slow;            // EMA lenta  (timeframe de operativa)
   ENUM_TIMEFRAMES   trend_tf;            // timeframe superior para el filtro de tendencia
   int               trend_ema;           // EMA del timeframe superior
   int               rsi_period;          // periodo RSI
   double            rsi_max_buy;         // no comprar por encima de este RSI
   double            rsi_min_sell;        // no vender por debajo de este RSI
   int               atr_period;          // periodo ATR
   int               adx_period;          // periodo ADX
   double            adx_min;             // fuerza minima de tendencia
   double            min_atr_points;      // volatilidad minima en puntos (0 = sin filtro)
   double            max_atr_points;      // volatilidad maxima en puntos (0 = sin filtro)
   double            sl_atr_mult;         // stop loss = N x ATR
   double            tp1_atr_mult;        // objetivo 1 = N x ATR
   double            tp2_atr_mult;        // objetivo 2 = N x ATR
   double            max_spread_points;   // spread maximo admitido (0 = sin filtro)
   bool              use_session;         // filtrar por horario
   int               session1_start;      // hora inicio sesion 1 (hora del servidor)
   int               session1_end;        // hora fin sesion 1
   int               session2_start;      // hora inicio sesion 2 (-1 = desactivada)
   int               session2_end;        // hora fin sesion 2
   bool              skip_friday_close;   // no operar las ultimas horas del viernes
   int               min_score;           // puntuacion minima para publicar la senal
  };

//--- resultado de la evaluacion de una vela
struct GSResult
  {
   ENUM_GS_DIR       dir;          // direccion publicada (GS_NONE si filtrada)
   ENUM_GS_DIR       raw_dir;      // direccion del gatillo antes de filtros
   datetime          bar_time;     // vela evaluada
   double            price;        // precio de referencia de entrada
   double            sl;           // stop loss sugerido
   double            tp1;          // objetivo 1
   double            tp2;          // objetivo 2
   double            atr;
   double            rsi;
   double            adx;
   double            ema_fast;
   double            ema_slow;
   double            ema_trend;
   int               score;        // 0..100 confianza
   string            setup;        // nombre del patron disparado
   string            notes;        // motivo del bloqueo, si lo hubo
  };

//--- valores por defecto pensados para XAUUSD en M5/M15
void GSDefaults(GSSettings &s)
  {
   s.ema_fast          = 21;
   s.ema_slow          = 50;
   s.trend_tf          = PERIOD_H1;
   s.trend_ema         = 200;
   s.rsi_period        = 14;
   s.rsi_max_buy       = 72.0;
   s.rsi_min_sell      = 28.0;
   s.atr_period        = 14;
   s.adx_period        = 14;
   s.adx_min           = 20.0;
   s.min_atr_points    = 0.0;
   s.max_atr_points    = 0.0;
   s.sl_atr_mult       = 1.5;
   s.tp1_atr_mult      = 1.5;
   s.tp2_atr_mult      = 3.0;
   s.max_spread_points = 0.0;
   s.use_session       = true;
   s.session1_start    = 8;
   s.session1_end      = 12;
   s.session2_start    = 13;
   s.session2_end      = 19;
   s.skip_friday_close = true;
   s.min_score         = 60;
  }

//+------------------------------------------------------------------+
//| Motor de senales                                                  |
//+------------------------------------------------------------------+
class CGoldSignalEngine
  {
private:
   string            m_symbol;
   ENUM_TIMEFRAMES   m_tf;
   GSSettings        m_cfg;
   double            m_point;
   int               m_digits;

   int               m_h_ema_fast;
   int               m_h_ema_slow;
   int               m_h_ema_trend;
   int               m_h_rsi;
   int               m_h_atr;
   int               m_h_adx;

   int               m_loaded;      // velas cargadas en el ultimo Refresh()

   double            m_open[];
   double            m_high[];
   double            m_low[];
   double            m_close[];
   datetime          m_time[];
   double            m_ema_f[];
   double            m_ema_s[];
   double            m_rsi[];
   double            m_atr[];
   double            m_adx[];

   bool              CopySeries(const int handle,const int buffer,const int count,double &dst[]);
   double            TrendEmaAt(const datetime bar_time);
   bool              SessionAllowed(const datetime bar_time,string &why);

public:
                     CGoldSignalEngine(void);
                    ~CGoldSignalEngine(void);

   bool              Init(const string symbol,const ENUM_TIMEFRAMES tf,const GSSettings &cfg);
   void              Deinit(void);
   bool              Refresh(const int bars);
   bool              Evaluate(const int shift,GSResult &res);

   int               Loaded(void) const { return m_loaded; }
   GSSettings        Settings(void) const { return m_cfg; }
   int               MinBars(void) const;
   double            Ema(const int shift,const bool fast) const;
   double            Atr(const int shift) const;
   double            High(const int shift) const;
   double            Low(const int shift) const;
   string            Bias(const int shift);
  };

//+------------------------------------------------------------------+
CGoldSignalEngine::CGoldSignalEngine(void) : m_symbol(""),
                                             m_tf(PERIOD_CURRENT),
                                             m_point(0.0),
                                             m_digits(0),
                                             m_h_ema_fast(INVALID_HANDLE),
                                             m_h_ema_slow(INVALID_HANDLE),
                                             m_h_ema_trend(INVALID_HANDLE),
                                             m_h_rsi(INVALID_HANDLE),
                                             m_h_atr(INVALID_HANDLE),
                                             m_h_adx(INVALID_HANDLE),
                                             m_loaded(0)
  {
   GSDefaults(m_cfg);
  }
//+------------------------------------------------------------------+
CGoldSignalEngine::~CGoldSignalEngine(void)
  {
   Deinit();
  }
//+------------------------------------------------------------------+
//| Velas minimas necesarias para que todos los indicadores tengan    |
//| datos validos                                                     |
//+------------------------------------------------------------------+
int CGoldSignalEngine::MinBars(void) const
  {
   int need = MathMax(m_cfg.ema_slow,m_cfg.ema_fast);
   need = MathMax(need,m_cfg.rsi_period);
   need = MathMax(need,m_cfg.atr_period);
   need = MathMax(need,m_cfg.adx_period*3);
   return(need+10);
  }
//+------------------------------------------------------------------+
bool CGoldSignalEngine::Init(const string symbol,const ENUM_TIMEFRAMES tf,const GSSettings &cfg)
  {
   Deinit();

   m_symbol = (symbol=="" ? _Symbol : symbol);
   m_tf     = (tf==PERIOD_CURRENT ? (ENUM_TIMEFRAMES)Period() : tf);
   m_cfg    = cfg;
   m_point  = SymbolInfoDouble(m_symbol,SYMBOL_POINT);
   m_digits = (int)SymbolInfoInteger(m_symbol,SYMBOL_DIGITS);

   if(m_point<=0.0)
     {
      Print("GoldSignals: simbolo no disponible -> ",m_symbol);
      return(false);
     }

   m_h_ema_fast  = iMA(m_symbol,m_tf,m_cfg.ema_fast,0,MODE_EMA,PRICE_CLOSE);
   m_h_ema_slow  = iMA(m_symbol,m_tf,m_cfg.ema_slow,0,MODE_EMA,PRICE_CLOSE);
   m_h_ema_trend = iMA(m_symbol,m_cfg.trend_tf,m_cfg.trend_ema,0,MODE_EMA,PRICE_CLOSE);
   m_h_rsi       = iRSI(m_symbol,m_tf,m_cfg.rsi_period,PRICE_CLOSE);
   m_h_atr       = iATR(m_symbol,m_tf,m_cfg.atr_period);
   m_h_adx       = iADX(m_symbol,m_tf,m_cfg.adx_period);

   if(m_h_ema_fast==INVALID_HANDLE || m_h_ema_slow==INVALID_HANDLE ||
      m_h_ema_trend==INVALID_HANDLE || m_h_rsi==INVALID_HANDLE ||
      m_h_atr==INVALID_HANDLE || m_h_adx==INVALID_HANDLE)
     {
      Print("GoldSignals: no se pudieron crear los indicadores (error ",GetLastError(),")");
      Deinit();
      return(false);
     }

   ArraySetAsSeries(m_open,true);
   ArraySetAsSeries(m_high,true);
   ArraySetAsSeries(m_low,true);
   ArraySetAsSeries(m_close,true);
   ArraySetAsSeries(m_time,true);
   ArraySetAsSeries(m_ema_f,true);
   ArraySetAsSeries(m_ema_s,true);
   ArraySetAsSeries(m_rsi,true);
   ArraySetAsSeries(m_atr,true);
   ArraySetAsSeries(m_adx,true);

   return(true);
  }
//+------------------------------------------------------------------+
void CGoldSignalEngine::Deinit(void)
  {
   if(m_h_ema_fast!=INVALID_HANDLE)  { IndicatorRelease(m_h_ema_fast);  m_h_ema_fast=INVALID_HANDLE;  }
   if(m_h_ema_slow!=INVALID_HANDLE)  { IndicatorRelease(m_h_ema_slow);  m_h_ema_slow=INVALID_HANDLE;  }
   if(m_h_ema_trend!=INVALID_HANDLE) { IndicatorRelease(m_h_ema_trend); m_h_ema_trend=INVALID_HANDLE; }
   if(m_h_rsi!=INVALID_HANDLE)       { IndicatorRelease(m_h_rsi);       m_h_rsi=INVALID_HANDLE;       }
   if(m_h_atr!=INVALID_HANDLE)       { IndicatorRelease(m_h_atr);       m_h_atr=INVALID_HANDLE;       }
   if(m_h_adx!=INVALID_HANDLE)       { IndicatorRelease(m_h_adx);       m_h_adx=INVALID_HANDLE;       }
   m_loaded=0;
  }
//+------------------------------------------------------------------+
bool CGoldSignalEngine::CopySeries(const int handle,const int buffer,const int count,double &dst[])
  {
   ArraySetAsSeries(dst,true);
   int copied = CopyBuffer(handle,buffer,0,count,dst);
   return(copied==count);
  }
//+------------------------------------------------------------------+
//| Carga en memoria las ultimas `bars` velas y sus indicadores       |
//+------------------------------------------------------------------+
bool CGoldSignalEngine::Refresh(const int bars)
  {
   m_loaded = 0;

   int want = MathMax(bars,MinBars());
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

   if(!CopySeries(m_h_ema_fast,0,want,m_ema_f)) return(false);
   if(!CopySeries(m_h_ema_slow,0,want,m_ema_s)) return(false);
   if(!CopySeries(m_h_rsi,0,want,m_rsi))        return(false);
   if(!CopySeries(m_h_atr,0,want,m_atr))        return(false);
   if(!CopySeries(m_h_adx,0,want,m_adx))        return(false);   // buffer 0 = ADX principal

   m_loaded = want;
   return(true);
  }
//+------------------------------------------------------------------+
//| EMA del timeframe superior alineada a la vela indicada            |
//+------------------------------------------------------------------+
double CGoldSignalEngine::TrendEmaAt(const datetime bar_time)
  {
   int htf_shift = iBarShift(m_symbol,m_cfg.trend_tf,bar_time,false);
   if(htf_shift<0)
      return(0.0);

   double tmp[];
   ArraySetAsSeries(tmp,true);
   if(CopyBuffer(m_h_ema_trend,0,htf_shift,1,tmp)!=1)
      return(0.0);
   return(tmp[0]);
  }
//+------------------------------------------------------------------+
//| Filtro horario (horas del servidor del broker)                    |
//+------------------------------------------------------------------+
bool CGoldSignalEngine::SessionAllowed(const datetime bar_time,string &why)
  {
   MqlDateTime dt;
   TimeToStruct(bar_time,dt);

   if(m_cfg.skip_friday_close && dt.day_of_week==5 && dt.hour>=20)
     {
      why = "cierre de viernes";
      return(false);
     }
   if(dt.day_of_week==0 || dt.day_of_week==6)
     {
      why = "fin de semana";
      return(false);
     }
   if(!m_cfg.use_session)
      return(true);

   bool in1 = (m_cfg.session1_start>=0 &&
               dt.hour>=m_cfg.session1_start && dt.hour<m_cfg.session1_end);
   bool in2 = (m_cfg.session2_start>=0 &&
               dt.hour>=m_cfg.session2_start && dt.hour<m_cfg.session2_end);

   if(in1 || in2)
      return(true);

   why = StringFormat("fuera de sesion (%02d:xx servidor)",dt.hour);
   return(false);
  }
//+------------------------------------------------------------------+
double CGoldSignalEngine::Ema(const int shift,const bool fast) const
  {
   if(shift<0 || shift>=m_loaded)
      return(0.0);
   return(fast ? m_ema_f[shift] : m_ema_s[shift]);
  }
//+------------------------------------------------------------------+
double CGoldSignalEngine::Atr(const int shift) const
  {
   if(shift<0 || shift>=m_loaded)
      return(0.0);
   return(m_atr[shift]);
  }
//+------------------------------------------------------------------+
double CGoldSignalEngine::High(const int shift) const
  {
   if(shift<0 || shift>=m_loaded)
      return(0.0);
   return(m_high[shift]);
  }
//+------------------------------------------------------------------+
double CGoldSignalEngine::Low(const int shift) const
  {
   if(shift<0 || shift>=m_loaded)
      return(0.0);
   return(m_low[shift]);
  }
//+------------------------------------------------------------------+
//| Sesgo actual en texto legible, para el panel                      |
//+------------------------------------------------------------------+
string CGoldSignalEngine::Bias(const int shift)
  {
   if(shift<0 || shift>=m_loaded)
      return("sin datos");

   double trend = TrendEmaAt(m_time[shift]);
   if(trend<=0.0)
      return("sin datos");

   bool up   = (m_ema_f[shift]>m_ema_s[shift] && m_close[shift]>trend);
   bool down = (m_ema_f[shift]<m_ema_s[shift] && m_close[shift]<trend);

   if(up)   return("ALCISTA");
   if(down) return("BAJISTA");
   return("LATERAL");
  }
//+------------------------------------------------------------------+
//| Evalua la vela `shift`. shift>=1 => vela cerrada (no repinta).    |
//+------------------------------------------------------------------+
bool CGoldSignalEngine::Evaluate(const int shift,GSResult &res)
  {
   res.dir       = GS_NONE;
   res.raw_dir   = GS_NONE;
   res.bar_time  = 0;
   res.price     = 0.0;
   res.sl        = 0.0;
   res.tp1       = 0.0;
   res.tp2       = 0.0;
   res.atr       = 0.0;
   res.rsi       = 0.0;
   res.adx       = 0.0;
   res.ema_fast  = 0.0;
   res.ema_slow  = 0.0;
   res.ema_trend = 0.0;
   res.score     = 0;
   res.setup     = "";
   res.notes     = "";

   if(shift<0 || shift+2>=m_loaded)
      return(false);

   res.bar_time  = m_time[shift];
   res.ema_fast  = m_ema_f[shift];
   res.ema_slow  = m_ema_s[shift];
   res.rsi       = m_rsi[shift];
   res.atr       = m_atr[shift];
   res.adx       = m_adx[shift];
   res.ema_trend = TrendEmaAt(m_time[shift]);

   if(res.atr<=0.0 || res.ema_trend<=0.0)
      return(false);

   double c  = m_close[shift];
   double c1 = m_close[shift+1];

   //--- sesgo de fondo: EMAs alineadas + precio del lado correcto de la EMA superior
   bool bull_bias = (res.ema_fast>res.ema_slow && c>res.ema_trend);
   bool bear_bias = (res.ema_fast<res.ema_slow && c<res.ema_trend);
   if(!bull_bias && !bear_bias)
      return(false);

   //--- gatillo A: cruce de EMAs en esta misma vela
   bool cross_up   = (m_ema_f[shift]>m_ema_s[shift] && m_ema_f[shift+1]<=m_ema_s[shift+1]);
   bool cross_down = (m_ema_f[shift]<m_ema_s[shift] && m_ema_f[shift+1]>=m_ema_s[shift+1]);

   //--- gatillo B: retroceso a la EMA rapida y cierre de continuacion
   bool pull_up   = (bull_bias && m_low[shift]<=res.ema_fast && c>res.ema_fast && c>m_open[shift] && c>c1);
   bool pull_down = (bear_bias && m_high[shift]>=res.ema_fast && c<res.ema_fast && c<m_open[shift] && c<c1);

   if(bull_bias && (cross_up || pull_up))
     {
      res.raw_dir = GS_BUY;
      res.setup   = cross_up ? "cruce EMA al alza" : "pullback alcista a EMA";
     }
   else if(bear_bias && (cross_down || pull_down))
     {
      res.raw_dir = GS_SELL;
      res.setup   = cross_down ? "cruce EMA a la baja" : "pullback bajista a EMA";
     }
   else
      return(false);

   //--- niveles operativos derivados del ATR
   double atr_pts = res.atr/m_point;
   res.price = NormalizeDouble(c,m_digits);

   if(res.raw_dir==GS_BUY)
     {
      double swing = MathMin(m_low[shift],m_low[shift+1]);
      res.sl  = NormalizeDouble(MathMin(swing,c-m_cfg.sl_atr_mult*res.atr),m_digits);
      res.tp1 = NormalizeDouble(c+m_cfg.tp1_atr_mult*res.atr,m_digits);
      res.tp2 = NormalizeDouble(c+m_cfg.tp2_atr_mult*res.atr,m_digits);
     }
   else
     {
      double swing = MathMax(m_high[shift],m_high[shift+1]);
      res.sl  = NormalizeDouble(MathMax(swing,c+m_cfg.sl_atr_mult*res.atr),m_digits);
      res.tp1 = NormalizeDouble(c-m_cfg.tp1_atr_mult*res.atr,m_digits);
      res.tp2 = NormalizeDouble(c-m_cfg.tp2_atr_mult*res.atr,m_digits);
     }

   //--- filtros de calidad: cada bloqueo se explica en res.notes
   string blocks = "";

   if(res.adx<m_cfg.adx_min)
      blocks += StringFormat("ADX %.1f < %.1f; ",res.adx,m_cfg.adx_min);

   if(res.raw_dir==GS_BUY && res.rsi>m_cfg.rsi_max_buy)
      blocks += StringFormat("RSI %.1f sobrecomprado; ",res.rsi);
   if(res.raw_dir==GS_SELL && res.rsi<m_cfg.rsi_min_sell)
      blocks += StringFormat("RSI %.1f sobrevendido; ",res.rsi);

   if(m_cfg.min_atr_points>0.0 && atr_pts<m_cfg.min_atr_points)
      blocks += StringFormat("ATR %.0f pts < %.0f; ",atr_pts,m_cfg.min_atr_points);
   if(m_cfg.max_atr_points>0.0 && atr_pts>m_cfg.max_atr_points)
      blocks += StringFormat("ATR %.0f pts > %.0f; ",atr_pts,m_cfg.max_atr_points);

   string why_session = "";
   if(!SessionAllowed(m_time[shift],why_session))
      blocks += why_session+"; ";

   //--- el spread solo tiene sentido en tiempo real (ultima vela cerrada)
   if(shift<=1 && m_cfg.max_spread_points>0.0)
     {
      double spread_pts = (double)SymbolInfoInteger(m_symbol,SYMBOL_SPREAD);
      if(spread_pts>m_cfg.max_spread_points)
         blocks += StringFormat("spread %.0f > %.0f pts; ",spread_pts,m_cfg.max_spread_points);
     }

   //--- puntuacion de confianza
   int score = 50;
   if(res.adx>=m_cfg.adx_min)      score += 10;
   if(res.adx>=m_cfg.adx_min+10.0) score += 10;
   if(StringFind(res.setup,"pullback")>=0) score += 10;   // continuacion > cruce puro
   double dist_trend = MathAbs(c-res.ema_trend)/res.atr;
   if(dist_trend>=1.0) score += 10;
   if(res.raw_dir==GS_BUY  && res.rsi>50.0 && res.rsi<=m_cfg.rsi_max_buy)  score += 10;
   if(res.raw_dir==GS_SELL && res.rsi<50.0 && res.rsi>=m_cfg.rsi_min_sell) score += 10;
   res.score = MathMin(score,100);

   if(blocks!="")
     {
      res.notes = "filtrada: "+blocks;
      return(true);           // hubo gatillo, pero no se publica
     }
   if(res.score<m_cfg.min_score)
     {
      res.notes = StringFormat("filtrada: score %d < %d",res.score,m_cfg.min_score);
      return(true);
     }

   res.dir = res.raw_dir;
   return(true);
  }

#endif // GOLD_SIGNALS_SIGNAL_ENGINE_MQH
