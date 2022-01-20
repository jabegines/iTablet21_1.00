package es.albainformatica.albamobileandroid.historicos

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.*
import kotlinx.android.synthetic.main.acum_comp_sem_mes.*
import java.text.SimpleDateFormat
import java.util.*


class AcumComSemMes: AppCompatActivity() {
    private lateinit var fConfiguracion: Configuracion
    private lateinit var fHistorico: HcoComSemMes

    private lateinit var fRecyclerView: RecyclerView
    private lateinit var fAdapter: AcumComSemMesRvAdapter


    private var fCliente = 0
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

        fRecyclerView = rvAcumCompSemMes
        fRecyclerView.layoutManager = LinearLayoutManager(this)

        prepararRecyclerView()
        porSemanas(null)
    }


    private fun prepararRecyclerView() {
        fAdapter = AcumComSemMesRvAdapter(getAcumulados(), this, object: AcumComSemMesRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: DatosHcoCompSemMes) {
            }
        })

        fRecyclerView.adapter = fAdapter
    }


    private fun getAcumulados(): List<DatosHcoCompSemMes> {
        fHistorico.abrir(fCliente, sHoy, sHoyMenos6, sHoyMenos7, sHoyMenos13)
        return fHistorico.lHcoCompSemMes
    }



    fun cancelarHco(view: View?) {
        view?.getTag(0)          // Para que no dé warning el compilador
        finish()
    }


    fun porMeses(view: View?) {
        view?.getTag(0)          // Para que no dé warning el compilador

        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
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
        var queTexto = sHoyMenos6.substring(0, 5) + "-" + sHoy.substring(0, 5)
        tvFecha1.text = queTexto
        queTexto = sHoyMenos13.substring(0, 5) + "-" + sHoyMenos7.substring(0, 5)
        tvFecha2.text = queTexto

        prepararRecyclerView()
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
        var queTexto = sHoyMenos6.substring(0, 5) + "-" + sHoy.substring(0, 5)
        tvFecha1.text = queTexto
        queTexto = sHoyMenos13.substring(0, 5) + "-" + sHoyMenos7.substring(0, 5)
        tvFecha2.text = queTexto

        prepararRecyclerView()
    }

}