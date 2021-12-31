package es.albainformatica.albamobileandroid.ventas

import android.widget.EditText
import android.text.TextWatcher
import android.text.Editable
import java.lang.NumberFormatException
import java.text.DecimalFormat

/**
 * Created by jabegines on 14/10/13.
 */
class NumberTextWatcher(et: EditText, DigEnteros: Int, DigDecimales: Int) : TextWatcher {
    private val df: DecimalFormat = DecimalFormat("#.#")
    private val dfnd: DecimalFormat
    private var hasFractionalPart: Boolean
    private val et: EditText

    // @SuppressWarnings("unused")
    // private static final String TAG = "NumberTextWatcher";

    override fun afterTextChanged(s: Editable) {
        et.removeTextChangedListener(this)
        try {
            val inilen: Int = et.text.length

            // Damos formato al edittext. Quitamos los separadores de miles.
            val v = s.toString().replace(df.decimalFormatSymbols.groupingSeparator.toString(), "")

            // Number n = df.parse(v);
            val n = v.toDouble()
            val cp = et.selectionStart
            if (hasFractionalPart) {
                et.setText(df.format(n))
            } else {
                et.setText(dfnd.format(n))
            }
            // Colocamos el cursor al final del edittext.
            val endlen: Int = et.text.length
            val sel = cp + (endlen - inilen)
            if (sel > 0 && sel <= et.text.length) {
                et.setSelection(sel)
            } else {
                et.setSelection(et.text.length - 1)
            }
        } catch (nfe: NumberFormatException) {

            // } catch (ParseException e) {
        }
        et.addTextChangedListener(this)
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        // if
        // (s.toString().contains(String.valueOf(df.getDecimalFormatSymbols().getDecimalSeparator())))
        // {

        // Esto es debido al bug que Google aún no ha solucionado, ya que siempre
        // toma como DecimalSeparator el punto, independientemente de la
        // configuración regional que tengas en el dispositivo.

        // getDecimalSeparator() me devuelve la coma (','), que es el separador
        // decimal español, pero el teclado numérico de android sólo deja introducir
        // el punto, así que, por ahora, compruebo si ha sido pulsado cualquiera de
        // los dos caracteres y lo doy por válido como separador decimal.
        hasFractionalPart = s.toString().contains(".") || s.toString().contains(",")
    }

    init {
        df.isDecimalSeparatorAlwaysShown = true
        df.maximumIntegerDigits = DigEnteros
        df.maximumFractionDigits = DigDecimales
        dfnd = DecimalFormat("#")
        dfnd.maximumIntegerDigits = DigEnteros
        this.et = et
        hasFractionalPart = false
    }
}