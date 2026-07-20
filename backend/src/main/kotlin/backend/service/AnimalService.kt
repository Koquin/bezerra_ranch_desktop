package backend.service

import backend.dashboard.AnimalDashboard
import backend.dashboard.AnimalDashboardCalculator
import backend.model.Animal
import backend.repository.AnimalRepository

/** Serviços do rebanho armazenado exclusivamente no SQLite local. */
class AnimalService(private val repository: AnimalRepository) {
    fun criar(animal: Animal): Animal = repository.create(animal)
    fun buscarPorId(id: Long): Animal? = repository.findById(id)
    fun listar(): List<Animal> = repository.findAll()
    fun atualizar(id: Long, animal: Animal): Animal? = repository.update(id, animal)
    fun excluir(id: Long): Boolean = repository.delete(id)
    fun painel(): AnimalDashboard = AnimalDashboardCalculator.calculate(listar())
}
