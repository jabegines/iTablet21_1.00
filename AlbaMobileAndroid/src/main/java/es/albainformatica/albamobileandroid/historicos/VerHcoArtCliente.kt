package es.albainformatica.albamobileandroid.historicos

import android.app.Activity
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import android.os.Bundle
import android.widget.TextView
import android.net.Uri
import androidx.preference.PreferenceManager
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.LineasDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import kotlinx.android.synthetic.main.ventas_verhcoartclte.*
import java.io.File

/**
 * Created by jabegines on 26/11/13.
 */
class VerHcoArtCliente: Activity() {
    private var lineasDao: LineasDao? = MyDatabase.getInstance(this)?.lineasDao()
    private lateinit var fConfiguracion: Configuracion
    private lateinit var fArticulos: ArticulosClase

    private lateinit var fRecycler: RecyclerView
    private lateinit var fAdapter: HcoArtClteRvAdapter

    private var fCliente = 0
    private var fArticulo = 0

    private var fFtoDecPrBase: String = ""
    private var fFtoDecPrII: String = ""
    private var fFtoDecCant: String = ""
    private var fDecPrII = 0
    private var carpetaImagenes: String = ""
    private lateinit var imgArticulo: ImageView


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.ventas_verhcoartclte)

        fConfiguracion = Comunicador.fConfiguracion
        fArticulos = ArticulosClase(this)
        val intent = intent
        fCliente = intent.getIntExtra("cliente", 0)
        fArticulo = intent.getIntExtra("articulo", 0)
        inicializarControles()
    }


    private fun inicializarControles() {
        fFtoDecPrBase = fConfiguracion.formatoDecPrecioBase()
        fFtoDecPrII = fConfiguracion.formatoDecPrecioIva()
        fFtoDecCant = fConfiguracion.formatoDecCantidad()
        fDecPrII = fConfiguracion.decimalesPrecioIva()

        if (fArticulos.existeArticulo(fArticulo)) {
            val tvArticulo = findViewById<TextView>(R.id.tvArticulo)
            val queTexto = """
                Artículo: ${fArticulos.fCodigo}
                ${fArticulos.fDescripcion }
                """.trimIndent()
            tvArticulo.text = queTexto
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            carpetaImagenes = prefs.getString("rutacomunicacion" + "/imagenes/", "/storage/sdcard0/alba/imagenes/") ?: ""
            imgArticulo = findViewById(R.id.imgArticulo)
            mostrarImagen()
        }

        fRecycler = rvHcoArtClte
        fRecycler.layoutManager = LinearLayoutManager(this)
        prepararRecyclerView()

        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.hco_art_clte)
    }

    private fun mostrarImagen() {
        val path = carpetaImagenes + fArticulos.getImagen()
        val file = File(path)
        if (file.exists()) {
            imgArticulo.visibility = View.VISIBLE
            imgArticulo.setImageURI(Uri.parse(path))
        }
    }


    private fun prepararRecyclerView() {
        fAdapter = HcoArtClteRvAdapter(getDatosHco(), this, object: HcoArtClteRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: DatosHcoArtClte) {
            }
        })
    }

    private fun getDatosHco(): List<DatosHcoArtClte> {
        return lineasDao?.abrirHcoArtClte(fCliente, fArticulo) ?: emptyList<DatosHcoArtClte>().toMutableList()
    }


}