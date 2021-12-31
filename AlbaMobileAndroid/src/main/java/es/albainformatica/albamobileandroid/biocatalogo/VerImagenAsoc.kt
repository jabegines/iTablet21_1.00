package es.albainformatica.albamobileandroid.biocatalogo

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import es.albainformatica.albamobileandroid.R
import kotlinx.android.synthetic.main.bio_ver_imagen_asoc.*
import java.io.File


class VerImagenAsoc: AppCompatActivity() {


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.bio_ver_imagen_asoc)

        val i = intent
        val fImagen = i.getStringExtra("imagen")

        val imgFile = File(fImagen)
        if (imgFile.exists()) {
            val myBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            imgVerImgAsoc.setImageBitmap(myBitmap)
        }
    }


}