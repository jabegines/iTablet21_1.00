package es.albainformatica.albamobileandroid.maestros

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import es.albainformatica.albamobileandroid.R
import java.io.File

/**
 * Created by iguerrero on 19/06/2018.
 */

class ArticuloImagen : Activity() {
    private var fImagen: String = ""


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.articulo_imagen)

        val i = intent
        fImagen = i.getStringExtra("imagen") ?: ""

        inicializarControles()
    }


    private fun inicializarControles() {
        val imgArticulo: ImageView = findViewById(R.id.imgArticulo)
        val file = File(fImagen)
        if (file.exists())
            imgArticulo.setImageURI(Uri.parse(fImagen))
        else
            imgArticulo.setImageBitmap(null)
    }

}
