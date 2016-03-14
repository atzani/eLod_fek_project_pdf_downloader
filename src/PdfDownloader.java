import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import au.com.bytecode.opencsv.CSVReader;
import java.util.Arrays;
import java.util.Scanner;

/**
 * @author A. Tzanis
 */


/** code to connect to all pdf links in a csv file,
 *  download the pdf file and store them to a specific
 *  directory with specific file name **/

public class PdfDownloader {

	public static void main(String[] args) throws IOException, InterruptedException {
		
		//String filePath = "C:/Users/Aggelos/workspace/EtPdfDownloader/";
		String filePath = "/home/tzanis/theFekProject/fekPdfDownload/";
		String csvFilePath = null;
		String pdfLink = null;
		String[] fields;
		String AePath = null;
		String EpePath = null;
		String downloadPath = null;
		String ffPath = "/usr/bin/firefox";								// firefox path in the server
		String displayNum = "99";										// the number of virtual display
		
		//System.out.println("Enter csv file name: ");					// Input method, call the user to give the input csv fileName
		@SuppressWarnings({ "resource", "unused" })
		Scanner scanner = new Scanner(System.in);
		//csvFilePath = filePath + scanner.nextLine() + ".csv";			// Get the user input, to find the csv we want to open
		//csvFilePath = filePath + "Τροποποίηση Καταστατικού" + ".csv";	// Get the user input, to find the csv we want to open
		csvFilePath = filePath + "Announcement Subject" + ".csv";	// Get the user input, to find the csv we want to open
		
		File AeDirectory = directoryCreator (filePath, "AE");			// Create AE_pdf directory
		File EpeDirectory = directoryCreator (filePath, "EPE");			// Create EPE_pdf directory
		
		AePath = AeDirectory.getPath().toString();						// Get AE_pdf directory path
		EpePath = EpeDirectory.getPath().toString();					// Get EPE_pdf directory path
		
		
		try {
			
			@SuppressWarnings("resource")			
			CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(csvFilePath), "UTF-8"), 
					 ',', '"', '\0');
			
			String[] nextLine;

			
			while ((nextLine = reader.readNext()) != null) {					// Reads the csv file by row 
				
				fields = new String[nextLine.length];																			
				
				for (int i = 0; i < nextLine.length; i++) {						// fields[0]=afm, fields[1]=orgCategory, fields[2]=fekType,
					fields[i] = nextLine[i];									// fields[3]=fekThema, fields[4]=originalDate, fields[5]=fekNumber,
					System.out.print(fields[i]+"\t");							// fields[6]=pdfLink
				}
				
				System.out.print("\n");
				
					// Check if company is AE
					if (fields[1].equalsIgnoreCase("http://linkedeconomy.org/resource/OrganizationCategory/AE")){						
						String thisPath = "/" + fields[0].replaceAll("[\"]","") + "/" 
										+ fields[2].replaceAll("[\"]","") + "/" 				// AE/AFM/fekType/fekThema/pdf
										+ fields[3].replaceAll("[\"]","");
						File thisDirectory = directoryCreator (AePath, thisPath);				// Create the exact directory of each fekPdf
						System.out.println("thisDirectory is: "+thisDirectory);
						downloadPath = thisDirectory.getPath().toString();
					}
					
					// Check if company is EPE
					else if (fields[1].equalsIgnoreCase("http://linkedeconomy.org/resource/OrganizationCategory/EPE")){		
						
						String thisPath = "/" + fields[0].replaceAll("[\"]","") + "/" 
										+ fields[2].replaceAll("[\"]","") + "/" 				// EPE/AFM/fekType/fekThema/pdf
										+ fields[3].replaceAll("[\"]","");
						File thisDirectory = directoryCreator (EpePath, thisPath);				// Create the exact directory of each fekPdf
						System.out.println("thisDirectory is: "+thisDirectory);
						downloadPath = thisDirectory.getPath().toString();
					}
				
					String pdfName = fields[4].substring(0,fields[4].length()-6) + "_" 		// originalDate_
							+ fields[5].replaceAll("[\"]","") + "_"							// fekNumber_
							+ fields[0].replaceAll("[\"]","") + ".pdf";						// AFM.pdf
			
					pdfLink = fields[6].replaceAll("[\"]","");								// PDF file download link
					
					File pdf = new File(downloadPath + "/" + pdfName);
					
					// Check if file already exists 
					if (pdf.exists()){
						System.out.println("\nFile " + pdfName + " already exist!\n");
						continue;
					}
					else if (!pdf.exists())	{					
						
						System.out.println("downloadPath: " + downloadPath);
				
						pdfDownload(pdfLink, downloadPath, ffPath, displayNum);					// Downloads the pdf File
				
						File pdfFile = getTheNewestFile(downloadPath, "pdf");					// Get the last pdf File in directory
						
						fileRenamer(pdfFile, pdfName, downloadPath);							// Rename the last pdf file
					}
			}
	
		} catch (FileNotFoundException e) {
		e.printStackTrace();
		}
	}	

 
	/** Firefox profile builder **/
    public static FirefoxProfile firefoxProfile(String downloadPath){
    	
    	FirefoxProfile profile = new FirefoxProfile();

    	 profile.setPreference("browser.download.folderList", 2);
         profile.setPreference("browser.download.dir", downloadPath); 							//this will download pdf inside downloadPath.
         profile.setPreference("plugin.disable_full_page_plugin_for_types", "application/pdf");
         profile.setPreference("browser.helperApps.neverAsk.saveToDisk","application/csv,text/csv,application/pdfss, application/excel" );
         profile.setPreference("browser.download.manager.showWhenStarting", false);
         profile.setPreference("pdfjs.disabled", true);
         
		return profile;
    	
    	}
     
    /** Start firefox in headless gui **/
    public static FirefoxBinary firefoxBinary(String ffPath, String displayNum){
    	
    	// Setup firefox binary to start in Xvfb        
        displayNum = ":" + displayNum;											// Stores the display number created in xcfb
    	String Xport = System.getProperty(						
                "lmportal.xvfb.id", displayNum);								// Get the xvfb id
        final File firefoxPath = new File(System.getProperty(
                "lmportal.deploy.firefox.path", ffPath));						// Get the firefox path in the system
        FirefoxBinary firefoxBinary = new FirefoxBinary(firefoxPath);			// Create a new instance of the firefox binary 
        firefoxBinary.setEnvironmentProperty("DISPLAY", Xport);					// Set the display number to the firefoxBinary
    	
        return firefoxBinary;
    }
    
    
    /** Download a pdf file of the pdfLink and save it to the saveDir 
     * @throws InterruptedException **/
    public static void  pdfDownload (String pdfLink , String saveDir , String ffPath , String displayNum) throws InterruptedException{
    	
    	String fileName = "document.pdf";
    	boolean downloaded = false;
    	FirefoxProfile profile = firefoxProfile(saveDir);					// Creates a new profile
    	FirefoxBinary binary = firefoxBinary(ffPath, displayNum);			// Creates a new binary
    	WebDriver driver = new FirefoxDriver(binary, profile);				// Create a new instance of the html unit driver
    	//WebDriver driver = new FirefoxDriver(profile);				// Create a new instance of the html unit driver
		driver.get(pdfLink);												// Conects to the WebSite
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		
		while (downloaded != true) {
			downloaded = isFileDownloaded(saveDir, fileName);				// wait until file is downloaded
			TimeUnit.SECONDS.sleep(2);
		}
		
		driver.quit();
	}
  	
    
    /** Get the newest file for a specific extension **/
    public static File getTheNewestFile(String filePath, String ext) {
       
    	File theNewestFile = null;
    	
        File dir = new File(filePath);									// Get the directory we want to scan
        FileFilter fileFilter = new WildcardFileFilter("*." + ext);		// Get the fileName we want to search
        File[] files = dir.listFiles(fileFilter);						// Get the list of files in the directory

        if (files.length > 0) {											// Check if there are files in the directory
            /** The newest file comes first after the sorting **/
            Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
            theNewestFile = files[0];									// Get the newest file
        }

        return theNewestFile;
    }
    
    
    /** Rename the last file in a directory **/
    public static void  fileRenamer (File oldFile , String newFileName, String directory) throws IOException{
    	
    	File newFile = new File(directory, newFileName);		// Create file with the new name

    	if (newFile.exists())
    		throw new java.io.IOException("file exists");		// Check if file exists

    	boolean success = oldFile.renameTo(newFile);			// Rename the oldFile

    	if (!success) {
    		System.out.println("File rename was not successfull!");
    	}
    
    }
    
    /** Create a new directory at directoryPath with the directoryName we want **/
    public static File directoryCreator (String directoryPath, String directoryName) {
       
    	File newDirectory = new File(directoryPath, directoryName);		// Create a new directory
		
    	if(!newDirectory.exists()){										// Check if directory exists
			newDirectory.mkdirs();
		}
		
        return newDirectory;
    }
  	
    /** check if file has been downloaded **/
    public static boolean isFileDownloaded(String downloadPath, String fileName) {
    	
		boolean flag = false;
		
	    File dir = new File(downloadPath);							// Get the directory we want to scan
	    File[] dir_contents = dir.listFiles();						// Get all the files of directory
	  	    
	    for (int i = 0; i < dir_contents.length; i++) {
	        if (dir_contents[i].getName().equals(fileName))			// Check if the fie we want is in the directory
	            return flag=true;
	            }

	    return flag;											
	}
}