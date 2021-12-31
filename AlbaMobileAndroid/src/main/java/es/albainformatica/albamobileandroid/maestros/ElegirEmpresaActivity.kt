package es.albainformatica.albamobileandroid.maestros

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.albainformatica.albamobileandroid.DatosEmpresas
import es.albainformatica.albamobileandroid.R
import es.albainformatica.albamobileandroid.dao.EmpresasDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import kotlinx.android.synthetic.main.selecc_empresa.*


class ElegirEmpresaActivity: AppCompatActivity() {
    private lateinit var fRecycler: RecyclerView
    private lateinit var fAdapter: EmpresasRvAdapter


    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.selecc_empresa)

        inicializarControles()
        prepararRecycler()
    }


    private fun inicializarControles() {
        fRecycler = rvEmpresas

        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.empresas)
    }


    private fun prepararRecycler() {
        fAdapter  = EmpresasRvAdapter(getEmpresas(), this, object: EmpresasRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: DatosEmpresas) {

                val returnIntent = Intent()
                returnIntent.putExtra("codEmpresa", data.codigo)
                setResult(RESULT_OK, returnIntent)
                finish()
            }
        })

        fRecycler.layoutManager = GridLayoutManager(this, 2)
        fRecycler.adapter = fAdapter
    }


    private fun getEmpresas(): MutableList<DatosEmpresas> {
        val empresasDao: EmpresasDao? = MyDatabase.getInstance(this)?.empresasDao()

        return empresasDao?.getAllEmpresas() ?: emptyList<DatosEmpresas>().toMutableList()
    }


}