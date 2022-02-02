package es.albainformatica.albamobileandroid.ventas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.DialogFragment
import es.albainformatica.albamobileandroid.R
import kotlinx.android.synthetic.main.fragment_selecc_doc.*
import kotlinx.android.synthetic.main.fragment_selecc_doc.view.*


class SeleccDocFragment: DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        val rootView: View = inflater.inflate(R.layout.fragment_selecc_doc, container, false)

        rootView.cancelarButton.setOnClickListener {
            dismiss()
        }

        rootView.aceptarButton.setOnClickListener {
            val selectedId = seleccDocRadioGroup.checkedRadioButtonId
            val radio = rootView.findViewById<RadioButton>(selectedId)

            (activity as VentasLineas).cambiarTipoDoc(radio.tag.toString().toInt())
            dismiss()
        }

        return rootView
    }


}