package org.example.source;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.sql.*;

public class ClassToAnalyze {

    public ClassToAnalyze() throws FileNotFoundException {
    }

    public void getUserInput() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Inserisci il tuo nome: ");
        String userInput = scanner.nextLine();
        System.out.println("Hai inserito: " + userInput);
    }

    public void getApiResponse() throws IOException {
        URL url = new URL("http://api.example.com/data");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        System.out.println("Risposta API: " + content);
    }

    public void handleFileUpload(String filePath) throws IOException {
        File file = new File(filePath);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("Contenuto del file: " + line);
        }
        reader.close();
    }

    public void readEnvironmentVariable() {
        String path = System.getenv("PATH");
        System.out.println("Variabile d'ambiente PATH: " + path);
    }

    public void callRemoteService() throws IOException {
        URL url = new URL("http://remote.example.com/service");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        System.out.println("Risposta dal servizio remoto: " + content.toString());
    }

    public void readLogFile(String logFilePath) throws IOException {
        File logFile = new File(logFilePath);
        BufferedReader reader = new BufferedReader(new FileReader(logFile));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("Log: " + line);
        }
        reader.close();
    }

    public void readConfigFile(String configFilePath) throws IOException {
        File configFile = new File(configFilePath);
        BufferedReader reader = new BufferedReader(new FileReader(configFile));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("Configurazione: " + line);
        }
        reader.close();
    }
    
    public void processDatabaseQuery() {
        String jdbcUrl = "jdbc:mysql://localhost:3306/mydatabase";
        String username = "utente";
        String password = "password";
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {
            String sql = "SELECT * FROM utenti";
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String nome = rs.getString("nome");
                System.out.println("Utente: " + nome);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void processInputStream() {
        try {
            InputStream inputStream = new FileInputStream("input.txt");
            int data = inputStream.read();
            while (data != -1) {
                System.out.print((char) data);
                data = inputStream.read();
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
