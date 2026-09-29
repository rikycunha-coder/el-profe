# Arquitectura

## La app

> PENDIENTE de rellenar cuando el código llegue al repositorio (tarea 1).
> Debe responder a: qué tecnología usa, cómo se organiza el código, cómo se
> compila, cómo se arranca, y qué depende de qué.

- Tecnología:
- Estructura de carpetas:
- Comando de compilación:
- Comando de arranque:
- Dependencias externas:

## El sistema de automatización

Esto sí está decidido (ver `DECISIONES.md`), aunque se monte en la tarea 6.

```
Google Play
    │
    ├─ Real-time Developer Notifications ──► Pub/Sub ──► n8n ──► Telegram / email
    │      compras, renovaciones, cancelaciones, reembolsos, en tiempo real
    │
    ├─ Play Developer API ◄── consulta periódica desde n8n
    │      reseñas, valoraciones, estadísticas (no hay push para reseñas)
    │
    └─ Informes financieros ──► bucket de Cloud Storage
```

**Reparto de responsabilidades:**

| Capa | Herramienta | Por qué |
|---|---|---|
| Recibir eventos y avisar | n8n / Cloud Function | Determinista, barato, sin límites de cuota |
| Interpretar y redactar | Claude | Requiere criterio: resúmenes, respuestas, backlog |
| Guardar el estado | Este repositorio | Legible por cualquier chat y por Obsidian |

**Lo que Play no da:** los correos de los usuarios. Para enviar emails hay que
pedirlos en la app con consentimiento y usar un proveedor de envío.
