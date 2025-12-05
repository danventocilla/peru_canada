package dao;

import org.hibernate.Session;
import org.hibernate.Transaction;
import util.HibernateUtil;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Data Access Object para operaciones de Administrador.
 * Gestiona estadísticas, listados y acciones de aprobación/asignación.
 */
public class AdminDAO {

    // =============================================================
    // ✅ 0. LOGIN DE ADMINISTRADOR (¡ESTO ES LO NUEVO!)
    // =============================================================
    public boolean login(String user, String pass) {
        // 1. LOGIN MAESTRO DE EMERGENCIA (Para que entres SI o SI)
        // Puedes usar usuario: "admin" y contraseña: "admin" o "admin123"
        if ("admin".equals(user) && ("admin123".equals(pass) || "admin".equals(pass))) {
            return true;
        }

        // 2. VALIDACIÓN EN BASE DE DATOS (Opcional, si tienes usuarios creados)
        // Asumimos que el id_rol = 3 es Admin (o el que uses en tu BD)
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = "SELECT COUNT(*) FROM usuarios WHERE username = :u AND password = :p AND id_rol = 3";
            Number count = (Number) session.createNativeQuery(sql)
                    .setParameter("u", user)
                    .setParameter("p", pass)
                    .uniqueResult();
            return count != null && count.intValue() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // =============================================================
    // 1. ESTADÍSTICAS
    // =============================================================
    public Map<String, Long> obtenerEstadisticas() {
        Map<String, Long> stats = new HashMap<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            // Función auxiliar para obtener conteos de manera segura
            java.util.function.Function<String, Long> getCount = sql -> {
                Number result = (Number) session.createNativeQuery(sql).uniqueResult();
                return result != null ? result.longValue() : 0L;
            };

            stats.put("estudiantes", getCount.apply("SELECT COUNT(*) FROM estudiantes"));
            stats.put("docentes", getCount.apply("SELECT COUNT(*) FROM docentes"));
            stats.put("tutores", getCount.apply("SELECT COUNT(*) FROM tutores"));
            stats.put("pendientes", getCount.apply("SELECT COUNT(*) FROM usuarios WHERE estado = 0"));

        } catch (Exception e) {
            e.printStackTrace();
            stats.put("estudiantes", 0L);
            stats.put("docentes", 0L);
            stats.put("tutores", 0L);
            stats.put("pendientes", 0L);
        }
        return stats;
    }

    // =============================================================
    // 2. DIRECTORIOS (Tablas completas)
    // =============================================================

    // Lista completa de Alumnos
    public List<Map<String, Object>> listarAlumnosGeneral() {
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Usamos LEFT JOIN para que traiga alumnos incluso si no tienen tutor asignado aún
            String sql = "SELECT e.nombres, g.nombre_grado, g.seccion, COALESCE(t.nombres, 'Sin Tutor') " +
                    "FROM estudiantes e " +
                    "LEFT JOIN grados g ON e.id_grado = g.id_grado " +
                    "LEFT JOIN tutores t ON e.id_tutor = t.id_tutor " +
                    "ORDER BY g.id_grado, e.nombres";

            List<Object[]> res = session.createNativeQuery(sql).list();
            for (Object[] row : res) {
                Map<String, Object> map = new HashMap<>();
                // Se asegura que grado y sección no sean nulos antes de concatenar
                String grado = (row[1] != null ? row[1].toString() : "S/G");
                String seccion = (row[2] != null ? row[2].toString() : "");

                map.put("nombre", row[0] != null ? row[0].toString() : "N/A");
                map.put("grado", grado + " " + seccion);
                map.put("tutor", row[3] != null ? row[3].toString() : "Sin Tutor");
                lista.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    // Lista completa de Tutores
    public List<Map<String, Object>> listarTutoresGeneral() {
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = "SELECT nombres, dni, telefono FROM tutores ORDER BY nombres";
            List<Object[]> res = session.createNativeQuery(sql).list();
            for (Object[] row : res) {
                Map<String, Object> map = new HashMap<>();
                map.put("nombre", row[0] != null ? row[0].toString() : "N/A");
                map.put("dni", row[1] != null ? row[1].toString() : "N/A");
                map.put("telefono", row[2] != null ? row[2].toString() : "-");
                lista.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    // =============================================================
    // 3. GESTIÓN DE CURSOS Y DOCENTES
    // =============================================================
    public List<Map<String, Object>> listarDocentes() {
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = "SELECT id_docente, CONCAT(nombres, ' ', apellidos) as nombre_completo FROM docentes";
            List<Object[]> res = session.createNativeQuery(sql).list();
            for (Object[] row : res) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", row[0] != null ? ((Number) row[0]).intValue() : 0);
                map.put("nombre", row[1] != null ? row[1].toString() : "N/A");
                lista.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<Map<String, Object>> listarCursosConDocente() {
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = "SELECT c.id_curso, c.nombre_curso, g.nombre_grado, g.seccion, " +
                    "COALESCE(CONCAT(d.nombres, ' ', d.apellidos), 'Sin Asignar') as docente " +
                    "FROM cursos c " +
                    "JOIN grados g ON c.id_grado = g.id_grado " +
                    "LEFT JOIN docentes d ON c.id_docente = d.id_docente " +
                    "ORDER BY g.id_grado, c.nombre_curso";
            List<Object[]> res = session.createNativeQuery(sql).list();
            for (Object[] row : res) {
                Map<String, Object> map = new HashMap<>();
                String curso = row[1] != null ? row[1].toString() : "N/A";
                String grado = row[2] != null ? row[2].toString() : "S/G";
                String seccion = row[3] != null ? row[3].toString() : "";

                map.put("id", row[0] != null ? ((Number) row[0]).intValue() : 0);
                map.put("curso", curso + " (" + grado + " " + seccion + ")");
                map.put("docente", row[4] != null ? row[4].toString() : "Sin Asignar");
                lista.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public boolean asignarDocente(int idCurso, int idDocente) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            String sql = "UPDATE cursos SET id_docente = :d WHERE id_curso = :c";
            session.createNativeQuery(sql).setParameter("d", idDocente).setParameter("c", idCurso).executeUpdate();
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return false;
        }
    }

    // =============================================================
    // 4. APROBACIÓN DE USUARIOS PENDIENTES
    // =============================================================
    public List<Map<String, Object>> listarPendientes() {
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // UNION para traer Tutores y Docentes con estado = 0
            String sql = """
                SELECT u.id_usuario, t.nombres, t.dni, u.username, 'Tutor' as rol
                FROM tutores t JOIN usuarios u ON t.id_usuario = u.id_usuario WHERE u.estado = 0
                UNION
                SELECT u.id_usuario, d.nombres, d.dni, u.username, 'Docente' as rol
                FROM docentes d JOIN usuarios u ON d.id_usuario = u.id_usuario WHERE u.estado = 0
            """;

            List<Object[]> resultados = session.createNativeQuery(sql).list();
            for (Object[] row : resultados) {
                Map<String, Object> map = new HashMap<>();
                // El ID es el id_usuario, que es el que necesitamos para aprobar.
                map.put("id_tutor", row[0] != null ? ((Number) row[0]).intValue() : 0);
                map.put("nombres", row[1] != null ? row[1].toString() : "N/A");
                map.put("dni", row[2] != null ? row[2].toString() : "N/A");
                String usuario = row[3] != null ? row[3].toString() : "";
                String rol = row[4] != null ? row[4].toString() : "Usuario";

                map.put("usuario", usuario + " (" + rol + ")");
                lista.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public boolean aprobarTutor(int idUsuario) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            String sql = "UPDATE usuarios SET estado = 1 WHERE id_usuario = :id";

            int filas = session.createNativeQuery(sql)
                    .setParameter("id", idUsuario)
                    .executeUpdate();

            tx.commit();
            return filas > 0;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return false;
        }
    }
}