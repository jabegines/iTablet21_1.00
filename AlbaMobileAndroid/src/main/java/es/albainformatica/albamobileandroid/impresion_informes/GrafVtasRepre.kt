package es.albainformatica.albamobileandroid.impresion_informes

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
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
import es.albainformatica.albamobileandroid.entity.HistRepreEnt
import es.albainformatica.albamobileandroid.historicos.HistoricoRepre
import java.util.*


class GrafVtasRepre: AppCompatActivity() {
    private lateinit var fHcoRepre: HistoricoRepre
    private lateinit var fConfiguracion: Configuracion
    private var queMes = 0
    private var queAnyo = 0
    private var posicion = 0f
    private var entriesImp = ArrayList<BarEntry>()
    private lateinit var labels: Array<String>
    private var fTotalImpte: Double = 0.0
    private var fFtoImpteII = ""
    private lateinit var tvNombre: TextView


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.graf_vtas_clte)

        //val i = intent
        fHcoRepre = HistoricoRepre(this)
        fConfiguracion = Comunicador.fConfiguracion

        inicializarControles()
        graficoBarras()
    }



    fun inicializarControles() {
        tvNombre = findViewById(R.id.tvVtasClte)
        tvNombre.gravity = Gravity.END
        tvNombre.text = ""

        fFtoImpteII = fConfiguracion.formatoDecImptesIva()
    }

    @SuppressLint("SetTextI18n")
    private fun graficoBarras() {
        val chart = findViewById<BarChart>(R.id.barchartClte)
        labels = arrayOf("", "", "", "", "", "", "", "", "", "", "", "", "")

        val mesActual = Calendar.getInstance().get(Calendar.MONTH) + 1
        val anyoActual = Calendar.getInstance().get(Calendar.YEAR)

        prepararMesYAnyo(mesActual, anyoActual)

        val lHcoRepre = fHcoRepre.abrir()
        if (lHcoRepre.isNotEmpty()) {
            calcularMes(lHcoRepre)
            calcularMes(lHcoRepre)
            calcularMes(lHcoRepre)
            calcularMes(lHcoRepre)
            calcularMes(lHcoRepre)
            calcularMes(lHcoRepre)
            calcularMes(lHcoRepre)
            calcularMes(lHcoRepre)
            calcularMes(lHcoRepre)
            calcularMes(lHcoRepre)
            calcularMes(lHcoRepre)
            calcularMes(lHcoRepre)
            calcularMes(lHcoRepre)
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

        // Configuramos la leyenda del gráfico
        val legend = chart.legend
        legend.isEnabled = false

        // Configuramos el eje X del gráfico
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textSize = 10f
        xAxis.textColor = Color.RED
        xAxis.setDrawAxisLine(true)
        xAxis.setDrawGridLines(false)
        // Indicamos que queremos los 13 nombres de los meses. Si no lo indicamos nos representa sólo la mitad.
        xAxis.labelCount = 13
        // Establecemos las etiquetas de cada columna
        xAxis.valueFormatter = MyXAxisValueFormatter(labels)

        val description = chart.description
        description.text = ""

        tvNombre.text = getString(R.string.totalVentas) + String.format(fFtoImpteII, fTotalImpte) + getString(R.string.simboloEuro)
    }


    private fun calcularMes(lHcoRepre: MutableList<HistRepreEnt>) {
        var sImpte: String
        var fImpte = 0.0f

        for (hcoRepre in lHcoRepre) {
            if (hcoRepre.mes == queMes && hcoRepre.anyo == queAnyo) {

                sImpte = hcoRepre.importe
                fImpte += java.lang.Float.parseFloat(sImpte)
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
            queAnyo++
        }
    }

    private fun prepararMesYAnyo(mesActual: Int, anyoActual: Int) {

        queMes = mesActual
        queAnyo = anyoActual

        for (i in 1..12) {
            queMes--
            if (queMes < 1) {
                queMes = 12
                queAnyo--
            }
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