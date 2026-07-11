package com.Stock.Utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ExcelUtilityTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void readExcelReturnsRowsMappedByHeader() throws Exception {
        File file = createWorkbook(
                new String[] {"CompanyName", "Date"},
                new String[][] {{"Acme", "2023-07-17"}, {"Globex", "2023-07-18"}});
        ExcelUtility utility = new ExcelUtility(file.getAbsolutePath(), "Stocks");

        List<HashMap<String, String>> rows = utility.readExcel();

        assertEquals(2, rows.size());
        assertEquals("Acme", rows.get(0).get("CompanyName"));
        assertEquals("2023-07-18", rows.get(1).get("Date"));
    }

    @Test
    public void readExcelAsListFlattensDataRows() throws Exception {
        File file = createWorkbook(
                new String[] {"CompanyName", "Date"},
                new String[][] {{"Acme", "2023-07-17"}, {"Globex", "2023-07-18"}});
        ExcelUtility utility = new ExcelUtility(file.getAbsolutePath(), "Stocks");

        assertEquals(
                Arrays.asList("Acme", "2023-07-17", "Globex", "2023-07-18"),
                utility.readExcelasList());
    }

    @Test
    public void setValuesToExcelWritesRowsUsingHeaderNames() throws Exception {
        File file = createWorkbook(new String[] {"CompanyName", "Date"}, new String[0][]);
        ExcelUtility utility = new ExcelUtility(file.getAbsolutePath(), "Stocks");
        HashMap<String, String> row = new HashMap<>();
        row.put("CompanyName", "Acme");
        row.put("Date", "2023-07-17");
        List<HashMap<String, String>> rows = new ArrayList<>();
        rows.add(row);

        utility.setValuestoExcel(2, rows);

        try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
            assertEquals("Acme", workbook.getSheet("Stocks").getRow(1).getCell(0).getStringCellValue());
            assertEquals("2023-07-17", workbook.getSheet("Stocks").getRow(1).getCell(1).getStringCellValue());
        }
    }

    @Test
    public void setValuesToExcelAsListAppendsRows() throws Exception {
        File file = createWorkbook(new String[] {"CompanyName"}, new String[][] {{"Acme"}});
        ExcelUtility utility = new ExcelUtility(file.getAbsolutePath(), "Stocks");

        utility.setValuestoExcelasList(Arrays.asList("Globex", "Initech"));

        try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
            assertEquals("Acme", workbook.getSheet("Stocks").getRow(1).getCell(0).getStringCellValue());
            assertEquals("Globex", workbook.getSheet("Stocks").getRow(2).getCell(0).getStringCellValue());
            assertEquals("Initech", workbook.getSheet("Stocks").getRow(3).getCell(0).getStringCellValue());
        }
    }

    @Test
    public void cleanExcelDatabaseRemovesDataRows() throws Exception {
        File file = createWorkbook(
                new String[] {"CompanyName"},
                new String[][] {{"Acme"}, {"Globex"}});
        ExcelUtility utility = new ExcelUtility(file.getAbsolutePath(), "Stocks");

        utility.cleanExcelDatabase();

        try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
            assertEquals("CompanyName", workbook.getSheet("Stocks").getRow(0).getCell(0).getStringCellValue());
            assertNull(workbook.getSheet("Stocks").getRow(1));
            assertNull(workbook.getSheet("Stocks").getRow(2));
        }
    }

    @Test
    public void getDateReturnsEmptyStringWhenNoDataRowExists() throws Exception {
        File file = createWorkbook(
                new String[] {"CompanyName", "Board Meeting Outcome", "Dividend", "Buyback", "Bounus", "Date"},
                new String[0][]);
        ExcelUtility utility = new ExcelUtility(file.getAbsolutePath(), "Stocks");

        assertEquals("", utility.getDateFromExcelDatabase());
    }

    private File createWorkbook(String[] headers, String[][] dataRows) throws Exception {
        File file = temporaryFolder.newFile("stocks-" + System.nanoTime() + ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                FileOutputStream output = new FileOutputStream(file)) {
            workbook.createSheet("Stocks");
            for (int column = 0; column < headers.length; column++) {
                if (workbook.getSheet("Stocks").getRow(0) == null) {
                    workbook.getSheet("Stocks").createRow(0);
                }
                workbook.getSheet("Stocks").getRow(0).createCell(column).setCellValue(headers[column]);
            }
            for (int row = 0; row < dataRows.length; row++) {
                workbook.getSheet("Stocks").createRow(row + 1);
                for (int column = 0; column < dataRows[row].length; column++) {
                    workbook.getSheet("Stocks").getRow(row + 1).createCell(column).setCellValue(dataRows[row][column]);
                }
            }
            workbook.write(output);
        }
        return file;
    }
}
