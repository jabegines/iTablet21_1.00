package es.albainformatica.albamobileandroid



const val VERSION_PROGRAMA = "1.00"
const val COMPILACION_PROGRAMA = ".25"
const val VERSION_BD = 4
const val VERSION_BD_ROOM = 7


const val est_Telef_Browse: Byte = 1
const val est_Telef_Nuevo: Byte = 2
const val est_Telef_Editar: Byte = 3
const val est_Direcc_Browse: Byte = 1
const val est_Direc_Nueva: Byte = 2
const val est_Direc_Editar: Byte = 3


const val est_Vl_Browse: Byte = 1
const val est_Vl_Nueva: Byte = 2
const val est_Vl_Editar: Byte = 3

// Tipos de búsqueda
const val tipoBusq_Caja: Short = 4

// Modos de venta
const val mVta_Historico = 2
const val mVta_Catalogo = 3


// Color de la aplicación para menús (y otros controles)
const val COLOR_MENUS = "#009DD2"

// Anchos de campos
const val ancho_cod_almacen: Byte = 3
const val ancho_codclte: Byte = 6
const val ancho_empresa: Byte = 3
const val ancho_cod_incidencia: Byte = 2
const val ancho_codprov: Byte = 5
const val ancho_departamento: Byte = 3
const val ancho_formato: Byte = 2
const val ancho_grupo: Byte = 3
const val ancho_incidencia: Int = 200
const val ancho_nota_clte: Int = 200
const val ancho_tarifa: Byte = 2



const val TIPODOC_FACTURA: Short = 1
const val TIPODOC_ALBARAN: Short = 2
const val TIPODOC_PEDIDO: Short = 3
const val TIPODOC_PRESUPUESTO: Short = 6


// Flag articulos.
const val FLAGARTICULO_APLICARTRFCAJAS = 512
const val FLAGARTICULO_USARFORMATOS = 4096
const val FLAGARTICULO_VENDER_POR_DOSIS = 8192

// Flag 2 artículos
const val FLAGARTICULO_CONTROLA_TRAZABILIDAD = 512
const val FLAGARTICULO_USARPIEZAS = 16384

// Flag clientes.
// Flag1
const val FLAGCLIENTE_NOVENDER = 4
const val FLAGCLIENTE_CONTROLARRIESGO = 64
const val FLAGCLIENTE_APLICAROFERTAS = 4096

// Flag2
//const val FLAGCLIENTE_EXENTOIVA = 4

// Flag pendiente
const val FLAGPENDIENTE_EN_CARTERA = 128

// Flag rating
const val FLAGRATING_DESCUENTOIMPORTE = 2

// Configuración de tarifas
const val FLAGCNFTARIFAS_PARA_PIEZAS = 1

// Series
const val FLAGSERIE_INV_SUJ_PASIVO = 16
const val FLAGSERIE_PARA_FRA_SIMPL = 128


// Flag cabecera
const val FLAGCABECERAVENTA_PRECIOS_IVA_INCLUIDO = 1

// Flag linea venta
const val FLAGLINEAVENTA_SIN_CARGO = 2
const val FLAGLINEAVENTA_PRECIO_RATING = 8
const val FLAGLINEAVENTA_CAMBIAR_DESCRIPCION = 16
const val FLAGLINEAVENTA_CAMBIAR_PRECIO = 32
const val FLAGLINEAVENTA_CAMBIAR_TARIFA_PRECIO = 128
const val FLAGLINEAVENTA_ARTICULO_EN_OFERTA = 4096
const val FLAGLINEAVENTA_POSIBLE_OFERTA = 16384

// Flag3
const val FLAG3LINEAVENTA_ARTICULO_POR_PIEZAS = 1
const val FLAG3LINEAVENTA_PRECIO_POR_PIEZAS = 256


// LLamadas a CatalogoArticulos
const val LISTA_ARTICULOS = 1
const val GRUPOS_Y_DEP = 2
const val CATALOGOS = 3
const val HISTORICO = 4
const val CLASIFICADORES = 5


// Tipos de impresora
const val IMPRESORA_STARTDP8340S = 1
const val IMPRESORA_EPSONTMU295 = 2
const val IMPRESORA_DATAMAX_APEX_2 = 3
const val IMPRESORA_INTERMEC_PB51 = 4
const val IMPRESORA_BIXOLON_SPP_R410 = 5
const val IMPRESORA_GENERICA_110 = 6
const val IMPRESORA_GENERICA_80 = 7
const val IMPRESORA_ZEBRA_80 = 8


class ListaClientes {
    var clienteId: Int = 0
    var codigo: String = ""
    var nombre: String = ""
    var nombreComercial: String = ""
    var flag: Int = 0
}



// Clase que usamos para el adaptador del recyclerview en ServicioEnviar
class ListaPaquetes {
    var numPaquete = 0
    var fechaHoraEnvio: String = ""
    var fechaHoraRecogida: String = ""
}


class DatosVtaFtos {
    var codigo = 0
    var descripcion: String = ""
    var idFtosLineas = 0
    var borrar: String = ""
    var idHistorico = 0
    var dosis1: String = ""
}

class DatosVtaSeries {
    var serie: String = ""
}


class DatosEmpresas {
    var codigo: Int = 0
    var nombreFiscal: String = ""
}


class DatosDivisa {
    var codigo: String = ""
    var descripcion: String = ""


    // Si no incluimos esta función el spinner no mostrará bien la descripción
    override fun toString(): String {
        return descripcion
    }
}


class DatosInfCobros {
    var clienteId: String = ""
    var fechaCobro: String = ""
    var tipoDoc: Short = 0
    var serie: String = ""
    var numero: String = ""
    var cobro: String = ""
}

class DatosResCobros {
    var descripcion: String = ""
    var cobro: Double = 0.0
}

class DatosHcoPorDoc {
    var idHco = 0
    var articulo = 0
    var codigo: String = ""
    var descr: String = ""
    var porcDev: String = ""
    var cantpedida: String = ""
}


// Clase que usamos para calcular las ofertas por volumen en la clase Documento
class ListOftVol {
    var idOferta = 0
    var importe: Double = 0.0
    var articuloDesct = 0
    var tarifa: String = ""
}


class DatosCarga {
    var cargaId = 0
    var empresa: Short = 0
    var fecha: String = ""
    var hora: String = ""
    var finDeDia: String = ""
}




var fArtSeleccCat = 0
