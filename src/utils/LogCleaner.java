import java.io.*;
import java.util.regex.*;

public class LogCleaner {
    public static void main(String[] args) {
        // Check if the correct number of arguments are provided
        if (args.length != 2) {
            System.out.println("Usage: java LogCleaner <inputFilePath> <outputFilePath>");
            return;
        }

        // Assign file paths from command-line arguments
        String inputFilePath = args[0] + "logs/debug.log";  // Concatenate debug.log to the directory path
        String outputFilePath = args[1] + "logs";  // Second argument for output file path

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {

            String line;
            // Regex pattern to match date, time, class information, and "INFO:"
            Pattern pattern = Pattern.compile("^.*?INFO:\\s*");

            while ((line = reader.readLine()) != null) {
                // Remove the matched pattern from each line
                String cleanedLine = pattern.matcher(line).replaceFirst("");
                writer.write(cleanedLine);
                writer.newLine(); // Write a newline character to the output file
            }

            System.out.println("Log cleaning completed successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
