package api_microservice_aesa_emision_vida.controller;

import api_microservice_aesa_emision_vida.entity.AseguradoControlCaEntity;
import api_microservice_aesa_emision_vida.entity.AuditoriaCargaEntity;
import api_microservice_aesa_emision_vida.repository.AuditoriaCargaRepository;
import api_microservice_aesa_emision_vida.service.ControlAseguradoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/carga")
public class ControlAseguradoController {

    @Autowired
    private ControlAseguradoService controlAseguradoService;


    @PostMapping("/cargar-excel")
    public ResponseEntity<?> cargarExcel(@RequestParam("archivo") MultipartFile archivo,
                                         @RequestParam("banco") String banco) {
        if (archivo.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "No se envió un archivo."
            ));
        }

        try {
            // Procesar Excel
            controlAseguradoService.procesarExcel(archivo, banco);

            // Obtener archivo generado
            FileSystemResource file = controlAseguradoService.getArchivoGenerado();

            if (file == null || !file.exists()) {
                return ResponseEntity.status(500).body(Map.of(
                        "status", "error",
                        "message", "El archivo no fue generado correctamente."
                ));
            }

            // Devolver el archivo generado
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getFilename())
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(new InputStreamResource(file.getInputStream()));

        } catch (IllegalStateException e) {
            // Caso mes ya emitido
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            // Errores generales
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Error al procesar el archivo: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/auditoria")
    public ResponseEntity<List<AuditoriaCargaEntity>> obtenerAuditoria() {
        List<AuditoriaCargaEntity> lista = controlAseguradoService.obtenerAuditoria();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/listado_asegurados")
    public ResponseEntity<List<AseguradoControlCaEntity>> getAllAsegurados() {
        List<AseguradoControlCaEntity> asegurados = controlAseguradoService.obtenerTodos();

        return  ResponseEntity.ok(asegurados);
    }




//    @PostMapping("/test")
//    public ResponseEntity<String> testEndpoind() {
//        return ResponseEntity.ok("Oikoite la API.");
//    }

}