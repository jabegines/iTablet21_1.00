package es.albainformatica.albamobileandroid.reparto


import android.app.Activity
import es.albainformatica.albamobileandroid.historicos.Historico
import android.os.Bundle
import android.content.Intent
import android.view.KeyEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import kotlinx.android.synthetic.main.reparto_devoluciones.*


class RepartoDevoluciones: Activity() {
    private lateinit var fConfiguracion: Configuracion
    private lateinit var fHistorico: Historico

    private lateinit var fRecyclerView: RecyclerView
    private lateinit var fAdapter: RepDevRvAdapter


    private var fCliente = 0
    private var fEmpresa: Short = 0
    private var fLinea = 0

    private val fRequestDatosDevolucion = 1


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.reparto_devoluciones)

        val i = intent
        fCliente = i.getIntExtra("cliente", 0)
        fEmpresa = i.getShortExtra("empresa", 0)
        fConfiguracion = Comunicador.fConfiguracion
        fHistorico = Historico(this)
        Comunicador.fHistorico = fHistorico
        inicializarControles()
    }


    private fun inicializarControles() {
        fRecyclerView = rvHcoDev
        fRecyclerView.layoutManager = LinearLayoutManager(this)
        prepararRecyclerView()
    }


    private fun prepararRecyclerView() {
        fAdapter = RepDevRvAdapter(getDatosHco(), this, object: RepDevRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: DatosHistorico) {
                fLinea = data.historicoId
                val i = Intent(this@RepartoDevoluciones, DatosDevolucion::class.java)
                i.putExtra("linea", fLinea)
                i.putExtra("articulo", data.articuloId)
                i.putExtra("codigo", data.codigo)
                i.putExtra("descripcion", data.descripcion)
                startActivityForResult(i, fRequestDatosDevolucion)
            }
        })

        fRecyclerView.adapter = fAdapter
    }


    private fun getDatosHco(): List<DatosHistorico> {
        fHistorico.abrir(fCliente, fEmpresa)
        return fHistorico.lDatosHistorico
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // Actividad editar linea
        if (requestCode == fRequestDatosDevolucion) {
            if (resultCode == RESULT_OK) {
                prepararRecyclerView()
            }
        }
    }


    fun cancelarRepDev(view: View?) {
        view?.getTag(0)              // Para que no dé warning el compilador

        val aldDialog = nuevoAlertBuilder(this, "Salir", "¿Anular las devoluciones?", true)
        aldDialog.setPositiveButton("Sí") { _, _ ->
            fHistorico.borrar()
            val returnIntent = Intent()
            setResult(RESULT_CANCELED, returnIntent)
            finish()
        }
        val alert = aldDialog.create()
        alert.show()
    }

    fun salvarRepDev(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val returnIntent = Intent()
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    // Manejo los eventos del teclado en la actividad.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            cancelarRepDev(null)
            // Si el listener devuelve true, significa que el evento está procesado, y nadie debe hacer nada más.
            return true
        }
        // Para las demás cosas, se reenvía el evento al listener habitual.
        return super.onKeyDown(keyCode, event)
    }




}