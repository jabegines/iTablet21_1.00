package es.albainformatica.albamobileandroid.maestros

import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.ProveedoresDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import java.io.File


class FichaArticuloActivity: AppCompatActivity() {
    private var fArticulo = 0
    private lateinit var fArticulos: ArticulosClase
    private lateinit var fConfiguracion: Configuracion
    private lateinit var carpetaImagenes: String
    private var fEmpresaActual: Short = 0
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.ficha_articulos)

        val i = intent
        fArticulo = i.getIntExtra("articulo", 0)
        fArticulos = ArticulosClase(this)
        fConfiguracion = Comunicador.fConfiguracion
        inicializarControles()
    }


    override fun onDestroy() {
        fArticulos.close()
        super.onDestroy()
    }


    private fun inicializarControles() {
        carpetaImagenes = dimeRutaImagenes(this)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        fEmpresaActual = prefs.getInt("ultima_empresa", 0).toShort()
        mostrarFicha()
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.ficha_art)
    }


    fun ampliarImagen(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val i = Intent(this, ArticuloImagen::class.java)
        i.putExtra("imagen", carpetaImagenes + fArticulos.getImagen())
        startActivity(i)
    }


    private fun mostrarFicha() {
        val fDecCantidad = fConfiguracion.formatoDecCantidad()
        if (fArticulos.abrirUnArticulo(fArticulo, fEmpresaActual)) {
            val tvDescrArt = findViewById<TextView>(R.id.tvDescrArt)
            val tvCodArt = findViewById<TextView>(R.id.tvCodArt)
            val imgArticulo = findViewById<ImageView>(R.id.imgArticulo)
            val edtCBarras = findViewById<EditText>(R.id.edtCBarrasArt)
            val edtProveedor = findViewById<EditText>(R.id.edtProvArt)
            val edtTipoIva = findViewById<EditText>(R.id.edtTipoIvaArt)
            val edtCosto = findViewById<EditText>(R.id.edtCostoArt)
            val edtPeso = findViewById<EditText>(R.id.edtPesoArt)
            val edtUndCj = findViewById<EditText>(R.id.edtUndCaja)
            val edtExistencias = findViewById<EditText>(R.id.edtExistArt)
            val edtCajas = findViewById<EditText>(R.id.edtCajasArt)
            val path = carpetaImagenes + fArticulos.getImagen()
            val file = File(path)
            if (file.exists()) imgArticulo.setImageURI(Uri.parse(path)) else imgArticulo.setImageBitmap(null)
            tvDescrArt.text = fArticulos.fDescripcion
            tvCodArt.text = fArticulos.fCodigo
            edtCBarras.setText(fArticulos.fCodBarras)
            if (fConfiguracion.verProveedores()) {
                val provDao: ProveedoresDao? = MyDatabase.getInstance(this)?.proveedoresDao()
                val queNombreProv = provDao?.getNombreProv(fArticulos.fCodProv.toInt()) ?: ""
                val queTexto = ponerCeros(fArticulos.fCodProv.toString(), ancho_codprov) + " " + queNombreProv
                edtProveedor.setText(queTexto)

            } else edtProveedor.setText("")
            val textoTipoIva = String.format("%.2f", fArticulos.fPorcIva) + '%'
            edtTipoIva.setText(textoTipoIva)
            edtCosto.setText((String.format(fConfiguracion.formatoDecPrecioBase(), fArticulos.getCosto()) + " €"))
            edtPeso.setText(String.format(fDecCantidad, fArticulos.fPeso))
            edtUndCj.setText(String.format(fDecCantidad, fArticulos.fUCaja))
            edtExistencias.setText(String.format(fDecCantidad, fArticulos.getExistencias()))
            edtCajas.setText(String.format(fDecCantidad, fArticulos.getCajas()))
            mostrarTarifas()
        }
    }


    private fun mostrarTarifas() {
        val columnas: Array<String>
        val to: IntArray
        if (fArticulos.usarFormatos()) {
            columnas = arrayOf("tarifa", "nombretrf", "nombrefto", "precio", "priva", "dto")
            to = intArrayOf(
                R.id.ly_tarifa,
                R.id.ly_nombretrf,
                R.id.ly_nombrefto,
                R.id.ly_prbase,
                R.id.ly_priva,
                R.id.ly_dto
            )
        } else {
            columnas = arrayOf("tarifa", "nombretrf", "precio", "priva", "dto")
            to = intArrayOf(R.id.ly_tarifa, R.id.ly_nombretrf, R.id.ly_prbase, R.id.ly_priva, R.id.ly_dto)
        }
        val adapterTarif = SimpleCursorAdapter(this, R.layout.layout_tarifas_artic, fArticulos.cTarifas, columnas, to, 0)

        // Formateamos las columnas.
        adapterTarif.viewBinder = SimpleCursorAdapter.ViewBinder { view: View, cursor: Cursor, column: Int ->

            when (column) {
                // Código tarifa
                1 -> {
                    val tv = view as TextView
                    var tarifa = cursor.getString(cursor.getColumnIndex("tarifa"))
                    tarifa = ponerCeros(tarifa, ancho_tarifa)
                    tv.text = tarifa
                    return@ViewBinder true

                // Precio base
                }
                2 -> {
                    formatearPrBase(view, cursor)
                    return@ViewBinder true

                // Precio Iva
                }
                3 -> {
                    formatearPrIva(view, cursor)
                    return@ViewBinder true
                }
            }
                false
            }
        val lvTarifas = findViewById<ListView>(R.id.lvTarifArt)
        lvTarifas.adapter = adapterTarif
    }


    private fun formatearPrBase(view: View, cursor: Cursor) {
        val tv = view as TextView
        var sPrecio = cursor.getString(cursor.getColumnIndex("precio"))
        sPrecio = sPrecio.replace(',', '.')
        sPrecio = String.format(fConfiguracion.formatoDecPrecioBase(), sPrecio.toDouble())
        tv.text = sPrecio
    }


    private fun formatearPrIva(view: View, cursor: Cursor) {
        val tv = view as TextView
        var sPrecio = cursor.getString(cursor.getColumnIndex("priva"))
        sPrecio = sPrecio.replace(',', '.')
        var dPrecio = sPrecio.toDouble()
        val dImpIva = dPrecio * fArticulos.fPorcIva / 100
        dPrecio += dImpIva
        sPrecio = String.format(fConfiguracion.formatoDecPrecioIva(), dPrecio)
        tv.text = sPrecio
    }


}