package model;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa la tabla 'tutores' en la base de datos.
 * Esta clase mapea la información del tutor (padre/apoderado) y su vínculo
 * con la tabla de usuarios.
 */
@Entity
@Table(name = "tutores")
public class Tutor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tutor")
    private int idTutor;

    @Column(name = "nombres", nullable = false)
    private String nombres;

    @Column(name = "dni", nullable = false, unique = true)
    private String dni;

    @Column(name = "telefono")
    private String telefono;

    // IMPORTANTE: Este campo vincula con la tabla usuarios
    @Column(name = "id_usuario", nullable = false)
    private int idUsuario;

    /**
     * Constructor vacío requerido por Hibernate.
     */
    public Tutor() {}

    /**
     * Constructor usado para la creación inicial de un Tutor.
     */
    public Tutor(String nombres, String dni, String telefono) {
        this.nombres = nombres;
        this.dni = dni;
        this.telefono = telefono;
    }

    // --- Getters y Setters ---

    public int getIdTutor() { return idTutor; }
    public void setIdTutor(int idTutor) { this.idTutor = idTutor; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; } // Este es el setter crucial para el DAO

    @Override
    public String toString() {
        return "Tutor{" +
                "idTutor=" + idTutor +
                ", nombres='" + nombres + '\'' +
                ", dni='" + dni + '\'' +
                ", telefono='" + telefono + '\'' +
                ", idUsuario=" + idUsuario +
                '}';
    }
}