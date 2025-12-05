import { BASE_URL } from './config.js';
import { state } from './state.js';
import { cambiarVista, mostrarVistaIntranet } from './ui.js';

const jsonHeaders = { 'Content-Type': 'application/json' };

console.log("🔒 Auth.js: Cargado y sincronizado con BD.");

// =========================================================
// 1. LOGIN (COMPATIBLE CON TU BD Y ESTADO TINYINT)
// =========================================================
export async function login(user, pass) {
    console.log("🔵 Intentando login con:", user);
    
    const btn = document.querySelector('#form-login button');
    if(btn) { btn.disabled = true; btn.innerText = "Verificando..."; }

    try {
        const response = await fetch(`${BASE_URL}/auth/login`, {
            method: 'POST', headers: jsonHeaders, body: JSON.stringify({ user, pass })
        });
        const data = await response.json();
        
        if (data.success) {
            console.log("✅ Login exitoso. Rol detected:", data.rol);
            
            // A. Guardar en Memoria RAM
            state.isLogged = true;
            state.usuario = data.nombre;
            state.dniUsuario = data.dni; // Importante: Tu BD devuelve el DNI del tutor/docente
            state.rol = data.rol;        // 'admin', 'docente' o 'tutor'

            // B. Guardar en Disco (LocalStorage) para soportar F5
            localStorage.setItem('sesion_escolar', JSON.stringify({
                usuario: data.nombre,
                dni: data.dni,
                rol: data.rol,
                isLogged: true
            }));

            // C. Entrar al sistema
            iniciarSesionExitosa();

        } else {
            console.warn("⚠️ Login rechazado:", data.mensaje);
            // Esto manejará el mensaje de "Cuenta PENDIENTE" si el estado en BD es 0
            const alertError = document.getElementById('login-error');
            if(alertError) {
                alertError.innerText = data.mensaje || "❌ Credenciales incorrectas";
                alertError.classList.remove('d-none');
                
                // Ocultar alerta automáticamente después de 5 seg
                setTimeout(() => alertError.classList.add('d-none'), 5000);
            } else { 
                alert(data.mensaje); 
            }
        }
    } catch (err) { 
        console.error("❌ Error crítico en Login:", err);
        alert("Error de conexión con el servidor (ApiServer)."); 
    } finally {
        if(btn) { btn.disabled = false; btn.innerText = "Iniciar Sesión"; }
    }
}

// =========================================================
// 2. CONFIGURACIÓN DE PANELES (UI)
// =========================================================
export function configurarDashboard(rol) {
    // Ocultar todos los paneles primero
    const pDocente = document.getElementById('dashboard-docente');
    const pAdmin   = document.getElementById('dashboard-admin');
    const pTutor   = document.getElementById('dashboard-cards');

    if(pDocente) pDocente.style.display = 'none';
    if(pAdmin)   pAdmin.style.display = 'none';
    if(pTutor)   pTutor.style.display = 'none';

    // Mostrar el que corresponde según tu BD
    if (rol === 'tutor') {
        if(pTutor) pTutor.style.display = 'flex';
    } else if (rol === 'docente') {
        if(pDocente) pDocente.style.display = 'block';
        if(window.app && window.app.cargarPanelDocente) window.app.cargarPanelDocente();
    } else if (rol === 'admin') {
        if(pAdmin) pAdmin.style.display = 'block';
        if(window.app && window.app.cargarEstadisticas) window.app.cargarEstadisticas();
    }
}

// =========================================================
// 3. UTILIDADES DE SESIÓN
// =========================================================
export function iniciarSesionExitosa() {
    state.modoConsulta = true;
    
    // Mensaje de bienvenida
    const welcomeTitle = document.getElementById('user-welcome');
    if(welcomeTitle && state.usuario) {
        welcomeTitle.innerHTML = `Bienvenido, ${state.usuario} <br><small class="badge bg-dark">${state.rol ? state.rol.toUpperCase() : 'USUARIO'}</small>`;
    }

    cambiarVista('intranet');
    configurarDashboard(state.rol);
}

export function cerrarSesion() {
    state.isLogged = false;
    state.usuario = null;
    state.dniUsuario = null;
    state.rol = null;

    localStorage.removeItem('sesion_escolar'); // Borrar del disco

    cambiarVista('public');
    
    // Limpiar formulario
    const u = document.getElementById('login-user');
    const p = document.getElementById('login-pass');
    if(u) u.value = "";
    if(p) p.value = "";
    
    location.reload();
}

// =========================================================
// 4. REGISTRO (INSERT EN BD)
// =========================================================

// REGISTRO TUTOR (Inserta en tablas: usuarios, tutores, estudiantes)
export async function registrarTutor(payload) {
    const btnSubmit = document.querySelector('#form-register-tutor button[type="submit"]');
    const textoOriginal = btnSubmit ? btnSubmit.innerText : "Registrar";
    
    if(btnSubmit) { btnSubmit.disabled = true; btnSubmit.innerText = "Guardando..."; }

    try {
        const response = await fetch(`${BASE_URL}/auth/register/tutor`, {
            method: 'POST', headers: jsonHeaders, body: JSON.stringify(payload)
        });
        const data = await response.json();
        
        if (data.success) { 
            // Mensaje de éxito (Estado 0 en BD)
            alert(data.mensaje || "🎉 ¡Familia registrada!\nSu cuenta requiere aprobación del Admin."); 
            location.reload(); 
        } else { 
            alert("❌ Error: " + (data.mensaje || "El DNI o Usuario ya existe.")); 
        }
    } catch (err) { 
        console.error(err);
        alert("Error de conexión."); 
    } finally {
        if(btnSubmit) { btnSubmit.disabled = false; btnSubmit.innerText = textoOriginal; }
    }
}

// REGISTRO DOCENTE (Inserta en tablas: usuarios, docentes)
export async function registrarDocente(payload) {
    const btnSubmit = document.querySelector('#form-register-docente button[type="submit"]');
    const textoOriginal = btnSubmit ? btnSubmit.innerText : "Registrar Docente";
    
    if(btnSubmit) { btnSubmit.disabled = true; btnSubmit.innerText = "Guardando..."; }

    try {
        const response = await fetch(`${BASE_URL}/auth/register/docente`, {
            method: 'POST', headers: jsonHeaders, body: JSON.stringify(payload)
        });
        const data = await response.json();
        
        if (data.success) { 
            alert(data.mensaje || "🎉 Registro exitoso. Espere activación."); 
            location.reload(); 
        } else { 
            alert("❌ Error: " + (data.mensaje || "Datos duplicados.")); 
        }
    } catch (err) { 
        console.error(err);
        alert("Error de conexión.");
    } finally {
        if(btnSubmit) { btnSubmit.disabled = false; btnSubmit.innerText = textoOriginal; }
    }
}