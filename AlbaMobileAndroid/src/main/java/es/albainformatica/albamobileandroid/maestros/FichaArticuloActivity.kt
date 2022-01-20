package es.albainformatica.albamobileandroid.maestros

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.*
import es.albainformatica.albamobileandroid.database.MyDatabase
import kotlinx.android.synthetic.main.ficha_articulos.*
import java.io.File


class FichaArticuloActivity: AppCompatActivity() {
    private var tarifasDao: TarifasDao? = MyDatabase.getInstance(this)?.tarifasDao()
    private var trfFormatosDao: TrfFormatosDao? = MyDatabase.getInstance(this)?.trfFormatosDao()

    private lateinit var fArticulos: ArticulosClase
    private lateinit var fConfiguracion: Configuracion

    private lateinit var fRecyclerView: RecyclerView
    private lateinit var fAdapter: TrfArtRvAdapter


    private var fArticulo = 0
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
                val queNombreProv = provDao?.getNombreProv(fArticulos.fCodProv) ?: ""
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

            fRecyclerView = rvTarifArt
            fRecyclerView.layoutManager = LinearLayoutManager(this)
            mostrarTarifas()
        }
    }


    private fun mostrarTarifas() {
        fAdapter = TrfArtRvAdapter(getTarifas(), fArticulos.usarFormatos(), fArticulos.fPorcIva, this,
            object: TrfArtRvAdapter.OnItemClickListener {
                override fun onClick(view: View, data: DatosTrfArt) {
                }
            })

        fRecyclerView.adapter = fAdapter
    }


    private fun getTarifas(): List<DatosTrfArt> {
        return if (fArticulos.usarFormatos()) {
            trfFormatosDao?.getTarifasArt(fArticulos.fArticulo) ?: emptyList<DatosTrfArt>().toMutableList()

        } else {
            tarifasDao?.getTarifasArt(fArticulos.fArticulo) ?: emptyList<DatosTrfArt>().toMutableList()
        }
    }

}