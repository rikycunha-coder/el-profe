# Pendiente

> Ordenado por dependencia: cada bloque necesita el anterior. Marca `[x]` al
> completar. No reordenes sin anotar el motivo en `DECISIONES.md`.
>
> Regla del loop: coge **la primera tarea sin marcar** y solo esa.

## 1. Traer el código al repositorio  ← BLOQUEA TODO LO DEMÁS

- [ ] Exportar la app desde la conversación donde está, a este repositorio
- [ ] Comprobar que el repositorio contiene todo: código, recursos, dependencias
- [ ] Rellenar `ARQUITECTURA.md` con lo que hay realmente
- [ ] Reescribir `ESTADO.md` con la situación real del código

## 2. Que compile y arranque

- [ ] Compilar en local sin errores
- [ ] Arrancar en un emulador o dispositivo
- [ ] Anotar en `ARQUITECTURA.md` los comandos exactos de compilación y arranque
- [ ] Añadir algo que verifique automáticamente que sigue funcionando
      (test, aunque sea uno, o al menos que el build sea comprobable)

## 3. Cuenta de Google Play y prueba cerrada

> El paso más lento en calendario. Conviene empezarlo en cuanto el paso 2 esté
> listo, porque el reloj de los 14 días corre solo.

- [ ] Crear la cuenta de desarrollador (25 USD, pago único)
- [ ] Decidir si la cuenta es personal o de empresa
      (las personales creadas después del 13/11/2023 tienen el requisito de
      los 12 testers; las de empresa están exentas)
- [ ] Preparar la ficha de Play: nombre, descripción, capturas, icono,
      política de privacidad
- [ ] Subir la primera versión a prueba cerrada
- [ ] Reunir 12 testers y mantenerlos 14 días seguidos
      (si uno se va antes de los 14 días, no cuenta)
- [ ] Solicitar acceso a producción

## 4. Play Billing (solo si va a haber suscripciones o compras)

- [ ] Integrar la biblioteca de Play Billing en la app
- [ ] Definir los productos y planes en Play Console
- [ ] Probar compras con cuentas de prueba

## 5. Publicar en producción

- [ ] Revisar los avisos de la ficha antes de enviar
- [ ] Publicar
- [ ] Comprobar que la app se instala desde la tienda

## 6. Sistema de notificaciones

> Ahora sí: ya hay eventos que notificar.

- [ ] Crear el proyecto de Google Cloud y el tema de Pub/Sub
- [ ] Activar Real-time Developer Notifications en Play Console apuntando al tema
- [ ] Montar n8n (autoalojado o en la nube)
- [ ] Flujo: Pub/Sub → filtrar por tipo de evento → Telegram y/o email
- [ ] Eventos mínimos: compra nueva, renovación, cancelación, reembolso,
      entrada en periodo de gracia
- [ ] Flujo aparte, cada pocas horas: consultar reseñas nuevas por la
      Play Developer API (no hay push para reseñas)
- [ ] Resumen diario: ingresos, instalaciones, altas y bajas

## 7. Claude sobre las reseñas

- [ ] Volcado semanal de reseñas a una nota del repositorio
- [ ] Resumen con temas repetidos y reseñas que piden respuesta urgente
- [ ] Convertir las quejas recurrentes en tareas de este archivo
- [ ] Borradores de respuesta para las reseñas negativas (los envías tú)

## Ideas sin fecha

> Aquí van las cosas que aparecen a mitad de otra tarea y no deben hacerse
> ahora. Sacarlas de aquí solo cuando toque.

- Emails a suscriptores: requiere pedir el email en la app con consentimiento y
  un proveedor de envío (Play no da los correos de los usuarios)
- Panel en Obsidian con Dataview sobre las notas de ventas y reseñas
- Versión para iOS
