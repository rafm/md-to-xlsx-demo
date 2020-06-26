package dev.rafm.mdtoxlsxdemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MdToXlsxDemoApplication {

    public static class QuickInfo {

        public QuickInfoKey key;

        public String value;

        public QuickInfo(QuickInfoKey key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    public static enum QuickInfoKey {

        CATEGORY("Category"),
        DESCRIPTION("Description"),
        MORE_INFO("More Info"),
        RECOMMENDED_ACTION("Recommended Action"),
        PLUGIN_TITLE("Plugin Title"),
        CLOUD("Cloud"),
        AWS_LINK("AWS Link");

        private String keyName;

        private QuickInfoKey(String keyName) {
            this.keyName = keyName;
        }

        public String getColumnName() {
            return keyName;
        }

        public static QuickInfoKey valueOfKey(String key) {
            return Arrays.stream(values())
                .filter(value -> value.getColumnName().equalsIgnoreCase(key))
                .findFirst()
                .orElse(null);
        }
    }

    public static void main(String[] args) throws IOException {
        Set<String> columns = new HashSet<>();
        Path mardownResourcesPath = Paths.get("aws");
        createInitialExcel();
        Files.walk(mardownResourcesPath).forEach(path -> {
            if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(".md")) {
                String name = null;
                String category = null;
                StringBuilder detail = new StringBuilder();
                boolean readCategoryAndName = false;

                boolean readInfo = false;
                boolean startedReadingInfo = false;

                boolean readDetail = false;
                boolean startedReadingDetail = false;
                try (BufferedReader reader = Files.newBufferedReader(path)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!readCategoryAndName) {
                            if (line.startsWith("# AWS")) {
                                String[] categoryAndName = line.split("/");
                                category = categoryAndName[categoryAndName.length-2];
                                name = categoryAndName[categoryAndName.length-1];
                                // TODO put on the xlsx
                                readCategoryAndName = true;
                            }
                        } else if (!readInfo) {
                            if (startedReadingInfo) {
                                if (!line.startsWith("|")) {
                                    readInfo = true;
                                } else {
                                    readInfo(line);
                                }
                            } else if (line.startsWith("|")) {
                                startedReadingInfo = true;
                                readInfo(line);
                            }
                        } else if (!readDetail) {
                            if (startedReadingDetail) {
                                if (detail.length() != 0) {
                                    detail.append("\n");
                                }
                                detail.append(line);
                            } else {
                                if (line.startsWith("## Detail")) {
                                    startedReadingDetail = true;
                                }
                            }
                        }
                    }
                    // TODO put detail on excel
                    readDetail = true;
                } catch (IOException e) {
                    System.err.println("Failure to read file: " + path.getFileName());
                }
            }
        });
        System.out.println(columns);
    }

    // TODO
    private static void createInitialExcel() {

    }

    private static void readInfo(String line) { // TODO excel
        QuickInfo quickInfo = getQuickInfo(line);
        if (quickInfo != null) {
            // TODO switch and put to excel
        }
    }

    private static QuickInfo getQuickInfo(String line) {
        String[] parsedLine = line.split("\\|");
        String key = parsedLine[1].trim();
        if (key.isEmpty() || key.equals("-")) {
            return null;
        }
        if (key.startsWith("**")) {
            key = key.substring(2, key.length()-2);
        }
        return new QuickInfo(QuickInfoKey.valueOfKey(key), parsedLine[2]);
    }
}
