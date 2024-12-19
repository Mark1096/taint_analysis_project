package org.example.source;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Scanner;
import java.io.BufferedReader;

public class ClassToAnalyze1 {

    public void processUserInput() {
        // Sorgente: userInput (Non Fidato)
        Scanner scanner = new Scanner(System.in);
        System.out.print("Inserisci un input: ");
        String userInput = scanner.nextLine();
        System.out.println("Hai inserito: " + userInput);

        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Inserisci un testo:");

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Letto: " + line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void processApiResponse() {
        // Sorgente: apiResponse (Non Fidato)
        try {
            URL url = new URL("https://api.example.com/data");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );
            String inputLine;
            StringBuilder apiResponse = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                apiResponse.append(inputLine);
            }
            in.close();

            System.out.println("Risposta API: " + apiResponse.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void processDatabaseQuery() {
        // Sorgente: databaseQuery (Fidato)
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

    public void processFileUpload() {
        // Sorgente: fileUpload (Non Fidato)
        File file = new File("uploaded_file.txt"); // Simula un file caricato dall'utente
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            System.out.println("Contenuto del file caricato:");
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void processCommandLineArg(String[] args) {
        // Sorgente: commandLineArg (Non Fidato)
        if (args.length > 0) {
            String arg = args[0];
            System.out.println("Argomento della linea di comando: " + arg);
        } else {
            System.out.println("Nessun argomento passato.");
        }
    }

    public void processEnvironmentVariable() {
        // Sorgente: environmentVariable (Fidato)
        String envVar = System.getenv("HOME"); // O qualsiasi altra variabile d'ambiente
        System.out.println("Variabile d'ambiente HOME: " + envVar);
    }

    public void processRemoteServiceCall() {
        // Sorgente: remoteServiceCall (Non Fidato)
        try {
            URL url = new URL("https://service.example.com/data.xml");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );
            String inputLine;
            StringBuilder xmlResponse = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                xmlResponse.append(inputLine);
            }
            in.close();

            System.out.println("Risposta XML dal servizio remoto: " + xmlResponse.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void processEmailInput() {
        // Sorgente: emailInput (Non Fidato)
        String emailContent = receiveEmail(); // Simula la ricezione di un'email
        System.out.println("Contenuto dell'email ricevuta: " + emailContent);
    }

    private String receiveEmail() {
        // Metodo fittizio per simulare la ricezione di un'email
        return "Questo è un messaggio di posta elettronica.";
    }

    public void processLogFile() {
        // Sorgente: logFile (Fidato)
        try {
            String logContent = new String(Files.readAllBytes(Paths.get("app.log")));
            System.out.println("Contenuto del file di log:");
            System.out.println(logContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    public void processConfigFile() {
        // Sorgente: configFile (Fidato)
        try (InputStream input = new FileInputStream("config.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            String setting = prop.getProperty("impostazione");
            System.out.println("Valore della configurazione: " + setting);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    */
    public void processInputStream() {
        try {
            InputStream inputStream = new FileInputStream("input.txt");
            int data = inputStream.read();
            while (data != -1) {
                System.out.print((char) data); // Visualizza i dati letti
                data = inputStream.read(); // Leggi il byte successivo
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

