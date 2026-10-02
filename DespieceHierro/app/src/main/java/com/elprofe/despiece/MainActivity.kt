package com.elprofe.despiece

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewFeature
import java.io.File

/**
 * La app muestra la página «Despiece de ferralla» (assets/web/index.html) sin conexión.
 * Android se encarga de lo que la página no puede hacer sola: guardar o compartir el PDF y el
 * Excel, copiar el resumen y el botón «atrás».
 */
class MainActivity : ComponentActivity() {

    private lateinit var web: WebView

    /** Archivo exportado que espera a que el usuario elija dónde guardarlo. */
    private var pendiente: File? = null

    private val elegirDestino = registerForActivityResult(CrearDocumento()) { destino ->
        val archivo = pendiente
        pendiente = null
        if (destino != null && archivo != null) guardar(archivo, destino)
    }

    /** Respuesta pendiente de un <input type="file"> de la página (DXF, foto o PDF del plano). */
    private var eleccionArchivo: ValueCallback<Array<Uri>>? = null

    private val elegirArchivo = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val respuesta = eleccionArchivo
        eleccionArchivo = null
        respuesta?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(r.resultCode, r.data))
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        savedInstanceState?.getString(CLAVE_PENDIENTE)?.let { pendiente = File(it) }

        val cargador = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                // La página tiene su propio modo oscuro; no se oscurece a la fuerza.
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, false)
            }
            addJavascriptInterface(Puente(), "Android")
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    view: WebView,
                    filePathCallback: ValueCallback<Array<Uri>>,
                    params: FileChooserParams,
                ): Boolean {
                    eleccionArchivo?.onReceiveValue(null)
                    eleccionArchivo = filePathCallback
                    return try {
                        elegirArchivo.launch(params.createIntent())
                        true
                    } catch (e: ActivityNotFoundException) {
                        eleccionArchivo = null
                        Toast.makeText(this@MainActivity, "No hay gestor de archivos para elegir el plano.", Toast.LENGTH_LONG).show()
                        false
                    }
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                    cargador.shouldInterceptRequest(request.url)

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (request.url.host == WebViewAssetLoader.DEFAULT_DOMAIN) return false
                    // Cualquier enlace externo se abre en el navegador, nunca dentro de la app.
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    } catch (e: ActivityNotFoundException) {
                        // Sin navegador: se ignora el enlace.
                    }
                    return true
                }
            }
        }

        val raiz = FrameLayout(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.fondo))
            addView(web, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        // Deja libres la barra de estado, la de navegación y el teclado.
        ViewCompat.setOnApplyWindowInsetsListener(raiz) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(barras.left, barras.top, barras.right, maxOf(barras.bottom, teclado.bottom))
            WindowInsetsCompat.CONSUMED
        }
        setContentView(raiz)

        onBackPressedDispatcher.addCallback(this) {
            web.evaluateJavascript("window.atrasAndroid ? window.atrasAndroid() : false") { usado ->
                if (usado != "true") {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        }

        if (savedInstanceState != null) web.restoreState(savedInstanceState)
        if (web.url == null) web.loadUrl("https://${WebViewAssetLoader.DEFAULT_DOMAIN}/assets/web/index.html")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
        pendiente?.let { outState.putString(CLAVE_PENDIENTE, it.absolutePath) }
    }

    override fun onDestroy() {
        eleccionArchivo?.onReceiveValue(null)
        eleccionArchivo = null
        web.destroy()
        super.onDestroy()
    }

    /** Métodos que la página llama como window.Android.* (en un hilo aparte). */
    private inner class Puente {
        @JavascriptInterface
        fun exportar(nombre: String, mime: String, base64: String) {
            val datos = Base64.decode(base64, Base64.DEFAULT)
            runOnUiThread { ofrecer(nombreSeguro(nombre), mime, datos) }
        }

        @JavascriptInterface
        fun copiar(texto: String) {
            runOnUiThread {
                val portapapeles = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                portapapeles.setPrimaryClip(ClipData.newPlainText("Despiece", texto))
            }
        }
    }

    private fun ofrecer(nombre: String, mime: String, datos: ByteArray) {
        val carpeta = File(cacheDir, "exportados").apply { mkdirs() }
        val archivo = File(carpeta, nombre).apply { writeBytes(datos) }
        AlertDialog.Builder(this)
            .setTitle(nombre)
            .setItems(arrayOf("Guardar en el teléfono", "Compartir (WhatsApp, correo…)")) { _, opcion ->
                if (opcion == 0) {
                    pendiente = archivo
                    try {
                        elegirDestino.launch(archivo to mime)
                    } catch (e: ActivityNotFoundException) {
                        pendiente = null
                        Toast.makeText(this, "No hay gestor de archivos: usa «Compartir».", Toast.LENGTH_LONG).show()
                    }
                } else {
                    compartir(archivo, mime)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun compartir(archivo: File, mime: String) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", archivo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, archivo.nameWithoutExtension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartir ${archivo.name}"))
    }

    private fun guardar(archivo: File, destino: Uri) {
        try {
            contentResolver.openOutputStream(destino)?.use { salida -> archivo.inputStream().use { it.copyTo(salida) } }
                ?: throw IllegalStateException("sin acceso al destino")
            Toast.makeText(this, "Guardado: ${archivo.name}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo guardar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /** Selector de Android para elegir carpeta y nombre (Descargas, Drive…). */
    private class CrearDocumento : ActivityResultContract<Pair<File, String>, Uri?>() {
        override fun createIntent(context: Context, input: Pair<File, String>): Intent =
            Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(input.second)
                .putExtra(Intent.EXTRA_TITLE, input.first.name)

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
            if (resultCode == Activity.RESULT_OK) intent?.data else null
    }

    companion object {
        private const val CLAVE_PENDIENTE = "pendiente"

        private fun nombreSeguro(nombre: String): String =
            nombre.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifEmpty { "despiece" }
    }
}
