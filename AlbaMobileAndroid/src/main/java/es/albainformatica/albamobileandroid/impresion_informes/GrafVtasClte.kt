package es.albainformatica.albamobileandroid.impresion_informes

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import es.albainformatica.albamobileandroid.Configuracion
import es.albainformatica.albamobileandroid.Comunicador
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.historicos.HistoricoMes
import java.util.*


class GrafVtasClte: AppCompatActivity() {
    private var fCliente: Int = 0
    private var fNombre: String = ""
    private lateinit var fHcoMes: HistoricoMes
    private lateinit var fConfiguracion: Configuracion
    private var mesActual = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var queMes = mesActual
    private var posicion = 0f
    private var estoyEnAnyoActual = false
    private var entriesImp = ArrayList<BarEntry>()
    private lateinit var labels: Array<String>
    private var fTotalImpte: Double = 0.0
    private var fFtoImpteII = ""


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.graf_vtas_clte)

        val i = intent
        fCliente = i.getIntExtra("cliente", 0)
        fNombre = i.getStringExtra("nombre") ?: ""
        fHcoMes = HistoricoMes(this)
        fConfiguracion = Comunicador.fConfiguracion

        inicializarControles()
        graficoBarras()
    }



    fun inicializarControles() {
        val tvNombre = findViewById<TextView>(R.id.tvVtasClte)
        tvNombre.text = fNombre

        fFtoImpteII = fConfiguracion.formatoDecImptesIva()
    }

    private fun graficoBarras() {
        val chart = findViewById<BarChart>(R.id.barchartClte)
        labels = arrayOf("", "", "", "", "", "", "", "", "", "", "", "", "")

        // Recorreremos la tabla histmes desde el mes actual del a??o anterior hasta el mes actual de este a??o,
        // por eso llamamos a calcularMes 13 veces.
        if (fHcoMes.abrirCliente(fCliente)) {
            calcularMes()
            calcularMes()
            calcularMes()
            calcularMes()
            calcularMes()
            calcularMes()
            calcularMes()
            calcularMes()
            calcularMes()
            calcularMes()
            calcularMes()
            calcularMes()
            calcularMes()
        }

        val dataset = BarDataSet(entriesImp, "")
        val colors = ArrayList<Int>()
        for (c in ColorTemplate.VORDIPLOM_COLORS) colors.add(c)
        for (c in ColorTemplate.COLORFUL_COLORS) colors.add(c)
        for (c in ColorTemplate.JOYFUL_COLORS) colors.add(c)
        dataset.colors = colors

        val data = BarData(dataset)
        chart.data = data
        chart.animateY(2000)

        // Configuramos la leyenda del gr??fico
        val legend = chart.legend
        legend.isEnabled = false

        // Configuramos el eje X del gr??fico
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.TOP
        xAxis.textSize = 10f
        xAxis.textColor = Color.parseColor("#009DD2")
        xAxis.setDrawAxisLine(true)
        xAxis.setDrawGridLines(false)
        // Indicamos que queremos los 13 nombres de los meses. Si no lo indicamos nos representa s??lo la mitad.
        xAxis.labelCount = 13
        // Establecemos las etiquetas de cada columna
        xAxis.valueFormatter = MyXAxisValueFormatter(labels)

        val description = chart.description
        description.text = "Total importe: " + String.format(fFtoImpteII, fTotalImpte) + " ???"
        description.textSize = 12f
        description.textColor = Color.RED
    }

    fun calcularMes() {
        var sImpte: String
        var fImpte = 0.0f

        for (hco in fHcoMes.lDatosHistMes) {
            if (hco.mes == queMes) {
                sImpte = if (estoyEnAnyoActual) {
                    hco.importe
                } else {
                    hco.importeAnt
                }

                fImpte += sImpte.toFloat()
            }
        }
        entriesImp.add(BarEntry(posicion, fImpte))
        // Calculamos los totales
        fTotalImpte += fImpte

        calculaLabel(queMes)
        posicion++
        queMes++
        if (queMes > 12) {
            queMes = 1
            estoyEnAnyoActual = true
        }
    }


    private fun calculaLabel(queMes: Int) {
        when (queMes) {
            1 -> labels[posicion.toInt()] = "EN"
            2 -> labels[posicion.toInt()] = "FB"
            3 -> labels[posicion.toInt()] = "MZ"
            4 -> labels[posicion.toInt()] = "AB"
            5 -> labels[posicion.toInt()] = "MY"
            6 -> labels[posicion.toInt()] = "JN"
            7 -> labels[posicion.toInt()] = "JL"
            8 -> labels[posicion.toInt()] = "AG"
            9 -> labels[posicion.toInt()] = "SP"
            10 -> labels[posicion.toInt()] = "OC"
            11 -> labels[posicion.toInt()] = "NV"
            12 -> labels[posicion.toInt()] = "DC"
        }
    }


    inner class MyXAxisValueFormatter(private val mValues: Array<String>): IAxisValueFormatter {

        override fun getFormattedValue(value: Float, axis: AxisBase): String {
            // "value" represents the position of the label on the axis (x or y)
            return mValues[value.toInt()]
        }
    }

}