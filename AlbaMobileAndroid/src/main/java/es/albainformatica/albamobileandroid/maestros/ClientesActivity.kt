package es.albainformatica.albamobileandroid.maestros

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.ClientesDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.historicos.CargarHco
import kotlinx.android.synthetic.main.clientes_activity.*
import java.util.*


class ClientesActivity: AppCompatActivity(), View.OnClickListener {
    private val clientesDao: ClientesDao? = MyDatabase.getInstance(this)?.clientesDao()
    private lateinit var fConfiguracion: Configuracion

    private lateinit var fRecyclerView: RecyclerView
    private lateinit var fAdapter: ClientesRvAdapter

    private var fCliente: Int = 0
    private var fBuscar = false
    private lateinit var prefs: SharedPreferences

    private lateinit var queBuscar: String
    private var queOrdenacion: Short = 0     // 0-> por nombre, 1-> por nombre comercial, 2-> por código
    private lateinit var llSideIndex: LinearLayout
    private var fEnBusqueda = false
    private lateinit var btnOrdenar: Button
    private lateinit var btnBuscar: Button

    private val fRequestAlta = 1

    private lateinit var mapIndex: MutableMap<String, Int>
    private var alfabeto = arrayOf("#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "Ñ", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.clientes_activity)

        // Vemos si hemos sido llamados desde ventas, para entrar en modo búsqueda.
        val i = intent
        fBuscar = i != null && i.getBooleanExtra("buscar", false)
        fConfiguracion = Comunicador.fConfiguracion
        inicializarControles()
        // Nada más entrar presentamos todos los clientes.
        prepararRecyclerView("")
        // Preparamos la búsqueda por letra.
        displayIndex()
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

            // Vamos recorriendo el cursor para encontrar el primer registro que coincida con la letra que
            // estamos indexando. Una vez encontrado guardamos su posición en el objeto mapIndex.
            // Si llegamos a un registro cuya primera letra es mayor que la que estamos indexando, salimos del bucle.
            if (mapIndex[letra] == null) { // La eñe tenemos que tratarla aparte, ya que con ella no funciona bien el compareTo().
                for (item in fAdapter.clientes) {
                    if (item.nombre != "") {
                        if (letra == "Ñ") {
                            if (item.nombre.substring(0, 1) == letra)  {
                                posicion = fAdapter.clientes.indexOf(item)
                                break
                            } else {
                                if (item.nombre.substring(0, 1).uppercase(Locale.ROOT) > "N") {
                                    posicion = fAdapter.clientes.indexOf(item)
                                    break
                                }
                            }
                        } else {
                            if (item.nombre.substring(0, 1) == letra) {
                                posicion = fAdapter.clientes.indexOf(item)
                                break
                            } else {
                                if (item.nombre.substring(0, 1).uppercase(Locale.ROOT) > letra) {
                                    posicion = fAdapter.clientes.indexOf(item)
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
        mapIndex = LinkedHashMap()
        queBuscar = ""
        llSideIndex = findViewById<View>(R.id.side_index) as LinearLayout
        if (queOrdenacion.toInt() == 2) {
            // Cuando ordenamos por código no presentamos la barra de búsqueda por letra.
            llSideIndex.visibility = View.GONE
        }
        btnOrdenar = findViewById(R.id.btnOrdenarClte)
        btnBuscar = findViewById(R.id.btnBuscarClte)

        fRecyclerView = rvClientes
        fRecyclerView.layoutManager = LinearLayoutManager(this)
        // Añadimos una línea divisoria entre elementos
        val dividerItemDecoration = DividerItemDecoration(fRecyclerView.context, (fRecyclerView.layoutManager as LinearLayoutManager).orientation)
        fRecyclerView.addItemDecoration(dividerItemDecoration)

        fCliente = 0

        // Si estamos buscando, ocultamos algunos botones del menú inferior.
        if (fBuscar) {
            //val lyMenu = findViewById<View>(R.id.lyMenuBuscarClt) as LinearLayout
            //lyMenu.visibility = View.GONE
            val btnFicha = findViewById<Button>(R.id.btnVerClte)
            val btnModif = findViewById<Button>(R.id.btnEditClte)
            val btnHco = findViewById<Button>(R.id.btnHcoClte)
            btnFicha.visibility =  View.GONE
            btnModif.visibility = View.GONE
            btnHco.visibility = View.GONE
        }
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.clientes)
    }


    private fun leerPreferencias() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        queOrdenacion = prefs.getInt("cltes_ordenarpor", 0).toShort()
    }

    private fun guardarPreferencias() {
        prefs.edit().putInt("cltes_ordenarpor", queOrdenacion.toInt()).apply()
    }

    fun buscarClte(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        // Conmutamos fEnBusqueda para mostrar o no el layout con el edit para la búsqueda.
        if (!fEnBusqueda) {
            fEnBusqueda = true

            // Lo primero que debemos hacer es rescatar el layout creado para el prompt.
            val li = LayoutInflater.from(this)
            val prompt = li.inflate(R.layout.cltes_alert_dialog_with_edittext, null)
            // Luego, creamos un constructor de Alert Dialog que nos ayudará a utilizar nuestro layout.
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setView(prompt)
            val editText = prompt.findViewById<EditText>(R.id.editText)

            // Por ultimo, creamos el cuadro de dialogo y las acciones requeridas al aceptar o cancelar el prompt.
            // Mostramos el mensaje del cuadro de diálogo
            alertDialogBuilder.setPositiveButton("OK") { _: DialogInterface?, _: Int ->
                buscar(editText.text.toString())
            }
            alertDialogBuilder.setCancelable(false).setNegativeButton("Cancelar") { dialog: DialogInterface, _: Int ->
                // Cancelamos el cuadro de dialogo
                dialog.cancel()
            }

            // Creamos un AlertDialog y lo mostramos
            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()
        } else {
            fEnBusqueda = false
            queBuscar = ""
            btnBuscar.setText(R.string.buscar)
            // Volvemos a cargar los clientes de inicio.
            prepararRecyclerView(queBuscar)
        }
    }


    private fun buscar(cadBuscar: String) {
        if (cadBuscar != "") {
            fEnBusqueda = true
            if (fConfiguracion.fTamanyoPantLargo) btnBuscar.setText(R.string.anular_busq)
            else btnBuscar.setText(R.string.anular_busq_resum)
            prepararRecyclerView(cadBuscar)
        }
    }

    fun ordenarClte(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        when {
            queOrdenacion.toInt() == 0 -> {
                queOrdenacion = 1
                llSideIndex.visibility = View.VISIBLE
                btnOrdenar.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(this, R.drawable.ordenacion), null, null)
            }
            queOrdenacion.toInt() == 1 -> {
                queOrdenacion = 2
                llSideIndex.visibility = View.GONE
                btnOrdenar.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(this, R.drawable.ordenacion_cod), null, null)
            }
            queOrdenacion.toInt() == 2 -> {
                queOrdenacion = 0
                llSideIndex.visibility = View.VISIBLE
                btnOrdenar.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(this, R.drawable.ordenacion_alf), null, null)
            }
        }
        prepararRecyclerView("")
    }


    fun nuevoClte(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        // Daremos de alta si tenemos el correspondiente permiso
        if (fConfiguracion.altaDeClientes()) {
            val i = Intent(this, FichaClteActivity::class.java)
            i.putExtra("cliente", 0)
            startActivityForResult(i, fRequestAlta)
        } else MsjAlerta(this).alerta(resources.getString(R.string.msj_NoAutorizado))
    }

    fun editarClte(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fCliente > 0) {
            val i = Intent(this, FichaClteActivity::class.java)
            i.putExtra("cliente", fCliente)
            startActivity(i)
        }
    }

    fun fichaClte(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fCliente > 0) {
            val i = Intent(this, FichaClteActivity::class.java)
            i.putExtra("cliente", fCliente)
            i.putExtra("solover", true)
            startActivity(i)
        }
    }

    fun hcoClte(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fCliente > 0) {
            val i = Intent(this, CargarHco::class.java)
            i.putExtra("cliente", fCliente)
            i.putExtra("desdecltes", true)
            startActivity(i)
        }
    }

/*
    private fun formatearColumnas() {
        adapterClientes.viewBinder = SimpleCursorAdapter.ViewBinder { view: View, cursor: Cursor, column: Int ->
            if (column == 2 || column == 3) {
                // Nombre fiscal o comercial. Los pondremos en negrita si el registro es el seleccionado. Idem con el código.
                val tv = view as TextView
                if (cursor.getInt(cursor.getColumnIndex("_id")) == fCliente) tv.setTypeface(null, Typeface.BOLD)
                else tv.setTypeface(null, Typeface.NORMAL)
                // Tengo que hacer esto porque si no, me desaparece el código, me pone siempre: 'Clientes'.
                tv.text = cursor.getString(column)
                true
            }
            if (column == 4) {
                val iv = view as ImageView
                val queFlag = cursor.getInt(cursor.getColumnIndex("flag"))
                if (queFlag and FLAGCLIENTE_NOVENDER > 0) {
                    iv.visibility = View.VISIBLE
                } else {
                    iv.visibility = View.INVISIBLE
                }
                true
            }
            false
        }
    }
*/
/*
    private fun prepararListView(clteBuscar: String) {
        val cadenaLike: String
        val columns = arrayOf("codigo", "nombre", "nombre2", "flag")
        val to = intArrayOf(R.id.lyclt_codclte, R.id.lyclt_nombreclte, R.id.lyclt_nombreclte2, R.id.lyclt_imgNoVender)
        // Presentaremos primero el nombre comercial o el fiscal según la ordenación que estemos aplicando.
        sQueryCursor = if (queOrdenacion.toInt() == 1) "SELECT cliente _id, codigo, nomco nombre, nomfi nombre2, flag FROM clientes"
        else "SELECT cliente _id, codigo, nomfi nombre, nomco nombre2, flag FROM clientes"

        if (clteBuscar != "") {
            cadenaLike = "LIKE('%$clteBuscar%')"
            sQueryCursor = "$sQueryCursor WHERE nomfi $cadenaLike OR nomco $cadenaLike OR codigo $cadenaLike"
        }
        sQueryCursor = if (queOrdenacion.toInt() == 0) "$sQueryCursor ORDER BY nomfi" else if (queOrdenacion.toInt() == 1) "$sQueryCursor ORDER BY nomco" else "$sQueryCursor ORDER BY codigo"
        cursorClientes = dbAlba.rawQuery(sQueryCursor, null)
        adapterClientes = SimpleCursorAdapter(this, R.layout.layout_clientes, cursorClientes, columns, to, 0)
        formatearColumnas()
        listView.adapter = adapterClientes

        // Establecemos el evento on click del ListView.
        listView.onItemClickListener =
            AdapterView.OnItemClickListener { adapter: AdapterView<*>, _: View?, position: Int, _: Long ->
                val cursor = adapter.getItemAtPosition(position) as Cursor
                // Tomamos el campo cliente de la fila en la que hemos pulsado
                fCliente = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
                cursorClientes = dbAlba.rawQuery(sQueryCursor, null)
                adapterClientes.changeCursor(cursorClientes)
                if (fBuscar) seleccionar()
            }

        // Cargamos el Map con las posiciones para la búsqueda por letra.
        getIndexList()
    }
*/

    private fun seleccionar() {
        // Devolvemos el cliente que tengamos seleccionado (fCliente).
        if (fCliente > 0) {
            val returnIntent = Intent()
            returnIntent.putExtra("cliente", fCliente)
            setResult(RESULT_OK, returnIntent)
            finish()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fRequestAlta) {
            if (resultCode == RESULT_OK)
                prepararRecyclerView("")
        }
    }

    private fun prepararRecyclerView(queBuscar: String) {
        fAdapter = ClientesRvAdapter(getClientes(queBuscar), this, object: ClientesRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: ListaClientes) {
                fCliente = data.clienteId
                if (fBuscar)
                    seleccionar()
            }
        })

        fRecyclerView.adapter = fAdapter

        // Cargamos el Map con las posiciones para la búsqueda por letra.
        getIndexList()
    }



    private fun getClientes(queBuscar: String): List<ListaClientes> {

        val lListaCltes: List<ListaClientes> = if (queBuscar != "") {
            clientesDao?.getCltesBusq("%$queBuscar%", queOrdenacion) ?: emptyList<ListaClientes>().toMutableList()
        } else {
            clientesDao?.getCltes(queOrdenacion) ?: emptyList<ListaClientes>().toMutableList()
        }

        return lListaCltes
    }


}