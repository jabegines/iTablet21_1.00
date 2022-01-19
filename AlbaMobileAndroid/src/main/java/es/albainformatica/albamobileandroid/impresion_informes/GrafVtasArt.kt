package es.albainformatica.albamobileandroid.impresion_informes


import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.historicos.HistoricoMes
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import es.albainformatica.albamobileandroid.Configuracion
import es.albainformatica.albamobileandroid.Comunicador
import java.util.*


class GrafVtasArt: AppCompatActivity() {
    private var fArticulo: Int = 0
    private var fDescr: String = ""
    private var fCliente: Int = 0
    private var fNombre: String = ""
    private lateinit var fHcoMes: HistoricoMes
    private lateinit var fConfiguracion: Configuracion
    private var mesActual = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var queMes = mesActual
    private var posicion = 0f
    private var estoyEnAnyoActual = false
    private var entriesImp = ArrayList<BarEntry>()
    private var entriesCant = ArrayList<PieEntry>()
    private lateinit var labels: Array<String>
    private val colors = ArrayList<Int>()
    private var fTotalImpte: Double = 0.0
    private var fTotalCant: Double = 0.0
    private var fFtoImpteII = ""
    private var fFtoDecCant = ""


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.graf_vtas_art)

        val i = intent
        fArticulo = i.getIntExtra("articulo", 0)
        fDescr = i.getStringExtra("descripcion") ?: ""
        fCliente = i.getIntExtra("cliente", 0)
        fNombre = i.getStringExtra("nombre") ?: ""
        fHcoMes = HistoricoMes(this)
        fConfiguracion = Comunicador.fConfiguracion

        inicializarControles()
        graficoBarras()
    }



    fun inicializarControles() {
        val tvDescr = findViewById<TextView>(R.id.tvVtasArtDescr)
        val tvNombre = findViewById<TextView>(R.id.tvVtasArtNClte)
        tvDescr.text = fDescr
        tvNombre.text = fNombre

        fFtoImpteII = fConfiguracion.formatoDecImptesIva()
        fFtoDecCant = fConfiguracion.formatoDecCantidad()

        for (c in ColorTemplate.VORDIPLOM_COLORS) colors.add(c)
        for (c in ColorTemplate.COLORFUL_COLORS) colors.add(c)
        for (c in ColorTemplate.JOYFUL_COLORS) colors.add(c)
    }

    private fun graficoTarta() {
        val chart = findViewById<PieChart>(R.id.piechartArt)
        val dataset = PieDataSet(entriesCant, "")

        dataset.sliceSpace = 3f
        dataset.selectionShift = 5f

        dataset.valueLinePart1OffsetPercentage = 80f
        dataset.valueLinePart1Length = 0.2f
        dataset.valueLinePart2Length = 0.5f
        dataset.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataset.colors = colors


        val data = PieData(dataset)
        chart.data = data

        val description = chart.description
        description.text = "Total cantidad: " + String.format(fFtoDecCant, fTotalCant)
        description.textSize = 12f
        description.textColor = Color.RED

        chart.animateXY(2000, 2000)
        chart.setUsePercentValues(false)

        val l = chart.legend
        l.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        l.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        l.orientation = Legend.LegendOrientation.VERTICAL
        l.setDrawInside(false)
        l.isEnabled = false


        chart.centerText = "Ventas por artículo\ny cliente"
        chart.setDrawCenterText(true)
    }


    private fun graficoBarras() {
        val chart = findViewById<BarChart>(R.id.barchartArt)
        labels = arrayOf("", "", "", "", "", "", "", "", "", "", "", "", "")

        // Recorreremos la tabla histmes desde el mes actual del año anterior hasta el mes actual de este año,
        // por eso llamamos a calcularMes 13 veces.
        if (fHcoMes.abrirArticulo(fArticulo, fCliente)) {
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
        dataset.colors = colors

        val data = BarData(dataset)
        chart.data = data
        chart.animateY(2000)

        // Configuramos la leyenda del gráfico
        val legend = chart.legend
        legend.isEnabled = false

        // Configuramos el eje X del gráfico
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.TOP
        xAxis.textSize = 10f
        xAxis.textColor = Color.parseColor("#009DD2")
        xAxis.setDrawAxisLine(true)
        xAxis.setDrawGridLines(false)
        // Indicamos que queremos los 13 nombres de los meses. Si no lo indicamos nos representa sólo la mitad.
        xAxis.labelCount = 13
        // Establecemos las etiquetas de cada columna
        xAxis.valueFormatter = MyXAxisValueFormatter(labels)

        val description = chart.description
        description.text = "Total importe: " + String.format(fFtoImpteII, fTotalImpte) + " €"
        description.textSize = 12f
        description.textColor = Color.RED

        // Llamamos al gráfico de tarta para representar las cantidades
        graficoTarta()
    }



    fun calcularMes() {
        var sImpte: String
        var fImpte = 0.0f
        var sCantidad: String
        var fCantidad = 0.0f

        for (hco in fHcoMes.lDatosHistMes) {
            if (hco.mes == queMes) {
                if (estoyEnAnyoActual) {
                    sImpte = hco.importe
                    sCantidad = hco.cantidad
                } else {
                    sImpte = hco.importeAnt
                    sCantidad = hco.cantidadAnt
                }

                fImpte += java.lang.Float.parseFloat(sImpte)
                fCantidad += java.lang.Float.parseFloat(sCantidad)
            }
        }
        calculaLabel()

        entriesImp.add(BarEntry(posicion, fImpte))
        if (fCantidad != 0.0f) entriesCant.add(PieEntry(fCantidad, labels[posicion.toInt()]))
        // Calculamos los totales
        fTotalImpte += fImpte
        fTotalCant += fCantidad

        posicion++
        queMes++
        if (queMes > 12) {
            queMes = 1
            estoyEnAnyoActual = true
        }
    }



    private fun calculaLabel() {
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

        /** this is only needed if numbers are returned, else return 0  */
        //fun getDecimalDigits(): Int {
        //    return 0
        //}
    }
}