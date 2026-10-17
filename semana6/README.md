# Proyecto final · Semana 6 · GitHub Copilot

**Alumno:** Andrés Juárez Garduño · **Usuario de GitHub:** Andres189

## 1. Qué construí

| | Feature | Especificación |
|---|---|---|
| [X] | `PATCH /tasks/{id}/assignee` — cambiar el responsable | [`specs/assignee.md`](../specs/assignee.md) |

## 2. El pull request

- **URL del PR (mergeado):** https://github.com/Andres189/taskflow-copilot-andres189/pull/5
- **Commit del merge en `main`:** ba9ab6d (HEAD -> main, origin/main) Merge pull request #5 from Andres189/feature/assignee
- **Comentarios de Copilot code review:** Copilot dejó 0 comentarios.

## 3. Cómo lo hice

| Paso | Qué hice | Evidencia |
|---|---|---|
| Rama y spec | `git switch -c feature/assignee` y copié la spec a `specs/` | `git log --oneline main..feature/assigne` (antes del merge) |
| Implementación | `copilot -p "/crear-endpoint-taskflow …"` con `gpt-5-mini` | `semana6/sesion-implementacion.md` (tiene la línea `Skill "crear-endpoint-taskflow" loaded successfully`) |
| Revisión | agente `revisor` sobre `semana6/proyecto-final.diff` | `semana6/revision.md` (termina con `Veredicto:`) |
| Tests | `mvn test` en verde | Tests run: 85 |
| Comprobación REST | `verificar.ps1` con `casos-assignee.ps1` | sección 5 de este documento |
| Code review | Copilot en el PR | la pestaña *Files changed* del PR |

## 4. Qué hizo el agente y qué corregí yo

| # | Qué hizo mal el agente (archivo) | Quién lo detectó | Cómo quedó corregido |
|---|---|---|---|
| 1 | El agente puso letras "n" sueltas al inicio de algunas líneas en TaskAssigneeUpdateRequest.java y 2 test no compilaban | test | prompt para corregirlo realiza los cambios directamente en los archivos. Primero inspecciona src/test/java/com/taskflow/slice/ReasignarTareaControllerTest.java y elimina los caracteres no imprimibles sin alterar el contenido lógico del test. |
| 2 | TaskAssigneeUpdateRequest.java no terminó con salto de línea y el @NotNull no trae message como los otros DTO | revisor | No las apliqué, son sugerencias y el veredicto pide @NotNull @Positive. |

**Lo que el agente hizo bien a la primera** (una o dos líneas): TaskService.reasignar y el endpoint de TaskController: nunca dieron errores.

## 5. Comprobaciones REST

```text
[OK]    GET /tasks/overdue devuelve solo la tarea 7
[OK]    GET /tasks/unassigned devuelve las tareas 4 y 6
[OK]    GET /projects/1/summary
[OK]    GET /projects/2/summary
[OK]    GET /projects/3/summary
[OK]    GET /projects/99/summary responde 404
[OK]    GET /projects/1/summary sin token responde 401
[OK]    PATCH /tasks/4/assignee asigna a luis
[OK]    GET /tasks/4 conserva el responsable nuevo
[OK]    PATCH /tasks/4/status a DONE ahora responde 200
[OK]    PATCH /tasks/2/assignee (DONE) responde 422
[OK]    PATCH /tasks/99/assignee responde 404
[OK]    PATCH /tasks/6/assignee con {} responde 400 y nombra assigneeId
[OK]    PATCH /tasks/6/assignee con 0 responde 400
[OK]    PATCH /tasks/6/assignee sin token responde 401
App detenida (PID 21408).
[OK]    App apagada: el puerto 8080 ya no responde
RESULTADO: 16/16 OK
```

## 6. Créditos de la semana

| Qué | AI credits |
|---|---|
| Usados en octubre según github.com (incluye semanas anteriores si usaste Copilot antes) | 265 |
| Implementación con la skill (`AI Credits` del PF-2) | 9.82 |
| Revisión del `revisor` (`AI Credits` del PF-3) | 1.88 |
| Correcciones del PF-4 y del PF-6, si hubo (`AI Credits`) | 0 |
