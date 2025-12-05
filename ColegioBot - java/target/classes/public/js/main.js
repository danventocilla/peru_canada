// 1. Definimos la URL base de tu servidor (Backend)
const API_URL = "http://localhost:4567/api";

/**
 * Función que se ejecuta cuando das clic al botón "Aprobar"
 * @param {number} idTutor - El ID del tutor que viene de la base de datos
 */
function aprobarTutor(idTutor) {
    console.log("Intentando aprobar al tutor con ID:", idTutor);

    // 1. Pedir confirmación al usuario para no borrar por error
    if (!confirm("¿Estás seguro de que deseas ACTIVAR la cuenta de este tutor?")) {
        return; // Si dice "Cancelar", no hacemos nada.
    }

    // 2. Enviar la orden al servidor usando fetch (AJAX)
    fetch(`${API_URL}/admin/aprobarTutor`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        // Convertimos el ID a formato JSON: { "id": 15 }
        body: JSON.stringify({ id: idTutor })
    })
    .then(response => {
        // Verificamos si la respuesta de red es correcta
        if (!response.ok) {
            throw new Error("Error en la red o servidor no encontrado");
        }
        return response.json(); // Convertimos la respuesta del servidor a texto legible
    })
    .then(data => {
        // 3. Analizamos qué nos respondió el servidor (Java)
        if (data.success) {
            alert("✅ ¡Cuenta activada con éxito!");
            // Recargamos la página para que la tabla se actualice sola
            location.reload();
        } else {
            // Si el servidor dice que no (ej. ID no existe)
            alert("❌ Error: " + (data.mensaje || "No se pudo realizar la acción."));
        }
    })
    .catch(error => {
        // 4. Si hay un error técnico (servidor apagado, sin internet, etc.)
        console.error("Error grave:", error);
        alert("⚠️ Error de conexión. Asegúrate de que el ApiServer esté corriendo (botón Play en IntelliJ).");
    });
}