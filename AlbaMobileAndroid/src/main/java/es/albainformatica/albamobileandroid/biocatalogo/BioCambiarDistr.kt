package es.albainformatica.albamobileandroid.biocatalogo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import es.albainformatica.albamobileandroid.R

class BioCambiarDistr: AppCompatActivity() {

    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.bio_cambiar_distr)
    }


    fun cambiarDistr(view: View) {
        val returnIntent = Intent()

        when (view.tag as String) {
            "1" -> returnIntent.putExtra("distribucion", 1)
            "2" -> returnIntent.putExtra("distribucion", 2)
            "3" -> returnIntent.putExtra("distribucion", 3)
        }
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }


    // Manejo de los eventos del teclado en la actividad.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            val returnIntent = Intent()
            setResult(Activity.RESULT_CANCELED, returnIntent)
            finish()
            return true
        }
        // Para las demás cosas, se reenvía el evento al listener habitual.
        return super.onKeyDown(keyCode, event)
    }


}