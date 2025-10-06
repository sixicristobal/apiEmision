package api_microservice_aesa_emision_vida.repository;

import api_microservice_aesa_emision_vida.entity.PrepagaCaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrepagaCaRepository extends JpaRepository<PrepagaCaEntity, Integer> {
    boolean existsByNombreIgnoreCase(String nombre);

    Optional<PrepagaCaEntity> findByNombreIgnoreCase(String nombre);
}
