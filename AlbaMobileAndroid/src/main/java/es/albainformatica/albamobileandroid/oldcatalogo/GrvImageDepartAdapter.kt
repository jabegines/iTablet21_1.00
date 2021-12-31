package es.albainformatica.albamobileandroid.oldcatalogo

import es.albainformatica.albamobileandroid.dimeRutaImagenes
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.widget.BaseAdapter
import es.albainformatica.albamobileandroid.maestros.Departamentos
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import es.albainformatica.albamobileandroid.R
import java.io.File
import java.util.ArrayList

class GrvImageDepartAdapter(private val activity: Activity, fGrupo: Int) : BaseAdapter() {
    private val carpetaImagenes: String = dimeRutaImagenes(activity)
    private val fDepartamentos: Departamentos = Departamentos(activity)
    private val itemsDepartam: ArrayList<ItemDepartam>

    private fun obtenerItems(): ArrayList<ItemDepartam> {
        val items = ArrayList<ItemDepartam>()
        if (fDepartamentos.cursor.moveToFirst()) {
            fDepartamentos.cursor.moveToPosition(-1)
            while (fDepartamentos.cursor.moveToNext()) {
                items.add(
                    ItemDepartam(
                        fDepartamentos.getGrupo(),
                        fDepartamentos.getCodigo(),
                        fDepartamentos.getDescripcion()
                    )
                )
            }
        }
        return items
    }

    // Devuelve el número de elementos que se introducen en el adapter
    override fun getCount(): Int {
        return itemsDepartam.size
    }

    override fun getItem(position: Int): Any {
        return itemsDepartam[position]
    }

    override fun getItemId(position: Int): Long {
        return itemsDepartam[position].codigo.toLong()
    }

    // Crear un nuevo ImageView para cada item referenciado por el Adapter
    override fun getView(position: Int, contentView: View?, parent: ViewGroup?): View? {
        // Este método crea una nueva View para cada elemento añadido al ImageAdapter.
        // Se le pasa el View en el que se ha pulsado, contentView.
        // Si contentView es null, se instancia y configura un ImageView con las
        // propiedades deseadas para la presentación de la imagen junto con un TextView para el nombre del departamento.
        // Si contentView no es null, el ImageView local es inicializado con este objeto View.
        var vi = contentView
        if (contentView == null) {
            val inflater =
                activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            vi = inflater.inflate(R.layout.ly_cat_grupos, null)
        }
        val item = itemsDepartam[position]

        // Obtenemos la imagen del departamento
        val imageView = vi?.findViewById<View>(R.id.imvLyClasific) as ImageView
        val path = carpetaImagenes + item.imagen
        val file = File(path)
        if (file.exists()) imageView.setImageURI(Uri.parse(path)) else imageView.setImageDrawable(
            null
        )
        val tvDescr = vi.findViewById<View>(R.id.tvLyClasific) as TextView
        tvDescr.text = item.descr
        return vi
    }

    init {
        fDepartamentos.abrir(fGrupo)
        itemsDepartam = obtenerItems()
    }
}