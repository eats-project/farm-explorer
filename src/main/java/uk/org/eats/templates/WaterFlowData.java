package uk.org.eats.templates;

import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class WaterFlowData {
	
	
	public static String parseDataInJSONLD (MultipartFile file , String sensorIRI) {
		String json = "{\"@graph\":[";
		
		  try (InputStreamReader reader = new InputStreamReader(file.getInputStream());
				  	         CSVParser parser = CSVParser.parse(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
	            List<String> headers = parser.getHeaderNames();

	            System.out.println (headers);
	            String namespace = sensorIRI+":WaterFlow:Observation:";
	           
	            for (CSVRecord record : parser) {
	            
	             String reading = record.get("Water Flow");
	             String timestamp = DateToXmlTimestamp (record.get("Timestamp"));
	            	
	             json = json + "{\"@id\":\""+namespace+timestamp+"\","
	             		+ "    \"@type\": \"http://www.w3.org/ns/sosa/Observation\","
	             		+ "     \"http://www.w3.org/ns/sosa/madeBySensor\":{\"@id\":\""+sensorIRI+"\"},"
	             		+ "    \"http://www.w3.org/ns/sosa/hasResult\":\""+reading+"\","
	             		+ "     \"http://www.w3.org/ns/sosa/resultTime\":\""+timestamp+"\""
	             		+ "    },";
	             }
	            	  
	            json =  json.substring(0, json.length() - 1);
	            
	            json = json + "]}";    
	           
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
