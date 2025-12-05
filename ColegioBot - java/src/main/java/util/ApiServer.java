package util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dao.*; // Importamos todos los DAOs
import model.Tutor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import service.GoogleAIService;
import spark.Spark;

public class ApiServer {
    public static void main(String[] args) {

        // 1. CONFIGURACIÓN DEL PUERTO Y ARCHIVOS PÚBLICOS
        Spark.port(4567);
        Spark.staticFiles.location("/public");

        // 2. CONFIGURACIÓN CORS (Para que el navegador no bloquee las peticiones)
        Spark.options("/*", (request, response) -> {
            String h = request.headers("Access-Control-Request-Headers");
            if (h != null) {
                response.header("Access-Control-Allow-Headers", h);
            }
            String m = request.headers("Access-Control-Request-Method");
            if (m != null) {
                response.header("Access-Control-Allow-Methods", m);
            }
            return "OK";
        });

        Spark.before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Accept");
            res.type("application/json");
        });

        // 3. INICIALIZACIÓN DE DAOS Y SERVICIOS
        GoogleAIService cerebroIA = new GoogleAIService();
        Gson gson = new Gson();

        AsistenciaDAO asistenciaDAO = new AsistenciaDAO();
        TutorDAO tutorDAO = new TutorDAO();
        DocenteDAO docenteDAO = new DocenteDAO();
        NotaDAO notaDAO = new NotaDAO();
        AdminDAO adminDAO = new AdminDAO();
        ObservacionDAO obsDAO = new ObservacionDAO();
        AsistenciaDAO asistDAO = new AsistenciaDAO();

        System.out.println("🚀 Servidor Web LISTO en http://localhost:4567");

        // ==========================================
        // RUTA DE CHATBOT (IA)
        // ==========================================
        Spark.post("/api/chat", (req, res) -> {
            Map body = gson.fromJson(req.body(), Map.class);
            String usuario = (String)body.get("usuario");
            String mensaje = (String)body.get("mensaje");
            String dniContexto = (String)body.get("dniContexto");

            String promptFinal = "ERES: El asistente escolar.";
            if (dniContexto != null && !dniContexto.equals("null")) {
                String datos = tutorDAO.obtenerInfoCompleta(dniContexto);
                if (datos != null && !datos.contains("No encontrado")) {
                    promptFinal = promptFinal + "\nDATOS: " + datos;
                }
            }

            String respIA = cerebroIA.preguntarIA(usuario == null ? "User" : usuario, mensaje, promptFinal);
            return gson.toJson(Map.of("reply", respIA));
        });

        // ==========================================
        // RUTA LOGIN PRINCIPAL (Para la web)
        // ==========================================
        Spark.post("/api/auth/login", (req, res) -> {
            try {
                Map body = gson.fromJson(req.body(), Map.class);
                Map<String, String> user = tutorDAO.validarCredenciales((String)body.get("user"), (String)body.get("pass"));
                Map<String, Object> r = new HashMap<>();

                if (user != null) {
                    int estado = Integer.parseInt(user.get("estado"));
                    if (estado == 0) {
                        r.put("success", false);
                        r.put("mensaje", "⚠️ Cuenta REGISTRADA pero PENDIENTE de aprobación.");
                    } else {
                        r.put("success", true);
                        r.put("nombre", user.get("nombre"));
                        r.put("rol", user.get("rol"));
                        r.put("dni", user.get("dni"));
                    }
                } else {
                    r.put("success", false);
                    r.put("mensaje", "Credenciales incorrectas.");
                }

                return gson.toJson(r);
            } catch (Exception e) {
                e.printStackTrace();
                return gson.toJson(Map.of("success", false, "mensaje", "Error interno en Login"));
            }
        });

        // ==========================================
        // ✅ LOGIN INTELIGENTE (TUTOR O DOCENTE CON DNI)
        // ==========================================
        Spark.post("/api/login", (req, res) -> {
            try {
                Map body = gson.fromJson(req.body(), Map.class);

                System.out.println("📦 [API] Recibido: " + body);

                // 1. Recuperar datos (user O dni)
                String user = (String) body.get("user");
                if (user == null) user = (String) body.get("dni");
                String pass = (String) body.get("pass");

                Map<String, Object> r = new HashMap<>();

                // ---------------------------------------------------------
                // CASO A: Login Normal (Usuario y Contraseña)
                // ---------------------------------------------------------
                if (user != null && pass != null) {

                    // 1. Intentamos validar como TUTOR
                    Map<String, String> credTutor = tutorDAO.validarCredenciales(user, pass);
                    if (credTutor != null) {
                        int estado = Integer.parseInt(credTutor.get("estado"));
                        if (estado == 0) {
                            r.put("success", false);
                            r.put("mensaje", "Cuenta pendiente.");
                        } else {
                            r.put("success", true);
                            r.put("nombre", credTutor.get("nombre"));
                            r.put("rol", credTutor.get("rol"));
                            r.put("dni", credTutor.get("dni"));
                        }
                        return gson.toJson(r);
                    }

                    // 2. Intentamos validar como ADMIN (¡NUEVO!) 👮‍♂️
                    // Asumimos que tienes un método login en AdminDAO, si no, usa validación simple
                    if (adminDAO.login(user, pass)) {
                        r.put("success", true);
                        r.put("nombre", "Administrador Principal");
                        r.put("rol", "admin"); // <--- IMPORTANTE
                        r.put("dni", "00000000");
                        return gson.toJson(r);
                    }

                    // 3. Intentamos validar como DOCENTE (Opcional si quieres login con pass)
                    // (Aquí iría la lógica de docenteDAO.login(user, pass) si la tuvieras)

                    r.put("success", false);
                    r.put("mensaje", "Credenciales incorrectas.");
                }

                // ---------------------------------------------------------
                // CASO B: Login Rápido / Chatbot (SOLO DNI)
                // ---------------------------------------------------------
                else if (user != null) {

                    // 1. ¿ES UN TUTOR?
                    String infoTutor = tutorDAO.obtenerInfoCompleta(user);
                    if (!infoTutor.contains("No encontrado")) {
                        model.Tutor t = tutorDAO.obtenerPorDNI(user);
                        r.put("success", true);
                        r.put("nombre", (t != null) ? t.getNombres() : "Padre de Familia");
                        r.put("rol", "tutor");
                        r.put("dni", user);
                        return gson.toJson(r);
                    }

                    // 2. ¿ES UN DOCENTE? (¡ESTO ES LO NUEVO!) 🆕
                    String nombreDocente = docenteDAO.obtenerNombreDocente(user);
                    if (nombreDocente != null) {
                        r.put("success", true);
                        r.put("nombre", nombreDocente);
                        r.put("rol", "docente"); // <--- Importante para que el JS sepa a dónde redirigir
                        r.put("dni", user);
                        return gson.toJson(r);
                    }

                    // 3. SI NO ES NINGUNO
                    r.put("success", false);
                    r.put("mensaje", "DNI no encontrado en el sistema.");
                }

                else {
                    r.put("success", false);
                    r.put("mensaje", "Faltan datos.");
                }

                return gson.toJson(r);

            } catch (Exception e) {
                e.printStackTrace();
                return gson.toJson(Map.of("success", false, "mensaje", "Error interno"));
            }
        });




        // --- RUTA PARA QUE EL PADRE VEA ASISTENCIA ---
        Spark.get("/api/asistencia/tutor/:dni", (req, res) -> {
            String dni = req.params("dni");
            // Usamos el nuevo método que acabamos de crear
            List<Map<String, Object>> historial = docenteDAO.obtenerHistorialAsistenciaTutor(dni);

            Map<String, Object> respuesta = new HashMap<>();
            if (historial.isEmpty()) {
                respuesta.put("success", false);
                respuesta.put("message", "No hay registros");
            } else {
                respuesta.put("success", true);
                respuesta.put("data", historial);
            }
            return gson.toJson(respuesta);
        });

        // ==========================================
        // RUTAS DE REGISTRO
        // ==========================================
        Spark.post("/api/auth/register/tutor", (req, res) -> {
            try {
                Map body = gson.fromJson(req.body(), Map.class);
                int idGrado = 1;
                try {
                    idGrado = Double.valueOf(body.get("idGrado").toString()).intValue();
                } catch (Exception var7) { }

                boolean ok = tutorDAO.registrarFamiliaCompleta((String)body.get("nombreT"), (String)body.get("dniT"), (String)body.get("telT"), (String)body.get("user"), (String)body.get("pass"), (String)body.get("nombreHijo"), (String)body.get("dniHijo"), idGrado);
                return ok ? gson.toJson(Map.of("success", true, "mensaje", "¡Registro exitoso! Pendiente de aprobación.")) : gson.toJson(Map.of("success", false, "mensaje", "Error: DNI o Usuario ya existen."));
            } catch (Exception e) {
                e.printStackTrace();
                return gson.toJson(Map.of("success", false, "mensaje", "Error interno: " + e.getMessage()));
            }
        });

        Spark.post("/api/auth/register/docente", (req, res) -> {
            try {
                Map body = gson.fromJson(req.body(), Map.class);
                boolean ok = docenteDAO.registrarNuevoDocente((String)body.get("nombres"), (String)body.get("apellidos"), (String)body.get("dni"), (String)body.get("user"), (String)body.get("pass"));
                return gson.toJson(Map.of("success", ok, "mensaje", ok ? "Registro exitoso." : "Error: Datos duplicados."));
            } catch (Exception e) {
                e.printStackTrace();
                return gson.toJson(Map.of("success", false, "mensaje", "Error interno"));
            }
        });

        // ==========================================
        // RUTAS DOCENTE
        // ==========================================
        Spark.get("/api/docente/cursos/:dni", (req, res) -> gson.toJson(docenteDAO.obtenerCursosPorDocente(req.params(":dni"))));
        Spark.get("/api/docente/alumnos/:id", (req, res) -> gson.toJson(docenteDAO.obtenerAlumnosPorCurso(Integer.parseInt(req.params(":id")))));

        Spark.get("/api/docente/notas/:idCurso/:bimestre", (req, res) -> {
            int c = Integer.parseInt(req.params(":idCurso"));
            int b = Integer.parseInt(req.params(":bimestre"));
            return gson.toJson(docenteDAO.obtenerNotasCursoBimestre(c, b));
        });

        Spark.get("/api/docente/asistencia/:idCurso/:fecha", (req, res) -> {
            int c = Integer.parseInt(req.params(":idCurso"));
            String f = req.params(":fecha");
            return gson.toJson(docenteDAO.obtenerAsistenciaCursoFecha(c, f));
        });

        Spark.post("/api/docente/guardarNotasMasiva", (req, res) -> {
            Type listType = (new TypeToken<ArrayList<Map<String, Object>>>() {}).getType();
            List<Map<String, Object>> lista = gson.fromJson(req.body(), listType);
            boolean ok = docenteDAO.guardarNotasMasiva(lista);
            return gson.toJson(Map.of("success", ok));
        });

        Spark.post("/api/asistencia/guardarMasiva", (req, res) -> {
            Type listType = (new TypeToken<ArrayList<Map<String, Object>>>() {}).getType();
            List<Map<String, Object>> lista = gson.fromJson(req.body(), listType);

            boolean ok = docenteDAO.guardarAsistenciaMasiva(lista);

            return gson.toJson(Map.of("success", ok));
        });

        Spark.post("/api/observaciones/guardar", (req, res) -> {
            Map body = gson.fromJson(req.body(), Map.class);

            String dniAlumno = (String)body.get("dniAlumno");
            int idEst = docenteDAO.obtenerIdEstudiantePorDni(dniAlumno);

            if (idEst > 0) {
                boolean ok = obsDAO.registrarObservacion(
                        (String)body.get("titulo"),
                        (String)body.get("descripcion"),
                        (String)body.get("nivel"),
                        idEst,
                        1 // ID del docente (puedes ajustarlo si tienes el ID real en la sesión)
                );
                return gson.toJson(Map.of("success", ok));
            } else {
                return gson.toJson(Map.of("success", false, "mensaje", "Alumno no encontrado con ese DNI."));
            }
        });

        // ==========================================
        // RUTAS TUTOR Y PERFIL
        // ==========================================
        Spark.post("/api/perfil/actualizar", (req, res) -> {
            Map body = gson.fromJson(req.body(), Map.class);
            boolean ok = tutorDAO.actualizarTelefono((String)body.get("dni"), (String)body.get("telefono"));
            return gson.toJson(Map.of("success", ok));
        });

        Spark.get("/api/notas/:dni", (req, res) -> {
            int idEst = notaDAO.obtenerIdEstudiantePorDniPadre(req.params(":dni"));
            return gson.toJson(notaDAO.obtenerNotasPorEstudiante(idEst));
        });

        Spark.get("/api/asistencia/ver/:dni", (req, res) -> {
            int idEst = notaDAO.obtenerIdEstudiantePorDniPadre(req.params(":dni"));
            return gson.toJson(asistDAO.obtenerAsistenciaPorEstudiante(idEst));
        });

        // --- RUTA ACTUALIZADA: OBSERVACIONES POR TUTOR (Incluye nombre del hijo) ---
        Spark.get("/api/observaciones/ver/:dni", (req, res) -> {
            String dni = req.params(":dni");
            // Usamos el nuevo método que cruza tablas para sacar el nombre del hijo
            return gson.toJson(obsDAO.listarPorTutor(dni));
        });

        // ==========================================
        // RUTAS ADMINISTRADOR
        // ==========================================
        Spark.get("/api/admin/stats", (req, res) -> gson.toJson(adminDAO.obtenerEstadisticas()));
        Spark.get("/api/admin/pendientes", (req, res) -> gson.toJson(adminDAO.listarPendientes()));

        Spark.post("/api/admin/aprobarTutor", (req, res) -> {
            Map body = gson.fromJson(req.body(), Map.class);
            int id = ((Double)body.get("id")).intValue();
            boolean ok = adminDAO.aprobarTutor(id);
            return gson.toJson(Map.of("success", ok));
        });

        Spark.get("/api/admin/alumnos", (req, res) -> gson.toJson(adminDAO.listarAlumnosGeneral()));
        Spark.get("/api/admin/tutores", (req, res) -> gson.toJson(adminDAO.listarTutoresGeneral()));
        Spark.get("/api/admin/docentes", (req, res) -> gson.toJson(adminDAO.listarDocentes()));
        Spark.get("/api/admin/cursos", (req, res) -> gson.toJson(adminDAO.listarCursosConDocente()));

        Spark.post("/api/admin/asignar", (req, res) -> {
            Map body = gson.fromJson(req.body(), Map.class);
            int c = ((Double)body.get("idCurso")).intValue();
            int d = ((Double)body.get("idDocente")).intValue();
            boolean ok = adminDAO.asignarDocente(c, d);
            return gson.toJson(Map.of("success", ok));
        });
    }
}