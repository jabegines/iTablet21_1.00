package es.albainformatica.albamobileandroid.maestros

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.OfertasDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.OfertasEnt
import es.albainformatica.albamobileandroid.oldcatalogo.ItemArticulo
import es.albainformatica.albamobileandroid.oldcatalogo.ItemArticuloAdapter
import java.util.*


class ArticulosActivity: AppCompatActivity(), View.OnClickListener {
    private val ofertasDao: OfertasDao? = MyDatabase.getInstance(this)?.ofertasDao()
    private lateinit var fConfiguracion: Configuracion

    private var fArticulo = 0
    private var fVendiendo: Boolean = false
    private var fBuscando = false
    private var fTarifa: Short = 0
    private var fProveedor = 0
    private var fEmpresaActual: Int = 0

    private lateinit var queBuscar: String
    private var queOrdenacion: Short = 0    // 0-> por descripción, 1-> por código
    private lateinit var llSideIndex: LinearLayout
    private lateinit var prefs: SharedPreferences
    private lateinit var btnOrdenar: Button
    private lateinit var btnBuscar: Button
    private lateinit var btnHco: Button

    private var fEnBusqueda = false
    private var fEnOfertas = false
    private var fIndiceMostrado = false


    private lateinit var mapIndex: MutableMap<String, Int>
    private var alfabeto = arrayOf("#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "Ñ", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z")
    private lateinit var listView: ListView
    private lateinit var cursor: Cursor


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.buscar_articulos)

        // Vemos si hemos sido llamados desde ventas, para entrar en modo búsqueda.
        val i = intent
        fVendiendo = i != null && i.getBooleanExtra("vendiendo", false)
        fBuscando = i != null && i.getBooleanExtra("buscando", false)
        if (i != null) queBuscar = i.getStringExtra("buscar") ?: ""
        fConfiguracion = Comunicador.fConfiguracion

        // Vemos la tarifa que tendremos que utilizar para presentar los precios de cada artículo.
        fTarifa = if (fVendiendo) {
            val fDocumento = Comunicador.fDocumento
            if (fDocumento.fTarifaDoc != null) fDocumento.fTarifaDoc
            else fConfiguracion.tarifaVentas()

        } else {
            fConfiguracion.tarifaVentas()
        }

        inicializarControles()

        val fCargarTodosArt = prefs.getBoolean("cargar_todos_art", true)
        if (fCargarTodosArt) {
            // Cargamos los artículos de inicio.
            prepararListView("", 1)
            // Preparamos la búsqueda por letra.
            displayIndex()
            fIndiceMostrado = true
        }
        else {
            buscar(queBuscar)
        }
    }

    override fun onDestroy() {
        guardarPreferencias()
        super.onDestroy()
    }

    private fun getIndexList() {
        var letra: String?
        cursor.moveToFirst()
        mapIndex.clear()
        mapIndex[alfabeto[0]] = 0
        for (i in 1 until alfabeto.size) {
            letra = alfabeto[i]

            // Vamos recorriendo el cursor para encontrar el primer registro que coincida con la letra que
            // estamos indexando. Una vez encontrado guardamos su posición en el objeto mapIndex.
            // Si llegamos a un registro cuya primera letra es mayor que la que estamos indexando, salimos del bucle.
            if (mapIndex[letra] == null) // La eñe tenemos que tratarla aparte, ya que con ella no funciona bien el compareTo().
                while (!cursor.isAfterLast) {
                    if (cursor.getString(2) != "") {
                        if (letra == "Ñ") {
                            if (cursor.getString(2).substring(0, 1) == letra) break
                            else if (cursor.getString(2).substring(0, 1).uppercase(Locale.getDefault()) > "N") break
                        } else {
                            if (cursor.getString(2).substring(0, 1) == letra) break
                            else if (cursor.getString(2).substring(0, 1).uppercase(Locale.getDefault()) > letra) break
                        }
                    }
                    cursor.moveToNext()
                }
            mapIndex[letra] = cursor.position
        }
    }


    private fun displayIndex() {
        val indexLayout = findViewById<View>(R.id.side_index) as LinearLayout
        var textView: TextView
        val indexList: List<String> = ArrayList(mapIndex.keys)
        for (index in indexList) {
            textView = View.inflate(this, R.layout.side_index_item, null) as TextView
            textView.text = index
            // En los móviles pondremos el tamaño de la fuente un poco más pequeño.
            //if (!fConfiguracion.fTamanyoPantLargo) textView.textSize = 12f
            textView.setOnClickListener(this)
            // Establecemos el layout:width, el layout:height y el layout:weight del TextView.
            textView.layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.MATCH_PARENT, 1f)
            indexLayout.addView(textView)
        }
    }


    override fun onClick(view: View) {
        val selectedIndex = view as TextView
        val quePosicion = mapIndex[selectedIndex.text] ?: 0
        listView.setSelection(quePosicion)
    }

    private fun inicializarControles() {
        // Leemos las preferencias de la aplicación;
        leerPreferencias()
        fEmpresaActual = prefs.getInt("ultima_empresa", 0)

        mapIndex = LinkedHashMap()
        btnOrdenar = findViewById(R.id.btnOrdenar)
        btnBuscar = findViewById(R.id.btnBuscar)
        btnHco = findViewById(R.id.btnCatGrpHco)
        if (!fVendiendo) btnHco.visibility = View.GONE

        fProveedor = 0
        llSideIndex = findViewById(R.id.side_index)
        if (queOrdenacion.toInt() == 1) {
            // Cuando ordenamos por código no presentamos la barra de búsqueda por letra.
            llSideIndex.visibility = View.GONE
            btnOrdenar.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(this, R.drawable.ordenacion_cod), null, null)
        } else {
            btnOrdenar.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(this, R.drawable.ordenacion_alf), null, null)
        }
        fArticulo = 0

        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.articulos)
    }


    private fun leerPreferencias() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        queOrdenacion = prefs.getInt("ordenarpor", 0).toShort()
        if (prefs.getBoolean("mantener_ult_busq", false))
            if (queBuscar == "") queBuscar = prefs.getString("ult_busqueda", "") ?: ""
    }

    private fun guardarPreferencias() {
        // Hay que tener cuidado con ésto. El programa pasa por aquí (y por el onDestroy) una vez que
        // la actividad a la que retornamos ha tomado el control. Si en dicha actividad (p.ej. VentasLineas) queremos
        // hacer uso de cualquiera de las preferencias que vamos a guardar a continuación, aún no tendrán los valores
        // que vamos a guardar, puesto que, como digo, por aquí pasa DESPUES de que la actividad padre haya sido lanzada de nuevo.
        prefs.edit().putInt("modoVisArtic", LISTA_ARTICULOS).apply()
        prefs.edit().putInt("ordenarpor", queOrdenacion.toInt()).apply()
        if (prefs.getBoolean("mantener_ult_busq", false)) {
            prefs.edit().putString("ult_busqueda", queBuscar).apply()
        }
    }


    fun ordenarArt(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (queOrdenacion.toInt() == 1) {
            queOrdenacion = 0
            llSideIndex.visibility = View.VISIBLE
            btnOrdenar.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(this, R.drawable.ordenacion_alf), null, null)
        } else {
            queOrdenacion = 1
            llSideIndex.visibility = View.GONE
            btnOrdenar.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(this, R.drawable.ordenacion_cod), null, null)
        }
        when {
            fEnOfertas -> prepararListView(queBuscar, 2)
            else -> prepararListView(queBuscar, 1)
        }
    }

    fun buscarArt(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (!fEnBusqueda) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Buscar artículos")
            val inflater = layoutInflater
            val dialogLayout = inflater.inflate(R.layout.bio_alert_dialog_with_edittext, null)
            builder.setView(dialogLayout)
            val editText = dialogLayout.findViewById<EditText>(R.id.editText)
            editText.requestFocus()
            builder.setPositiveButton("OK") { _: DialogInterface?, _: Int ->
                buscar(editText.text.toString())
            }
            builder.setNegativeButton("Cancelar") { dialog: DialogInterface, _: Int -> dialog.cancel() }
            val alert = builder.create()
            alert.show()
        } else {
            fEnBusqueda = false
            queBuscar = ""
            btnBuscar.setText(R.string.buscar)
            // Volvemos a cargar los artículos de inicio.
            prepararListView("", 1)
            // Si hemos entrado sin mostrar el índice, lo mostramos.
            if (!fIndiceMostrado) {
                displayIndex()
                fIndiceMostrado = true
            }
        }
    }


    fun buscar(cadBuscar: String) {
        if (cadBuscar != "") {
            fEnBusqueda = true
            queBuscar = cadBuscar
            btnBuscar.setText(R.string.anular_busq)
            prepararListView(queBuscar, 1)
        } else MsjAlerta(this).alerta(getString(R.string.msj_SinBusqueda))
    }


    fun verPromociones(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (!fEnBusqueda) {
            if (fEnOfertas) {
                fEnOfertas = false
                // Volvemos a cargar los artículos de inicio.
                prepararListView("", 1)
            } else {
                fEnOfertas = true
                queBuscar = ""
                prepararListView("", 2)
            }
        }
    }

    fun verGrupos(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val returnIntent = Intent()
        returnIntent.putExtra("voyA", GRUPOS_Y_DEP)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    fun verCatalogos(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val returnIntent = Intent()
        returnIntent.putExtra("voyA", CATALOGOS)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    fun verHistorico(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fVendiendo) {
            val returnIntent = Intent()
            returnIntent.putExtra("voyA", HISTORICO)
            setResult(RESULT_OK, returnIntent)
            finish()
        }
    }


    private fun prepararListView(artBuscar: String, tipoBusqueda: Int) {
        var sQuery: String
        listView = findViewById(R.id.lvArticulos)
        // Vemos la tarifa para cajas.
        val queTrfCajas = fConfiguracion.tarifaCajas()

        // Tipo busqueda
        // 1: Por descripción o código, es el tipo que aplicamos cuando entramos en la activity
        // 2: Sólo ofertas
        //when (tipoBusqueda) {
        //    1 -> {
        val cadenaLike = "LIKE('%$artBuscar%')"
                sQuery =
                    "SELECT DISTINCT A.articulo, A.codigo, A.descr, A.ucaja, " +
                            " C.precio, C.dto, D.precio prCajas, D.dto dtCajas, E.iva porciva, " +
                            " (F.ent - F.sal) stock, '' descrfto" +
                            " FROM articulos A" +
                            " LEFT JOIN busquedas B ON B.articulo = A.articulo AND B.tipo = 6" +
                            " LEFT JOIN tarifas C ON C.articulo = A.articulo AND C.tarifa = " + fTarifa +
                            " LEFT JOIN tarifas D ON D.articulo = A.articulo AND D.tarifa = " + queTrfCajas +
                            " LEFT JOIN ivas E ON E.tipo = A.tipoiva" +
                            " LEFT JOIN stock F ON F.articulo = A.articulo AND F.empresa = " + fEmpresaActual +
                            " WHERE A.descr " + cadenaLike + " OR A.codigo " + cadenaLike + " OR B.clave " + cadenaLike
        //    }
        //    2 -> {
                /*
                // Ofertas
                sQuery =
                    "SELECT DISTINCT A.articulo, A.codigo, A.descr, A.ucaja," +
                            //" B.precio, B.dto, 0 prCajas, 0 dtCajas, D.iva porciva," +
                            " 0 precio, 0 dto, 0 prCajas, 0 dtCajas, D.iva porciva," +
                            " (E.ent - E.sal) stock, '' descrfto" +
                            //" FROM articulos A, ofertas B" +
                            " FROM Articulos A" +
                            " LEFT JOIN ivas D ON D.tipo = A.tipoiva" +
                            " LEFT JOIN stock E ON E.articulo = A.articulo AND E.empresa = " + fEmpresaActual
                            //" LEFT JOIN formatos F ON F.codigo = B.formato"
                            //" WHERE B.articulo = A.articulo"
                */
        //    }
        //}
        sQuery = if (queOrdenacion.toInt() == 0) "$sQuery ORDER BY A.descr" else "$sQuery ORDER BY A.Codigo"
        // TODO
        //cursor = dbAlba.rawQuery(sQuery, null)

        // En modo catálogo visualizaremos la imagen del artículo y usaremos un
        // adaptador distinto para el listView. Se trata de un adaptador propio, al que he llamado ItemArticuloAdapter.
        val itemsArticulo: ArrayList<ItemArticulo> = obtenerItems(cursor, tipoBusqueda)
        //else obtenerOftasItems()

        val adapterLineas = ItemArticuloAdapter(this, itemsArticulo, fEmpresaActual)
        listView.adapter = adapterLineas

        // Establecemos el evento on click del ListView.
        listView.onItemClickListener =
            AdapterView.OnItemClickListener { listView: AdapterView<*>, _: View?, position: Int, _: Long ->
                val queItem = listView.getItemAtPosition(position) as ItemArticulo
                fArticulo = queItem.articulo
                if (fVendiendo || fBuscando) seleccionar() else abrirFicha()
            }

        // Cargamos el Map con las posiciones para la búsqueda por letra.
        getIndexList()
    }


    // En modo lista uso un adaptador propio para el listView: ItemArticuloAdapter, que usa un ArrayList con objetos de la clase
    // ItemArticulo. En obtenerItems lo que hago es llenar dicho ArrayList.
    private fun obtenerItems(cursor: Cursor, tipoBusqueda: Int): ArrayList<ItemArticulo> {
        val items = ArrayList<ItemArticulo>()
        val ofertas: MutableList<Int>
        // Si usamos ofertas lo que hacemos es cargar en un array los artículos de las que pertenezcan
        // a la empresa y tarifa actuales para, luego, buscar el precio y dto.
        val usarOfertas = fConfiguracion.usarOfertas()
        ofertas = if (usarOfertas) {
            // Obtenemos las ofertas para la empresa y tarifa actuales
            ofertasDao?.getAllOftas(fEmpresaActual, fTarifa.toShort()) ?: emptyList<Int>().toMutableList()
        }
        else emptyList<Int>().toMutableList()

        if (cursor.moveToFirst()) {
            cursor.moveToPosition(-1)
            while (cursor.moveToNext()) {

                var quePrOfta = "0.0"
                var queDtoOfta = "0.0"
                var queArticulo = 0

                if (usarOfertas) {
                    // Vemos si el artículo está en el array de ofertas, en cuyo caso buscaremos el precio y dto. de la oferta
                    queArticulo = ofertas.find {  it == cursor.getInt(0) } ?: 0
                    if (queArticulo > 0) {
                        val ofertaEnt = ofertasDao?.getOftaArt(queArticulo, fEmpresaActual, fTarifa.toShort()) ?: OfertasEnt()
                        quePrOfta = ofertaEnt.precio
                        queDtoOfta = ofertaEnt.dto
                    }
                }

                val hayOferta = queArticulo > 0
                // Si queremos presentar sólo las ofertas comprobaremos que el artículo esté en el array de ofertas,
                // en caso contrario no lo presentamos
                if (tipoBusqueda == 1 || queArticulo > 0) {
                    items.add(
                        ItemArticulo(
                            cursor.getInt(0), cursor.getString(1), cursor.getString(2),
                            cursor.getString(3), cursor.getString(4), cursor.getString(5),
                            cursor.getString(6), cursor.getString(7),
                            quePrOfta, queDtoOfta,
                            cursor.getDouble(8), hayOferta, "0.0", "0.0", cursor.getDouble(9),
                            false, ""
                        )
                    )
                }
            }
        }
        return items
    }


    private fun abrirFicha() {
        if (fArticulo > 0) {
            val i = Intent(this, FichaArticuloActivity::class.java)
            i.putExtra("articulo", fArticulo)
            startActivity(i)
            fArticulo = 0
        }
    }

    private fun seleccionar() {
        // Devolvemos el artículo que tengamos seleccionado (fArticulo).
        if (fArticulo > 0) {
            val returnIntent = Intent()
            returnIntent.putExtra("vengoDe", LISTA_ARTICULOS)
            returnIntent.putExtra("articulo", fArticulo)
            setResult(RESULT_OK, returnIntent)
            finish()
        }
    }


}