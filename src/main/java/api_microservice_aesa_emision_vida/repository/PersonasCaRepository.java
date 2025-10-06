package api_microservice_aesa_emision_vida.repository;

import api_microservice_aesa_emision_vida.entity.PersonasCaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonasCaRepository extends JpaRepository<PersonasCaEntity, Long> {
    Optional<PersonasCaEntity> findByCi(Integer ci);
}
