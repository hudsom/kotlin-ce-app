package com.hudsom.kotlinceapp.dados

import androidx.room.*
import com.hudsom.kotlinceapp.model.Comunidade

@Dao
interface ComunidadeDao {
    @Query("SELECT * FROM comunidades")
    fun listarTodas(): List<Comunidade>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun salvar(comunidade: Comunidade)

    @Query("DELETE FROM comunidades WHERE id = :id")
    fun excluir(id: String)

    @Query("SELECT * FROM comunidades WHERE id = :id LIMIT 1")
    fun buscarPorId(id: String): Comunidade?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun salvarTodas(comunidades: List<Comunidade>)

    @Query("DELETE FROM comunidades")
    fun limparTodas()
}
