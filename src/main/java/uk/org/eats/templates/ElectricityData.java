package uk.org.eats.templates;

import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.web.multipart.MultipartFile;

public class ElectricityData {

	public static String parseDataInJSONLD (MultipartFile file , String sensorIRI) {
		String json = "{\"@graph\":[";
		
		  try (InputStreamReader reader = new InputStreamReader(file.getInputStream());
				  	         CSVParser parser = CSVParser.parse(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
	            List<String> headers = parser.getHeaderNames();

	            System.out.println (headers);
	            String namespace = sensorIRI+":Electricity:Observation:";
	           
	            for (CSVRecord record : parser) {
	            
	             String reading = record.get("Reading");
	             String timestamp = record.get("Timestamp");
	            	
	             json = json + "{\"@id\":\""+namespace+timestamp+"\","
		             		+ "    \"@type\": \"http://www.w3.org/ns/sosa/Observation\","
		             		+ "     \"http://www.w3.org/ns/sosa/madeBySensor\":{\"@id\":\""+sensorIRI+"\"},"
		             		+ "     \"http://www.w3.org/ns/sosa/resultTime\":\""+timestamp+"\","
		             				+ "\"http://www.w3.org/ns/sosa/hasResult\": {"
		             				+ "	\"@id\":\""+namespace+timestamp+":Result\","
		             				+ "	\"@type\":[\"http://www.w3.org/ns/sosa/Result\",\"http://qudt.org/schema/qudt/\"],"
		             				+ "	\"http://qudt.org/schema/qudt/unit\":{\"@id\":\"http://www.wikidata.org/entity/Q182098\"},"
		             				+ "	\"http://qudt.org/schema/qudt/hasQuantityKind\":{\"@id\":\"http://www.wikidata.org/entity/Q12725\"},"
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
	

	
	
}
