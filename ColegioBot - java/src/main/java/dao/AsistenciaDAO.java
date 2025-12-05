package dao;

import org.hibernate.Session;
import org.hibernate.Transaction;
import util.HibernateUtil;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;

public class AsistenciaDAO {

    // 1. REGISTRAR ASISTENCIA INDIVIDUAL
    public boolean registrarAsistencia(int idEstudiante, String estado) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            String fechaHoy = LocalDate.now().toString(); // Fecha automática del servidor

            // Verificar si ya existe registro hoy
            String sqlCheck = "SELECT id_asistencia FROM asistencia WHERE id_estudiante = :id AND fecha = :f";
            Object existe = session.createNativeQuery(sqlCheck)
                    .setParameter("id", idEstudiante)
                    .setParameter("f", fechaHoy)
                    .uniqueResult();

            if (existe != null) {
                // UPDATE: Si ya existe, actualizamos el estado
                String sqlUpdate = "UPDATE asistencia SET estado = :e WHERE id_asistencia = :aid";
                session.createNativeQuery(sqlUpdate)
                        .setParameter("e", estado)
                        .setParameter("aid", existe)
                        .executeUpdate();
            } else {
                // INSERT: Si no existe, creamos uno nuevo
                String sqlInsert = "INSERT INTO asistencia (fecha, estado, id_estudiante) VALUES (:f, :e, :id)";
                session.createNativeQuery(sqlInsert)
                        .setParameter("f", fechaHoy)
                        .setParameter("e", estado)
                        .setParameter("id", idEstudiante)
                        .executeUpdate();
            }

            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return false;
        }
    }

    // 2. REGISTRAR ASISTENCIA MASIVA (Recibe Lista de Mapas, no de Objetos)
    public boolean registrarAsistenciaMasiva(List<Map<String, Object>> listaAsistencias) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            String fechaHoy = LocalDate.now().toString();

            for (Map<String, Object> item : listaAsistencias) {
                // Parseo seguro de datos desde el JSON (Map)
                int idEst = ((Double) item.get("idEstudiante")).intValue();
                String estado = (String) item.get("estado");

                // Verificar existencia
                String sqlCheck = "SELECT id_asistencia FROM asistencia WHERE id_estudiante = :id AND fecha = :f";
                Object idExistente = session.createNativeQuery(sqlCheck)
                        .setParameter("id", idEst)
                        .setParameter("f", fechaHoy)
                        .uniqueResult();

                if (idExistente != null) {
                    session.createNativeQuery("UPDATE asistencia SET estado = :e WHERE id_asistencia = :aid")
                            .setParameter("e", estado)
                            .setParameter("aid", idExistente)
                            .executeUpdate();
                } else {
                    session.createNativeQuery("INSERT INTO asistencia (fecha, estado, id_estudiante) VALUES (:f, :e, :id)")
                            .setParameter("f", fechaHoy)
                            .setParameter("e", estado)
                            .setParameter("id", idEst)
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
    // 3. OBTENER ASISTENCIA POR CURSO Y FECHA (NUEVO)
    public List<Map<String, Object>> obtenerAsistenciaPorCursoYFecha(int idCurso, String fecha) {
        List<Map<String, Object>> lista = new ArrayList<>();

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql =
                    "SELECT a.id_asistencia, e.id_estudiante, e.nombres, a.estado " +
                            "FROM asistencia a " +
                            "JOIN estudiantes e ON a.id_estudiante = e.id_estudiante " +
                            "JOIN grados g ON e.id_grado = g.id_grado " +
                            "JOIN cursos c ON c.id_grado = g.id_grado " +
                            "WHERE c.id_curso = :id AND a.fecha = :f " +
                            "ORDER BY e.nombres ASC";

            List<Object[]> res = session.createNativeQuery(sql)
                    .setParameter("id", idCurso)
                    .setParameter("f", fecha)
                    .list();

            for (Object[] row : res) {
                Map<String, Object> map = new HashMap<>();
                map.put("idAsistencia", row[0]);
                map.put("idEstudiante", row[1]);
                map.put("nombre", row[2]);
                map.put("estado", row[3]); // Presente / Falta / Tardanza
                lista.add(map);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return lista;
    }

    // 4. OBTENER HISTORIAL (Para el Padre/Tutor)
    public List<Map<String, Object>> obtenerAsistenciaPorEstudiante(int idEstudiante) {
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Traemos las últimas 20 asistencias
            String sql = "SELECT fecha, estado FROM asistencia WHERE id_estudiante = :id ORDER BY fecha DESC LIMIT 20";

            List<Object[]> res = session.createNativeQuery(sql)
                    .setParameter("id", idEstudiante)
                    .list();

            for (Object[] row : res) {
                Map<String, Object> map = new HashMap<>();
                map.put("fecha", row[0].toString());
                map.put("estado", row[1]); // Presente, Tardanza, Falta
                lista.add(map);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    // 5. OBTENER ASISTENCIA DE LOS HIJOS (POR DNI DEL TUTOR) - ¡NUEVO!
    public List<Map<String, Object>> obtenerAsistenciaHijosPorTutor(String dniTutor) {
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Unimos 3 tablas: Asistencia <- Estudiante <- Tutor
            String sql =
                    "SELECT e.nombres, a.fecha, a.estado " +
                            "FROM asistencia a " +
                            "JOIN estudiantes e ON a.id_estudiante = e.id_estudiante " +
                            "JOIN tutores t ON e.id_tutor = t.id_tutor " +
                            "WHERE t.dni = :d " +
                            "ORDER BY a.fecha DESC LIMIT 20"; // Últimos 20 registros

            List<Object[]> res = session.createNativeQuery(sql)
                    .setParameter("d", dniTutor)
                    .list();

            for (Object[] row : res) {
                Map<String, Object> map = new HashMap<>();
                map.put("hijo", row[0]);
                map.put("fecha", row[1].toString());
                map.put("estado", row[2]);
                lista.add(map);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }


}