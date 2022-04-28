package es.albainformatica.albamobileandroid.maestros

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.ArticulosDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import kotlinx.android.synthetic.main.buscar_articulos.*
import java.util.*


class ArticulosActivity: AppCompatActivity(), View.OnClickListener {
    private var fConfiguracion: Configuracion = Comunicador.fConfiguracion

    private var fArticulo = 0
    private var fEnBusqueda = false
    private var fVendiendo: Boolean = false
    private var fVerPromociones = false
    private var fBuscando = false
    private var fTarifaVtas: Short = 0
    private var fTarifaCajas: Short = fConfiguracion.tarifaCajas()
    private var fProveedor = 0
    private var fEmpresaActual: Int = 0
    private var fIvaIncluido: Boolean = false

    private lateinit var fRecyclerView: RecyclerView
    private lateinit var fAdapter: ArticulosRvAdapter

    private var queBuscar: String = ""
    private var queOrdenacion: Short = 0    // 0-> por descripción, 1-> por código
    private lateinit var llSideIndex: LinearLayout
    private lateinit var prefs: SharedPreferences
    private lateinit var btnOrdenar: Button
    private lateinit var btnBuscar: Button
    private lateinit var btnHco: Button

    private var fIndiceMostrado = false

    private lateinit var mapIndex: MutableMap<String, Int>
    private var alfabeto = arrayOf("#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "Ñ", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z")



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.buscar_articulos)

        // Vemos si hemos sido llamados desde ventas, para entrar en modo búsqueda.
        val i = intent
        fVendiendo = i.getBooleanExtra("vendiendo", false)
        fIvaIncluido = i.getBooleanExtra("ivaIncluido", false)
        fBuscando = i.getBooleanExtra("buscando", false)
        if (i != null) queBuscar = i.getStringExtra("buscar") ?: ""

        // Vemos la tarifa que tendremos que utilizar para presentar los precios de cada artículo.
        fTarifaVtas = if (fVendiendo) {
            val fDocumento = Comunicador.fDocumento
            if (fDocumento.fTarifaDoc > 0) fDocumento.fTarifaDoc
            else fConfiguracion.tarifaVentas()

        } else {
            fConfiguracion.tarifaVentas()
        }

        inicializarControles()

        val fCargarTodosArt = prefs.getBoolean("cargar_todos_art", true)
        if (fCargarTodosArt) {
            // Cargamos los artículos de inicio.
            prepararRecyclerView()
            // Preparamos la búsqueda por letra.
            displayIndex()
            fIndiceMostrado = true
        }
        else {
            buscarArticulos(queBuscar)
        }
    }

    override fun onDestroy() {
        guardarPreferencias()
        super.onDestroy()
    }


    private fun getIndexList() {
        var letra: String
        var posicion = 0
        mapIndex.clear()
        mapIndex[alfabeto[0]] = 0
        for (i in 1 until alfabeto.size) {
            letra = alfabeto[i]

            // Vamos recorriendo la lista para encontrar el primer registro que coincida con la letra que
            // estamos indexando. Una vez encontrado guardamos su posición en el objeto mapIndex.
            // Si llegamos a un registro cuya primera letra es mayor que la que estamos indexando, salimos del bucle.
            if (mapIndex[letra] == null) { // La eñe tenemos que tratarla aparte, ya que con ella no funciona bien el compareTo().
                for (item in fAdapter.articulos) {
                    if (item.descripcion != "") {
                        if (letra == "Ñ") {
                            if (item.descripcion.substring(0, 1) == letra)  {
                                posicion = fAdapter.articulos.indexOf(item)
                                break
                            } else {
                                if (item.descripcion.substring(0, 1).uppercase(Locale.ROOT) > "N") {
                                    posicion = fAdapter.articulos.indexOf(item)
                                    break
                                }
                            }
                        } else {
                            if (item.descripcion.substring(0, 1) == letra) {
                                posicion = fAdapter.articulos.indexOf(item)
                                break
                            } else {
                                if (item.descripcion.substring(0, 1).uppercase(Locale.ROOT) > letra) {
                                    posicion = fAdapter.articulos.indexOf(item)
                                    break
                                }
                            }
                        }
                    }
                }
            }
            mapIndex[letra] = posicion
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
        fRecyclerView.layoutManager?.scrollToPosition(quePosicion)
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

        fRecyclerView = rvArticulos
        fRecyclerView.layoutManager = LinearLayoutManager(this)
        // Añadimos una línea divisoria entre elementos
        val dividerItemDecoration = DividerItemDecoration(fRecyclerView.context, (fRecyclerView.layoutManager as LinearLayoutManager).orientation)
        fRecyclerView.addItemDecoration(dividerItemDecoration)
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
        view.getTag(0)              // Para que no dé warning el compilador

        queOrdenacion = if (queOrdenacion == 0.toShort()) 1 else 0
        prepararRecyclerView()

        if (queOrdenacion == 1.toShort()) llSideIndex.visibility = View.GONE
        else llSideIndex.visibility = View.VISIBLE
    }


    fun buscarArt(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        if (!fEnBusqueda) {
            val builder = AlertDialog.Builder(this)
            val inflater = layoutInflater
            builder.setTitle("Buscar artículos")
            val dialogLayout = inflater.inflate(R.layout.bio_alert_dialog_with_edittext, null)
            val editText = dialogLayout.findViewById<EditText>(R.id.editText)
            editText.requestFocus()
            builder.setView(dialogLayout)
            builder.setPositiveButton("OK") { _, _ -> buscarArticulos(editText.text.toString()) }
            builder.setNegativeButton("Cancelar") { _, _ -> }
            builder.show()
        } else {
            fEnBusqueda = false
            queBuscar = ""
            btnBuscar.text = getString(R.string.buscar)
            prepararRecyclerView()
        }
    }


    private fun buscarArticulos(cadBuscar: String) {
        if (cadBuscar != "") {
            fEnBusqueda = true
            queBuscar = cadBuscar
            btnBuscar.text = getString(R.string.anular_busq)
            prepararRecyclerView()
        }
        else MsjAlerta(this).alerta(resources.getString(R.string.msj_SinBusqueda))
    }




    fun verPromociones(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        fVerPromociones = !fVerPromociones
        prepararRecyclerView()
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



    private fun prepararRecyclerView() {
        fAdapter = ArticulosRvAdapter(getArticulos(), fIvaIncluido, this, object: ArticulosRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: ListaArticulos) {
                if (fVendiendo || fBuscando) seleccionar(data.articuloId)
                else abrirFicha(data.articuloId)
            }
        })

        fRecyclerView.adapter = fAdapter

        // Cargamos el Map con las posiciones para la búsqueda por letra.
        getIndexList()
    }


    private fun getArticulos(): MutableList<ListaArticulos> {
        val articulosDao: ArticulosDao? = MyDatabase.getInstance(this)?.articulosDao()

        return if (fVerPromociones) {
            if (fConfiguracion.sumarStockEmpresas())
                articulosDao?.getArticPorPromSuma()?.toMutableList() ?: emptyList<ListaArticulos>().toMutableList()
            else
                articulosDao?.getArticPorPromoc(fEmpresaActual.toShort())?.toMutableList() ?: emptyList<ListaArticulos>().toMutableList()
        }
        else {
            if (fConfiguracion.sumarStockEmpresas())
                articulosDao?.getArticPorDCSuma(queOrdenacion, "%$queBuscar%", fEmpresaActual, fTarifaVtas,
                    fTarifaCajas)?.toMutableList() ?: emptyList<ListaArticulos>().toMutableList()
            else
                articulosDao?.getArticPorDescrCod(queOrdenacion, "%$queBuscar%", fEmpresaActual, fTarifaVtas,
                    fTarifaCajas, fEmpresaActual.toShort())?.toMutableList() ?: emptyList<ListaArticulos>().toMutableList()
        }
    }



    private fun abrirFicha(articuloId: Int) {
        val i = Intent(this, FichaArticuloActivity::class.java)
        i.putExtra("articulo", articuloId)
        startActivity(i)
    }

    private fun seleccionar(articuloId: Int) {
        // Devolvemos el artículo que tengamos seleccionado (fArticulo).
        if (articuloId > 0) {
            val returnIntent = Intent()
            returnIntent.putExtra("vengoDe", LISTA_ARTICULOS)
            returnIntent.putExtra("articulo", articuloId)
            setResult(RESULT_OK, returnIntent)
            finish()
        }
    }


}