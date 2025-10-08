# API Emisión Vida — Documentación funcional

## Resumen del proyecto
Este repositorio contiene un microservicio Spring Boot que automatiza la carga y validación de planillas de asegurados para productos de vida. La aplicación recibe un archivo Excel con información de titulares y adherentes, aplica reglas de negocio dependientes del banco, persiste los registros en PostgreSQL y genera un archivo de exportación en formato texto con los titulares aprobados.【F:src/main/java/api_microservice_aesa_emision_vida/controller/ControlAseguradoController.java†L19-L83】【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L79-L171】

## Arquitectura y componentes principales
- **ControlAseguradoController**: expone los endpoints REST para cargar el Excel y consultar auditorías/listados.【F:src/main/java/api_microservice_aesa_emision_vida/controller/ControlAseguradoController.java†L19-L83】
- **ControlAseguradoService**: encapsula el procesamiento del Excel, la aplicación de reglas, la persistencia y la generación de archivos de salida.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L33-L171】
- **GrupoGeneralBancoService**: carga en memoria los parámetros de montos y capital asegurado por banco para ser reutilizados durante el procesamiento.【F:src/main/java/api_microservice_aesa_emision_vida/service/GrupoGeneralBancoService.java†L11-L83】
- **Capa de datos**: se compone de entidades JPA (`AseguradoControlCaEntity`, `PersonasCaEntity`, etc.) y sus repositorios, que mapean las tablas involucradas en la carga.【F:src/main/java/api_microservice_aesa_emision_vida/entity/AseguradoControlCaEntity.java†L10-L200】
- **Configuración**: las propiedades de conexión a la base de datos y puertos se definen en `application.properties` y pueden ajustarse por entorno.【F:src/main/resources/application.properties†L1-L24】

## Flujo de carga del Excel
1. El endpoint `POST /api/carga/cargar-excel` recibe el archivo y el identificador del banco. Se validan parámetros básicos y se delega al servicio principal.【F:src/main/java/api_microservice_aesa_emision_vida/controller/ControlAseguradoController.java†L27-L69】
2. `ControlAseguradoService.procesarExcel` inicializa el estado, carga los parámetros del banco y abre la hoja `DETALLE GRAL`, omitiendo el encabezado.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L79-L108】
3. Se procesa la primera fila para fijar el mes y año de vigencia; luego se recorren las filas restantes aplicando las validaciones de negocio y acumulando errores si aparecen inconsistencias.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L105-L171】
4. Tras leer todas las filas, se actualizan las cuotas finales de los titulares sumando los importes absorbidos por sus adherentes y se generan los artefactos derivados (actualización de fechas, exportación TXT).【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L127-L170】

## Validaciones y reglas clave
- **Mes y año de vigencia**: se derivan de la columna de vigencia; si faltan o son inconsistentes, se aborta la carga. También se evita duplicar cargas para un mismo periodo.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L333-L388】
- **Unicidad por cédula y periodo**: no se persiste un registro si ya existe la combinación CI/mes/año en base de datos.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L350-L355】
- **Referenciales**: se valida la existencia de grupo, categoría y prepaga antes de crear el registro.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L390-L699】
- **Datos personales**: se comparan edad declarada y fecha de nacimiento, y se reconstruyen apellidos/nombres a partir del valor concatenado del Excel.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L303-L409】【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L667-L675】
- **Reglas de cuota**: los montos dependen de la categoría (titular, padres, adherentes) y del grupo; se distribuyen porcentajes para banco y funcionario y se almacenan aportes adicionales por adherente para sumarlos al titular.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L321-L427】

## Persistencia y auditoría
- Cada fila válida se transforma en un `DatosPersonasDto`, se sincroniza la entidad `Personas` y se construye la entidad `Asegurado`, incluyendo relaciones y fechas de vigencia.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L395-L835】
- Los titulares se guardan y se almacenan temporalmente para actualizar sus cuotas con los valores absorbidos; los adherentes registran su contribución para ser agregada posteriormente.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L775-L918】
- Todas las cargas generan un registro en `auditoria_carga` indicando usuario, archivo, resultado y estado final, incluso cuando ocurre un error durante la exportación.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L1017-L1100】

## Exportación del archivo TXT
- Una vez completada la carga, se crea un archivo `asegurados_yyyyMMdd_HHmmss.txt` en `C:/API/archivos-generados/` con una línea por titular y longitudes fijas de 1.427 caracteres, codificado en `windows-1252`.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L908-L1037】
- El controlador intenta devolver el archivo generado inmediatamente después de procesar el Excel, utilizando el mismo patrón de nomenclatura en el recurso de salida.【F:src/main/java/api_microservice_aesa_emision_vida/controller/ControlAseguradoController.java†L41-L55】【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L1056-L1062】

## Endpoints disponibles
| Método | Ruta | Descripción |
| --- | --- | --- |
| `POST` | `/api/carga/cargar-excel` | Carga la planilla Excel y descarga el TXT generado si la operación finaliza correctamente.【F:src/main/java/api_microservice_aesa_emision_vida/controller/ControlAseguradoController.java†L27-L55】 |
| `GET` | `/api/carga/auditoria` | Devuelve todas las ejecuciones registradas en la tabla de auditoría.【F:src/main/java/api_microservice_aesa_emision_vida/controller/ControlAseguradoController.java†L72-L75】 |
| `GET` | `/api/carga/listado_asegurados` | Retorna los asegurados almacenados en base de datos (titulares y adherentes).【F:src/main/java/api_microservice_aesa_emision_vida/controller/ControlAseguradoController.java†L78-L83】 |

## Formato esperado del Excel
La hoja debe llamarse `DETALLE GRAL` y tener los campos siguientes (coordenadas relativas al archivo original):

| Columna | Campo | Uso |
| --- | --- | --- |
| B | CI titular | Se usa para asociar adherentes con su titular.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L297-L326】 |
| E | Apellido, Nombre | Se separa en apellido y nombre (separador coma).【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L303-L308】 |
| F | Código de categoría | Determina la regla aplicada; se valida contra catálogo. Si falta, se usa valor por defecto 9.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L313-L321】 |
| H | Prepaga | Validación referencial obligatoria.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L329-L393】 |
| I | CI asegurado | Identificador único por periodo; obligatorio.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L295-L355】 |
| J | Fecha de nacimiento | Se usa para validar edad y completar datos personales.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L309-L408】 |
| K | Fecha inicio inclusión | Fallback para calcular mes de vigencia si la columna específica está vacía.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L329-L341】 |
| N | Edad declarada | Se contrasta con la fecha de nacimiento y corrige inconsistencias mayores a un año.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L309-L320】【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L667-L675】 |
| O | Grupo | Determina reglas de cuota y capital por banco.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L317-L327】【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L414-L415】 |
| R/T | Cuota total, absorciones banco/funcionario | Se combinan con parámetros del banco para calcular importes finales.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L316-L327】 |
| V/U | Mes de vigencia | Obligatorio; define mes/año de la carga y evita duplicados.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L333-L388】 |
| W | Emails | Se almacenan en la entidad persona si están presentes.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L329-L413】 |

Cualquier fila que no cumpla las reglas anteriores se agrega a la lista de errores y no se persiste.

## Configuración y ejecución local
1. **Requisitos**: Java 17+, Maven, PostgreSQL con acceso a la base `1030` o la que se defina en la configuración.【F:src/main/resources/application.properties†L1-L19】
2. **Variables de entorno**: ajustar `spring.datasource.url`, `spring.datasource.username` y `spring.datasource.password` según el entorno (se recomienda externalizarlos para producción).【F:src/main/resources/application.properties†L5-L7】
3. **Ejecución**:
   ```bash
   ./mvnw spring-boot:run
   ```
   La aplicación expone los endpoints en el puerto `8084` por defecto.【F:src/main/resources/application.properties†L2-L2】

## Manejo de errores y respuestas
- Errores de validación del archivo retornan HTTP 400 con un cuerpo JSON indicando el problema (por ejemplo, archivo faltante).【F:src/main/java/api_microservice_aesa_emision_vida/controller/ControlAseguradoController.java†L30-L35】
- Si el mes/año ya fue cargado anteriormente se responde con HTTP 409 y el mensaje correspondiente.【F:src/main/java/api_microservice_aesa_emision_vida/controller/ControlAseguradoController.java†L57-L62】【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L381-L388】
- Las excepciones inesperadas generan HTTP 500 y se registran en la lista de errores y en la auditoría.【F:src/main/java/api_microservice_aesa_emision_vida/controller/ControlAseguradoController.java†L63-L68】【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L150-L169】【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L1017-L1100】

## Consideraciones adicionales
- El método `getArchivoGenerado` calcula el nombre del archivo con la hora actual, por lo que se debe garantizar sincronización con el archivo realmente escrito para evitar respuestas vacías en entornos concurrentes.【F:src/main/java/api_microservice_aesa_emision_vida/service/ControlAseguradoService.java†L1056-L1062】
- Para nuevos bancos o productos, basta con registrar los parámetros de `grupo_generales_parametro` y la lógica reutilizará esos valores gracias al servicio de caching.【F:src/main/java/api_microservice_aesa_emision_vida/service/GrupoGeneralBancoService.java†L40-L83】

