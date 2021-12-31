package es.albainformatica.albamobileandroid.oldcatalogo


class ItemCatalogo(val codigo: Int, val descr: String) {

    val imagen: String
        get() = "CLS_$codigo.jpg"
}