package Java.ClientServer.Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/*
Server program that accepts messages from clients. It should be
capable of handling messages sent by multiple clients simultaneously.

Upon receipt of a message from a client, the server should use the message to reconstruct a
filtered properties file and write it to disk, using the original filename.

The server program’s main method should accept an argument specifying a config file path.
*/

public class Server {
    public static void main(String[] args) {

        if (args.length == 0 || args[0].equals("/help") || args[0].equals("-help") || args[0].equals("/?")) {
            // Improvent: Add input validation
            displayUsage();

        } else {
            Properties props = getConfig(args[0]);

            if (props != null) {
                String filteredPath = props.getProperty("filtered.path");
                int port = Integer.parseInt(props.getProperty("port.num"));

                ServerSocket server = null;
                try {
                    server = new ServerSocket(port);
                    server.setReuseAddress(true);
                    while (true) {
                        Socket client = server.accept();
                        // System.out.println("New client connected " +
                        // client.getInetAddress().getHostAddress());
                        ClientHandler clientSock = new ClientHandler(client, filteredPath);
                        new Thread(clientSock).start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (server != null) {
                        try {
                            server.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            // Improvement: Add additional data validation, error handling, debug logging,
                            // code cleanup.
                        }
                    }
                }
            }
        }

    }

    // Display Client program usage information.
    private static void displayUsage() {

        System.out.println("Usage:  java Server.java <relative path to Server confile file>");
        // Improvement: Expand usage info.
        // Improvement: Move all strings to a separate strings file.
    }

    // Get config file values.
    private static Properties getConfig(String configLocation) {

        Properties props = new Properties();
        String fileName = configLocation;

        try (FileInputStream fis = new FileInputStream(fileName)) {
            props.load(fis);
        } catch (FileNotFoundException ex) {
            System.out.println("Server config file not found at " + configLocation + ".\n");
        } catch (IOException ex) {
            System.out.println("An error occurred reading the Server config file.\n");
            ex.printStackTrace();
        }
        // Improvement: Check for missing config entries. Validate existing config
        // entries.

        return props;
    }

    private static class ClientHandler implements Runnable {

        private final Socket clientSocket;
        private final String filteredPath;

        public ClientHandler(Socket socket, String filteredPath) {
            this.clientSocket = socket;
            this.filteredPath = filteredPath;
        }

        @Override
        public void run() {
            PrintWriter out = null;
            BufferedReader in = null;
            String fileName = filteredPath + "\\default.properties";

            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String line;
                Map<String, String> mapFromFile = new HashMap<String, String>();

                while ((line = in.readLine()) != null) {
                    out.println(line);

                    // In a real world scenario, I would have asked for direction regarding a better
                    // approach for getting the message into a map, or I would have taken a bit more
                    // time to research it further. I know there's a better way than this.
                    line = line.replace("{", "");
                    line = line.replace("}", "");
                    String[] myelements = line.split(", ");

                    for (int i = 0; i < myelements.length; i++) {
                        String key = myelements[i].split("=")[0];
                        String value = myelements[i].split("=")[1];
                        if (key.equals("propFilesFilteredFileName")) {
                            // Given more time, I would have come up with a better approach for
                            // passing the filename from the client to the server.
                            fileName = filteredPath + "\\" + value;
                        } else {
                            mapFromFile.put(key, value);
                        }
                    }
                }

                try {
                    // Make sure filtered path exists.
                    File filteredPathDir = new File(filteredPath);
                    if (!filteredPathDir.exists()) {
                        filteredPathDir.mkdirs();
                    }

                    // Create the filtered properties file.
                    File myObj = new File(fileName);
                    myObj.createNewFile();
                } catch (IOException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }

                try {
                    FileWriter myWriter = new FileWriter(fileName);

                    for (Map.Entry<String, String> entry : mapFromFile.entrySet()) {
                        myWriter.write(entry.getKey() + "=" + entry.getValue() + "\n");
                    }

                    myWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (IOException e) {
                e.printStackTrace();

            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                    if (in != null) {
                        in.close();
                    }
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
