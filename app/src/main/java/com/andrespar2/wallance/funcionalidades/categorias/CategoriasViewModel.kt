package com.andrespar2.wallance.funcionalidades.categorias

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.wallance.nucleo.basedatos.entidades.Categoria
import com.andrespar2.wallance.nucleo.basedatos.entidades.TipoCategoria
import com.andrespar2.wallance.nucleo.basedatos.repositorios.ErrorCategoria
import com.andrespar2.wallance.nucleo.basedatos.repositorios.RepositorioCategorias
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado de la pantalla de categorías. */
data class EstadoCategorias(
    val cargando: Boolean = true,
    val ingresos: List<Categoria> = emptyList(),
    val gastos: List<Categoria> = emptyList()
)

@HiltViewModel
class CategoriasViewModel @Inject constructor(
    private val repositorio: RepositorioCategorias
) : ViewModel() {

    init {
        viewModelScope.launch { repositorio.siembraInicial() }
    }

    val estado: StateFlow<EstadoCategorias> = repositorio.todas()
        .map { lista ->
            EstadoCategorias(
                cargando = false,
                ingresos = lista.filter { it.tipo == TipoCategoria.INGRESO },
                gastos = lista.filter { it.tipo == TipoCategoria.GASTO }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoCategorias())

    var categoriaEnEdicion by mutableStateOf<Categoria?>(null)
        private set

    var errorFormulario by mutableStateOf<String?>(null)
        private set

    fun nuevaCategoria(tipo: TipoCategoria) {
        errorFormulario = null
        categoriaEnEdicion = Categoria(
            nombre = "",
            tipo = tipo,
            color = ColoresCategoria.random()
        )
    }

    fun editarCategoria(categoria: Categoria) {
        errorFormulario = null
        categoriaEnEdicion = categoria
    }

    fun cerrarFormulario() {
        categoriaEnEdicion = null
        errorFormulario = null
    }

    fun guardarCategoria(nombre: String, tipo: TipoCategoria, color: Long) {
        viewModelScope.launch {
            val base = categoriaEnEdicion ?: Categoria(nombre = nombre, tipo = tipo)
            try {
                repositorio.guardar(
                    base.copy(nombre = nombre.trim(), tipo = tipo, color = color)
                )
                categoriaEnEdicion = null
                errorFormulario = null
            } catch (error: ErrorCategoria) {
                errorFormulario = error.message
            } catch (error: Exception) {
                errorFormulario = "No se pudo guardar la categoría."
            }
        }
    }

    fun archivarCategoria(categoria: Categoria) {
        viewModelScope.launch { repositorio.archivar(categoria) }
    }

    fun restaurarCategoria(categoria: Categoria) {
        viewModelScope.launch { repositorio.restaurar(categoria) }
    }
}

/** Paleta de colores sugerida para categorías. */
object ColoresCategoria {
    val paleta: List<Long> = listOf(
        0xFFFCD535, 0xFF0ECB81, 0xFFF6465D, 0xFF4C9AFF,
        0xFFB37FEB, 0xFFFF9C6D, 0xFF36CFC9, 0xFF848E9C
    )

    fun random(): Long = paleta.random()
}
