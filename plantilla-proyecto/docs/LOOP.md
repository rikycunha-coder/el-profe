# El loop de desarrollo

Cómo hacer que el proyecto avance solo, y con qué frenos.

## La idea

```
1. lee la memoria
2. coge la primera tarea pendiente
3. hazla en una rama nueva
4. comprueba que funciona
5. actualiza la memoria
6. abre un pull request y avisa
7. duerme hasta la próxima vuelta
```

El comando `/siguiente` hace los pasos 1 a 6. El loop solo repite la llamada.

**Sin memoria esto no funciona.** Un agente que se despierta y no sabe qué hizo
la vez anterior repite trabajo o rompe lo que ya estaba bien. Por eso los
archivos de `memoria/` no son documentación: son la condición para que el loop
sirva de algo.

## Tres formas de ejecutarlo

**A mano, una vuelta cada vez** — lo más sensato para empezar:

```
claude
> /siguiente
```

**Loop con intervalo**, dentro de Claude Code:

```
/loop 1d /siguiente
```

**Tarea programada**, en Claude Code web: una Routine con cron que dispare
`/siguiente` una vez al día en una sesión nueva. Útil porque no depende de que
tu ordenador esté encendido.

**Modo headless**, para engancharlo al Programador de tareas de Windows:

```powershell
claude -p "/siguiente"
```

## Los frenos, y por qué están

| Freno | Para qué |
|---|---|
| Rama + pull request siempre | Poder deshacer sin drama y revisar antes de fusionar |
| Una tarea por vuelta | Un error no se construye encima de otros cuatro |
| Prohibido fusionar el PR | El punto donde una persona decide |
| Parar si falta algo, en vez de improvisar | Evita que invente una solución al problema equivocado |
| Verificación automática obligatoria, o decirlo | Sin señal de éxito, el agente no sabe si acertó |

## El coste

Esto es lo que no suelen contar: **un loop consume mucho más que trabajar a
mano**, porque está trabajando todo el rato. Una vuelta diaria es asumible; una
cada hora agota un plan rápido.

Empieza con **una vuelta al día**. Sube la frecuencia solo cuando hayas visto
varios PR seguidos que no haya habido que rehacer.

## Cuándo no usar un loop

- Cuando la tarea siguiente necesita una decisión tuya. El agente la tomará por
  ti, y probablemente no como querías.
- Cuando no hay tests ni build comprobable. Estarás acumulando código que nadie
  ha verificado.
- Cuando el proyecto está en una fase de exploración, con el diseño abierto. Los
  loops son buenos ejecutando, no decidiendo qué construir.
