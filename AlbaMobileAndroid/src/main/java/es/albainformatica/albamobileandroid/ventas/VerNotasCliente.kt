package es.albainformatica.albamobileandroid.ventas

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.MsjAlerta
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.entity.NotasCltesEnt
import kotlinx.android.synthetic.main.ver_notas_cliente.*

/**
 * Created by jabegines on 27/12/2017.
 */
class VerNotasCliente: Activity() {
    private lateinit var fNotas: NotasClientes

    private lateinit var fRecyclerView: RecyclerView
    private lateinit var fAdapter: NotasClteAdapter

    private var fCliente = 0
    private var fIdNota = 0
    private var fTextoActual: String = ""
    private var fEstadoActual: String = ""
    private var fFechaActual: String = ""

    private val fRequestNuevaNota = 1
    private val fRequestEditarNota = 2


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.ver_notas_cliente)

        val i = intent
        fCliente = i.getIntExtra("cliente", 0)
        fNotas = NotasClientes(this)
        fNotas.abrirUnCliente(fCliente)
        inicializarControles()
    }

    override fun onDestroy() {
        fNotas.close()
        super.onDestroy()
    }

    private fun inicializarControles() {
        fIdNota = 0
        fRecyclerView = rvNotasCltes
        fRecyclerView.layoutManager = LinearLayoutManager(this)
        // Añadimos una línea divisoria entre elementos
        val dividerItemDecoration = DividerItemDecoration(fRecyclerView.context, (fRecyclerView.layoutManager as LinearLayoutManager).orientation)
        fRecyclerView.addItemDecoration(dividerItemDecoration)

        prepararRecyclerView()
    }


    private fun prepararRecyclerView() {
        fAdapter = NotasClteAdapter(getNotas(), this, object: NotasClteAdapter.OnItemClickListener {
            override fun onClick(view: View, data: NotasCltesEnt) {
                fIdNota = data.notaId
                fTextoActual = data.nota
                fEstadoActual = data.estado
                fFechaActual = data.fecha
            }
        })

        fRecyclerView.adapter = fAdapter
    }


    private fun getNotas(): MutableList<NotasCltesEnt> {
        return fNotas.abrirUnCliente(fCliente)
    }


    fun nuevaNota(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val i = Intent(this, EditarNota::class.java)
        startActivityForResult(i, fRequestNuevaNota)
    }

    fun editarNota(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fIdNota > 0) {
            val i = Intent(this, EditarNota::class.java)
            i.putExtra("nota", fTextoActual)
            startActivityForResult(i, fRequestEditarNota)
        } else {
            MsjAlerta(this).alerta(resources.getString(R.string.msj_NoRegSelecc))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == fRequestNuevaNota) {
            if (resultCode == RESULT_OK) {
                val queNota = data.getStringExtra("nota") ?: ""
                fNotas.anyadirNota(fCliente, queNota)
                prepararRecyclerView()
            }
        } else if (requestCode == fRequestEditarNota) {
            if (resultCode == RESULT_OK) {
                val queNota = data.getStringExtra("nota") ?: ""
                fNotas.editarNota(fIdNota, fCliente, queNota, fEstadoActual, fFechaActual)
                fIdNota = 0
                prepararRecyclerView()
            }
        }
    }

}