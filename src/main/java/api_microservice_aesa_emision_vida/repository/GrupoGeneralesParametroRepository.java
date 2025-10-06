package api_microservice_aesa_emision_vida.repository;

import api_microservice_aesa_emision_vida.entity.GrupoGeneralesParametroEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrupoGeneralesParametroRepository extends JpaRepository<GrupoGeneralesParametroEntity, Long> {

    // Si "banco" en BD viene con espacios o mayúsculas mezcladas:
    @Query("""
           select p
             from GrupoGeneralesParametroEntity p
            where upper(trim(p.banco)) = upper(trim(:banco))
           """)
    List<GrupoGeneralesParametroEntity> findAllByBancoNormalized(String banco);

    // Si tus datos ya están prolijos, también podés usar este:
    List<GrupoGeneralesParametroEntity> findByBancoIgnoreCase(String banco);
}
