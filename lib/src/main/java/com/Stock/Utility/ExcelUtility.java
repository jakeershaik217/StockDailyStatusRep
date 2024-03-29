package com.Stock.Utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import org.apache.poi.EmptyFileException;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelUtility {
	
	private FileOutputStream Fout;
	private FileInputStream Fin;
	private XSSFWorkbook workbook;
	private XSSFSheet    sheet;
	private XSSFRow      row;
	private File         file;
	private String ExcelDataBasePath;
	
	public ExcelUtility(String fileName,String SheetName) throws IOException{
        try {
		file=Paths.get(fileName).toFile();
		Fin=new FileInputStream(file);
		workbook=new XSSFWorkbook(Fin);	
		sheet=workbook.getSheet(SheetName);
		ExcelDataBasePath=fileName;
        }catch(EmptyFileException e) {
        	
        	Fin.close();
        	Paths.get(fileName).toFile().delete();
    		workbook=new XSSFWorkbook();	
    		sheet=workbook.createSheet(SheetName);
    		sheet.createRow(0).createCell(0).setCellValue("CompanyName");
    		sheet.getRow(0).createCell(1).setCellValue("Board Meeting Outcome");
    		sheet.getRow(0).createCell(2).setCellValue("Dividend");
    		sheet.getRow(0).createCell(3).setCellValue("Buyback");
    		sheet.getRow(0).createCell(4).setCellValue("Bounus");
    		sheet.getRow(0).createCell(5).setCellValue("Date");
    		try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
                workbook.write(outputStream);
                System.out.println("File created successfully at " + fileName);
                workbook.close();
                outputStream.close();
                
            } catch (IOException e1) {
                e.printStackTrace();
            }
    		
    		ExcelDataBasePath=fileName;
        	
        }catch(FileNotFoundException e) {
        	
        	workbook=new XSSFWorkbook();	
    		sheet=workbook.createSheet(SheetName);
    		sheet.createRow(0).createCell(0).setCellValue("CompanyName");
    		sheet.getRow(0).createCell(1).setCellValue("Board Meeting Outcome");
    		sheet.getRow(0).createCell(2).setCellValue("Dividend");
    		sheet.getRow(0).createCell(3).setCellValue("Buyback");
    		sheet.getRow(0).createCell(4).setCellValue("Bounus");
    		sheet.getRow(0).createCell(5).setCellValue("Date");
    		try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
                workbook.write(outputStream);
                System.out.println("File created successfully at " + fileName);
                workbook.close();
                outputStream.close();
            } catch (IOException e1) {
                e.printStackTrace();
            }
    		
    		ExcelDataBasePath=fileName;
        }
	}
	
	
	
	public List<HashMap<String,String>> readExcel() throws FileNotFoundException{
		
		HashMap<String, String> DataMap;
		List<HashMap<String,String>> DataMapList=new ArrayList<>();
		if(sheet==null)
			throw new FileNotFoundException("Sheet :"+sheet.getSheetName()+" is not visible/Accessible");
		
		int totalRowCount=sheet.getLastRowNum()+1;
		int totalColumnCount=sheet.getRow(0).getLastCellNum();
	try {
		for(int i=1;i<totalRowCount;i++) {
			DataMap=new HashMap<>();
			for(int j=0;j<totalColumnCount;j++) {
				DataMap.put(sheet.getRow(0).getCell(j).getStringCellValue(), sheet.getRow(i).getCell(j).getStringCellValue());
			
			}DataMapList.add(DataMap);
		}
	}catch(NullPointerException n) {
		
	}
		
		return DataMapList;
	}
	
	
     public List<String> readExcelasList() throws FileNotFoundException{
		
		List<String> DataMap=new ArrayList<>();
		if(sheet==null)
			throw new FileNotFoundException("Sheet :"+sheet.getSheetName()+" is not visible/Accessible");
		
		int totalRowCount=sheet.getLastRowNum()+1;
		int totalColumnCount=sheet.getRow(0).getLastCellNum();
	try {
		for(int i=1;i<totalRowCount;i++) 
			for(int j=0;j<totalColumnCount;j++) 
				DataMap.add(sheet.getRow(i).getCell(j).getStringCellValue());
			
			
		
	}catch(NullPointerException n) {
		
	}
		
		return DataMap;
	}
	
	public void cleanExcelDatabase() throws IOException {
		int totalRowCount=sheet.getLastRowNum()+1;
 		for(int i=1;i<totalRowCount;i++) 
				sheet.removeRow(sheet.getRow(i));
		Fout = new FileOutputStream(new File(ExcelDataBasePath));
		workbook.write(Fout);
		workbook.close();
		Fout.close();
		Fin.close();
	}
	
	public void setValuestoExcel(int NumberOfColumns ,List<HashMap<String, String>> CompnayDataCustm) throws IOException {
		int totalRows=sheet.getLastRowNum()+1;
		int totalColumns=sheet.getRow(0).getLastCellNum();
		int ColumnCount=NumberOfColumns;
		int rowCount=CompnayDataCustm.size();
		int counter=0;
		if(totalRows==1 && totalColumns==NumberOfColumns) {
			
			for(int i=0;i<rowCount;i++) {
				XSSFRow row=sheet.createRow(i+1);
				for(int j=0;j<ColumnCount;j++) {
					String Headings=sheet.getRow(0).getCell(j).getStringCellValue();
					row.createCell(j, CellType.STRING).setCellValue(CompnayDataCustm.get(i).get(Headings));
						
					}
				
				}
				
	     }else {
			 for(int i=totalRows;i<rowCount+totalRows;i++) {
				 XSSFRow row=sheet.createRow(i);
					for(int j=0;j<ColumnCount;j++) {
						String Headings=sheet.getRow(0).getCell(j).getStringCellValue();
						System.out.println(Headings);
						row.createCell(j, CellType.STRING).setCellValue(CompnayDataCustm.get(counter).get(Headings));
							
						}
					counter++;
					}
				 
			 }
	    	 
	    	 
		Fout = new FileOutputStream(new File(ExcelDataBasePath));
		workbook.write(Fout);
		workbook.close();
		Fout.close();
		Fin.close();
		
	}
	public void setValuestoExcelasList(List<String> CompnayDataCustm) throws IOException {
		int totalRows=sheet.getLastRowNum()+1;
		int totalColumns=sheet.getRow(0).getLastCellNum();
		int rowCount=CompnayDataCustm.size();
		int counter=0;
		if(totalRows==1) {
			
			for(int i=0;i<rowCount;i++) {
				XSSFRow row=sheet.createRow(i+1);
					row.createCell(0, CellType.STRING).setCellValue(CompnayDataCustm.get(i));
						
					}
				
				
	     }else {
			 for(int i=totalRows;i<rowCount+totalRows;i++) {
				 XSSFRow row=sheet.createRow(i);
						row.createCell(0, CellType.STRING).setCellValue(CompnayDataCustm.get(counter));
							
					counter++;
					}
				 
			 }
	    	 
	    	 
		Fout = new FileOutputStream(new File(ExcelDataBasePath));
		workbook.write(Fout);
		workbook.close();
		Fout.close();
		Fin.close();
		
	}	
	
	public String getDateFromExcelDatabase() {
		try {
		return sheet.getRow(1).getCell(5).getStringCellValue();
		}catch(Exception e) {
			return "";
		}
	}

}
