package com.elprofe.despiece.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max

/*
 * Unidades de los datos de entrada:
 *  - largos de elementos (luces, alturas, largos de muro/losa): metros
 *  - secciones, espesores, recubrimientos, separaciones, patas y anclajes: centímetros
 *  - diámetros de barra: milímetros
 *
 * Un campo de pata con valor null significa "automática" (Ajustes.pataDiametros × Ø).
 */

/** Norma de referencia: fija los largos de pata, gancho y traslapo por defecto. */
enum class Norma(
    val etiqueta: String,
    val detalle: String,
    val pataDiametros: Double,
    val ganchoDiametros: Double,
    val empalmeDiametros: Double,
    val empalmeGruesoDiametros: Double,
    val acero: String,
) {
    ACI_318(
        "ACI 318",
        "Pata de gancho a 90°: 12Ø. Ganchos de estribos y trabas a 135°: 6Ø (mín. 7,5 cm). " +
            "Traslapo clase B = 1,3·ld para acero fy 420 MPa y f'c 25 MPa: 52Ø hasta Ø20, 65Ø desde Ø22. " +
            "Base de NCh 430, NSR-10, E.060, CIRSOC 201 y NTC.",
        12.0, 6.0, 52.0, 65.0, "Acero corrugado grado 60 (fy 420 MPa)",
    ),
    EUROCODIGO_2(
        "Eurocódigo 2",
        "Patilla a 90°: 10Ø. Ganchos de cercos a 135°: 10Ø (mín. 7,5 cm). " +
            "Solape l0 = 1,5·lb,rqd para B500S, C25/30 y buena adherencia: 60Ø. " +
            "Base del Código Estructural y de la EHE-08.",
        10.0, 10.0, 60.0, 60.0, "Acero corrugado B500S",
    ),
    PROPIA(
        "Criterio propio",
        "Valores definidos a mano en esta obra.",
        12.0, 10.0, 50.0, 50.0, "Acero corrugado",
    ),
}

@Serializable
data class Ajustes(
    /** Norma con la que se fijaron los valores (PROPIA si se cambiaron a mano). */
    val norma: Norma = Norma.PROPIA,
    /** Material principal, p. ej. "B500S" o "A630-420H". */
    val acero: String = Norma.PROPIA.acero,
    /** Largo de la barra comercial, en metros. */
    val largoComercial: Double = 12.0,
    /** Largo de traslapo (empalme por solape) en número de diámetros, para Ø ≤ 20 mm. */
    val empalmeDiametros: Double = 50.0,
    /** Largo de traslapo en número de diámetros para Ø ≥ 22 mm (null = el mismo que para Ø ≤ 20). */
    val empalmeGruesoDiametros: Double? = null,
    /** Largo de la pata automática en número de diámetros. */
    val pataDiametros: Double = 12.0,
    /** Largo de cada gancho de estribos y trabas en número de diámetros (mínimo 7,5 cm). */
    val ganchoDiametros: Double = 10.0,
    /** Resta el alargamiento por doblado: 2Ø por cada doblez a 90°. */
    val descontarDoblado: Boolean = false,
    /** Margen de desperdicio adicional para la compra, en %. Se informa aparte del neto. */
    val margenDesperdicio: Double = 0.0,
) {
    val largoComercialCm: Int get() = max(100, redondear(largoComercial * 100))

    fun empalme(diametro: Int): Int {
        val factor = if (diametro >= DIAMETRO_GRUESO) empalmeGruesoDiametros ?: empalmeDiametros else empalmeDiametros
        return redondearArriba(factor * diametro / 10.0)
    }

    fun pataAutomatica(diametro: Int): Int = redondearArriba(pataDiametros * diametro / 10.0)

    fun gancho(diametro: Int): Int =
        redondearArriba(max(ganchoDiametros * diametro / 10.0, GANCHO_MINIMO_CM))

    /** Pata en cm: null = automática, 0 = sin pata. */
    fun pata(valor: Double?, diametro: Int): Int = valor?.let { redondear(it) } ?: pataAutomatica(diametro)

    /** Centímetros que se restan por cada doblez a 90° (0 si no se descuenta el doblado). */
    fun descuentoPorDoblez(diametro: Int): Int = if (descontarDoblado) redondear(2.0 * diametro / 10.0) else 0

    /** Aplica los valores de una norma, conservando el largo comercial, el margen y el descuento por doblado. */
    fun conNorma(n: Norma): Ajustes = copy(
        norma = n,
        acero = if (acero.isBlank() || Norma.entries.any { it.acero == acero }) n.acero else acero,
        pataDiametros = n.pataDiametros,
        ganchoDiametros = n.ganchoDiametros,
        empalmeDiametros = n.empalmeDiametros,
        empalmeGruesoDiametros = n.empalmeGruesoDiametros,
    )

    companion object {
        const val GANCHO_MINIMO_CM = 7.5
        const val DIAMETRO_GRUESO = 22

        fun de(n: Norma): Ajustes = Ajustes().conNorma(n)
    }
}

enum class TipoElemento(val etiqueta: String, val plural: String, val inicial: String, val descripcion: String) {
    LOSA("Losa", "losas", "L", "Mallas inferior y superior en dos direcciones"),
    VIGA("Viga", "vigas", "V", "Barras inferiores, superiores, de piel y estribos"),
    MURO("Muro", "muros", "M", "Barras verticales, horizontales y trabas"),
    PILAR("Pilar", "pilares", "P", "Barras longitudinales, piel, estribos o zunchos");

    fun crear(nombre: String): Elemento = when (this) {
        LOSA -> Losa(nombre = nombre)
        VIGA -> Viga(nombre = nombre)
        MURO -> Muro(nombre = nombre)
        PILAR -> Pilar(nombre = nombre)
    }
}

@Serializable
sealed class Elemento {
    abstract val id: String
    abstract val nombre: String
    /** Número de elementos iguales. */
    abstract val cantidad: Int
    abstract val extras: List<BarraExtra>
    abstract val tipo: TipoElemento
}

/** Copia cualquier elemento cambiando sus datos comunes. */
fun Elemento.con(
    id: String = this.id,
    nombre: String = this.nombre,
    cantidad: Int = this.cantidad,
    extras: List<BarraExtra> = this.extras,
): Elemento = when (this) {
    is Losa -> copy(id = id, nombre = nombre, cantidad = cantidad, extras = extras)
    is Viga -> copy(id = id, nombre = nombre, cantidad = cantidad, extras = extras)
    is Muro -> copy(id = id, nombre = nombre, cantidad = cantidad, extras = extras)
    is Pilar -> copy(id = id, nombre = nombre, cantidad = cantidad, extras = extras)
}

/** Malla de losa. Las barras "X" miden a lo largo de X y se reparten a lo ancho de Y. */
@Serializable
data class Malla(
    val diametroX: Int = 10,
    val separacionX: Double = 20.0,
    val pataX: Double? = 0.0,
    val diametroY: Int = 10,
    val separacionY: Double = 20.0,
    val pataY: Double? = 0.0,
)

@Serializable
@SerialName("losa")
data class Losa(
    override val id: String = nuevoId(),
    override val nombre: String = "Losa",
    override val cantidad: Int = 1,
    val largoX: Double = 5.0,
    val largoY: Double = 4.0,
    val espesor: Double = 15.0,
    val recubrimiento: Double = 2.5,
    val inferior: Malla = Malla(),
    val conSuperior: Boolean = false,
    val superior: Malla = Malla(diametroX = 8, diametroY = 8, pataX = null, pataY = null),
    override val extras: List<BarraExtra> = emptyList(),
) : Elemento() {
    override val tipo: TipoElemento get() = TipoElemento.LOSA
}

@Serializable
data class GrupoBarras(
    val cantidad: Int = 2,
    val diametro: Int = 12,
    val pata: Double? = null,
)

@Serializable
data class Estribos(
    val diametro: Int = 8,
    val separacion: Double = 20.0,
    /** Largo de la zona de confinamiento en cada extremo, cm (0 = sin zona). */
    val largoZona: Double = 0.0,
    val separacionZona: Double = 10.0,
)

@Serializable
@SerialName("viga")
data class Viga(
    override val id: String = nuevoId(),
    override val nombre: String = "Viga",
    override val cantidad: Int = 1,
    /** Ancho de la sección, cm. */
    val b: Double = 20.0,
    /** Alto de la sección, cm. */
    val h: Double = 50.0,
    /** Largo total de la viga entre caras exteriores de los apoyos extremos, m. */
    val largo: Double = 5.0,
    val recubrimiento: Double = 2.5,
    val inferior: GrupoBarras = GrupoBarras(cantidad = 3, diametro = 16),
    val superior: GrupoBarras = GrupoBarras(cantidad = 2, diametro = 12),
    val piel: GrupoBarras = GrupoBarras(cantidad = 0, diametro = 10, pata = 0.0),
    val estribos: Estribos = Estribos(diametro = 8, separacion = 20.0, largoZona = 60.0, separacionZona = 10.0),
    /** Largo que se reparte con estribos, m (null = largo total). */
    val largoEstribado: Double? = null,
    /** Número de vanos iguales; cada uno lleva sus zonas de confinamiento. */
    val tramos: Int = 1,
    /** Trabas por cada estribo (amarran las barras de piel). */
    val trabas: Int = 0,
    override val extras: List<BarraExtra> = emptyList(),
) : Elemento() {
    override val tipo: TipoElemento get() = TipoElemento.VIGA
}

@Serializable
data class Refuerzo(
    val diametro: Int = 10,
    val separacion: Double = 20.0,
)

@Serializable
@SerialName("muro")
data class Muro(
    override val id: String = nuevoId(),
    override val nombre: String = "Muro",
    override val cantidad: Int = 1,
    val largo: Double = 4.0,
    val altura: Double = 2.5,
    val espesor: Double = 20.0,
    val recubrimiento: Double = 2.5,
    val dobleMalla: Boolean = true,
    val vertical: Refuerzo = Refuerzo(diametro = 12, separacion = 20.0),
    /** Largo recto que entra en la fundación, cm. */
    val anclaje: Double = 0.0,
    val pataInferior: Double? = null,
    /** Deja la espera con traslapo por encima de la altura del muro. */
    val traslapoSuperior: Boolean = true,
    val horizontal: Refuerzo = Refuerzo(diametro = 10, separacion = 20.0),
    val pataHorizontal: Double? = null,
    val diametroTrabas: Int = 8,
    val trabasPorM2: Double = 0.0,
    override val extras: List<BarraExtra> = emptyList(),
) : Elemento() {
    override val tipo: TipoElemento get() = TipoElemento.MURO
}

@Serializable
@SerialName("pilar")
data class Pilar(
    override val id: String = nuevoId(),
    override val nombre: String = "Pilar",
    override val cantidad: Int = 1,
    val circular: Boolean = false,
    /** Lados de la sección rectangular, cm. */
    val b: Double = 30.0,
    val h: Double = 30.0,
    /** Diámetro de la sección circular, cm. */
    val diametroSeccion: Double = 40.0,
    /** Altura del pilar, m. */
    val altura: Double = 3.0,
    val recubrimiento: Double = 2.5,
    val longitudinal: GrupoBarras = GrupoBarras(cantidad = 4, diametro = 16),
    /**
     * Armadura de piel: barras intermedias en las caras, entre las de esquina (solo sección
     * rectangular). Número total en las cuatro caras; mismo anclaje, pata y traslapo que las longitudinales.
     */
    val piel: GrupoBarras = GrupoBarras(cantidad = 0, diametro = 12),
    /** Largo recto que entra en la fundación, cm. */
    val anclaje: Double = 0.0,
    val traslapoSuperior: Boolean = true,
    val estribos: Estribos = Estribos(diametro = 8, separacion = 15.0, largoZona = 50.0, separacionZona = 10.0),
    /** Trabas por nivel de estribo paralelas al lado b y al lado h. */
    val trabasB: Int = 0,
    val trabasH: Int = 0,
    override val extras: List<BarraExtra> = emptyList(),
) : Elemento() {
    override val tipo: TipoElemento get() = TipoElemento.PILAR
}

enum class FormaExtra(val etiqueta: String) {
    BARRA("Barra"),
    ESTRIBO("Estribo"),
}

/** Barra dibujada a mano: suples, bastones, esquineros, estribos especiales… */
@Serializable
data class BarraExtra(
    val id: String = nuevoId(),
    val descripcion: String = "",
    val cantidad: Int = 2,
    val diametro: Int = 12,
    val forma: FormaExtra = FormaExtra.BARRA,
    /** Barra: largo recto, cm. Estribo: ancho a, cm. */
    val largo: Double = 200.0,
    val pataInicio: Double = 0.0,
    val pataFin: Double = 0.0,
    /** Estribo: alto b, cm. */
    val alto: Double = 30.0,
)

@Serializable
data class Obra(
    val id: String = nuevoId(),
    val nombre: String = "Obra",
    val creada: Long = 0L,
    val ajustes: Ajustes = Ajustes(),
    val elementos: List<Elemento> = emptyList(),
)
