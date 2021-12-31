package es.albainformatica.albamobileandroid.actividades

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.widget.ArrayAdapter
import android.os.Bundle
import android.widget.TextView
import android.widget.AdapterView
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import es.albainformatica.albamobileandroid.R

/**
 * Created by jabegines on 10/10/13.
 */
class BuscarBluetooth : Activity() {
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var mPairedDevicesArrayAdapter: ArrayAdapter<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.buscar_bluetooth)

        val tvTitulo = findViewById<TextView>(R.id.tvNombreActivity)
        tvTitulo.setText(R.string.asignar_impr)
        buscar()
    }

    override fun onDestroy() {
        super.onDestroy()
        mBluetoothAdapter.cancelDiscovery()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_buscarblueth, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.mni_buscarblueth -> {
                buscar()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun buscar() {
        mPairedDevicesArrayAdapter = ArrayAdapter(this, R.layout.device_name)
        val mPairedListView = findViewById<View>(R.id.disp_vinculados) as ListView
        mPairedListView.adapter = mPairedDevicesArrayAdapter
        mPairedListView.onItemClickListener = mDeviceClickListener
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val mPairedDevices = mBluetoothAdapter.bondedDevices
        if (mPairedDevices.size > 0) {
            for (mDevice in mPairedDevices) {
                mPairedDevicesArrayAdapter.add("""${mDevice.name}${mDevice.address}""".trimIndent())
            }
        } else {
            mPairedDevicesArrayAdapter.add("No se encontraron dispositivos vinculados")
        }
    }

    private val mDeviceClickListener =
        AdapterView.OnItemClickListener { _, mView, _, _ ->
            mBluetoothAdapter.cancelDiscovery()
            val mDeviceInfo = (mView as TextView).text.toString()
            val mDeviceAddress = mDeviceInfo.substring(mDeviceInfo.length - 17)

            // Guardamos la dirección del dispositivo en las preferencias.
            val prefs = PreferenceManager.getDefaultSharedPreferences(this@BuscarBluetooth)
            val editor = prefs.edit()
            editor.putString("impresoraBT", mDeviceAddress)
            editor.commit()
            finish()
        }
}