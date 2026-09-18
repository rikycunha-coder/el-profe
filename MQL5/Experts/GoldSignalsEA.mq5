//+------------------------------------------------------------------+
//|                                                 GoldSignalsEA.mq5 |
//|   Robot de senales de ORO (XAUUSD).                               |
//|                                                                   |
//|   Por defecto NO opera: solo emite las senales (popup, push al    |
//|   movil, Telegram, CSV). Si activas InpEnableTrading, ejecuta las |
//|   mismas senales con SL/TP y tamano por riesgo.                   |
//+------------------------------------------------------------------+
#property copyright "el-profe"
#property version   "1.00"
#property description "Senales XAUUSD en tiempo real, con ejecucion opcional."

#include <Trade/Trade.mqh>
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
input double           InpAdxMin         = 20.0;        // ADX minimo
input int              InpAtrPeriod      = 14;          // Periodo ATR
input double           InpMinAtrPoints   = 0.0;         // ATR minimo en puntos (0 = off)
input double           InpMaxAtrPoints   = 0.0;         // ATR maximo en puntos (0 = off)
input double           InpMaxSpreadPts   = 500.0;       // Spread maximo en puntos (0 = off)
input int              InpMinScore       = 60;          // Puntuacion minima (0-100)
//--- Horario
input group                "Horario (hora del servidor)"
input bool             InpUseSession     = true;        // Filtrar por sesion
input int              InpSession1Start  = 8;           // Sesion 1: hora inicio
input int              InpSession1End    = 12;          // Sesion 1: hora fin
input int              InpSession2Start  = 13;          // Sesion 2: hora inicio (-1 = off)
input int              InpSession2End    = 19;          // Sesion 2: hora fin
input bool             InpSkipFriday     = true;        // Evitar cierre del viernes
//--- Niveles
input group                "Niveles"
input double           InpSlAtrMult      = 1.5;         // Stop loss = N x ATR
input double           InpTp1AtrMult     = 1.5;         // Objetivo 1 = N x ATR
input double           InpTp2AtrMult     = 3.0;         // Objetivo 2 = N x ATR
//--- Avisos
input group                "Avisos"
input bool             InpAlertPopup     = true;        // Ventana de alerta
input bool             InpAlertPush      = true;        // Push al movil (MetaQuotes ID)
input bool             InpAlertEmail     = false;       // Email
input bool             InpLogCsv         = true;        // Guardar senales en CSV
input string           InpTgToken        = "";          // Telegram: token del bot (vacio = off)
input string           InpTgChatId       = "";          // Telegram: chat id
//--- Ejecucion
input group                "Ejecucion (opcional)"
input bool             InpEnableTrading  = false;       // Operar automaticamente
input double           InpRiskPercent    = 0.5;         // Riesgo por operacion (% del balance)
input double           InpFixedLots      = 0.0;         // Lotes fijos (>0 ignora el riesgo)
input bool             InpUseTp2         = true;        // Take profit en TP2 (si no, TP1)
input bool             InpBreakEven      = true;        // Mover a break-even en TP1
input bool             InpTrailAtr       = true;        // Trailing por ATR
input double           InpTrailAtrMult   = 1.5;         // Trailing = N x ATR
input int              InpMaxTradesDay   = 3;           // Maximo de operaciones por dia
input long             InpMagic          = 20260918;    // Numero magico
input int              InpSlippagePts    = 30;          // Deslizamiento maximo (puntos)

CTrade            g_trade;
CGoldSignalEngine g_engine;
datetime          g_last_bar        = 0;
datetime          g_last_signal_bar = 0;
int               g_trades_today    = 0;
int               g_today           = -1;

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
   GSSettings cfg;
   BuildSettings(cfg);
   if(!g_engine.Init(_Symbol,(ENUM_TIMEFRAMES)Period(),cfg))
      return(INIT_FAILED);

   g_trade.SetExpertMagicNumber(InpMagic);
   g_trade.SetDeviationInPoints(InpSlippagePts);
   g_trade.SetTypeFillingBySymbol(_Symbol);
   g_trade.SetAsyncMode(false);

   g_engine.Refresh(g_engine.MinBars()+50);   // datos listos desde el primer tick

   if(StringFind(_Symbol,"XAU")<0 && StringFind(_Symbol,"GOLD")<0)
      Print("Aviso: el EA esta pensado para oro (XAUUSD); simbolo actual -> ",_Symbol);

   PrintFormat("GoldSignalsEA iniciado en %s %s | operativa automatica: %s",
               _Symbol,EnumToString((ENUM_TIMEFRAMES)Period()),
               (InpEnableTrading ? "SI" : "NO (solo senales)"));
   return(INIT_SUCCEEDED);
  }
//+------------------------------------------------------------------+
void OnDeinit(const int reason)
  {
   g_engine.Deinit();
  }
//+------------------------------------------------------------------+
string DirText(const ENUM_GS_DIR d)
  {
   if(d==GS_BUY)  return("COMPRA");
   if(d==GS_SELL) return("VENTA");
   return("-");
  }
//+------------------------------------------------------------------+
//| Envio opcional a Telegram (requiere permitir la URL en el         |
//| terminal: Herramientas > Opciones > Asesores Expertos)            |
//+------------------------------------------------------------------+
void SendTelegram(const string text)
  {
   if(InpTgToken=="" || InpTgChatId=="")
      return;

   string url  = "https://api.telegram.org/bot"+InpTgToken+"/sendMessage";
   string body = "chat_id="+InpTgChatId+"&text="+text;
   char   post[];
   char   result[];
   string headers = "Content-Type: application/x-www-form-urlencoded\r\n";
   string result_headers;

   StringToCharArray(body,post,0,StringLen(body),CP_UTF8);
   ArrayResize(post,StringLen(body));

   ResetLastError();
   int code = WebRequest("POST",url,headers,5000,post,result,result_headers);
   if(code!=200)
      PrintFormat("Telegram fallo (http %d, error %d). Permite api.telegram.org en Opciones > Asesores Expertos.",
                  code,GetLastError());
  }
//+------------------------------------------------------------------+
void LogCsv(const GSResult &r)
  {
   if(!InpLogCsv)
      return;

   string file  = "GoldSignals_EA_"+_Symbol+".csv";
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
void Announce(const GSResult &r)
  {
   string head = StringFormat("%s %s %s | %s | score %d",
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
   if(InpAlertPush)  SendNotification(body);
   if(InpAlertEmail) SendMail("Senal "+_Symbol,body);
   SendTelegram(body);
   Print(body);
   LogCsv(r);
  }
//+------------------------------------------------------------------+
bool HasOpenPosition(void)
  {
   for(int i=PositionsTotal()-1;i>=0;i--)
     {
      ulong ticket = PositionGetTicket(i);
      if(ticket==0)
         continue;
      if(PositionGetString(POSITION_SYMBOL)==_Symbol &&
         PositionGetInteger(POSITION_MAGIC)==InpMagic)
         return(true);
     }
   return(false);
  }
//+------------------------------------------------------------------+
//| Lotes segun el riesgo en dinero y la distancia al stop            |
//+------------------------------------------------------------------+
double CalcLots(const double entry,const double sl)
  {
   double lot_min  = SymbolInfoDouble(_Symbol,SYMBOL_VOLUME_MIN);
   double lot_max  = SymbolInfoDouble(_Symbol,SYMBOL_VOLUME_MAX);
   double lot_step = SymbolInfoDouble(_Symbol,SYMBOL_VOLUME_STEP);

   if(InpFixedLots>0.0)
     {
      double fixed = MathMin(MathMax(InpFixedLots,lot_min),lot_max);
      return(NormalizeDouble(MathFloor(fixed/lot_step)*lot_step,2));
     }

   double dist = MathAbs(entry-sl);
   if(dist<=0.0)
      return(0.0);

   double tick_value = SymbolInfoDouble(_Symbol,SYMBOL_TRADE_TICK_VALUE);
   double tick_size  = SymbolInfoDouble(_Symbol,SYMBOL_TRADE_TICK_SIZE);
   if(tick_value<=0.0 || tick_size<=0.0)
      return(0.0);

   double risk_money = AccountInfoDouble(ACCOUNT_BALANCE)*InpRiskPercent/100.0;
   double loss_1lot  = (dist/tick_size)*tick_value;
   if(loss_1lot<=0.0)
      return(0.0);

   double lots = risk_money/loss_1lot;
   lots = MathFloor(lots/lot_step)*lot_step;
   lots = MathMin(MathMax(lots,lot_min),lot_max);

   if(lots<lot_min)
      return(0.0);
   return(NormalizeDouble(lots,2));
  }
//+------------------------------------------------------------------+
//| Respeta la distancia minima de stops del broker                   |
//+------------------------------------------------------------------+
bool ValidStops(const ENUM_GS_DIR dir,const double entry,double &sl,double &tp)
  {
   double point = SymbolInfoDouble(_Symbol,SYMBOL_POINT);
   double stops = (double)SymbolInfoInteger(_Symbol,SYMBOL_TRADE_STOPS_LEVEL)*point;
   if(stops<=0.0)
      stops = 10*point;

   if(dir==GS_BUY)
     {
      if(entry-sl < stops) sl = entry-stops;
      if(tp-entry < stops) tp = entry+stops;
      if(sl>=entry || tp<=entry) return(false);
     }
   else
     {
      if(sl-entry < stops) sl = entry+stops;
      if(entry-tp < stops) tp = entry-stops;
      if(sl<=entry || tp>=entry) return(false);
     }

   sl = NormalizeDouble(sl,_Digits);
   tp = NormalizeDouble(tp,_Digits);
   return(true);
  }
//+------------------------------------------------------------------+
void OpenTrade(const GSResult &r)
  {
   if(HasOpenPosition())
     {
      Print("Ya hay una posicion abierta del EA: se omite la entrada.");
      return;
     }
   if(InpMaxTradesDay>0 && g_trades_today>=InpMaxTradesDay)
     {
      Print("Limite diario de operaciones alcanzado.");
      return;
     }

   double entry = (r.dir==GS_BUY ? SymbolInfoDouble(_Symbol,SYMBOL_ASK)
                                 : SymbolInfoDouble(_Symbol,SYMBOL_BID));
   double sl    = r.sl;
   double tp    = (InpUseTp2 ? r.tp2 : r.tp1);

   if(!ValidStops(r.dir,entry,sl,tp))
     {
      Print("Niveles invalidos para el broker: entrada omitida.");
      return;
     }

   double lots = CalcLots(entry,sl);
   if(lots<=0.0)
     {
      Print("Lotaje calculado 0 (riesgo demasiado bajo o stop demasiado ancho): entrada omitida.");
      return;
     }

   string comment = StringFormat("GS %s s%d",r.setup,r.score);
   bool ok = (r.dir==GS_BUY) ? g_trade.Buy(lots,_Symbol,0.0,sl,tp,comment)
                             : g_trade.Sell(lots,_Symbol,0.0,sl,tp,comment);

   if(ok)
     {
      g_trades_today++;
      PrintFormat("Orden enviada: %s %.2f lotes  SL %s  TP %s",
                  DirText(r.dir),lots,DoubleToString(sl,_Digits),DoubleToString(tp,_Digits));
     }
   else
      PrintFormat("Error al enviar la orden: %d - %s",
                  g_trade.ResultRetcode(),g_trade.ResultRetcodeDescription());
  }
//+------------------------------------------------------------------+
//| Break-even y trailing sobre la posicion abierta                   |
//+------------------------------------------------------------------+
void ManagePosition(void)
  {
   if(!InpEnableTrading || (!InpBreakEven && !InpTrailAtr))
      return;

   for(int i=PositionsTotal()-1;i>=0;i--)
     {
      ulong ticket = PositionGetTicket(i);
      if(ticket==0)
         continue;
      if(PositionGetString(POSITION_SYMBOL)!=_Symbol ||
         PositionGetInteger(POSITION_MAGIC)!=InpMagic)
         continue;

      long   type  = PositionGetInteger(POSITION_TYPE);
      double open  = PositionGetDouble(POSITION_PRICE_OPEN);
      double sl    = PositionGetDouble(POSITION_SL);
      double tp    = PositionGetDouble(POSITION_TP);
      double price = (type==POSITION_TYPE_BUY ? SymbolInfoDouble(_Symbol,SYMBOL_BID)
                                              : SymbolInfoDouble(_Symbol,SYMBOL_ASK));
      double atr   = g_engine.Atr(1);
      if(atr<=0.0)
         continue;

      double new_sl = sl;

      if(InpBreakEven)
        {
         double trigger = InpTp1AtrMult*atr;
         if(type==POSITION_TYPE_BUY && price-open>=trigger && (sl<open || sl==0.0))
            new_sl = open;
         if(type==POSITION_TYPE_SELL && open-price>=trigger && (sl>open || sl==0.0))
            new_sl = open;
        }

      if(InpTrailAtr)
        {
         double dist = InpTrailAtrMult*atr;
         if(type==POSITION_TYPE_BUY)
           {
            double candidate = price-dist;
            if(candidate>new_sl && candidate>open)
               new_sl = candidate;
           }
         else
           {
            double candidate = price+dist;
            if((new_sl==0.0 || candidate<new_sl) && candidate<open)
               new_sl = candidate;
           }
        }

      new_sl = NormalizeDouble(new_sl,_Digits);
      if(new_sl!=NormalizeDouble(sl,_Digits) && new_sl>0.0)
        {
         if(!g_trade.PositionModify(ticket,new_sl,tp))
            PrintFormat("No se pudo mover el stop: %d - %s",
                        g_trade.ResultRetcode(),g_trade.ResultRetcodeDescription());
        }
     }
  }
//+------------------------------------------------------------------+
void ResetDailyCounter(void)
  {
   MqlDateTime dt;
   TimeToStruct(TimeCurrent(),dt);
   if(dt.day!=g_today)
     {
      g_today        = dt.day;
      g_trades_today = 0;
     }
  }
//+------------------------------------------------------------------+
void OnTick(void)
  {
   ResetDailyCounter();
   ManagePosition();

   //--- trabajar solo al cierre de vela: las senales no repintan
   datetime bar_time = iTime(_Symbol,(ENUM_TIMEFRAMES)Period(),0);
   if(bar_time==g_last_bar)
      return;
   g_last_bar = bar_time;

   if(!g_engine.Refresh(g_engine.MinBars()+50))
      return;

   GSResult r;
   if(!g_engine.Evaluate(1,r))
      return;
   if(r.dir==GS_NONE)
     {
      if(r.notes!="")
         Print("Gatillo descartado (",DirText(r.raw_dir),"): ",r.notes);
      return;
     }
   if(r.bar_time==g_last_signal_bar)
      return;

   g_last_signal_bar = r.bar_time;
   Announce(r);

   if(InpEnableTrading)
      OpenTrade(r);
  }
//+------------------------------------------------------------------+
