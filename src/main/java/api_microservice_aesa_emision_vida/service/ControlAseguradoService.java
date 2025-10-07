package api_microservice_aesa_emision_vida.service;

import api_microservice_aesa_emision_vida.dto.DatosPersonasDto;
import api_microservice_aesa_emision_vida.entity.*;
import api_microservice_aesa_emision_vida.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import api_microservice_aesa_emision_vida.entity.GrupoGeneralesParametroEntity;

@Service
public class ControlAseguradoService {

    private static final Logger log = LoggerFactory.getLogger(ControlAseguradoService.class);
    private final Map<String, Integer> edadesPadres = new HashMap<>();
    private final Map<Integer, AseguradoControlCaEntity> titularesPorCi = new HashMap<>();
//  private final Map<Integer, ResultadoCuota> cuotasPendientePorTitular = new HashMap<>();
    private static final String ExcesoGastosMedicos = "120000000"; // Setea el Monto del Capital Asegurado en Exceso de Gastosmedicos
    private final Map<Integer, List<ResultadoCuota>> cuotasPorTitular = new HashMap<>();
    private Integer ultimoCiTitularValido = null;
    private final AtomicInteger mesVigenciaExcel = new AtomicInteger(-1);
    private final AtomicInteger anioVigenciaExcel = new AtomicInteger(-1);
    private boolean verificacionMesRealizada = false;

    @Autowired private AseguradoControlCaRepository aseguradoControlCaRepository;
    @Autowired private CategoriasCaRepository categoriasCaRepository;
    @Autowired private GruposGeneralesCaRepository gruposGeneralesCaRepository;
    @Autowired private PrepagaCaRepository prepagaCaRepository;
    @Autowired private PersonasCaRepository personasCaRepository;
    @Autowired private AuditoriaCargaRepository auditoriaCargaRepository;
    @Autowired private GrupoGeneralBancoService grupoGeneralBancoService;

    private static class ResultadoCuota {

        private double cuotaTotal, banco, funcionario;

        public ResultadoCuota(double cuota, double banco, double funcionario) {
            this.cuotaTotal = cuota;
            this.banco = banco;
            this.funcionario = funcionario;
        }

        public double getCuotaTotal() { return cuotaTotal; }
        public void setCuotaTotal(double cuotaTotal) { this.cuotaTotal = cuotaTotal; }
        public double getBanco() { return banco; }
        public void setBanco(double banco) { this.banco = banco; }
        public double getFuncionario() { return funcionario; }
        public void setFuncionario(double funcionario) { this.funcionario = funcionario; }
    }

    private static final String HOJA_DETALLE_GRAL = "DETALLE GRAL";

    @Transactional
    public void completarFechasNacimientoFaltantes() {
        aseguradoControlCaRepository.actualizarFechaNacimientoDesdePersonas();
    }

    private final List<String> errores = new ArrayList<>();

    public void procesarExcel(MultipartFile archivo, String bancoActual) throws Exception {
        Objects.requireNonNull(archivo, "El archivo no puede ser nulo");
        inicializarEstado();

        grupoGeneralBancoService.cargarParametrosPorBanco(bancoActual);

        try (InputStream inputStream = archivo.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet hoja = workbook.getSheet(HOJA_DETALLE_GRAL);
            if (hoja == null)
                throw new Exception("No se encontró la hoja '" + HOJA_DETALLE_GRAL + "' en el archivo");

            List<Row> filas = new ArrayList<>();
            boolean primeraFila = true;

            for (Row fila : hoja) {
                if (primeraFila) {
                    primeraFila = false;
                    continue;
                }
                filas.add(fila);
            }

            System.out.println("Total filas procesadas: " + filas.size());

            // Primero procesamos SOLO la primera fila para obtener el mes de vigencia
            if (!filas.isEmpty()) {
                procesarFila(filas.get(0), 1, bancoActual); // procesarFila ya setea mesVigenciaExcel
            }

            // 🔹 Verificamos si ya existe ese mes en BD
//            if (mesVigenciaExcel.get() != -1) {
//                boolean yaExiste = aseguradoControlCaRepository.existsByMesVigencia(mesVigenciaExcel.get());
//                if (yaExiste) {
//                    throw new IllegalStateException("Ya existe una carga para el mes " + mesVigenciaExcel.get());
//                }
//            }

            // Procesamos el resto de filas normalmente
            int nroFila = 1;
            for (Row fila : filas) {
                if (nroFila != 1) { // ya procesamos la primera
                    procesarFila(fila, nroFila, bancoActual);
                }
                nroFila++;
            }

            // Ajuste de cuotas y guardado
            for (AseguradoControlCaEntity titular : titularesPorCi.values()) {
                double cuota = titular.getCuotaTotal();
                double banco = titular.getAbsorbeBanco();
                double funcionario = titular.getAbsorbeFuncionario();

                if (cuotasPorTitular.containsKey(titular.getCi())) {
                    for (ResultadoCuota adicional : cuotasPorTitular.get(titular.getCi())) {
                        cuota += adicional.getCuotaTotal();
                        banco += adicional.getBanco();
                        funcionario += adicional.getFuncionario();
                    }
                }

                titular.setCuotaTotal(cuota);
                titular.setAbsorbeBanco(banco);
                titular.setAbsorbeFuncionario(funcionario);
                aseguradoControlCaRepository.save(titular);
            }

            System.out.println("Total errores: " + errores.size());
            getErrores().forEach(System.out::println);

        } catch (Exception e) {
            errores.add("Error al procesar el archivo: " + e.getMessage());
            throw e;

        } finally {
            if (errores.isEmpty() || !errores.get(0).startsWith("Error al procesar el archivo")) {
                completarFechasNacimientoFaltantes();

                for (AseguradoControlCaEntity titular : aseguradoControlCaRepository.findByEsTitular(true)) {
                    System.out.println("TITULAR " + titular.getCi() + " CUOTA TOTAL: " + titular.getCuotaTotal());
                }

                try {
                    if (mesVigenciaExcel.get() > 0 && anioVigenciaExcel.get() > 0) {
                        generarTxtExport(mesVigenciaExcel.get(), anioVigenciaExcel.get());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    errores.add("Error al generar TXT: " + ex.getMessage());
                }
            }
        }
    }


//    public void procesarExcel(MultipartFile archivo, String bancoActual) throws IOException {
//        Objects.requireNonNull(archivo, "El archivo no puede ser nulo");
//        errores.clear();
//        inicializarEstado(); // Asegura que el estado se limpia para cada nuevo procesamiento
//        this.mesVigenciaExcel.set(-1); //  Resetea el valor anterior
//
//        grupoGeneralBancoService.cargarParametrosPorBanco(bancoActual);
//
////        System.out.println("🔍 bancoActual recibido: [" + bancoActual + "]");
//
//        try (InputStream inputStream = archivo.getInputStream();
//             Workbook workbook = new XSSFWorkbook(inputStream)) {
//
//            Sheet hoja = workbook.getSheet(HOJA_DETALLE_GRAL);
//            if (hoja == null)
//                throw new Exception("No se encontró la hoja '" + HOJA_DETALLE_GRAL + "' en el archivo");
//
//            List<Row> filas = new ArrayList<>();
//            boolean primeraFila = true;
//
//            for (Row fila : hoja) {
//                if (primeraFila) {
//                    primeraFila = false;
//                    continue;
//                }
//                filas.add(fila);
//            }
//
//            System.out.println("Total filas procesadas: " + filas.size());
//
//            // Procesa las filas primero para poblar titularesPorCi y cuotasPorTitular
//            int nroFila = 1; // Comienza a contar desde 1 para las filas de datos reales
//            for (Row fila : filas) {
//                procesarFila(fila, nroFila++, bancoActual);
//            }
//
//            // Esto es crucial porque un adherente podría ser procesado antes que su titular en la hoja de Excel
//            for (AseguradoControlCaEntity titular : titularesPorCi.values()) {
//                double cuota = titular.getCuotaTotal();
//                double banco = titular.getAbsorbeBanco();
//                double funcionario = titular.getAbsorbeFuncionario();
//
//                if (cuotasPorTitular.containsKey(titular.getCi())) {
//                    for (ResultadoCuota adicional : cuotasPorTitular.get(titular.getCi())) {
//                        cuota += adicional.getCuotaTotal();
//                        banco += adicional.getBanco();
//                        funcionario += adicional.getFuncionario();
//                    }
//                }
//
//                titular.setCuotaTotal(cuota);
//                titular.setAbsorbeBanco(banco);
//                titular.setAbsorbeFuncionario(funcionario);
//                aseguradoControlCaRepository.save(titular); // Guarda el titular actualizado
//            }
//
//            System.out.println("Total errores: " + errores.size());
//            getErrores().forEach(System.out::println);
//
//        } catch (Exception e) {
//            errores.add("Error al procesar el archivo: " + e.getMessage());
//            throw e;
//
//        } finally {
//            if (errores.isEmpty() || !errores.get(0).startsWith("Error al procesar el archivo")) {
//                completarFechasNacimientoFaltantes();
//
//                for (AseguradoControlCaEntity titular : aseguradoControlCaRepository.findByEsTitular(true)) {
//                    System.out.println("TITULAR " + titular.getCi() + " CUOTA TOTAL: " + titular.getCuotaTotal());
//                }
//
//                try {
//                    generarTxtExport(); // ya funciona gracias al this.mesVigenciaExcel global
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                    errores.add("Error al generar TXT: " + ex.getMessage());
//                }
//            }
//        }
//    }

    private void procesarFila(Row fila, int nroFila, String bancoActual) {
        try {
            int ciTitular = getIntValue(fila.getCell(1)); // columna B
            int ci = getIntValue(fila.getCell(8));        // columna I

            String nombreCompleto = getStringValue(fila.getCell(4)); // columna E
            String[] partes = nombreCompleto.split(",", 2);
            String apellido = partes.length > 0 ? partes[0].trim() : "";
            String nombres = partes.length > 1 ? partes[1].trim() : "";

            LocalDate fechaNac = getDateValue(fila.getCell(9)); // columna J
            int edadExcel = getIntValue(fila.getCell(13));      // columna N
            int edadCalculada = calcularEdadDesdeFechaNac(fechaNac);
            int edad = (edadExcel <= 0) ? edadCalculada : edadExcel;

            if (fechaNac == null && edadExcel <= 0) {
                errores.add("Fila " + nroFila + ": Fecha de nacimiento y edad no proporcionadas. No se puede calcular la edad.");
                return;
            }
            if (edadExcel > 0 && Math.abs(edadExcel - edadCalculada) > 1 && fechaNac != null) {
                errores.add("Fila " + nroFila + ": La edad (" + edadExcel + ") no coincide con la fecha de nacimiento (" + fechaNac + "). Edad calculada: " + edadCalculada);
            }

            double cuotaInput = getDoubleValue(fila.getCell(17));       // columna R
            double bancoInput = getDoubleValue(fila.getCell(18));       // columna S
            double funcionarioInput = getDoubleValue(fila.getCell(19)); // columna T

            Integer categoriaId = getIntValue(fila.getCell(6)); // columna F
            if (categoriaId == null || categoriaId == 0) {
                System.out.println("ID de categoría vacío o cero en fila " + nroFila + ". Asignando 10 por defecto.");
                categoriaId = 9;
            }

            Optional<CategoriasCAEntity> categoriaOpt = categoriasCaRepository.findById(categoriaId);
            if (categoriaOpt.isEmpty()) {
                errores.add("Fila " + nroFila + ": ID de categoría no válido -> " + categoriaId);
                return;
            }
            CategoriasCAEntity categoriaEntity = categoriaOpt.get();
            String categoriaNombre = categoriaEntity.getNombre();

            if ("TITULAR".equalsIgnoreCase(categoriaNombre) && ci > 0) {
                ultimoCiTitularValido = ci;
            }
            if (ciTitular == 0 && ultimoCiTitularValido != null) {
                System.out.println("🛠️ Asignando último titular válido al CI " + ci + ": " + ultimoCiTitularValido);
                ciTitular = ultimoCiTitularValido;
            }

            int grupoId = getIntValue(fila.getCell(15)); // columna O
            if (grupoId == 0) {
                System.out.println("⚠️ Fila " + nroFila + ": ID de grupo es 0, se asigna 10 por defecto.");
                grupoId = 10;
            }

            Optional<GrupoGeneralesCaEntity> grupoOpt = gruposGeneralesCaRepository.findById(grupoId);
            if (grupoOpt.isEmpty()) {
                errores.add("Fila " + nroFila + ": ID de grupo no válido -> " + grupoId);
                return;
            }
            GrupoGeneralesCaEntity grupoEntity = grupoOpt.get();
            String grupoNombre = grupoEntity.getNombre();

            double montoGrupo = grupoGeneralBancoService.obtenerMontoCuota(bancoActual, grupoId);

            ResultadoCuota resultadoCuotaFila = new ResultadoCuota(0.0, 0.0, 0.0);

            aplicarSolteroSolos(categoriaEntity, grupoEntity, resultadoCuotaFila);
            aplicarLogicaSolteroConPadres(categoriaEntity, grupoEntity, edad, resultadoCuotaFila);
            aplicarGrupoFamiliar(categoriaEntity, grupoEntity, edad, resultadoCuotaFila);
            aplicarLogicaOtrosAdherentes(categoriaEntity, grupoEntity, edad, resultadoCuotaFila);
            aplicarMayores64OtrosAdherentes(categoriaEntity, grupoEntity, edad, resultadoCuotaFila);

            String prepaga = getStringValue(fila.getCell(7));        // columna H
            LocalDate fechaInicioInclusion = getDateValue(fila.getCell(10)); // columna K
            String emails = getStringValue(fila.getCell(22));        // columna W

            LocalDate mesVig = getDateValue(fila.getCell(21)); // col V/U

// Fallbacks
            if (mesVig == null) {
                mesVig = parseDateLoose(getStringValue(fila.getCell(21))); // <-- aquí el fix
            }
            if (mesVig == null && fechaInicioInclusion != null) {
                mesVig = fechaInicioInclusion.withDayOfMonth(1);
            }
            if (mesVig == null) {
                throw new IllegalStateException("Fila " + nroFila + " sin Mes de Vigencia (columna V/U). No se puede guardar anio_vigencia NOT NULL.");
            }

// >>> DERIVAR SIEMPRE DE mesVig <<<
            int mesVigencia  = mesVig.getMonthValue();
            int anioVigencia = mesVig.getYear();

// Evitar duplicados por (CI, mes, año)
            if (aseguradoControlCaRepository
                    .existsByCiAndMesVigenciaAndAnioVigencia(ci, mesVigencia, anioVigencia)) {
                errores.add("Fila " + nroFila + ": asegurado con CI " + ci + " ya procesado para " + mesVigencia + "/" + anioVigencia);
                return;
            }

            // Validación dura: no persistir si no hay mes de vigencia
            if (mesVig == null) {
                throw new IllegalStateException("Fila " + nroFila + " sin Mes de Vigencia (columna V/U). No se puede guardar anio_vigencia NOT NULL.");
            }


            //     Cell cellMesVigencia = fila.getCell(21);                 // columna V

//            int mesVigencia;
//
//            if (cellMesVigencia != null && cellMesVigencia.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cellMesVigencia)) {
//                mesVigencia = cellMesVigencia.getLocalDateTimeCellValue().toLocalDate().getMonthValue();
//            } else if (cellMesVigencia != null && cellMesVigencia.getCellType() == CellType.NUMERIC) {
//
//                mesVigencia = (int) cellMesVigencia.getNumericCellValue();
//                if (mesVigencia < 1 || mesVigencia > 12) {
//                    errores.add("Fila " + nroFila + ": El mes de vigencia (" + mesVigencia + ") no es un valor válido (1-12).");
//                    return;
//                }
//            } else {
//                errores.add("Fila " + nroFila + ": El campo de mes de vigencia no contiene una fecha o número válido.");
//                return;
//            }

            registrarMesYAnioVigencia(mesVigencia, anioVigencia, nroFila);

            if (!verificacionMesRealizada && mesVigenciaExcel.get() != -1 && anioVigenciaExcel.get() != -1) {
                if (aseguradoControlCaRepository.existsByMesVigenciaAndAnioVigencia(mesVigenciaExcel.get(), anioVigenciaExcel.get())) {
                    throw new IllegalStateException("Ya existe una carga para el mes " + mesVigenciaExcel.get() + "/" + anioVigenciaExcel.get());
                }
                verificacionMesRealizada = true;
            }

            if (!validarGrupoExiste(grupoNombre, nroFila)) return;
            if (!validarCategoriaExiste(categoriaNombre, nroFila)) return;
            if (!validarPrepaga(prepaga, nroFila)) return;
            if (!validarEdadVsFechaNac(edad, fechaNac, nroFila)) return;

            DatosPersonasDto dto = new DatosPersonasDto();
            dto.setCi(ci);
            dto.setEdadActual(edad);
            dto.setCuotaTotal(resultadoCuotaFila.getCuotaTotal());
            dto.setAbsorbeBanco(resultadoCuotaFila.getBanco());
            dto.setAbsorbeFuncionario(resultadoCuotaFila.getFuncionario());
            dto.setCategoriaNombre(categoriaNombre);
            dto.setGrupoNombre(grupoNombre);
            dto.setPrepagaNombre(prepaga);
            dto.setNombre(nombres);
            dto.setApellido(apellido);
            dto.setFechaInicioInclusion(fechaInicioInclusion);
            dto.setFechaNacimiento(fechaNac);
            dto.setMail(emails);
            dto.setCiTitular(ciTitular);
            dto.setMesVigencia(mesVigencia);
            dto.setAnioVigencia(anioVigencia);
            dto.setFechaCreacion(LocalDate.now());

            double capital = grupoGeneralBancoService.obtenerCapitalAsegurado(bancoActual, grupoId);
            dto.setCapitalAsegurado(capital);

            if (!esTitular(dto)) {
                ResultadoCuota cuotaAdicional = new ResultadoCuota(
                        dto.getCuotaTotal(),
                        dto.getAbsorbeBanco(),
                        dto.getAbsorbeFuncionario()
                );
                if (dto.getCiTitular() > 0) {
                    cuotasPorTitular.computeIfAbsent(dto.getCiTitular(), k -> new ArrayList<>()).add(cuotaAdicional);
                }
            }

            guardarAsegurado(dto);

        } catch (Exception e) {
            errores.add("Fila " + nroFila + ": error -> " + e.getMessage());
            System.err.println("Error procesando fila " + nroFila + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void inicializarEstado() {
        errores.clear();
        edadesPadres.clear();
        cuotasPorTitular.clear();
        titularesPorCi.clear();
        mesVigenciaExcel.set(-1);
        anioVigenciaExcel.set(-1);
        verificacionMesRealizada = false;
    }

    private void registrarMesYAnioVigencia(int mesVigencia, int anioVigencia, int fila) {
        if (mesVigenciaExcel.compareAndSet(-1, mesVigencia)) {
            anioVigenciaExcel.set(anioVigencia);
            return;
        }

        anioVigenciaExcel.compareAndSet(-1, anioVigencia);

        if (mesVigenciaExcel.get() != mesVigencia || anioVigenciaExcel.get() != anioVigencia) {
            String mensaje = "Fila " + fila + ": mes/año de vigencia " + mesVigencia + "/" + anioVigencia +
                    " no coincide con el declarado previamente " + mesVigenciaExcel.get() + "/" + anioVigenciaExcel.get();
            errores.add(mensaje);
            throw new IllegalStateException(mensaje);
        }
    }

    private void aplicarSolteroSolos(CategoriasCAEntity categoriaEntity, GrupoGeneralesCaEntity grupoEntity, ResultadoCuota resultado) {
        // La comparación se hace directamente con el nombre de la entidad, usando equalsIgnoreCase
        if (!"Soltero Solos".equalsIgnoreCase(grupoEntity.getNombre())) return;

        double baseTitular = grupoEntity.getMontoCuota(); // Ya tenemos la entidad, usamos su monto

        if ("TITULAR".equalsIgnoreCase(categoriaEntity.getNombre())) {
            resultado.setCuotaTotal(baseTitular);
            resultado.setBanco(baseTitular * 0.40);
            resultado.setFuncionario(baseTitular * 0.60);
        } else {
            // Para otras categorías en el grupo "Soltero Solos", la cuota es 0
            resultado.setCuotaTotal(0.0);
            resultado.setBanco(0.0);
            resultado.setFuncionario(0.0);
        }
    }

    private void aplicarLogicaSolteroConPadres(CategoriasCAEntity categoriaEntity, GrupoGeneralesCaEntity grupoEntity, int edad, ResultadoCuota resultado) {
        if (!"Soltero + Padres".equalsIgnoreCase(grupoEntity.getNombre())) return;

        double baseTitular = grupoEntity.getMontoCuota(); // Ya tenemos la entidad, usamos su monto

        if ("PADRE".equalsIgnoreCase(categoriaEntity.getNombre()) || "MADRE".equalsIgnoreCase(categoriaEntity.getNombre())) {
            edadesPadres.put(categoriaEntity.getNombre().toUpperCase(), edad);
            resultado.setCuotaTotal(0.0);
            resultado.setBanco(0.0);
            resultado.setFuncionario(0.0);
            return;
        }

        if ("TITULAR".equalsIgnoreCase(categoriaEntity.getNombre())) {
            // obtenerMontoCuotaPorEdad sigue usando findByNombreIgnoreCase, por lo que los nombres pasados deben ser consistentes
            double adicionalPadre = obtenerMontoCuotaPorEdad(
                    "Padre/Madre Mayores de 64 años", "Padre/Madre Menores de 64 años",
                    edadesPadres.get("PADRE")
            );
            double adicionalMadre = obtenerMontoCuotaPorEdad(
                    "Padre/Madre Mayores de 64 años", "Padre/Madre Menores de 64 años",
                    edadesPadres.get("MADRE")
            );

            double total = baseTitular + adicionalPadre + adicionalMadre;
            double banco = baseTitular * 0.40;
            double funcionario = baseTitular * 0.60 + adicionalPadre + adicionalMadre;

            resultado.setCuotaTotal(total);
            resultado.setBanco(banco);
            resultado.setFuncionario(funcionario);
        }
    }

    private PersonasCaEntity buscarOCrearPersonas(Integer ci, String nombre,String apellido, LocalDate fechaNac, String emails) {
        PersonasCaEntity persona = personasCaRepository.findByCi(ci).orElseGet(() -> {
            PersonasCaEntity nueva = new PersonasCaEntity();
            nueva.setCi(ci);
            nueva.setFechaCreate(LocalDate.now());
            return nueva;
        });

        persona.setNombre(nombre);
        persona.setApellido(apellido);


        // Siempre actualiza los datos esenciales
//        persona.setNombre(nombre);

        if (fechaNac != null) {
            persona.setFechaNac(fechaNac);
        }

        if (emails != null && !emails.isBlank()) {
            persona.setEmails(emails);
        }

        return personasCaRepository.save(persona);
    }

    private int calcularEdadDesdeFechaNac(LocalDate fechaNac) {
        if (fechaNac == null) return 0;
        LocalDate hoy = LocalDate.now();
        return hoy.getYear() - fechaNac.getYear() - ((hoy.getDayOfYear() < fechaNac.getDayOfYear()) ? 1 : 0);
    }

    private double obtenerMontoCuotaPorEdad(String grupoMayor, String grupoMenor, Integer edad) {
        if (edad == null) return 0.0;
        if (edad > 64 && grupoMayor != null) {
            return gruposGeneralesCaRepository.findByNombreIgnoreCase(grupoMayor)
                    .map(GrupoGeneralesCaEntity::getMontoCuota).orElse(0.0);
        } else if (grupoMenor != null) {
            return gruposGeneralesCaRepository.findByNombreIgnoreCase(grupoMenor)
                    .map(GrupoGeneralesCaEntity::getMontoCuota).orElse(0.0);
        }
        return 0.0;
    }

    private LocalDate getDateValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            } else if (cell.getCellType() == CellType.STRING) {
                // Intenta analizar cadenas de fecha si están en un formato común, por ejemplo, "YYYY-MM-DD" o "DD/MM/YYYY"
                try {
                    return LocalDate.parse(cell.getStringCellValue(), DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (Exception e) {
                    // Intenta otro formato común
                    try {
                        return LocalDate.parse(cell.getStringCellValue(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    } catch (Exception ex) {
                        // Registra el error o devuelve nulo si el análisis falla
                        System.err.println("No se pudo analizar la cadena de fecha: " + cell.getStringCellValue() + " - " + ex.getMessage());
                        return null;
                    }
                }
            }
        } catch (IllegalStateException e) {
            // Maneja casos en los que se llama a getLocalDateTimeCellValue en un tipo de celda que no es de fecha
            System.err.println("IllegalStateException al obtener el valor de fecha de la celda: " + cell.getStringCellValue() + " - " + e.getMessage());
            return null;
        }
        return null;
    }

    private LocalDate parseDateLoose(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try { return LocalDate.parse(s); } catch (Exception ignore) {}
        try { return LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")); } catch (Exception ignore) {}
        try { return LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern("M/d/yyyy")); } catch (Exception ignore) {}
        return null;
    }


    private int getIntValue(Cell cell) {
        if (cell == null) return 0;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String valor = cell.getStringCellValue().trim().toUpperCase();

                // Detecta texto común para cédula inválida
                if (valor.matches(".*SIN.*CI.*") || valor.matches(".*NO.*CUENTA.*") || valor.matches(".*A.*DECLARAR.*")  || valor.matches(".*S.*CI.*") || valor.matches(".*S/C.*") || valor.equals("S/CI")) {
                    return 0; // Asignar valor 0 explícitamente
                }

                return Integer.parseInt(valor); // Si es número en string
            }
        } catch (NumberFormatException e) {
            System.err.println("holaaaa No se pudo analizar la cadena a entero: " + cell.getStringCellValue());
            return 0;
        }
        return 0;
    }

    private double getDoubleValue(Cell cell) {
        if (cell == null) return 0.0;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                try {
                    return Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    System.err.println("No se pudo analizar la cadena a double: " + cell.getStringCellValue());
                    return 0.0;
                }
            }
            return 0.0;
        } catch (Exception e) {
            System.err.println("Error al leer el valor double de la celda: " + e.getMessage());
            return 0.0;
        }
    }

    private String getStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue()); // Convierte numérico a string, asumiendo representación entera para IDs
            case FORMULA -> { // Maneja celdas de fórmula
                try {
                    // Intenta evaluar la fórmula como string
                    yield cell.getStringCellValue().trim();
                } catch (IllegalStateException e) {
                    // Si no es un string, intenta numérico
                    try {
                        yield String.valueOf(cell.getNumericCellValue());
                    } catch (IllegalStateException ex) {
                        yield ""; // Retorno de seguridad
                    }
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case BLANK -> "";
            case ERROR -> "";
            default -> "";
        };
    }

    private boolean validarEdadVsFechaNac(int edad, LocalDate fechaNac, int fila) {
        if (fechaNac == null) return true; // No se puede validar si falta la fecha
        int edadCalculada = calcularEdadDesdeFechaNac(fechaNac);
        if (Math.abs(edadCalculada - edad) > 1) {
            errores.add("Fila " + fila + ": La edad ingresada (" + edad + ") no coincide con la calculada desde la fecha de nacimiento (" + fechaNac + "). Edad calculada: " + edadCalculada);
            return false;
        }
        return true;
    }

    private boolean validarCategoriaExiste(String nombre, int fila) {
        if (!categoriasCaRepository.existsByNombreIgnoreCase(nombre)) {
            errores.add("Fila " + fila + ": Categoria invalida = " + nombre);
            return false;
        }
        return true;
    }

    private boolean validarGrupoExiste(String nombre, int fila) {
        if (!gruposGeneralesCaRepository.existsByNombreIgnoreCase(nombre)) {
            errores.add("Fila " + fila + ": Grupo General invalido = " + nombre);
            return false;
        }
        return true;
    }

    private boolean validarPrepaga(String nombre, int fila) {
        if (!prepagaCaRepository.existsByNombreIgnoreCase(nombre)) {
            errores.add("Fila " + fila + ": Prepaga invalida = " + nombre);
            return false;
        }
        return true;
    }

    private void aplicarLogicaOtrosAdherentes(CategoriasCAEntity categoriaEntity, GrupoGeneralesCaEntity grupoEntity, int edad, ResultadoCuota resultado) {
        if ("Otros Adherentes Familiar de 21 hasta 64 años".equalsIgnoreCase(grupoEntity.getNombre())
                && (edad >= 21 && edad <= 64)
                && ("Hermano/a".equalsIgnoreCase(categoriaEntity.getNombre()) || "Nieto".equalsIgnoreCase(categoriaEntity.getNombre()) || "Sobrino/a".equalsIgnoreCase(categoriaEntity.getNombre()))) {
            double monto = 122900;
            resultado.setCuotaTotal(monto);
            resultado.setBanco(monto * 0.00);
            resultado.setFuncionario(monto);
        }
    }

    private void aplicarMayores64OtrosAdherentes(CategoriasCAEntity categoriaEntity, GrupoGeneralesCaEntity grupoEntity, int edad, ResultadoCuota resultado) {
        if ("Otros Adherentes Familiar Mayor de 64 años".equalsIgnoreCase(grupoEntity.getNombre())
                && (edad >= 64)
                && ("Hermano/a".equalsIgnoreCase(categoriaEntity.getNombre()) || "Suegro/a".equalsIgnoreCase(categoriaEntity.getNombre()) || "Sobrino/a".equalsIgnoreCase(categoriaEntity.getNombre()))) {
            double monto = 219300;
            resultado.setCuotaTotal(monto);
            resultado.setBanco(monto * 0.00);
            resultado.setFuncionario(monto);
        }

    }

    private void aplicarCuotaPorDefecto(CategoriasCAEntity categoriaEntity, GrupoGeneralesCaEntity grupoEntity, ResultadoCuota resultado) {
        if (!"TITULAR".equalsIgnoreCase(categoriaEntity.getNombre())) return;

        double monto = grupoEntity.getMontoCuota();
        resultado.setCuotaTotal(monto);
        resultado.setBanco(monto * 0.40);
        resultado.setFuncionario(monto * 0.60);
    }

    public List<String> getErrores() {
        return new ArrayList<>(errores);
    }

    private void aplicarGrupoFamiliar(CategoriasCAEntity categoriaEntity, GrupoGeneralesCaEntity grupoEntity, int edad, ResultadoCuota resultado) {
        if (!"Grupo Familiar".equalsIgnoreCase(grupoEntity.getNombre())) return;

        if ("PADRE".equalsIgnoreCase(categoriaEntity.getNombre()) || "MADRE".equalsIgnoreCase(categoriaEntity.getNombre())) {
            edadesPadres.put(categoriaEntity.getNombre().toUpperCase(), edad);
            resultado.setCuotaTotal(0.0);
            resultado.setBanco(0.0);
            resultado.setFuncionario(0.0);
            return;
        }

        if ("TITULAR".equalsIgnoreCase(categoriaEntity.getNombre())) {
            double base = grupoEntity.getMontoCuota(); // Ya tenemos la entidad, usamos su monto


            double adicionalPadre = obtenerMontoCuotaPorEdad(
                    "Padre/Madre Mayores de 64 años", "Padre/Madre Menores de 64 años",
                    edadesPadres.get("PADRE") // Esto asume que la clave 'PADRE' está poblada para los padres del titular actual
            );
            double adicionalMadre = obtenerMontoCuotaPorEdad(
                    "Padre/Madre Mayores de 64 años", "Padre/Madre Menores de 64 años",
                    edadesPadres.get("MADRE") // Esto asume que la clave 'MADRE' está poblada para los padres del titular actual
            );

            double total = base + adicionalPadre + adicionalMadre;

            resultado.setCuotaTotal(total);
            resultado.setBanco(base * 0.40);
            resultado.setFuncionario(base * 0.60 + adicionalPadre + adicionalMadre);
            return;
        }

        // Otros beneficiarios del grupo familiar (cónyuge, hijos, etc.)
        resultado.setCuotaTotal(0.0);
        resultado.setBanco(0.0);
        resultado.setFuncionario(0.0);
    }

    @Transactional
    private void guardarAsegurado(DatosPersonasDto dto) {
        PersonasCaEntity persona = crearOActualizarPersonas(dto);
        AseguradoControlCaEntity asegurado = crearEntidadAsegurado(dto, persona);
        configurarFechasVigencia(dto, asegurado);
        asociarReferencias(dto, asegurado);

        if (!esTitular(dto)) {
            procesarAdherente(dto, asegurado);
        } else {
            // Para el titular, guarda inmediatamente y añade al mapa para actualizaciones posteriores por adherentes
            procesarTitular(dto, asegurado);
        }
        // Guarda inmediatamente, las cantidades acumuladas finales para los TITULARES se actualizarán más tarde

        aseguradoControlCaRepository.save(asegurado);

        System.out.println("Guardando: " + dto.getCi() + " - " + dto.getCategoriaNombre());

    }

    private PersonasCaEntity crearOActualizarPersonas(DatosPersonasDto dto) {
        PersonasCaEntity persona = personasCaRepository.findByCi(dto.getCi()).orElseGet(() -> {
            PersonasCaEntity nueva = new PersonasCaEntity();
            nueva.setCi(dto.getCi());
            nueva.setFechaCreate(LocalDate.now());
            return nueva;
        });

        persona.setNombre(dto.getNombre());
        persona.setApellido(dto.getApellido());
        persona.setEsTitular(esTitular(dto)); // Establece el indicador esTitular

        if (dto.getFechaNacimiento() != null)
            persona.setFechaNac(dto.getFechaNacimiento());

        if (dto.getMail() != null && !dto.getMail().isBlank())
            persona.setEmails(dto.getMail());

        return personasCaRepository.save(persona);
    }

    private AseguradoControlCaEntity crearEntidadAsegurado(DatosPersonasDto dto,PersonasCaEntity persona) {
        AseguradoControlCaEntity asegurado = new AseguradoControlCaEntity();
        asegurado.setPersona(persona);
        asegurado.setCi(dto.getCi());
        asegurado.setEdadActual(dto.getEdadActual());
        asegurado.setEsTitular(esTitular(dto)); // Establece el indicador esTitular
        asegurado.setFechaInicioInclusion(Optional.ofNullable(dto.getFechaInicioInclusion()).orElse(LocalDate.now()));
        asegurado.setMesVigencia(dto.getMesVigencia());
        asegurado.setFechaCreacion(dto.getFechaCreacion());
        asegurado.setEstado(true); // Por defecto verdadero
        asegurado.setOperador(Optional.ofNullable(dto.getOperador()).orElse("cristobal.acuna@aesaseguros.com.py"));
        asegurado.setMail(dto.getMail());
        asegurado.setAnioVigencia(dto.getAnioVigencia());



       // asegurado.setCapitalAsegurado(dto.getCapitalAsegurado());
        return asegurado;

    }

    private void configurarFechasVigencia(DatosPersonasDto dto, AseguradoControlCaEntity asegurado) {
        int anho = dto.getAnioVigencia() > 0 ? dto.getAnioVigencia() : LocalDate.now().getYear();
        LocalDate inicio = LocalDate.of(anho, dto.getMesVigencia(), 1);
        LocalDate fin = inicio.withDayOfMonth(inicio.lengthOfMonth()); // Último día del mes

        asegurado.setFechaInicioVigencia(inicio);
        asegurado.setFechaFinVigencia(fin);
    }

    private void asociarReferencias(DatosPersonasDto dto, AseguradoControlCaEntity asegurado) {
        // Usa nombres de categoría y grupo normalizados para la búsqueda
        categoriasCaRepository.findByNombreIgnoreCase(dto.getCategoriaNombre()).ifPresent(asegurado::setCategoria);
        gruposGeneralesCaRepository.findByNombreIgnoreCase(dto.getGrupoNombre()).ifPresent(asegurado::setGrupoGeneral);
        prepagaCaRepository.findByNombreIgnoreCase(dto.getPrepagaNombre()).ifPresent(asegurado::setPrepaga);
    }

    private void procesarAdherente(DatosPersonasDto dto, AseguradoControlCaEntity asegurado) {
        asegurado.setCiTitular(dto.getCiTitular());

        // Esta verificación debe ser más robusta si el orden de procesamiento no está garantizado.
        // Es mejor procesar todas las cuotas de los adherentes y luego aplicarlas al titular en una segunda pasada.
        // Por ahora, solo añadiremos al mapa y verificaremos más tarde.
//        if (!titularesPorCi.containsKey(dto.getCiTitular())) {
//            // Es posible que el titular aún no haya sido procesado.
//            // Este error será menos crítico si el cálculo final se realiza en una segunda pasada.
//            // errores.add("Fila con CI " + dto.getCi() + ": No se encontró titular con CI " + dto.getCiTitular() + " al que pertenece.");
//        }

        if (dto.getCuotaTotal() == 0 && dto.getAbsorbeFuncionario() == 0) {
            System.out.println("⚠️ Adherente con CI " + dto.getCi() + " no tiene cuota directa. Se asumirá que es absorbida.");
        }


        // Almacena la contribución de cuota específica del adherente para ser sumada por el titular
        ResultadoCuota cuotaAdicional = new ResultadoCuota(dto.getCuotaTotal(), dto.getAbsorbeBanco(), dto.getAbsorbeFuncionario());
        cuotasPorTitular.computeIfAbsent(dto.getCiTitular(), k -> new ArrayList<>()).add(cuotaAdicional);

        // Los adherentes suelen tener cuota_total 0, su cuota es absorbida por el titular.
        // Los valores establecidos aquí son las *contribuciones individuales del adherente* que luego se agregan.
        asegurado.setCuotaTotal(dto.getCuotaTotal()); // Esto podría ser distinto de cero para algunos tipos de adherentes
        asegurado.setAbsorbeBanco(dto.getAbsorbeBanco());
        asegurado.setAbsorbeFuncionario(dto.getAbsorbeFuncionario());
    }

    private void procesarTitular(DatosPersonasDto dto, AseguradoControlCaEntity asegurado) {
        // Valores iniciales para el titular basados en su propia fila
        double cuota = dto.getCuotaTotal();
        double banco = dto.getAbsorbeBanco();
        double funcionario = dto.getAbsorbeFuncionario();

        // Este metodo se llama durante el procesamiento inicial.
        // Las cuotas de los adherentes se sumarán en un paso de finalización separado.
        // Por ahora, solo almacena la entidad del titular para recuperarla más tarde.
        asegurado.setCuotaTotal(cuota);
        asegurado.setAbsorbeBanco(banco);
        asegurado.setAbsorbeFuncionario(funcionario);

        titularesPorCi.put(dto.getCi(), asegurado); // Almacena el objeto titular para su posterior recuperación/actualización
    }

    private boolean esTitular(DatosPersonasDto dto) {
        return "TITULAR".equalsIgnoreCase(dto.getCategoriaNombre());
    }

    public void generarTxtExport(int mesExcel, int anioExcel) throws IOException {
        // Solo busca titulares porque el archivo de texto se genera solo para titulares.
        // El indicador `esTitular` debe establecerse con precisión durante el procesamiento.
        List<AseguradoControlCaEntity> asegurados = aseguradoControlCaRepository
                .findByEsTitularTrueAndMesVigenciaAndAnioVigencia(mesExcel, anioExcel);

        DateTimeFormatter nombreFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String nombreArchivo = "asegurados_" + LocalDateTime.now().format(nombreFormatter) + ".txt";
        Path archivoTxt = Paths.get("C:/API/archivos-generados/", nombreArchivo);
        Files.createDirectories(archivoTxt.getParent());

        BufferedWriter writer = null; // Inicializa el escritor fuera del try-with-resources para acceder al bloque finally
        String resultadoApi = ""; // Se establecerá en el bloque try
        String estado = "FALLIDO"; // Estado predeterminado

        try {
            writer = Files.newBufferedWriter(archivoTxt, Charset.forName("windows-1252"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");

            int contador = 1;

            for (AseguradoControlCaEntity a : asegurados) {
                // Filtra solo titulares
                if (!Boolean.TRUE.equals(a.getEsTitular())) {
                    continue; // salta beneficiarios y nulos
                }

                PersonasCaEntity persona = a.getPersona();
                if (persona == null) {
                    System.err.println("Saltando asegurado " + a.getCi() + " debido a datos de persona faltantes.");
                    continue;
                }

                String fechaNacimiento = persona.getFechaNac() != null
                        ? persona.getFechaNac().format(formatter)
                        : "01011900";

                String fechaInicioVigencia = a.getFechaInicioVigencia() != null
                        ? a.getFechaInicioVigencia().format(formatter)
                        : "01011900";

                String fechaFinVigencia = a.getFechaFinVigencia() != null
                        ? a.getFechaFinVigencia().format(formatter)
                        : "01011900";

                StringBuilder linea = new StringBuilder();
                linea.append(padLeft(contador++, 6)).append(","); // Nº de orden
                linea.append(padRight(
                        Optional.ofNullable(persona.getApellido()).orElse("") + " " +
                                Optional.ofNullable(persona.getNombre()).orElse(""), 100)).append(",");
                linea.append(padLeft(a.getCi(), 9)).append(",");
                linea.append(padLeft(Optional.ofNullable(persona.getRuc()).orElse(""), 11)).append(",");
                linea.append(padRight("", 15)).append(",");
                linea.append(padRight("", 15)).append(",");
                linea.append(padRight("", 2)).append(",");
                linea.append(padRight("", 20)).append(",");
                linea.append(fechaNacimiento).append(",");
                linea.append(fechaInicioVigencia).append(",");
                linea.append(fechaFinVigencia).append(",");
                linea.append(padRight("", 500)).append(",");
                linea.append(padRight("", 1)).append(",");
                linea.append(padRight(Optional.ofNullable(persona.getEmails()).orElse(""), 150)).append(",");
                linea.append(padRight("", 10)).append(",");
                linea.append(padRight("0", 4)).append(",");
                linea.append(padRight("", 16)).append(",");
                linea.append(padRight("", 7)).append(",");
                linea.append(padRight("", 11)).append(",");
                linea.append(padRight("", 100)).append(",");

                // Montos fijos
                linea.append(padLeft("5", 19)).append(",");
                linea.append(padLeft("0", 19)).append(",");
                linea.append(padLeft("0", 19)).append(",");
                linea.append(padLeft("0", 19)).append(",");
                linea.append(padLeft("0", 19)).append(",");
                linea.append(padLeft("0", 19)).append(",");
                linea.append(padLeft("0", 4)).append(",");
                linea.append(padLeft("0", 19)).append(",");
                linea.append(padLeft("0", 19)).append(",");
                linea.append(padLeft(ExcesoGastosMedicos, 19)).append(",");
                linea.append(padLeft(formatDecimal(a.getAbsorbeFuncionario()), 19)).append(",");
                linea.append(padLeft("0", 19)).append(",");
                linea.append(padLeft("9", 19)).append(",");
                linea.append(padLeft("10", 19)).append(",");
                linea.append(padLeft("11", 19)).append(",");
                linea.append(padLeft("12", 19)).append(",");
                linea.append(padLeft("13", 19)).append(",");
                linea.append(padLeft("14", 19)).append(",");
                linea.append(padLeft("0", 19)).append(",");
                linea.append(padLeft("1", 19)).append(",");
                linea.append(padLeft("2", 19)).append(",");

                linea.append(padLeft(formatDecimal(a.getCuotaTotal()), 20)).append(",");
                for (int i = 0; i < 5; i++) {
                    if (i < 4) {
                        linea.append(padLeft("14", 20)).append(",");
                    } else {
                        linea.append(padLeft("15", 20));
                    }
                }

                // Ajuste de longitud
                int longitudEsperada = 1427;
                int longitudActual = linea.length();
                if (longitudActual > longitudEsperada) {
                    linea.setLength(longitudEsperada);
                } else if (longitudActual < longitudEsperada) {
                    linea.append(" ".repeat(longitudEsperada - longitudActual));
                }

                writer.write(linea.toString());
                writer.newLine();
            }


            System.out.println("✅ Archivo generado con líneas de 1427 caracteres exactos (comas incluidas): " + archivoTxt.toAbsolutePath());
            resultadoApi = "Archivo generado con " + asegurados.size() + " registros.";
            estado = "COMPLETADO";


        } catch (IOException e) {
            errores.add("Error al generar el archivo de exportación: " + e.getMessage());
            resultadoApi = "Error al generar archivo de exportación: " + e.getMessage();
            estado = "FALLIDO";
            throw e; // Lanza de nuevo para indicar el fallo
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.err.println("Error al cerrar BufferedWriter: " + e.getMessage());
                }
            }
            // Siempre intenta registrar la auditoría, incluso si la generación del archivo falla
            registrarAuditoriaCarga("sistema", nombreArchivo, resultadoApi, estado);
        }
    }

//    public void generarTxtExport() throws IOException {
//        if (this.mesVigenciaExcel != null && this.mesVigenciaExcel.get() >= 1
//                && this.anioVigenciaExcel != null && this.anioVigenciaExcel.get() >= 1) {
//            generarTxtExport(this.mesVigenciaExcel.get(), this.anioVigenciaExcel.get());
//        } else {
//            throw new IllegalStateException("❌ mes/anio de vigencia no están inicializados o son inválidos.");
//        }
//    }

    private String padLeft(Object texto, int length) {
        if (texto == null) texto = "";
        String s = texto.toString();
        // Asegura que la cadena no sea más larga que la longitud deseada antes de recortar
        return String.format("%" + length + "s", s).substring(0, Math.min(length, String.format("%" + length + "s", s).length()));
    }

    public FileSystemResource getArchivoGenerado() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        String nombreArchivo = "asegurados_" + timestamp + ".txt";

        return new FileSystemResource("C:/API/archivos-generados/" + nombreArchivo);
    }

    private String padRight(String texto, int longitud) {
        if (texto == null) texto = "";
        String s = texto;
        // Asegura que la cadena no sea más larga que la longitud deseada antes de recortar
        return String.format("%-" + longitud + "s", s).substring(0, Math.min(longitud, String.format("%-" + longitud + "s", s).length()));
    }

    private String formatDecimal(double value) {
        // Formatea un double a una cadena que representa un long, eliminando efectivamente los decimales
        return String.valueOf((long) Math.round(value));
    }


    public List<AseguradoControlCaEntity> obtenerTodos() {
        return aseguradoControlCaRepository.findAll();
    }

    public List<AuditoriaCargaEntity> obtenerAuditoria() {
        return auditoriaCargaRepository.findAll();
    }

    @Transactional
    private void registrarAuditoriaCarga (String usuario, String nombreArchivo, String resultado, String estado) {
        try {
            AuditoriaCargaEntity auditoria = new AuditoriaCargaEntity();
            auditoria.setUsuario(usuario);
            auditoria.setArchivo(nombreArchivo);
            auditoria.setFechaCarga(LocalDateTime.now());
            auditoria.setResultadoApi(resultado);
            auditoria.setEstado(estado);
            auditoriaCargaRepository.save(auditoria);
            System.out.println("Auditoría de carga registrada exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al registrar auditoría de carga: " + e.getMessage());
            e.printStackTrace(); // Registra el seguimiento de la pila para depuración
        }
    }
}
