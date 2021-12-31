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





@Database(entities=[AlmacenesEnt::class, ArtHabitualesEnt::class, ArticDatAdicEnt::class, ArticulosEnt::class,
    BusquedasEnt::class, CabDiferidasEnt::class, ClasificadoresEnt::class, CargasEnt::class, CargasLineasEnt::class,
    CatalogoLineasEnt::class, CobrosEnt::class, ConfiguracionEnt::class, ContactosCltesEnt::class,
    CostosArticulosEnt::class, DepartamentosEnt::class, DivisasEnt::class, DocsCabPiesEnt::class,
    EjerciciosEnt::class, EmpresasEnt::class, FormasPagoEnt::class, FormatosEnt::class, GruposEnt::class,
    HistRepreEnt::class, IvasEnt::class, LineasDifEnt::class, LotesEnt::class, NotasCltesEnt::class,
    NumExportEnt::class, OfertasEnt::class, OftCantRangosEnt::class, OftVolRangosEnt::class, OftVolumenEnt::class,
    PendienteEnt::class, ProveedoresEnt::class, RatingArtEnt::class, RatingGruposEnt::class, RatingProvEnt::class,
    RutasEnt::class, RuterosEnt::class, SaldosEnt::class, SeriesEnt::class, StockEnt::class, TarifasEnt::class,
    TempCltesEnt::class, TiposIncEnt::class, TrfFormatosEnt::class],
    version = VERSION_BD_ROOM,
    exportSchema = true
)
abstract class MyDatabase: RoomDatabase() {

    abstract fun almacenesDao(): AlmacenDao
    abstract fun artHabitualesDao(): ArtHabitualesDao
    abstract fun articDatAdicDao(): ArticDatAdicDao
    abstract fun articulosDao(): ArticulosDao
    abstract fun busquedasDao(): BusquedasDao
    abstract fun cabDiferidasDao(): CabDiferidasDao
    abstract fun cargasDao(): CargasDao
    abstract fun cargasLineas(): CargasLineasDao
    abstract fun catalogoLineasDao(): CatalogoLineasDao
    abstract fun clasificadoresDao(): ClasificadoresDao
    abstract fun cobrosDao(): CobrosDao
    abstract fun configuracionDao(): ConfiguracionDao
    abstract fun contactosCltesDao(): ContactosCltesDao
    abstract fun costosArticulosDao(): CostosArticulosDao
    abstract fun departamentosDao(): DepartamentosDao
    abstract fun divisasDao(): DivisasDao
    abstract fun docsCabPiesDao(): DocsCabPiesDao
    abstract fun ejerciciosDao(): EjerciciosDao
    abstract fun empresasDao(): EmpresasDao
    abstract fun formasPagoDao(): FormasPagoDao
    abstract fun formatosDao(): FormatosDao
    abstract fun gruposDao(): GruposDao
    abstract fun histRepreDao(): HistRepreDao
    abstract fun ivasDao(): IvasDao
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
    abstract fun ratingArtDao(): RatingArtDao
    abstract fun ratingGruposDao(): RatingGruposDao
    abstract fun ratingProvDao(): RatingProvDao
    abstract fun rutasDao(): RutasDao
    abstract fun ruterosDao(): RuterosDao
    abstract fun saldosDao(): SaldosDao
    abstract fun seriesDao(): SeriesDao
    abstract fun stockDao(): StockDao
    abstract fun tarifasDao(): TarifasDao
    abstract fun  tempCltesDao(): TempCltesDao
    abstract fun tiposIncDao(): TiposIncDao
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
                        .build()
                }
            }
            return INSTANCE
        }


        fun destroyInstance() {
            INSTANCE = null
        }

    }

}