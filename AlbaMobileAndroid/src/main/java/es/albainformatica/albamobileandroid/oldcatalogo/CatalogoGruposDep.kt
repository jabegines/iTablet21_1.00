package es.albainformatica.albamobileandroid.oldcatalogo

import android.app.Activity
import es.albainformatica.albamobileandroid.maestros.Grupos
import es.albainformatica.albamobileandroid.maestros.Departamentos
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import es.albainformatica.albamobileandroid.*
import java.io.File

class CatalogoGruposDep: Activity() {
    private lateinit var fGrupos: Grupos
    private lateinit var fDepartamentos: Departamentos
    private lateinit var fArticulosGrv: ArticulosClase
    private lateinit var lyScrollGrupos: LinearLayout
    private lateinit var grvDepartam: GridView

    private var fGrupo: Short = 0
    private var fVendiendo: Boolean = false


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.catalogo_gruposdep)

        fGrupos = Grupos(this)
        fDepartamentos = Departamentos(this)
        fArticulosGrv = ArticulosClase(this)
        // Pasamos fArticulosGrv al comunicador para hacer uso del objeto en CatalogoFichaArtic.
        Comunicador.fArticulosGrv = fArticulosGrv

        // fVendiendo nos servirá para saber si hemos entrado desde ventas o desde la ficha de artículos.
        val i = intent
        fVendiendo = i.getBooleanExtra("vendiendo", false)
        inicializarControles()
    }

    override fun onDestroy() {
        guardarPreferencias()
        fArticulosGrv.close()
        super.onDestroy()
    }

    private fun guardarPreferencias() {
        // Hay que tener cuidado con ésto. El programa pasa por aquí (y por el onDestroy) una vez que
        // la actividad a la que retornamos ha tomado el control. Si en dicha actividad (p.ej. VentasLineas) queremos
        // hacer uso de cualquiera de las preferencias que vamos a guardar a continuación, aún no tendrán los valores
        // que vamos a guardar, puesto que, como digo, por aquí pasa DESPUES de que la actividad padre haya sido lanzada de nuevo.
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        pref.edit().putInt("modoVisArtic", GRUPOS_Y_DEP).apply()
    }

    private fun inicializarControles() {
        val btnHco = findViewById<Button>(R.id.btnCatGrpHco)
        if (!fVendiendo) btnHco.visibility = View.GONE
        ocultarTeclado(this)
        grvDepartam = findViewById(R.id.grvDepart)
        carpetaImagenes = dimeRutaImagenes(this)
        lyScrollGrupos = findViewById(R.id.lyScrollGrupos)
        verGrupos()
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.grupos_y_dep)
    }

    private fun verGrupos() {
        if (fGrupos.abrir()) {
            val inflater = this.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            for (grupo in fGrupos.lGrupos) {
                // Usamos un layout donde tenemos una imagen y una etiqueta.
                val vi = inflater.inflate(R.layout.ly_cat_grupos, null)
                val imageView = vi.findViewById<ImageView>(R.id.imvLyClasific)
                imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                val queFichero = carpetaImagenes + "GRP_" + grupo.codigo + ".jpg"
                val file = File(queFichero)
                if (file.exists())
                    imageView.setImageURI(Uri.parse(queFichero)) else imageView.setImageDrawable(null)

                imageView.tag = ponerCeros(grupo.codigo.toString(), ancho_grupo) + grupo.descripcion
                imageView.setOnClickListener { v: View ->
                    val queTag = v.tag.toString()
                    val sGrupo = queTag.substring(0, ancho_grupo.toInt())
                    fGrupo = sGrupo.toShort()

                    cambiarAlpha(fGrupo, false)
                    verDepartamentos(fGrupo)
                }
                val tvDescr = vi.findViewById<TextView>(R.id.tvLyClasific)
                tvDescr.tag = ponerCeros(grupo.codigo.toString(), ancho_grupo) + grupo.descripcion
                tvDescr.text = grupo.descripcion
                lyScrollGrupos.addView(vi)
            }
        }
    }

    private fun cambiarAlpha(queGrupo: Short, todos: Boolean) {
        // Recorremos el layout lyScrollGrupos y vamos cambiando el valor Alpha de los ImageView.
        for (i in 0 until lyScrollGrupos.childCount) {
            val rl = lyScrollGrupos.getChildAt(i) as LinearLayout
            for (x in 0 until rl.childCount) {
                val v = rl.getChildAt(x)
                val c: Class<*> = v.javaClass
                if (c == ImageView::class.java) {
                    val iv = v as ImageView
                    val sGrupo = iv.tag.toString().substring(0, 3)
                    val iGrupo = sGrupo.toShort()
                    if (todos) {
                        iv.imageAlpha = 255
                    } else {
                        if (iGrupo == queGrupo) iv.imageAlpha = 255 else iv.imageAlpha = 40
                    }
                } else if (c == TextView::class.java) {
                    val tv = v as TextView
                    val sText = tv.tag.toString().substring(0, 3)
                    val iText = sText.toShort()
                    if (todos) {
                        tv.alpha = 0.4f
                    } else {
                        if (iText == queGrupo) tv.alpha = 1f else tv.alpha = 0.4f
                    }
                }
            }
        }
    }

    private fun verArticulos(queGrupo: Short, queDepart: Short, fDescrDep: String) {
        val i = Intent(this, CatalogoArticulos::class.java)
        i.putExtra("modoVisArtic", GRUPOS_Y_DEP)
        i.putExtra("grupo", queGrupo)
        i.putExtra("departamento", queDepart)
        //i.putExtra("descrgrupo", tvDescrGrupo.getText());
        i.putExtra("descr_titulo", fDescrDep)
        i.putExtra("vendiendo", fVendiendo)
        startActivityForResult(i, REQUEST_CAT_ARTICULOS)
    }

    private fun verDepartamentos(queGrupo: Short) {
        grvDepartam.adapter = GrvImageDepartAdapter(this, queGrupo)

        // Si el grupo no tiene departamentos iremos directamente a ver los artículos del grupo.
        if (queGrupo > 0) {
            if (grvDepartam.adapter.count == 0) {
                verArticulos(queGrupo, 0, "")
            }
        }
        grvDepartam.onItemClickListener =
            AdapterView.OnItemClickListener { parent: AdapterView<*>, _: View?, position: Int, _: Long ->
                val queItem = parent.getItemAtPosition(position) as ItemDepartam
                val fDepartam = queItem.codigo
                val fDescrDep = queItem.descr
                verArticulos(queGrupo, fDepartam, fDescrDep)
            }
    }

    private fun irAInicio(view: View?) {
        view?.getTag(0)              // Para que no dé warning el compilador

        verDepartamentos(0)
        cambiarAlpha(0, true)
        //verNombreGrupo("GRUPOS");
        fGrupo = 0
    }

    /*
    fun irAlGrupo(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        verDepartamentos(fGrupo)
    }
    */


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Venimos de la pantalla de artículos
        if (requestCode == REQUEST_CAT_ARTICULOS) {
            if (resultCode == RESULT_OK) {
                // Si venimos de la venta, abandonamos la actividad para retornar a la pantalla de ventas.
                if (fVendiendo) {
                    val returnIntent = Intent()
                    returnIntent.putExtra("vengoDe", GRUPOS_Y_DEP)
                    setResult(RESULT_OK, returnIntent)
                    finish()
                } else {
                    val irAGrupo = data?.getBooleanExtra("irAGrupo", false) ?: false
                    if (irAGrupo) {
                        verDepartamentos(fGrupo)
                    } else irAInicio(null)
                }
            }
        }
    }

    /*
    fun catBuscarArt(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador
        // Tenemos que tener esta función para que no nos dé error al pulsar sobre la lupa en la actividad
    }
    */

    fun aceptarCatalogo(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador
    }

    fun modoLista(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val returnIntent = Intent()
        returnIntent.putExtra("voyA", LISTA_ARTICULOS)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    fun verCatalogos(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val returnIntent = Intent()
        returnIntent.putExtra("voyA", CATALOGOS)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    fun verHistorico(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        if (fVendiendo) {
            val returnIntent = Intent()
            returnIntent.putExtra("voyA", HISTORICO)
            setResult(RESULT_OK, returnIntent)
            finish()
        }
    }

    companion object {
        private var carpetaImagenes: String? = null
        private const val REQUEST_CAT_ARTICULOS = 1
    }
}