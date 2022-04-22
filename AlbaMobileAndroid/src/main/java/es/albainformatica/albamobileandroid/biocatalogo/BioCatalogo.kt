package es.albainformatica.albamobileandroid.biocatalogo


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationView
import es.albainformatica.albamobileandroid.*
import es.albainformatica.albamobileandroid.dao.CatalogoLineasDao
import es.albainformatica.albamobileandroid.dao.FtosLineasDao
import es.albainformatica.albamobileandroid.database.MyDatabase
import es.albainformatica.albamobileandroid.database.MyDatabase.Companion.queBDRoom
import es.albainformatica.albamobileandroid.entity.CatalogoLineasEnt
import es.albainformatica.albamobileandroid.entity.FtosLineasEnt
import es.albainformatica.albamobileandroid.impresion_informes.GrafHcoClte
import es.albainformatica.albamobileandroid.impresion_informes.GrafVtasArt
import es.albainformatica.albamobileandroid.impresion_informes.GrafVtasClte
import es.albainformatica.albamobileandroid.maestros.ArticulosClase
import es.albainformatica.albamobileandroid.maestros.Clasificadores
import es.albainformatica.albamobileandroid.maestros.Departamentos
import es.albainformatica.albamobileandroid.maestros.Grupos
import es.albainformatica.albamobileandroid.ventas.Documento
import es.albainformatica.albamobileandroid.ventas.VentasDatosLinea
import kotlinx.android.synthetic.main.bio_catalogo.*
import kotlinx.android.synthetic.main.bio_fragment_page.*
import kotlinx.android.synthetic.main.nav_header_biocat.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.yesButton
import java.io.File
import java.util.*


class BioCatalogo: AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var catLineasDao: CatalogoLineasDao? = MyDatabase.getInstance(this)?.catalogoLineasDao()
    private var ftosLineasDao: FtosLineasDao? = MyDatabase.getInstance(this)?.ftosLineasDao()

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: ViewPagerAdapter
    private lateinit var aImages: ArrayList<Int>

    private lateinit var fRecyclerLeftD: RecyclerView
    private lateinit var fAdpGrupos: GruposRvAdapter
    private lateinit var fAdpDepart: DepartRvAdapter
    private lateinit var fAdpCatalogos: CatalogosRvAdapter


    private lateinit var fArticulos: ArticulosClase
    private lateinit var fDocumento: Documento
    private lateinit var fConfiguracion: Configuracion
    private lateinit var prefs: SharedPreferences
    private lateinit var fCatalogos: Clasificadores
    private lateinit var fGrupos: Grupos
    private lateinit var fDepartamentos: Departamentos
    private var carpetaDocAsoc: String = ""

    private var fIdCatalogo: Int = 0
    private var fIdGrupo: Short = 0
    private var fIdDepartamento: Short = 0
    private var fPosicion: Int = 0
    private var fPagina: Int = 0
    private var fItemActual: Int = 0
    private var viewSeleccionada: View? = null

    private lateinit var edtCantidad: EditText
    private lateinit var edtCajas: EditText
    private lateinit var dwLayout: DrawerLayout
    private lateinit var txtDescrCat: TextView
    private var fDecPrBase = 0
    private var fDecPrII = 0
    private var fIvaIncluido = false
    private var fAplicarIva = true
    private var fUsarFormatos = false
    private var fPedirDetalles = false
    private var fFtoPrecioBase = ""
    private var fFtoPrecioII = ""
    private var fFtoImpteBase = ""
    private var fFtoImpteII = ""
    private var fRutaImagenes = ""
    private var fBuscando: Boolean = false
    private val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 60)
    private var fImagPorPagina = 1
    private var fViendoDep: Boolean = false
    private var fModoVtaCat: Int = 1
    private var fOrdenacion: Int = 1

    private var fEmpresaActual: Short = 0

    private val fRequestVtaFormatos = 1
    private val fRequestCambDistr = 2
    private val fRequestVtaDetalles = 3
    private val fRequestModoLista = 4



    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.bio_catalogo)

        // Creamos el objeto fArticulos y lo pasamos al comunicador para que PageFragment pueda hacer uso de él
        fArticulos = ArticulosClase(this)
        fCatalogos = Clasificadores(this)
        fGrupos = Grupos(this)
        fDepartamentos = Departamentos(this)
        Comunicador.fArticulos = fArticulos
        fDocumento = Comunicador.fDocumento
        fConfiguracion = Comunicador.fConfiguracion
        fUsarFormatos = fConfiguracion.usarFormatos()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        fEmpresaActual = prefs.getInt("ultima_empresa", 0).toShort()
        fPedirDetalles = prefs.getBoolean("ventas_detall_cat_vis", false)
        fImagPorPagina = (prefs.getString("num_imag_cat", "1") ?: "1").toInt()
        // Establecemos la ruta de imágenes
        val usarMultisistema = prefs.getBoolean("usar_multisistema", false)
        val localDirectory = prefs.getString("rutacomunicacion", "") ?: ""
        fRutaImagenes = if (usarMultisistema) "$localDirectory/imagenes/$queBDRoom"
        else "$localDirectory/imagenes"

        fModoVtaCat = prefs.getInt("modo_vta_cat", 1)

        // Inicializamos el panel lateral y lo abrimos para que el usuario escoja un catálogo
        inicializarNavViewCatalogos()
        inicializarNavEstadisticas()

        // Llenamos el array con los ids de las imágenes y también el array de datos
        setImagesData()

        viewPager = findViewById(R.id.view_pager)
        inicAdaptadorViewPager()

        inicializarControles()
    }

    override fun onDestroy() {
        prefs.edit().putInt("modo_vta_cat", fModoVtaCat).apply()
        super.onDestroy()
    }


    private fun inicAdaptadorViewPager() {
        // Inicializamos el adaptador del ViewPager y lo asociamos a éste, pasándole el arraylist con los ids de las imágenes
        adapter = ViewPagerAdapter(fRutaImagenes, fImagPorPagina, fEmpresaActual)
        adapter.setItem(aImages)
        viewPager.adapter = adapter
    }



    @SuppressLint("SetTextI18n")
    private fun inicializarControles() {
        // Al tener un editText en el layout, android activa el teclado en cuanto lo muestra
        ocultarTeclado(this)

        carpetaDocAsoc = dimeRutaDocAsoc(this)

        fIvaIncluido = fConfiguracion.ivaIncluido(fEmpresaActual)
        fAplicarIva = fDocumento.fClientes.fAplIva
        fDecPrBase = fConfiguracion.decimalesPrecioBase()
        fDecPrII = fConfiguracion.decimalesPrecioIva()
        fFtoPrecioBase = fConfiguracion.formatoDecPrecioBase()
        fFtoPrecioII = fConfiguracion.formatoDecPrecioIva()
        fFtoImpteBase = fConfiguracion.formatoDecImptesBase()
        fFtoImpteII = fConfiguracion.formatoDecImptesIva()

        txtDescrCat = findViewById(R.id.tvBioDescrCat)
        when (fModoVtaCat) {
            1 -> if (fCatalogos.lClasCat.count() > 0)
                txtDescrCat.text = fCatalogos.lClasCat[0].descripcion
            2 -> if (fViendoDep) {
                if (fDepartamentos.lDepCat.count() > 0)
                    txtDescrCat.text = fDepartamentos.lDepCat[0].descripcion
            }
            3 -> txtDescrCat.text = resources.getString(R.string.btn_hco)
        }
        val queTexto = (fPosicion+1).toString() + "/" + aImages.size.toString()
        bioTextSeekBar.text = queTexto

        edtCantidad = findViewById(R.id.bioEdtCant)
        edtCajas = findViewById(R.id.bioEdtCj)
        if (!fConfiguracion.pedirCajas())
            bioLyCajas.visibility = View.GONE

        if (fImagPorPagina == 1) {
            if (aImages.isNotEmpty()) {
                if (!fUsarFormatos) {
                    val aDatosArt = fDocumento.datosArtEnCatLineas(aImages[fPosicion])

                    if (aDatosArt[0] != "F") {
                        edtCantidad.setText(String.format("%.0f", aDatosArt[2].toDouble()))
                        verImpte()

                    } else edtCantidad.setText("0")
                }
                mostrarDatosAdicionales()
            }
        }

        // Si vamos a usar formatos no presentaremos los botones con el más y el menos para las cantidades y las cajas
        if (fUsarFormatos) {
            bioImgRestar.visibility = View.INVISIBLE
            bioImgSumar.visibility = View.INVISIBLE
            bioEdtCant.visibility = View.INVISIBLE
            bioImgRestarCj.visibility = View.INVISIBLE
            bioImgSumarCj.visibility = View.INVISIBLE
            bioEdtCj.visibility = View.INVISIBLE
        }

        inicializarViewPager()
    }



    private fun inicializarViewPager() {
        // Establecemos estas propiedades para que no se vean restos de imágenes de las páginas anterior o siguiente
        viewPager.clipToPadding = false
        viewPager.setPadding(0, 0, 0, 0)
        // Inicializamos artSeleccCat para que no se quede ninguna imagen en transparente
        fArtSeleccCat = 0

        // Establecemos el evento onPageChangeListener al ViewPager para poder actualizar el seekBar y la cantidad
        // cada vez que cambiemos de página.
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                fPagina = position
                // A través de la página obtenemos la posición. En este caso será el primer elemento de la página, porque
                // acabamos de cambiar de página.
                fPosicion = (fPagina * fImagPorPagina)

                val queTexto = (fPosicion + 1).toString() + "/" + aImages.size.toString()
                bioTextSeekBar.text = queTexto

                if (fImagPorPagina == 1) {
                    viewSeleccionada = image1
                    if (!fUsarFormatos) {
                        val aDatosArt = fDocumento.datosArtEnCatLineas(aImages[fPosicion])
                        if (aDatosArt[0] != "F") {
                            edtCantidad.setText(String.format("%.0f", aDatosArt[2].toDouble()))
                            edtCajas.setText(String.format("%.0f", aDatosArt[1].toDouble()))
                        }
                        else {
                            edtCantidad.setText("0")
                            edtCajas.setText("0")
                        }

                        verImpte()
                    }
                } else {
                    // Al cambiar de página deseleccionamos la imagen para quitar la transparencia en la nueva página
                    fArtSeleccCat = 0
                    viewSeleccionada = null
                }

                // Preparamos los datos adicionales del artículo
                mostrarDatosAdicionales()
            }
        })
    }

    private fun mostrarDatosAdicionales() {
        // Limpiamos el layout de datos adicionales
        lyBioScroll.removeAllViews()

        if (fArticulos.datosAdicionales(aImages[fPosicion])) {

            for (datAdic in fArticulos.lDatAdic) {
                val tipo = dimeMiTipoDeArchivo(datAdic)

                // El documento es una imagen.
                when {
                    tipo.equals("image", ignoreCase = true) -> imagenAGaleria(carpetaDocAsoc + datAdic, datAdic)

                    // El documento es un pdf.
                    tipo.equals("pdf", ignoreCase = true) -> pdfAGaleria(datAdic)

                    // El documento es un archivo word
                    tipo.equals("word", ignoreCase = true) -> wordAGaleria(datAdic)

                    // El documento es un video.
                    //} else if (tipo.substring(0, 5).equals("video", ignoreCase = true)) {
                    //videoAGaleria()
                }
            }
        }
    }


    private fun imagenAGaleria(queImagen: String, queNombre: String) {
        val ly = LinearLayout(this)
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        ly.orientation = LinearLayout.VERTICAL
        ly.layoutParams = lp
        ly.background = ContextCompat.getDrawable(this, R.drawable.mi_borde)
        ly.gravity = Gravity.CENTER

        val iv = ImageView(this)
        iv.scaleType = ImageView.ScaleType.FIT_XY
        iv.setImageBitmap(decodeBitmapDesdeFichero(queImagen, 70, 70))

        val dw = iv.drawable
        val bt = Button(this)
        bt.tag = queImagen
        bt.text = queNombre
        bt.textSize = 10f
        bt.background = ColorDrawable(Color.parseColor("#FFFFFF"))
        bt.setCompoundDrawablesWithIntrinsicBounds(null, dw, null, null)
        bt.setOnClickListener { v ->
            mostrarImagenArt(v.tag as String)
        }

        ly.addView(bt)
        lyBioScroll.addView(ly)
    }

    private fun pdfAGaleria(queNombre: String) {
        val ly = LinearLayout(this)
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        ly.layoutParams = lp
        ly.background = ContextCompat.getDrawable(this, R.drawable.mi_borde)
        ly.gravity = Gravity.CENTER

        val bt = Button(this)
        bt.text = queNombre
        bt.textSize = 10f
        bt.background = ColorDrawable(Color.parseColor("#FFFFFF"))
        bt.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(this, R.drawable.logopdf), null, null)
        bt.setOnClickListener { v ->
            val queb = v as Button
            val quet = queb.text as String
            abrirPdf(quet)
        }

        ly.addView(bt)
        lyBioScroll.addView(ly)
    }

    private fun wordAGaleria(queNombre: String) {
        val ly = LinearLayout(this)
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        ly.layoutParams = lp
        ly.background = ContextCompat.getDrawable(this, R.drawable.mi_borde)
        ly.gravity = Gravity.CENTER

        val bt = Button(this)
        bt.text = queNombre
        bt.textSize = 10f
        bt.background = ColorDrawable(Color.parseColor("#FFFFFF"))
        bt.setCompoundDrawablesWithIntrinsicBounds(null, ContextCompat.getDrawable(this, R.drawable.logoword), null, null)
        bt.setOnClickListener { v ->
            val queb = v as Button
            val quet = queb.text as String
            abrirWord(quet)
        }

        ly.addView(bt)
        lyBioScroll.addView(ly)
    }


    private fun mostrarImagenArt(queImagen: String) {
        val i = Intent(this, VerImagenAsoc::class.java)
        i.putExtra("imagen", queImagen)
        startActivity(i)
    }

    private fun abrirPdf(quePdf: String) {
        try {
            val file = File(carpetaDocAsoc, quePdf)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val fileUri = FileProvider.getUriForFile(this, this.packageName + ".provider", file)
                val intent = Intent(Intent.ACTION_VIEW, fileUri)
                intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                intent.setDataAndType(fileUri, "application/pdf")
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(intent)

            } else {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(Uri.fromFile(file), "application/pdf")
                startActivity(intent)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun abrirWord(queWord: String) {
        try {
            val file = File(carpetaDocAsoc, queWord)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val fileUri = FileProvider.getUriForFile(this, this.packageName + ".provider", file)
                val intent = Intent(Intent.ACTION_VIEW, fileUri)
                intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                intent.setDataAndType(fileUri, "application/msword")
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(intent)

            } else {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(Uri.fromFile(file), "application/msword")
                startActivity(intent)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun inicializarNavViewCatalogos() {
        dwLayout = findViewById(R.id.dwlBioDrawer)

        fRecyclerLeftD = rvLeftDrawer
        fRecyclerLeftD.layoutManager = LinearLayoutManager(this)

        // Dejamos abiertos tanto los catálogos como los grupos, así podremos conmutar entre ellos sin tener
        // que estar abriendo y cerrando constantemente
        fCatalogos.abrirBioCatalogo()
        fGrupos.abrirParaCatalogo()

        if (fModoVtaCat < 3) {
            if (fModoVtaCat == 1) {
                if (fCatalogos.lClasCat.count() > 0)
                    prepararRvCatalogos()
                else {
                    if (fGrupos.lGrupCat.count() > 0) {
                        prepararRvGrupos()
                    } else {
                        alert("No tiene ningún catálogo ni tampoco ningún grupo.\nNo podrá vender en el modo Catálogo Visual.") {
                            title = "Información"
                            yesButton { finish() }
                        }.show()
                    }
                }
            } else {
                if (fGrupos.lGrupCat.count() > 0)
                    prepararRvGrupos()
                else {
                    if (fCatalogos.lClasCat.count() > 0) {
                        prepararRvCatalogos()
                    } else {
                        alert("No tiene ningún catálogo ni tampco ningún grupo.\nNo podrá vender en el modo Catálogo Visual.") {
                            title = "Información"
                            yesButton { finish() }
                        }.show()
                    }
                }
            }

            abrirNavViewPrimVez()
        }
    }

    private fun prepararRvCatalogos() {
        if (fCatalogos.lClasCat.count() > 0) {
            fModoVtaCat = 1
            nhTvTipoCatalogo.text = resources.getString(R.string.cat_catalogos)

            // Por ahora abrimos el primer catálogo de la tabla de catálogos
            fIdCatalogo = fCatalogos.lClasCat[0].clasificadorId

            fAdpCatalogos = CatalogosRvAdapter(getCatalogos(), this, object : CatalogosRvAdapter.OnItemClickListener {
                    override fun onClick(view: View, data: ClasifParaCat) {
                        // Primero guardamos en el documento para mantener las cantidades pedidas
                        if (fUsarFormatos) {
                            bioCat2DocFtos()
                        } else {
                            bioCat2Doc(false)
                        }
                        // Tomamos el campo _id de la fila en la que hemos pulsado y cerramos el DrawerLayout
                        fIdCatalogo = data.clasificadorId
                        dwLayout.closeDrawer(GravityCompat.START)
                        // Volvemos a cargar los arrays
                        setImagesData()

                        // Reiniciamos el adaptador y demás controles
                        reiniciarAdaptador(false, data.descripcion)
                        if (fImagPorPagina == 1)
                            verImpte()
                    }
                })

            fRecyclerLeftD.adapter = fAdpCatalogos
        }
        else {
            prepararRvGrupos()
        }
    }


    private fun getCatalogos(): List<ClasifParaCat> {
        return fCatalogos.lClasCat
    }


    private fun prepararRvGrupos() {
        if (fGrupos.lGrupCat.count() > 0) {
            fModoVtaCat = 2
            nhTvTipoCatalogo.text = resources.getString(R.string.grupos)

            fAdpGrupos = GruposRvAdapter(getGrupos(), this, object : GruposRvAdapter.OnItemClickListener {
                    override fun onClick(view: View, data: GruposParaCat) {
                        fIdGrupo = data.codigo
                        // Cargamos los departamentos del grupo
                        prepararRvDepartamentos(data.descripcion)
                    }
                })

            fRecyclerLeftD.adapter = fAdpGrupos

        } else {
            alert("No tiene ningún grupo") {
                title = "Información"
                yesButton {}
            }.show()
        }
    }


    private fun getGrupos(): List<GruposParaCat> {
        return fGrupos.lGrupCat
    }


    private fun prepararRvDepartamentos(fDescrGrupo: String) {
        fViendoDep = true
        nhTvTipoCatalogo.text = fDescrGrupo

        fAdpDepart = DepartRvAdapter(getDepartam(), this, object: DepartRvAdapter.OnItemClickListener {
            override fun onClick(view: View, data: DepartParaCat) {
                // Primero guardamos en el documento para mantener las cantidades pedidas
                if (fUsarFormatos) {
                    bioCat2DocFtos()
                } else {
                    bioCat2Doc(false)
                }
                // Tomamos el campo _id de la fila en la que hemos pulsado y cerramos el DrawerLayout
                fIdDepartamento = data.departamentoId
                dwLayout.closeDrawer(GravityCompat.START)
                // Volvemos a cargar los arrays
                setImagesData()

                // Reiniciamos el adaptador y demás controles
                reiniciarAdaptador(false, data.descripcion)
                if (fImagPorPagina == 1)
                    verImpte()
            }
        })

        fRecyclerLeftD.adapter = fAdpDepart
    }

    private fun getDepartam(): List<DepartParaCat> {
        fDepartamentos.abrirParaCatalogo(fIdGrupo)
        return fDepartamentos.lDepCat
    }




    fun cambiarTipoCatalogo(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fViendoDep) {
            fViendoDep = false
            prepararRvGrupos()
        } else {
            fModoVtaCat = if (fModoVtaCat == 1) 2
            else 1

            if (fModoVtaCat == 1) prepararRvCatalogos()
            else prepararRvGrupos()
        }
    }

    fun tipoCatalogoHco(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (fArticulos.abrirBioHistorico(fDocumento.fCliente, fOrdenacion)) {
            fViendoDep = false
            fModoVtaCat = 3

            // Primero guardamos en el documento para mantener las cantidades pedidas
            if (fUsarFormatos) {
                bioCat2DocFtos()
            } else {
                bioCat2Doc(false)
            }
            // Cerramos el DrawerLayout
            dwLayout.closeDrawer(GravityCompat.START)
            // Volvemos a cargar los arrays
            setImagesData()

            // Reiniciamos el adaptador y demás controles
            reiniciarAdaptador(false, "")
            if (fImagPorPagina == 1)
                verImpte()

        } else {
            MsjAlerta(this).alerta(resources.getString(R.string.msj_SinDatos))
        }
    }

    fun venderModoLista(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val i = Intent(this, VentasDatosLinea::class.java)
        i.putExtra("estado", est_Vl_Nueva)
        i.putExtra("primera_vez", true)
        startActivityForResult(i, fRequestModoLista)
    }


    fun cambiarDistribucion(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        val i = Intent(this, BioCambiarDistr::class.java)
        startActivityForResult(i, fRequestCambDistr)
    }


    private fun inicializarNavEstadisticas() {
        val navigationView: NavigationView = findViewById(R.id.nav_view_graf)
        navigationView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.mni_bioImgAcumArt -> {
                if (aImages.isNotEmpty()) {
                    fArticulos.existeArticulo(aImages[fPosicion])
                    val i = Intent(this, GrafVtasArt::class.java)
                    i.putExtra("articulo", aImages[fPosicion])
                    i.putExtra("descripcion", fArticulos.fDescripcion)
                    i.putExtra("cliente", fDocumento.fCliente)
                    if (fConfiguracion.aconsNomComercial()) i.putExtra("nombre", fDocumento.fClientes.fNomComercial)
                    else i.putExtra("nombre", fDocumento.fClientes.fNombre)
                    startActivity(i)
                }
                return true
            }

            R.id.mni_bioVtasClte -> {
                val i = Intent(this, GrafVtasClte::class.java)
                i.putExtra("cliente", fDocumento.fCliente)
                if (fConfiguracion.aconsNomComercial()) i.putExtra("nombre", fDocumento.fClientes.fNomComercial)
                else i.putExtra("nombre", fDocumento.fClientes.fNombre)
                startActivity(i)

                return true
            }

            R.id.mni_bioHcoClte -> {
                val i = Intent(this, GrafHcoClte::class.java)
                i.putExtra("cliente", fDocumento.fCliente)
                startActivity(i)

                return true
            }

            else -> return true
        }
    }


    @SuppressLint("SetTextI18n")
    private fun reiniciarAdaptador(irAUltItem: Boolean, descrCat: String) {
        fPagina = 0
        fPosicion = (fPagina * fImagPorPagina)

        // Reiniciamos el adaptador
        inicAdaptadorViewPager()
        // Reiniciamos el TextView con el contador
        when (fModoVtaCat) {
            1 -> txtDescrCat.text = descrCat
            2 -> if (fViendoDep) txtDescrCat.text = descrCat
            3 -> txtDescrCat.text = resources.getString(R.string.btn_hco)
        }

        bioTextSeekBar.text = (fPosicion+1).toString() + "/" + aImages.size.toString()

        if (fImagPorPagina == 1) {
            if (!fUsarFormatos) {
                val aDatosArt = fDocumento.datosArtEnCatLineas(aImages[fPosicion])

                if (aDatosArt[0] != "F") edtCantidad.setText(String.format("%.0f", aDatosArt[2].toDouble()))
                else edtCantidad.setText("0")
            }
        }

        // Nos situamos en el artículo que estuviéramos viendo antes de abandonar el catálogo (p. ej. para buscar)
        if (irAUltItem)
            viewPager.currentItem = fItemActual
    }


    fun abrirNavView(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        if (!fBuscando) {
            if (fModoVtaCat == 3) prepararRvCatalogos()
            dwLayout.openDrawer(GravityCompat.START)
        }
        else
            alert("En modo búsqueda no puede abrir catálogos.\nTermine la búsqueda para poder abrirlos.") {
                title = "Información"
                yesButton {}
            }.show()
    }

    private fun abrirNavViewPrimVez() {
        dwLayout.openDrawer(GravityCompat.START)
    }

    fun abrirNavEstadisticas(view: View) {
        view.getTag(0)          // Para que no dé warning el compilador

        mostrarDatosAdicionales()
        dwLayout.openDrawer(GravityCompat.END)
    }


    private fun setImagesData(): Boolean {

        aImages = ArrayList()
        catLineasDao?.vaciar()

        if (fUsarFormatos)
            ftosLineasDao?.vaciar()

        var continuar = false
        when (fModoVtaCat) {
            1 -> continuar = fArticulos.abrirBioCatalogo(fIdCatalogo, fOrdenacion)
            2 -> continuar = fArticulos.abrirBioDepartamento(fIdGrupo, fIdDepartamento, fOrdenacion)
            3 -> continuar = fArticulos.abrirBioHistorico(fDocumento.fCliente, fOrdenacion)
        }


        if (continuar) {
            // Insertamos en aImages todos los artículos que pertenezcan al catálogo o departamento seleccionado
            for (articuloId in fArticulos.lArticulos) {
                aImages.add(articuloId)
            }

            // Añadiremos un elemento en la tabla temporal catalogoLineas por cada línea del documento. De esta forma
            // esta tabla no tendrá tantos elementos como aImages, sino solamente los que insertemos en
            // las lineas del documento. Si usamos formatos no usaremos ésta sino la tabla temporal ftosLineas.
            for (linea in fDocumento.lLineas) {
                if (fUsarFormatos) {
                    val ftosLineasEnt = FtosLineasEnt()

                    ftosLineasEnt.lineaId = linea.lineaId
                    ftosLineasEnt.articuloId = linea.articuloId
                    ftosLineasEnt.cajas = linea.cajas
                    ftosLineasEnt.piezas = linea.piezas
                    ftosLineasEnt.cantidad = linea.cantidad
                    ftosLineasEnt.precio = linea.precio
                    ftosLineasEnt.dto = linea.dto
                    ftosLineasEnt.textoLinea = linea.textoLinea
                    ftosLineasEnt.flag5 = linea.flag5
                    ftosLineasEnt.flag = linea.flag
                    ftosLineasEnt.borrar = "F"
                    ftosLineasEnt.formatoId = linea.formatoId

                    ftosLineasDao?.insertar(ftosLineasEnt)

                } else {
                    val catLineasEnt = CatalogoLineasEnt()
                    catLineasEnt.linea = linea.lineaId
                    catLineasEnt.articuloId = linea.articuloId
                    catLineasEnt.cajas = linea.cajas
                    catLineasEnt.piezas = linea.piezas
                    catLineasEnt.cantidad = linea.cantidad
                    catLineasEnt.precio = linea.precio
                    catLineasEnt.dto = linea.dto
                    catLineasEnt.textoLinea = linea.textoLinea
                    catLineasEnt.flag5 = linea.flag5
                    catLineasEnt.flag = linea.flag
                    catLineasEnt.esEnlace = linea.esEnlace
                    catLineasEnt.precioII = linea.precioII
                    catLineasEnt.importe = linea.importe
                    catLineasEnt.importeII = linea.importeII

                    catLineasDao?.insertar(catLineasEnt)
                }

            }
        }

        return continuar
    }



    private fun calcularImpte(queCantidad: String): Double {
        // Tenemos que comprobar si el artículo ya está incluido en el documento, para mantener el precio
        // que tenga la línea (puede que le hayamos modificado el precio después de salir del catálogo).
        // Si es así, tomaremos los datos que necesitamos de la línea del documento.
        val queLinea = fDocumento.existeLineaArticulo(aImages[fPosicion])
        var dImpte: Double

        if (queLinea > 0) {
            fDocumento.cargarLinea(queLinea)
            fDocumento.fCantidad = java.lang.Double.parseDouble(queCantidad)

            // Tenemos que estar situados en el artículo correspondiente
            fArticulos.existeArticulo(aImages[fPosicion])

        } else {
            if (fArticulos.existeArticulo(aImages[fPosicion])) {
                fDocumento.inicializarLinea()
                fDocumento.fArticulo = aImages[fPosicion]

                fDocumento.fCantidad = java.lang.Double.parseDouble(queCantidad)
                fDocumento.calculaPrecioYDto(fArticulos.fGrupo, fArticulos.fDepartamento, fArticulos.fCodProv, fArticulos.fPorcIva)
            }
        }

        if (fIvaIncluido) {
            dImpte = fDocumento.fPrecioII * fDocumento.fCantidad
            // Calculo el % de dto. y se lo resto al importe
            val dImpteDto = dImpte * fDocumento.fDtoLin / 100
            dImpte -= dImpteDto

        } else {
            dImpte = fDocumento.fPrecio * fDocumento.fCantidad
            // Calculo el % de dto. y se lo resto al importe
            val dImpteDto = dImpte * fDocumento.fDtoLin / 100
            dImpte -= dImpteDto
        }
        return dImpte
    }

    private fun verImpte() {
        // Tenemos que comprobar si el artículo ya está incluido en el documento, para mantener el precio
        // que tenga la línea (puede que le hayamos modificado el precio después de salir del catálogo).
        // Si es así, tomaremos los datos que necesitamos de la línea del documento.
        val queLinea = fDocumento.existeLineaArticulo(aImages[fPosicion])
        val aDatosArt = fDocumento.datosArtEnCatLineas(aImages[fPosicion])

        if (queLinea > 0) {
            fDocumento.cargarLinea(queLinea)
            if (aDatosArt[0] != "F") fDocumento.fCantidad = java.lang.Double.parseDouble(aDatosArt[2])
            else fDocumento.fCantidad = 0.0

            // Tenemos que estar situados en el artículo correspondiente
            fArticulos.existeArticulo(aImages[fPosicion])

        } else {

            if (fArticulos.existeArticulo(aImages[fPosicion])) {
                fDocumento.inicializarLinea()
                fDocumento.fArticulo = aImages[fPosicion]

                if (aDatosArt[0] != "F") fDocumento.fCantidad = java.lang.Double.parseDouble(aDatosArt[2])
                else fDocumento.fCantidad = 0.0
                fDocumento.calculaPrecioYDto(fArticulos.fGrupo, fArticulos.fDepartamento, fArticulos.fCodProv, fArticulos.fPorcIva)
            }
        }

        if (fIvaIncluido) {
            var dImpte = fDocumento.fPrecioII * fDocumento.fCantidad
            // Calculo el % de dto. y se lo resto al importe
            val dImpteDto = dImpte * fDocumento.fDtoLin / 100
            dImpte -= dImpteDto

        } else {
            var dImpte = fDocumento.fPrecio * fDocumento.fCantidad
            // Calculo el % de dto. y se lo resto al importe
            val dImpteDto = dImpte * fDocumento.fDtoLin / 100
            dImpte -= dImpteDto
        }
    }


    fun sumarCantidad(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        if (aImages.isNotEmpty()) {

            if ((viewSeleccionada != null) || (fImagPorPagina == 1)) {
                var iTag = 0

                if (fImagPorPagina != 1) {
                    val sTag = viewSeleccionada?.tag as String
                    iTag = sTag.toInt()
                }

                fPosicion = (fPagina * fImagPorPagina) + iTag
                val aDatosArt = fDocumento.datosArtEnCatLineas(aImages[fPosicion])
                var insertar = false
                val catLineaEnt = CatalogoLineasEnt()
                catLineaEnt.articuloId = aImages[fPosicion]

                // Si no existe ningún registro para el artículo en la tabla catalogoLineas, lo añadimos
                if (aDatosArt[0] == "F") {
                    insertar = true
                    aDatosArt[2] = "1"
                    catLineaEnt.cajas = "0"
                    catLineaEnt.piezas = "0"
                } else {
                    val iCantidad = aDatosArt[2].toInt() + 1
                    aDatosArt[2] = iCantidad.toString()
                }

                fDocumento.fArticulo = aImages[fPosicion]
                fDocumento.calculaPrecioYDto(fArticulos.fGrupo, fArticulos.fDepartamento, fArticulos.fCodProv, fArticulos.fPorcIva)

                catLineaEnt.cantidad = aDatosArt[2]
                catLineaEnt.precio = fDocumento.fPrecio.toString()
                catLineaEnt.precioII = fDocumento.fPrecioII.toString()
                catLineaEnt.importe = fDocumento.fImporte.toString()
                catLineaEnt.importeII = fDocumento.fImpteII.toString()
                catLineaEnt.dto = fDocumento.fDtoLin.toString()

                if (insertar) catLineasDao?.insertar(catLineaEnt)
                else catLineasDao?.actualizar(catLineaEnt.cajas, catLineaEnt.piezas, catLineaEnt.cantidad,
                                catLineaEnt.precio, catLineaEnt.precioII, catLineaEnt.importe, catLineaEnt.importeII,
                                catLineaEnt.dto, aImages[fPosicion])

                edtCantidad.setText(String.format("%.0f", aDatosArt[2].toDouble()))

                if (fImagPorPagina > 1)
                    viewPager.adapter?.notifyDataSetChanged()

            } else alert("No ha seleccionado ningún artículo", "Sumar cantidad") {
                positiveButton("Ok") { }
            }.show()
        }
    }


    fun sumarCajas(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        if (aImages.isNotEmpty()) {
            if ((viewSeleccionada != null) || (fImagPorPagina == 1)) {
                var iTag = 0

                if (fImagPorPagina != 1) {
                    val sTag = viewSeleccionada?.tag as String
                    iTag = sTag.toInt()
                }

                fPosicion = (fPagina * fImagPorPagina) + iTag
                val aDatosArt = fDocumento.datosArtEnCatLineas(aImages[fPosicion])
                var insertar = false
                val catLineaEnt = CatalogoLineasEnt()
                catLineaEnt.articuloId = aImages[fPosicion]

                // Si no existe ningún registro para el artículo en la tabla catalogoLineas, lo añadimos
                if (aDatosArt[0] == "F") {
                    insertar = true
                    aDatosArt[1] = "1"
                    catLineaEnt.piezas = "0"
                } else {
                    val iCajas = aDatosArt[1].toInt() + 1
                    aDatosArt[1] = iCajas.toString()
                }
                val queCantidad = calcularCantidad(aDatosArt[1])
                catLineaEnt.cantidad = queCantidad.toString()

                fDocumento.fArticulo = aImages[fPosicion]
                fDocumento.calculaPrecioYDto(fArticulos.fGrupo, fArticulos.fDepartamento, fArticulos.fCodProv, fArticulos.fPorcIva)

                catLineaEnt.cajas = aDatosArt[1]
                catLineaEnt.precio = fDocumento.fPrecio.toString()
                catLineaEnt.precioII = fDocumento.fPrecioII.toString()
                catLineaEnt.importe = fDocumento.fImporte.toString()
                catLineaEnt.importeII = fDocumento.fImpteII.toString()
                catLineaEnt.dto = fDocumento.fDtoLin.toString()

                if (insertar) catLineasDao?.insertar(catLineaEnt)
                else catLineasDao?.actualizar(catLineaEnt.cajas, catLineaEnt.piezas, catLineaEnt.cantidad,
                    catLineaEnt.precio, catLineaEnt.precioII, catLineaEnt.importe, catLineaEnt.importeII,
                    catLineaEnt.dto, aImages[fPosicion])

                edtCajas.setText(String.format("%.0f", aDatosArt[1].toDouble()))

            } else alert("No ha seleccionado ningún artículo", "Sumar caja") {
                positiveButton("Ok") { }
            }.show()
        }
    }


    private fun calcularCantidad(queCajas: String): Double {
        val numCajas = queCajas.toDouble()
        return fArticulos.fUCaja * numCajas
    }

    fun restarCantidad(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        if (aImages.isNotEmpty()) {
            if ((viewSeleccionada != null) || (fImagPorPagina == 1)) {
                var iTag = 0

                if (fImagPorPagina != 1) {
                    val sTag = viewSeleccionada?.tag as String
                    iTag = sTag.toInt()
                }

                fPosicion = (fPagina * fImagPorPagina) + iTag
                val aDatosArt = fDocumento.datosArtEnCatLineas(aImages[fPosicion])
                var insertar = false
                val catLineaEnt = CatalogoLineasEnt()
                catLineaEnt.articuloId = aImages[fPosicion]

                // Si no existe ningún registro para el artículo en la tabla catalogoLineas, lo añadimos
                if (aDatosArt[0] == "F") {
                    insertar = true
                    aDatosArt[2] = "-1"
                    catLineaEnt.cajas = "0"
                    catLineaEnt.piezas = "0"
                } else {
                    val iCantidad = aDatosArt[2].toInt() - 1
                    aDatosArt[2] = iCantidad.toString()
                }

                fDocumento.fArticulo = aImages[fPosicion]
                fDocumento.calculaPrecioYDto(fArticulos.fGrupo, fArticulos.fDepartamento, fArticulos.fCodProv, fArticulos.fPorcIva)

                catLineaEnt.cajas = aDatosArt[2]
                catLineaEnt.precio = fDocumento.fPrecio.toString()
                catLineaEnt.precioII = fDocumento.fPrecioII.toString()
                catLineaEnt.importe = fDocumento.fImporte.toString()
                catLineaEnt.importeII = fDocumento.fImpteII.toString()
                catLineaEnt.dto = fDocumento.fDtoLin.toString()

                // Comprobamos si podemos vender en negativo
                if (aDatosArt[2].toDouble() < 0 && fConfiguracion.noVenderNeg()) {
                    MsjAlerta(this).alerta(resources.getString(R.string.msj_NoVenderNeg))
                    edtCantidad.setText(String.format("%.0f", 0.0))
                }
                else {
                    if (insertar) catLineasDao?.insertar(catLineaEnt)
                    else catLineasDao?.actualizar(catLineaEnt.cajas, catLineaEnt.piezas, catLineaEnt.cantidad,
                        catLineaEnt.precio, catLineaEnt.precioII, catLineaEnt.importe, catLineaEnt.importeII,
                        catLineaEnt.dto, aImages[fPosicion])

                    edtCantidad.setText(String.format("%.0f", aDatosArt[2].toDouble()))
                }

                if (fImagPorPagina > 1)
                    viewPager.adapter?.notifyDataSetChanged()

            } else alert("No ha seleccionado ningún artículo", "Restar cantidad") {
                positiveButton("Ok") { }
            }.show()
        }
    }

    fun restarCajas(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        if (aImages.isNotEmpty()) {
            if ((viewSeleccionada != null) || (fImagPorPagina == 1)) {
                var iTag = 0

                if (fImagPorPagina != 1) {
                    val sTag = viewSeleccionada?.tag as String
                    iTag = sTag.toInt()
                }

                fPosicion = (fPagina * fImagPorPagina) + iTag
                val aDatosArt = fDocumento.datosArtEnCatLineas(aImages[fPosicion])
                var insertar = false
                val catLineaEnt = CatalogoLineasEnt()
                catLineaEnt.articuloId = aImages[fPosicion]

                // Si no existe ningún registro para el artículo en la tabla catalogoLineas, lo añadimos
                if (aDatosArt[0] == "F") {
                    insertar = true
                    aDatosArt[1] = "-1"
                    catLineaEnt.piezas = "0"
                } else {
                    val iCajas = aDatosArt[1].toInt() - 1
                    aDatosArt[1] = iCajas.toString()
                }
                val queCantidad = calcularCantidad(aDatosArt[1])
                catLineaEnt.cantidad = queCantidad.toString()

                fDocumento.fArticulo = aImages[fPosicion]
                fDocumento.calculaPrecioYDto(fArticulos.fGrupo, fArticulos.fDepartamento, fArticulos.fCodProv, fArticulos.fPorcIva)

                catLineaEnt.cajas = aDatosArt[1]
                catLineaEnt.precio = fDocumento.fPrecio.toString()
                catLineaEnt.precioII = fDocumento.fPrecioII.toString()
                catLineaEnt.importe = fDocumento.fImporte.toString()
                catLineaEnt.importeII = fDocumento.fImpteII.toString()
                catLineaEnt.dto = fDocumento.fDtoLin.toString()

                if (insertar) catLineasDao?.insertar(catLineaEnt)
                else catLineasDao?.actualizar(catLineaEnt.cajas, catLineaEnt.piezas, catLineaEnt.cantidad,
                    catLineaEnt.precio, catLineaEnt.precioII, catLineaEnt.importe, catLineaEnt.importeII,
                    catLineaEnt.dto, aImages[fPosicion])

                edtCajas.setText(String.format("%.0f", aDatosArt[1].toDouble()))

            } else alert("No ha seleccionado ningún artículo", "Restar caja") {
                positiveButton("Ok") { }
            }.show()
        }
    }


    fun seleccArticulo(view: View) {
        // Si volvemos a pulsar sobre un artículo seleccionado lo deseleccionamos
        if (viewSeleccionada == view && (fImagPorPagina > 1)) {
            viewSeleccionada = null
            fArtSeleccCat = 0
            viewPager.adapter?.notifyDataSetChanged()

        } else {
            viewSeleccionada = view
            val sTag = viewSeleccionada?.tag as String
            val iTag = sTag.toInt()
            fPosicion = (fPagina * fImagPorPagina) + iTag

            // Controlamos si hemos pulsado en una view sin datos
            if (aImages.count() > fPosicion) {
                val artNumVecesEnDoc = fDocumento.artNumVecesEnDoc(aImages[fPosicion])
                // Si usamos formatos no sumaremos la cantidad al pulsar en la imagen, sino que llamaremos a otra ventana
                if (fUsarFormatos) {
                    val i = Intent(this, VtaFormatosCat::class.java)
                    i.putExtra("articulo", aImages[fPosicion])
                    i.putExtra("rutaimagenes", fRutaImagenes)
                    i.putExtra("empresa", fEmpresaActual)
                    // Establecemos fItemActual para poder volver luego al artículo en el que estamos
                    fItemActual = viewPager.currentItem
                    startActivityForResult(i, fRequestVtaFormatos)

                    // Entraremos en la actividad VtaDetallesCat si así lo tenemos configurado o si en el documento
                    // tenemos más de una línea del artículo (por haber vendido en modo lista)
                } else if (fPedirDetalles || (artNumVecesEnDoc > 1)) {
                    val i = Intent(this, VtaDetallesCat::class.java)
                    i.putExtra("articulo", aImages[fPosicion])
                    i.putExtra("numVecesEnDoc", artNumVecesEnDoc)
                    i.putExtra("empresa", fEmpresaActual)
                    fItemActual = viewPager.currentItem
                    startActivityForResult(i, fRequestVtaDetalles)

                } else {
                    if (fImagPorPagina == 1) {
                        toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                        sumarCantidad(view)

                    } else {
                        fArtSeleccCat = aImages[fPosicion]
                        viewPager.adapter?.notifyDataSetChanged()

                        val aDatosArt = fDocumento.datosArtEnCatLineas(aImages[fPosicion])
                        if (aDatosArt[0] != "F") {
                            edtCajas.setText(String.format("%.0f", aDatosArt[1].toDouble()))
                            edtCantidad.setText(String.format("%.0f", aDatosArt[2].toDouble()))
                        } else {
                            edtCajas.setText("")
                            edtCantidad.setText("")
                        }
                    }
                }
            } else viewSeleccionada = null
        }
    }


    private fun tomarCantidad(queCantidad: String) {

        if (queCantidad != "") {
            val aDatosArt = fDocumento.datosArtEnCatLineas(aImages[fPosicion])
            var insertar = false
            val catLineaEnt = CatalogoLineasEnt()
            catLineaEnt.articuloId = aImages[fPosicion]
            catLineaEnt.cantidad = queCantidad

            // Si no existe ningún registro para el artículo en la tabla catalogoLineas, lo añadimos
            if (aDatosArt[0] == "F") {
                insertar = true
                catLineaEnt.cajas = "0"
                catLineaEnt.piezas = "0"
            }

            val dImpte = calcularImpte(queCantidad)
            if (fIvaIncluido) catLineaEnt.precio = fDocumento.fPrecioII.toString()
            else catLineaEnt.precio = fDocumento.fPrecio.toString()
            catLineaEnt.importe = dImpte.toString()
            catLineaEnt.dto = fDocumento.fDtoLin.toString()

            if (insertar) catLineasDao?.insertar(catLineaEnt)
            else catLineasDao?.actualizar(catLineaEnt.cajas, catLineaEnt.piezas, catLineaEnt.cantidad,
                catLineaEnt.precio, catLineaEnt.precioII, catLineaEnt.importe, catLineaEnt.importeII,
                catLineaEnt.dto, aImages[fPosicion])

            viewPager.adapter?.notifyDataSetChanged()
            edtCantidad.setText(String.format("%.0f", queCantidad.toDouble()))
        }
    }



    @SuppressLint("InflateParams")
    fun bioBuscar(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        if (!fBuscando) {
            val builder = AlertDialog.Builder(this)
            val inflater = layoutInflater
            builder.setTitle("Buscar artículos")
            val dialogLayout = inflater.inflate(R.layout.bio_alert_dialog_with_edittext, null)
            val editText = dialogLayout.findViewById<EditText>(R.id.editText)
            builder.setView(dialogLayout)
            builder.setPositiveButton("OK") { _, _ -> buscarArticulos(editText.text.toString()) }
            builder.setNegativeButton("Cancelar") { _, _ -> }
            builder.show()
        } else {
            fBuscando = false
            imgBioBuscar.setImageResource(R.drawable.lupa)

            // Guardamos en el documento las cantidades pedidas
            if (fUsarFormatos) {
                bioCat2DocFtos()
            } else {
                bioCat2Doc(false)
            }

            // Volvemos a cargar los arrays
            setImagesData()
            // Reiniciamos el adaptador y demás controles
            reiniciarAdaptador(true, "")
            if (fImagPorPagina == 1)
                verImpte()
            // Preparamos los datos adicionales del artículo
            mostrarDatosAdicionales()

            // Desbloqueamos el drawerlayout
            dwLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }
    }

    fun bioOrdenar(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        when (fOrdenacion) {
            1 -> {
                fOrdenacion = 2
                imgBioOrd.setImageResource(R.drawable.ordenacion_cod)
            }
            2 -> {
                fOrdenacion = 3
                imgBioOrd.setImageResource(R.drawable.ordenacion_alf)

            }
            3 -> {
                fOrdenacion = 1
                imgBioOrd.setImageResource(R.drawable.ordenacion_catalogo)
            }
        }

        // Volvemos a cargar los arrays
        setImagesData()

        // Reiniciamos el adaptador y demás controles
        reiniciarAdaptador(false, "")
        if (fImagPorPagina == 1)
            verImpte()
    }


    private fun buscarArticulos(queBuscar: String) {

        if (queBuscar != "") {
            // Primero guardamos en el documento para mantener las cantidades pedidas. Si usamos formatos no hace falta, porque
            // tenemos los datos en la tabla ftosLineas.
            if (!fUsarFormatos) {
                bioCat2Doc(false)
            }

            if (fArticulos.bioBuscar(queBuscar)) {

                fBuscando = true
                fItemActual = viewPager.currentItem
                imgBioBuscar.setImageResource(R.drawable.lupa_48)
                // Desactivamos los controles para no poder cambiar de catálogo durante la búsqueda
                dwLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

                aImages = ArrayList()
                // Por ahora mantenemos el array con los datos del documento tal y como está.
                for (articuloId in fArticulos.lArticulos) {
                    aImages.add(articuloId)
                }

                // Reiniciamos el adaptador y demás controles
                reiniciarAdaptador(false, "")
                if (fImagPorPagina == 1)
                    verImpte()

                // Preparamos los datos adicionales del artículo
                mostrarDatosAdicionales()
            }
            else MsjAlerta(this).alerta(resources.getString(R.string.msj_SinDatos))
        }
        else MsjAlerta(this).alerta(resources.getString(R.string.msj_SinBusqueda))
    }


    @SuppressLint("InflateParams")
    fun bioPedirCant(view: View) {
        view.getTag(0)              // Para que no dé warning el compilador

        if (aImages.isNotEmpty()) {
            val builder = AlertDialog.Builder(this)
            val inflater = layoutInflater
            builder.setTitle("Introducir cantidad")
            val dialogLayout = inflater.inflate(R.layout.bio_alert_dialog_pedir_cant, null)
            val editText = dialogLayout.findViewById<EditText>(R.id.editText)
            builder.setView(dialogLayout)
            builder.setPositiveButton("OK") { _, _ -> tomarCantidad(editText.text.toString()) }
            builder.setNegativeButton("Cancelar") { _, _ -> }
            builder.show()
        }
    }



    fun aceptarBioCat(view: View?) {
        view?.getTag(0)              // Para que no dé warning el compilador

        if (fBuscando) {
            // Si estamos buscando puede que en catalogoLineas no tengamos todas las líneas que queremos vender,
            // por eso hacemos el proceso de terminar la búsqueda antes de llamar a bioCat2Doc()
            // Guardamos en el documento las cantidades pedidas
            if (fUsarFormatos) {
                bioCat2DocFtos()
            } else {
                bioCat2Doc(false)
            }

            // Volvemos a cargar los arrays
            setImagesData()
        }

        // Si usamos formatos los datos de cantidades están en la tabla ftosLineas
        if (fUsarFormatos) {
            bioCat2DocFtos()
        } else {
            bioCat2Doc(true)
        }

        // Volvemos al documento
        val returnIntent = Intent()
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }



    private fun bioCat2DocFtos() {

        val lFtosLinea = ftosLineasDao?.getAllFtos() ?: emptyList<FtosLineasEnt>().toMutableList()

        if (lFtosLinea.isNotEmpty()) {
            // Primero borramos los artículos del documento
            for (ftoLineaEnt in lFtosLinea) {
                val idArticulo = ftoLineaEnt.articuloId
                fDocumento.borrarArticuloDeDoc(idArticulo)
            }

            // A continuación insertamos
            for (ftoLineaEnt in lFtosLinea) {
                if (ftoLineaEnt.cantidad != "" && ftoLineaEnt.borrar != "T") {

                    val idArticulo = ftoLineaEnt.articuloId
                    fArticulos.existeArticulo(idArticulo)

                    fDocumento.inicializarLinea()
                    fDocumento.fArticulo = ftoLineaEnt.articuloId
                    fDocumento.fAlmacen = fConfiguracion.almacen()
                    fDocumento.fCantidad = ftoLineaEnt.cantidad.replace(',', '.').toDouble()
                    val sCajas = ftoLineaEnt.cajas.replace(',', '.')
                    val sPiezas = ftoLineaEnt.piezas.replace(',', '.')
                    fDocumento.fCajas = if (sCajas != "") sCajas.toDouble() else 0.0
                    fDocumento.fPiezas = if (sPiezas != "") sPiezas.toDouble() else 0.0
                    fDocumento.fTextoLinea = ftoLineaEnt.textoLinea
                    fDocumento.fFlag5 = ftoLineaEnt.flag5

                    val queFlag = ftoLineaEnt.flag
                    fDocumento.fArtEnOferta = queFlag and FLAGLINEAVENTA_ARTICULO_EN_OFERTA > 0
                    fDocumento.fPrecioRating = queFlag and FLAGLINEAVENTA_PRECIO_RATING > 0
                    fDocumento.fHayCambPrecio = queFlag and FLAGLINEAVENTA_CAMBIAR_PRECIO > 0

                    fDocumento.fTasa1 = 0.0
                    fDocumento.fTasa2 = 0.0
                    fDocumento.fFormatoLin = ftoLineaEnt.formatoId
                    fDocumento.fCodArt = fArticulos.fCodigo
                    fDocumento.fDescr = fArticulos.fDescripcion
                    fDocumento.fCodigoIva = fArticulos.fCodIva
                    fDocumento.fPorcIva = fArticulos.fPorcIva
                    val sDtoLin = ftoLineaEnt.dto.replace(',', '.')
                    fDocumento.fDtoLin = if (sDtoLin != "") sDtoLin.toDouble() else 0.0

                    // Calculamos precios e importes
                    val sPrecio = ftoLineaEnt.precio.replace(',', '.')
                    if ((fIvaIncluido) && fAplicarIva) {
                        fDocumento.fPrecioII = if (sPrecio != "") sPrecio.toDouble() else 0.0
                        fDocumento.calculaPrBase()
                        fDocumento.calcularImpteII(false)
                        fDocumento.calcularImpte(true)
                    } else {
                        fDocumento.fPrecio = if (sPrecio != "") sPrecio.toDouble() else 0.0
                        fDocumento.calculaPrecioII()
                        fDocumento.calcularImpte(false)
                        fDocumento.calcularImpteII(true)
                    }

                    fDocumento.insertarLinea()
                }
            }
        }

        // Vaciamos la tabla temporal
        ftosLineasDao?.vaciar()
    }


    private fun bioCat2Doc(terminarVenta: Boolean) {

        val lCatLineas = catLineasDao?.getAllLineas() ?: emptyList<CatalogoLineasEnt>().toMutableList()

        if (lCatLineas.isNotEmpty()) {
            for (catLinea in lCatLineas) {
                // Si tenemos alguna cantidad, vendemos
                if (catLinea.cantidad != "") {

                    val idArticulo = catLinea.articuloId
                    fArticulos.existeArticulo(idArticulo)

                    val queLinea = fDocumento.existeLineaArticulo(idArticulo)
                    if (queLinea > 0) {
                        fDocumento.cargarLinea(queLinea)
                    } else {
                        fDocumento.inicializarLinea()
                        fDocumento.fArticulo = catLinea.articuloId
                        fDocumento.fAlmacen = fConfiguracion.almacen()
                    }
                    fDocumento.borrarArticuloDeDoc(idArticulo)

                    val sCajas = catLinea.cajas.replace(',', '.')
                    val sPiezas = catLinea.piezas.replace(',', '.')
                    val dCantidad = catLinea.cantidad.replace(',', '.').toDouble()
                    val dCajas = sCajas.toDouble()

                    fDocumento.fCajas = if (sCajas != "") dCajas else 0.0
                    fDocumento.fPiezas = if (sPiezas != "") sPiezas.toDouble() else 0.0
                    fDocumento.fCantidad = dCantidad
                    fDocumento.fLineaEsEnlace = catLinea.esEnlace == "T"

                    fDocumento.fTasa1 = 0.0
                    fDocumento.fTasa2 = 0.0
                    fDocumento.fCodArt = fArticulos.fCodigo
                    fDocumento.fDescr = fArticulos.fDescripcion
                    fDocumento.fCodigoIva = fArticulos.fCodIva
                    fDocumento.fPorcIva = fArticulos.fPorcIva
                    fDocumento.fLote = catLinea.lote

                    val sPrecio = catLinea.precio.replace(',', '.')
                    fDocumento.fPrecio = if (sPrecio != "") sPrecio.toDouble() else 0.0
                    if (catLinea.precioII != "") {
                        val sPrecioII = catLinea.precioII.replace(',', '.')
                        fDocumento.fPrecioII = if (sPrecioII != "") sPrecioII.toDouble() else 0.0
                    }
                    else fDocumento.fPrecioII = 0.0
                    val sDtoLin = catLinea.dto.replace(',', '.')
                    fDocumento.fDtoLin = if (sDtoLin != "") sDtoLin.toDouble() else 0.0

                    if ((fIvaIncluido) && fAplicarIva) {
                        fDocumento.calculaPrBase()
                        fDocumento.calcularImpteII(false)
                        fDocumento.calcularImpte(true)
                    } else {
                        fDocumento.calculaPrecioII()
                        fDocumento.calcularImpte(false)
                        fDocumento.calcularImpteII(true)
                    }

                    fDocumento.insertarLinea()

                    // Si el artículo tiene otro enlazado, lo vendemos ahora
                    val lineaEsEnlace = catLinea.esEnlace == "T"
                    if (terminarVenta && !lineaEsEnlace && fArticulos.tieneEnlace()) {
                        venderEnlace(dCajas, sPiezas, dCantidad)
                    }
                }
            }
        }

        // Vaciamos la tabla temporal
        catLineasDao?.vaciar()
    }


    private fun venderEnlace(dCajas: Double, sPiezas: String, dCantidad: Double) {

        val idArticulo = fArticulos.fEnlace
        // Comprobamos que el artículo a enlazar no esté ya en catalogoLineas
        if (!enlaceEnCatLineas(idArticulo)) {
            if (fArticulos.existeArticulo(idArticulo)) {

                fDocumento.inicializarLinea()

                fDocumento.fArticulo = fArticulos.fArticulo
                fDocumento.fAlmacen = fConfiguracion.almacen()

                if (fConfiguracion.igualarCantArtEnlace()) {
                    fDocumento.fCajas = dCajas
                    fDocumento.fPiezas = if (sPiezas != "") sPiezas.toDouble() else 0.0
                    fDocumento.fCantidad = dCantidad
                } else {
                    fDocumento.fCajas = 0.0
                    fDocumento.fPiezas = 0.0
                    fDocumento.fCantidad = 1.0
                }

                fDocumento.fTasa1 = 0.0
                fDocumento.fTasa2 = 0.0
                fDocumento.fCodArt = fArticulos.fCodigo
                fDocumento.fDescr = fArticulos.fDescripcion
                fDocumento.fCodigoIva = fArticulos.fCodIva
                fDocumento.fPorcIva = fArticulos.fPorcIva
                //val sDtoLin = cCatLineas.getString(cCatLineas.getColumnIndex("dto")).replace(',', '.')
                //fDocumento.fDtoLin = if (sDtoLin != "") sDtoLin.toDouble() else 0.0
                fDocumento.fDtoLin = 0.0

                fDocumento.calculaPrecioYDto(fArticulos.fGrupo, fArticulos.fDepartamento, fArticulos.fCodProv, fArticulos.fPorcIva)
                if (fIvaIncluido && fAplicarIva) {
                    fDocumento.calculaPrBase()
                    fDocumento.calcularImpteII(false)
                    fDocumento.calcularImpte(true)
                } else {
                    fDocumento.calculaPrecioII()
                    fDocumento.calcularImpte(false)
                    fDocumento.calcularImpteII(true)
                }
                fDocumento.fLineaEsEnlace = true

                fDocumento.insertarLinea()
            }
        }
    }

    private fun enlaceEnCatLineas(idArticulo: Int): Boolean {
        val queLinea = catLineasDao?.getLineaFromArt(idArticulo) ?: 0
        return queLinea != 0
    }



    // Manejo de los eventos del teclado en la actividad.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            aceptarBioCat(null)
            // Si el listener devuelve true, significa que el evento está procesado, y nadie debe hacer nada más.
            return true
        }
        // Para las demás cosas, se reenvía el evento al listener habitual.
        return super.onKeyDown(keyCode, event)
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fRequestVtaFormatos) {
            if (resultCode == Activity.RESULT_OK) {
                reiniciarAdaptador(true, "")
            }
        }

        if (requestCode == fRequestVtaDetalles) {
            if (resultCode == Activity.RESULT_OK) {

                val aDatosArt = fDocumento.datosArtEnCatLineas(aImages[fPosicion])
                // Si no existe ningún registro para el artículo en la tabla catalogoLineas, lo añadimos
                val insertar = (aDatosArt[0] == "F")
                val queLinea = data?.getIntExtra("linea", 0) ?: 0
                var sCajas = data?.getStringExtra("cajas") ?: ""
                var sPiezas = data?.getStringExtra("piezas") ?: ""
                var sCantidad = data?.getStringExtra("cantidad") ?: ""
                if (sCajas == "") sCajas = "0"
                if (sPiezas == "") sPiezas = "0"
                if (sCantidad == "") sCantidad = "0"

                val catLineaEnt = CatalogoLineasEnt()
                catLineaEnt.articuloId = aImages[fPosicion]
                catLineaEnt.cantidad = sCantidad
                catLineaEnt.cajas = sCajas
                catLineaEnt.piezas = sPiezas
                catLineaEnt.lote = data?.getStringExtra("lote") ?: ""

                val dCantidad = sCantidad.toDouble()
                val sPrecio = data?.getStringExtra("precio")?.replace(',', '.') ?: "0.0"
                val sDto = data?.getStringExtra("dto")?.replace(',', '.') ?: "0.0"

                if (fIvaIncluido && fAplicarIva) {
                    fDocumento.fPrecioII = if (sPrecio != "") sPrecio.toDouble() else 0.0
                    fDocumento.calculaPrBase()
                    fDocumento.calcularImpteII(false)
                    fDocumento.calcularImpte(true)
                } else {
                    fDocumento.fPrecio = if (sPrecio != "") sPrecio.toDouble() else 0.0
                    fDocumento.calculaPrecioII()
                    fDocumento.calcularImpte(false)
                    fDocumento.calcularImpteII(true)
                }

                catLineaEnt.precio = fDocumento.fPrecio.toString()
                catLineaEnt.precioII = fDocumento.fPrecioII.toString()
                catLineaEnt.importe = fDocumento.fImporte.toString()
                catLineaEnt.importeII = fDocumento.fImpteII.toString()
                catLineaEnt.dto = sDto

                if (insertar) catLineasDao?.insertar(catLineaEnt)
                else catLineasDao?.actualizarLinea(catLineaEnt.articuloId, catLineaEnt.cajas, catLineaEnt.piezas,
                    catLineaEnt.cantidad, catLineaEnt.precio, catLineaEnt.precioII, catLineaEnt.importe,
                    catLineaEnt.importeII, catLineaEnt.dto, queLinea)

                viewPager.adapter?.notifyDataSetChanged()
                edtCantidad.setText(String.format("%.0f", dCantidad))
            }
        }

        if (requestCode == fRequestCambDistr) {
            if (resultCode == Activity.RESULT_OK) {
                when (data?.getIntExtra("distribucion", -1) ?: -1) {
                    1 -> {
                        fImagPorPagina = 1
                        prefs.edit().putString("num_imag_cat", "1").apply()
                    }
                    2 -> {
                        fImagPorPagina = 4
                        prefs.edit().putString("num_imag_cat", "4").apply()
                    }
                    3 -> {
                        fImagPorPagina = 6
                        prefs.edit().putString("num_imag_cat", "6").apply()
                    }
                }

                fArtSeleccCat = 0
                reiniciarAdaptador(false, "")
            }
        }
    }




}