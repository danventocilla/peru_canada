package dao;

import org.hibernate.Session;
import util.HibernateUtil;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class NotaDAO {

    // 1. OBTENER ID ALUMNO POR DNI DEL PADRE
    // Este método es crucial: conecta el login del padre con los datos del hijo.
    public int obtenerIdEstudiantePorDniPadre(String dniTutor) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Consulta nativa que cruza Tutores -> Estudiantes
            String sql = "SELECT e.id_estudiante " +
                    "FROM estudiantes e " +
                    "JOIN tutores t ON e.id_tutor = t.id_tutor " +
                    "WHERE t.dni = :dni LIMIT 1";

            Object result = session.createNativeQuery(sql)
                    .setParameter("dni", dniTutor)
                    .uniqueResult();

            if (result != null) {
                return ((Number) result).intValue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0; // 0 indica "No encontrado"
    }

    // 2. LEER NOTAS (Para el Padre)
    public List<Map<String, Object>> obtenerNotasPorEstudiante(int idEstudiante) {
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Traemos Nombre del Curso, Bimestre y Valor
            String sql = "SELECT c.nombre_curso, n.bimestre, n.valor " +
                    "FROM notas n " +
                    "JOIN cursos c ON n.id_curso = c.id_curso " +
                    "WHERE n.id_estudiante = :id " +
                    "ORDER BY n.bimestre ASC, c.nombre_curso ASC";

            List<Object[]> res = session.createNativeQuery(sql)
                    .setParameter("id", idEstudiante)
                    .list();

            for (Object[] row : res) {
                Map<String, Object> map = new HashMap<>();
                map.put("nombreCurso", row[0]);
                map.put("bimestre", row[1]);
                map.put("valor", row[2]);
                lista.add(map);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }
}