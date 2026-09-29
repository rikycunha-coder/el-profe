---
description: Coge la primera tarea pendiente del proyecto y la lleva hasta el pull request
---

Trabaja **una sola tarea**: la primera sin marcar de `memoria/PENDIENTE.md`.

## Pasos

1. Lee `memoria/ESTADO.md`, `memoria/DECISIONES.md` y `memoria/PENDIENTE.md`.
2. Identifica la primera tarea `[ ]`. **Dila antes de empezar**, para que se
   pueda frenar si no es la que toca.
3. Si esa tarea no se puede completar sin algo que no tienes —código que no está
   en el repositorio, una credencial, una decisión de negocio, una acción que
   solo puede hacer una persona— **no la improvises ni la sustituyas por otra**.
   Explica exactamente qué falta, anótalo en `ESTADO.md` y para ahí.
4. Crea una rama: `tarea/<descripcion-corta>`.
5. Haz solo esa tarea. Si por el camino aparece otro problema, apúntalo en la
   sección "Ideas sin fecha" de `PENDIENTE.md` y sigue con lo tuyo.
6. Comprueba que funciona con lo que haya en el proyecto: tests, compilación,
   linter. **Si no existe ninguna forma automática de comprobarlo, dilo
   claramente** en el resumen en lugar de darlo por bueno.
7. Actualiza la memoria:
   - marca la tarea `[x]` en `PENDIENTE.md`
   - reescribe la parte afectada de `ESTADO.md`
   - añade entrada en `memoria/diario/AAAA-MM-DD.md`
   - si se tomó una decisión de diseño, añádela a `DECISIONES.md` con fecha y motivo
8. Commit y **pull request**. Nunca directo a `main`.
9. Cierra con un resumen de tres líneas: qué se hizo, qué queda, y qué necesita
   que lo revise una persona antes de fusionar.

## Lo que no debes hacer

- Coger varias tareas "porque son rápidas".
- Reordenar `PENDIENTE.md` sin anotar el motivo.
- Contradecir algo de `DECISIONES.md` sin preguntar primero.
- Añadir dependencias nuevas sin dejar el motivo escrito.
- Fusionar tú el pull request.
