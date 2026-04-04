package com.hudsom.kotlinceapp.dados

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hudsom.kotlinceapp.model.Comunidade

@Database(entities = [Comunidade::class], version = 5)
abstract class BancoLocal : RoomDatabase() {
    abstract fun comunidadeDao(): ComunidadeDao

    companion object {
        @Volatile private var instancia: BancoLocal? = null

        fun obterInstancia(contexto: Context): BancoLocal =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    contexto.applicationContext, BancoLocal::class.java, "app_banco.db"
                ).fallbackToDestructiveMigration().build().also { instancia = it }
            }
    }
}
