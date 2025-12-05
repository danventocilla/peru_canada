import { BASE_URL } from './config.js';
import { state } from './state.js';
import { iniciarSesionExitosa } from './auth.js';

const chatUI = {
    window: document.getElementById('chatbot-window'),
    body: document.getElementById('chatbot-body'),
    input: document.getElementById('chatbot-input')
};

export function toggleChat(forceOpen = false) {
    const isHidden = chatUI.window.style.display === 'none' || chatUI.window.style.display === '';
    if (isHidden || forceOpen) {
        chatUI.window.style.display = 'flex';
        chatUI.window.style.flexDirection = 'column';
        if (!state.chatIniciado) activarModoLogin();
        chatUI.input.focus();
    } else {
        chatUI.window.style.display = 'none';
    }
}

function activarModoLogin() {
    state.chatIniciado = true;
    chatUI.body.innerHTML = ''; 
    chatUI.input.placeholder = "Ingrese su DNI (8 dígitos)...";
    agregarMensajeBotHTML(`🔐 <strong>Acceso a Intranet</strong><br>DNI para iniciar sesión.`);
}

// EN chatbot.js

export async function procesarMensaje() {
    const texto = chatUI.input.value.trim();
    if (!texto) return;
    agregarMensajeUsuario(texto);
    chatUI.input.value = '';

    // Lógica DNI
    if (/^\d{8}$/.test(texto)) state.dniUsuario = texto;

    // A. Si ya está logueado, consulta IA
    if (state.usuario) { 
        consultarIA(state.usuario, texto); 
        return; 
    }
    
    // B. Modo libre
    const checkModo = document.getElementById('mode-free');
    if (checkModo && (checkModo.checked || state.modoConsulta)) { 
        consultarIA("Visitante", texto); 
        return; 
    }

    // C. LOGIN VÍA CHAT
    agregarMensajeBot("Verificando...");
    
    try {
        // Enviamos user y dni para asegurar compatibilidad con tu Backend parcheado
        const res = await fetch(`${BASE_URL}/login`, { 
            method: 'POST', 
            body: JSON.stringify({ dni: texto, user: texto }) 
        });
        
        const data = await res.json();
        
        if (data.success) {
            agregarMensajeBot(`✅ ¡Bienvenido ${data.nombre}! Entrando...`);

            // --- AQUÍ ESTABA EL PROBLEMA: EL SELLO ---
            // Ahora usamos EXACTAMENTE la misma estructura que auth.js
            
            const sesionData = {
                usuario: data.nombre,
                dni: data.dni,
                rol: data.rol,
                isLogged: true
            };

            // Guardamos con la llave CORRECTA 'sesion_escolar'
            localStorage.setItem('sesion_escolar', JSON.stringify(sesionData));
            
            // Actualizamos estado local por si acaso
            state.usuario = data.nombre;
            state.rol = data.rol;
            state.isLogged = true;

            // --- REDIRECCIÓN ---
            setTimeout(() => {
                // Te mandamos al panel principal
                window.location.href = "colegio.html"; 
            }, 1500);

        } else {
            agregarMensajeBot("❌ " + (data.mensaje || "DNI no encontrado."));
        }
    } catch (e) { 
        console.error(e);
        agregarMensajeBot("⚠️ Error de conexión."); 
    }
}

async function consultarIA(usuario, mensaje) {
    // Verificar modo (con protección por si el elemento no existe)
    const checkModo = document.getElementById('mode-free');
    const modoLibre = checkModo ? checkModo.checked : false;
    const promptContexto = modoLibre ? "MODO_LIBRE" : "MODO_COLEGIO";

    try {
        const payload = { 
            usuario, mensaje, 
            dniContexto: state.dniUsuario,
            modo: promptContexto
        };
        const res = await fetch(`${BASE_URL}/chat`, { method: 'POST', body: JSON.stringify(payload) });
        const data = await res.json();

        // Acciones especiales
        if (data.reply && data.reply.includes("[REDIRECT_NOTAS]")) {
            // Intentamos redirigir a notas si la IA lo pide
            window.location.href = "notas.html"; 
            return;
        }
        agregarMensajeBot(data.reply);
    } catch (e) { agregarMensajeBot("IA desconectada."); }
}

function agregarMensajeUsuario(msg) {
    chatUI.body.innerHTML += `<div class="chat-message user">${msg}</div>`;
    chatUI.body.scrollTop = chatUI.body.scrollHeight;
}
function agregarMensajeBot(msg) {
    chatUI.body.innerHTML += `<div class="chat-message bot">${msg}</div>`;
    chatUI.body.scrollTop = chatUI.body.scrollHeight;
}
function agregarMensajeBotHTML(html) {
    chatUI.body.innerHTML += `<div class="chat-message bot">${html}</div>`;
    chatUI.body.scrollTop = chatUI.body.scrollHeight;
}