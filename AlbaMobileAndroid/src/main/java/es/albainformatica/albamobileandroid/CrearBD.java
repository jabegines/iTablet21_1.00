package es.albainformatica.albamobileandroid;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by jabegines on 10/10/13.
 */
public class CrearBD extends BaseDatos {
    private final SQLiteDatabase dbAlba;

    public CrearBD(Context contexto) {
        super(contexto);
        dbAlba = getWritableDatabase();

        crearArticulos();
        crearArtHabituales();
        crearBusquedas();
        crearIvas();
        crearStock();
        crearClientes();
        crearDtosCltes();
        crearCnfTarifas();
        crearRutero();
        crearDirecciones();
        crearTarifas();
        crearCabeceras();
        crearLineasDiferidas();
        crearDesctosLineas();
        crearRatingArt();
        crearLineas();
        crearFtosLineas();
        crearHistorico();
        crearTmpHco();
        crearGrupos();
        crearDepartamentos();
        crearClasificadores();
        crearArticulosClasif();
        crearFormatos();
        crearTrfFormatos();
        crearHistMes();
        crearCargas();
        crearCargasLineas();
        crearHcoArtClte();
        crearEstadDevoluc();
        crearHcoCompSemMes();
    }

    public void close() {
        if (dbAlba != null)
            dbAlba.close();
    }

  private void crearArticulos() {
      dbAlba.execSQL("DROP TABLE IF EXISTS articulos");

      String cadenaSQL = "CREATE TABLE IF NOT EXISTS articulos("
              + "articulo INTEGER NOT NULL, codigo VARCHAR(15) NOT NULL, descr VARCHAR(40) COLLATE NOCASE NOT NULL,"
              + " tipoiva VARCHAR(3), grupo VARCHAR(3), dpto VARCHAR(3), prov VARCHAR(5), costo VARCHAR(10), ucaja VARCHAR(7),"
              + " medida VARCHAR(3), flag1 VARCHAR(5), flag2 VARCHAR(5), peso VARCHAR(10), tasa1 VARCHAR(10),"
              + " tasa2 VARCHAR(10), enlace INTEGER, PRIMARY KEY (articulo))";
      dbAlba.execSQL(cadenaSQL);
  }

  private void crearArtHabituales() {
    dbAlba.execSQL("DROP TABLE IF EXISTS arthabituales");

    String cadenaSQL = "CREATE TABLE IF NOT EXISTS arthabituales(" +
              "articulo INTEGER NOT NULL, cliente VARCHAR(9) NOT NULL, formato INTEGER NOT NULL, flag INTEGER," +
              " texto VARCHAR(200), PRIMARY KEY (articulo, cliente, formato))";
    dbAlba.execSQL(cadenaSQL);
  }

    private void crearClientes() {
        dbAlba.execSQL("DROP TABLE IF EXISTS clientes");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS clientes("
            + "cliente VARCHAR(9) NOT NULL, codigo VARCHAR(6), nomfi VARCHAR(40) COLLATE NOCASE,"
            + "nomco VARCHAR(40) COLLATE NOCASE, cif VARCHAR(16), direcc VARCHAR(35), locali VARCHAR(30),"
            + "cpostal VARCHAR(5), provin VARCHAR(20), apliva VARCHAR(1), aplrec VARCHAR(1),"
            + "tipoiva VARCHAR(3), tarifa VARCHAR(2), tardto VARCHAR(2), tarifaPiezas VARCHAR(2), fpago VARCHAR(3),"
            + "ruta VARCHAR(4), riesgo VARCHAR(10), pendiente  VARCHAR(10), flag VARCHAR(5), estado VARCHAR(2), tipo VARCHAR(1),"
            + "flag2 VARCHAR(5), ramo VARCHAR(4), numexport INTEGER, tieneincid VARCHAR(1), maxdias INTEGER, "
            + "maxfraspdtes INTEGER, matricula VARCHAR(50),"
            + " PRIMARY KEY (cliente))";
        dbAlba.execSQL(cadenaSQL);
    }


    private void crearDtosCltes() {
        dbAlba.execSQL("DROP TABLE IF EXISTS dtoscltes");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS dtoscltes("
            + "cliente INTEGER, iddescuento INTEGER, dto VARCHAR(7), PRIMARY KEY (cliente, iddescuento))";

        dbAlba.execSQL(cadenaSQL);
    }

    private void crearBusquedas() {
        dbAlba.execSQL("DROP TABLE IF EXISTS busquedas");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS busquedas("
            + "clave VARCHAR(25), articulo INTEGER, tipo VARCHAR(1),"
            + " tcaja VARCHAR(1), ucaja VARCHAR(10), PRIMARY KEY (clave))";
        dbAlba.execSQL(cadenaSQL);
    }


    private void crearIvas() {
        dbAlba.execSQL("DROP TABLE IF EXISTS ivas");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS ivas("
                + "codigo VARCHAR(3), tipo VARCHAR(3), iva VARCHAR(5),"
                + " recargo VARCHAR(5), PRIMARY KEY (codigo))";
        dbAlba.execSQL(cadenaSQL);
    }


    private void crearStock() {
        dbAlba.execSQL("DROP TABLE IF EXISTS stock");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS stock("
                + "articulo INTEGER, empresa INTEGER, ent VARCHAR(12), entc VARCHAR(12), entp VARCHAR(12),"
                + " sal VARCHAR(12), salc VARCHAR(12), salp VARCHAR(12), PRIMARY KEY (articulo, empresa))";
        dbAlba.execSQL(cadenaSQL);
    }


    private void crearCnfTarifas() {
        dbAlba.execSQL("DROP TABLE IF EXISTS cnftarifas");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS cnftarifas("
                + "codigo VARCHAR(2), tarifa VARCHAR(20), precios VARCHAR(1), flag INTEGER,"
                + "PRIMARY KEY (codigo))";
        dbAlba.execSQL(cadenaSQL);
    }



    private void crearRutero() {
        dbAlba.execSQL("DROP TABLE IF EXISTS rutero");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS rutero("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, ruta VARCHAR(4), orden VARCHAR(4), cliente VARCHAR(9))";
        dbAlba.execSQL(cadenaSQL);
    }


    private void crearDirecciones() {
        dbAlba.execSQL("DROP TABLE IF EXISTS dirclientes");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS dirclientes("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, cliente VARCHAR(9), alm VARCHAR(3), orden VARCHAR(3), sucursal VARCHAR(3),"
                + "direcc VARCHAR(50), poblac VARCHAR(50), provin VARCHAR(40), cpostal VARCHAR(5),"
                + "pais VARCHAR(30), dirdoc VARCHAR(1), dirmer VARCHAR(1), estado VARCHAR(2), numexport INTEGER,"
                + "matricula VARCHAR(50))";
        dbAlba.execSQL(cadenaSQL);
    }

    private void crearTarifas() {
        dbAlba.execSQL("DROP TABLE IF EXISTS tarifas");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS tarifas("
                + "articulo INTEGER, tarifa VARCHAR(2), precio VARCHAR(10), dto VARCHAR(7))";
        dbAlba.execSQL(cadenaSQL);
    }

    private void crearCabeceras() {
        dbAlba.execSQL("DROP TABLE IF EXISTS cabeceras");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS cabeceras("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, tipodoc VARCHAR(2), alm VARCHAR(3), serie VARCHAR(6),"
            + " numero VARCHAR(6), ejer VARCHAR(3), empresa VARCHAR(3), fecha VARCHAR(10), hora VARCHAR(5),"
            + " cliente VARCHAR(9), apliva VARCHAR(1), aplrec VARCHAR(1), bruto VARCHAR(14), dto VARCHAR(7), dto2 VARCHAR(7),"
            + " dto3 VARCHAR(7), dto4 VARCHAR(7), base VARCHAR(14), iva VARCHAR(11), recargo VARCHAR(9), total VARCHAR(14),"
            + " direnv VARCHAR(1), ruta VARCHAR(4), estado VARCHAR(1), estadoinicial VARCHAR(1), flag VARCHAR(5), obs1 VARCHAR(80),"
            + " obs2 VARCHAR(80), facturado VARCHAR(1), numexport INTEGER, fechaentrega VARCHAR(10), fpago VARCHAR(3),"
            + " tipoincidencia INTEGER, textoincidencia VARCHAR(200), firmado VARCHAR(1), fechafirma VARCHAR(10), horafirma VARCHAR(5),"
            + " hoja INTEGER, orden INTEGER, tipoPedido INTEGER, almDireccion VARCHAR(3), ordenDireccion VARCHAR(3)," +
              "imprimido VARCHAR(1), matricula VARCHAR(50))";
        dbAlba.execSQL(cadenaSQL);
    }

    private void crearLineas() {
        dbAlba.execSQL("DROP TABLE IF EXISTS lineas");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS lineas(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, cabeceraId INTEGER, linea VARCHAR(3), articulo INTEGER," +
                " codigo VARCHAR(15), descr VARCHAR(40), tarifa VARCHAR(2), precio VARCHAR(10), precioii VARCHAR(10)," +
                " codigoiva VARCHAR(3), cajas VARCHAR(7), cantidad VARCHAR(13), importe VARCHAR(14), importeii VARCHAR(14)," +
                " dto VARCHAR(6), dtoi VARCHAR(7), dtoiii VARCHAR(7), lote VARCHAR(20), piezas VARCHAR(13), flag VARCHAR(5)," +
                " flag3 VARCHAR(5), flag5 VARCHAR(5), tasa1 VARCHAR(10), tasa2 VARCHAR(10), formato INTEGER, incidencia INTEGER," +
                " textolinea VARCHAR(200), cajasorg VARCHAR(7), cantidadorg VARCHAR(13), piezasorg VARCHAR(13), modif_nueva VARCHAR(1)," +
                " precioTarifa VARCHAR(10), dtoTarifa VARCHAR(6), almacenPedido VARCHAR(3), idOferta INTEGER," +
                " dtoOftVol VARCHAR(6), esEnlace VARCHAR(1))";
        dbAlba.execSQL(cadenaSQL);
    }


    private void crearLineasDiferidas() {
        dbAlba.execSQL("DROP TABLE IF EXISTS lineasDiferidas");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS lineasDiferidas(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, cabeceraId INTEGER, serie VARCHAR(6), numero VARCHAR(6), fecha VARCHAR(10)," +
                " linea VARCHAR(3), articulo INTEGER, codigo VARCHAR(15), descr VARCHAR(40), precio VARCHAR(10), cajas VARCHAR(7)," +
                " cantidad VARCHAR(13), importe VARCHAR(14), dto VARCHAR(6), piezas VARCHAR(13), codigoiva VARCHAR(3), flag VARCHAR(5)," +
                " flag3 VARCHAR(5), formato INTEGER)";
        dbAlba.execSQL(cadenaSQL);
    }


    private void crearFtosLineas() {
        dbAlba.execSQL("DROP TABLE IF EXISTS ftosLineas");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS ftosLineas(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, linea INTEGER, articulo INTEGER, formato INTEGER, " +
                " cajas VARCHAR(7), cantidad VARCHAR(13), piezas VARCHAR(13), precio VARCHAR(10), dto VARCHAR(6)," +
                " textolinea VARCHAR(200), flag VARCHAR(5), flag5 VARCHAR(5), borrar VARCHAR(1))";
        dbAlba.execSQL(cadenaSQL);
    }


    private void crearDesctosLineas() {
        dbAlba.execSQL("DROP TABLE IF EXISTS desctoslineas");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS desctoslineas("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, linea INTEGER, orden INTEGER, descuento VARCHAR(6), importe VARCHAR(10),"
            + " cantidad1 VARCHAR(10), cantidad2 VARCHAR(10), desderating VARCHAR(1))";
        dbAlba.execSQL(cadenaSQL);
    }

    private void crearRatingArt() {
        dbAlba.execSQL("DROP TABLE IF EXISTS ratingart");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS ratingart("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, articulo INTEGER, alm VARCHAR(3), cliente VARCHAR(9), ramo VARCHAR(4), tarifa VARCHAR(2),"
                + "inicio VARCHAR(10), fin VARCHAR(10), formato INTEGER, precio VARCHAR(10), dto VARCHAR(6), flag VARCHAR(5))";
        dbAlba.execSQL(cadenaSQL);
    }


    private void crearHistorico() {
        dbAlba.execSQL("DROP TABLE IF EXISTS historico");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS historico("
              + "_id INTEGER PRIMARY KEY AUTOINCREMENT, cliente VARCHAR(9), articulo INTEGER, cajas VARCHAR(7),"
              + " cantidad VARCHAR(13), piezas VARCHAR(13), precio VARCHAR(10), dto VARCHAR(6), formato INTEGER, fecha VARCHAR(10))";

        dbAlba.execSQL(cadenaSQL);
    }

    private void crearHistMes() {
        dbAlba.execSQL("DROP TABLE IF EXISTS histmes");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS histmes("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, cliente VARCHAR(9), articulo INTEGER, mes INTEGER,"
            + " cantidad VARCHAR(13), cantidadant VARCHAR(13), importe VARCHAR(14), importeant VARCHAR(14),"
            + " cajas VARCHAR(7), cajasant VARCHAR(7), piezas VARCHAR(13), piezasant VARCHAR(13))";

        dbAlba.execSQL(cadenaSQL);
    }


    private void crearTmpHco() {
        dbAlba.execSQL("DROP TABLE IF EXISTS tmphco");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS tmphco("
            + "linea INTEGER, articulo INTEGER, codigo VARCHAR(15), descr VARCHAR(40), cajas VARCHAR(7),"
            + " cantidad VARCHAR(13), piezas VARCHAR(13), precio VARCHAR(10), precioii VARCHAR(10), dto VARCHAR(6), dtoi VARCHAR(7),"
            + " dtoiii VARCHAR(7), codigoiva VARCHAR(3), tasa1 VARCHAR(10), tasa2 VARCHAR(10), formato INTEGER, flag VARCHAR(5),"
            + " flag3 VARCHAR(5), flag5 VARCHAR(5), textolinea VARCHAR(200), lote VARCHAR(20), almacenPedido VARCHAR(3), incidencia INTEGER)";
        dbAlba.execSQL(cadenaSQL);
    }


    private void crearGrupos(){
        dbAlba.execSQL("DROP TABLE IF EXISTS grupos");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS grupos(codigo INTEGER PRIMARY KEY, descr VARCHAR(30))";
        dbAlba.execSQL(cadenaSQL);
    }

    private void crearDepartamentos(){
        dbAlba.execSQL("DROP TABLE IF EXISTS departamentos");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS departamentos("
            + "grupo INTEGER, codigo INTEGER, descr VARCHAR(30), PRIMARY KEY(grupo, codigo))";
        dbAlba.execSQL(cadenaSQL);
    }

    private void crearClasificadores(){
        dbAlba.execSQL("DROP TABLE IF EXISTS clasificadores");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS clasificadores("
            + "codigo INTEGER PRIMARY KEY, descr VARCHAR(150), padre INTEGER, nivel INTEGER, orden INTEGER, flag INTEGER)";
        dbAlba.execSQL(cadenaSQL);
    }

    private void crearArticulosClasif(){
        dbAlba.execSQL("DROP TABLE IF EXISTS articclasif");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS articclasif("
            + "articulo INTEGER, clasificador INTEGER, orden INTEGER, PRIMARY KEY(articulo, clasificador))";
        dbAlba.execSQL(cadenaSQL);
    }


    private void crearFormatos(){
        dbAlba.execSQL("DROP TABLE IF EXISTS formatos");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS formatos("
            + "codigo INTEGER PRIMARY KEY, descr VARCHAR(30), flag INTEGER, dosis1 VARCHAR(8))";
        dbAlba.execSQL(cadenaSQL);
    }

    private void crearTrfFormatos(){
        dbAlba.execSQL("DROP TABLE IF EXISTS trfformatos");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS trfformatos("
            + "articulo INTEGER, tarifa INTEGER, formato INTEGER, precio VARCHAR(10), dto VARCHAR(7), PRIMARY KEY (articulo, tarifa, formato))";
        dbAlba.execSQL(cadenaSQL);
    }


    private void crearCargas() {
        dbAlba.execSQL("DROP TABLE IF EXISTS cargas");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS cargas("
            + "cargaId INTEGER PRIMARY KEY AUTOINCREMENT, empresa INTEGER, fecha VARCHAR(10), "
            + "hora VARCHAR(5), esFinDeDia VARCHAR(1),"
            + " estado VARCHAR(1), numexport INTEGER, matricula VARCHAR(50))";
        dbAlba.execSQL(cadenaSQL);
    }

    private void crearCargasLineas() {
        dbAlba.execSQL("DROP TABLE IF EXISTS cargasLineas");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS cargasLineas("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, cargaId INTEGER, articulo INTEGER, lote VARCHAR(20), cajas VARCHAR(7),"
            + " cantidad VARCHAR(13))";
        dbAlba.execSQL(cadenaSQL);
    }



    private void crearHcoArtClte() {
        dbAlba.execSQL("DROP TABLE IF EXISTS hcoPorArticClte");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS hcoPorArticClte(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, articulo INTEGER, cliente INTEGER, tipodoc VARCHAR(2), serie VARCHAR(6)," +
                " numero INTEGER, ejercicio INTEGER, fecha VARCHAR(10), ventas VARCHAR(14), devoluciones VARCHAR(14))";
        dbAlba.execSQL(cadenaSQL);
    }

    private void crearEstadDevoluc() {
        dbAlba.execSQL("DROP TABLE IF EXISTS estadDevoluc");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS estadDevoluc(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, cliente INTEGER, articulo INTEGER, porcDevol VARCHAR(6))";
        dbAlba.execSQL(cadenaSQL);
    }


    private void crearHcoCompSemMes() {
        dbAlba.execSQL("DROP TABLE IF EXISTS hcoCompSemMes");

        String cadenaSQL = "CREATE TABLE IF NOT EXISTS hcoCompSemMes(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, fecha VARCHAR(10), cliente INTEGER, articulo INTEGER, cantidad VARCHAR(13))";
        dbAlba.execSQL(cadenaSQL);
    }


}
