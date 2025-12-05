package dao;

import org.hibernate.Session;
import org.hibernate.Transaction;
import util.HibernateUtil;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;

public class ObservacionDAO {

    // 1. REGISTRAR OBSERVACIÓN (SQL Nativo)
    public boolean registrarObservacion(String titulo, String descripcion, String nivel, int idEst, int idDoc) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            String fechaHoy = LocalDate.now().toString();

            String sql = "INSERT INTO observaciones (titulo, descripcion, nivel_gravedad, fecha, id_estudiante, id_docente) " +
                    "VALUES (:t, :d, :n, :f, :ie, :id)";

            session.createNativeQuery(sql)
                    .setParameter("t", titulo)
                    .setParameter("d", descripcion)
                    .setParameter("n", nivel)
                    .setParameter("f", fechaHoy)
                    .setParameter("ie", idEst)
                    .setParameter("id", idDoc)
                    .executeUpdate();

            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return false;
        }
    }

    // 2. LISTAR POR ESTUDIANTE (SQL Nativo)
    public List<Map<String, Object>> listarPorEstudiante(int idEstudiante) {
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = "SELECT o.fecha, o.titulo, o.nivel_gravedad, o.descripcion, d.nombres " +
                    "FROM observaciones o " +
                    "JOIN docentes d ON o.id_docente = d.id_docente " +
                    "WHERE o.id_estudiante = :id " +
                    "ORDER BY o.fecha DESC";

            List<Object[]> filas = session.createNativeQuery(sql)
                    .setParameter("id", idEstudiante)
                    .list();

            for (Object[] fila : filas) {
                Map<String, Object> map = new HashMap<>();
                map.put("fecha", fila[0].toString());
                map.put("titulo", fila[1]);
                map.put("gravedad", fila[2]);
                map.put("descripcion", fila[3]);
                map.put("docente", fila[4]);
                lista.add(map);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    // 3. LISTAR POR DNI DEL TUTOR (¡NUEVO!)
    // Une: Tutor -> Estudiantes -> Observaciones
    public List<Map<String, Object>> listarPorTutor(String dniTutor) {
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Seleccionamos la fecha, qué pasó (título), gravedad, detalle y EL NOMBRE DEL HIJO
            String sql = "SELECT o.fecha, o.titulo, o.nivel_gravedad, o.descripcion, e.nombres " +
                    "FROM observaciones o " +
                    "JOIN estudiantes e ON o.id_estudiante = e.id_estudiante " +
                    "JOIN tutores t ON e.id_tutor = t.id_tutor " +
                    "WHERE t.dni = :d " +
                    "ORDER BY o.fecha DESC";

            List<Object[]> filas = session.createNativeQuery(sql)
                    .setParameter("d", dniTutor)
                    .list();

            for (Object[] fila : filas) {
                Map<String, Object> map = new HashMap<>();
                map.put("fecha", fila[0].toString());
                map.put("titulo", fila[1]);
                map.put("gravedad", fila[2]);
                map.put("descripcion", fila[3]);
                map.put("hijo", fila[4]); // Importante para saber a cuál hijo regañaron
                lista.add(map);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }
}