package es.albainformatica.albamobileandroid.cargas

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.CargasDao
import es.albainformatica.albamobileandroid.dao.CargasLineasDao
import es.albainformatica.albamobileandroid.dao.StockDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.CargasEnt
import es.albainformatica.albamobileandroid.entity.CargasLineasEnt
import es.albainformatica.albamobileandroid.entity.StockEnt
import es.albainformatica.albamobileandroid.impresion_informes.ImprGenerica
import es.albainformatica.albamobileandroid.impresion_informes.ImprIntermecPB51
import es.albainformatica.albamobileandroid.impresion_informes.ImprimirDocumento
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import es.albainformatica.albamobileandroid.maestros.LotesClase
import es.albainformatica.albamobileandroid.registroEventos.RegistroEventosClase
import kotlinx.android.synthetic.main.cargas.*
import org.jetbrains.anko.alert
import java.text.SimpleDateFormat
import java.util.*


class VerCargas: AppCompatActivity() {
    private var cargasDao: CargasDao? = MyDatabase.getInstance(this)?.cargasDao()
    private var cargasLineasDao: CargasLineasDao? = MyDatabase.getInstance(this)?.cargasLineasDao()
    private lateinit var fConfiguracion: Configuracion
    private lateinit var fRegEventos: RegistroEventosClase
    private lateinit var fLotes: LotesClase
    private lateinit var fArticulos: ArticulosClase

    private lateinit var fRecyclerView: RecyclerView
    private lateinit var fRecVDetalle: RecyclerView
    private lateinit var fAdapter: CargasRvAdapter
    private lateinit var fAdpDetalle: DetCargaRvAdapter

    private var fDecimalesCant: Int = 0


    private val fRequestNuevaCarga = 1



    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.cargas)

        fRegEventos = Comunicador.fRegEventos
        fRegEventos.registrarEvento(codEv_Cargas_Entrar, descrEv_Cargas_Entrar)

        fLotes = LotesClase(this)
        fArticulos = ArticulosClase(this)
        fConfiguracion = Comunicador.fConfiguracion
        fDecimalesCant = fConfiguracion.decimalesCantidad()
        inicializarControles()
    }

    override fun onDestroy() {
        fRegEventos.registrarEvento(codEv_Cargas_Salir, descrEv_Cargas_Salir)
        super.onDestroy()
    }



    private fun inicializarControles() {

        fRecyclerView = rvCargas
        fRecyclerView.layoutManager = LinearLayoutManager(this)

        fRecVDetalle = rvDetCarga
        fRecVDetalle.layoutManager = LinearLayoutManager(this)

        prepararRecyclerView()

        hacerClickEnRecycler()
    }

    private fun hacerClickEnRecycler() {
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


    private fun prepararRecyclerView() {
        fAdapter = CargasRvAdapter(getCargas(), this, object: CargasRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: CargasEnt) {
                prepararRvDetalle(data.cargaId)
            }
        })

        fRecyclerView.adapter = fAdapter
    }



    private fun getCargas(): List<CargasEnt> {
        return cargasDao?.getAllCargas() ?: emptyList<CargasEnt>().toMutableList()
    }




    private fun prepararRvDetalle(queCargaId: Int) {
        fAdpDetalle = DetCargaRvAdapter(getDetCarga(queCargaId), this, object: DetCargaRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: DatosDetCarga) {
            }
        })

        fRecVDetalle.adapter = fAdpDetalle
    }


    private fun getDetCarga(queCargaId: Int): List<DatosDetCarga> {
        return cargasLineasDao?.getCarga(queCargaId) ?: emptyList<DatosDetCarga>().toMutableList()
    }




    fun nuevaCarga(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val i = Intent(this, NuevaCarga::class.java)
        startActivityForResult(i, fRequestNuevaCarga)
    }



    fun puestaACero(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        alert("¿Realizar la puesta a cero?" + "\nSe creará una nueva carga con los stocks de cada artículo") {
            title = "Puesta a cero"
            positiveButton("SI") { hacerPuestaACero() }
            negativeButton("NO") { }
        }.show()
    }


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



    private fun hacerPuestaACero() {
        var fHayFinDeDia = false
        var fCargaId = 0

        // Si usamos trazabilidad repasamos la tabla de lotes
        if (fConfiguracion.usarTrazabilidad()) {
            // Vemos los lotes
            fLotes.abrirLotesFinDia()
            if (fLotes.lLotes.count() > 0) {
                var queEmpresa = fLotes.lLotes[0].empresa

                for (loteEnt in fLotes.lLotes) {
                    val fStock = loteEnt.stock.replace(",", ".")
                    var dStock = fStock.toDouble()

                    if (dStock != 0.0) {

                        if (!fHayFinDeDia || loteEnt.empresa != queEmpresa) {
                            queEmpresa = loteEnt.empresa
                            // Añadimos una nueva carga
                            fCargaId = anyadirCarga(queEmpresa)
                            fHayFinDeDia = true
                        }

                        val lineaEnt = CargasLineasEnt()
                        lineaEnt.cargaId = fCargaId
                        lineaEnt.articuloId = loteEnt.articuloId
                        lineaEnt.lote = loteEnt.lote
                        lineaEnt.cajas = "0.0"
                        lineaEnt.cantidad = redondear(dStock, fDecimalesCant).toString()

                        cargasLineasDao?.insertar(lineaEnt)

                        // Actualizamos el stock del lote y también del artículo.
                        fLotes.actStockLote(loteEnt.articuloId, dStock, loteEnt.lote, queEmpresa)
                        // Ponemos la cantidad en negativo, para que sume.
                        dStock *= -1
                        fArticulos.actualizarStock(loteEnt.articuloId, queEmpresa, dStock, 0.0, true)
                    }

                }
            }

        } else {
            // Iremos desestocando de la tabla de stock
            val stockDao: StockDao? = MyDatabase.getInstance(this)?.stockDao()
            val lStock = stockDao?.abrirParaFinDeDia() ?: emptyList<StockEnt>().toMutableList()

            if (lStock.count() > 0) {
                var queEmpresa = lStock[0].empresa

                for (stockEnt in lStock) {
                    fArticulos.abrirUnArticulo(stockEnt.articuloId, stockEnt.empresa)

                    var fExistencias = fArticulos.getExistencias()
                    var fCajas =  fArticulos.getCajas()

                    if (fExistencias != 0.0 || fCajas != 0.0) {

                        if (!fHayFinDeDia || stockEnt.empresa != queEmpresa) {
                            queEmpresa = stockEnt.empresa
                            // Añadimos una nueva carga
                            fCargaId = anyadirCarga(queEmpresa)
                            fHayFinDeDia = true
                        }

                        val lineaEnt = CargasLineasEnt()
                        lineaEnt.cargaId = fCargaId
                        lineaEnt.articuloId = stockEnt.articuloId
                        lineaEnt.lote = ""
                        lineaEnt.cajas = fCajas.toString()
                        lineaEnt.cantidad = redondear(fExistencias, fDecimalesCant).toString()

                        cargasLineasDao?.insertar(lineaEnt)

                        // Actualizamos el stock del artículo. Antes ponemos fCajas y fExistencias en negativo, para que resten.
                        fCajas *= -1
                        fExistencias *= -1
                        fArticulos.actualizarStock(stockEnt.articuloId, queEmpresa, fExistencias, 0.0, true)
                    }
                }
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



    private fun anyadirCarga(queEmpresa: Short): Int {
        // Obtenemos la fecha y hora actuales
        val tim = System.currentTimeMillis()
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fFecha = df.format(tim)
        val dfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
        val fHora = dfHora.format(tim)

        val cargaEnt = CargasEnt()
        cargaEnt.empresa = queEmpresa
        cargaEnt.fecha = fFecha
        cargaEnt.hora = fHora
        cargaEnt.esFinDeDia = "T"
        cargaEnt.estado = "N"

        return cargasDao?.insertar(cargaEnt)?.toInt() ?: 0
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Actividad nueva carga
        if (requestCode == fRequestNuevaCarga) {
            if (resultCode == Activity.RESULT_OK) {
                // Refrescamos el adaptador del recyclerView si hemos añadido alguna carga
                //val queCargaId = data?.getIntExtra("cargaId", 0) ?: 0
                prepararRecyclerView()
                hacerClickEnRecycler()
            }
        }
    }


}