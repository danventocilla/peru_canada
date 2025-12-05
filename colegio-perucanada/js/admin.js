import { BASE_URL } from './config.js';
import { state } from './state.js';
import { mostrarVistaIntranet } from './ui.js';

// --- CONFIGURACIÓN ---
const jsonHeaders = { 'Content-Type': 'application/json' };

// ==========================================
// 1. DASHBOARD Y ESTADÍSTICAS
// ==========================================

function updateText(id, val) {
    const el = document.getElementById(id);
    if (el) {
        if (id === 'badge-pendientes-count') {
            el.innerText = `${val || 0} nuevas`;
        } else {
            el.innerText = val || 0;
        }
    }
}

export async function cargarEstadisticas() {
    try {
        const res = await fetch(`${BASE_URL}/admin/stats`);
        if(!res.ok) return; 
        
        const stats = await res.json();
        
        updateText('count-estudiantes', stats.estudiantes);
        updateText('count-docentes', stats.docentes);
        updateText('count-tutores', stats.tutores);
        updateText('count-pendientes', stats.pendientes);
        updateText('badge-pendientes-count', stats.pendientes); 
        
    } catch(e) { 
        console.error("Error cargando estadísticas:", e); 
    }
}

// ==========================================
// 2. GESTIÓN DE USUARIOS PENDIENTES
// ==========================================

export async function cargarPendientes() {
    const tbody = document.getElementById("tabla-pendientes");
    if(!tbody) return;
    
    tbody.innerHTML = '<tr><td colspan="4" class="text-center text-primary"><div class="spinner-border spinner-border-sm"></div> Cargando...</td></tr>';

    try {
        const res = await fetch(`${BASE_URL}/admin/pendientes`); 
        if(!res.ok) throw new Error("API Error");
        
        const lista = await res.json();
        tbody.innerHTML = ""; 

        if(lista.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">No hay solicitudes pendientes.</td></tr>';
            return;
        }

        lista.forEach(item => {
            const fila = `
                <tr>
                    <td>${item.nombres}</td>
                    <td>${item.dni}</td>
                    <td>${item.usuario}</td>
                    <td>
                        <button onclick="app.aprobarTutor(${item.id_tutor})" class="btn btn-sm btn-success">
                            <i class="bi bi-check-lg"></i> Aprobar
                        </button>
                    </td>
                </tr>
            `;
            tbody.insertAdjacentHTML('beforeend', fila);
        });
    } catch(e) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-danger"><small>Error de conexión.</small></td></tr>';
    }
}

export async function aprobarTutor(idTutor) {
    if(!confirm("¿Está seguro de dar acceso a este usuario?")) return;
    
    try {
        const res = await fetch(`${BASE_URL}/admin/aprobarTutor`, {
            method: 'POST', 
            headers: jsonHeaders,
            body: JSON.stringify({ id: idTutor })
        });
        
        const resultado = await res.json();
        
        if(resultado.success) {
            alert("✅ Usuario aprobado correctamente.");
            cargarPendientes();    
            cargarEstadisticas();  
        } else {
            alert("❌ Error al aprobar.");
        }
    } catch (e) { 
        alert("Error de conexión."); 
    }
}

// ==========================================
// 3. DIRECTORIOS (LISTAS COMPLETAS)
// ==========================================

export async function mostrarListasCompletas() {
    const panel = document.getElementById('admin-listas-view');
    if(panel) {
        panel.style.display = 'block';
        cargarTablaAlumnos(); 
        cargarTablaTutores(); 
        panel.scrollIntoView({ behavior: 'smooth' });
    }
}

async function cargarTablaAlumnos() {
    const tbody = document.querySelector('#tab-alumnos tbody');
    if(!tbody) return;
    tbody.innerHTML = '<tr><td colspan="3" class="text-center">Cargando...</td></tr>';
    
    try {
        const res = await fetch(`${BASE_URL}/admin/alumnos`);
        const lista = await res.json();
        tbody.innerHTML = '';
        
        if(lista.length === 0) {
            tbody.innerHTML = '<tr><td colspan="3" class="text-center">No hay alumnos registrados.</td></tr>';
            return;
        }

        lista.forEach(a => {
            tbody.innerHTML += `
                <tr>
                    <td>${a.nombre}</td>
                    <td><span class="badge bg-light text-dark border">${a.grado}</span></td>
                    <td><small>${a.tutor}</small></td>
                </tr>`;
        });
    } catch(e) { 
        tbody.innerHTML = '<tr><td colspan="3" class="text-danger text-center">Error al cargar datos.</td></tr>'; 
    }
}

async function cargarTablaTutores() {
    const tbody = document.querySelector('#tab-tutores tbody');
    if(!tbody) return;
    tbody.innerHTML = '<tr><td colspan="3" class="text-center">Cargando...</td></tr>';
    
    try {
        const res = await fetch(`${BASE_URL}/admin/tutores`);
        const lista = await res.json();
        tbody.innerHTML = '';

        if(lista.length === 0) {
            tbody.innerHTML = '<tr><td colspan="3" class="text-center">No hay tutores registrados.</td></tr>';
            return;
        }

        lista.forEach(t => {
            tbody.innerHTML += `
                <tr>
                    <td>${t.nombre}</td>
                    <td>${t.dni}</td>
                    <td>${t.telefono || '-'}</td>
                </tr>`;
        });
    } catch(e) { 
        tbody.innerHTML = '<tr><td colspan="3" class="text-danger text-center">Error al cargar datos.</td></tr>'; 
    }
}

// ==========================================
// 4. GESTIÓN DE CARGA ACADÉMICA (ASIGNACIÓN)
// ==========================================

export async function abrirPanelAsignacion() {
    const panel = document.getElementById('admin-asignacion-view');
    if(panel) {
        panel.style.display = 'block';
        panel.scrollIntoView({ behavior: 'smooth' });
        cargarSelectsAdmin();
        cargarTablaAsignaciones();
    }
}

async function cargarSelectsAdmin() {
    try {
        const selDoc = document.getElementById('admin-select-docente');
        const resDoc = await fetch(`${BASE_URL}/admin/docentes`);
        const docentes = await resDoc.json();
        
        selDoc.innerHTML = '<option value="">Seleccione Docente...</option>';
        docentes.forEach(d => selDoc.innerHTML += `<option value="${d.id}">${d.nombre}</option>`);

        const selCur = document.getElementById('admin-select-curso');
        const resCur = await fetch(`${BASE_URL}/admin/cursos`);
        const cursos = await resCur.json();
        
        selCur.innerHTML = '<option value="">Seleccione Curso...</option>';
        cursos.forEach(c => selCur.innerHTML += `<option value="${c.id}">${c.curso}</option>`);
    } catch(e) { console.error("Error selects:", e); }
}

async function cargarTablaAsignaciones() {
    const tbody = document.querySelector('#tabla-asignaciones tbody');
    if(!tbody) return;
    tbody.innerHTML = '<tr><td colspan="2" class="text-center">Cargando...</td></tr>';
    
    try {
        const res = await fetch(`${BASE_URL}/admin/cursos`);
        const data = await res.json();
        
        tbody.innerHTML = '';
        data.forEach(item => {
            const color = item.docente === 'Sin Asignar' ? 'text-danger fw-bold' : 'text-success';
            tbody.innerHTML += `
                <tr>
                    <td>${item.curso}</td>
                    <td class="${color}">${item.docente}</td>
                </tr>`;
        });
    } catch(e) { console.error(e); }
}

export async function guardarAsignacion() {
    const idCurso = document.getElementById('admin-select-curso').value;
    const idDocente = document.getElementById('admin-select-docente').value;

    if (!idCurso || !idDocente) {
        alert("⚠️ Por favor seleccione un curso y un docente.");
        return; 
    }

    try {
        const res = await fetch(`${BASE_URL}/admin/asignar`, {
            method: 'POST', 
            headers: jsonHeaders,
            body: JSON.stringify({ idCurso: parseInt(idCurso), idDocente: parseInt(idDocente) })
        });
        const data = await res.json();
        
        if (data.success) {
            alert("✅ Docente asignado correctamente.");
            cargarTablaAsignaciones();
        } else {
            alert("Error al asignar.");
        }
    } catch (e) { 
        alert("Error de conexión."); 
    }
}

// ==========================================
// 5. BÚSQUEDA DE NOTAS (ADMIN)
// ==========================================

export async function adminBuscarNotas() {
    const dniBuscado = document.getElementById('admin-search-dni').value.trim();
    if (!/^\d{8}$/.test(dniBuscado)) {
        alert("⚠️ Ingrese un DNI válido de 8 dígitos.");
        return;
    }
    
    // 1. Modificar el estado para simular que este DNI está logeado (solo en memoria)
    state.dniUsuario = dniBuscado; 
    state.adminSearchingNotes = true; // ACTIVAMOS LA BANDERA
    
    // 2. Navegar a la vista de notas
    // Usamos mostrarVistaIntranet porque es la función central
    // Asegúrate de que esta función esté disponible o importada si la usas aquí
    // Como mostrarVistaIntranet está en ui.js y admin.js es importado por main.js, 
    // lo mejor es llamar a la función global app.mostrarVistaIntranet si existe
    
    // Mostramos la vista de notas
    const pNotas = document.getElementById('notas-detalle');
    const pAdmin = document.getElementById('dashboard-admin');
    
    if(pAdmin) pAdmin.style.display = 'none';
    if(pNotas) pNotas.style.display = 'block';
    
    // 3. Llamar a la función que carga las notas 
    if (window.app && window.app.cargarNotasEnTabla) {
        window.app.cargarNotasEnTabla();
    } else {
        console.error("Error: app.cargarNotasEnTabla no está disponible.");
    }
}