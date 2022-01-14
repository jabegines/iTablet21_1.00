package es.albainformatica.albamobileandroid.maestros

import android.app.Activity
import es.albainformatica.albamobileandroid.Configuracion
import android.os.Bundle
import es.albainformatica.albamobileandroid.Comunicador
import android.content.Intent
import android.database.Cursor
import android.view.View
import android.widget.*
import es.albainformatica.albamobileandroid.MsjAlerta
import es.albainformatica.albamobileandroid.R
import java.util.ArrayList


class GetDireccClte: Activity() {
    private lateinit var adapterDir: SimpleCursorAdapter
    private lateinit var fClientes: ClientesClase
    private lateinit var fConfiguracion: Configuracion

    private var fCliente = 0
    private var fIdDireccion = 0
    private var fAlmDireccion = ""
    private var fOrdenDireccion = ""


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
    }

    private fun mostrarDirecciones() {
        val columns = arrayOf("direcc", "poblac")
        val to = intArrayOf(R.id.ly_direcc, R.id.ly_poblac)
        adapterDir = SimpleCursorAdapter(
            this,
            R.layout.layout_direcc_cltes,
            fClientes.cDirecciones,
            columns,
            to,
            0
        )
        val listViewDir = findViewById<ListView>(R.id.lvDirCltes)
        listViewDir.adapter = adapterDir
        mostrarDatosDirecc()
        fIdDireccion = 0
        fAlmDireccion = ""
        fOrdenDireccion = ""
        listViewDir.onItemClickListener =
            AdapterView.OnItemClickListener { adapter: AdapterView<*>, _: View?, position: Int, _: Long ->
                val cursor = adapter.getItemAtPosition(position) as Cursor
                fIdDireccion = cursor.getInt(cursor.getColumnIndexOrThrow("_id"))
                fAlmDireccion = cursor.getString(cursor.getColumnIndexOrThrow("alm"))
                fOrdenDireccion = cursor.getString(cursor.getColumnIndexOrThrow("orden"))
                mostrarDatosDirecc()
            }
    }

    private fun mostrarDatosDirecc() {
        val edtDirDirecc = findViewById<View>(R.id.edtDir_Direcc) as EditText
        val edtDirPoblac = findViewById<View>(R.id.edtDir_Poblacion) as EditText
        val edtDirCP = findViewById<View>(R.id.edtDir_CPostal) as EditText
        val edtDirProv = findViewById<View>(R.id.edtDir_Provincia) as EditText
        edtDirDirecc.setText(fClientes.getDir_Direccion())
        edtDirPoblac.setText(fClientes.getDir_Poblac())
        edtDirCP.setText(fClientes.getDir_CP())
        edtDirProv.setText(fClientes.getDir_Provincia())
    }

    fun aceptarDireccion(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val returnIntent = Intent()
        returnIntent.putExtra("idDireccion", fIdDireccion)
        returnIntent.putExtra("almDireccion", fAlmDireccion)
        returnIntent.putExtra("ordenDireccion", fOrdenDireccion)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    fun nuevaDir(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val i = Intent(this, EditarDirClte::class.java)
        i.putExtra("nuevo", true)
        startActivityForResult(i, REQUEST_EDITARDIR)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_EDITARDIR) {
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
        adapterDir.changeCursor(fClientes.cDirecciones)
        mostrarDatosDirecc()
    }

    companion object {
        private const val REQUEST_EDITARDIR = 1
    }
}