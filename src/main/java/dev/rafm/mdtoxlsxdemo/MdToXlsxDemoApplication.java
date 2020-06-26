package dev.rafm.mdtoxlsxdemo;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
        Path mardownResourcesPath = Paths.get("aws");

        FileOutputStream out = new FileOutputStream(String.format("CloudSpoit-%s.xls", new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_SSSS").format(new Date())));
        Workbook wb = new XSSFWorkbook();
        Sheet s = wb.createSheet();
        wb.setSheetName(0, "CloudSpoit cloud vulnerabilities");
        AtomicInteger rownum = new AtomicInteger();

        createHeader(s.createRow(rownum.getAndIncrement()));
        Files.walk(mardownResourcesPath).forEach(path -> {
            if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(".md")) {
                Row row = s.createRow(rownum.getAndIncrement());
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

                                String category = categoryAndName[categoryAndName.length-2];
                                row.createCell(0).setCellValue(category);

                                String name = categoryAndName[categoryAndName.length-1];
                                row.createCell(1).setCellValue(name);

                                readCategoryAndName = true;
                            }
                        } else if (!readInfo) {
                            if (startedReadingInfo) {
                                if (!line.startsWith("|")) {
                                    readInfo = true;
                                } else {
                                    readInfo(line, row);
                                }
                            } else if (line.startsWith("|")) {
                                startedReadingInfo = true;
                                readInfo(line, row);
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
                    row.createCell(6).setCellValue(detail.toString());
                    readDetail = true;
                } catch (IOException e) {
                    System.err.println("Failure to read file: " + path.getFileName());
                }
            }
        });
        wb.write(out);
        out.close();
        wb.close();
    }

    private static void createHeader(Row headerRow) {
        int columnIndex = 0;
        headerRow.createCell(columnIndex++).setCellValue("Category");
        headerRow.createCell(columnIndex++).setCellValue("Name");
        headerRow.createCell(columnIndex++).setCellValue("Description");
        headerRow.createCell(columnIndex++).setCellValue("More Info");
        headerRow.createCell(columnIndex++).setCellValue("AWS Link");
        headerRow.createCell(columnIndex++).setCellValue("Recommended Action");
        headerRow.createCell(columnIndex++).setCellValue("Detailed Remediation Steps");
    }

    private static void readInfo(String line, Row row) {
        QuickInfo quickInfo = getQuickInfo(line);
        if (quickInfo != null) {
            switch (quickInfo.key) {
                case DESCRIPTION: {
                    row.createCell(2).setCellValue(quickInfo.value);
                    break;
                }
                case MORE_INFO: {
                    row.createCell(3).setCellValue(quickInfo.value);
                    break;
                }
                case AWS_LINK: {
                    row.createCell(4).setCellValue(quickInfo.value);
                    break;
                }
                case RECOMMENDED_ACTION: {
                    row.createCell(5).setCellValue(quickInfo.value);
                    break;
                }
                default: {
                    break;
                }
            }
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
