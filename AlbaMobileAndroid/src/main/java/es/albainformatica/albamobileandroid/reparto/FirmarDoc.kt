package es.albainformatica.albamobileandroid.reparto

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.content.Intent
import android.graphics.*
import android.preference.PreferenceManager
import android.graphics.drawable.BitmapDrawable
import android.view.View.OnTouchListener
import android.view.MotionEvent
import android.widget.TextView
import es.albainformatica.albamobileandroid.BaseDatos
import android.widget.Toast
import android.view.View
import android.widget.ImageView
import es.albainformatica.albamobileandroid.R
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Exception

/**
 * Created by jabegines on 11/06/2014.
 */
class FirmarDoc : Activity() {
    private lateinit var prefs: SharedPreferences
    private var usarMultisistema: Boolean = false
    private var fIdDoc = 0
    private var fOtroDoc = 0
    private lateinit var imgFirma: ImageView
    private var downx = 0f
    private var downy = 0f
    private var upx = 0f
    private var upy = 0f
    private lateinit var bitmap: Bitmap
    private lateinit var canvas: Canvas
    private lateinit var paint: Paint


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.firmar_doc)

        val intent = intent
        fIdDoc = intent.getIntExtra("id_doc", 0)
        fOtroDoc = intent.getIntExtra("otro_doc", 0)
        inicializarControles()
    }

    private fun inicializarControles() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        usarMultisistema = prefs.getBoolean("usar_multisistema", false)

        //bitmap = Bitmap.createBitmap(500, 250, Bitmap.Config.ARGB_8888);
        bitmap = Bitmap.createBitmap(650, 300, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)
        paint = Paint()
        paint.color = Color.BLACK
        paint.strokeWidth = 5f
        imgFirma = findViewById(R.id.imvLyClasific)
        imgFirma.setImageDrawable(BitmapDrawable(resources, bitmap))
        imgFirma.setOnTouchListener(OnTouchListener { v: View?, event: MotionEvent ->
            imgFirma.setImageBitmap(bitmap)
            val action = event.action
            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    downx = event.x
                    downy = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    upx = event.x
                    upy = event.y
                    canvas.drawLine(downx, downy, upx, upy, paint)
                    imgFirma.invalidate()
                    downx = upx
                    downy = upy
                }
                MotionEvent.ACTION_UP -> {
                    upx = event.x
                    upy = event.y
                    canvas.drawLine(downx, downy, upx, upy, paint)
                    imgFirma.invalidate()
                }
                MotionEvent.ACTION_CANCEL -> {}
                else -> {}
            }
            true
        })
        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.btn_firmar)
    }

    fun firmaABitmap(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        imgFirma.buildDrawingCache()
        val bm = imgFirma.drawingCache
        var fOut: OutputStream
        var rutaLocal = prefs.getString("rutacomunicacion", "") ?: ""
        rutaLocal = if (rutaLocal == "") {
            if (usarMultisistema) "/storage/sdcard0/alba/firmas/" + BaseDatos.queBaseDatos else "/storage/sdcard0/alba/firmas/"
        } else {
            if (usarMultisistema) rutaLocal + "/firmas/" + BaseDatos.queBaseDatos else "$rutaLocal/firmas/"
        }
        try {
            val rutaFichFirma = File(rutaLocal)
            if (!rutaFichFirma.exists()) rutaFichFirma.mkdirs()
            var sdImageMainDirectory = File(rutaFichFirma, "$fIdDoc.jpg")
            fOut = FileOutputStream(sdImageMainDirectory)
            try {
                bm.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
                fOut.flush()
                fOut.close()

                // Vemos si queremos copiar la firma en otro fichero.
                if (fOtroDoc > 0) {
                    sdImageMainDirectory = File(rutaFichFirma, "$fOtroDoc.jpg")
                    fOut = FileOutputStream(sdImageMainDirectory)
                    try {
                        bm.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
                        fOut.flush()
                        fOut.close()
                    } catch (ignored: Exception) {
                    }
                }
            } catch (ignored: Exception) {
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ha ocurrido un error, intentelo de nuevo", Toast.LENGTH_SHORT)
                .show()
        }
        val returnIntent = Intent()
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    fun limpiarFirma(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        imgFirma.invalidate()
    }

    fun cancelarFirma(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val returnIntent = Intent()
        setResult(RESULT_CANCELED, returnIntent)
        finish()
    }
}