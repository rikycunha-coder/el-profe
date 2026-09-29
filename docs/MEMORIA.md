# Memoria de proyecto entre chats

Cómo conseguir que cualquier chat de Claude retome un proyecto sabiendo dónde
se quedó, sin depender de que "recuerde" nada.

## El principio

Claude no recuerda entre conversaciones. Cada chat empieza en blanco, y ningún
chat puede leer otro. Por tanto "memoria" no significa recordar: significa
**dejar archivos que el siguiente chat lea al empezar**.

Para que los lean *todos* los chats —los de tu PC y los de la nube— esos
archivos tienen que estar en un **repositorio git**. Un vault de Obsidian en tu
disco solo lo ve el Claude de esa máquina.

```
repositorio git ──► cualquier chat, cualquier máquina
      │
      └── es también tu vault de Obsidian (misma carpeta)
```

Obsidian es el editor cómodo; git es el transporte; el repo es la memoria.

## Estructura

```
CLAUDE.md                  el disparador: Claude Code lo lee automáticamente
memoria/
  ESTADO.md                dónde está el proyecto hoy. Se reescribe entero
  DECISIONES.md            qué se decidió y por qué. Solo se añade, nunca se borra
  ARQUITECTURA.md          cómo está construido
  PENDIENTE.md             lo siguiente, ordenado
  diario/2026-09-29.md     una entrada corta por sesión
```

La más valiosa con diferencia es `DECISIONES.md`. Sin ella, cada chat nuevo te
vuelve a proponer lo que ya descartaste, y te toca discutirlo otra vez.

## El archivo que lo activa

`CLAUDE.md` en la raíz del repositorio. Claude Code lo carga solo al abrir el
proyecto, así que es el único sitio donde una instrucción se cumple sin que
tengas que recordarla tú:

```markdown
# Memoria del proyecto

Antes de tocar código, lee en este orden:

1. `memoria/ESTADO.md` — en qué punto está el proyecto
2. `memoria/DECISIONES.md` — qué se decidió y por qué (no lo re-discutas)
3. `memoria/PENDIENTE.md` — lo siguiente

## Al terminar la sesión, obligatorio

- Actualiza `ESTADO.md` con lo que haya cambiado.
- Si se ha tomado una decisión de diseño, añádela a `DECISIONES.md` con fecha
  y motivo. Nunca borres entradas anteriores.
- Añade una entrada corta en `memoria/diario/AAAA-MM-DD.md`.

Escribe conclusiones, no transcripciones de la conversación.
```

Esa última línea es importante: volcar el historial de un chat entero llena el
contexto de ruido y empeora el resultado. Lo que sirve es el estado y los
porqués.

## Sacar un proyecto que solo existe en un chat

Si el proyecto vive dentro de una conversación, hay que exportarlo. Ese chat es
el único que puede hacerlo, porque es el único que lo conoce. Pégale esto:

```text
Quiero dejar este proyecto preparado para poder seguirlo desde cualquier otro
chat, porque cada conversación empieza sin memoria.

1. Sube todo el código de la app a un repositorio nuevo de GitHub.
   Si no tienes acceso a GitHub, dame los archivos para descargarlos y lo subo yo.

2. Crea además estos archivos, rellenados con lo que sabes del proyecto por
   nuestra conversación:

   - CLAUDE.md — que indique leer memoria/ESTADO.md, memoria/DECISIONES.md y
     memoria/PENDIENTE.md antes de tocar código, y actualizarlos al terminar
     cada sesión.
   - memoria/ESTADO.md — qué funciona, qué está a medias, qué está roto.
   - memoria/DECISIONES.md — cada decisión de diseño tomada, con fecha y el
     motivo por el que se eligió eso y no la alternativa.
   - memoria/ARQUITECTURA.md — cómo está montada la app y qué hace cada parte.
   - memoria/PENDIENTE.md — lo siguiente a hacer, ordenado por prioridad.

   Conclusiones, no transcripción de la conversación.

3. Dime el nombre del repositorio cuando esté subido.
```

Con el nombre del repo, cualquier chat posterior arranca sabiendo dónde está
todo.

## Lo que esto no resuelve

- **No se mantiene solo.** Funciona si al cerrar cada sesión se actualiza el
  estado. La instrucción va dentro de `CLAUDE.md` para que cada chat se lo
  exija a sí mismo, pero cuenta con revisarlo tú de vez en cuando.
- **La calidad de la memoria es la calidad de lo escrito.** Un `ESTADO.md`
  vago da un arranque vago.
- **Obsidian y OneDrive en la misma carpeta se pisan.** Si el vault va a ser
  repositorio git, mejor sacarlo de OneDrive y dejar que git haga la
  sincronización.
