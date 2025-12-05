import { state } from './state.js';

// ==========================================
// 1. NAVEGACIÓN PRINCIPAL (Portada / Login / Intranet)
// ==========================================

export function cambiarVista(nombreVista) {
    // Referencias a las vistas principales
    const views = {
        public: document.getElementById('public-view'),
        login: document.getElementById('login-view'),
        intranet: document.getElementById('intranet-view')
    };

    // 1. Ocultar TODO primero
    if(views.public) views.public.style.display = 'none';
    if(views.login) views.login.style.display = 'none';
    if(views.intranet) views.intranet.style.display = 'none';
    
    // Control del Navbar (Visible solo en portada)
    const navbar = document.querySelector('.navbar');
    if(navbar) navbar.style.display = (nombreVista === 'public') ? 'flex' : 'none';

    // 2. Mostrar la vista solicitada
    switch(nombreVista) {
        case 'public':
            if(views.public) views.public.style.display = 'block';
            break;
        case 'login':
            if(views.login) views.login.style.display = 'flex';
            break;
        case 'intranet':
            if(views.intranet) views.intranet.style.display = 'block';
            break;
    }
    
    // Resetear scroll siempre arriba
    window.scrollTo(0, 0);
}

// ==========================================
// 2. NAVEGACIÓN INTERNA (Dentro de la Intranet)
// ==========================================

// js/ui.js

// js/ui.js

export function mostrarVistaIntranet(vista) {
    // 1. OCULTAR TODOS LOS PANELES INTERNOS
    // Agregamos 'docente-dashboard-cards' a la lista para asegurarnos de apagarlo primero
    const paneles = [
        'dashboard-cards', 
        'notas-detalle', 
        'dashboard-docente',        // Nombre nuevo
        'docente-dashboard-cards',  // <--- Nombre antiguo (IMPORTANTE AGREGARLO)
        'docente-gestion-notas', 
        'docente-asistencia-view',  // Agrega este si existe
        'docente-observaciones-view',
        'dashboard-admin', 
        'panel-aprobacion', 
        'admin-asignacion-view', 
        'admin-listas-view', 
        'observaciones-view'
    ];

    paneles.forEach(id => {
        const el = document.getElementById(id);
        if(el) el.style.display = 'none';
    }); 

    // ... (El bloque de LÓGICA DE ROLES / rawRol / rolLimpio SE QUEDA IGUAL) ...
    // ... (No cambies la parte que arregla el texto del perfil) ...

    // 1. Obtenemos el rol crudo y lo limpiamos
    const rawRol = state.rol || state.rolUsuario || '';
    const usuario = state.usuario || '';
    if (usuario === 'admin' && !rawRol) state.rol = 'admin'; // Parche admin
    const rolLimpio = rawRol.toString().toLowerCase().trim(); 

    // Actualización del texto del perfil (sin cambios aquí)
    const labelRol = document.getElementById('perfil-rol');
    if (labelRol) {
        let textoRol = 'Tutor / Padre de familia';
        if (rolLimpio === 'admin' || rolLimpio === 'administrador') textoRol = 'Administrador del Sistema';
        else if (rolLimpio === 'docente') textoRol = 'Docente de Aula';
        labelRol.innerText = `Perfil: ${textoRol}`;
    }

    // ==========================================
    // 2. MOSTRAR EL PANEL CORRESPONDIENTE
    // ==========================================
    if (vista === 'dashboard') {
        
        // CASO ADMIN
        if (rolLimpio === 'admin' || rolLimpio === 'administrador') {
            const pAdmin = document.getElementById('dashboard-admin');
            if(pAdmin) pAdmin.style.display = 'block';
            
            const pAprobar = document.getElementById('panel-aprobacion');
            if(pAprobar) pAprobar.style.display = 'block';

            import('./admin.js').then(m => {
                if(m.cargarEstadisticas) m.cargarEstadisticas();
                if(m.cargarPendientes) m.cargarPendientes();
            }).catch(e => console.error("Error admin", e));
        } 
        
        // CASO DOCENTE (AQUÍ ESTÁ LA CORRECCIÓN CLAVE 🟢)
        else if (rolLimpio === 'docente') {
            // Buscamos con AMBOS nombres posibles para que no falle
            const pDoc = document.getElementById('dashboard-docente') || document.getElementById('docente-dashboard-cards');
            
            if(pDoc) {
                // Usamos 'flex' si son cards, o 'block' si es normal. 
                // Si usas Bootstrap row/col, 'block' o 'flex' suelen funcionar.
                pDoc.style.display = 'flex'; 
                
                // Cargar la lógica del docente
                import('./docente.js').then(module => {
                     if(module.cargarPanelDocente) module.cargarPanelDocente();
                });
            } else {
                console.error("❌ ERROR: No encuentro el DIV del dashboard docente en el HTML.");
                alert("Error: No se encuentra el panel del docente (ID incorrecto en HTML).");
            }
        } 
        
        // CASO TUTOR (Default)
        else {
            const pTutor = document.getElementById('dashboard-cards');
            if(pTutor) pTutor.style.display = 'flex';
        }
    }
    
    // ... (Resto de la función igual: notas-detalle, etc.) ...
    if (vista === 'notas-detalle') {
        const pNotas = document.getElementById('notas-detalle');
        if(pNotas) pNotas.style.display = 'block';
    }
    if (vista === 'observaciones-tutor') {
        const pObs = document.getElementById('observaciones-view');
        if(pObs) pObs.style.display = 'block';
    }
}

// ==========================================
// 3. NAVEGACIÓN DE RETORNO (SOLUCIÓN AL BUG ADMIN)
// ==========================================

export function volverADashboard() {
    
    // 1. Verificar si estamos en el flujo de búsqueda de notas del Admin
    if (state.adminSearchingNotes) {
        console.log("⬅️ Admin: Volviendo a Panel de Administración.");
        
        // Desactivar la bandera
        state.adminSearchingNotes = false; 
        
        // Ocultar la vista de notas
        const notasView = document.getElementById('notas-detalle');
        if(notasView) notasView.style.display = 'none';

        // Mostrar el panel de admin forzando la vista (reutilizando la lógica de mostrarVistaIntranet)
        mostrarVistaIntranet('dashboard'); 
        
    } else {
        // 2. Flujo normal (Tutor o Docente volviendo al dashboard principal)
        mostrarVistaIntranet('dashboard'); 
    }
}


// ==========================================
// 4. UTILIDADES DE INTERFAZ
// ==========================================

export function toggleRegisterForm(tipo) {
    const formTutor = document.getElementById('form-register-tutor');
    const formDocente = document.getElementById('form-register-docente');
    const btns = document.querySelectorAll('#pills-register .d-flex button');

    if (tipo === 'tutor') {
        formTutor.style.display = 'block';
        formDocente.style.display = 'none';
        // Estilos de botones
        btns[0].classList.add('active', 'btn-outline-primary');
        btns[1].classList.remove('active', 'btn-outline-primary');
    } else {
        formTutor.style.display = 'none';
        formDocente.style.display = 'block';
        // Estilos de botones
        btns[0].classList.remove('active', 'btn-outline-primary');
        btns[1].classList.add('active', 'btn-outline-primary');
    }
}

// ==========================================
// 5. VALIDACIONES DE TEXTO (Anti-Spam)
// ==========================================

export function activarProteccionInputs() {
    console.log("🛡️ Activando protección de inputs de texto...");
    
    // Lista de IDs de los campos que deben ser SOLO TEXTO (Nombres y Apellidos)
    const camposProtegidos = [
        'reg-t-nombre',     // Nombre Tutor
        'reg-st-nombre',    // Nombre Hijo
        'reg-d-nombre',     // Nombre Docente
        'reg-d-apellido'    // Apellido Docente
    ];

    camposProtegidos.forEach(id => {
        const input = document.getElementById(id);
        if (input) {
            // A. Bloqueo en tiempo real: Solo permite letras y espacios
            input.addEventListener('input', (e) => {
                const valorOriginal = e.target.value;
                // Regex: Solo permite a-z, A-Z, áéíóú, ñ, Ñ y espacios
                const valorLimpio = valorOriginal.replace(/[^a-zA-ZáéíóúÁÉÍÓÚñÑ\s]/g, '');
                
                if (valorOriginal !== valorLimpio) {
                    e.target.value = valorLimpio;
                    // Feedback visual (borde rojo momentáneo)
                    e.target.classList.add('border', 'border-danger');
                    setTimeout(() => e.target.classList.remove('border', 'border-danger'), 500);
                }
            });

            // B. Validación de coherencia al salir del campo (Blur)
            input.addEventListener('blur', (e) => {
                const texto = e.target.value.trim();
                if (texto.length === 0) return;

                // Reglas anti-galimatías:
                const tieneVocal = /[aeiouáéíóú]/i.test(texto);
                const repeticiones = /(.)\1\1/.test(texto);
                const muyCorto = texto.length < 2;

                if (!tieneVocal || repeticiones || muyCorto) {
                    alert(`⚠️ El nombre "${texto}" parece incorrecto o mal escrito.\nPor favor ingrese un nombre válido.`);
                    e.target.value = ""; // Limpiar campo
                    setTimeout(() => e.target.focus(), 100); // Regresar el foco
                }
            });
        }
    });
}