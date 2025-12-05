package dao;

import model.Usuario;
import util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import java.util.List;

public class UsuarioDAO {

    // 1. LOGIN MEJORADO: Solo deja pasar si estado = 1 (Activo)
    public Usuario validarLogin(String user, String pass) {
        Transaction transaction = null;
        Usuario usuario = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // HQL: Hibernate Query Language (Usamos nombres de Clases, no de tablas)
            String hql = "FROM Usuario U WHERE U.username = :username AND U.password = :password AND U.estado = 1";
            Query<Usuario> query = session.createQuery(hql, Usuario.class);
            query.setParameter("username", user);
            query.setParameter("password", pass);
            usuario = query.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return usuario;
    }

    // 2. REGISTRAR USUARIO (Por defecto entra con estado 0 si así lo configuras en el objeto)
    public void registrarUsuario(Usuario usuario) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(usuario);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    // 3. LISTAR PENDIENTES (Para el Dashboard del Admin)
    public List<Usuario> listarPendientes() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Trae todos los usuarios con estado 0
            return session.createQuery("FROM Usuario WHERE estado = 0", Usuario.class).list();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 4. DAR VISTO BUENO (Aprobar)
    public boolean aprobarUsuario(int idUsuario) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            Usuario usuario = session.get(Usuario.class, idUsuario);
            if (usuario != null) {
                usuario.setEstado(1); // Cambiamos a ACTIVO
                session.update(usuario);
                transaction.commit();
                return true;
            }
            return false;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            return false;
        }
    }

    // ✅ NUEVA FUNCIÓN 1: Verificar si el username ya existe
    public boolean existeUsuario(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT count(U) FROM Usuario U WHERE U.username = :username";
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("username", username);
            Long count = query.uniqueResult();
            return count != null && count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ✅ NUEVA FUNCIÓN 2: Verificar si el DNI ya existe (Muy importante para tutores/docentes)
    // Asumiendo que tu modelo Usuario o la tabla Persona tiene un campo 'dni' o similar.
    // Si el DNI está en la tabla 'Tutor' o 'Docente', esta función iría en sus respectivos DAOs.
    /* public boolean existeDni(String dni) {
       // Lógica similar a existeUsuario pero buscando por DNI
    }
    */

}