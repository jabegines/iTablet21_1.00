package es.albainformatica.albamobileandroid.oldcatalogo

import es.albainformatica.albamobileandroid.ancho_departamento
import es.albainformatica.albamobileandroid.ancho_grupo
import es.albainformatica.albamobileandroid.ponerCeros


class ItemDepartam(var grupo: Short, var codigo: Short, var descr: String) {
    val imagen: String
        get() = "DPT_" + ponerCeros(grupo.toString(), ancho_grupo) +
                ponerCeros(codigo.toString(), ancho_departamento) + ".jpg"
}