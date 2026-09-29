# Decisiones

> Solo se añade, nunca se borra. Si una decisión se revierte, se añade una
> entrada nueva explicando por qué, dejando la antigua donde está.
>
> Este archivo existe para que ningún chat nuevo vuelva a proponer lo que ya
> se descartó, ni haga falta volver a discutirlo.

## 2026-09-29 — La memoria vive en el repositorio, no en un vault local

**Decisión:** los archivos de memoria van dentro del repositorio git del
proyecto.

**Por qué:** un vault de Obsidian en el disco solo lo ve el Claude de esa
máquina. En el repositorio lo lee cualquier chat, local o en la nube, y se
puede escribir en él sin consumir cuota. Obsidian se usa como editor de esa
misma carpeta, no como almacén.

**Alternativa descartada:** vault sincronizado por OneDrive. Además de ser
invisible para las sesiones en la nube, OneDrive y git en la misma carpeta se
pisan.

## 2026-09-29 — Plugin claude-mem desactivado

**Decisión:** desactivar `claude-mem@thedotmack` y `claude-mem-cowork`.

**Por qué:** guarda la memoria en su propia base SQLite con búsqueda
vectorial, que el usuario no puede leer ni editar; requiere Bun y uv; y
comprime con IA en cada sesión, lo que consume cuota. El objetivo era que la
memoria fueran notas legibles y editables.

**Cómo revertirlo:** poner sus valores a `true` en
`~/.claude/settings.json`. No se desinstaló nada.

## 2026-09-29 — Las notificaciones las hace n8n, no Claude

**Decisión:** el aviso de eventos (ventas, suscripciones, bajas, reembolsos)
lo monta n8n o una Cloud Function, disparado por Real-time Developer
Notifications de Google Play vía Pub/Sub.

**Por qué:** "si llega el evento X, manda el mensaje Y" es determinista y de
alto volumen. Un modelo de lenguaje ahí sale caro, es más lento, no garantiza
el mismo resultado y choca con los límites de cuota.

**Qué sí hace Claude:** lo que exige criterio o redacción — resumir las
reseñas de la semana, redactar respuestas, convertir quejas repetidas en
tareas de `PENDIENTE.md`, escribir las notas de versión.

## 2026-09-29 — El loop trabaja en rama y pull request, nunca en main

**Decisión:** cualquier ejecución automática crea rama propia y abre PR.

**Por qué:** un agente sin supervisión se equivoca, y los errores se
construyen unos encima de otros. El PR es el punto donde un humano puede
frenar, y git el que permite deshacer.

## 2026-09-29 — Las notificaciones son el último paso, no el primero

**Decisión:** no montar el sistema de avisos hasta que la app esté publicada y
vendiendo.

**Por qué:** no hay eventos que notificar antes de que existan ventas. El
orden está en `PENDIENTE.md`.
