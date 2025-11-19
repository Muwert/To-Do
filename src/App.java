public class App {
    public static void main(String[] args) throws Exception {
        TaskManager manager = new TaskManager();
        WebServer webServer = new WebServer(manager);

        webServer.start(8080);

        System.out.println("Веб-интерфейс доступен по адресу: http://localhost:8080");
        System.out.println("Нажмите Enter для остановки сервера...");
        
        System.in.read();
        webServer.stop();
        System.out.println("Сервер остановлен.");
    }
}

