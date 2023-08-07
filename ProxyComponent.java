import java.io.*;
import java.net.*;

public class ProxyComponent {
    public static void main(String[] args) {
        int port = 8081; // Puerto en el que el proxy escuchará
        try {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Proxy HTTP escuchando en el puerto " + port);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new ProxyThread(clientSocket).start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ProxyThread extends Thread {
    private Socket clientSocket;

    public ProxyThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void run() {
        try {
            // Obtener flujos de entrada y salida para el cliente
            BufferedReader clientInput = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            DataOutputStream clientOutput = new DataOutputStream(clientSocket.getOutputStream());

            // Leer la solicitud del cliente
            String request = clientInput.readLine();
            System.out.println("Solicitud del cliente: " + request);

            // Parsear la solicitud para obtener la URL
            String[] requestParts = request.split(" ");
            String urlString = requestParts[1];
            URL url = new URL(urlString);

            // Modificar la URL para apuntar al servidor Spring Boot en el puerto 8080
            URL modifiedUrl = new URL("http://localhost:8080" + url.getPath());

            // Establecer conexión con el servidor Spring Boot
            HttpURLConnection serverConnection = (HttpURLConnection) modifiedUrl.openConnection();

            // Copiar cabeceras de la solicitud del cliente a la solicitud al servidor Spring Boot
            String headerLine;
            while ((headerLine = clientInput.readLine()).length() != 0) {
                serverConnection.addRequestProperty(headerLine.split(": ")[0], headerLine.split(": ")[1]);
            }

            // Obtener flujos de entrada y salida para la respuesta del servidor Spring Boot
            InputStream serverInput = serverConnection.getInputStream();
            DataOutputStream serverOutput = new DataOutputStream(clientSocket.getOutputStream());

            // Copiar la respuesta del servidor Spring Boot de vuelta al cliente
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = serverInput.read(buffer)) != -1) {
                clientOutput.write(buffer, 0, bytesRead);
            }

            // Cerrar conexiones
            serverConnection.disconnect();
            clientInput.close();
            clientOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}