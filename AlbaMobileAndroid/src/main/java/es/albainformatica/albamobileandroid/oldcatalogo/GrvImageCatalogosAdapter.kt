package es.albainformatica.albamobileandroid.oldcatalogo

import es.albainformatica.albamobileandroid.dimeRutaImagenes
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.widget.BaseAdapter
import es.albainformatica.albamobileandroid.maestros.Clasificadores
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import es.albainformatica.albamobileandroid.R
import java.io.File
import java.util.ArrayList

class GrvImageCatalogosAdapter(private val activity: Activity): BaseAdapter() {
    private val carpetaImagenes: String = dimeRutaImagenes(activity)
    private val fCatalogos: Clasificadores = Clasificadores(activity)
    private val itemsCatalogos: ArrayList<ItemCatalogo>


    init {
        fCatalogos.abrirCatalogos()
        itemsCatalogos = obtenerItems()
    }



    private fun obtenerItems(): ArrayList<ItemCatalogo> {
        val items = ArrayList<ItemCatalogo>()
        if (fCatalogos.lClasificadores.isNotEmpty()) {

            for (catalogo in fCatalogos.lClasificadores) {
                items.add(ItemCatalogo(catalogo.clasificadorId, catalogo.descripcion))
            }
        }
        return items
    }


    // Devuelve el número de elementos que se introducen en el adapter
    override fun getCount(): Int {
        return itemsCatalogos.size
    }


    override fun getItem(position: Int): Any {
        return itemsCatalogos[position]
    }

    override fun getItemId(position: Int): Long {
        return itemsCatalogos[position].codigo.toLong()
    }

    // Crear un nuevo ImageView para cada item referenciado por el Adapter
    override fun getView(position: Int, contentView: View?, parent: ViewGroup?): View? {
        // Este método crea una nueva View para cada elemento añadido al ImageAdapter.
        // Se le pasa el View en el que se ha pulsado, contentView.
        // Si contentView es null, se instancia y configura un ImageView con las
        // propiedades deseadas para la presentación de la imagen junto con un TextView para el nombre del catálogo.
        // Si contentView no es null, el ImageView local es inicializado con este objeto View.
        var vi = contentView
        if (contentView == null) {
            val inflater =
                activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            vi = inflater.inflate(R.layout.ly_cat_grupos, null)
        }
        val item = itemsCatalogos[position]

        // Obtenemos la imagen del catálogo
        val imageView = vi?.findViewById<ImageView>(R.id.imvLyClasific)
        val path = carpetaImagenes + item.imagen
        val file = File(path)
        if (file.exists()) imageView?.setImageURI(Uri.parse(path)) else imageView?.setImageDrawable(null)
        val tvDescr = vi?.findViewById<View>(R.id.tvLyClasific) as TextView
        tvDescr.text = item.descr
        return vi
    }

}