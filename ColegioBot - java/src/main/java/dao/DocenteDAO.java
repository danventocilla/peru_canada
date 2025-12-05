package dao;

import org.hibernate.Session;
import org.hibernate.Transaction;
import util.HibernateUtil;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class DocenteDAO {

    // 1. REGISTRAR DOCENTE
    public boolean registrarNuevoDocente(String nombres, String apellidos, String dni, String user, String pass) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            // A. Crear Usuario (ESTADO 0 = PENDIENTE)
            String sqlUser = "INSERT INTO usuarios (username, password, id_rol, estado) VALUES (:u, :p, 2, 0)";
            session.createNativeQuery(sqlUser)
                    .setParameter("u", user)
                    .setParameter("p", pass)
                    .executeUpdate();

            // B. Obtener ID
            Number idUserNum = (Number) session.createNativeQuery("SELECT LAST_INSERT_ID()").uniqueResult();
            int idUser = idUserNum.intValue();

            // C. Crear Docente
            String sqlDoc = "INSERT INTO docentes (nombres, apellidos, dni, id_usuario) VALUES (:n, :a, :d, :uid)";
            session.createNativeQuery(sqlDoc)
                    .setParameter("n", nombres)
                    .setParameter("a", apellidos)
                    .setParameter("d", dni)
                    .setParameter("uid", idUser)
                    .executeUpdate();

            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return false;
        }
    }

    // 2. OBTENER CURSOS
    public List<Map<String, Object>> obtenerCursosPorDocente(String dni) {
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = "SELECT c.id_curso, c.nombre_curso, g.nombre_grado, g.seccion " +
                    "FROM cursos c " +
                    "JOIN grados g ON c.id_grado = g.id_grado " +
                    "JOIN docentes d ON c.id_docente = d.id_docente " +
                    "WHERE d.dni = :dni";

            List<Object[]> res = session.createNativeQuery(sql)
                    .setParameter("dni", dni)
                    .list();

            for (Object[] row : res) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", row[0]);
                map.put("nombre", row[1] + " (" + row[2] + " " + row[3] + ")");
                lista.add(map);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    // 3. OBTENER ALUMNOS
    public List<Map<String, Object>> obtenerAlumnosPorCurso(int idCurso) {
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = "SELECT e.id_estudiante, e.nombres, e.dni " +
                    "FROM estudiantes e " +
                    "JOIN grados g ON e.id_grado = g.id_grado " +
                    "JOIN cursos c ON c.id_grado = g.id_grado " +
                    "WHERE c.id_curso = :id";

            List<Object[]> res = session.createNativeQuery(sql)
                    .setParameter("id", idCurso)
                    .list();

            for (Object[] row : res) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", row[0]);
                map.put("nombre", row[1]);
                map.put("dni", row[2]);
                lista.add(map);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    // 4. GUARDAR NOTA INDIVIDUAL
    public boolean guardarNota(int idEstudiante, int idCurso, int bimestre, double valor) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            String sqlCheck = "SELECT id_nota FROM notas WHERE id_estudiante=:e AND id_curso=:c AND bimestre=:b";
            Object idExistente = session.createNativeQuery(sqlCheck)
                    .setParameter("e", idEstudiante)
                    .setParameter("c", idCurso)
                    .setParameter("b", bimestre)
                    .uniqueResult();

            if (idExistente != null) {
                session.createNativeQuery("UPDATE notas SET valor=:v WHERE id_nota=:id")
                        .setParameter("v", valor).setParameter("id", idExistente).executeUpdate();
            } else {
                session.createNativeQuery("INSERT INTO notas (valor, bimestre, id_estudiante, id_curso) VALUES (:v, :b, :e, :c)")
                        .setParameter("v", valor).setParameter("b", bimestre).setParameter("e", idEstudiante).setParameter("c", idCurso).executeUpdate();
            }
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return false;
        }
    }

    // 5. GUARDAR NOTAS MASIVAS
    public boolean guardarNotasMasiva(List<Map<String, Object>> listaNotas) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            for (Map<String, Object> item : listaNotas) {
                // Usamos Number para evitar errores de casteo entre Integer y Double
                int idEst = ((Number) item.get("idEstudiante")).intValue();
                int idCurso = ((Number) item.get("idCurso")).intValue();
                int bimestre = ((Number) item.get("bimestre")).intValue();

                Object notaObj = item.get("nota");
                if (notaObj == null) notaObj = item.get("valorNota");
                double valor = Double.parseDouble(notaObj.toString());

                String sqlCheck = "SELECT id_nota FROM notas WHERE id_estudiante=:e AND id_curso=:c AND bimestre=:b";
                Object idExistente = session.createNativeQuery(sqlCheck)
                        .setParameter("e", idEst).setParameter("c", idCurso).setParameter("b", bimestre).uniqueResult();

                if (idExistente != null) {
                    session.createNativeQuery("UPDATE notas SET valor=:v WHERE id_nota=:id")
                            .setParameter("v", valor).setParameter("id", idExistente).executeUpdate();
                } else {
                    session.createNativeQuery("INSERT INTO notas (valor, bimestre, id_estudiante, id_curso) VALUES (:v, :b, :e, :c)")
                            .setParameter("v", valor).setParameter("b", bimestre).setParameter("e", idEst).setParameter("c", idCurso).executeUpdate();
                }
            }
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return false;
        }
    }

    // =============================================================
    // ✅ 6. GUARDAR ASISTENCIA MASIVA (ESTE ERA EL QUE FALTABA)
    // =============================================================
    public boolean guardarAsistenciaMasiva(List<Map<String, Object>> listaAsistencia) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            for (Map<String, Object> item : listaAsistencia) {
                // Casteo seguro usando Number
                int idEstudiante = ((Number) item.get("idEstudiante")).intValue();
                int idCurso = ((Number) item.get("idCurso")).intValue(); // ¡AQUÍ ESTÁ LA CLAVE!
                String estado = (String) item.get("estado");
                String fechaStr = (String) item.get("fecha");

                // 1. VERIFICAR SI YA EXISTE (Filtrando TAMBIÉN por curso)
                String sqlCheck = "SELECT id_asistencia FROM asistencia WHERE id_estudiante = :id AND fecha = :f AND id_curso = :c";

                Object existeRaw = session.createNativeQuery(sqlCheck)
                        .setParameter("id", idEstudiante)
                        .setParameter("f", fechaStr)
                        .setParameter("c", idCurso) // Filtro nuevo
                        .uniqueResult();

                if (existeRaw != null) {
                    // 2. SI EXISTE, ACTUALIZAMOS
                    int idAsistencia = ((Number) existeRaw).intValue();
                    String sqlUpdate = "UPDATE asistencia SET estado = :e WHERE id_asistencia = :idA";
                    session.createNativeQuery(sqlUpdate)
                            .setParameter("e", estado)
                            .setParameter("idA", idAsistencia)
                            .executeUpdate();
                } else {
                    // 3. SI NO EXISTE, INSERTAMOS (Con id_curso)
                    String sqlInsert = "INSERT INTO asistencia (id_estudiante, id_curso, fecha, estado) VALUES (:id, :c, :f, :e)";
                    session.createNativeQuery(sqlInsert)
                            .setParameter("id", idEstudiante)
                            .setParameter("c", idCurso) // Dato nuevo
                            .setParameter("f", fechaStr)
                            .setParameter("e", estado)
                            .executeUpdate();
                }
            }

            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return false;
        }
    }

    // 7. OBTENER NOMBRE DOCENTE
    public String obtenerNombreDocente(String dni) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = "SELECT nombres FROM docentes WHERE dni = :d";
            return (String) session.createNativeQuery(sql)
                    .setParameter("d", dni)
                    .uniqueResult();
        } catch (Exception e) { return null; }
    }

    // 8. OBTENER NOTAS (Para llenar los inputs)
    public List<Map<String, Object>> obtenerNotasCursoBimestre(int idCurso, int bimestre) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id_estudiante, valor FROM notas WHERE id_curso = :idCurso AND bimestre = :bimestre";

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Object[]> res = session.createNativeQuery(sql)
                    .setParameter("idCurso", idCurso)
                    .setParameter("bimestre", bimestre)
                    .list();

            for (Object[] row : res) {
                Map<String, Object> map = new HashMap<>();
                map.put("idEstudiante", row[0]);
                map.put("nota", row[1]);
                lista.add(map);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    // 9. OBTENER ASISTENCIA (Para pintar botones)
    public List<Map<String, Object>> obtenerAsistenciaCursoFecha(int idCurso, String fecha) {
        List<Map<String, Object>> lista = new ArrayList<>();
        // Este ya estaba bien, filtrando por curso
        String sql = "SELECT id_estudiante, estado FROM asistencia WHERE id_curso = :idCurso AND fecha = :fecha";

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Object[]> res = session.createNativeQuery(sql)
                    .setParameter("idCurso", idCurso)
                    .setParameter("fecha", fecha)
                    .list();

            for (Object[] row : res) {
                Map<String, Object> map = new HashMap<>();
                map.put("idEstudiante", row[0]);
                map.put("estado", row[1]);
                lista.add(map);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    // =============================================================
    // ✅ 10. HISTORIAL ASISTENCIA PARA EL TUTOR (CON CURSO)
    // =============================================================
    public List<Map<String, Object>> obtenerHistorialAsistenciaTutor(String dniTutor) {
        List<Map<String, Object>> lista = new ArrayList<>();
        // Usamos LEFT JOIN para que salgan incluso las asistencias antiguas que no tenían curso
        String sql = "SELECT c.nombre_curso, a.fecha, a.estado, e.nombres " +
                "FROM asistencia a " +
                "JOIN estudiantes e ON a.id_estudiante = e.id_estudiante " +
                "JOIN tutores t ON e.id_tutor = t.id_tutor " +
                "LEFT JOIN cursos c ON a.id_curso = c.id_curso " + // <--- ESTO ES LA CLAVE
                "WHERE t.dni = :dni " +
                "ORDER BY a.fecha DESC, c.nombre_curso ASC";

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Object[]> res = session.createNativeQuery(sql)
                    .setParameter("dni", dniTutor)
                    .list();

            for (Object[] row : res) {
                Map<String, Object> map = new HashMap<>();
                // Si el curso es NULL (registros viejos), ponemos "General"
                map.put("curso", row[0] != null ? row[0] : "General");
                map.put("fecha", row[1].toString());
                map.put("estado", row[2]);
                map.put("hijo", row[3]);
                lista.add(map);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    // =============================================================
    // ✅ 11. BUSCAR ID ESTUDIANTE POR SU PROPIO DNI (Corrección)
    // =============================================================
    public int obtenerIdEstudiantePorDni(String dniEstudiante) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Buscamos directamente en la tabla 'estudiantes'
            String sql = "SELECT id_estudiante FROM estudiantes WHERE dni = :d";

            Object result = session.createNativeQuery(sql)
                    .setParameter("d", dniEstudiante)
                    .uniqueResult();

            // Si lo encuentra devuelve el ID, si no devuelve -1
            return result != null ? ((Number) result).intValue() : -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }




}