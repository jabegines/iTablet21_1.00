package es.albainformatica.albamobileandroid.database

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import es.albainformatica.albamobileandroid.VERSION_BD_ROOM
import es.albainformatica.albamobileandroid.dao.*
import es.albainformatica.albamobileandroid.dao.DocsCabPiesDao
import es.albainformatica.albamobileandroid.entity.*





@Database(entities=[AlmacenesEnt::class, ArticDatAdicEnt::class, CabDiferidasEnt::class, CatalogoLineasEnt::class,
    CobrosEnt::class, ConfiguracionEnt::class, ContactosCltesEnt::class, CostosArticulosEnt::class, DivisasEnt::class,
    DocsCabPiesEnt::class, EjerciciosEnt::class, EmpresasEnt::class, FormasPagoEnt::class, HistRepreEnt::class,
    LineasDifEnt::class, LotesEnt::class, NotasCltesEnt::class, NumExportEnt::class, OfertasEnt::class,
    OftCantRangosEnt::class, OftVolRangosEnt::class, OftVolumenEnt::class, PendienteEnt::class, ProveedoresEnt::class,
    RatingGruposEnt::class, RatingProvEnt::class, RutasEnt::class, SaldosEnt::class, SeriesEnt::class,
    TempCltesEnt::class, TiposIncEnt::class],
    version = VERSION_BD_ROOM,
    exportSchema = true
)
abstract class MyDatabase: RoomDatabase() {

    abstract fun almacenesDao(): AlmacenDao
    abstract fun articDatAdicDao(): ArticDatAdicDao
    abstract fun cabDiferidasDao(): CabDiferidasDao
    abstract fun catalogoLineasDao(): CatalogoLineasDao
    abstract fun cobrosDao(): CobrosDao
    abstract fun configuracionDao(): ConfiguracionDao
    abstract fun contactosCltesDao(): ContactosCltesDao
    abstract fun costosArticulosDao(): CostosArticulosDao
    abstract fun divisasDao(): DivisasDao
    abstract fun docsCabPiesDao(): DocsCabPiesDao
    abstract fun ejerciciosDao(): EjerciciosDao
    abstract fun empresasDao(): EmpresasDao
    abstract fun formasPagoDao(): FormasPagoDao
    abstract fun histRepreDao(): HistRepreDao
    abstract fun lineasDifDao(): LineasDifDao
    abstract fun lotesDao(): LotesDao
    abstract fun notasCltesDao(): NotasCltesDao
    abstract fun numExportDao(): NumExportDao
    abstract fun ofertasDao(): OfertasDao
    abstract fun oftCantRangosDao(): OftCantRangosDao
    abstract fun oftVolRangosDao(): OftVolRangosDao
    abstract fun oftVolumenDao(): OftVolumenDao
    abstract fun pendienteDao(): PendienteDao
    abstract fun proveedoresDao(): ProveedoresDao
    abstract fun ratingGruposDao(): RatingGruposDao
    abstract fun ratingProvDao(): RatingProvDao
    abstract fun rutasDao(): RutasDao
    abstract fun saldosDao(): SaldosDao
    abstract fun seriesDao(): SeriesDao
    abstract fun  tempCltesDao(): TempCltesDao
    abstract fun tiposIncDao(): TiposIncDao



    companion object {
        var INSTANCE: MyDatabase? = null
        var queBDRoom: String = "ibsTablet00.db"

        fun getInstance(context: Context): MyDatabase? {
            if (INSTANCE == null) {
                synchronized(MyDatabase::class) {
                    INSTANCE = androidx.room.Room.databaseBuilder(context.applicationContext,
                        MyDatabase::class.java, queBDRoom)
                        .allowMainThreadQueries()
                        .addMigrations(MIGRATION_2_3)
                        .addMigrations(MIGRATION_3_4)
                        .addMigrations(MIGRATION_4_5)
                        .addMigrations(MIGRATION_5_6)
                        .addMigrations(MIGRATION_6_7)
                        .build()
                }
            }
            return INSTANCE
        }


        fun destroyInstance() {
            INSTANCE = null
        }


        private val MIGRATION_2_3: Migration = object: Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE 'Empresas' ADD COLUMN 'venderIvaIncl' TEXT NOT NULL DEFAULT ''")
                database.execSQL("UPDATE Empresas SET venderIvaIncl = 'F'")
            }
        }

        private val MIGRATION_3_4: Migration = object: Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS 'Ofertas' ('articuloId' INTEGER NOT NULL, 'empresa' INTEGER NOT NULL," +
                            "'tarifa' INTEGER NOT NULL, 'precio' TEXT NOT NULL, 'dto' TEXT NOT NULL, " +
                            "'formato' INTEGER NOT NULL, 'tipoOferta' INTEGER NOT NULL, 'idOferta' INTEGER NOT NULL, " +
                            "'fFinal' TEXT NOT NULL," +
                            "PRIMARY KEY ('articuloId', 'empresa', 'tarifa'))")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS 'OftVolumen' ('oftVolumenId' INTEGER NOT NULL PRIMARY KEY, " +
                            "'almacen' INTEGER NOT NULL, 'articuloDesct' INTEGER NOT NULL, 'tarifa' INTEGER NOT NULL)")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS 'OftVolRangos' ('oftVolRangosId' INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "'idOferta' INTEGER NOT NULL, 'desdeImpte' INTEGER NOT NULL, " +
                            "'hastaImpte' INTEGER NOT NULL, 'descuento' TEXT NOT NULL)")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS 'OftCantRangos' ('oftCantRangosId' INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                            "'idOferta' INTEGER NOT NULL, articuloId INTEGER NOT NULL, 'desdeCantidad' TEXT NOT NULL, " +
                            "'hastaCantidad' TEXT NOT NULL, 'precioBase' TEXT NOT NULL)")

                database.execSQL("ALTER TABLE 'Saldos' ADD COLUMN pendiente TEXT NOT NULL DEFAULT ''")
                database.execSQL("UPDATE Saldos SET pendiente = '0.0'")

                database.execSQL("ALTER TABLE 'Lotes' ADD COLUMN empresa INTEGER NOT NULL DEFAULT 0")
                database.execSQL("UPDATE Lotes SET empresa = 0")
            }
        }

        private val MIGRATION_4_5: Migration = object: Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS 'Divisas' ('codigo' TEXT NOT NULL PRIMARY KEY, 'clave' TEXT NOT NULL, " +
                             "'orden' INTEGER NOT NULL, 'descripcion' TEXT NOT NULL, 'pideAnotacion' TEXT NOT NULL, " +
                             "'anotacion' TEXT NOT NULL)")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS 'Cobros' ('cobroId' INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "'clienteId' INTEGER NOT NULL, 'tipoDoc' INTEGER NOT NULL, 'almacen' INTEGER NOT NULL, " +
                            "'serie' TEXT NOT NULL, 'numero' INTEGER NOT NULL, 'ejercicio' INTEGER NOT NULL, " +
                            "'empresa' INTEGER NOT NULL, 'fechaCobro' TEXT NOT NULL, 'cobro' TEXT NOT NULL, " +
                            "'fPago' TEXT NOT NULL, 'divisa' TEXT NOT NULL, 'anotacion' TEXT NOT NULL, " +
                            "'codigo' TEXT NOT NULL, 'estado' TEXT NOT NULL, 'vAlmacen' TEXT NOT NULL, " +
                            "'vPuesto' TEXT NOT NULL, 'vApunte' TEXT NOT NULL, 'vEjercicio' TEXT NOT NULL, " +
                            "'numExport' INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_5_6: Migration = object: Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL("ALTER TABLE 'Cobros' ADD COLUMN 'matricula' TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_6_7: Migration = object: Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS 'Rutas' ('rutaId' INTEGER NOT NULL PRIMARY KEY, " +
                            "'descripcion' TEXT NOT NULL)")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS 'NotasCltes' ('notaId' INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "'clienteId' INTEGER NOT NULL, 'nota' TEXT NOT NULL, 'fecha' TEXT NOT NULL, " +
                            "'estado' TEXT NOT NULL, 'numExport' INTEGER NOT NULL, 'matricula' TEXT NOT NULL)")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS 'TiposInc' ('tipoIncId' INTEGER NOT NULL PRIMARY KEY, " +
                            "'descripcion' TEXT NOT NULL)")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS 'TempCltes' ('clienteId' INTEGER NOT NULL PRIMARY KEY, " +
                            "'codigo' INTEGER NOT NULL, 'nombre' TEXT NOT NULL COLLATE NOCASE, " +
                            "'nombreComercial' TEXT NOT NULL COLLATE NOCASE, 'cif' TEXT NOT NULL, " +
                            "'direccion' TEXT NOT NULL, 'localidad' TEXT NOT NULL, 'cPostal' TEXT NOT NULL, " +
                            "'provincia' TEXT NOT NULL, 'aplIva' TEXT NOT NULL, 'aplRec' TEXT NOT NULL, " +
                            "'tipoIva' INTEGER NOT NULL, 'tarifaId' INTEGER NOT NULL, 'tarifaDtoId' INTEGER NOT NULL, " +
                            "'fPago' TEXT NOT NULL, 'rutaId' INTEGER NOT NULL, 'riesgo' TEXT NOT NULL, " +
                            "'pendiente' TEXT NOT NULL, 'flag' INTEGER NOT NULL, 'flag2' INTEGER NOT NULL, " +
                            "'estado' TEXT NOT NULL, 'ramo' INTEGER NOT NULL, " +
                            "'numExport' INTEGER NOT NULL, 'tieneIncid' TEXT NOT NULL, 'maxDias' INTEGER NOT NULL, " +
                            "'maxFrasPdtes' INTEGER NOT NULL, 'matricula' TEXT NOT NULL)")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS 'NumExport' ('numExport' INTEGER NOT NULL PRIMARY KEY, " +
                            "'fecha' TEXT NOT NULL, 'hora' TEXT NOT NULL)")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS 'HistRepre' ('histRepreId' INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "'representanteId' INTEGER NOT NULL, 'importe' TEXT NOT NULL, 'mes' INTEGER NOT NULL, " +
                            "'anyo' INTEGER NOT NULL)")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS 'ArticDatAdic' ('articuloId' INTEGER NOT NULL, " +
                            "'valor' INTEGER NOT NULL, 'cadena' TEXT NOT NULL, " +
                            "PRIMARY KEY ('articuloId', 'valor'))")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS 'RatingProv' ('ratingProvId' INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "'proveedorId' INTEGER NOT NULL, 'almacen' INTEGER NOT NULL, 'clienteId' INTEGER NOT NULL, " +
                            "'ramoId' INTEGER NOT NULL, 'tarifa' INTEGER NOT NULL, 'inicio' TEXT NOT NULL, " +
                            "'fin' TEXT NOT NULL, 'dto' TEXT NOT NULL)")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS 'CatalogoLineas' ('catLineasId' INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "'linea' INTEGER NOT NULL, 'articuloId' INTEGER NOT NULL, 'cajas' TEXT NOT NULL, " +
                            "'cantidad' TEXT NOT NULL, 'piezas' TEXT NOT NULL, 'precio' TEXT NOT NULL, 'precioII' TEXT NOT NULL, " +
                            "'dto' TEXT NOT NULL, 'importe' TEXT NOT NULL, 'importeII' TEXT NOT NULL, 'textoLinea' TEXT NOT NULL, " +
                            "'flag' INTEGER NOT NULL, 'flag5' INTEGER  NOT NULL, 'lote' TEXT NOT NULL, 'esEnlace' TEXT NOT NULL)")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS 'CabDiferidas' ('cabDiferidaId' INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "'serie' TEXT NOT NULL, 'numero' INTEGER NOT NULL, 'ejercicio' INTEGER NOT NULL, " +
                            "'empresa' INTEGER NOT NULL, 'fecha' TEXT NOT NULL, 'clienteId' INTEGER NOT NULL, " +
                            "'aplIva' TEXT NOT NULL, 'aplRec' TEXT NOT NULL, 'bruto' TEXT NOT NULL, 'dto' TEXT NOT NULL, " +
                            "'base' TEXT NOT NULL, 'iva' TEXT NOT NULL, 'recargo' TEXT NOT NULL, 'total' TEXT NOT NULL, " +
                            "'flag' INTEGER NOT NULL, 'obs1' TEXT NOT NULL, 'obs2' TEXT NOT NULL)")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS 'LineasDiferidas' ('lineaDiferidaId' INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "'cabeceraId' INTEGER NOT NULL, 'serie' TEXT NOT NULL, 'numero' INTEGER NOT NULL, " +
                            "'fecha' TEXT NOT NULL, 'linea' INTEGER NOT NULL, 'articuloId' INTEGER NOT NULL, " +
                            "'codigo' TEXT NOT NULL, 'descripcion' TEXT NOT NULL, 'precio' TEXT NOT NULl, " +
                            "'cajas' TEXT NOT NULL, 'cantidad' TEXT NOT NULL, 'piezas' TEXT NOT NULL, 'importe' TEXT NOT NULL, " +
                            "'dto' TEXT NOT NULL, 'codigoIva' INTEGER NOT NULL, 'flag' INTEGER NOT NULL, 'flag3' INTEGER NOT NULL, " +
                            "'formatoId' INTEGER NOT NULL)")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS 'ContactosCltes' ('contactoClteId' INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "'clienteId' INTEGER NOT NULL, 'almacen' INTEGER NOT NULL, 'orden' INTEGER NOT NULL, " +
                            "'sucursal' INTEGER NOT NULL, 'nombre' TEXT NOT NULL, 'telefono1' TEXT NOT NULL, " +
                            "'telefono2' TEXT NOT NULL, 'obs1' TEXT NOT NULL, 'eMail' TEXT NOT NULL, 'flag' INTEGER NOT NULL, " +
                            "'estado' TEXT NOT NULL, 'numExport' INTEGER NOT NULL, 'matricula' TEXT NOT NULL)")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS 'Proveedores' ('proveedorId' INTEGER NOT NULL PRIMARY KEY, " +
                            "'nombre' TEXT NOT NULL, 'cif' TEXT NOT NULL)")

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS 'RatingGrupos' ('ratingGruposId' INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "'grupo' INTEGER NOT NULL, 'departamento' INTEGER NOT NULL, 'almacen' INTEGER NOT NULL, " +
                            "'clienteId' INTEGER NOT NULL, 'ramo' INTEGER NOT NULL, 'tarifaId' INTEGER NOT NULL, " +
                            "'inicio' TEXT NOT NULL, 'fin' TEXT NOT NULL, 'dto' TEXT NOT NULL)")

            }
        }

    }

}