package api_microservice_aesa_emision_vida.service;

import api_microservice_aesa_emision_vida.entity.GrupoGeneralesParametroEntity;
import api_microservice_aesa_emision_vida.repository.GrupoGeneralesParametroRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GrupoGeneralBancoService {

    private final GrupoGeneralesParametroRepository parametroRepository;

    // cache: BANCO -> (grupoId -> entidad)
    private final Map<String, Map<Integer, GrupoGeneralesParametroEntity>> cache = new ConcurrentHashMap<>();

    public GrupoGeneralBancoService(GrupoGeneralesParametroRepository parametroRepository) {
        this.parametroRepository = parametroRepository;
    }

    /** Normaliza clave de banco para usar en el cache */
    private static String norm(String banco) {
        return banco == null ? "" : banco.trim().toUpperCase(Locale.ROOT);
    }

    /** Carga en memoria TODOS los parámetros al arrancar (opcional si es grande). */
    @PostConstruct
    public void precargar() {
        cache.clear();
        List<GrupoGeneralesParametroEntity> todos = parametroRepository.findAll();
        for (GrupoGeneralesParametroEntity g : todos) {
            String bancoKey = norm(g.getBanco());
            cache.computeIfAbsent(bancoKey, k -> new ConcurrentHashMap<>())
                    .put(g.getGrupoId(), g);
        }
    }

    /** Carga solo los parámetros de un banco (si no están en cache o se pide explícitamente). */
    public void cargarParametrosPorBanco(String bancoNombre) {
        String bancoKey = norm(bancoNombre);
        List<GrupoGeneralesParametroEntity> parametros =
                parametroRepository.findAllByBancoNormalized(bancoKey);

        Map<Integer, GrupoGeneralesParametroEntity> grupoMap = new ConcurrentHashMap<>();
        for (GrupoGeneralesParametroEntity p : parametros) {
            grupoMap.put(p.getGrupoId(), p);
        }

        cache.put(bancoKey, grupoMap);
        System.out.println("Parámetros cargados para banco: " + bancoKey +
                " -> " + grupoMap.size() + " grupos");
    }

    /** Obtiene el mapa grupo->parametros; si no existe en cache, lo carga on-demand. */
    private Map<Integer, GrupoGeneralesParametroEntity> getMapaBanco(String banco) {
        String bancoKey = norm(banco);
        // carga perezosa si no fue precargado
        return cache.computeIfAbsent(bancoKey, key -> {
            List<GrupoGeneralesParametroEntity> parametros =
                    parametroRepository.findAllByBancoNormalized(bancoKey);
            Map<Integer, GrupoGeneralesParametroEntity> m = new ConcurrentHashMap<>();
            for (GrupoGeneralesParametroEntity p : parametros) {
                m.put(p.getGrupoId(), p);
            }
            return m;
        });
    }

    /** Devuelve la cuota configurada para el banco/grupo (0.0 si no hay dato). */
    public double obtenerMontoCuota(String banco, int grupoId) {
        Map<Integer, GrupoGeneralesParametroEntity> grupoMap = getMapaBanco(banco);
        GrupoGeneralesParametroEntity p = grupoMap.get(grupoId);
        return (p == null) ? 0.0 : p.getMontoCuota();
    }

    /** Devuelve el capital asegurado configurado para el banco/grupo (0.0 si no hay dato). */
    public double obtenerCapitalAsegurado(String banco, int grupoId) {
        Map<Integer, GrupoGeneralesParametroEntity> grupoMap = getMapaBanco(banco);
        GrupoGeneralesParametroEntity p = grupoMap.get(grupoId);
        return (p == null) ? 0.0 : p.getCapitalAsegurado();
    }
}
