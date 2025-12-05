package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class GoogleAIService {

    // TU API KEY
    private static final String API_KEY = "AIzaSyA3o4x-_G3Uc3YjAS8A2MHFV1OnloUwP80";

    // URL del modelo
    private static final String URL_API = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;

    /**
     * Método principal para hablar con Gemini.
     * @param nombreUsuario Nombre de la persona (o "Visitante").
     * @param pregunta Lo que escribió el usuario.
     * @param instruccionesSistema El contexto o instrucciones especiales (BD, roles, redirección).
     */
    public String preguntarIA(String nombreUsuario, String pregunta, String instruccionesSistema) {
        try {
            // 1. CONSTRUCCIÓN DEL PROMPT MAESTRO
            // Aquí unimos todo para que la IA entienda el contexto completo.
            String promptFinal =
                    "INSTRUCCIONES DEL SISTEMA:\n" + instruccionesSistema + "\n\n" +
                            "CONTEXTO DE USUARIO:\n" +
                            "Estás hablando con: " + nombreUsuario + ".\n" +
                            "PREGUNTA DEL USUARIO:\n" + pregunta;

            // 2. Construir el JSON (Escapamos comillas y saltos de línea para que no rompa el JSON)
            // Nota: .replace("\"", "\\\"") escapa las comillas dobles dentro del texto.
            //       .replace("\n", "\\n") convierte los saltos de línea reales en saltos de línea literales para JSON.
            String jsonTexto = promptFinal.replace("\"", "\\\"").replace("\n", "\\n");

            String jsonBody = "{ \"contents\": [{ \"parts\": [{ \"text\": \"" + jsonTexto + "\" }] }] }";

            // 3. Enviar la solicitud HTTP
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL_API))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            // 4. Recibir respuesta
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 5. Devolver solo el texto limpio
            return extraerTexto(response.body());

        } catch (Exception e) {
            e.printStackTrace();
            return "Lo siento, tuve un problema de conexión con el cerebro del sistema.";
        }
    }

    // Método de limpieza para sacar solo la respuesta de Gemini del JSON gigante que devuelve Google
    private String extraerTexto(String jsonBruto) {
        try {
            String marcador = "\"text\": \"";
            int inicio = jsonBruto.indexOf(marcador);

            if (inicio != -1) {
                inicio += marcador.length();
                int fin = jsonBruto.indexOf("\"", inicio);

                // Extraemos el texto crudo
                String respuestaLimpia = jsonBruto.substring(inicio, fin);

                // Limpiamos caracteres de escape JSON para que se vea bien en el chat
                return respuestaLimpia
                        .replace("\\n", "<br>") // Convertimos saltos de línea a HTML para el chat web
                        .replace("\\\"", "\"");
            } else {
                // Si falla (por ejemplo, si la API devuelve error de cuota), devolvemos el error crudo para depurar
                return "Error de la API: " + jsonBruto;
            }
        } catch (Exception e) {
            return "Error procesando la respuesta.";
        }
    }
}