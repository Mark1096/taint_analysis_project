package org.example.destination;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

public class InputSanitizer {

    public static String sanitizeUserInput(String input) {
        if (input == null) {
            return "";
        }

        // Step 1: Rimuovi tag HTML per prevenire XSS
        input = input.replaceAll("<[^>]*>", ""); // Elimina tutti i tag HTML/XML.

        // Step 2: Rimuovi script e codici JS potenzialmente nascosti
        input = input.replaceAll("(?i)(<script.*?>.*?</script>|javascript:|on\\w+=)", "");

        // Step 3: Rimuovi parole chiave SQL per prevenire SQL Injection
        input = input.replaceAll("(?i)(DROP|SELECT|INSERT|DELETE|UPDATE|TABLE|FROM|WHERE|--|;|\\*|=)", "");

        // Step 4: Blocca caratteri speciali non sicuri
        input = input.replaceAll("[^a-zA-Z0-9 .,!?@#%&()_-]", ""); // Alfanumerici e simboli sicuri.

        // Step 5: Normalizza sequenze Unicode sospette
        input = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFKC);

        // Step 6: Limita la lunghezza massima dell'input
        if (input.length() > 1024) {
            input = input.substring(0, 1024);
        }

        // Step 7: Trim e rimuovi spazi in eccesso
        return input.strip();
    }

    public static String sanitizeStreamInput(String input) {
        if (input == null) {
            return "";
        }

        // Step 1: Rimuovi caratteri null o byte non stampabili
        input = input.replaceAll("\\p{Cntrl}", ""); // Rimuove caratteri di controllo

        // Step 2: Rimuovi tag HTML o XML
        input = input.replaceAll("<[^>]*>", ""); // Rimuove tag come <script>, <style>, ecc.

        // Step 3: Normalizza sequenze Unicode
        input = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFKC);

        // Step 4: Rimuovi script, URI pericolosi, e altri pattern sospetti
        input = input.replaceAll("(?i)(<script.*?>.*?</script>|javascript:|on\\w+=)", "");

        // Step 5: Blocca parole chiave SQL e simboli dannosi
        input = input.replaceAll("(?i)(DROP|SELECT|INSERT|DELETE|UPDATE|TABLE|FROM|WHERE|--|;|\\*|=)", "");

        // Step 6: Mantieni solo caratteri alfanumerici e simboli di base
        input = input.replaceAll("[^a-zA-Z0-9 .,!?@#%&()\\[\\]{}:;\"'-_+|/\\\\]", ""); // Blocca simboli non sicuri.

        // Step 7: Rimuovi sequenze di directory traversal
        input = input.replaceAll("(\\.\\./|\\.\\.\\\\)", ""); // Rimuove tentativi di navigazione nel file system.

        // Step 8: Limita la lunghezza massima dell'input
        if (input.length() > 4096) { // Adatta il limite a seconda del contesto di streaming
            input = input.substring(0, 4096);
        }

        // Step 9: Trim e rimuovi spazi multipli
        return input.replaceAll("\\s+", " ").strip();
    }

    public static String sanitizeCommandLineArgs(String input) {
        if (input == null) {
            return "";
        }

        // Step 1: Rimuovi caratteri null o byte non stampabili
        input = input.replaceAll("\\p{Cntrl}", ""); // Rimuove caratteri di controllo

        // Step 2: Normalizza sequenze Unicode per evitare spoofing
        input = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFKC);

        // Step 3: Blocca directory traversal e percorsi assoluti
        input = input.replaceAll("(\\.{2,}/|\\.{2,}\\\\|~|/|\\\\)", ""); // Blocca traversal o percorsi non sicuri

        // Step 4: Rimuovi parole chiave sospette specifiche di shell
        input = input.replaceAll("(?i)(rm -rf|sudo|chmod|chown|kill|shutdown|reboot|mkfs|dd|ps|grep)", "");

        // Step 5: Blocca sequenze e simboli specifici della shell
        input = input.replaceAll("([$`|;&<>*?!\"'])", ""); // Rimuove simboli dannosi come `$`, `|`, `;`, ecc.

        // Step 6: Mantieni solo caratteri alfanumerici e simboli di base
        input = input.replaceAll("[^a-zA-Z0-9 .,!?@#%&()\\[\\]{}:;\"'-_+|/\\\\]", ""); // Blocca caratteri non sicuri

        // Step 7: Limita la lunghezza massima dell'input
        if (input.length() > 1024) { // Adatta il limite in base alla lunghezza accettabile degli argomenti
            input = input.substring(0, 1024);
        }

        // Step 8: Rimuovi spazi multipli e trim
        return input.replaceAll("\\s+", " ").strip();
    }

    public static String sanitizeApiResponse(String input) {
        if (input == null) {
            return "";
        }

        // Limita la lunghezza dell'input
        if (input.length() > 4096) {
            throw new IllegalArgumentException("API response too large.");
        }

        // Se il formato è JSON, valida e pulisci
        if (input.trim().startsWith("{") && input.trim().endsWith("}")) {
            return sanitizeJson(input);
        }

        // Se il formato è XML o HTML, usa una libreria per parsare e pulire
        if (input.trim().startsWith("<") && input.trim().endsWith(">")) {
            return sanitizeHtmlOrXml(input);
        }

        // Default: applica sanitizzazione generica per stringhe non strutturate
        return sanitizeString(input);
    }

    private static String sanitizeJson(String json) {
        // Usa una libreria come Jackson per validare il JSON
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(json);

            // Manipola i nodi per sanitizzare i valori
            sanitizeJsonNode(rootNode);

            return objectMapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON input.");
        }
    }

    // Metodo di supporto per sanitizzare i nodi JSON
    private static void sanitizeJsonNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.fields().forEachRemaining(entry -> {
                JsonNode childNode = entry.getValue();
                if (childNode.isTextual()) {
                    // Sanitizza valori stringa
                    objectNode.put(entry.getKey(), sanitizeString(childNode.asText()));
                } else if (childNode.isContainerNode()) {
                    // Ricorsivamente sanitizza nodi figli
                    sanitizeJsonNode(childNode);
                }
            });
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode childNode = arrayNode.get(i);
                if (childNode.isTextual()) {
                    // Sanitizza valori stringa negli array
                    arrayNode.set(i, new TextNode(sanitizeString(childNode.asText())));
                } else if (childNode.isContainerNode()) {
                    // Ricorsivamente sanitizza nodi figli
                    sanitizeJsonNode(childNode);
                }
            }
        }
    }

    private static String sanitizeHtmlOrXml(String input) {
        // Usa Jsoup per analizzare e ripulire l'HTML o XML
        try {
            // Analizza il contenuto come documento Jsoup
            Document document = Jsoup.parse(input);

            // Rimuove elementi pericolosi come script, iframe, embed, ecc.
            document.select("script, iframe, object, embed, link, meta").remove();

            // Rimuove attributi potenzialmente dannosi (es. eventi JavaScript o stili)
            document.select("*").forEach(element -> {
                // Rimuovi attributi pericolosi
                element.removeAttr("onmouseover")
                        .removeAttr("onclick")
                        .removeAttr("onerror")
                        .removeAttr("style")
                        .removeAttr("src")
                        .removeAttr("href");
            });

            // Codifica entità HTML per prevenire XSS
            return Jsoup.clean(document.html(), "", Safelist.basic());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid HTML or XML input.");
        }
    }

    public static String sanitizeRemoteServiceCall(String input) {
        if (input == null) {
            return "";
        }

        // Step 1: Rimuovi caratteri di controllo (byte non stampabili)
        input = input.replaceAll("\\p{Cntrl}", ""); // Esclude caratteri come \0, \b, ecc.

        // Step 2: Normalizza l'input per prevenire spoofing Unicode
        input = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFKC);

        // Step 3: Rimuovi sequenze di escape dannose o non necessarie
        input = input.replaceAll("([\\\\%$<>`|;{}\\[\\]])", ""); // Blocca simboli di escape e metacaratteri

        // Step 4: Decodifica input codificati (ad esempio, URL o Base64) in modo sicuro
        try {
            input = java.net.URLDecoder.decode(input, "UTF-8"); // Decodifica URL
        } catch (Exception e) {
            // Ignora errori di decodifica, il che significa che l'input non era codificato
        }

        // Step 5: Valida e sanifica specifici formati come JSON o XML
        if (isJson(input)) {
            input = sanitizeJson(input); // Uso del metodo JSON specifico
        } else if (isXml(input)) {
            input = sanitizeHtmlOrXml(input); // Uso del metodo HTML/XML
        }

        // Step 6: Mantieni solo caratteri alfanumerici e simboli di base
        input = input.replaceAll("[^a-zA-Z0-9 .,!?@#%&()\\[\\]{}:;\"'-_+|/\\\\]", ""); // Blocca caratteri non sicuri

        // Step 7: Limita la lunghezza dell'input per prevenire DoS
        if (input.length() > 2048) { // Adatta la lunghezza massima al contesto
            input = input.substring(0, 2048);
        }

        // Step 8: Rimuovi spazi multipli e trim
        return input.replaceAll("\\s+", " ").strip();
    }

    private static boolean isJson(String input) {
        try {
            new ObjectMapper().readTree(input); // Usa Jackson per validare JSON
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isXml(String input) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(new InputSource(new StringReader(input)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String sanitizeEmailInput(String input) {
        if (input == null) {
            return "";
        }

        // Step 1: Rimuovi caratteri di controllo (byte non stampabili)
        input = input.replaceAll("\\p{Cntrl}", ""); // Esclude \0, \b, ecc.

        // Step 2: Normalizza l'input per prevenire spoofing Unicode
        input = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFKC);

        // Step 3: Rimuovi script HTML, tag e commenti
        input = input.replaceAll("<!--.*?-->", ""); // Rimuove commenti HTML
        input = input.replaceAll("<[^>]*>", "");   // Rimuove tag HTML

        // Step 4: Decodifica input codificati (ad esempio, URL)
        try {
            input = java.net.URLDecoder.decode(input, "UTF-8"); // Decodifica URL
        } catch (Exception e) {
            // Ignora errori di decodifica
        }

        // Step 5: Rimuovi indirizzi email o link malevoli
        input = input.replaceAll("(?i)mailto:|javascript:|data:|file:", ""); // Blocca protocolli pericolosi
        input = input.replaceAll("(https?|ftp)://[^\\s]+", "");              // Blocca URL

        // Step 6: Mantieni solo caratteri alfanumerici e simboli accettabili
        input = input.replaceAll("[^a-zA-Z0-9@._%+\\-\\s]", ""); // Blocca caratteri non sicuri

        // Step 7: Valida la lunghezza dell'input
        if (input.length() > 512) { // Adatta il limite in base alle esigenze
            input = input.substring(0, 512);
        }

        // Step 8: Rimuovi spazi multipli e trim
        return input.replaceAll("\\s+", " ").strip();
    }

    // Esempio di metodo generico per sanitizzazione delle stringhe
    private static String sanitizeString(String input) {
        if (input == null) {
            return "";
        }
        // Rimuove caratteri potenzialmente pericolosi
        return input.replaceAll("[<>\\\"\\']", "").strip();
    }

    public static String sanitizeUploadFile(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Invalid file name.");
        }

        // Step 1: Rimuovi caratteri di controllo (byte non stampabili)
        input = input.replaceAll("\\p{Cntrl}", ""); // Rimuove caratteri non visibili (\0, \b, ecc.)

        // Step 2: Normalizza l'input per prevenire spoofing Unicode
        input = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFKC);

        // Step 3: Rimuovi percorsi relativi e caratteri non sicuri
        input = input.replaceAll("\\.\\./", "")     // Rimuove path traversal (../)
                .replaceAll("\\\\", "/");      // Uniforma separatori di directory

        // Step 4: Mantieni solo caratteri validi per nomi di file
        input = input.replaceAll("[^a-zA-Z0-9._-]", ""); // Blocca simboli non sicuri

        // Step 5: Limita la lunghezza del nome del file
        if (input.length() > 255) { // Limite standard per i nomi di file su molti file system
            input = input.substring(0, 255);
        }

        // Step 6: Rimuovi nomi di file riservati (esempio: Windows)
        String[] reservedNames = {
                "CON", "PRN", "AUX", "NUL",
                "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
                "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
        };
        for (String reserved : reservedNames) {
            if (input.equalsIgnoreCase(reserved)) {
                throw new IllegalArgumentException("Invalid file name: reserved name.");
            }
        }

        // Step 7: Rimuovi spazi inutili e ritorna il risultato
        return input.strip();
    }

}

