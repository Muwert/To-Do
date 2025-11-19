import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class WebServer {
    private final TaskManager taskManager;
    private HttpServer server;

    public WebServer(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new StaticFileHandler());

        server.createContext("/api/tasks", new TasksApiHandler());
        server.createContext("/api/tasks/complete", new CompleteTaskHandler());
        server.createContext("/api/tasks/delete", new DeleteTaskHandler());
        
        server.setExecutor(null);
        server.start();
        System.out.println("Сервер запущен на http://localhost:" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            if (path.equals("/")) {
                path = "/index.html";
            }

            InputStream resourceStream = getClass().getResourceAsStream("/web" + path);
            
            if (resourceStream != null) {
                byte[] response = resourceStream.readAllBytes();
                String contentType = getContentType(path);
                
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, response.length);
                
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
            } else {
                String response = "404 - Not Found";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }

        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            return "text/plain";
        }
    }

    private class TasksApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String jsonResponse = "{\"tasks\":" + tasksToJson(taskManager.getAllTasks()) + "}";
                sendJsonResponse(exchange, jsonResponse);
                
            } else if ("POST".equals(exchange.getRequestMethod())) {
                String requestBody;
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    requestBody = reader.lines().collect(Collectors.joining("\n"));
                }

                String description = extractDescriptionFromJson(requestBody);
                
                try {
                    Task task = taskManager.addTask(description);
                    String jsonResponse = "{\"success\":true,\"task\":" + taskToJson(task) + "}";
                    sendJsonResponse(exchange, jsonResponse);
                } catch (IllegalArgumentException e) {
                    String jsonResponse = "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}";
                    sendJsonResponse(exchange, jsonResponse, 400);
                }
            }
        }
    }

    private class CompleteTaskHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                int taskId = extractTaskIdFromQuery(query);
                
                boolean success = taskManager.completeTask(taskId);
                String jsonResponse = "{\"success\":" + success + "}";
                sendJsonResponse(exchange, jsonResponse);
            }
        }
    }

    private class DeleteTaskHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                int taskId = extractTaskIdFromQuery(query);
                
                boolean success = taskManager.deleteTask(taskId);
                String jsonResponse = "{\"success\":" + success + "}";
                sendJsonResponse(exchange, jsonResponse);
            }
        }
    }

    private void sendJsonResponse(HttpExchange exchange, String json) throws IOException {
        sendJsonResponse(exchange, json, 200);
    }

    private void sendJsonResponse(HttpExchange exchange, String json, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, response.length);
        
        OutputStream os = exchange.getResponseBody();
        os.write(response);
        os.close();
    }

    private String tasksToJson(List<Task> tasks) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < tasks.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(taskToJson(tasks.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private String taskToJson(Task task) {
        return String.format(
            "{\"id\":%d,\"description\":\"%s\",\"completed\":%s}",
            task.getId(),
            escapeJson(task.getDescription()),
            task.isCompleted()
        );
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private String extractDescriptionFromJson(String json) {
        int start = json.indexOf("\"description\":\"");
        if (start == -1) return "";
        
        start += 15;
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        
        return json.substring(start, end).replace("\\\"", "\"");
    }

    private int extractTaskIdFromQuery(String query) {
        if (query == null) return -1;
        
        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && "id".equals(keyValue[0])) {
                try {
                    return Integer.parseInt(keyValue[1]);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }
}
