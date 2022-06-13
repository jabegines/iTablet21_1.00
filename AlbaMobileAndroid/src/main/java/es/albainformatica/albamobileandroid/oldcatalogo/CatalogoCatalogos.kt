package es.albainformatica.albamobileandroid.oldcatalogo

import android.app.Activity
import android.os.Bundle
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import android.content.Intent
import android.preference.PreferenceManager
import android.view.View
import android.widget.TextView
import android.widget.GridView
import android.widget.AdapterView
import android.widget.Button
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.registroEventos.RegistroEventosClase

class CatalogoCatalogos : Activity() {
    private lateinit var fRegEventos: RegistroEventosClase

    private var fCatalogo = 0
    private var fDescrCat: String = ""
    private var fVendiendo: Boolean = false

    private val fRequestCatalogoArt = 1

    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.catalogo_catalogos)

        fRegEventos = Comunicador.fRegEventos
        fRegEventos.registrarEvento(codEv_ArticCatal_Entrar, descrEv_ArticCatal_Entrar)

        // Pasamos fArticulosGrv al comunicador para hacer uso del objeto en CatalogoFichaArtic.
        Comunicador.fArticulosGrv = ArticulosClase(this)

        // fVendiendo nos servirá para saber si hemos entrado desde ventas o desde la ficha de artículos.
        val i = intent
        fVendiendo = i.getBooleanExtra("vendiendo", false)
        inicializarControles()
    }

    override fun onDestroy() {
        guardarPreferencias()
        fRegEventos.registrarEvento(codEv_ArticCatal_Salir, descrEv_ArticCatal_Salir)

        super.onDestroy()
    }

    private fun guardarPreferencias() {
        // Hay que tener cuidado con ésto. El programa pasa por aquí (y por el onDestroy) una vez que
        // la actividad a la que retornamos ha tomado el control. Si en dicha actividad (p.ej. VentasLineas) queremos
        // hacer uso de cualquiera de las preferencias que vamos a guardar a continuación, aún no tendrán los valores
        // que vamos a guardar, puesto que, como digo, por aquí pasa DESPUES de que la actividad padre haya sido lanzada de nuevo.
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        pref.edit().putInt("modoVisArtic", CATALOGOS).apply()
    }

    private fun inicializarControles() {
        val btnHco = findViewById<Button>(R.id.btnCatGrpHco)
        if (!fVendiendo) btnHco.visibility = View.GONE
        ocultarTeclado(this)
        verCatalogos()
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.mni_vercatalogos)
    }

    private fun verCatalogos() {
        val grvCatalogos = findViewById<GridView>(R.id.grvCatalogos)
        grvCatalogos.adapter = GrvImageCatalogosAdapter(this)
        grvCatalogos.onItemClickListener =
            AdapterView.OnItemClickListener { parent: AdapterView<*>, _: View?, position: Int, _: Long ->
                val queItem = parent.getItemAtPosition(position) as ItemCatalogo
                fCatalogo = queItem.codigo
                fDescrCat = queItem.descr
                verArticulos()
            }
    }

    private fun verArticulos() {
        val i = Intent(this, CatalogoArticulos::class.java)
        i.putExtra("modoVisArtic", CATALOGOS)
        i.putExtra("catalogo", fCatalogo)
        i.putExtra("descr_titulo", fDescrCat)
        i.putExtra("vendiendo", fVendiendo)
        startActivityForResult(i, fRequestCatalogoArt)
    }

    /*
    public void irAlGrupo(View view) {
        // Tenemos que tener esta función para que no nos dé error al pulsar sobre el textview en la actividad
    }
    */
    /*
    public void catBuscarArt(View view) {
        // Tenemos que tener esta función para que no nos dé error al pulsar sobre la lupa en la actividad
    }
    */
    fun aceptarCatalogo(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador
    }

    private fun irAInicio(view: View?) {
        view?.getTag(0)              // Para que no dé warning el compilador

        // Si estamos vendiento el botón Home nos servirá para volver al documento de venta.
        if (fVendiendo) {
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Actividad articulos del catálogo.
        if (requestCode == fRequestCatalogoArt) {
            if (resultCode == RESULT_OK) {
                // Si venimos de la venta, abandonamos la actividad para retornar a la pantalla de ventas.
                if (fVendiendo) {
                    val returnIntent = Intent()
                    returnIntent.putExtra("vengoDe", CATALOGOS)
                    setResult(RESULT_OK, returnIntent)
                    finish()
                } else {
                    val irADocumento = data?.getBooleanExtra("irADocumento", false) ?: false
                    if (irADocumento) irAInicio(null)
                }
            }
        }
    }

    fun modoLista(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val returnIntent = Intent()
        returnIntent.putExtra("voyA", LISTA_ARTICULOS)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    fun verGrupos(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        val returnIntent = Intent()
        returnIntent.putExtra("voyA", GRUPOS_Y_DEP)
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


}