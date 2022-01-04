package es.albainformatica.albamobileandroid.historicos

import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import es.albainformatica.albamobileandroid.Configuracion
import es.albainformatica.albamobileandroid.Comunicador
import es.albainformatica.albamobileandroid.R
import kotlinx.android.synthetic.main.acum_comp_sem_mes.*
import java.text.SimpleDateFormat
import java.util.*


class AcumComSemMes: AppCompatActivity() {
    private lateinit var lvLineas: ListView
    private var fCliente = 0
    private lateinit var adapterLineas: SimpleCursorAdapter
    private lateinit var fHistorico: HcoComSemMes
    private lateinit var fConfiguracion: Configuracion
    private var sHoy = ""
    private var sHoyMenos6 = ""
    private var sHoyMenos7 = ""
    private var sHoyMenos13 = ""
    private var fFtoDecCantidad = ""

    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.acum_comp_sem_mes)

        fHistorico = HcoComSemMes(this)
        fConfiguracion = Comunicador.fConfiguracion
        val i = intent
        fCliente = i.getIntExtra("cliente", 0)

        inicializarControles()
    }


    private fun inicializarControles() {
        fFtoDecCantidad = fConfiguracion.formatoDecCantidad()

        lvLineas = findViewById<View>(R.id.lvAcumCompSemMes) as ListView
        prepararListView()
        porSemanas(null)
    }


    private fun prepararListView() {
        val columnas = arrayOf("codigo", "descr", "suma1", "suma2")
        val to = intArrayOf(R.id.lyhcoSemMesCodigo, R.id.lyhcoSemMesDescr, R.id.lyhcoSemMesCant1, R.id.lyhcoSemMesCant2)
        adapterLineas = SimpleCursorAdapter(this, R.layout.ly_hco_com_sem_mes, fHistorico.cCursorHco, columnas, to, 0)
        //        // Formateamos las columnas.
        formatearColumnas()
        lvLineas.adapter = adapterLineas
    }


    private fun formatearColumnas() {
        adapterLineas.viewBinder = SimpleCursorAdapter.ViewBinder { view, cursor, column ->
            val tv = view as TextView
            // El orden de las columnas será el que tengan en el cursor que estemos utilizando
            // (en este caso fHistorico.cCursorHco), comenzando por la cero.
            // Formateamos las cantidades.
            if (column == 1 || column == 2) {
                val sCantidad = if (column == 1) cursor.getString(cursor.getColumnIndex("suma1")).replace(',', '.')
                else cursor.getString(cursor.getColumnIndex("suma2")).replace(',', '.')
                val dCantidad = sCantidad.toDouble()
                tv.text = String.format(fFtoDecCantidad, dCantidad)

                true
            }
            else false
        }
    }


    fun cancelarHco(view: View?) {
        view?.getTag(0)          // Para que no dé warning el compilador
        finish()
    }


    fun porMeses(view: View?) {
        view?.getTag(0)          // Para que no dé warning el compilador

        val df = SimpleDateFormat("dd/MM/yyyy")
        val calendar = Calendar.getInstance()
        calendar.time = calendar.time
        sHoy = df.format(calendar.time)
        // Restamos 29 días
        calendar.add(Calendar.DAY_OF_YEAR, -29)
        sHoyMenos6 = df.format(calendar.time)
        // Restamos 30 días
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        sHoyMenos7 = df.format(calendar.time)
        // Restamos 29 días de nuevo
        calendar.add(Calendar.DAY_OF_YEAR, -29)
        sHoyMenos13 = df.format(calendar.time)

        val tvFecha1 = tvCompSemMesCant1
        val tvFecha2 = tvCompSemMesCant2
        tvFecha1.text = sHoyMenos6.substring(0, 5) + "-" + sHoy.substring(0, 5)
        tvFecha2.text = sHoyMenos13.substring(0, 5) + "-" + sHoyMenos7.substring(0, 5)

        fHistorico.abrir(fCliente, sHoy, sHoyMenos6, sHoyMenos7, sHoyMenos13)

        adapterLineas.changeCursor(fHistorico.cCursorHco)
    }

    fun porSemanas(view: View?) {
        view?.getTag(0)          // Para que no dé warning el compilador

        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = calendar.time
        sHoy = df.format(calendar.time)
        // Restamos 6 días
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        sHoyMenos6 = df.format(calendar.time)
        // Restamos 7 días
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        sHoyMenos7 = df.format(calendar.time)
        // Restamos 6 días de nuevo
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        sHoyMenos13 = df.format(calendar.time)

        val tvFecha1 = tvCompSemMesCant1
        val tvFecha2 = tvCompSemMesCant2
        tvFecha1.text = sHoyMenos6.substring(0, 5) + "-" + sHoy.substring(0, 5)
        tvFecha2.text = sHoyMenos13.substring(0, 5) + "-" + sHoyMenos7.substring(0, 5)

        fHistorico.abrir(fCliente, sHoy, sHoyMenos6, sHoyMenos7, sHoyMenos13)

        adapterLineas.changeCursor(fHistorico.cCursorHco)
    }

}