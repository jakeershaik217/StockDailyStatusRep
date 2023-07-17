package practice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;


public class PDFReaderClass {

	public static void main(String[] args) throws IOException {

		File file=new File("C:\\Users\\Jakeer_Shaik\\Downloads\\aa8154a1-0774-4c3c-8f32-ec523be8f405.pdf");
		FileInputStream fis=new FileInputStream(file);
		/*PDDocument pdfbox=Loader.loadPDF(fis);
		PDFTextStripper pdfSt=new PDFTextStripper();
		String STr=pdfSt.getText(pdfbox);
		System.out.println(STr);*/
		
	}

}
