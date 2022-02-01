package es.albainformatica.albamobileandroid.historicos

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import androidx.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import android.view.KeyEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import es.albainformatica.albamobileandroid.*
import kotlinx.android.synthetic.main.cargar_hcopordoc.*
import org.jetbrains.anko.alert


class CargarHcoPorDoc: AppCompatActivity() {

    private lateinit var fHistorico: Historico
    private var fCliente: Int = 0

    private lateinit var fRecArticulos: RecyclerView
    private lateinit var fRecDocs: RecyclerView
    private lateinit var fAdpArt: ArtHcoDocRvAdapter
    private lateinit var fAdpDocs: DocHcoDocRvAdapter


    private lateinit var prefs: SharedPreferences
    private var queOrdenacion: Short = 0    // 0-> por descripción, 1-> por código

    private val fRequestEditarHco = 1



    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.cargar_hcopordoc)

        fHistorico = Comunicador.fHistorico
        val i = intent
        fCliente = i.getIntExtra("cliente", 0)

        inicializarControles()
    }

    override fun onDestroy() {
        guardarPreferencias()

        super.onDestroy()
    }


    private fun inicializarControles() {
        leerPreferencias()

        fRecArticulos = rvArtHcoPorDoc
        fRecArticulos.layoutManager = LinearLayoutManager(this)
        fRecDocs = rvDocHcoPorDoc
        fRecDocs.layoutManager = LinearLayoutManager(this)

        prepararRvArticulos()

        // Mediante este código seleccionamos el primer registro del recyclerView y hacemos como si pulsáramos
        // click en él. Hay que hacerlo con un Handler().postDelayed() porque si no, da errores.
        if (fAdpArt.docs.count() > 0) {
            Handler().postDelayed({
                fRecArticulos.findViewHolderForAdapterPosition(0)?.itemView?.performClick()
            }, 100)
        }
    }

    private fun leerPreferencias() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        queOrdenacion = prefs.getInt("cathcodoc_orden", 0).toShort()
    }

    private fun guardarPreferencias() {
        prefs.edit().putInt("cathcodoc_orden", queOrdenacion.toInt()).apply()
    }


    private fun prepararRvArticulos() {
        fAdpArt = ArtHcoDocRvAdapter(getArticulos(), this, object: ArtHcoDocRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: DatosArtHcArtClte) {
                prepararRvDocs()
            }
        })

        fRecArticulos.adapter = fAdpArt
    }


    private fun getArticulos(): List<DatosArtHcArtClte> {
        fHistorico.abrirArtsHcoPorArtClte(fCliente, queOrdenacion)
        return fHistorico.lDatArtHcoArtClte
    }


    fun ordenarArt(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (queOrdenacion.toInt() == 1) {
            queOrdenacion = 0
            btnOrdenar.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(this, R.drawable.ordenacion_alf), null, null)
        } else {
            queOrdenacion = 1
            btnOrdenar.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(this, R.drawable.ordenacion_cod), null, null)
        }
        prepararRvArticulos()
        fAdpArt.articuloId = fAdpArt.docs[0].articuloId
        prepararRvDocs()
    }


    private fun prepararRvDocs() {
        fAdpDocs = DocHcoDocRvAdapter(getDocs(), this, object: DocHcoDocRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: DatosDocsHcArtClte) {

                val i = Intent(this@CargarHcoPorDoc, EditarHcoActivity::class.java)
                i.putExtra("linea", data.hcoPorArticClteId)
                i.putExtra("desdeHcoDoc", true)
                i.putExtra("articulo", data.articuloId)
                startActivityForResult(i, fRequestEditarHco)
            }
        })

        fRecDocs.adapter = fAdpDocs
    }


    private fun getDocs(): List<DatosDocsHcArtClte> {
        fHistorico.abrirDocsHcoPorArtClte(fAdpArt.articuloId, fCliente)
        return fHistorico.lDatDocHcoArtClte
    }



    fun editarHco(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fAdpArt.articuloId > 0) {
            val i = Intent(this, EditarHcoActivity::class.java)
            i.putExtra("linea", fAdpArt.idHco)
            i.putExtra("desdeHcoArtClte", true)
            i.putExtra("articulo", fAdpArt.articuloId)
            startActivityForResult(i, fRequestEditarHco)
        }
    }



    fun salvarHco(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val returnIntent = Intent()
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Actividad editar historico.
        if (requestCode == fRequestEditarHco) {
            if (resultCode == Activity.RESULT_OK) {
                // Refrescamos el adaptador del recyclerView por si hemos indicado alguna cantidad a vender
                prepararRvArticulos()
            }
        }
    }


    fun cancelarHco(view: View?) {
        view?.getTag(0)          // Para que no dé warning el compilador

        alert("¿Cancelar histórico?") {
            title = "Abandonar"
            positiveButton("SI") {
                fHistorico.borrar()
                val returnIntent = Intent()
                setResult(Activity.RESULT_CANCELED, returnIntent)
                finish()
            }
            negativeButton("NO") { }
        }.show()
    }


    // Manejo los eventos del teclado en la actividad.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            cancelarHco(null)
        }
        // Para las demas cosas, se reenvia el evento al listener habitual.
        return super.onKeyDown(keyCode, event)
    }


}