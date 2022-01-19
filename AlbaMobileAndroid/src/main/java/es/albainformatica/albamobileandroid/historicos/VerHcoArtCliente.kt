package es.albainformatica.albamobileandroid.historicos

import android.app.Activity
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import android.os.Bundle
import android.widget.TextView
import android.net.Uri
import android.preference.PreferenceManager
import android.view.View
import android.widget.ImageView
import android.widget.SimpleCursorAdapter
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


    /*
    private fun prepararListView() {
        val lvLineas = findViewById<ListView>(R.id.lvHcoArtClte)
        val campos =
            arrayOf("tipodoc", "fecha", "serie", "numero", "cantidad", "precio", "precioii", "dto")
        val vistas = intArrayOf(
            R.id.lyhcoArtCl_TipoDoc,
            R.id.lyhcoArtCl_Fecha,
            R.id.lyhcoArtCl_Serie,
            R.id.lyhcoArtCl_Numero,
            R.id.lyhcoArtCl_Cant,
            R.id.lyhcoArtCl_Precio,
            R.id.lyhcoArtCl_PrecioII,
            R.id.lyhcoArtCl_Dto
        )
        adapterLineas = SimpleCursorAdapter(this, R.layout.ly_hco_artic_clte, fHistorico.cHcoArtClte, campos, vistas, 0)
        formatearColumnas()
        lvLineas.adapter = adapterLineas
    }

    private fun formatearColumnas() {
        adapterLineas.viewBinder =
            SimpleCursorAdapter.ViewBinder { view: View, cursor: Cursor, column: Int ->
                val tv = view as TextView

                // Las columnas se empiezan a contar desde la cero.
                if (column == 1) {
                    val bTipoDoc = cursor.getString(cursor.getColumnIndex("tipodoc")).toByte()
                    tv.text = tipoDocAsString(bTipoDoc.toShort())
                    return@ViewBinder true
                }
                // Formateamos el precio.
                if (column == 2) {
                    var sPrecio = cursor.getString(cursor.getColumnIndex("precio"))
                    if (sPrecio != null) {
                        sPrecio = sPrecio.replace(',', '.')
                        val dPrecio = sPrecio.toDouble()
                        tv.text = String.format(fFtoDecPrBase, dPrecio)
                        return@ViewBinder true
                    }
                }
                // Formateamos el precio iva incluído.
                if (column == 3) {
                    var sPrecio = cursor.getString(cursor.getColumnIndex("precio"))
                    var sPorcIva = cursor.getString(cursor.getColumnIndex("porciva"))
                    if (sPrecio != null && sPorcIva != null) {
                        sPrecio = sPrecio.replace(',', '.')
                        sPorcIva = sPorcIva.replace(',', '.')
                        val dPrecio = sPrecio.toDouble()
                        val dPorcIva = sPorcIva.toDouble()
                        var dPrecioII = dPrecio + dPrecio * dPorcIva / 100
                        dPrecioII = Redondear(dPrecioII, fDecPrII)
                        tv.text = String.format(fFtoDecPrII, dPrecioII)
                        return@ViewBinder true
                    }
                }
                // Formateamos la cantidad.
                if (column == 4) {
                    var sCantidad = cursor.getString(cursor.getColumnIndex("cantidad"))
                    if (sCantidad != null) {
                        sCantidad = sCantidad.replace(',', '.')
                        val dCantidad = sCantidad.toDouble()
                        tv.text = String.format(fFtoDecCant, dCantidad)
                        return@ViewBinder true
                    }
                }
                // Formateamos el % dto.
                if (column == 5) {
                    var sDto = cursor.getString(cursor.getColumnIndex("dto"))
                    if (sDto != null) {
                        sDto = sDto.replace(',', '.')
                        val dDto = sDto.toDouble()
                        tv.text = String.format(Locale.getDefault(), "%.2f", dDto)
                        return@ViewBinder true
                    }
                }
                false
            }
    }
    */

}