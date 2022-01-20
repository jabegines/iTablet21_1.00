package es.albainformatica.albamobileandroid.maestros

import android.app.Activity
import es.albainformatica.albamobileandroid.Configuracion
import android.os.Bundle
import es.albainformatica.albamobileandroid.Comunicador
import android.content.Intent
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.MsjAlerta
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.dao.DireccCltesDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.entity.DireccCltesEnt
import kotlinx.android.synthetic.main.ly_selecc_direc_clte.*
import java.util.ArrayList


class GetDireccClte: Activity() {
    private val direccCltesDao: DireccCltesDao? = MyDatabase.getInstance(this)?.direccCltesDao()
    private lateinit var fClientes: ClientesClase
    private lateinit var fConfiguracion: Configuracion

    private lateinit var fRecDirecciones: RecyclerView
    private lateinit var fAdpDirecciones: DirCltesRvAdapter
    private lateinit var fDatActDir: DireccCltesEnt

    private var fCliente = 0
    private val fRequestEditarDir = 1


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.ly_selecc_direc_clte)

        val extras = intent.extras
        fCliente = extras?.getInt("cliente") ?: 0
        fClientes = ClientesClase(this)
        fClientes.abrirUnCliente(fCliente)
        fConfiguracion = Comunicador.fConfiguracion
        inicializarControles()
        mostrarDirecciones()
    }

    override fun onDestroy() {
        fClientes.close()
        super.onDestroy()
    }

    private fun inicializarControles() {
        val tvNombreClte = findViewById<TextView>(R.id.tvDirNombreClte)
        tvNombreClte.text = fClientes.fNombre
        val tvDirecc = findViewById<TextView>(R.id.tvDirDirecc)
        val queTexto = fClientes.fDireccion + "-" + fClientes.fCodPostal + "-" +
                fClientes.fPoblacion + "-" + fClientes.fProvincia
        tvDirecc.text = queTexto

        fRecDirecciones = rvSelecDirCltes
        fRecDirecciones.layoutManager = LinearLayoutManager(this)
    }

    private fun mostrarDirecciones() {
        fAdpDirecciones = DirCltesRvAdapter(getDirecciones(), this, object: DirCltesRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: DireccCltesEnt) {
                //fIdDir = data.direccionId
                fDatActDir = data
                mostrarDatosDirecc()
            }
        })

        fRecDirecciones.adapter = fAdpDirecciones
    }

    private fun getDirecciones(): List<DireccCltesEnt> {
        return direccCltesDao?.getDirClte(fCliente) ?: emptyList<DireccCltesEnt>().toMutableList()
    }


    private fun mostrarDatosDirecc() {
        val edtDirDirecc = findViewById<View>(R.id.edtDir_Direcc) as EditText
        val edtDirPoblac = findViewById<View>(R.id.edtDir_Poblacion) as EditText
        val edtDirCP = findViewById<View>(R.id.edtDir_CPostal) as EditText
        val edtDirProv = findViewById<View>(R.id.edtDir_Provincia) as EditText
        edtDirDirecc.setText(fDatActDir.direccion)
        edtDirPoblac.setText(fDatActDir.localidad)
        edtDirCP.setText(fDatActDir.cPostal)
        edtDirProv.setText(fDatActDir.provincia)
    }

    fun aceptarDireccion(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val returnIntent = Intent()
        returnIntent.putExtra("idDireccion", fDatActDir.direccionId)
        returnIntent.putExtra("almDireccion", fDatActDir.almacen)
        returnIntent.putExtra("ordenDireccion", fDatActDir.orden)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    fun nuevaDir(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val i = Intent(this, EditarDirClte::class.java)
        i.putExtra("nuevo", true)
        startActivityForResult(i, fRequestEditarDir)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == fRequestEditarDir) {
            if (resultCode == RESULT_OK) {
                val aDatosDirecc = ArrayList<String>(7)
                aDatosDirecc[0] = 0.toString()
                aDatosDirecc[1] = fCliente.toString()
                aDatosDirecc[2] = fConfiguracion.almacen().toString()
                aDatosDirecc[3] = data?.getStringExtra("direccion") ?: ""
                aDatosDirecc[4] = data?.getStringExtra("poblacion") ?: ""
                aDatosDirecc[5] = data?.getStringExtra("codpostal") ?: ""
                aDatosDirecc[6] = data?.getStringExtra("provincia") ?: ""
                aceptarDir(aDatosDirecc)
                refrescarDirecc()
            }
        }
    }

    private fun aceptarDir(aDatosDirecc: ArrayList<String>) {
        if (aDatosDirecc[3] != "") {
            fClientes.aceptarCambDirec(aDatosDirecc, true)
        } else MsjAlerta(this).alerta("Tiene que indicar alguna dirección")
    }

    private fun refrescarDirecc() {
        mostrarDirecciones()
        mostrarDatosDirecc()
    }



}