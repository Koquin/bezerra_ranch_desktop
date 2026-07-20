package backend.controller

import backend.database.SQLiteDatabase
import backend.dashboard.AnimalDashboard
import backend.model.Animal
import backend.repository.AnimalRepository
import backend.service.AnimalService
import backend.service.CloudAnimalReadService

/** Ponto de entrada do frontend para o CRUD offline e para o painel em nuvem. */
class AnimalController(
    private val localService: AnimalService = AnimalService(AnimalRepository(SQLiteDatabase())),
    private val cloudService: CloudAnimalReadService = CloudAnimalReadService()
) {
    fun criar(animal: Animal): Animal = localService.criar(animal)
    fun buscarPorId(id: Long): Animal? = localService.buscarPorId(id)
    fun listar(): List<Animal> = localService.listar()
    fun atualizar(id: Long, animal: Animal): Animal? = localService.atualizar(id, animal)
    fun excluir(id: Long): Boolean = localService.excluir(id)

    /** Dados do rebanho local, para utilização offline. */
    fun painelLocal(): AnimalDashboard = localService.painel()

    /** Dados atuais da nuvem para a tela principal; não persiste cópia local. */
    fun painelNuvem(): AnimalDashboard = cloudService.painel()
}
