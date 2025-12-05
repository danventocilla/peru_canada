import { BASE_URL } from './config.js';
import { state } from './state.js';

// Usamos el estado local para el curso en edición/asistencia
let cursoActualId = null;

console.log("👩‍🏫 Docente.js: Cargado.");

// ==========================================
// 1. CARGAR PANEL PRINCIPAL (DASHBOARD)
// ==========================================
export async function cargarPanelDocente() {
    const contenedor = document.getElementById('contenedor-cursos-docente');
    if (!contenedor) return;
    
    contenedor.innerHTML = '<div class="spinner-border text-primary"></div> Cargando cursos...';

    try {
        const response = await fetch(`${BASE_URL}/docente/cursos/${state.dniUsuario}`);
        const cursos = await response.json();
        
        contenedor.innerHTML = '';
        if (cursos.length === 0) {
            contenedor.innerHTML = '<div class="alert alert-info">No tiene cursos asignados todavía.</div>';
            return;
        }

        cursos.forEach(curso => {
            const card = `
                <div class="col-md-4">
                    <div class="card text-center h-100 border-0 shadow-sm card-hover">
                        <div class="card-body">
                            <i class="bi bi-journal-bookmark-fill text-primary display-4 mb-3"></i>
                            <h5 class="card-title">${curso.nombre}</h5>
                            
                            <div class="d-grid gap-2 mt-3">
                                <button class="btn btn-primary btn-sm" onclick="app.abrirGestionNotas(${curso.id}, '${curso.nombre}')">
                                    <i class="bi bi-pencil-square"></i> Notas
                                </button>
                                <button class="btn btn-success btn-sm" onclick="app.abrirTomarAsistencia(${curso.id}, '${curso.nombre}')">
                                    <i class="bi bi-calendar-check"></i> Asistencia
                                </button>
                            </div>
                        </div>
                    </div>
                </div>`;
            contenedor.insertAdjacentHTML('beforeend', card);
        });
    } catch (e) { 
        console.error(e);
        contenedor.innerHTML = '<div class="alert alert-danger">Error al cargar cursos.</div>';
    }
}

// ==========================================
// 2. GESTIÓN DE NOTAS (MODO MASIVO)
// ==========================================

// Abre la vista de gestión de notas
export async function abrirGestionNotas(idCurso, nombreCurso) { 
    const dashboard = document.getElementById('docente-dashboard-cards');
    if(dashboard) dashboard.style.display = 'none';

    const vistaNotas = document.getElementById('docente-gestion-notas');
    if(vistaNotas) {
        vistaNotas.style.display = 'block';
        vistaNotas.style.opacity = 0;
        setTimeout(() => { vistaNotas.style.opacity = 1; }, 50);
    }
    
    const titulo = document.getElementById('titulo-curso-gestion');
    if(titulo) titulo.innerText = "Notas: " + nombreCurso;
    
    cursoActualId = idCurso;
    
    // Primero listamos a todos los alumnos para preparar la tabla
    await listarAlumnosParaNotas(idCurso); 

    // Luego cargamos las notas del bimestre seleccionado por defecto
    const bimestreDefault = document.getElementById('select-bimestre').value;
    if(bimestreDefault) {
        cargarNotasPorBimestre(parseInt(bimestreDefault));
    }
}

// Carga la lista de alumnos y genera los inputs (sin valores por defecto)
async function listarAlumnosParaNotas(idCurso) {
    const tbody = document.querySelector('#tabla-gestion-notas tbody');
    if(!tbody) return;
    tbody.innerHTML = '<tr><td colspan="2" class="text-center">Cargando lista de alumnos...</td></tr>';

    try {
        const response = await fetch(`${BASE_URL}/docente/alumnos/${idCurso}`);
        const alumnos = await response.json();
        
        tbody.innerHTML = '';
        if (alumnos.length === 0) {
            tbody.innerHTML = '<tr><td colspan="2" class="text-center">No hay alumnos matriculados.</td></tr>';
            return;
        }

        alumnos.forEach(alumno => {
            // Generamos la estructura de la fila con el input y el data-attribute
            const fila = `
                <tr>
                    <td>${alumno.nombre}</td>
                    <td>
                        <input type="number" min="0" max="20" class="form-control text-center input-nota-masiva" 
                                data-id-estudiante="${alumno.id}" placeholder="-" value="">
                    </td>
                </tr>`;
            tbody.insertAdjacentHTML('beforeend', fila);
        });

        // BOTÓN "GUARDAR TODO" AL FINAL
        tbody.insertAdjacentHTML('beforeend', `
            <tr>
                <td colspan="2" class="p-3 bg-light">
                    <button class="btn btn-primary w-100 fw-bold py-2" onclick="app.guardarNotasMasivo()">
                        <i class="bi bi-save-fill"></i> GUARDAR TODAS LAS NOTAS
                    </button>
                </td>
            </tr>
        `);

    } catch (e) { console.error("Error al listar alumnos para notas:", e); }
}

// **NUEVA FUNCIÓN REQUERIDA** - Carga las notas existentes para el bimestre seleccionado
export async function cargarNotasPorBimestre(bimestre) {
    if (!cursoActualId) return;

    console.log(`Cargando notas de ${cursoActualId} para Bimestre ${bimestre}`);

    try {
        const res = await fetch(`${BASE_URL}/docente/notas/${cursoActualId}/${bimestre}`);
        const notasExistentes = await res.json();
        
        // Mapear notas por ID de estudiante para fácil acceso
        const notasMap = new Map();
        notasExistentes.forEach(n => notasMap.set(n.idEstudiante, n.nota));
        
        // Rellenar los inputs en la tabla
        document.querySelectorAll('.input-nota-masiva').forEach(input => {
            const idEstudiante = parseInt(input.getAttribute('data-id-estudiante'));
            const nota = notasMap.get(idEstudiante);
            
            // Si existe una nota, la rellena, si no, lo deja en blanco
            input.value = nota !== undefined ? nota : '';
        });

    } catch(e) {
        console.error("Error al cargar notas por bimestre:", e);
    }
}

// FUNCIÓN PARA GUARDAR TODO DE GOLPE
export async function guardarNotasMasivo() {
    const inputs = document.querySelectorAll('.input-nota-masiva');
    const bimestreSelect = document.getElementById('select-bimestre');
    const bimestre = parseInt(bimestreSelect.value);
    const listaParaEnviar = [];

    // Validar bimestre
    if (isNaN(bimestre) || bimestre < 1 || bimestre > 4) {
        alert("⚠️ Seleccione un bimestre válido (1-4).");
        return;
    }

    let errorEncontrado = false;
    // Recolectar datos y validar
    inputs.forEach(input => {
        const valor = input.value.trim();
        input.classList.remove('is-invalid'); // Limpiar validación previa
        
        if (valor !== "") { 
            const notaNum = parseFloat(valor);
            if(notaNum >= 0 && notaNum <= 20) {
                listaParaEnviar.push({
                    idEstudiante: parseInt(input.getAttribute('data-id-estudiante')),
                    idCurso: cursoActualId,
                    bimestre: bimestre,
                    nota: notaNum
                });
            } else {
                input.classList.add('is-invalid');
                errorEncontrado = true;
            }
        }
    });

    if (errorEncontrado) {
        alert("❌ Hay notas inválidas (deben ser entre 0 y 20). Corrija antes de guardar.");
        return;
    }

    if (listaParaEnviar.length === 0) { 
        alert("⚠️ No has escrito ninguna nota válida (0-20) para guardar."); 
        return; 
    }

    // Enviar al Backend (Lista Completa)
    try {
        const res = await fetch(`${BASE_URL}/docente/guardarNotasMasiva`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(listaParaEnviar)
        });
        const data = await res.json();
        
        if (data.success) {
            alert("✅ ¡Todas las notas se guardaron correctamente!");
        } else {
            alert("❌ Hubo un error al guardar las notas.");
        }
    } catch(e) { 
        console.error(e);
        alert("Error de conexión con el servidor."); 
    }
}

// ==========================================
// 3. GESTIÓN DE ASISTENCIA (MODO MASIVO)
// ==========================================

// Abre la vista de toma de asistencia
export async function abrirTomarAsistencia(idCurso, nombreCurso) {
    const dashboard = document.getElementById('docente-dashboard-cards');
    if(dashboard) dashboard.style.display = 'none';

    const panel = document.getElementById('docente-asistencia-view'); 
    
    if(panel) {
        panel.style.display = 'block';
        const titulo = document.getElementById('titulo-curso-asistencia');
        if(titulo) titulo.innerText = "Asistencia: " + nombreCurso;
        
        cursoActualId = idCurso;
        await listarAlumnosAsistencia(idCurso); // Prepara la tabla
        
        // Carga la asistencia del día actual por defecto
        const inputFecha = document.getElementById('input-fecha-asistencia');
        if (inputFecha) {
            const today = new Date().toISOString().split('T')[0];
            inputFecha.value = today;
            cargarAsistenciaPorFecha(today);
        }
    } else {
        console.error("Falta el div 'docente-asistencia-view'");
    }
}

// Carga la lista de alumnos y genera los botones de selección
async function listarAlumnosAsistencia(idCurso) {
    const tbody = document.querySelector('#tabla-asistencia-docente tbody');
    if(!tbody) return;
    tbody.innerHTML = '<tr><td colspan="2" class="text-center">Cargando lista...</td></tr>';

    try {
        const response = await fetch(`${BASE_URL}/docente/alumnos/${idCurso}`);
        const alumnos = await response.json();
        tbody.innerHTML = '';

        if (alumnos.length === 0) {
            tbody.innerHTML = '<tr><td colspan="2" class="text-center">No hay alumnos.</td></tr>';
            return;
        }

        alumnos.forEach(alum => {
            const fila = `
                <tr class="fila-asistencia" data-id-estudiante="${alum.id}">
                    <td>${alum.nombre}</td>
                    <td>
                        <div class="btn-group w-100" role="group">
                            <button type="button" class="btn btn-outline-success btn-sm btn-asist" onclick="app.seleccionarEstadoAsistencia(this, 'Presente')">P</button>
                            <button type="button" class="btn btn-outline-warning btn-sm btn-asist" onclick="app.seleccionarEstadoAsistencia(this, 'Tardanza')">T</button>
                            <button type="button" class="btn btn-outline-danger btn-sm btn-asist" onclick="app.seleccionarEstadoAsistencia(this, 'Falta')">F</button>
                        </div>
                        <input type="hidden" class="estado-seleccionado" value=""> 
                    </td>
                </tr>`;
            tbody.insertAdjacentHTML('beforeend', fila);
        });

        // BOTÓN "GUARDAR TODO" AL FINAL
        tbody.insertAdjacentHTML('beforeend', `
            <tr>
                <td colspan="2" class="p-3 bg-light">
                    <button class="btn btn-success w-100 fw-bold py-2" onclick="app.guardarAsistenciaMasivo()">
                        <i class="bi bi-check-circle-fill"></i> GUARDAR ASISTENCIA DEL DÍA
                    </button>
                </td>
            </tr>
        `);
    } catch(e) { console.error("Error al listar alumnos para asistencia:", e); }
}


// **NUEVA FUNCIÓN REQUERIDA** - Carga la asistencia existente para la fecha seleccionada
export async function cargarAsistenciaPorFecha(fecha) {
    if (!cursoActualId) return;

    console.log(`Cargando asistencia de ${cursoActualId} para fecha ${fecha}`);

    try {
        const res = await fetch(`${BASE_URL}/docente/asistencia/${cursoActualId}/${fecha}`);
        const asistenciaExistente = await res.json();
        
        const asistenciaMap = new Map();
        asistenciaExistente.forEach(a => asistenciaMap.set(a.idEstudiante, a.estado));

        document.querySelectorAll('.fila-asistencia').forEach(fila => {
            const idEstudiante = parseInt(fila.getAttribute('data-id-estudiante'));
            const estado = asistenciaMap.get(idEstudiante);
            
            // Si existe un estado, simula el clic para actualizar la UI
            if (estado) {
                const buttonToClick = fila.querySelector(`button[onclick*="'${estado}'"]`);
                if (buttonToClick) {
                    seleccionarEstadoAsistencia(buttonToClick, estado);
                }
            } else {
                // Limpiar si no hay registro para esa fecha
                const inputHidden = fila.querySelector('.estado-seleccionado');
                inputHidden.value = '';
                // Limpiar estilos (usando el primer botón para limpiar)
                const primerBoton = fila.querySelector('.btn-asist');
                if (primerBoton) {
                    // Solo limpiamos los estilos sin seleccionar un estado
                    Array.from(primerBoton.parentElement.children).forEach(b => {
                        b.className = 'btn btn-sm btn-asist'; 
                        if(b.innerText === 'P') b.classList.add('btn-outline-success');
                        if(b.innerText === 'T') b.classList.add('btn-outline-warning');
                        if(b.innerText === 'F') b.classList.add('btn-outline-danger');
                    });
                }
            }
        });

    } catch(e) {
        console.error("Error al cargar asistencia por fecha:", e);
    }
}

// **NUEVA FUNCIÓN REQUERIDA** - Permite marcar a todos con un estado
export function marcarAsistenciaMasiva(estado) {
    console.log(`Marcando a todos como: ${estado}`);
    document.querySelectorAll('.fila-asistencia').forEach(fila => {
        const buttonToClick = fila.querySelector(`button[onclick*="'${estado}'"]`);
        if (buttonToClick) {
            seleccionarEstadoAsistencia(buttonToClick, estado);
        }
    });
}

// **EXPORTADA** - Función puramente visual (Pinta el botón seleccionado)
export function seleccionarEstadoAsistencia(btn, estado) {
    const grupo = btn.parentElement;
    const inputHidden = grupo.parentElement.querySelector('.estado-seleccionado');
    
    // 1. Limpiar estilos de todos los botones del grupo
    Array.from(grupo.children).forEach(b => {
        b.className = 'btn btn-sm btn-asist'; // Clase base
        // Restaurar bordes (outlines)
        if(b.innerText === 'P') b.classList.add('btn-outline-success');
        if(b.innerText === 'T') b.classList.add('btn-outline-warning');
        if(b.innerText === 'F') b.classList.add('btn-outline-danger');
    });

    // 2. Activar el botón clicado (Relleno sólido)
    if(estado === 'Presente') btn.className = 'btn btn-success btn-sm text-white fw-bold';
    if(estado === 'Tardanza') btn.className = 'btn btn-warning btn-sm text-dark fw-bold';
    if(estado === 'Falta') btn.className = 'btn btn-danger btn-sm text-white fw-bold';

    // 3. Guardar valor en el input oculto para enviarlo luego
    inputHidden.value = estado;
}

// FUNCIÓN PARA GUARDAR TODA LA ASISTENCIA
export async function guardarAsistenciaMasivo() {
    const filas = document.querySelectorAll('.fila-asistencia');
    const inputFecha = document.getElementById('input-fecha-asistencia');
    const fecha = inputFecha ? inputFecha.value : null;
    
    if (!fecha || !cursoActualId) {
        alert("⚠️ Seleccione una fecha válida y asegúrese de que el curso está cargado."); 
        return;
    }

    const listaEnviar = [];
    let contadorMarcados = 0;

    filas.forEach(tr => {
        const idEst = parseInt(tr.getAttribute('data-id-estudiante'));
        const estado = tr.querySelector('.estado-seleccionado').value;
        
        if (estado) { // Solo si seleccionó algo (P, T o F)
            listaEnviar.push({ 
                idEstudiante: idEst, 
                estado: estado,
                fecha: fecha, // Enviamos la fecha seleccionada
                idCurso: cursoActualId
            });
            contadorMarcados++;
        }
    });

    if (contadorMarcados === 0) { 
        alert("⚠️ No has marcado la asistencia de ningún alumno."); 
        return; 
    }

    try {
        const res = await fetch(`${BASE_URL}/asistencia/guardarMasiva`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(listaEnviar)
        });
        const data = await res.json();
        
        if(data.success) {
            alert(`✅ Asistencia registrada correctamente para ${contadorMarcados} alumnos.`);
        } else {
            alert("❌ Error al guardar asistencia.");
        }
    } catch(e) { 
        console.error(e);
        alert("Error de conexión."); 
    }
}


// ==========================================
// 4. OBSERVACIONES (CONDUCTA)
// ==========================================

export function abrirPanelObservacionesDocente() {
    // 1. Ocultar el Dashboard correcto
    const dashboard = document.getElementById('docente-dashboard-cards');
    if(dashboard) dashboard.style.display = 'none';

    // 2. Mostrar la vista DE DOCENTE (con formulario), no la del Tutor
    const view = document.getElementById('docente-observaciones-view');
    if(view) view.style.display = 'block';
    
    // (Opcional) Limpiar campos al entrar
    document.getElementById('obs-dni-alumno').value = "";
    document.getElementById('obs-titulo').value = "";
    document.getElementById('obs-descripcion').value = "";
}

export async function registrarObservacion() {
    // Referencias a los inputs del nuevo HTML
    const dni = document.getElementById('obs-dni-alumno').value;
    const nivel = document.getElementById('obs-nivel').value;
    const desc = document.getElementById('obs-descripcion').value;
    const tituloInput = document.getElementById('obs-titulo'); // Nuevo campo
    
    // Validamos que haya título, si no, ponemos uno por defecto
    const titulo = tituloInput && tituloInput.value.trim() !== "" 
                   ? tituloInput.value.trim() 
                   : "Reporte de Conducta";

    if(!dni || !desc) { 
        alert("⚠️ Complete el DNI del alumno y la descripción."); 
        return; 
    }

    try {
        const response = await fetch(`${BASE_URL}/observaciones/guardar`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                titulo: titulo,      // Enviamos el título real
                descripcion: desc,
                nivel: nivel,
                dniAlumno: dni,
                userDocente: state.usuario 
            })
        });
        const data = await response.json();
        
        if(data.success) {
            alert("✅ Observación registrada correctamente.");
            // Limpiamos formulario
            document.getElementById('obs-descripcion').value = "";
            document.getElementById('obs-dni-alumno').value = "";
            if(tituloInput) tituloInput.value = "";
        } else {
            alert("❌ Error: " + (data.mensaje || "DNI alumno no encontrado."));
        }
    } catch(e) { 
        console.error(e);
        alert("Error de conexión."); 
    }
}
