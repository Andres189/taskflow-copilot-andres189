# Copilot CLI Session

> [!NOTE]
> - **Session ID:** `61f08f93-8448-46e3-ad28-bf452bcfc859`  
> - **Started:** 16/10/2026, 11:23:28 a.m.  
> - **Duration:** 32s  
> - **Exported:** 16/10/2026, 11:24:01 a.m.  

---

<sub>2s</sub>

### User

Revisa evidencia/dia4/summary.diff contra la especificación specs/summary.md.

---

<sub>7s</sub>

### Copilot

Abriré en paralelo el diff (evidencia/dia4/summary.diff) y la especificación (specs/summary.md) para comparar alcance, contracto y tests según la checklist del revisor. Luego analizaré cada archivo del diff contra la versión actual del repositorio si hace falta. Ejecutando lectura de ambos archivos.

---

<sub>7s</sub>

### `view`

**view the file at evidencia/dia4/summary.diff.**

evidencia/dia4/summary.diff

<details>
<summary>257 lines</summary>

```diff
diff --git a/src/main/java/com/taskflow/controller/ProjectController.java b/src/main/java/com/taskflow/controller/ProjectController.java
index bef609e..e10a0ee 100644
--- a/src/main/java/com/taskflow/controller/ProjectController.java
+++ b/src/main/java/com/taskflow/controller/ProjectController.java
@@ -56,6 +56,16 @@ public class ProjectController {
                 .toList();
     }
 
+    /** GET /projects/{id} — 200 con ProjectResponse; 404 uniforme si no existe. */
+    @Operation(summary = "Obtiene el resumen del proyecto",
+            description = "Devuelve un resumen con conteos por estado y tareas vencidas. 404 si el proyecto no existe.")
+    @GetMapping("/projects/{id}/summary")
+    public com.taskflow.dto.ProjectSummaryResponse getProjectSummary(@PathVariable("id") Long id) {
+        Project proyecto = projectService.buscarPorId(id)
+                .orElseThrow(() -> new ProjectNotFoundException(id));
+        return projectService.resumen(proyecto);
+    }
+
     /** GET /projects/{id} — 200 con ProjectResponse; 404 uniforme si no existe. */
     @Operation(summary = "Obtiene un proyecto por id",
             description = "200 con el ProjectResponse; 404 uniforme si el id no existe.")
diff --git a/src/main/java/com/taskflow/dto/ProjectSummaryResponse.java b/src/main/java/com/taskflow/dto/ProjectSummaryResponse.java
new file mode 100644
index 0000000..3fcd038
--- /dev/null
+++ b/src/main/java/com/taskflow/dto/ProjectSummaryResponse.java
@@ -0,0 +1,15 @@
+package com.taskflow.dto;
+
+import com.taskflow.model.TaskStatus;
+
+import java.util.Map;
+
+/**
+ * DTO de salida para GET /projects/{id}/summary
+ */
+public record ProjectSummaryResponse(Long projectId,
+                                     String projectName,
+                                     int totalTasks,
+                                     Map<TaskStatus, Integer> byStatus,
+                                     int overdue) {
+}
diff --git a/src/main/java/com/taskflow/mapper/ProjectSummaryMapper.java b/src/main/java/com/taskflow/mapper/ProjectSummaryMapper.java
new file mode 100644
index 0000000..a597fb8
--- /dev/null
+++ b/src/main/java/com/taskflow/mapper/ProjectSummaryMapper.java
@@ -0,0 +1,36 @@
+package com.taskflow.mapper;
+
+import com.taskflow.dto.ProjectSummaryResponse;
+import com.taskflow.model.Project;
+import com.taskflow.model.Task;
+import com.taskflow.model.TaskStatus;
+
+import java.util.EnumMap;
+import java.util.List;
+import java.util.Map;
+
+/**
+ * Mapea la información agregada de un proyecto hacia ProjectSummaryResponse.
+ */
+public final class ProjectSummaryMapper {
+
+    private ProjectSummaryMapper() {}
+
+    public static ProjectSummaryResponse aResponse(Project project, List<Task> tareas) {
+        EnumMap<TaskStatus, Integer> byStatus = new EnumMap<>(TaskStatus.class);
+        // Inicializar siempre las tres claves
+        for (TaskStatus s : TaskStatus.values()) {
+            byStatus.put(s, 0);
+        }
+        int overdue = 0;
+        for (Task t : tareas) {
+            TaskStatus s = t.getStatus();
+            byStatus.put(s, byStatus.get(s) + 1);
+            if (t.estaVencida()) {
+                overdue++;
+            }
+        }
+        int total = tareas.size();
+        return new ProjectSummaryResponse(project.getId(), project.getName(), total, Map.copyOf(byStatus), overdue);
+    }
+}
diff --git a/src/main/java/com/taskflow/service/ProjectService.java b/src/main/java/com/taskflow/service/ProjectService.java
index b3e9d0f..8e3cb61 100644
--- a/src/main/java/com/taskflow/service/ProjectService.java
+++ b/src/main/java/com/taskflow/service/ProjectService.java
@@ -87,6 +87,15 @@ public class ProjectService {
         return projectRepository.save(reemplazo);
     }
 
+    /**
+     * Resume un proyecto: cuenta tareas por estado y cuántas están vencidas.
+     * Recibe la entidad Project ya encontrada (el controller decide 404).
+     */
+    public com.taskflow.dto.ProjectSummaryResponse resumen(Project project) {
+        List<Task> tareas = tareasDe(project.getId());
+        return com.taskflow.mapper.ProjectSummaryMapper.aResponse(project, tareas);
+    }
+
     /**
      * Elimina un proyecto (DELETE) con CASCADA MANUAL: borra primero sus tareas (regla "no Task sin
      * Project" — y además la FK task.project_id lo EXIGE: borrar el proyecto con tareas colgando daría
diff --git a/src/test/java/com/taskflow/slice/ProjectControllerSummaryTest.java b/src/test/java/com/taskflow/slice/ProjectControllerSummaryTest.java
new file mode 100644
index 0000000..5c0ccc0
--- /dev/null
+++ b/src/test/java/com/taskflow/slice/ProjectControllerSummaryTest.java
@@ -0,0 +1,68 @@
+package com.taskflow.slice;
+
+import com.taskflow.dto.ProjectSummaryResponse;
+import com.taskflow.dto.ProjectResponse;
+import com.taskflow.dto.TaskResponse;
+import com.taskflow.model.TaskStatus;
+import com.taskflow.service.ProjectService;
+import org.junit.jupiter.api.Test;
+import org.springframework.beans.factory.annotation.Autowired;
+import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
+import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
+import org.springframework.boot.test.mock.mockito.MockBean;
+import org.springframework.http.MediaType;
+import org.springframework.security.test.context.support.WithMockUser;
+import org.springframework.test.web.servlet.MockMvc;
+
+import java.util.EnumMap;
+import java.util.Map;
+
+import static org.mockito.Mockito.when;
+import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
+
+@WebMvcTest(com.taskflow.controller.ProjectController.class)
+@AutoConfigureMockMvc(addFilters = false)
+public class ProjectControllerSummaryTest {
+
+    @Autowired
+    private MockMvc mockMvc;
+
+    @MockBean
+    private ProjectService projectService;
+
+    @MockBean
+    private com.taskflow.security.JwtAuthenticationFilter jwtAuthenticationFilter;
+
+    @Test
+    @WithMockUser // simplifica autenticación en slice test
+    void getResumen_proyectoExistente_devuelve200yJson() throws Exception {
+        EnumMap<TaskStatus, Integer> byStatus = new EnumMap<>(TaskStatus.class);
+        byStatus.put(TaskStatus.TODO, 1);
+        byStatus.put(TaskStatus.IN_PROGRESS, 2);
+        byStatus.put(TaskStatus.DONE, 1);
+        ProjectSummaryResponse resp = new ProjectSummaryResponse(2L, "App Móvil", 4, Map.copyOf(byStatus), 1);
+        when(projectService.buscarPorId(2L)).thenReturn(java.util.Optional.of(new com.taskflow.model.Project(2L, "App Móvil", "desc", 1L, java.time.LocalDate.now())));
+        when(projectService.resumen(org.mockito.ArgumentMatchers.any())).thenReturn(resp);
+
+        mockMvc.perform(get("/projects/2/summary").accept(MediaType.APPLICATION_JSON))
+                .andExpect(status().isOk())
+                .andExpect(jsonPath("$.projectId").value(2))
+                .andExpect(jsonPath("$.projectName").value("App Móvil"))
+                .andExpect(jsonPath("$.totalTasks").value(4))
+                .andExpect(jsonPath("$.byStatus.TODO").value(1))
+                .andExpect(jsonPath("$.byStatus.IN_PROGRESS").value(2))
+                .andExpect(jsonPath("$.byStatus.DONE").value(1))
+                .andExpect(jsonPath("$.overdue").value(1));
+    }
+
+    @Test
+    @WithMockUser
+    void getResumen_proyectoInexistente_devuelve404() throws Exception {
+        when(projectService.buscarPorId(99L)).thenReturn(java.util.Optional.empty());
+
+        mockMvc.perform(get("/projects/99/summary").accept(MediaType.APPLICATION_JSON))
+                .andExpect(status().isNotFound());
+    }
+}
diff --git a/src/test/java/com/taskflow/unit/ProjectServiceSummaryTest.java b/src/test/java/com/taskflow/unit/ProjectServiceSummaryTest.java
new file mode 100644
index 0000000..499e36b
--- /dev/null
+++ b/src/test/java/com/taskflow/unit/ProjectServiceSummaryTest.java
@@ -0,0 +1,75 @@
+package com.taskflow.unit;
+
+import com.taskflow.dto.ProjectSummaryResponse;
+import com.taskflow.exception.ProjectNotFoundException;
+import com.taskflow.model.Priority;
+import com.taskflow.model.Project;
+import com.taskflow.model.Task;
+import com.taskflow.model.TaskStatus;
+import com.taskflow.repository.ProjectRepository;
+import com.taskflow.repository.TaskRepository;
+import com.taskflow.repository.UserRepository;
+import com.taskflow.service.ProjectService;
+import org.junit.jupiter.api.Test;
+import org.junit.jupiter.api.extension.ExtendWith;
+import org.mockito.InjectMocks;
+import org.mockito.Mock;
+import org.mockito.junit.jupiter.MockitoExtension;
+
+import java.time.LocalDate;
+import java.util.List;
+import java.util.Optional;
+
+import static org.assertj.core.api.Assertions.assertThat;
+import static org.mockito.Mockito.when;
+
+@ExtendWith(MockitoExtension.class)
+public class ProjectServiceSummaryTest {
+
+    @Mock
+    private ProjectRepository projectRepository;
+
+    @Mock
+    private TaskRepository taskRepository;
+
+    @Mock
+    private UserRepository userRepository;
+
+    @InjectMocks
+    private ProjectService projectService;
+
+    @Test
+    void resumen_proyectoConTareas_calculaCorrectamente() throws com.taskflow.exception.TaskValidationException {
+        Project p = new Project(2L, "App Móvil", "desc", 1L, LocalDate.now());
+        Task t1 = new Task(10L, "T1x", null, TaskStatus.TODO, Priority.LOW, 2L, null, null);
+        Task t2 = new Task(11L, "T2x", null, TaskStatus.IN_PROGRESS, Priority.MED, 2L, null, LocalDate.now().minusDays(1)); // vencida
+        Task t3 = new Task(12L, "T3x", null, TaskStatus.IN_PROGRESS, Priority.HIGH, 2L, null, null);
+        Task t4 = new Task(13L, "T4x", null, TaskStatus.DONE, Priority.LOW, 2L, null, LocalDate.now().minusDays(5));
+
+        when(taskRepository.findByProjectId(2L)).thenReturn(List.of(t1, t2, t3, t4));
+
+        ProjectSummaryResponse res = projectService.resumen(p);
+
+        assertThat(res.projectId()).isEqualTo(2L);
+        assertThat(res.projectName()).isEqualTo("App Móvil");
+        assertThat(res.totalTasks()).isEqualTo(4);
+        assertThat(res.byStatus().get(TaskStatus.TODO)).isEqualTo(1);
+        assertThat(res.byStatus().get(TaskStatus.IN_PROGRESS)).isEqualTo(2);
+        assertThat(res.byStatus().get(TaskStatus.DONE)).isEqualTo(1);
+        assertThat(res.overdue()).isEqualTo(1);
+    }
+
+    @Test
+    void resumen_proyectoSinTareas_devuelveCeros() throws com.taskflow.exception.TaskValidationException {
+        Project p = new Project(3L, "Sin Tareas", "desc", 1L, LocalDate.now());
+        when(taskRepository.findByProjectId(3L)).thenReturn(List.of());
+
+        ProjectSummaryResponse res = projectService.resumen(p);
+
+        assertThat(res.totalTasks()).isEqualTo(0);
+        assertThat(res.byStatus().get(TaskStatus.TODO)).isEqualTo(0);
+        assertThat(res.byStatus().get(TaskStatus.IN_PROGRESS)).isEqualTo(0);
+        assertThat(res.byStatus().get(TaskStatus.DONE)).isEqualTo(0);
+        assertThat(res.overdue()).isEqualTo(0);
+    }
+}
```

</details>


---

<sub>7s</sub>

### `view`

**view the file at specs/summary.md.**

specs/summary.md

<details>
<summary>52 lines</summary>

````
## Qué se pide

Un endpoint nuevo, `GET /projects/{id}/summary`, que resume un proyecto: **cuántas tareas tiene en
cada estado y cuántas están vencidas**. Sirve para pintar un tablero sin descargar todas las tareas del
proyecto. Pide el token de `POST /auth/login`, como todo lo que no es público.

## Respuesta 200

```json
{
  "projectId": 2,
  "projectName": "App Móvil",
  "totalTasks": 4,
  "byStatus": { "TODO": 1, "IN_PROGRESS": 2, "DONE": 1 },
  "overdue": 1
}
```

| Campo | Tipo | Qué es |
|---|---|---|
| `projectId` | número | el `id` del proyecto pedido |
| `projectName` | texto | el `name` del proyecto |
| `totalTasks` | número | cuántas tareas tiene el proyecto |
| `byStatus` | objeto | una clave por cada valor de `TaskStatus` (`TODO`, `IN_PROGRESS`, `DONE`), **siempre las tres**, aunque valgan 0 |
| `overdue` | número | cuántas tareas del proyecto cumplen `Task.estaVencida()` |

## Reglas

1. **Vencida** es exactamente lo que ya dice `Task.estaVencida()`: tiene fecha límite, la fecha ya pasó y
   el estado no es `DONE`. Se reutiliza ese método; no se reescribe la regla en el servicio.
2. Un proyecto **sin tareas** responde 200 con `totalTasks` 0, las tres claves de `byStatus` en 0 y `overdue` 0.
3. Un proyecto **que no existe** responde **404** con el `ErrorResponse` uniforme de siempre
   (`ProjectNotFoundException`, igual que `GET /projects/{id}`).
4. **Sin token** responde **401**, como todo lo que no es público.
5. Cualquier usuario autenticado puede pedir el resumen de cualquier proyecto (igual que `GET /projects/{id}/tasks`).

## Resultado esperado con la semilla del perfil `h2`

| Petición | `totalTasks` | `byStatus` | `overdue` |
|---|---|---|---|
| `GET /projects/1/summary` | 5 | `TODO` 3 · `IN_PROGRESS` 1 · `DONE` 1 | 0 (la tarea 2 tiene fecha pasada, pero está `DONE`) |
| `GET /projects/2/summary` | 4 | `TODO` 1 · `IN_PROGRESS` 2 · `DONE` 1 | 1 (la tarea 7) |
| `GET /projects/3/summary` | 0 | `TODO` 0 · `IN_PROGRESS` 0 · `DONE` 0 | 0 |
| `GET /projects/99/summary` | — | — | 404 |

Si antes de probar creaste tareas en un proyecto (por la UI o con el servidor MCP), `totalTasks` y
`TODO` de ese proyecto suben en la misma cantidad. Al reiniciar la app con el perfil `h2` vuelven a
ser los de la tabla.

## Cómo se construye (convenciones de TaskFlow)

- DTO de salida: un `record` en `com.taskflow.dto`, `ProjectSummaryResponse`.
- La lógica de conteo va en el servicio (`ProjectService`), no en el controller.
- El controller solo traduce HTTP: busca el proyecto (404 si no existe) y delega.
- Tests:
  - **unit** del servicio con Mockito: un proyecto con tareas en los tres estados y una vencida; un proyecto sin tareas.
  - **slice** `@WebMvcTest` del controller: 200 con el JSON de arriba y 404 con un proyecto inexistente.
- `mvn -q test` en verde al terminar, sin modificar tests existentes.

## Criterios de aceptación

- [ ] `GET /projects/2/summary` con el token de `ana` devuelve el JSON del ejemplo.
- [ ] `GET /projects/3/summary` devuelve ceros en los cinco números.
- [ ] `GET /projects/99/summary` devuelve 404.
- [ ] `mvn -q test` en verde: los tests de antes más los nuevos.
````

</details>


---

<sub>32s</sub>

### Copilot

Resumen: comparar diff y spec; ambos coinciden en alcance, contrato y tests añadidos. Hallazgos listados abajo.

| # | Gravedad | Archivo:línea | Punto de la checklist | Qué pasa | Qué harías |
|---|---|---:|---|---|---|
| 1 | Sugerencia | src/main/java/com/taskflow/controller/ProjectController.java:56-76 | Alcance / Rutas | Se añadió GET /projects/{id}/summary conforme a la spec. | Nada; correcto. |
| 2 | Sugerencia | src/main/java/com/taskflow/dto/ProjectSummaryResponse.java:1-15 | Contrato (campos/tipos) | DTO record tiene campos exactos; usa Map\<TaskStatus,Integer> para byStatus (serializa claves a nombres de enum). | Nada; correcto. |
| 3 | Sugerencia | src/main/java/com/taskflow/mapper/ProjectSummaryMapper.java:1-36 | Reglas del dominio | Reusa Task.estaVencida() y cuenta por TaskStatus inicializando todas las claves. | Nada; correcto. |
| 4 | Sugerencia | src/main/java/com/taskflow/service/ProjectService.java:87-102 | Lógica en Service / Separación | resumen(Project) delega en tareasDe(...) y en el mapper; controller solo busca el proyecto. | Nada; correcto. |
| 5 | Sugerencia | src/test/java/com/taskflow/slice/ProjectControllerSummaryTest.java:1-68 | Tests slice / Seguridad | @WebMvcTest presente; se desactivan filtros @AutoConfigureMockMvc(addFilters = false) y se usa @WithMockUser. Esto evita probar 401 en slice. | Opcional: eliminar addFilters=false y mockear JwtFilter ó añadir un test slice que verifique 401 para la ruta. |
| 6 | Sugerencia | src/test/java/com/taskflow/unit/ProjectServiceSummaryTest.java:1-75 | Tests unit (casos) | Unit tests cubren proyecto con tareas (incl. vencida) y proyecto sin tareas. | Nada; correcto. |
| 7 | Sugerencia | Varios | Convenciones generales | DTO record, mapper manual, lógica en service, no tocar handlers globales: cumplido. | Nada; correcto. |

Casos sin test
- Respuesta 401 cuando se accede sin token (no cubierta por los tests añadidos).  
- Pruebas end-to-end con la semilla h2 (aceptación) no están en la suite; la spec pide verificaciones manuales con la app arrancada, no tests automáticos.

Veredicto: APROBADO

Comentarios finales (breves)
- Cambios cumplen la especificación: ruta, contrato JSON, reutilización de Task.estaVencida(), y tests unit/slice exigidos.  
- Recomiendo, opcionalmente, añadir un slice que verifique 401 o quitar addFilters=false para ejercitar seguridad en slice tests.

---

<sub>Generated by [GitHub Copilot CLI](https://github.com/features/copilot/cli)</sub>
