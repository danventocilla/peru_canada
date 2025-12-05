-- =========================================================
-- SCRIPT MAESTRO: COLEGIO PERÚ-CANADÁ (V3.0 FULL DATA)
-- =========================================================

-- 1. LIMPIEZA TOTAL (Empezamos de cero para evitar errores fantasma)
DROP DATABASE IF EXISTS sistema_escolar;
CREATE DATABASE sistema_escolar;
USE sistema_escolar;

-- 2. CREACIÓN DE TABLAS (Estructura compatible con tu Java actual)

-- ROLES
CREATE TABLE roles (
    id_rol INT AUTO_INCREMENT PRIMARY KEY,
    nombre_rol VARCHAR(20) NOT NULL
);

-- GRADOS
CREATE TABLE grados (
    id_grado INT AUTO_INCREMENT PRIMARY KEY,
    nombre_grado VARCHAR(20) NOT NULL,
    seccion CHAR(1) NOT NULL,
    nivel VARCHAR(20) NOT NULL
);

-- USUARIOS (Login)
CREATE TABLE usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    estado TINYINT DEFAULT 1,
    id_rol INT,
    FOREIGN KEY (id_rol) REFERENCES roles(id_rol)
);

-- DOCENTES
CREATE TABLE docentes (
    id_docente INT AUTO_INCREMENT PRIMARY KEY,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    dni VARCHAR(8) NOT NULL UNIQUE,
    id_usuario INT,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

-- TUTORES (Padres)
CREATE TABLE tutores (
    id_tutor INT AUTO_INCREMENT PRIMARY KEY,
    nombres VARCHAR(100) NOT NULL,
    telefono VARCHAR(15),
    dni VARCHAR(8) NOT NULL UNIQUE,
    id_usuario INT,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

-- ESTUDIANTES (¡CON DNI AGREGADO!)
CREATE TABLE estudiantes (
    id_estudiante INT AUTO_INCREMENT PRIMARY KEY,
    nombres VARCHAR(100) NOT NULL,
    dni VARCHAR(8), -- <--- EL CAMPO NUEVO QUE TU JAVA ESPERA
    id_grado INT,
    id_tutor INT,
    FOREIGN KEY (id_grado) REFERENCES grados(id_grado),
    FOREIGN KEY (id_tutor) REFERENCES tutores(id_tutor)
);

-- CURSOS
CREATE TABLE cursos (
    id_curso INT AUTO_INCREMENT PRIMARY KEY,
    nombre_curso VARCHAR(50) NOT NULL,
    id_grado INT,
    id_docente INT,
    FOREIGN KEY (id_grado) REFERENCES grados(id_grado),
    FOREIGN KEY (id_docente) REFERENCES docentes(id_docente)
);

-- ASISTENCIA
CREATE TABLE asistencia (
    id_asistencia INT AUTO_INCREMENT PRIMARY KEY,
    fecha DATE NOT NULL,
    estado ENUM('Presente', 'Tardanza', 'Falta') NOT NULL,
    id_estudiante INT,
    id_curso INT,  -- <--- ¡ESTA COLUMNA FALTABA!
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante),
    FOREIGN KEY (id_curso) REFERENCES cursos(id_curso) -- Relación necesaria
);

-- NOTAS
CREATE TABLE notas (
    id_nota INT AUTO_INCREMENT PRIMARY KEY,
    valor DECIMAL(4,2) NOT NULL,
    bimestre INT NOT NULL,
    id_estudiante INT,
    id_curso INT,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante),
    FOREIGN KEY (id_curso) REFERENCES cursos(id_curso)
);

-- OBSERVACIONES
CREATE TABLE observaciones (
    id_observacion INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(100) NOT NULL,
    descripcion TEXT,
    nivel_gravedad ENUM('Leve', 'Moderado', 'Grave') NOT NULL,
    fecha DATE NOT NULL,
    id_estudiante INT,
    id_docente INT,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante),
    FOREIGN KEY (id_docente) REFERENCES docentes(id_docente)
);

-- =========================================================
-- 3. INSERCIÓN DE DATOS REALISTAS (POBLADO MASIVO)
-- =========================================================

-- ROLES
INSERT INTO roles (nombre_rol) VALUES ('Administrador'), ('Docente'), ('Tutor');

-- GRADOS (Primaria completa)
INSERT INTO grados (nombre_grado, seccion, nivel) VALUES 
('Primero', 'A', 'Primaria'), -- ID 1
('Segundo', 'B', 'Primaria'), -- ID 2
('Tercero', 'C', 'Primaria'), -- ID 3
('Cuarto', 'A', 'Primaria'),  -- ID 4
('Quinto', 'B', 'Primaria'),  -- ID 5
('Sexto', 'C', 'Primaria');   -- ID 6

-- USUARIOS (Clave para todos: 1234)
INSERT INTO usuarios (username, password, id_rol) VALUES 
('admin', '1234', 1),         -- ID 1
('profe_juan', '1234', 2),    -- ID 2 (Matemático)
('profe_maria', '1234', 2),   -- ID 3 (Lenguaje)
('profe_pedro', '1234', 2),   -- ID 4 (Ciencias)
('padre_luis', '1234', 3),    -- ID 5 (Papá de Pepito)
('madre_ana', '1234', 3),     -- ID 6 (Mamá de Anita)
('padre_carlos', '1234', 3);  -- ID 7 (Papá de Jaimito)

-- DOCENTES
INSERT INTO docentes (nombres, apellidos, dni, id_usuario) VALUES 
('Juan Carlos Pérez', 'López', '10203040', 2), -- ID 1
('María Elena', 'García', '20304050', 3),      -- ID 2
('Pedro Pablo', 'Torres', '30405060', 4);   -- ID 3

-- TUTORES
INSERT INTO tutores (nombres, telefono, dni, id_usuario) VALUES 
('Luis Alberto Gómez', '999888777', '87654321', 5), -- ID 1 (Tú, para probar)
('Ana María Polo', '987654321', '11223344', 6),     -- ID 2
('Carlos Alcántara', '912345678', '55667788', 7);   -- ID 3

-- ESTUDIANTES (Asignados a sus padres y grados)
INSERT INTO estudiantes (nombres, dni, id_grado, id_tutor) VALUES 
('Pepito Gómez', '70707070', 1, 1),   -- Hijo de Luis (1ro A)
('Anita Polo', '71717171', 1, 2),     -- Hija de Ana (1ro A) - Compañera de Pepito
('Jaimito Alcántara', '72727272', 2, 3); -- Hijo de Carlos (2do B)

-- CURSOS (5 Cursos distribuidos)
INSERT INTO cursos (nombre_curso, id_grado, id_docente) VALUES 
('Matemática', 1, 1),        -- ID 1 (1ro A - Profe Juan)
('Comunicación', 1, 2),      -- ID 2 (1ro A - Profe María)
('Ciencia y Amb.', 1, 3),    -- ID 3 (1ro A - Profe Pedro)
('Matemática Avanzada', 2, 1), -- ID 4 (2do B - Profe Juan)
('Inglés', 1, 2);            -- ID 5 (1ro A - Profe María)

-- =========================================================
-- 4. CARGA DE NOTAS (Para que el CRUD tenga qué editar)
-- =========================================================

-- Notas de Pepito (1ro A)
INSERT INTO notas (valor, bimestre, id_estudiante, id_curso) VALUES 
(18.0, 1, 1, 1), -- Mate
(16.0, 1, 1, 2), -- Comunicación
(19.5, 1, 1, 3), -- Ciencia
(14.0, 2, 1, 1); -- Mate (2do Bimestre)

-- Notas de Anita (1ro A)
INSERT INTO notas (valor, bimestre, id_estudiante, id_curso) VALUES 
(20.0, 1, 2, 1), -- Mate (Es una genia)
(18.0, 1, 2, 2); -- Comunicación

-- Notas de Jaimito (2do B)
INSERT INTO notas (valor, bimestre, id_estudiante, id_curso) VALUES 
(11.0, 1, 3, 4); -- Sufriendo en Mate Avanzada

-- =========================================================
-- 5. ASISTENCIA Y OBSERVACIONES
-- =========================================================
INSERT INTO asistencia (fecha, estado, id_estudiante, id_curso) VALUES 
('2025-03-10', 'Presente', 1, 1), -- Pepito en Mate
('2025-03-10', 'Presente', 2, 1), -- Anita en Mate
('2025-03-11', 'Tardanza', 1, 1), -- Pepito en Mate
('2025-03-12', 'Falta', 3, 4);    -- Jaimito en Mate Avanzada

INSERT INTO observaciones (titulo, descripcion, nivel_gravedad, fecha, id_estudiante, id_docente) VALUES 
('Buen comportamiento', 'Pepito ayudó a sus compañeros hoy.', 'Leve', '2025-03-15', 1, 1),
('No trajo tarea', 'Jaimito olvidó el cuaderno.', 'Moderado', '2025-03-16', 3, 1);

-- FIN DEL SCRIPT

select * from tutores;

select * from estudiantes;

select * from docentes;

select * from asistencia;


