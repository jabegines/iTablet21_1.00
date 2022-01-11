package es.albainformatica.albamobileandroid.ventas

import es.albainformatica.albamobileandroid.editTextMaxLength
import android.app.Activity
import es.albainformatica.albamobileandroid.maestros.ClientesClase
import android.widget.EditText
import android.os.Bundle
import android.content.Intent
import android.view.View
import android.widget.TextView
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.ancho_incidencia

/**
 * Created by jabegines on 30/06/2014.
 */
class VentasIncidencia : Activity() {
    private lateinit var fClientes: ClientesClase
    private var fClteDoc = 0
    private var fIncidencia: String = ""
    private lateinit var edtIncidencia: EditText


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.ventas_incidencia)

        fClientes = ClientesClase(this)
        val intent = intent
        fClteDoc = intent.getIntExtra("cliente", 0)
        fIncidencia = intent.getStringExtra("incidencia") ?: ""
        val textoIncid = intent.getStringExtra("texto") ?: ""
        inicializarControles(textoIncid)
    }

    override fun onDestroy() {
        fClientes.close()
        super.onDestroy()
    }

    private fun inicializarControles(textoIncid: String) {
        fClientes.abrirUnCliente(fClteDoc)
        val tvNFiscal = findViewById<View>(R.id.tvNombreClte) as TextView
        val tvNComercial = findViewById<View>(R.id.tvNComClte) as TextView
        val tvIncidencia = findViewById<View>(R.id.tvIncidencia) as TextView
        val queNFiscal = fClientes.fCodigo + " - " + fClientes.fNombre
        tvNFiscal.text = queNFiscal
        tvNComercial.text = fClientes.fNomComercial
        tvIncidencia.text = fIncidencia
        edtIncidencia = findViewById<View>(R.id.edtIncid) as EditText
        editTextMaxLength(edtIncidencia, ancho_incidencia)
        edtIncidencia.setText(textoIncid)
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.incidencia)
    }

    fun cancelar(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        // Desactivamos el control para que el teclado desaparezca, porque, si mantenemos el teclado,
        // al volver a la pantalla de Ventas no vemos el registro seleccionado del ListView.
        edtIncidencia.isEnabled = false
        val returnIntent = Intent()
        setResult(RESULT_CANCELED, returnIntent)
        finish()
    }

    fun limpiar(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        edtIncidencia.setText("")
    }

    fun aceptar(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val returnIntent = Intent()
        returnIntent.putExtra("textoincid", edtIncidencia.text.toString())
        setResult(RESULT_OK, returnIntent)
        finish()
    }
}