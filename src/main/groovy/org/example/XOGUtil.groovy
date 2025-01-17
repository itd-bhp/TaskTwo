package org.example

import java.nio.file.Files
import java.nio.file.Paths

class XOGUtil {
    static void saveToFile(String fileName, String content) {
        println("Saving XOG XML to file: ${fileName}")
        try {
            Files.write(Paths.get(fileName), content.bytes)
            println("File saved successfully: ${fileName}")
        } catch (IOException e) {
            println("Failed to save file: ${fileName}")
            e.printStackTrace()
        }
    }
}
