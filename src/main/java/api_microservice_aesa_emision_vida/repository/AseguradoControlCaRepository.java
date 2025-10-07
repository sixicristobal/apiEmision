package api_microservice_aesa_emision_vida.repository;

import api_microservice_aesa_emision_vida.entity.AseguradoControlCaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AseguradoControlCaRepository extends JpaRepository<AseguradoControlCaEntity, Long> {
    boolean existsByCiAndMesVigenciaAndAnioVigencia(Integer ci, Integer mesVigencia, Integer anioVigencia);

    List<AseguradoControlCaEntity> findByEsTitular(boolean esTitular);

    List<AseguradoControlCaEntity> findByEsTitularTrueAndMesVigenciaAndAnioVigencia(int mesVigencia, int anioVigencia);

    @Modifying
    @Transactional
    @Query("""
            UPDATE AseguradoControlCaEntity ac
            SET ac.fechanacimiento = (
            SELECT p.fechaNac FROM PersonasCaEntity p
            WHERE p.ci = ac.ci
            )
            WHERE ac.fechanacimiento IS NULL
            """)
    void actualizarFechaNacimientoDesdePersonas();

    boolean existsByMesVigencia(int mesVigencia);

    boolean existsByMesVigenciaAndAnioVigencia(int mesVigencia, int anioVigencia);
}
