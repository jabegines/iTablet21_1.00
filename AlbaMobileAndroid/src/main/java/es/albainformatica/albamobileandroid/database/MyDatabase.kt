package es.albainformatica.albamobileandroid.database

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import es.albainformatica.albamobileandroid.VERSION_BD
import es.albainformatica.albamobileandroid.dao.*
import es.albainformatica.albamobileandroid.dao.DocsCabPiesDao
import es.albainformatica.albamobileandroid.entity.*



@Database(entities=[AlmacenesEnt::class, ArtHabitualesEnt::class, ArticClasifEnt::class, ArticDatAdicEnt::class,
    ArticulosEnt::class, BusquedasEnt::class, CabDiferidasEnt::class, CabecerasEnt::class, ClasificadoresEnt::class,
    CargasEnt::class, CargasLineasEnt::class, CatalogoLineasEnt::class, ClientesEnt::class, CnfTarifasEnt::class,
    CobrosEnt::class, ConfiguracionEnt::class, ContactosCltesEnt::class, CostosArticulosEnt::class,
    DepartamentosEnt::class, DireccCltesEnt::class, DivisasEnt::class, DocsCabPiesEnt::class, DtosCltesEnt::class,
    DtosLineasEnt::class, DtosLinFrasEnt::class, EjerciciosEnt::class, EmpresasEnt::class, EstadDevolucEnt::class,
    FacturasEnt::class, FormasPagoEnt::class, FormatosEnt::class, FtosLineasEnt::class, FtosLinFrasEnt::class,
    GruposEnt::class, HcoCompSemMesEnt::class, HcoPorArticClteEnt::class, HistMesEnt::class, HistoricoEnt::class,
    HistRepreEnt::class, IvasEnt::class, LineasDifEnt::class, LineasEnt::class, LineasFrasEnt::class, LotesEnt::class,
    NotasCltesEnt::class, NumExportEnt::class, OfertasEnt::class, OftCantRangosEnt::class, OftVolRangosEnt::class,
    OftVolumenEnt::class, PendienteEnt::class, ProveedoresEnt::class, RatingArtEnt::class, RatingGruposEnt::class,
    RatingProvEnt::class, RegistroDeEventosEnt::class, RutasEnt::class, RuterosEnt::class, SaldosEnt::class,
    SeriesEnt::class, StockEnt::class, TarifasEnt::class, TempCltesEnt::class, TiposIncEnt::class, TmpHcoEnt::class,
    TrfFormatosEnt::class],
    version = VERSION_BD,
    exportSchema = true
)
abstract class MyDatabase: RoomDatabase() {

    abstract fun almacenesDao(): AlmacenDao
    abstract fun artHabitualesDao(): ArtHabitualesDao
    abstract fun articClasifDao(): ArticClasifDao
    abstract fun articDatAdicDao(): ArticDatAdicDao
    abstract fun articulosDao(): ArticulosDao
    abstract fun busquedasDao(): BusquedasDao
    abstract fun cabDiferidasDao(): CabDiferidasDao
    abstract fun cabecerasDao(): CabecerasDao
    abstract fun cargasDao(): CargasDao
    abstract fun cargasLineasDao(): CargasLineasDao
    abstract fun catalogoLineasDao(): CatalogoLineasDao
    abstract fun clasificadoresDao(): ClasificadoresDao
    abstract fun clientesDao(): ClientesDao
    abstract fun cnfTarifasDao(): CnfTarifasDao
    abstract fun cobrosDao(): CobrosDao
    abstract fun configuracionDao(): ConfiguracionDao
    abstract fun contactosCltesDao(): ContactosCltesDao
    abstract fun costosArticulosDao(): CostosArticulosDao
    abstract fun departamentosDao(): DepartamentosDao
    abstract fun direccCltesDao(): DireccCltesDao
    abstract fun divisasDao(): DivisasDao
    abstract fun docsCabPiesDao(): DocsCabPiesDao
    abstract fun dtosCltesDao(): DtosCltesDao
    abstract fun dtosLineasDao(): DtosLineasDao
    abstract fun dtosLinFrasDao(): DtosLinFrasDao
    abstract fun ejerciciosDao(): EjerciciosDao
    abstract fun empresasDao(): EmpresasDao
    abstract fun estadDevolucDao(): EstadDevolucDao
    abstract fun facturasDao(): FacturasDao
    abstract fun formasPagoDao(): FormasPagoDao
    abstract fun formatosDao(): FormatosDao
    abstract fun ftosLineasDao(): FtosLineasDao
    abstract fun ftosLinFrasDao(): FtosLinFrasDao
    abstract fun gruposDao(): GruposDao
    abstract fun hcoCompSemMesDao(): HcoCompSemMesDao
    abstract fun hcoPorArticClteDao(): HcoPorArticClteDao
    abstract fun histMesDao(): HistMesDao
    abstract fun historicoDao(): HistoricoDao
    abstract fun histRepreDao(): HistRepreDao
    abstract fun ivasDao(): IvasDao
    abstract fun lineasDifDao(): LineasDifDao
    abstract fun lineasDao(): LineasDao
    abstract fun lineasFrasDao(): LineasFrasDao
    abstract fun lotesDao(): LotesDao
    abstract fun notasCltesDao(): NotasCltesDao
    abstract fun numExportDao(): NumExportDao
    abstract fun ofertasDao(): OfertasDao
    abstract fun oftCantRangosDao(): OftCantRangosDao
    abstract fun oftVolRangosDao(): OftVolRangosDao
    abstract fun oftVolumenDao(): OftVolumenDao
    abstract fun pendienteDao(): PendienteDao
    abstract fun proveedoresDao(): ProveedoresDao
    abstract fun ratingArtDao(): RatingArtDao
    abstract fun ratingGruposDao(): RatingGruposDao
    abstract fun ratingProvDao(): RatingProvDao
    abstract fun regEventosDao(): RegistroDeEventosDao
    abstract fun rutasDao(): RutasDao
    abstract fun ruterosDao(): RuterosDao
    abstract fun saldosDao(): SaldosDao
    abstract fun seriesDao(): SeriesDao
    abstract fun stockDao(): StockDao
    abstract fun tarifasDao(): TarifasDao
    abstract fun tempCltesDao(): TempCltesDao
    abstract fun tiposIncDao(): TiposIncDao
    abstract fun tmpHcoDao(): TmpHcoDao
    abstract fun trfFormatosDao(): TrfFormatosDao



    companion object {
        var INSTANCE: MyDatabase? = null
        var queBDRoom: String = "ibsTablet00.db"

        fun getInstance(context: Context): MyDatabase? {
            if (INSTANCE == null) {
                synchronized(MyDatabase::class) {
                    INSTANCE = androidx.room.Room.databaseBuilder(
                        context.applicationContext,
                        MyDatabase::class.java, queBDRoom
                    )
                        .allowMainThreadQueries()
                        .addMigrations(MIGRATION_1_2)
                        .build()
                }
            }
            return INSTANCE
        }


        fun destroyInstance() {
            INSTANCE = null
        }


        private val MIGRATION_1_2: Migration = object: Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE 'RegistroDeEventos' ADD COLUMN 'numExport' INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE 'RegistroDeEventos' ADD COLUMN 'estado' TEXT NOT NULL DEFAULT ''")
            }
        }


    }

}