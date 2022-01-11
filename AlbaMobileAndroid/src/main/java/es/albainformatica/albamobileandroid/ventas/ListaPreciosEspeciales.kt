package es.albainformatica.albamobileandroid.ventas

import android.app.Activity
import android.os.Bundle
import android.database.Cursor
import android.view.View
import android.widget.TextView
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import es.albainformatica.albamobileandroid.*
import java.util.*

/**
 * Created by jabegines on 16/02/2018.
 */
class ListaPreciosEspeciales : Activity() {
    private var fCliente = 0
    private lateinit var lvLineas: ListView
    private lateinit var adapterLineas: SimpleCursorAdapter
    private lateinit var fConfiguracion: Configuracion
    private var fFtoDecPrBase: String = ""
    private var fFtoDecPrII: String = ""
    private var fDecPrII = 0
    private lateinit var fDocumento: Documento

    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.lista_precios_esp)

        val i = intent
        fCliente = i.getIntExtra("cliente", 0)
        fConfiguracion = Comunicador.fConfiguracion
        fDocumento = Comunicador.fDocumento
        inicializarControles()
    }


    private fun inicializarControles() {
        fFtoDecPrBase = fConfiguracion.formatoDecPrecioBase()
        fFtoDecPrII = fConfiguracion.formatoDecPrecioIva()
        fDecPrII = fConfiguracion.decimalesPrecioIva()
        lvLineas = findViewById(R.id.lvListaPreciosEsp)
        prepararListView()
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.pr_especiales)
    }

    private fun prepararListView() {
        // TODO
        /*
        val fTarifaDoc: Short = fDocumento.fTarifaDoc
        var cadena =
            "SELECT A._id, A.precio, A.dto, A.precio prNeto, A.flag, B.descr, C.descr descrFto, D.iva porciva," +
                    " E.precio prTarifa, E.dto dtoTarifa, F.precio prTrfFto, F.dto dtoTrfFto FROM ratingart A" +
                    " LEFT JOIN articulos B ON B.articulo = A.articulo" +
                    " LEFT JOIN formatos C ON C.codigo = A.formato" +
                    " LEFT JOIN ivas D ON D.tipo = B.tipoiva" +  // Si el artículo tiene formatos los precios de tarifa estarán en la tabla 'trfformatos'. Si no, en 'tarifas',
                    // por eso hacemos join con las dos tablas.
                    " LEFT JOIN tarifas E ON E.articulo = A.articulo AND E.tarifa = " + fTarifaDoc +
                    " LEFT JOIN trfformatos F ON F.articulo = A.articulo AND F.formato = A.formato AND F.tarifa = " + fTarifaDoc +
                    " LEFT JOIN clientes G ON G.cliente = " + fCliente
        cadena = if (fDocumento.fClientes.getRamo().toInt() > 0) {
            "$cadena WHERE A.cliente = $fCliente OR A.ramo = G.ramo"
        } else {
            "$cadena WHERE A.cliente = $fCliente"
        }
        val cursor = dbAlba.rawQuery(cadena, null)
        val columnas = arrayOf("descr", "descrFto", "precio", "dto", "prNeto", "prTarifa", "dtoTarifa")
        val to = intArrayOf(
            R.id.pr_esp_descrArt,
            R.id.pr_esp_descrFto,
            R.id.pr_esp_tvPrecio,
            R.id.pr_esp_tvDto,
            R.id.pr_esp_tvPrNeto,
            R.id.pr_esp_tvPrTrf,
            R.id.pr_esp_tvDtTrf
        )
        adapterLineas = SimpleCursorAdapter(this, R.layout.ly_prec_especiales, cursor, columnas, to, 0)
        // Formateamos las columnas.
        formatearColumnas()
        lvLineas.adapter = adapterLineas

        // Establecemos el evento on click del ListView.
        lvLineas.onItemClickListener =
            AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, _: Int, _: Long -> }
        */
    }

    private fun formatearColumnas() {
        adapterLineas.viewBinder =
            SimpleCursorAdapter.ViewBinder { view: View, cursor: Cursor, columnIndex: Int ->
                val tv = view as TextView

                // Las columnas se empiezan a contar desde la cero.
                if (columnIndex == 1) {
                    var sPrecio = cursor.getString(cursor.getColumnIndex("precio"))
                    var sPorcIva = cursor.getString(cursor.getColumnIndex("porciva"))
                    if (sPrecio != null) {
                        if (fConfiguracion.ivaIncluido(
                                fDocumento.fEmpresa.toString().toInt()
                            )
                        ) {
                            if (sPorcIva != null) {
                                sPrecio = sPrecio.replace(',', '.')
                                sPorcIva = sPorcIva.replace(',', '.')
                                val dPrecio = sPrecio.toDouble()
                                val dPorcIva = sPorcIva.toDouble()
                                var dPrecioII = dPrecio + dPrecio * dPorcIva / 100
                                dPrecioII = Redondear(dPrecioII, fDecPrII)
                                tv.text = String.format(fFtoDecPrII, dPrecioII)
                            }
                        } else {
                            sPrecio = sPrecio.replace(',', '.')
                            val dPrecio = sPrecio.toDouble()
                            tv.text = String.format(fFtoDecPrBase, dPrecio)
                        }
                        return@ViewBinder true
                    }
                    // Puede ser que el precio del rating sea 0 y sólo tengamos un descuento. En este caso el descuento
                    // del rating se aplicaría sobre el precio de la tarifa.
                } else if (columnIndex == 3) {
                    var sPrecio = cursor.getString(cursor.getColumnIndex("precio"))
                    sPrecio = sPrecio.replace(',', '.')
                    var sDto = cursor.getString(cursor.getColumnIndex("dto"))
                    sDto = sDto.replace(',', '.')
                    val dDto = sDto.toDouble()
                    var sPorcIva = cursor.getString(cursor.getColumnIndex("porciva"))
                    sPorcIva = sPorcIva.replace(',', '.')
                    val dPorcIva = sPorcIva.toDouble()
                    val iFlag = cursor.getInt(cursor.getColumnIndex("flag"))
                    // Si en el rating no viene precio lo tomamos de la tarifa.
                    if (sPrecio.toDouble() == 0.0) {
                        sPrecio = if (cursor.getString(cursor.getColumnIndex("prTarifa")) != null) {
                            cursor.getString(cursor.getColumnIndex("prTarifa"))
                        } else {
                            cursor.getString(cursor.getColumnIndex("prTrfFto"))
                        }
                    }
                    sPrecio = sPrecio.replace(',', '.')
                    if (iFlag and FLAGRATING_DESCUENTOIMPORTE > 0) {
                        val dPrecio = sPrecio.toDouble() - dDto
                        if (fConfiguracion.ivaIncluido(
                                fDocumento.fEmpresa.toString().toInt()
                            )
                        ) {
                            var dPrecioII = dPrecio + dPrecio * dPorcIva / 100
                            dPrecioII = Redondear(dPrecioII, fDecPrII)
                            tv.text = String.format(fFtoDecPrII, dPrecioII)
                        } else tv.text = String.format(fFtoDecPrBase, dPrecio)
                    } else {
                        var dPrecio = sPrecio.toDouble()
                        dPrecio -= dPrecio * dDto / 100
                        if (fConfiguracion.ivaIncluido(
                                fDocumento.fEmpresa.toString().toInt()
                            )
                        ) {
                            var dPrecioII = dPrecio + dPrecio * dPorcIva / 100
                            dPrecioII = Redondear(dPrecioII, fDecPrII)
                            tv.text = String.format(fFtoDecPrII, dPrecioII)
                        } else tv.text = String.format(fFtoDecPrBase, dPrecio)
                    }
                    return@ViewBinder true

                } else if (columnIndex == 8) {
                    var sPrecio: String?
                    // Si el artículo no tiene formato el precio será el de la tarifa.
                    sPrecio = if (cursor.getString(cursor.getColumnIndex("prTarifa")) != null) {
                        cursor.getString(cursor.getColumnIndex("prTarifa"))
                    } else {
                        cursor.getString(cursor.getColumnIndex("prTrfFto"))
                    }
                    var sPorcIva = cursor.getString(cursor.getColumnIndex("porciva"))
                    if (sPrecio != null) {
                        if (fConfiguracion.ivaIncluido(
                                fDocumento.fEmpresa.toString().toInt()
                            )
                        ) {
                            if (sPorcIva != null) {
                                sPrecio = sPrecio.replace(',', '.')
                                sPorcIva = sPorcIva.replace(',', '.')
                                val dPrecio = sPrecio.toDouble()
                                val dPorcIva = sPorcIva.toDouble()
                                var dPrecioII = dPrecio + dPrecio * dPorcIva / 100
                                dPrecioII = Redondear(dPrecioII, fDecPrII)
                                tv.text = String.format(fFtoDecPrII, dPrecioII)
                            }
                        } else {
                            sPrecio = sPrecio.replace(',', '.')
                            val dPrecio = sPrecio.toDouble()
                            tv.text = String.format(fFtoDecPrBase, dPrecio)
                        }
                    }
                    return@ViewBinder true

                } else if (columnIndex == 9) {
                    var sDto: String?
                    sDto = if (cursor.getString(cursor.getColumnIndex("dtoTarifa")) != null) {
                        cursor.getString(cursor.getColumnIndex("dtoTarifa"))
                    } else {
                        cursor.getString(cursor.getColumnIndex("dtoTrfFto"))
                    }
                    if (sDto != null) {
                        sDto = sDto.replace(',', '.')
                        val dDto = sDto.toDouble()
                        tv.text = String.format(Locale.getDefault(), "%.2f", dDto)
                    }
                    return@ViewBinder true
                }
                false
            }
    }
}