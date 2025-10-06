package api_microservice_aesa_emision_vida.repository;

import api_microservice_aesa_emision_vida.entity.CategoriasCAEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoriasCaRepository extends JpaRepository<CategoriasCAEntity, Integer>{
    boolean existsByNombreIgnoreCase(String nombre);
    Optional<CategoriasCAEntity> findByNombreIgnoreCase(String nombre);

}
