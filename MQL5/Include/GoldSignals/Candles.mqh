//+------------------------------------------------------------------+
//|                                                       Candles.mqh |
//|   Deteccion de patrones de velas japonesas.                       |
//|                                                                   |
//|   Todas las funciones trabajan sobre arrays en modo serie         |
//|   (indice 0 = vela actual) y normalizan los tamanos con el ATR,   |
//|   de modo que valen igual en M5 que en H4.                        |
//+------------------------------------------------------------------+
#ifndef GOLD_SIGNALS_CANDLES_MQH
#define GOLD_SIGNALS_CANDLES_MQH

enum ENUM_CANDLE_PATTERN
  {
   CP_NONE = 0,
   CP_MORNING_STAR,      // estrella del amanecer
   CP_EVENING_STAR,      // estrella del atardecer
   CP_3_SOLDIERS,        // tres soldados blancos
   CP_3_CROWS,           // tres cuervos negros
   CP_ENGULF_BULL,       // envolvente alcista
   CP_ENGULF_BEAR,       // envolvente bajista
   CP_PIERCING,          // linea penetrante
   CP_DARK_CLOUD,        // nube oscura
   CP_HAMMER,            // martillo
   CP_HANGING_MAN,       // hombre colgado
   CP_SHOOTING_STAR,     // estrella fugaz
   CP_INV_HAMMER,        // martillo invertido
   CP_PIN_BULL,          // pin bar alcista
   CP_PIN_BEAR,          // pin bar bajista
   CP_DRAGONFLY,         // doji libelula
   CP_GRAVESTONE,        // doji lapida
   CP_DOJI,              // doji
   CP_HARAMI_BULL,       // harami alcista
   CP_HARAMI_BEAR,       // harami bajista
   CP_TWEEZER_BOTTOM,    // pinzas inferiores
   CP_TWEEZER_TOP,       // pinzas superiores
   CP_MARUBOZU_BULL,     // marubozu alcista
   CP_MARUBOZU_BEAR,     // marubozu bajista
   CP_OUTSIDE_BULL,      // vela exterior alcista
   CP_OUTSIDE_BEAR,      // vela exterior bajista
   CP_INSIDE_BAR         // vela interior
  };

struct CandleHit
  {
   ENUM_CANDLE_PATTERN pattern;
   int               dir;        // +1 alcista, -1 bajista, 0 neutro
   int               strength;   // 1 = leve, 2 = media, 3 = fuerte
   string            name;       // nombre largo
   string            tag;        // etiqueta corta para el grafico
  };

//+------------------------------------------------------------------+
string CandleName(const ENUM_CANDLE_PATTERN p)
  {
   switch(p)
     {
      case CP_MORNING_STAR:   return("Estrella del amanecer");
      case CP_EVENING_STAR:   return("Estrella del atardecer");
      case CP_3_SOLDIERS:     return("Tres soldados blancos");
      case CP_3_CROWS:        return("Tres cuervos negros");
      case CP_ENGULF_BULL:    return("Envolvente alcista");
      case CP_ENGULF_BEAR:    return("Envolvente bajista");
      case CP_PIERCING:       return("Linea penetrante");
      case CP_DARK_CLOUD:     return("Nube oscura");
      case CP_HAMMER:         return("Martillo");
      case CP_HANGING_MAN:    return("Hombre colgado");
      case CP_SHOOTING_STAR:  return("Estrella fugaz");
      case CP_INV_HAMMER:     return("Martillo invertido");
      case CP_PIN_BULL:       return("Pin bar alcista");
      case CP_PIN_BEAR:       return("Pin bar bajista");
      case CP_DRAGONFLY:      return("Doji libelula");
      case CP_GRAVESTONE:     return("Doji lapida");
      case CP_DOJI:           return("Doji");
      case CP_HARAMI_BULL:    return("Harami alcista");
      case CP_HARAMI_BEAR:    return("Harami bajista");
      case CP_TWEEZER_BOTTOM: return("Pinzas inferiores");
      case CP_TWEEZER_TOP:    return("Pinzas superiores");
      case CP_MARUBOZU_BULL:  return("Marubozu alcista");
      case CP_MARUBOZU_BEAR:  return("Marubozu bajista");
      case CP_OUTSIDE_BULL:   return("Exterior alcista");
      case CP_OUTSIDE_BEAR:   return("Exterior bajista");
      case CP_INSIDE_BAR:     return("Vela interior");
     }
   return("");
  }
//+------------------------------------------------------------------+
string CandleTag(const ENUM_CANDLE_PATTERN p)
  {
   switch(p)
     {
      case CP_MORNING_STAR:   return("AMANECER");
      case CP_EVENING_STAR:   return("ATARDECER");
      case CP_3_SOLDIERS:     return("3 SOLDADOS");
      case CP_3_CROWS:        return("3 CUERVOS");
      case CP_ENGULF_BULL:    return("ENVOLV+");
      case CP_ENGULF_BEAR:    return("ENVOLV-");
      case CP_PIERCING:       return("PENETRANTE");
      case CP_DARK_CLOUD:     return("NUBE OSCURA");
      case CP_HAMMER:         return("MARTILLO");
      case CP_HANGING_MAN:    return("COLGADO");
      case CP_SHOOTING_STAR:  return("FUGAZ");
      case CP_INV_HAMMER:     return("MARTILLO INV");
      case CP_PIN_BULL:       return("PIN+");
      case CP_PIN_BEAR:       return("PIN-");
      case CP_DRAGONFLY:      return("LIBELULA");
      case CP_GRAVESTONE:     return("LAPIDA");
      case CP_DOJI:           return("DOJI");
      case CP_HARAMI_BULL:    return("HARAMI+");
      case CP_HARAMI_BEAR:    return("HARAMI-");
      case CP_TWEEZER_BOTTOM: return("PINZAS+");
      case CP_TWEEZER_TOP:    return("PINZAS-");
      case CP_MARUBOZU_BULL:  return("MARUBOZU+");
      case CP_MARUBOZU_BEAR:  return("MARUBOZU-");
      case CP_OUTSIDE_BULL:   return("EXT+");
      case CP_OUTSIDE_BEAR:   return("EXT-");
      case CP_INSIDE_BAR:     return("INTERIOR");
     }
   return("");
  }
//+------------------------------------------------------------------+
void CandleFill(CandleHit &hit,const ENUM_CANDLE_PATTERN p,const int dir,const int strength)
  {
   hit.pattern  = p;
   hit.dir      = dir;
   hit.strength = strength;
   hit.name     = CandleName(p);
   hit.tag      = CandleTag(p);
  }
//+------------------------------------------------------------------+
//| Detecta el patron de la vela `s`.                                 |
//|                                                                   |
//|  ctx: contexto de tendencia inmediata (+1 subiendo, -1 bajando,   |
//|       0 sin definir). Cambia la lectura de las velas de una sola  |
//|       barra: un martillo en caida es giro alcista, la misma vela  |
//|       en subida es un hombre colgado.                             |
//|  atr: ATR de la vela, para medir cuerpos "grandes" o "pequenos".  |
//|                                                                   |
//| Devuelve el primer patron que encaja, de mas fuerte a mas debil.  |
//+------------------------------------------------------------------+
bool CandleAt(const double &o[],const double &h[],const double &l[],const double &c[],
              const int s,const int ctx,const double atr,CandleHit &hit)
  {
   CandleFill(hit,CP_NONE,0,0);

   int total = ArraySize(c);
   if(s<0 || s+3>=total || atr<=0.0)
      return(false);

   double range = h[s]-l[s];
   if(range<=0.0)
      return(false);

   double body  = MathAbs(c[s]-o[s]);
   double upper = h[s]-MathMax(c[s],o[s]);
   double lower = MathMin(c[s],o[s])-l[s];
   bool   bull  = (c[s]>o[s]);
   bool   bear  = (c[s]<o[s]);

   double body1 = MathAbs(c[s+1]-o[s+1]);
   bool   bull1 = (c[s+1]>o[s+1]);
   bool   bear1 = (c[s+1]<o[s+1]);

   double body2 = MathAbs(c[s+2]-o[s+2]);
   bool   bull2 = (c[s+2]>o[s+2]);
   bool   bear2 = (c[s+2]<o[s+2]);

   double mid2  = (o[s+2]+c[s+2])/2.0;
   double mid1  = (o[s+1]+c[s+1])/2.0;

   //--- 1) Estrellas de tres velas: giro mayor
   if(bear2 && body2>0.5*atr && body1<0.45*body2 && bull && body>0.5*body2 && c[s]>mid2)
     {
      CandleFill(hit,CP_MORNING_STAR,1,3);
      return(true);
     }
   if(bull2 && body2>0.5*atr && body1<0.45*body2 && bear && body>0.5*body2 && c[s]<mid2)
     {
      CandleFill(hit,CP_EVENING_STAR,-1,3);
      return(true);
     }

   //--- 2) Tres velas consecutivas en la misma direccion
   if(bull && bull1 && bull2 &&
      c[s]>c[s+1] && c[s+1]>c[s+2] &&
      body>0.3*atr && body1>0.3*atr && body2>0.3*atr &&
      o[s]>o[s+1] && o[s]<c[s+1])
     {
      CandleFill(hit,CP_3_SOLDIERS,1,3);
      return(true);
     }
   if(bear && bear1 && bear2 &&
      c[s]<c[s+1] && c[s+1]<c[s+2] &&
      body>0.3*atr && body1>0.3*atr && body2>0.3*atr &&
      o[s]<o[s+1] && o[s]>c[s+1])
     {
      CandleFill(hit,CP_3_CROWS,-1,3);
      return(true);
     }

   //--- 3) Envolventes
   if(bear1 && bull && c[s]>=o[s+1] && o[s]<=c[s+1] && body>body1 && body>0.35*atr)
     {
      CandleFill(hit,CP_ENGULF_BULL,1,(body>atr ? 3 : 2));
      return(true);
     }
   if(bull1 && bear && c[s]<=o[s+1] && o[s]>=c[s+1] && body>body1 && body>0.35*atr)
     {
      CandleFill(hit,CP_ENGULF_BEAR,-1,(body>atr ? 3 : 2));
      return(true);
     }

   //--- 4) Penetrante / nube oscura
   if(bear1 && bull && body1>0.5*atr && o[s]<c[s+1] && c[s]>mid1 && c[s]<o[s+1])
     {
      CandleFill(hit,CP_PIERCING,1,2);
      return(true);
     }
   if(bull1 && bear && body1>0.5*atr && o[s]>c[s+1] && c[s]<mid1 && c[s]>o[s+1])
     {
      CandleFill(hit,CP_DARK_CLOUD,-1,2);
      return(true);
     }

   //--- 5) Velas de sombra larga (martillo, fugaz, pin bar)
   bool hammer_shape = (lower>=0.6*range && upper<=0.25*range && body<=0.35*range);
   bool star_shape   = (upper>=0.6*range && lower<=0.25*range && body<=0.35*range);

   if(hammer_shape && range>0.5*atr)
     {
      if(ctx<0)       { CandleFill(hit,CP_HAMMER,1,3);      return(true); }
      if(ctx>0)       { CandleFill(hit,CP_HANGING_MAN,-1,2); return(true); }
      CandleFill(hit,CP_PIN_BULL,1,2);
      return(true);
     }
   if(star_shape && range>0.5*atr)
     {
      if(ctx>0)       { CandleFill(hit,CP_SHOOTING_STAR,-1,3); return(true); }
      if(ctx<0)       { CandleFill(hit,CP_INV_HAMMER,1,2);     return(true); }
      CandleFill(hit,CP_PIN_BEAR,-1,2);
      return(true);
     }

   //--- 6) Familia doji
   if(body<=0.1*range)
     {
      if(lower>=0.65*range) { CandleFill(hit,CP_DRAGONFLY,(ctx<0?1:0),2);  return(true); }
      if(upper>=0.65*range) { CandleFill(hit,CP_GRAVESTONE,(ctx>0?-1:0),2); return(true); }
      CandleFill(hit,CP_DOJI,0,1);
      return(true);
     }

   //--- 7) Harami (indecision tras vela grande)
   if(body1>0.6*atr && body<0.5*body1 &&
      MathMax(c[s],o[s])<=MathMax(c[s+1],o[s+1]) &&
      MathMin(c[s],o[s])>=MathMin(c[s+1],o[s+1]))
     {
      if(bear1 && bull) { CandleFill(hit,CP_HARAMI_BULL,1,2);  return(true); }
      if(bull1 && bear) { CandleFill(hit,CP_HARAMI_BEAR,-1,2); return(true); }
     }

   //--- 8) Pinzas (doble rechazo del mismo nivel)
   if(MathAbs(l[s]-l[s+1])<=0.12*atr && ctx<0 && bull && bear1)
     {
      CandleFill(hit,CP_TWEEZER_BOTTOM,1,2);
      return(true);
     }
   if(MathAbs(h[s]-h[s+1])<=0.12*atr && ctx>0 && bear && bull1)
     {
      CandleFill(hit,CP_TWEEZER_TOP,-1,2);
      return(true);
     }

   //--- 9) Marubozu (cuerpo pleno, continuacion)
   if(body>=0.9*range && body>0.6*atr)
     {
      CandleFill(hit,(bull ? CP_MARUBOZU_BULL : CP_MARUBOZU_BEAR),(bull ? 1 : -1),2);
      return(true);
     }

   //--- 10) Exterior / interior
   if(h[s]>h[s+1] && l[s]<l[s+1])
     {
      CandleFill(hit,(bull ? CP_OUTSIDE_BULL : CP_OUTSIDE_BEAR),(bull ? 1 : -1),2);
      return(true);
     }
   if(h[s]<=h[s+1] && l[s]>=l[s+1])
     {
      CandleFill(hit,CP_INSIDE_BAR,0,1);
      return(true);
     }

   return(false);
  }

#endif // GOLD_SIGNALS_CANDLES_MQH
