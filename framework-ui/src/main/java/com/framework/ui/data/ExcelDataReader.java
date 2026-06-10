package com.framework.ui.data;

import com.framework.common.utils.LogUtils;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads test data from an {@code .xlsx} file on the classpath using Apache POI.
 *
 * <p>The sheet's first row is the header (column names); each subsequent row is a
 * test case keyed by a globally-unique id in a designated column. {@link
 * #getRow} returns the matching row as a column-name → value map.
 *
 * <p>All cells are read via {@link DataFormatter}, which renders every cell —
 * numeric, string, etc. — as the string you'd see in Excel. So a postal code
 * typed as a number comes back as {@code "12345"}, not {@code "12345.0"}.
 *
 * <p>Fails loud: a missing file, missing id column, or missing test case throws
 * with a message naming exactly what went wrong.
 */
public final class ExcelDataReader {

    private static final Logger log = LogUtils.getLogger(ExcelDataReader.class);

    private ExcelDataReader() {
        // utility — no instances
    }

    /**
     * Returns the data row whose {@code idColumn} cell equals {@code idValue}.
     *
     * @param classpathResource e.g. {@code testdata/ui-testdata.xlsx}
     * @param idColumn          the header name of the id column, e.g. {@code testcase_id}
     * @param idValue           the id to look up, e.g. {@code TC001}
     * @return column-name → value for the matching row (insertion-ordered)
     */
    public static Map<String, String> getRow(String classpathResource, String idColumn, String idValue) {
        log.info("Reading test data: file={}, {}={}", classpathResource, idColumn, idValue);

        try (InputStream is = ExcelDataReader.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (is == null) {
                throw new IllegalArgumentException(
                        "Test data file not found on classpath: " + classpathResource);
            }
            try (Workbook workbook = new XSSFWorkbook(is)) {
                Sheet sheet = workbook.getSheetAt(0);
                DataFormatter formatter = new DataFormatter();

                List<String> columns = readHeader(sheet, formatter);
                int idIndex = columns.indexOf(idColumn);
                if (idIndex < 0) {
                    throw new IllegalArgumentException(
                            "Id column '" + idColumn + "' not found. Header is: " + columns);
                }

                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) {
                        continue;
                    }
                    String id = formatter.formatCellValue(row.getCell(idIndex)).trim();
                    if (id.equals(idValue)) {
                        return toMap(columns, row, formatter);
                    }
                }
                throw new IllegalArgumentException(
                        "No row with " + idColumn + "='" + idValue + "' in " + classpathResource);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read test data file: " + classpathResource, e);
        }
    }

    private static List<String> readHeader(Sheet sheet, DataFormatter formatter) {
        Row header = sheet.getRow(0);
        if (header == null) {
            throw new IllegalArgumentException("Test data sheet has no header row");
        }
        List<String> columns = new ArrayList<>();
        for (int c = 0; c < header.getLastCellNum(); c++) {
            columns.add(formatter.formatCellValue(header.getCell(c)).trim());
        }
        return columns;
    }

    private static Map<String, String> toMap(List<String> columns, Row row, DataFormatter formatter) {
        Map<String, String> data = new LinkedHashMap<>();
        for (int c = 0; c < columns.size(); c++) {
            Cell cell = row.getCell(c);
            data.put(columns.get(c), formatter.formatCellValue(cell).trim());
        }
        return data;
    }
}
