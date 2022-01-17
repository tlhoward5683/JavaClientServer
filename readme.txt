Java Client/Server Take-Home Assignment

Write a Java (client) program that monitors a directory. When a new Java properties file
appears in the monitored directory, it should process it as follows:
1) Read the file into a Map
2) Apply a regular expression pattern filter for the keys (i.e., remove key/value mappings
where keys do not match a configurable regular expression pattern).
3) Relay the filtered mappings to a server program
4) Delete the file

The client program’s main method should accept an argument specifying a config file path. The
client config file should contain values defining:
• the directory path that will be monitored
• the key filtering pattern that will be applied
• the address of the corresponding server program
• any other value(s) you think should be configurable

Also, write a corresponding server program that accepts messages from clients. It should be
capable of handling messages sent by multiple clients simultaneously.

Upon receipt of a message from a client, the server should use the message to reconstruct a
filtered properties file and write it to disk, using the original filename.

The server program’s main method should accept an argument specifying a config file path.
The server config file should contain values defining:
• the location of the directory to which to write the files
• what port to listen on
• any other value(s) you think should be configurable