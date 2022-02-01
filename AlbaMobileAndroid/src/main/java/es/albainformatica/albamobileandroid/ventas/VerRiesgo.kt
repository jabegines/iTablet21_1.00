package es.albainformatica.albamobileandroid.ventas

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import es.albainformatica.albamobileandroid.Configuracion
import es.albainformatica.albamobileandroid.Comunicador
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.cobros.PendienteClase
import es.albainformatica.albamobileandroid.maestros.ClientesClase
import kotlinx.android.synthetic.main.ver_riesgo.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class VerRiesgo: AppCompatActivity() {
    private lateinit var fClientes: ClientesClase
    private lateinit var fConfiguracion: Configuracion
    private lateinit var fPendiente: PendienteClase
    private var fCliente: Int = 0
    private var fTotalDoc: Double = 0.0
    private var fDocsPdtes: Int = 0
    private var fSoloVer: Boolean = false
    private var fEmpresa: Int = 0


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.ver_riesgo)

        val intent = intent
        fCliente = intent.getIntExtra("cliente", 0)
        fEmpresa = intent.getIntExtra("empresa", 0)
        fTotalDoc = intent.getDoubleExtra("totalDoc", 0.0)
        fDocsPdtes = intent.getIntExtra("numDocs", 0)
        fSoloVer = intent.getBooleanExtra("soloVer", false)

        fClientes = ClientesClase(this)
        fConfiguracion = Comunicador.fConfiguracion
        fPendiente = PendienteClase(this)

        inicializarControles()
    }



    private fun inicializarControles() {
        // Si el usuario no tiene permiso para vender con el riesgo superado ocultamos el botón de Aceptar.
        if (!fSoloVer && !fConfiguracion.venderRiesgoSuperado())
            btnRiesgo_Aceptar.visibility = View.GONE

        tvRgMaxAut.text = ""
        tvRgSaldo.text = ""
        tvRgAlcanzado.text = ""
        tvRgPdteFras.text = ""
        tvRgDocsPdtes.text = ""

        if (fClientes.abrirUnCliente(fCliente)) {

            val diasRiesgo: Int = if (fClientes.fMaxDias > 0) fClientes.fMaxDias
            else fConfiguracion.cltesMaxDiasRiesgo()
            val queTexto = "Movimientos pendientes superiores a $diasRiesgo días:"
            tvRgMovPdtes.text = queTexto

            tvRgMaxAut.text = String.format("%.2f", fClientes.fRiesgo)
            tvRgSaldo.text = String.format("%.2f", fClientes.getSaldo())
            tvRgAlcanzado.text = String.format("%.2f", fClientes.getSaldo() + fTotalDoc)

            if (fPendiente.abrirTodosDocClte(fCliente)) {
                var numDias = 0
                for (datosPdte in fPendiente.lTodosDocClte) {
                    val fEsDocNuevo = datosPdte.contains("/")
                    val strFechaDoc = datosPdte.replace('-', '/')
                    // Si el registro de la tabla Pendiente lo hemos hecho nuevo en la tablet, el formato de la fecha
                    // será dd/MM/yyyy. En cambio, si el registro viene de la gestión el formato será yyyy/MM/dd
                    var formatoFechaDoc = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                    if (fEsDocNuevo) formatoFechaDoc = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val formatoFechaAct = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                    val tim = System.currentTimeMillis()
                    val strFechaAct = formatoFechaAct.format(tim)
                    try {
                        val fFechaDoc = formatoFechaDoc.parse(strFechaDoc) ?: Date()
                        val fFechaAct = formatoFechaAct.parse(strFechaAct) ?: Date()
                        val dias = ((fFechaAct.time - fFechaDoc.time) / 86400000).toInt()
                        if (dias >= diasRiesgo) numDias++

                    } catch (ex: ParseException) {
                        ex.printStackTrace()
                    }

                }

                tvRgPdteFras.text = numDias.toString()
            }

            val fNumDocs = fPendiente.dimeNumDocsClte(fCliente, fEmpresa) + fDocsPdtes
            tvRgDocsPdtes.text = fNumDocs.toString()
        }
    }


    fun continuar(view: View) {
        view.getTag(0)       // Esto no vale para nada, sólo para que no dé warning el compilador

        val returnIntent = Intent()
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }


    fun cancelar(view: View) {
        view.getTag(0)       // Esto no vale para nada, sólo para que no dé warning el compilador

        val returnIntent = Intent()
        setResult(Activity.RESULT_CANCELED, returnIntent)
        finish()
    }

}