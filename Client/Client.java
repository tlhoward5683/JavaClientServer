package Java.ClientServer.Client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
Java (client) program that monitors a directory. When a new Java properties file
appears in the monitored directory, it should process it as follows:
1) Read the file into a Map
2) Apply a regular expression pattern filter for the keys (i.e., remove key/value mappings
where keys do not match a configurable regular expression pattern).
3) Relay the filtered mappings to a server program
4) Delete the file

The client program’s main method should accept an argument specifying a config file path.
*/

public class Client {

    public static void main(String[] args) {

        if (args.length == 0 || args[0].equals("/help") || args[0].equals("-help") || args[0].equals("/?")) {
            // Improvent: Add input validation.
            displayUsage();

        } else {
            Properties props = getConfig(args[0]);

            if (props != null) {
                try {
                    String monitoredPath = props.getProperty("monitored.path");
                    String filteringPattern = props.getProperty("filtering.pattern");
                    String host = props.getProperty("server.address");
                    int port = Integer.parseInt(props.getProperty("port.num"));
                    int monitorFrequency = Integer.parseInt(props.getProperty("monitor.frequency"));

                    while (true) {
                        // Check the monitoredPath for .properties file(s).
                        List<String> propertiesFiles = findPropertiesFiles(Paths.get(monitoredPath), "properties");

                        // For each .properties file found...
                        for (String propertiesFile : propertiesFiles) {
                            File file = new File(propertiesFile);

                            if (file.exists()) {
                                // 1) Read the file into a Map
                                Map<String, String> mapFromFile = createMapFromPropFile(file);

                                // 2) Apply a regular expression pattern filter for the keys (i.e., remove
                                // key/value mappings where keys do not match a configurable regular expression
                                // pattern).
                                mapFromFile = filterProperties(mapFromFile, filteringPattern, file.getName());

                                // 3) Relay the filtered mappings to a server program
                                sendMapMessage(host, port, mapFromFile);

                                // 4) Delete the file
                                file.delete();
                            }
                        }
                        TimeUnit.MINUTES.sleep(monitorFrequency);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // Improvement: Add additional data validation, error handling, debug logging,
                    // code cleanup.
                }
            }
        }
    }

    // Display Client program usage information.
    private static void displayUsage() {

        System.out.println("Usage:  java Client.java <relative path to Client confile file>");
        // Improvement: Expand usage info.
        // Improvement: Move all strings to a separate strings file.
    }

    // Get config file values.
    private static Properties getConfig(String configLocation) {

        Properties props = new Properties();
        String fileName = configLocation;

        try (FileInputStream fis = new FileInputStream(fileName)) {
            props.load(fis);
        } catch (FileNotFoundException e) {
            System.out.println("Client config file not found at " + configLocation + ".\n");
        } catch (IOException e) {
            System.out.println("An error occurred reading the Client config file.\n");
            e.printStackTrace();
        }
        // Improvement: Check for missing config entries. Validate existing config
        // entries.

        return props;
    }

    // Find properties files in monitored path.
    private static List<String> findPropertiesFiles(Path monitoredPath, String fileExtension)
            throws IOException {

        if (!Files.isDirectory(monitoredPath)) {
            throw new IllegalArgumentException("Monitored path must be a directory!");
        }

        List<String> propertiesFiles;

        try (Stream<Path> walk = Files.walk(monitoredPath)) {
            propertiesFiles = walk
                    .filter(p -> !Files.isDirectory(p))
                    .map(p -> p.toString().toLowerCase())
                    .filter(f -> f.endsWith(fileExtension))
                    .collect(Collectors.toList());
        }

        return propertiesFiles;
    }

    // Create a map from the properties file.
    public static Map<String, String> createMapFromPropFile(File file) {

        Map<String, String> map = new HashMap<String, String>();
        // Potential Improvement: If it's preferable for the filtered properties file to
        // retain the original order of the properties, use a LinkedHashMap instead of
        // HashMap.
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = br.readLine()) != null) {

                String[] parts = line.split("=");
                String name = parts[0].trim();
                String number = parts[1].trim();

                if (!name.equals("") && !number.equals(""))
                    map.put(name, number);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return map;
    }

    // Filter the properties according to the filtering pattern.
    public static Map<String, String> filterProperties(Map<String, String> mapFromFile, String filteringPattern,
            String fileName) {

        Map<String, String> filteredMap = new HashMap<String, String>();
        Iterator hmIterator = mapFromFile.entrySet().iterator();
        Pattern pattern = Pattern.compile(filteringPattern);

        // Add filename to map
        filteredMap.put("propFilesFilteredFileName", fileName);

        while (hmIterator.hasNext()) {

            Map.Entry mapElement = (Map.Entry) hmIterator.next();
            Matcher matcher = pattern.matcher(mapElement.getKey().toString());

            if (!matcher.matches()) {
                filteredMap.put(mapElement.getKey().toString(), mapElement.getValue().toString());
            }
        }

        return filteredMap;
    }

    // Send the map of filtered properties to the server
    public static void sendMapMessage(String host, int port, Map<String, String> mapFromFile) {

        try (Socket socket = new Socket(host, port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println(mapFromFile);
            out.flush();
            in.readLine();
            out.close();
            in.close();
        } catch (ConnectException e) {
            System.out.println("Client could not connect to Server.  Make sure Server is running.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
