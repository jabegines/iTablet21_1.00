package es.albainformatica.albamobileandroid.cargas

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import es.albainformatica.albamobileandroid.*
import kotlinx.android.synthetic.main.cargas_conf.*
import org.jetbrains.anko.alert


class ConfigurarCargas: AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var fConfiguracion: Configuracion


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.cargas_conf)

        fConfiguracion = Comunicador.fConfiguracion
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        inicializarControles()
    }


    private fun inicializarControles() {
        chkPedirCodValCargas.isChecked = prefs.getBoolean("cargas_pedir_cod_valid", false)
    }


    fun aceptarConfCargas(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador


        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        builder.setTitle("Guardar configuración")
        val dialogLayout = inflater.inflate(R.layout.cargas_clave_validacion, null)
        val editText = dialogLayout.findViewById<EditText>(R.id.editText)
        builder.setView(dialogLayout)
        builder.setPositiveButton("OK") { _, _ -> validarCodCarga(editText.text.toString()) }
        builder.setNegativeButton("Cancelar") { _, _ -> }
        builder.show()
    }


    private fun validarCodCarga(queCodigo: String) {
        // Comprobamos que el código introducido sea el mismo que el del supervisor
        val fCodigo = sha1(queCodigo)
        var fClaveSupervisor = fConfiguracion.claveSupervisor()
        if (fClaveSupervisor == "") fClaveSupervisor = sha1("")

        if (fCodigo.equals(fClaveSupervisor, true)) {
            guardarConfiguracion()
        } else {
            alert("Código inválido, no se ha podido guardar la configuración") {
                title = "Guardar configuración"
                positiveButton("OK") { }
            }.show()
        }
    }

    private fun guardarConfiguracion() {
        prefs.edit().putBoolean("cargas_pedir_cod_valid", chkPedirCodValCargas.isChecked).apply()
        finish()
    }

    fun cancelarConfCargas(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        finish()
    }

}