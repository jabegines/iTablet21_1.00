package es.albainformatica.albamobileandroid.actividades

import android.app.Activity
import android.widget.CheckBox
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import es.albainformatica.albamobileandroid.R

/**
 * Created by jabegines on 30/10/13.
 */
class ConfMultisistema: Activity() {
    private lateinit var chkUsarMult: CheckBox
    private lateinit var chkBD0: CheckBox
    private lateinit var chkBD1: CheckBox
    private lateinit var chkBD2: CheckBox
    private lateinit var chkBD3: CheckBox
    private lateinit var chkBD4: CheckBox
    private lateinit var chkBD5: CheckBox


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.conf_multisistema)
        inicializarControles()
    }

    fun cancelar(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        finish()
    }

    fun aceptar(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        pref.edit().putBoolean("usar_multisistema", chkUsarMult.isChecked).apply()
        if (chkUsarMult.isChecked) {
            pref.edit().putBoolean("usarBD00", chkBD0.isChecked).apply()
            pref.edit().putBoolean("usarBD10", chkBD1.isChecked).apply()
            pref.edit().putBoolean("usarBD20", chkBD2.isChecked).apply()
            pref.edit().putBoolean("usarBD30", chkBD3.isChecked).apply()
            pref.edit().putBoolean("usarBD40", chkBD4.isChecked).apply()
            pref.edit().putBoolean("usarBD50", chkBD5.isChecked).apply()
        } else {
            pref.edit().putBoolean("usarBD00", false).apply()
            pref.edit().putBoolean("usarBD10", false).apply()
            pref.edit().putBoolean("usarBD20", false).apply()
            pref.edit().putBoolean("usarBD30", false).apply()
            pref.edit().putBoolean("usarBD40", false).apply()
            pref.edit().putBoolean("usarBD50", false).apply()
        }
        finish()
    }

    private fun inicializarControles() {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        chkUsarMult = findViewById<View>(R.id.chkUsarMult) as CheckBox
        chkBD0 = findViewById<View>(R.id.checkBox) as CheckBox
        chkBD1 = findViewById<View>(R.id.checkBox1) as CheckBox
        chkBD2 = findViewById<View>(R.id.checkBox2) as CheckBox
        chkBD3 = findViewById<View>(R.id.checkBox3) as CheckBox
        chkBD4 = findViewById<View>(R.id.checkBox4) as CheckBox
        chkBD5 = findViewById<View>(R.id.checkBox5) as CheckBox
        chkUsarMult.isChecked = pref.getBoolean("usar_multisistema", false)
        chkBD0.isChecked = pref.getBoolean("usarBD00", true)
        chkBD1.isChecked = pref.getBoolean("usarBD10", false)
        chkBD2.isChecked = pref.getBoolean("usarBD20", false)
        chkBD3.isChecked = pref.getBoolean("usarBD30", false)
        chkBD4.isChecked = pref.getBoolean("usarBD40", false)
        chkBD5.isChecked = pref.getBoolean("usarBD50", false)
        actNumSistemas(null)
    }


    fun actNumSistemas(view: View?) {
        view?.getTag(0)          // Para que no dé warning el compilador

        chkBD0.isEnabled = chkUsarMult.isChecked
        chkBD1.isEnabled = chkUsarMult.isChecked
        chkBD2.isEnabled = chkUsarMult.isChecked
        chkBD3.isEnabled = chkUsarMult.isChecked
        chkBD4.isEnabled = chkUsarMult.isChecked
        chkBD5.isEnabled = chkUsarMult.isChecked
    }
}