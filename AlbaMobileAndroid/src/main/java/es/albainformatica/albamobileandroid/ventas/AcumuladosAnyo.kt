package es.albainformatica.albamobileandroid.ventas

import es.albainformatica.albamobileandroid.dimeNombreMesAbrev
import android.app.Activity
import es.albainformatica.albamobileandroid.historicos.HistoricoMes
import android.widget.LinearLayout
import android.widget.TextView
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.view.View
import android.widget.ImageView
import es.albainformatica.albamobileandroid.R
import kotlin.math.roundToInt

/**
 * Created by jabegines on 30/11/2017.
 */
class AcumuladosAnyo: Activity() {
    private var fCodArt: String = ""
    private var fDescrArt: String = ""
    private lateinit var fHistorico: HistoricoMes
    private lateinit var linearChart: LinearLayout
    lateinit var tvArticulo: TextView


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.acumulados_anyo)

        val i = intent
        val fCliente = i.getIntExtra("cliente", 0)
        val fArticulo = i.getIntExtra("articulo", 0)
        fCodArt = i.getStringExtra("codart") ?: ""
        fDescrArt = i.getStringExtra("descrart") ?: ""
        fHistorico = HistoricoMes(this)
        fHistorico.abrirAnyo(fCliente, fArticulo)
        inicializarControles()
        mostrarGrafico()
    }


    private fun inicializarControles() {
        linearChart = findViewById<View>(R.id.linearChart) as LinearLayout
        tvArticulo = findViewById<View>(R.id.textViewArt) as TextView
        val queTexto = "$fCodArt - $fDescrArt"
        tvArticulo.text = queTexto
    }

    private fun mostrarGrafico() {
        var mesActual = 1

        for (hco in fHistorico.lDatosHMAnyo) {
            val queMes = hco.mes
            while (mesActual < queMes) {
                drawChart(0, mesActual)
                mesActual++
            }

            val sCantidad = hco.cantidad.replace(',', '.')
            val dCantidad = sCantidad.toDouble()
            val queCantidad = dCantidad.roundToInt()

            drawChart(queCantidad, hco.mes)
            mesActual++
        }

        if (mesActual < 13) {
            while (mesActual < 13) {
                drawChart(0, mesActual)
                mesActual++
            }
        }
    }

    private fun drawChart(altura: Int, queMes: Int) {
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val llGrupo = LinearLayout(this)
        llGrupo.orientation = LinearLayout.VERTICAL
        params.leftMargin = 15
        llGrupo.layoutParams = params
        val text = TextView(this)
        text.text = altura.toString()
        text.gravity = Gravity.CENTER
        llGrupo.addView(text)
        val image = ImageView(this)
        val badge = ShapeDrawable(RectShape())
        badge.intrinsicWidth = 50
        badge.intrinsicHeight = altura.coerceAtMost(1000)
        badge.paint.color = Color.GRAY
        image.setImageDrawable(badge)
        llGrupo.addView(image)
        val mes = TextView(this)
        mes.text = dimeNombreMesAbrev(queMes)
        mes.gravity = Gravity.CENTER
        llGrupo.addView(mes)
        linearChart.addView(llGrupo)
    }
}