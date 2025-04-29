package taintanalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.collections4.CollectionUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.xml.sax.InputSource;
import taintanalysis.config.ConfigLoader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h1> InputSanitizer </h1>
 *
 * This class is used to provide sanitization methods to be applied to untrusted input data.
 */
public class InputSanitizer {

    /**
     * Map the name of the external data source to a specific sanitization method.
     *
     * @return map string string
     */
    public Map<String, String> creationMapping() {
        Map<String, String> sanitizationMethods = new HashMap<>();
        List<String> inputSources = ConfigLoader.getInstance().getUntrustedSources();

        if (CollectionUtils.isNotEmpty(inputSources)) {
            for (String source : inputSources) {
                String methodName = "sanitize" + capitalizeFirstLetter(source);
                sanitizationMethods.put(source, "InputSanitizer." + methodName);
            }
        }
        return sanitizationMethods;
    }

    /**
     * If the input string is not null, set its first letter to uppercase.
     *
     * @param input the input
     * @return string
     */
    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    /**
     * Applies sanitization of user-supplied input.
     *
     * @param input the input
     * @return string
     */
    public static String sanitizeUserInput(String input) {
        if (input == null) {
            return "";
        }

        // Step 1: Remove HTML tags to prevent XSS.
        input = input.replaceAll("<[^>]*>", "");

        // Step 2: Remove potentially hidden scripts and JS codes.
        input = input.replaceAll("(?i)(<script.*?>.*?</script>|javascript:|on\\w+=)", "");

        // Step 3: Remove SQL keywords to prevent SQL Injection.
        input = input.replaceAll("(?i)(DROP|SELECT|INSERT|DELETE|UPDATE|TABLE|FROM|WHERE|--|;|\\*|=)", "");

        // Step 4: Block unsafe special characters.
        input = input.replaceAll("[^a-zA-Z0-9 .,!?@#%&()_-]", "");

        // Step 5: Normalizes suspicious Unicode sequences.
        input = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFKC);

        // Step 6: Limits the maximum length of the input.
        if (input.length() > 1024) {
            input = input.substring(0, 1024);
        }

        // Step 7: Trim and remove excess spaces.
        return input.strip();
    }

    /**
     * It applies sanitization to the input stream.
     *
     * @param input the input
     * @return string
     */
    public static String sanitizeStreamInput(String input) {
        if (input == null) {
            return "";
        }

        // Step 1: Remove null characters or unprintable bytes.
        input = input.replaceAll("\\p{Cntrl}", "");

        // Step 2: Remove HTML or XML tags.
        input = input.replaceAll("<[^>]*>", "");

        // Step 3: Normalize Unicode sequences.
        input = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFKC);

        // Step 4: Remove scripts, dangerous URIs, and other suspicious patterns.
        input = input.replaceAll("(?i)(<script.*?>.*?</script>|javascript:|on\\w+=)", "");

        // Step 5: Blocks SQL keywords and malicious symbols.
        input = input.replaceAll("(?i)(DROP|SELECT|INSERT|DELETE|UPDATE|TABLE|FROM|WHERE|--|;|\\*|=)", "");

        // Step 6: Keep only alphanumeric characters and basic symbols.
        input = input.replaceAll("[^a-zA-Z0-9 .,!?@#%&()\\[\\]{}:;\"'-_+|/\\\\]", "");

        // Step 7: Remove traversal directory sequences.
        input = input.replaceAll("(\\.\\./|\\.\\.\\\\)", "");

        // Step 8: Limits the maximum length of the input.
        if (input.length() > 4096) {
            input = input.substring(0, 4096);
        }

        // Step 9: Trim and remove multiple spaces.
        return input.replaceAll("\\s+", " ").strip();
    }

    /**
     * Applies sanitization to command-line supplied arguments.
     *
     * @param input the input
     * @return string
     */
    public static String sanitizeCommandLineArgs(String input) {
        if (input == null) {
            return "";
        }

        // Step 1: Remove null characters or unprintable bytes.
        input = input.replaceAll("\\p{Cntrl}", "");

        // Step 2: Normalizes Unicode sequences to avoid spoofing.
        input = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFKC);

        // Step 3: Block traversal directories and absolute paths.
        input = input.replaceAll("(\\.{2,}/|\\.{2,}\\\\|~|/|\\\\)", "");

        // Step 4: Remove suspicious shell-specific keywords.
        input = input.replaceAll("(?i)(rm -rf|sudo|chmod|chown|kill|shutdown|reboot|mkfs|dd|ps|grep)", "");

        // Step 5: Lock shell-specific sequences and symbols.
        input = input.replaceAll("([$`|;&<>*?!\"'])", "");

        // Step 6: Keep only alphanumeric characters and basic symbols.
        input = input.replaceAll("[^a-zA-Z0-9 .,!?@#%&()\\[\\]{}:;\"'-_+|/\\\\]", "");

        // Step 7: Limits the maximum length of the input.
        if (input.length() > 1024) {
            input = input.substring(0, 1024);
        }

        // Step 8: Remove multiple spaces and trim.
        return input.replaceAll("\\s+", " ").strip();
    }

    /**
     * It applies sanitization to the responses provided by the API.
     *
     * @param input the input
     * @return string
     */
    public static String sanitizeApiResponse(String input) {
        if (input == null) {
            return "";
        }

        // Limits the length of the input.
        if (input.length() > 4096) {
            throw new IllegalArgumentException("API response too large.");
        }

        // If the format is JSON, validate and clean up.
        if (input.trim().startsWith("{") && input.trim().endsWith("}")) {
            return sanitizeJson(input);
        }

        // If the format is XML or HTML, use a library to parse and clean.
        if (input.trim().startsWith("<") && input.trim().endsWith(">")) {
            return sanitizeHtmlOrXml(input);
        }

        // Default: Applies generic sanitization for unstructured strings.
        return sanitizeString(input);
    }

    /**
     * Applies sanitization to input with Json syntax.
     *
     * @param json the json
     * @return string
     */
    private static String sanitizeJson(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(json);

            sanitizeJsonNode(rootNode);

            return objectMapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON input.");
        }
    }

    /**
     * Applies sanitization to Json format nodes.
     *
     * @param node the json node
     * @return string
     */
    private static void sanitizeJsonNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.fields().forEachRemaining(entry -> {
                JsonNode childNode = entry.getValue();
                if (childNode.isTextual()) {
                    objectNode.put(entry.getKey(), sanitizeString(childNode.asText()));
                } else if (childNode.isContainerNode()) {
                    sanitizeJsonNode(childNode);
                }
            });
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode childNode = arrayNode.get(i);
                if (childNode.isTextual()) {
                    arrayNode.set(i, new TextNode(sanitizeString(childNode.asText())));
                } else if (childNode.isContainerNode()) {
                    sanitizeJsonNode(childNode);
                }
            }
        }
    }

    /**
     * Applies sanitization to input with HTML or XML format.
     *
     * @param input the input
     * @return string
     */
    private static String sanitizeHtmlOrXml(String input) {
        try {

            Document document = Jsoup.parse(input);
            document.select("script, iframe, object, embed, link, meta").remove();

            document.select("*").forEach(element -> {
                element.removeAttr("onmouseover")
                        .removeAttr("onclick")
                        .removeAttr("onerror")
                        .removeAttr("style")
                        .removeAttr("src")
                        .removeAttr("href");
            });

            return Jsoup.clean(document.html(), "", Safelist.basic());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid HTML or XML input.");
        }
    }

    /**
     * Applies sanitization to responses obtained from a call to a remote service.
     *
     * @param input the input
     * @return string
     */
    public static String sanitizeRemoteServiceCall(String input) {
        if (input == null) {
            return "";
        }

        // Step 1: Remove control characters (non-printable bytes).
        input = input.replaceAll("\\p{Cntrl}", "");

        // Step 2: Normalizes input to prevent Unicode spoofing.
        input = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFKC);

        // Step 3: Remove harmful or unnecessary escape sequences.
        input = input.replaceAll("([\\\\%$<>`|;{}\\[\\]])", "");

        // Step 4: Decodes encoded input (e.g., URL or Base64) securely.
        try {
            input = java.net.URLDecoder.decode(input, "UTF-8");
        } catch (Exception e) {
            // It ignores decoding errors, which means that the input was not encoded.
        }

        // Step 5: Validates and sanitizes specific formats such as JSON or XML.
        if (isJson(input)) {
            input = sanitizeJson(input);
        } else if (isXml(input)) {
            input = sanitizeHtmlOrXml(input);
        }

        // Step 6: Keep only alphanumeric characters and basic symbols.
        input = input.replaceAll("[^a-zA-Z0-9 .,!?@#%&()\\[\\]{}:;\"'-_+|/\\\\]", "");

        // Step 7: Limit the length of the input to prevent DoS.
        if (input.length() > 2048) {
            input = input.substring(0, 2048);
        }

        // Step 8: Remove multiple spaces and trim.
        return input.replaceAll("\\s+", " ").strip();
    }

    /**
     * Verifies that the input provided has the syntax of the Json format.
     *
     * @param input the input
     * @return string
     */
    private static boolean isJson(String input) {
        try {
            new ObjectMapper().readTree(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifies that the input provided has the syntax of the XML format.
     *
     * @param input the input
     * @return string
     */
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

    /**
     * Applies sanitization to input provided by an e-mail.
     *
     * @param input the input
     * @return string
     */
    public static String sanitizeEmailInput(String input) {
        if (input == null) {
            return "";
        }

        // Step 1: Remove control characters (non-printable bytes).
        input = input.replaceAll("\\p{Cntrl}", "");

            // Step 2: Normalizes input to prevent Unicode spoofing.
        input = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFKC);

        // Step 3: Remove HTML scripts, tags, and comments.
        input = input.replaceAll("<!--.*?-->", "");
        input = input.replaceAll("<[^>]*>", "");

        // Step 4: Decoding encoded input (e.g., URLs).
        try {
            input = java.net.URLDecoder.decode(input, "UTF-8");
        } catch (Exception e) {
            // Ignore decoding errors.
        }

        // Step 5: Remove malicious email addresses or links.
        input = input.replaceAll("(?i)mailto:|javascript:|data:|file:", "");
        input = input.replaceAll("(https?|ftp)://[^\\s]+", "");

        // Step 6: Keep only acceptable alphanumeric characters and symbols.
        input = input.replaceAll("[^a-zA-Z0-9@._%+\\-\\s]", "");

        // Step 7: Validates the length of the input.
        if (input.length() > 512) {
            input = input.substring(0, 512);
        }

        // Step 8: Remove multiple spaces and trim.
        return input.replaceAll("\\s+", " ").strip();
    }

    /**
     * Applies generic sanitization to strings.
     *
     * @param input the input
     * @return string
     */
    private static String sanitizeString(String input) {
        if (input == null) {
            return "";
        }
        // Removes potentially dangerous characters.
        return input.replaceAll("[<>\\\"\\']", "").strip();
    }

    /**
     * Applies sanitization to data contained in files uploaded to the system.
     *
     * @param input the input
     * @return string
     */
    public static String sanitizeUploadFile(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Invalid file name.");
        }

        // Step 1: Remove control characters (non-printable bytes).
        input = input.replaceAll("\\p{Cntrl}", "");

        // Step 2: Normalizes input to prevent Unicode spoofing.
        input = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFKC);

        // Step 3: Remove relative paths and unsafe characters.
        input = input.replaceAll("\\.\\./", "")
                .replaceAll("\\\\", "/");

        // Step 4: Keep only valid characters for file names.
        input = input.replaceAll("[^a-zA-Z0-9._-]", "");

        // Step 5: Limits the length of the file name.
        if (input.length() > 255) {
            input = input.substring(0, 255);
        }

        // Step 6: Remove reserved file names (example: Windows).
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

        // Step 7: Remove unnecessary spaces and return the result.
        return input.strip();
    }

}

