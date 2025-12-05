package model;

// CAMBIO IMPORTANTE: Usamos 'jakarta' en lugar de 'javax'
import jakarta.persistence.*;
import java.util.Date;

/**
 * Entidad JPA que representa la tabla 'usuarios' en la base de datos.
 * Almacena las credenciales de acceso, el estado de la cuenta (activo/pendiente)
 * y el rol asociado (Tutor, Docente, Administrador).
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private int idUsuario;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    // 0: Pendiente (ej. nuevos registros de tutores), 1: Activo
    @Column(name = "estado", columnDefinition = "TINYINT default 0")
    private int estado;

    @Column(name = "id_rol", nullable = false)
    private int idRol;

    // Columna manejada por la base de datos (se usa para auditoría)
    @Column(name = "fecha_registro", insertable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaRegistro;

    // --- CONSTRUCTORES ---
    /**
     * Constructor vacío requerido por Hibernate.
     */
    public Usuario() {
    }

    /**
     * Constructor para crear un nuevo usuario. La fecha de registro se asigna en la BD.
     */
    public Usuario(String username, String password, int estado, int idRol) {
        this.username = username;
        this.password = password;
        this.estado = estado;
        this.idRol = idRol;
    }

    // --- GETTERS Y SETTERS ---
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getEstado() { return estado; }
    public void setEstado(int estado) { this.estado = estado; }

    public int getIdRol() { return idRol; }
    public void setIdRol(int idRol) { this.idRol = idRol; }

    public Date getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(Date fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}