// js/state.js 
export const state = {
    usuario: null,
    dniUsuario: null,
    rol: null, 
    modoConsulta: false,
    chatIniciado: false,
    adminSearchingNotes: false
};

export function limpiarSesion() {
    state.usuario = null;
    state.dniUsuario = null;
    state.rol = null; 
    state.modoConsulta = false;
    state.chatIniciado = false;
}