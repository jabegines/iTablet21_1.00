package es.albainformatica.albamobileandroid.oldcatalogo

import android.app.Dialog
import android.content.Context
import es.albainformatica.albamobileandroid.actividades.Dlg2Listener
import android.widget.EditText
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import es.albainformatica.albamobileandroid.R

class DialogoCajasUnid(context: Context, private var dlgEscucha: Dlg2Listener) : Dialog(context), View.OnClickListener {
    private lateinit var edtCajasUnd: EditText
    private lateinit var btnOkCajasUnd: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialogo_cajas_unid)

        edtCajasUnd = findViewById(R.id.edtDlgCajasUnd)
        btnOkCajasUnd = findViewById(R.id.btnOkDlgCjUnd)
        btnOkCajasUnd.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val texto = edtCajasUnd.text.toString()
        dlgEscucha.onOkClick(texto)
        dismiss()
    }
}