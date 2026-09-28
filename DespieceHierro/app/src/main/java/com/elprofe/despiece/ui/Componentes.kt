@file:OptIn(ExperimentalMaterial3Api::class)

package com.elprofe.despiece.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.elprofe.despiece.core.Acero
import com.elprofe.despiece.core.Ajustes
import com.elprofe.despiece.core.Formato

/** Ajustes de la obra abierta, para mostrar los valores automáticos en los campos. */
val LocalAjustes = compositionLocalOf { Ajustes() }

/** Contenido opcional para parámetros como suffix o supportingText. */
fun textoOpcional(t: String?): (@Composable () -> Unit)? = if (t == null) null else { { Text(t) } }

@Composable
fun BotonVolver(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
    }
}

@Composable
fun Seccion(titulo: String, modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            contenido()
        }
    }
}

@Composable
fun Subtitulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

@Composable
fun Ayuda(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun Fila(contenido: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
        content = contenido,
    )
}

@Composable
fun CampoTexto(etiqueta: String, valor: String, modifier: Modifier = Modifier, onValor: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValor,
        label = { Text(etiqueta) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Campo numérico que acepta coma o punto decimal. Mantiene su propio texto mientras
 * se escribe ("2," es válido a medias) y solo avisa cuando el número es válido.
 */
@Composable
fun CampoNumero(
    etiqueta: String,
    valor: Double,
    unidad: String,
    modifier: Modifier = Modifier,
    ayuda: String? = null,
    onValor: (Double) -> Unit,
) {
    var texto by remember { mutableStateOf(Formato.editable(valor)) }
    LaunchedEffect(valor) {
        if (Formato.leer(texto) != valor) texto = Formato.editable(valor)
    }
    OutlinedTextField(
        value = texto,
        onValueChange = { t ->
            texto = t
            Formato.leer(t)?.let(onValor)
        },
        label = { Text(etiqueta, maxLines = 1) },
        suffix = { Text(unidad) },
        supportingText = textoOpcional(ayuda),
        isError = Formato.leer(texto) == null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun CampoEntero(
    etiqueta: String,
    valor: Int,
    modifier: Modifier = Modifier,
    unidad: String? = null,
    onValor: (Int) -> Unit,
) {
    var texto by remember { mutableStateOf(valor.toString()) }
    LaunchedEffect(valor) {
        if (texto.trim().toIntOrNull() != valor) texto = valor.toString()
    }
    OutlinedTextField(
        value = texto,
        onValueChange = { t ->
            texto = t
            t.trim().toIntOrNull()?.takeIf { it >= 0 }?.let(onValor)
        },
        label = { Text(etiqueta, maxLines = 1) },
        suffix = textoOpcional(unidad),
        isError = texto.trim().toIntOrNull()?.takeIf { it >= 0 } == null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Campo numérico que puede quedar vacío: vacío = valor automático. */
@Composable
fun CampoOpcional(
    etiqueta: String,
    valor: Double?,
    unidad: String,
    textoAutomatico: String,
    modifier: Modifier = Modifier,
    ayudaConValor: String? = null,
    onValor: (Double?) -> Unit,
) {
    fun leer(t: String): Double? = if (t.isBlank()) null else Formato.leer(t)
    var texto by remember { mutableStateOf(valor?.let { Formato.editable(it) } ?: "") }
    LaunchedEffect(valor) {
        if (leer(texto) != valor) texto = valor?.let { Formato.editable(it) } ?: ""
    }
    OutlinedTextField(
        value = texto,
        onValueChange = { t ->
            texto = t
            if (t.isBlank()) onValor(null) else Formato.leer(t)?.let(onValor)
        },
        label = { Text(etiqueta, maxLines = 1) },
        placeholder = { Text("auto") },
        suffix = { Text(unidad) },
        supportingText = { Text(if (texto.isBlank()) textoAutomatico else ayudaConValor ?: "Déjalo vacío para usar el valor automático") },
        isError = texto.isNotBlank() && Formato.leer(texto) == null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Largo de pata: vacío = automática (pataDiametros × Ø), 0 = sin pata. */
@Composable
fun CampoPata(etiqueta: String, valor: Double?, diametro: Int, modifier: Modifier = Modifier, onValor: (Double?) -> Unit) {
    val ajustes = LocalAjustes.current
    CampoOpcional(
        etiqueta = etiqueta,
        valor = valor,
        unidad = "cm",
        textoAutomatico = "Automática: ${Formato.editable(ajustes.pataDiametros)}Ø = ${ajustes.pataAutomatica(diametro)} cm",
        ayudaConValor = "0 = sin pata · vacío = automática",
        modifier = modifier,
        onValor = onValor,
    )
}

@Composable
fun SelectorDiametro(etiqueta: String, valor: Int, modifier: Modifier = Modifier, onValor: (Int) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = "Ø $valor",
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta, maxLines = 1) },
            suffix = { Text("mm") },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // Capa transparente encima del campo para abrir el menú con un toque.
        Box(
            Modifier
                .matchParentSize()
                .clickable { abierto = true },
        )
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            Acero.DIAMETROS.forEach { d ->
                DropdownMenuItem(
                    text = { Text("Ø $d mm · ${Formato.num(Acero.kgPorMetro(d), 3)} kg/m") },
                    onClick = {
                        onValor(d)
                        abierto = false
                    },
                )
            }
        }
    }
}

@Composable
fun Interruptor(etiqueta: String, valor: Boolean, ayuda: String? = null, onValor: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onValor(!valor) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(etiqueta, style = MaterialTheme.typography.bodyLarge)
            if (ayuda != null) Ayuda(ayuda)
        }
        Switch(checked = valor, onCheckedChange = onValor)
    }
}

@Composable
fun DialogoTexto(
    titulo: String,
    etiqueta: String,
    inicial: String,
    confirmar: String,
    onConfirmar: (String) -> Unit,
    onCancelar: () -> Unit,
) {
    var texto by remember { mutableStateOf(inicial) }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { CampoTexto(etiqueta, texto) { texto = it } },
        confirmButton = { TextButton(onClick = { onConfirmar(texto) }) { Text(confirmar) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

@Composable
fun DialogoConfirmar(titulo: String, mensaje: String, confirmar: String, onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(onClick = onConfirmar) { Text(confirmar, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}
