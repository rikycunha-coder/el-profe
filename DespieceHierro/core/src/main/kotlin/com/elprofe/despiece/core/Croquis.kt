package com.elprofe.despiece.core

data class Punto(val x: Float, val y: Float)

data class Circulo(val centro: Punto, val radio: Float)

enum class Alineacion { IZQUIERDA, CENTRO, DERECHA }

/** Texto centrado verticalmente en [posicion]. */
data class Etiqueta(val texto: String, val posicion: Punto, val alineacion: Alineacion)

/** Dibujo esquemático (no a escala) de una barra, en una caja de [Croquis.ANCHO] × [Croquis.ALTO]. */
data class Dibujo(
    val lineas: List<List<Punto>>,
    val circulos: List<Circulo> = emptyList(),
    val etiquetas: List<Etiqueta> = emptyList(),
)

/** Geometría común para dibujar la forma de las barras en pantalla y en el PDF. */
object Croquis {
    const val ANCHO = 120f
    const val ALTO = 50f

    fun de(b: Barra): Dibujo {
        val t = b.tramos
        return when (b.forma) {
            Forma.RECTA -> Dibujo(
                lineas = listOf(listOf(Punto(8f, 32f), Punto(112f, 32f))),
                etiquetas = listOf(Etiqueta("${t[0]}", Punto(60f, 21f), Alineacion.CENTRO)),
            )
            Forma.L -> Dibujo(
                lineas = listOf(listOf(Punto(10f, 46f), Punto(10f, 24f), Punto(112f, 24f))),
                etiquetas = listOf(
                    Etiqueta("${t[0]}", Punto(15f, 36f), Alineacion.IZQUIERDA),
                    Etiqueta("${t[1]}", Punto(62f, 13f), Alineacion.CENTRO),
                ),
            )
            Forma.U -> Dibujo(
                lineas = listOf(listOf(Punto(10f, 46f), Punto(10f, 24f), Punto(110f, 24f), Punto(110f, 46f))),
                etiquetas = listOf(
                    Etiqueta("${t[0]}", Punto(15f, 36f), Alineacion.IZQUIERDA),
                    Etiqueta("${t[1]}", Punto(60f, 13f), Alineacion.CENTRO),
                    Etiqueta("${t[2]}", Punto(105f, 36f), Alineacion.DERECHA),
                ),
            )
            Forma.ESTRIBO -> estribo(t[0].toFloat(), t[1].toFloat())
            Forma.TRABA -> Dibujo(
                lineas = listOf(listOf(Punto(16f, 22f), Punto(24f, 32f), Punto(96f, 32f), Punto(104f, 22f))),
                etiquetas = listOf(Etiqueta("${t[0]}", Punto(60f, 22f), Alineacion.CENTRO)),
            )
            Forma.CIRCULO -> Dibujo(
                lineas = listOf(
                    listOf(Punto(31f, 11f), Punto(39f, 19f)),
                    listOf(Punto(27f, 12f), Punto(35f, 20f)),
                ),
                circulos = listOf(Circulo(Punto(32f, 28f), 18f)),
                etiquetas = listOf(Etiqueta("Ø ${t[0]}", Punto(56f, 28f), Alineacion.IZQUIERDA)),
            )
        }
    }

    private fun estribo(a: Float, h: Float): Dibujo {
        val proporcion = if (h > 0f) a / h else 1f
        var alto = 30f
        var ancho = alto * proporcion
        if (ancho > 70f) {
            ancho = 70f
            alto = ancho / proporcion
        }
        ancho = ancho.coerceAtLeast(14f)
        alto = alto.coerceIn(12f, 30f)
        val x0 = 10f
        val y0 = 16f + (30f - alto) / 2f
        val x1 = x0 + ancho
        val y1 = y0 + alto
        return Dibujo(
            lineas = listOf(
                listOf(Punto(x0, y0), Punto(x1, y0), Punto(x1, y1), Punto(x0, y1), Punto(x0, y0)),
                listOf(Punto(x0 + 1.5f, y0 + 5f), Punto(x0 + 9f, y0 + 12.5f)),
                listOf(Punto(x0 + 5f, y0 + 1.5f), Punto(x0 + 12.5f, y0 + 9f)),
            ),
            etiquetas = listOf(
                Etiqueta("${a.toInt()}", Punto((x0 + x1) / 2f, y0 - 7f), Alineacion.CENTRO),
                Etiqueta("${h.toInt()}", Punto(x1 + 4f, (y0 + y1) / 2f), Alineacion.IZQUIERDA),
            ),
        )
    }
}
