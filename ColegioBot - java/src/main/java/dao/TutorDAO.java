package dao;

import org.hibernate.Session;
import org.hibernate.Transaction;
import model.Tutor;
import util.HibernateUtil;
import java.util.List;      // NECESARIO para la lista de hijos
import java.util.ArrayList; // NECESARIO para inicializar listas
import java.util.Map;
import java.util.HashMap;

public class TutorDAO {

    // =========================================================================
    //  1. MÉTODO PARA EL CHATBOT (IA) - ¡ESTE ES EL QUE ARREGLA TU ERROR!
    // =========================================================================
    public String obtenerInfoCompleta(String dniTutor) {
        String dniLimpio = dniTutor != null ? dniTutor.trim() : "";

        System.out.println("🔎 [DEBUG] Buscando Tutor en BD con DNI: '" + dniLimpio + "'");

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            // PASO A: Buscamos SOLAMENTE al Tutor (Sin unir tablas todavía)
            // Esto evita que falle si el tutor no tiene hijos asignados
            String sqlTutor = "SELECT nombres, id_tutor FROM tutores WHERE dni = :d";

            Object resRaw = session.createNativeQuery(sqlTutor)
                    .setParameter("d", dniLimpio)
                    .uniqueResult();

            if (resRaw == null) {
                System.out.println("❌ [DEBUG] SQL no encontró nada en la tabla 'tutores'.");
                return "No encontrado (El DNI " + dniLimpio + " no figura como Tutor).";
            } else {
                System.out.println("✅ [DEBUG] ¡Encontrado!");
            }

            Object[] tutorRes = (Object[]) resRaw;
            String nombreTutor = (String) tutorRes[0];
            int idTutor = ((Number) tutorRes[1]).intValue();

            // PASO B: Ahora buscamos a sus hijos por separado
            StringBuilder info = new StringBuilder();
            info.append("El usuario identificado es ").append(nombreTutor).append(" (Padre/Tutor). ");

            String sqlHijos = "SELECT nombres, id_grado FROM estudiantes WHERE id_tutor = :idt";
            List<Object[]> hijos = session.createNativeQuery(sqlHijos)
                    .setParameter("idt", idTutor)
                    .list();

            if (hijos.isEmpty()) {
                info.append("Nota: Este tutor está registrado pero aún no tiene hijos asignados en el sistema.");
            } else {
                info.append("Tiene los siguientes hijos registrados: ");
                for (Object[] hijo : hijos) {
                    String nomHijo = (String) hijo[0];
                    info.append(nomHijo).append(", ");
                }
            }

            return info.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error interno al buscar datos: " + e.getMessage();
        }
    }

    // =========================================================================
    //  2. LOGIN (VALIDAR CREDENCIALES)
    // =========================================================================
    public Map<String, String> validarCredenciales(String user, String pass) {
        System.out.println("🔐 [LOGIN DEBUG] Intentando entrar con User: '" + user + "' y Pass: '" + pass + "'");

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = "SELECT id_usuario, id_rol, estado FROM usuarios WHERE username=:u AND password=:p";

            Object resRaw = session.createNativeQuery(sql).setParameter("u", user).setParameter("p", pass).uniqueResult();

            // --- ESPÍA DE RESULTADO ---
            if (resRaw == null) {
                System.out.println("❌ [LOGIN DEBUG] La BD dice: Usuario o contraseña incorrectos en tabla 'usuarios'.");
                return null;
            }
            System.out.println("✅ [LOGIN DEBUG] ¡Usuario encontrado en tabla 'usuarios'!");
            //

            Object[] res = (Object[]) resRaw;
            int idUser = ((Number) res[0]).intValue();
            int idRol  = ((Number) res[1]).intValue();
            int estado = ((Number) res[2]).intValue();

            Map<String, String> datos = new HashMap<>();
            datos.put("estado", String.valueOf(estado));

            if (idRol == 3) { // Tutor
                Object rawT = session.createNativeQuery("SELECT nombres, dni FROM tutores WHERE id_usuario = :id").setParameter("id", idUser).uniqueResult();
                if (rawT != null) {
                    Object[] t = (Object[]) rawT;
                    datos.put("nombre", (String) t[0]);
                    datos.put("dni", (String) t[1]);
                    datos.put("rol", "tutor");
                    return datos;
                }
            } else if (idRol == 2) { // Docente
                Object rawD = session.createNativeQuery("SELECT nombres, dni FROM docentes WHERE id_usuario = :id").setParameter("id", idUser).uniqueResult();
                if (rawD != null) {
                    Object[] d = (Object[]) rawD;
                    datos.put("nombre", (String) d[0]);
                    datos.put("dni", (String) d[1]);
                    datos.put("rol", "docente");
                    return datos;
                }
            } else if (idRol == 1) { // Admin
                datos.put("nombre", "Administrador");
                datos.put("dni", "00000000");
                datos.put("rol", "admin");
                return datos;
            }
            return null;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    // =========================================================================
    //  3. REGISTRO (CORREGIDO: EVITA EL ERROR DE CONEXIÓN CERRADA)
    // =========================================================================
    public boolean registrarFamiliaCompleta(String nomT, String dniT, String telT, String user, String pass,
                                            String nomHijo, String dniHijo, int idGrado) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            // -----------------------------------------------------------
            // ✅ PASO 0: VALIDACIÓN PREVIA (Esto arregla tu error)
            // -----------------------------------------------------------
            // Verificamos si el USUARIO ya existe
            Number countUser = (Number) session.createNativeQuery("SELECT count(*) FROM usuarios WHERE username = :u")
                    .setParameter("u", user)
                    .uniqueResult();

            if (countUser != null && countUser.intValue() > 0) {
                System.err.println("⚠️ Error: El nombre de usuario '" + user + "' ya está en uso.");
                return false; // Salimos elegantemente sin romper la conexión
            }

            // Verificamos si el DNI del TUTOR ya existe
            Number countDni = (Number) session.createNativeQuery("SELECT count(*) FROM tutores WHERE dni = :d")
                    .setParameter("d", dniT)
                    .uniqueResult();

            if (countDni != null && countDni.intValue() > 0) {
                System.err.println("⚠️ Error: El DNI '" + dniT + "' ya está registrado.");
                return false;
            }
            // -----------------------------------------------------------


            // AHORA SÍ, PROCEDEMOS A GUARDAR
            tx = session.beginTransaction();

            // A. Insertar Usuario
            String sqlUser = "INSERT INTO usuarios (username, password, id_rol, estado) VALUES (:u, :p, 3, 0)";
            session.createNativeQuery(sqlUser)
                    .setParameter("u", user)
                    .setParameter("p", pass)
                    .executeUpdate();

            // Obtener ID generado
            Number idUserNum = (Number) session.createNativeQuery("SELECT LAST_INSERT_ID()").uniqueResult();
            int idUser = idUserNum.intValue();

            // B. Insertar Tutor
            // OJO: Asegúrate si tu columna en BD se llama 'celular' o 'telefono'. Aquí usas 'celular'.
            String sqlTutor = "INSERT INTO tutores (nombres, dni, telefono, id_usuario) VALUES (:n, :d, :c, :uid)";
            session.createNativeQuery(sqlTutor)
                    .setParameter("n", nomT)
                    .setParameter("d", dniT)
                    .setParameter("c", telT)
                    .setParameter("uid", idUser)
                    .executeUpdate();

            Number idTutorNum = (Number) session.createNativeQuery("SELECT LAST_INSERT_ID()").uniqueResult();
            int idTutor = idTutorNum.intValue();

            // C. Insertar Estudiante
            String sqlEst = "INSERT INTO estudiantes (nombres, dni, id_grado, id_tutor) VALUES (:n, :d, :g, :t)";
            session.createNativeQuery(sqlEst)
                    .setParameter("n", nomHijo)
                    .setParameter("d", dniHijo)
                    .setParameter("g", idGrado)
                    .setParameter("t", idTutor)
                    .executeUpdate();

            tx.commit();
            return true;

        } catch (Exception e) {
            // Manejo seguro del Rollback
            if (tx != null && tx.getStatus().canRollback()) {
                try { tx.rollback(); } catch (Exception ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        }
    }





    // 4. ACTUALIZAR TELÉFONO (CORREGIDO)
    public boolean actualizarTelefono(String dni, String nuevoTelefono) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            // CAMBIO AQUÍ: Cambiamos 'celular' por 'telefono'
            // Si tu base de datos usa otro nombre, pon ese nombre aquí.
            int c = session.createNativeQuery("UPDATE tutores SET telefono=:t WHERE dni=:d")
                    .setParameter("t", nuevoTelefono)
                    .setParameter("d", dni)
                    .executeUpdate();

            tx.commit();
            return c > 0;
        } catch (Exception e) {
            if(tx!=null && tx.isActive()) tx.rollback(); // Pequeña mejora para evitar el otro error de "closed"
            e.printStackTrace();
            return false;
        }
    }

    // 5. HELPER SIMPLE
    public Tutor obtenerPorDNI(String dni) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createNativeQuery("SELECT * FROM tutores WHERE dni = :d", Tutor.class)
                    .setParameter("d", dni).uniqueResult();
        } catch (Exception e) { return null; }
    }
}