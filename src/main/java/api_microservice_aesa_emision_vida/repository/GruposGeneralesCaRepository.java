package api_microservice_aesa_emision_vida.repository;

import api_microservice_aesa_emision_vida.entity.CategoriasCAEntity;
import api_microservice_aesa_emision_vida.entity.GrupoGeneralesCaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GruposGeneralesCaRepository extends JpaRepository<GrupoGeneralesCaEntity, Integer> {
    boolean existsByNombreIgnoreCase(String nombre);
    Optional<GrupoGeneralesCaEntity> findByNombreIgnoreCase(String nombre);
}
