# Memoria del proyecto

Este repositorio es también la memoria del proyecto. Ningún chat recuerda el
anterior, así que lo que no esté escrito aquí se pierde.

## Antes de tocar código, lee en este orden

1. `memoria/ESTADO.md` — en qué punto está el proyecto hoy
2. `memoria/DECISIONES.md` — qué se decidió y por qué. **No lo re-discutas.**
3. `memoria/PENDIENTE.md` — lo siguiente, por orden

## Al terminar la sesión, obligatorio

- Actualiza `memoria/ESTADO.md` con lo que haya cambiado.
- Si se ha tomado una decisión de diseño, añádela a `memoria/DECISIONES.md`
  con fecha y motivo. **Nunca borres entradas anteriores**, solo añade.
- Añade una entrada corta en `memoria/diario/AAAA-MM-DD.md`.
- Marca en `memoria/PENDIENTE.md` lo que se haya completado.

Escribe conclusiones, no transcripciones de la conversación.

## Reglas de trabajo

- **Nunca commits directos a `main`.** Rama por tarea y pull request.
- Una tarea por sesión. Si aparece algo más, va a `PENDIENTE.md`, no se hace.
- Si no hay forma automática de comprobar que algo funciona (test, build,
  linter), dilo en voz alta antes de darlo por bueno.
- Si una decisión contradice `DECISIONES.md`, pregunta antes de cambiarla.
- No añadas dependencias nuevas sin anotar el motivo en `DECISIONES.md`.

## Comandos propios

- `/siguiente` — coge la primera tarea pendiente y la lleva hasta el PR.
- `/cerrar-sesion` — actualiza la memoria antes de cerrar.
