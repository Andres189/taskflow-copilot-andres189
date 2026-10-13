# Arquitectura de TaskFlow

Documento breve para un desarrollador nuevo: capas, recorrido de `POST /projects/{projectId}/tasks`, ubicación de reglas de negocio, seguridad JWT y organización de tests.

## Capas y paquetes

- Capa de presentación / controladores
  - Paquete: `com.taskflow.controller`
  - Ejemplo: `src/main/java/com/taskflow/controller/TaskController.java` (`TaskController`)
  - Responsable de recibir HTTP, validar `@Valid` DTOs y devolver DTOs de respuesta.

- Capa de servicio (casos de uso)
  - Paquete: `com.taskflow.service`
  - Ejemplo: `src/main/java/com/taskflow/service/TaskService.java` (`TaskService`), `AuthService`
  - Orquesta operaciones, transaccionalidad y llamadas a repositorios. No contiene reglas de negocio primarias (esas viven en el dominio cuando corresponda).

- Capa de dominio / modelo
  - Paquete: `com.taskflow.model`
  - Ejemplo: `src/main/java/com/taskflow/model/Task.java` (`Task`), `Project`, `User`
  - Aquí están las reglas de negocio fundamentales y los invariantes (factories como `Task.crear(...)`, validaciones en constructores, `Task.setStatus(...)`, `Task.estaVencida()`). La entidad es la fuente de verdad del comportamiento.

- Repositorios (persistencia)
  - Paquete: `com.taskflow.repository`
  - Ejemplo: `src/main/java/com/taskflow/repository/TaskRepository.java` (`TaskRepository`)
  - Repositorio Spring Data JPA: `save`, `findById`, `deleteById`.

- DTOs y mappers
  - Paquete: `com.taskflow.dto` y `com.taskflow.mapper`
  - Ejemplos: `src/main/java/com/taskflow/dto/TaskRequest.java` (`TaskRequest`), `src/main/java/com/taskflow/dto/TaskResponse.java` (`TaskResponse`), `src/main/java/com/taskflow/mapper/TaskMapper.java` (`TaskMapper`)
  - Los controladores exponen DTOs (no entidades). Los mappers convierten entre DTOs y entidades; el mapper delega la creación válida a `Task.crear(...)` cuando corresponde.

- Config y seguridad
  - Paquete: `com.taskflow.config`, `com.taskflow.security`
  - Ejemplos: `src/main/java/com/taskflow/config/SecurityConfig.java` (`SecurityConfig`), `src/main/java/com/taskflow/security/JwtService.java` (`JwtService`), `src/main/java/com/taskflow/security/JwtAuthenticationFilter.java` (`JwtAuthenticationFilter`), `src/main/java/com/taskflow/security/ProjectSecurity.java` (`ProjectSecurity`)
  - Seguridad declarativa: reglas por URL + `@PreAuthorize` para reglas data-driven (p. ej. solo owner o ADMIN borra proyecto).

- Advice / manejo de errores
  - Ejemplo: `src/main/java/com/taskflow/advice/GlobalExceptionHandler.java` (`GlobalExceptionHandler`)
  - Centraliza la traducción de excepciones de dominio/servicio a respuestas HTTP uniformes (400, 404, 422, 409, 401, etc.).

- Semilla de datos
  - Ejemplo: `src/main/java/com/taskflow/config/DataSeeder.java` (`DataSeeder`)
  - Llena la BD con usuarios y datos de ejemplo cuando la BD está vacía (perfil `h2`).

- Recursos estáticos
  - `src/main/resources/static/` — UI servida por la misma API.

## Recorrido de POST /projects/{projectId}/tasks

1. HTTP request llega a `TaskController.createTask` (`src/main/java/com/taskflow/controller/TaskController.java`).
2. Spring deserializa el body a `TaskRequest` (`src/main/java/com/taskflow/dto/TaskRequest.java`) y aplica `@Valid`.
   - Si falla Bean Validation, `GlobalExceptionHandler` convierte el error en `400 Bad Request` con detalles por campo.
3. El controller verifica que el proyecto exista llamando a `ProjectService.buscarPorId(projectId)`; si no existe lanza `ProjectNotFoundException` y se traduce a `404`.
4. El controller delega la creación a `TaskService.crear(request, projectId)` (`src/main/java/com/taskflow/service/TaskService.java`).
5. Dentro de `TaskService.crear` se convierte el DTO en entidad con `TaskMapper.aEntidadNueva(request, projectId)` (esa fábrica delega en `Task.crear(...)` para aplicar las reglas de negocio) y se persiste con `TaskRepository.save(...)` (`src/main/java/com/taskflow/repository/TaskRepository.java`), que asigna el id.
6. Tras persistir, `TaskController` construye la cabecera `Location` (`/tasks/{id}`) y responde `201 Created` con el body `TaskMapper.aResponse(creada)`.
7. Si `Task.crear` o la fábrica lanzan `TaskValidationException`, `GlobalExceptionHandler` la traduce a `400`. Si más adelante un cambio de estado viola reglas (p. ej. pasar a DONE sin assignee), el service/advice lo traduce a `422 Unprocessable Entity`.

Notas importantes:
- JPA rehidratación vs creación: JPA usa un constructor sin-arg para leer de BD; el factory `Task.crear(...)` es el camino de negocio para creación y aplica reglas temporales (como `dueDate` no pasado).
- El objeto `Task` tiene un campo `projectId` como escalar (la columna manda) y una asociación `@ManyToOne` solo para navegación (insertable=false/updatable=false). Por eso los DTOs y servicios usan `projectId`.

## Dónde viven las reglas de negocio

- Reglas estructurales y de invariant: dentro de la entidad de dominio `Task` (`src/main/java/com/taskflow/model/Task.java`). Ejemplos:
  - `Task.crear(...)` valida título, projectId y la regla temporal de `dueDate` al crear.
  - `Task.setStatus(...)` encapsula reglas de transición (p. ej. "no pasar a DONE sin assignee").
  - `Task.estaVencida()` implementa lógica derivada.
- Orquestación y validaciones transaccionales: `TaskService` coordina llamadas a repositorio y traduce excepciones de dominio en errores HTTP adecuados cuando hace falta.
- Validaciones de frontera (forma/JSON/constraints): declaradas en DTOs con Bean Validation (`@NotBlank`, `@Size`, etc.) y manejadas por `GlobalExceptionHandler`.

Regla general: "la lógica que define invariantes del modelo está en el dominio; la lógica que coordina casos de uso y persistencia está en los servicios".

## Seguridad (JWT)

- Emisión de tokens
  - `AuthService` (`src/main/java/com/taskflow/service/AuthService.java`) autentica credenciales con `AuthenticationManager`. Si pasan, utiliza `JwtService` (`src/main/java/com/taskflow/security/JwtService.java`) para generar un JWT firmado.
  - El JWT contiene `sub` (username), `role` (claim informativo) y `iat/exp`.
  - Configuración sensible: `src/main/resources/application.yml` lee `jwt.secret` y `jwt.expiration-ms`; en Docker/env el `JWT_SECRET` viene del ambiente.

- Autorización en requests
  - `JwtAuthenticationFilter` (`src/main/java/com/taskflow/security/JwtAuthenticationFilter.java`) extrae el header `Authorization: Bearer ...`, valida la firma y carga `UserDetails` desde la BD. Si el token es válido, coloca la `Authentication` en el `SecurityContext`.
  - Si el token está inválido o expirado, el filtro responde `401` y corta la cadena.
  - `SecurityConfig` (`src/main/java/com/taskflow/config/SecurityConfig.java`) define la `SecurityFilterChain`: rutas públicas (`/auth/**`, `/info`, Swagger, H2 console, static) y `anyRequest().authenticated()` para el resto. También habilita `@PreAuthorize` para reglas data-driven.
  - Reglas data-driven: `ProjectSecurity` (`src/main/java/com/taskflow/security/ProjectSecurity.java`) expone métodos que `@PreAuthorize` puede invocar para checks como "¿es owner?".

- Principio: JWT = stateless; la autorización definitiva para decisiones finas se consulta en la BD o en beans especializados (no se confía solo en claims para decisiones criticas).

## Organización de tests

- Unit tests (sin Spring)
  - Carpeta: `src/test/java/com/taskflow/unit`
  - Ejemplo: `src/test/java/com/taskflow/unit/TaskServiceTest.java` — prueban `TaskService` con Mockito, foco en lógica del servicio desacoplada de Spring.

- Slice tests / Web tests
  - Usan `@WebMvcTest` o similar para probar controladores y mappers en aislamiento.

- Integration tests
  - Nombres terminados en `*IT.java` usan Testcontainers y requieren `-Ddocker.tests=true` para ejecutarse (no corren con `mvn test` por defecto esta semana). Ver convenciones en el README.

- Semilla y perfiles de test
  - Perfil `h2`: arranque con H2 en memoria/archivo y `DataSeeder` (`src/main/java/com/taskflow/config/DataSeeder.java`) rellena usuarios (`ana`, `luis`, `admin`) y datos de ejemplo.
  - Comandos típicos (Windows PowerShell):
    - `mvn -q test` — suite normal (unit + slice)
    - `mvn -q test "-Dtest=TaskServiceTest"` — ejecutar una clase
    - `mvn spring-boot:run "-Dspring-boot.run.profiles=h2"` — correr app con H2 y datos semilla

## Dónde mirar primero si entras al código

- `src/main/java/com/taskflow/controller/TaskController.java` — rutas y contratos HTTP.
- `src/main/java/com/taskflow/dto/TaskRequest.java` / `TaskResponse.java` — forma de entrada/salida.
- `src/main/java/com/taskflow/mapper/TaskMapper.java` — cómo se transforma DTO↔Entidad.
- `src/main/java/com/taskflow/model/Task.java` — reglas de negocio e invariantes.
- `src/main/java/com/taskflow/service/TaskService.java` — casos de uso y transacciones.
- `src/main/java/com/taskflow/advice/GlobalExceptionHandler.java` — cómo se traducen errores a HTTP.
- `src/main/java/com/taskflow/config/SecurityConfig.java` y `src/main/java/com/taskflow/security/JwtAuthenticationFilter.java` — seguridad y flujo de auth.

---

Si quieres, puedo:
- Generar un diagrama simple de dependencias entre capas, o
- Extraer las rutas REST y DTOs a un README-condensado para frontend.

(Respuesta y archivo generados en español; las clases y rutas aparecen entre backticks como pediste.)

