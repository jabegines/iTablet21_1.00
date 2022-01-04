package es.albainformatica.albamobileandroid.cargas

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SimpleCursorAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.impresion_informes.ImprGenerica
import es.albainformatica.albamobileandroid.impresion_informes.ImprIntermecPB51
import es.albainformatica.albamobileandroid.impresion_informes.ImprimirDocumento
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import es.albainformatica.albamobileandroid.maestros.LotesClase
import kotlinx.android.synthetic.main.cargas.*
import org.jetbrains.anko.alert
import java.text.SimpleDateFormat


class VerCargas: AppCompatActivity() {
    private lateinit var fConfiguracion: Configuracion
    private lateinit var fLotes: LotesClase
    private lateinit var fArticulos: ArticulosClase

    private lateinit var fRecyclerView: RecyclerView
    private lateinit var fAdapter: RecAdapCargas

    private lateinit var adapterLineas: SimpleCursorAdapter
    private lateinit var fCursor: Cursor
    private var fDecimalesCant: Int = 0


    private val fRequestNuevaCarga = 1



    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.cargas)

        fLotes = LotesClase(this)
        fArticulos = ArticulosClase(this)
        fConfiguracion = Comunicador.fConfiguracion
        fDecimalesCant = fConfiguracion.decimalesCantidad()
        inicializarControles()
    }

    override fun onDestroy() {
        fArticulos.close()

        super.onDestroy()
    }


    private fun inicializarControles() {

        setupRecyclerView()
        prepararListView()

        // Mediante este código seleccionamos el primer registro del recyclerView y hacemos como si pulsáramos
        // click en él. Hay que hacerlo con un Handler().postDelayed() porque si no, da errores.
        if (fAdapter.cargas.count() > 0) {
            Handler().postDelayed({
                fRecyclerView.findViewHolderForAdapterPosition(0)?.itemView?.performClick()
            }, 100)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_cargas, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return if (item.itemId == R.id.mni_confcargas) {
            val i = Intent(this, ConfigurarCargas::class.java)
            startActivity(i)
            true
        } else true
    }


    private fun setupRecyclerView() {
        fRecyclerView = rvCargas
        fRecyclerView.layoutManager = LinearLayoutManager(this)

        // TODO
        //fAdapter = RecAdapCargas(getCargas(), this, object : RecAdapCargas.OnItemClickListener {
        //    override fun onClick(view: View, data: DatosCarga) {
        //        verCarga()
        //    }
        //})

        fRecyclerView.adapter = fAdapter
    }

    // TODO: terminar esto
    /*
    private fun getCargas(): MutableList<DatosCarga> {
        val cCargas = dbAlba.rawQuery("SELECT * FROM cargas", null)
        val lCargas: MutableList<DatosCarga> = arrayListOf()

        if (cCargas.moveToFirst()) {
            do {
                val dCarga = DatosCarga()
                dCarga.cargaId = cCargas.getInt(cCargas.getColumnIndex("cargaId"))
                dCarga.empresa = cCargas.getInt(cCargas.getColumnIndex("empresa")).toShort()
                dCarga.fecha = cCargas.getString(cCargas.getColumnIndex("fecha"))
                dCarga.hora = cCargas.getString(cCargas.getColumnIndex("hora"))
                dCarga.finDeDia = cCargas.getString(cCargas.getColumnIndex("esFinDeDia"))
                lCargas.add(dCarga)

            } while (cCargas.moveToNext())
        }
        cCargas.close()

        return lCargas
    }
    */

    private fun verCarga() {
        // Refrescamos el cursor de las cargas y mostramos los artículos de la que hemos seleccionado
        // TODO
        //cargarCursor()
        adapterLineas.changeCursor(fCursor)
    }


    private fun prepararListView() {
        val columnas: Array<String> = arrayOf("codigo", "descr", "lote", "cajas", "cantidad")
        val to: IntArray = intArrayOf(R.id.tvLNCCodigo, R.id.tvLNCDescr, R.id.tvLNCLote, R.id.tvLNCCajas, R.id.tvLNCCantidad)

        //cargarCursor()

        adapterLineas = SimpleCursorAdapter(this, R.layout.ly_ver_carga, fCursor, columnas, to, 0)

        lvCargas.adapter = adapterLineas
    }

    // TODO
    /*
    private fun cargarCursor() {

        fCursor = dbAlba.rawQuery("SELECT A.*, B.codigo, B.descr FROM cargasLineas A " +
                " LEFT JOIN articulos B ON B.articulo = A.articulo" +
                " WHERE A.cargaId = " + fAdapter.cargaId, null)

        fCursor.moveToFirst()
    }
    */

    fun nuevaCarga(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val i = Intent(this, NuevaCarga::class.java)
        startActivityForResult(i, fRequestNuevaCarga)
    }

    // TODO
    /*
    fun puestaACero(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        alert("¿Realizar la puesta a cero?" + "\nSe creará una nueva carga con los stocks de cada artículo") {
            title = "Puesta a cero"
            positiveButton("SI") { hacerPuestaACero() }
            negativeButton("NO") { }
        }.show()
    }
    */

    fun imprimirCarga(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        // Vemos el tipo de impresora por el que vamos a imprimir.
        if (fConfiguracion.impresora() ==  IMPRESORA_STARTDP8340S) {
            val imprCarga = ImprimirDocumento(this)
            imprCarga.imprimirCarga(fAdapter.cargaId)
        }
        else if (fConfiguracion.impresora() == IMPRESORA_INTERMEC_PB51) {
            val imprCarga = ImprIntermecPB51(this)
            imprCarga.imprimirCarga(fAdapter.cargaId)
        }
        else if (fConfiguracion.impresora() == IMPRESORA_BIXOLON_SPP_R410 ||
                fConfiguracion.impresora() == IMPRESORA_GENERICA_110 ||
                fConfiguracion.impresora() == IMPRESORA_GENERICA_80) {
            val imprCarga = ImprGenerica(this)
            imprCarga.imprimirCarga(fAdapter.cargaId)
        }
    }

    // TODO
    /*
    private fun hacerPuestaACero() {
        var fHayFinDeDia = false
        var fCargaId = 0

        // Si usamos trazabilidad repasamos la tabla de lotes
        if (fConfiguracion.usarTrazabilidad()) {
            // Vemos los lotes
            if (fArticulos.abrirLotesFinDia()) {
                var queEmpresa = fArticulos.cursor.getShort(fArticulos.cursor.getColumnIndex("empresa"))

                do {
                    val fStock = fArticulos.cursor.getString(fArticulos.cursor.getColumnIndex("stock")).replace(",", ".")
                    var dStock = fStock.toDouble()

                    if (dStock != 0.0) {

                        if (!fHayFinDeDia || fArticulos.cursor.getShort(fArticulos.cursor.getColumnIndex("empresa")) != queEmpresa) {
                            queEmpresa = fArticulos.cursor.getShort(fArticulos.cursor.getColumnIndex("empresa"))
                            // Añadimos una nueva carga
                            fCargaId = anyadirCarga(queEmpresa)
                            fHayFinDeDia = true
                        }

                        val values = ContentValues()
                        values.put("cargaId", fCargaId)
                        values.put("articulo", fArticulos.cursor.getInt(fArticulos.cursor.getColumnIndex("articuloId")))
                        values.put("lote", fArticulos.cursor.getString(fArticulos.cursor.getColumnIndex("lote")))
                        values.put("cajas", 0)
                        values.put("cantidad", Redondear(dStock, fDecimalesCant).toString())

                        dbAlba.insert("cargasLineas", null, values)
                        // Actualizamos el stock del lote y también del artículo. Antes ponemos la cantidad en negativo, para que sume.
                        dStock *= -1

                        fLotes.actStockLote(fArticulos.cursor.getInt(fArticulos.cursor.getColumnIndex("articuloId")),
                                    dStock, fArticulos.cursor.getString(fArticulos.cursor.getColumnIndex("lote")),
                                    queEmpresa)

                        fArticulos.actualizarStock(fArticulos.getArticulo(), queEmpresa, dStock, 0.0, false)
                    }

                } while (fArticulos.cursor.moveToNext())
            }

        } else {
            // Iremos desestocando de la tabla de stock
            if (fArticulos.abrirParaFinDeDia()) {
                var queEmpresa = fArticulos.cursor.getShort(fArticulos.cursor.getColumnIndex("empresa"))

                do {
                    var fExistencias = fArticulos.getExistencias()
                    var fCajas = fArticulos.getCajas()

                    if (fExistencias != 0.0 || fCajas != 0.0) {

                        if (!fHayFinDeDia || fArticulos.cursor.getShort(fArticulos.cursor.getColumnIndex("empresa")) != queEmpresa) {
                            queEmpresa = fArticulos.cursor.getShort(fArticulos.cursor.getColumnIndex("empresa"))
                            // Añadimos una nueva carga
                            fCargaId = anyadirCarga(queEmpresa)
                            fHayFinDeDia = true
                        }

                        val values = ContentValues()
                        values.put("cargaId", fCargaId)
                        values.put("articulo", fArticulos.getArticulo())
                        values.put("lote", "")
                        values.put("cajas", fCajas.toString())
                        values.put("cantidad", Redondear(fExistencias, fDecimalesCant).toString())

                        dbAlba.insert("cargasLineas", null, values)

                        // Actualizamos el stock del artículo. Antes ponemos fCajas y fExistencias en negativo, para que resten.
                        fCajas *= -1
                        fExistencias *= -1
                        fArticulos.actualizarStock(fArticulos.getArticulo(), queEmpresa, fExistencias, 0.0, false)
                    }

                } while (fArticulos.cursor.moveToNext())
            }
        }

        if (!fHayFinDeDia) {
            alert("No se encontraron artículos válidos para realizar la puesta a cero") {
                title = "Puesta a cero"
                positiveButton("OK") { }
            }.show()
        }
        else {
            alert("Se terminó de realizar la puesta a cero") {
                title = "Puesta a cero"
                positiveButton("OK") { finish() }
            }.show()
        }
    }
    */

    // TODO
    /*
    @SuppressLint("SimpleDateFormat")
    private fun anyadirCarga(queEmpresa: Short): Int {
        // Obtenemos la fecha y hora actuales
        val tim = System.currentTimeMillis()
        val df = SimpleDateFormat("dd/MM/yyyy")
        val fFecha = df.format(tim)
        val dfHora = SimpleDateFormat("HH:mm")
        val fHora = dfHora.format(tim)

        val valuesCarga = ContentValues()
        valuesCarga.put("empresa", queEmpresa)
        valuesCarga.put("fecha", fFecha)
        valuesCarga.put("hora", fHora)
        valuesCarga.put("esFinDeDia", "T")
        valuesCarga.put("estado", "N")

        return dbAlba.insert("cargas", null, valuesCarga).toInt()
    }
    */

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Actividad nueva carga
        if (requestCode == fRequestNuevaCarga) {
            if (resultCode == Activity.RESULT_OK) {
                // Refrescamos el adaptador del recyclerView si hemos añadido alguna carga
                // TODO
                //fAdapter.cargas = getCargas()
                fAdapter.notifyDataSetChanged()
            }
        }
    }


}