package uk.org.eats.templates;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import javax.swing.JFileChooser;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;

import uk.org.eats.graphdb.ConstantsDB;
import uk.org.eats.graphdb.GraphDBUtils;

public class FertiliserData {

	public static String parseDataInJSONLD (MultipartFile file , String sensorIRI) {
		String json = "{\"@graph\":[";
		
		  try (InputStreamReader reader = new InputStreamReader(file.getInputStream());
				  	         CSVParser parser = CSVParser.parse(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
	            List<String> headers = parser.getHeaderNames();

	            System.out.println (headers);
	            String namespace = sensorIRI+":Fertiliser:Observation:";
	           
	            for (CSVRecord record : parser) {
	            
	             String reading = record.get("Reading");
	             String timestamp = record.get("Timestamp");
	             String start = record.get("Start");
	             String end = record.get("End");
	             String label = record.get("Product Name");
	             UUID uuid = UUID.randomUUID();
	             
	             //assumption the N concentration in the fertiliser is only captured in teh label for this demo but for future use a separate property of specific IRI (e.g., to identify fertiliser with 15% N concentration) should be defined 
	            	
	             json = json + "{\"@id\":\""+namespace+timestamp+"\","
		             		+ "    \"@type\": \"http://www.w3.org/ns/sosa/Observation\","
	            		    + "\"http://www.w3.org/ns/sosa/hasFeatureOfInterest\":{\"@id\":\"urn:ngsi-ld:AgriFarm:Example%20Farm:Actuator:Irrigation%20Rig\"},"
	            		    + "\"http://www.w3.org/ns/sosa/observedProperty\":{\"@id\":\"urn:ngsi-ld:AgriFarm:Example%20Farm:Actuator:Irrigation%20Rig:Property:Fertiliser\"},"
			             	
	            		    + "     \"http://www.w3.org/ns/sosa/madeBySensor\":{\"@id\":\""+sensorIRI+"\","
		             		+ "	\"@type\":[\"http://xmlns.com/foaf/0.1/Person\"]"
		             				+ "},"
		             		+ "     \"http://www.w3.org/2000/01/rdf-schema#label\":\""+label+"\","
		             		+ "     \"http://www.w3.org/ns/sosa/resultTime\":\""+timestamp+"\","
		             		+ "     \"http://www.w3.org/ns/sosa/phenomenonTime\":{"
		             		+ "	\"@id\":\""+namespace+"PhenomenonTimeInstance:"+uuid+"\","
		             		+ "     \"http://www.w3.org/2006/time#hasBeginning\":{"
		             		+ "	\"@id\":\""+namespace+"PhenomenonTimeInstance:"+uuid+":Start\","
		             			+ "     \"http://www.w3.org/2006/time#inXSDDate\":\""+start+"\""
		             		+ "	},"
		             		+ "     \"http://www.w3.org/2006/time#hasEnd\":{"
		             		+ "	\"@id\":\""+namespace+"PhenomenonTimeInstance:"+uuid+":End\","
		             			+ "     \"http://www.w3.org/2006/time#inXSDDate\":\""+end+"\""
		             		+ "	}"
		             		+ "	},"
		             				+ "\"http://www.w3.org/ns/sosa/hasResult\": {"
		             				+ "	\"@id\":\""+namespace+timestamp+":Result\","
		             				+ "	\"@type\":[\"http://www.w3.org/ns/sosa/Result\",\"http://qudt.org/schema/qudt/\"],"
		             				+ "	\"http://qudt.org/schema/qudt/unit\":{\"@id\":\"http://www.wikidata.org/entity/Q844211\"},"
		             				+ "	\"http://qudt.org/schema/qudt/hasQuantityKind\":{\"@id\":\"http://www.wikidata.org/entity/Q906629\"},"
		             				+ "	\"http://qudt.org/schema/qudt/value\":{\"@value\":\""+reading+"\",\"@type\":\"http://www.w3.org/2001/XMLSchema#float\"}"
		             				+ "	}"
		             		+ "    },";
		             }
		            	  
	            	  
	            json =  json.substring(0, json.length() - 1);
	            
	            json = json + "]}";    
	            System.out.println (json);
	        } catch (Exception e) {
	            e.printStackTrace();
	           
	            return "{\"result\":\"error uploading data\"}";
	        }
		
		return json;
	}
	
	public static String DateToXmlTimestamp (String dateString ) throws Exception {
	    

	    // Parse the date string with specific format and timezone
	    SimpleDateFormat formatter = new SimpleDateFormat("MMM d, yyyy, hh:mm:ss a z");
	    formatter.setTimeZone(TimeZone.getTimeZone("BST")); // Set BST timezone
	    Date date = formatter.parse(dateString);

	    // Format the date to XML timestamp format (yyyy-MM-dd'T'HH:mm:ss'Z')
	    SimpleDateFormat xmlFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	    String xmlTimestamp = xmlFormatter.format(date);

	    
	    return xmlTimestamp;
	  }
	
	// Main method to open file selector and process CSV
    public static void main(String[] args) {
        // Open file chooser to select a CSV file
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
        	 File selectedFile = fileChooser.getSelectedFile();
             System.out.println("Selected file: " + selectedFile.getAbsolutePath());

             try {
                 // Convert the selected file into a MockMultipartFile
                 FileInputStream input = new FileInputStream(selectedFile);
                 byte[] fileContent = input.readAllBytes();

                 MultipartFile multipartFile = new MockMultipartFile(selectedFile.getName(), selectedFile.getName(), "text/csv", fileContent);

                 // Sensor IRI example
                 String sensorIRI = "http://example.org/Bob";

                 // Parse the selected CSV file and print the RDF model
                 String rdfModel = parseDataInJSONLD(multipartFile, sensorIRI);
                 System.out.println("Parsed RDF Model:");
                 System.out.println(rdfModel);

                 //add to triple store 
                 GraphDBUtils.addJsonLD(rdfModel, ConstantsDB.OBSERVATIONS_NAMED_GRAPH_IRI);
                 
                 System.out.println("data added to "  + ConstantsDB.OBSERVATIONS_NAMED_GRAPH_IRI);
                 
             } catch (Exception e) {
                 e.printStackTrace();
             }
         } else {
             System.out.println("No file selected.");
         }
    }
	
    
    	
  

    	
    
    
	
}
