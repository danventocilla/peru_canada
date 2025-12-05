import { state } from './state.js';
import { cambiarVista, mostrarVistaIntranet, activarProteccionInputs, toggleRegisterForm, volverADashboard } from './ui.js';
import * as Auth from './auth.js';
import * as Chat from './chatbot.js';
import * as Admin from './admin.js'; // Importamos todo lo de admin
import * as Docente from './docente.js';
import { BASE_URL } from './config.js';

console.log("✅ Main.js: Cargado.");

// Definimos window.app con TODAS las funciones necesarias
window.app = {
    cambiarVista, mostrarVistaIntranet, toggleRegisterForm, volverADashboard,
    login: Auth.login, cerrarSesion: Auth.cerrarSesion, 
    toggleChat: Chat.toggleChat, procesarMensaje: Chat.procesarMensaje,
    
    // Funciones de Admin (Conectadas desde admin.js)
    cargarEstadisticas: Admin.cargarEstadisticas,
    cargarPendientes: Admin.cargarPendientes,
    aprobarTutor: Admin.aprobarTutor,
    mostrarListasCompletas: Admin.mostrarListasCompletas,
    abrirPanelAsignacion: Admin.abrirPanelAsignacion,
    guardarAsignacion: Admin.guardarAsignacion,
    adminBuscarNotas: Admin.adminBuscarNotas,

    // Funciones de Docente (Conectadas desde docente.js)
    // ATENCIÓN: Aquí usamos nuestra versión corregida de cargarPanelDocente
    cargarPanelDocente: () => {
        console.log("👨‍🏫 Volviendo al panel de cursos...");
        const listaVistas = ['docente-gestion-notas', 'docente-asistencia-view', 'docente-observaciones-view'];
        listaVistas.forEach(id => {
            const el = document.getElementById(id);
            if (el) el.style.display = 'none';
        });
        const dashboard = document.getElementById('docente-dashboard-cards');
        if (dashboard) {
            dashboard.style.display = 'flex'; 
            Docente.cargarPanelDocente(); // Llamamos a la lógica interna
        }
    },
    abrirGestionNotas: Docente.abrirGestionNotas,
    abrirTomarAsistencia: Docente.abrirTomarAsistencia,
    abrirPanelObservacionesDocente: Docente.abrirPanelObservacionesDocente,
    registrarObservacion: Docente.registrarObservacion,
    cargarNotasPorBimestre: Docente.cargarNotasPorBimestre,
    guardarNotasMasivo: Docente.guardarNotasMasivo,
    seleccionarEstadoAsistencia: Docente.seleccionarEstadoAsistencia,
    marcarAsistenciaMasiva: Docente.marcarAsistenciaMasiva,
    guardarAsistenciaMasivo: Docente.guardarAsistenciaMasivo,
    cargarAsistenciaPorFecha: Docente.cargarAsistenciaPorFecha,

    // Funciones de Registro Manual (Tutor/Docente) en el Login
    procesarRegistroTutor: () => {
        const data = {
            nombreT: document.getElementById('reg-t-nombre').value.trim(),
            dniT: document.getElementById('reg-t-dni').value.trim(),
            telT: document.getElementById('reg-t-tel').value.trim(),
            nombreHijo: document.getElementById('reg-st-nombre').value.trim(),
            dniHijo: document.getElementById('reg-st-dni').value.trim(),
            idGrado: document.getElementById('reg-st-grado').value,
            user: document.getElementById('reg-t-user').value.trim(),
            pass: document.getElementById('reg-t-pass').value.trim()
        };
        if (!data.nombreT || !data.dniT || !data.user || !data.pass) { alert("Complete datos obligatorios."); return; }
        Auth.registrarTutor(data);
    },
    procesarRegistroDocente: () => {
        const data = {
            nombres: document.getElementById('reg-d-nombre').value,
            dni: document.getElementById('reg-d-dni').value,
            apellidos: document.getElementById('reg-d-apellido').value,
            user: document.getElementById('reg-d-user').value,
            pass: document.getElementById('reg-d-pass').value
        };
        Auth.registrarDocente(data);
    },

    // Funciones de Tutor (Perfil y Notas)
    actualizarTelefono: async () => {
        const nuevoTel = document.getElementById('input-nuevo-telefono').value.trim();
        if (!/^\d{9}$/.test(nuevoTel)) { alert("⚠️ El celular debe tener 9 dígitos."); return; }
        try {
            const response = await fetch(`${BASE_URL}/perfil/actualizar`, {
                method: 'POST', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ dni: state.dniUsuario, telefono: nuevoTel })
            });
            const data = await response.json();
            if (data.success) { alert("✅ Celular actualizado."); } else { alert("❌ Error al actualizar."); }
        } catch (e) { alert("Error de conexión."); }
    },

    cargarNotasEnTabla: async () => {
        console.log("📚 Cargando notas...");
        // Ocultar otros paneles para asegurar vista limpia
        const paneles = ['dashboard-cards', 'asistencia-tutor-view', 'observaciones-view', 'dashboard-admin'];
        paneles.forEach(id => {
            const el = document.getElementById(id);
            if(el) el.style.display = 'none';
        });
        
        const panelNotas = document.getElementById('notas-detalle');
        if(panelNotas) panelNotas.style.display = 'block';

        const tbody = document.querySelector('#tabla-notas tbody');
        if(!tbody) return;
        
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-primary py-3"><div class="spinner-border spinner-border-sm"></div> Cargando...</td></tr>';

        try {
            const dni = state.dniUsuario; 
            if (!dni) { alert("⚠️ Error de sesión."); return; }

            const res = await fetch(`${BASE_URL}/notas/${dni}`);
            const notas = await res.json();
            tbody.innerHTML = ''; 
            
            if(notas.length === 0) { 
                tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted p-4">📭 No se encontraron notas.</td></tr>'; 
                return; 
            }
            
            notas.forEach(n => {
                const valor = parseFloat(n.valor);
                let badge = valor >= 11 ? '<span class="badge bg-success">Aprobado</span>' : '<span class="badge bg-danger">Recuperación</span>';
                const nombreCurso = n.nombreCurso || n.curso || ('Curso ' + (n.idCurso || '?'));
                const notaFormateada = valor < 10 ? '0' + valor : valor;

                tbody.innerHTML += `<tr><td class="fw-bold text-dark">${nombreCurso}</td><td class="text-muted">${n.bimestre}° Bimestre</td><td class="text-center fw-bold fs-5">${notaFormateada}</td><td class="text-center">${badge}</td></tr>`;
            });
        } catch(e) { 
            tbody.innerHTML = '<tr><td colspan="4" class="text-center text-danger p-3">❌ Error de conexión.</td></tr>';
        }
    }
};

// --- FUNCIÓN DE INICIO ---
function iniciarAplicacion() {
    console.log("🚀 Aplicación iniciada.");
    activarProteccionInputs();
    
    const sesionGuardada = localStorage.getItem('sesion_escolar');
    
    if (sesionGuardada) {
        const s = JSON.parse(sesionGuardada);
        
        // 1. Restaurar Estado
        state.isLogged = true;
        state.usuario = s.usuario;
        state.rol = s.rol || s.rolUsuario; 
        state.rolUsuario = s.rol || s.rolUsuario; 
        state.dniUsuario = s.dni;
        
        // 2. Mostrar Bienvenida
        const welcomeEl = document.getElementById('user-welcome');
        if(welcomeEl) welcomeEl.innerHTML = `Bienvenido, ${s.nombre || s.usuario}`; 
        
        // 3. Activar la vista general y DELEGAR la lógica a UI.JS
        cambiarVista('intranet');
        mostrarVistaIntranet('dashboard'); 

    } else {
        cambiarVista('public');
    }

    // --- LISTENERS ---
    const btnAcceso = document.getElementById('btn-acceso-directo');
    if(btnAcceso) btnAcceso.onclick = () => cambiarVista('login');
    
    const formLogin = document.getElementById('form-login');
    if(formLogin) formLogin.onsubmit = (e) => {
        e.preventDefault();
        Auth.login(document.getElementById('login-user').value, document.getElementById('login-pass').value);
    };

    const btnLogout = document.getElementById('btn-logout');
    if(btnLogout) btnLogout.addEventListener('click', Auth.cerrarSesion);
    
    // Listener Registro Docente
    const formDocente = document.getElementById('form-register-docente');
    if(formDocente) {
        const btnDoc = formDocente.querySelector('button');
        if(btnDoc) {
             btnDoc.type = "button"; 
             btnDoc.onclick = () => window.app.procesarRegistroDocente();
        }
    }

    // Chatbot listeners
    const chatBtn = document.getElementById('chatbot-button');
    if(chatBtn) chatBtn.addEventListener('click', () => Chat.toggleChat());
    
    const closeChat = document.getElementById('btn-close-chat');
    if(closeChat) closeChat.addEventListener('click', () => Chat.toggleChat());
    
    const sendChat = document.getElementById('chatbot-send');
    if(sendChat) sendChat.addEventListener('click', () => Chat.procesarMensaje());


    // =======================================================
    // 🛡️ PARCHE NUCLEAR 3.0: MANEJADOR MAESTRO DE BOTONES
    // =======================================================
    document.addEventListener('click', async (e) => {

        // --- CASO 1: BOTÓN DE ASISTENCIA ---
        const btn = e.target.closest('#btn-asistencia');
        if (btn) {
            e.preventDefault();
            const dni = state.dniUsuario;
            if (!dni) { alert("⚠️ Error de sesión."); return; }

            const textoOriginal = btn.innerText;
            btn.innerText = "Cargando...";
            btn.disabled = true;

            try {
                const response = await fetch(`${BASE_URL}/asistencia/tutor/${dni}`);
                const result = await response.json();

                const vistaDashboard = document.getElementById('dashboard-cards');
                const vistaAsistencia = document.getElementById('asistencia-tutor-view');
                const tbody = document.querySelector('#tabla-asistencia-tutor tbody');
                
                // Ocultar otros paneles para limpieza visual
                ['notas-detalle', 'observaciones-view'].forEach(id => {
                    const el = document.getElementById(id);
                    if(el) el.style.display = 'none';
                });

                if (vistaDashboard && vistaAsistencia && tbody) {
                    tbody.innerHTML = '';
                    if (result.success && result.data.length > 0) {
                        result.data.forEach(fila => {
                            let badgeColor = 'bg-secondary';
                            let icono = '';
                            if (fila.estado === 'Presente') { badgeColor = 'bg-success'; icono = '✅'; }
                            else if (fila.estado === 'Falta') { badgeColor = 'bg-danger'; icono = '❌'; }
                            else if (fila.estado === 'Tardanza') { badgeColor = 'bg-warning text-dark'; icono = '⏰'; }

                            // Usamos una función simple para la fecha si no existe la otra
                            const fechaShow = fila.fecha; 
                            const cursoShow = fila.curso || 'General';

                            tbody.innerHTML += `
                                <tr>
                                    <td>${fechaShow}</td>
                                    <td class="text-primary fw-bold">${cursoShow}</td>
                                    <td><span class="fw-bold">${fila.hijo}</span></td>
                                    <td class="text-center"><span class="badge ${badgeColor}">${icono} ${fila.estado}</span></td>
                                </tr>`;
                        });
                    } else {
                        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted p-4">No hay registros recientes.</td></tr>';
                    }
                    vistaDashboard.style.display = 'none';
                    vistaAsistencia.style.display = 'block';
                }
            } catch (err) { alert("❌ Error de conexión."); } 
            finally { btn.innerText = textoOriginal; btn.disabled = false; }
        }

        // --- CASO 2: BOTÓN DE OBSERVACIONES ---
        const btnObs = e.target.closest('#btn-observaciones');
        if (btnObs) {
            e.preventDefault();
            const dni = state.dniUsuario;
            
            // Ocultar paneles
            ['dashboard-cards', 'asistencia-tutor-view', 'notas-detalle'].forEach(id => {
                const el = document.getElementById(id);
                if(el) el.style.display = 'none';
            });
            document.getElementById('observaciones-view').style.display = 'block';

            const tbody = document.getElementById('tabla-observaciones');
            if (tbody) {
                tbody.innerHTML = '<tr><td colspan="4" class="text-center py-3"><div class="spinner-border spinner-border-sm text-warning"></div> Buscando reportes...</td></tr>';
                try {
                    const res = await fetch(`${BASE_URL}/observaciones/ver/${dni}`);
                    if (res.ok) {
                        const data = await res.json();
                        tbody.innerHTML = '';
                        if (data.length === 0) {
                            tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted p-4">🎉 ¡Excelente! No se registran observaciones.</td></tr>';
                        } else {
                            data.forEach(o => {
                                let color = 'bg-secondary';
                                if(o.gravedad === 'Leve') color = 'bg-info text-dark';
                                if(o.gravedad === 'Grave') color = 'bg-danger';
                                if(o.gravedad === 'Moderado') color = 'bg-warning text-dark';
                                tbody.innerHTML += `<tr><td>${o.fecha}</td><td><div class="fw-bold">${o.titulo || 'Incidente'}</div><small class="text-muted">${o.hijo || 'Estudiante'}</small></td><td class="text-center"><span class="badge ${color}">${o.gravedad}</span></td><td>${o.descripcion}</td></tr>`;
                            });
                        }
                    } else {
                        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">No se encontraron observaciones.</td></tr>';
                    }
                } catch (err) { tbody.innerHTML = '<tr><td colspan="4" class="text-center text-danger">Error de conexión.</td></tr>'; }
            }
        }
        
        // --- CASO 3: BOTÓN ACTUALIZAR DATOS ---
        const btnDatos = e.target.closest('#btn-actualizar-datos');
        if (btnDatos) {
            e.preventDefault();
            window.app.actualizarTelefono();
        }
    });
}

if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', iniciarAplicacion);
else iniciarAplicacion();