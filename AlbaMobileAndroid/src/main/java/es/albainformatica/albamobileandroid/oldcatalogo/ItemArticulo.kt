package es.albainformatica.albamobileandroid.oldcatalogo

/**
 * Created by jabegines on 11/10/13.
 */
class ItemArticulo {
    private var imagen: String
    var descr: String
    var descrFto: String = ""
        private set
    var articulo: Int
    var codigo: String
    var undCaja: String
        private set
    var prClte: String?
        private set
    var prHco: String = ""
        private set
    var dto: String?
        private set
    var dtoHco: String = ""
        private set
    var prCajas: String?
        private set
    var dtoCajas: String?
        private set
    var prOfta: String
        private set
    var dtoOfta: String
        private set
    var porcIva: Double
        private set
    private var hayOferta: Boolean
    var cantidad: Double = 0.0
    var cajas: Double = 0.0
    var stock: Double = 0.0
        private set
    var tieneHco: Boolean
        private set
    var cantHco: Double = 0.0
        private set
    var cajasHco: Double = 0.0
        private set
    var fechaHco: String = ""
        private set

    constructor(queArticulo: Int, queCodigo: String, descr: String, queUndCaja: String, quePrClte: String?,
        queDto: String?, quePrCajas: String?, queDtoCajas: String?, quePrOfta: String, queDtoOfta: String,
        quePorcIva: Double, conOferta: Boolean, queCantidad: String, queCajas: String , queStock: Double,
        tieneHco: Boolean, queDescrFto: String) {
        articulo = queArticulo
        this.descr = descr
        descrFto = queDescrFto
        codigo = queCodigo
        undCaja = queUndCaja
        prClte = quePrClte ?: "0.0"
        dto = queDto ?: "0.0"
        prCajas = quePrCajas ?: "0.0"
        dtoCajas = queDtoCajas ?: "0.0"
        prOfta = quePrOfta
        dtoOfta = queDtoOfta
        porcIva = quePorcIva
        hayOferta = conOferta
        this.tieneHco = tieneHco
        val sCantidad = queCantidad.replace(',', '.')
        cantidad = sCantidad.toDouble()
        val sCajas = queCajas.replace(',', '.')
        cajas = sCajas.toDouble()
        stock = queStock
        imagen = ""
    }

    constructor(queArticulo: Int, queCodigo: String, descr: String, queUndCaja: String, queCantHco: Double,
        queCajasHco: Double, quePrecioHco: String, queDtoHco: String, quePrecio: String?, queDto: String?,
        quePrOfta: String, queDtoOfta: String, quePorcIva: Double, conOferta: Boolean, queCantidad: String,
        queCajas: String, queFecha: String) {
        articulo = queArticulo
        this.descr = descr
        codigo = queCodigo
        undCaja = queUndCaja
        cantHco = queCantHco
        cajasHco = queCajasHco
        prClte = quePrecio ?: "0.0"
        prHco = quePrecioHco
        // Entiendo que si en el histórico no tengo el precio por caja, tengo que aplicar en this.prCajas el mismo que en
        // this.prHco, para cuando vendamos cajas desde el histórico visual.
        prCajas = quePrecioHco
        dto = queDto ?: "0.0"
        dtoHco = queDtoHco
        // Idem que con this.dtoCajas.
        dtoCajas = queDtoHco
        prOfta = quePrOfta
        dtoOfta = queDtoOfta
        porcIva = quePorcIva
        hayOferta = conOferta
        // Cuando estemos vendiendo desde el histórico no presentaremos el icono que indica que el artículo tiene histórico.
        tieneHco = false
        val sCantidad = queCantidad.replace(',', '.')
        cantidad = sCantidad.toDouble()
        val sCajas = queCajas.replace(',', '.')
        cajas = sCajas.toDouble()
        fechaHco = queFecha
        imagen = ""
    }

    // La imagen tendrá como nombre el id del artículo + '.jpg';
    fun getImagen(): String {
        return "ART_$articulo.jpg"
    }

    fun tieneOferta(): Boolean {
        return hayOferta
    }
}