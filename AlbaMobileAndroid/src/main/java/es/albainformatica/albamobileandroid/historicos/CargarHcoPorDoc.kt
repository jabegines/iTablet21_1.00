package es.albainformatica.albamobileandroid.historicos

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import android.view.KeyEvent
import android.view.View
import android.widget.SimpleCursorAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import es.albainformatica.albamobileandroid.*
import kotlinx.android.synthetic.main.cargar_hcopordoc.*
import org.jetbrains.anko.alert


class CargarHcoPorDoc: AppCompatActivity() {

    private lateinit var fHistorico: Historico
    private var fCliente: Int = 0
    //private var fLinea: Int = 0
    private lateinit var fRecyclerView: RecyclerView
    private lateinit var fAdapter: RecAdapHcoPorDoc
    private lateinit var db: BaseDatos
    private lateinit var dbAlba: SQLiteDatabase

    private lateinit var adapterLineas: SimpleCursorAdapter
    private lateinit var fCursor: Cursor
    private lateinit var prefs: SharedPreferences
    private var queOrdenacion: Short = 0    // 0-> por descripción, 1-> por código

    private val fRequestEditarHco = 1



    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.cargar_hcopordoc)

        db = BaseDatos(this)
        dbAlba = db.writableDatabase
        fHistorico = Comunicador.fHistorico
        val i = intent
        fCliente = i.getIntExtra("cliente", 0)

        inicializarControles()
    }

    override fun onDestroy() {
        guardarPreferencias()
        dbAlba.close()
        db.close()

        super.onDestroy()
    }


    private fun inicializarControles() {
        leerPreferencias()

        setupRecyclerView()
        prepararListView()

        // Mediante este código seleccionamos el primer registro del recyclerView y hacemos como si pulsáramos
        // click en él. Hay que hacerlo con un Handler().postDelayed() porque si no, da errores.
        if (fAdapter.docs.count() > 0) {
            Handler().postDelayed({
                fRecyclerView.findViewHolderForAdapterPosition(0)?.itemView?.performClick()
            }, 100)
        }
    }

    private fun leerPreferencias() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        queOrdenacion = prefs.getInt("cathcodoc_orden", 0).toShort()
    }

    private fun guardarPreferencias() {
        prefs.edit().putInt("cathcodoc_orden", queOrdenacion.toInt()).apply()
    }


    private fun setupRecyclerView() {
        fRecyclerView = rvHcoPorDoc
        fRecyclerView.layoutManager = LinearLayoutManager(this)

        fAdapter = RecAdapHcoPorDoc(getDocs(), this, object: RecAdapHcoPorDoc.OnItemClickListener {
            override fun onClick(view: View, data: DatosHcoPorDoc) {
                verDocumento()
            }
        })

        fRecyclerView.adapter = fAdapter
    }


    private fun getDocs(): MutableList<DatosHcoPorDoc> {
        fHistorico.abrirHcoPorArtClte(fCliente, queOrdenacion)
        val lDocs: MutableList<DatosHcoPorDoc> = arrayListOf()

        if (fHistorico.cHco.moveToFirst()) {
            do {
                val dDoc = DatosHcoPorDoc()
                dDoc.idHco = fHistorico.cHco.getInt(fHistorico.cHco.getColumnIndex("_id"))
                dDoc.articulo = fHistorico.cHco.getInt(fHistorico.cHco.getColumnIndex("articulo"))
                dDoc.codigo = fHistorico.cHco.getString(fHistorico.cHco.getColumnIndex("codigo"))
                dDoc.descr = fHistorico.cHco.getString(fHistorico.cHco.getColumnIndex("descr"))
                dDoc.porcDev = fHistorico.cHco.getString(fHistorico.cHco.getColumnIndex("porcDevol"))
                if (fHistorico.cHco.getString(fHistorico.cHco.getColumnIndex("cantpedida")) != null)
                    dDoc.cantpedida = fHistorico.cHco.getString(fHistorico.cHco.getColumnIndex("cantpedida"))
                else
                    dDoc.cantpedida = "0.0"
                lDocs.add(dDoc)

            } while (fHistorico.cHco.moveToNext())
        }
        fHistorico.cHco.close()

        return lDocs
    }


    fun ordenarArt(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (queOrdenacion.toInt() == 1) {
            queOrdenacion = 0
            btnOrdenar.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(this, R.drawable.ordenacion_alf), null, null)
        } else {
            queOrdenacion = 1
            btnOrdenar.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(this, R.drawable.ordenacion_cod), null, null)
        }
        setupRecyclerView()
        fAdapter.articuloId = fAdapter.docs[0].articulo
        verDocumento()
    }

    private fun verDocumento() {
        // Refrescamos el cursor de las cargas y mostramos los artículos de la que hemos seleccionado
        cargarCursor()
        adapterLineas.changeCursor(fCursor)
    }



    private fun prepararListView() {
        val columnas = arrayOf("tipodoc", "serie", "fecha", "ventas", "devoluciones")

        val to = intArrayOf(R.id.tvHcoArtClTipoDoc, R.id.tvHcoArtClSerieNum, R.id.tvHcoArtClFecha,
                R.id.tvHcoArtClVentas, R.id.tvHcoArtClDevoluciones)

        cargarCursor()

        adapterLineas = SimpleCursorAdapter(this, R.layout.ly_hco_doc, fCursor, columnas, to, 0)
        // Formateamos las columnas.
        formatearColumnas()

        lvHcoPorDoc.adapter = adapterLineas

        /*
        // Establecemos el evento on click del ListView.
        lvHcoPorDoc.onItemClickListener = AdapterView.OnItemClickListener { listView, _, position, _ ->
            val cursor = listView.getItemAtPosition(position) as Cursor
            fLinea = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
            val queArticulo = cursor.getInt(cursor.getColumnIndexOrThrow("articulo"))

            val i = Intent(this, EditarHco::class.java)
            i.putExtra("linea", fLinea)
            i.putExtra("desdeHcoDoc", true)
            i.putExtra("articulo", queArticulo)
            startActivityForResult(i, fRequestEditarHco)
        }
        */
    }


    fun editarHco(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fAdapter.articuloId > 0) {
            val i = Intent(this, EditarHcoActivity::class.java)
            i.putExtra("linea", fAdapter.idHco)
            i.putExtra("desdeHcoArtClte", true)
            i.putExtra("articulo", fAdapter.articuloId)
            startActivityForResult(i, fRequestEditarHco)
        }
    }


    private fun formatearColumnas() {
        adapterLineas.viewBinder = SimpleCursorAdapter.ViewBinder { view, cursor, column ->
            val tv = view as TextView

            // El orden de las columnas será el que tengan en el cursor que estemos utilizando
            // (en este caso fHcoMes.cCursorHco), comenzando por la cero.
            // Formateamos el tipo de documento
            if (column == 3) {
                val queTipoDoc = cursor.getString(cursor.getColumnIndex("tipodoc")).toShort()
                tv.text = tipoDocAsString(queTipoDoc)

                return@ViewBinder true
            }
            // Serie/Número
            if (column == 4) {
                val queCadena = cursor.getString(cursor.getColumnIndex("serie")) + "/" +
                        cursor.getString(cursor.getColumnIndex("numero"))
                tv.text = queCadena

                return@ViewBinder true
            }

            false
        }
    }


    private fun cargarCursor() {

        fCursor = dbAlba.rawQuery("SELECT A.*, B.codigo, B.descr FROM hcoPorArticClte A " +
                " LEFT JOIN articulos B ON B.articulo = A.articulo" +
                " WHERE A.articulo = " + fAdapter.articuloId + " AND A.cliente = " + fCliente +
                " ORDER BY substr(A.fecha, 7)||substr(A.fecha, 4, 2)||substr(A.fecha, 1, 2) DESC", null)

        fCursor.moveToFirst()
    }


    fun salvarHco(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val returnIntent = Intent()
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Actividad editar historico.
        if (requestCode == fRequestEditarHco) {
            if (resultCode == Activity.RESULT_OK) {
                // Refrescamos el adaptador del recyclerView por si hemos indicado alguna cantidad a vender
                fAdapter.docs = getDocs()
                fAdapter.notifyDataSetChanged()
            }
        }
    }


    fun cancelarHco(view: View?) {
        view?.getTag(0)          // Para que no dé warning el compilador

        alert("¿Cancelar histórico?") {
            title = "Abandonar"
            positiveButton("SI") {
                fHistorico.borrar()
                val returnIntent = Intent()
                setResult(Activity.RESULT_CANCELED, returnIntent)
                finish()
            }
            negativeButton("NO") { }
        }.show()
    }


    // Manejo los eventos del teclado en la actividad.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            cancelarHco(null)
        }
        // Para las demas cosas, se reenvia el evento al listener habitual.
        return super.onKeyDown(keyCode, event)
    }


}