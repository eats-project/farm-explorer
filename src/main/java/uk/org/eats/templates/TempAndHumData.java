package uk.org.eats.templates;

import java.io.BufferedReader;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class TempAndHumData {
	
	public static String parseDataInJSONLD (MultipartFile file ) {
		String json = "";
		 try {
	            BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
	            CSVParser parser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
	            List<String> headers = parser.getHeaderNames();

	            // RDF setup
	            ValueFactory vf = SimpleValueFactory.getInstance();
	            LinkedHashModel model = new LinkedHashModel();
	            String namespace = "http://example.org/sensor/";
	            // Regex to split the temperature string into value and unit
	            Pattern pattern = Pattern.compile("^(\\d+(?:\\.\\d+)?)(.*)$");
	            // Parse each CSV row and create RDF triples
	            for (CSVRecord record : parser) {
	                String sensorId = "sensor123";  // Example sensor ID
	               // String timestampStr = record.get("Timestamp(Europe/London)");
	                String temperatureStr = record.get("Temperature");
	   //             String humidity = record.get("Humidity");
	             //   ZonedDateTime timestamp = ZonedDateTime.parse(timestampStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(java.time.ZoneId.of("Europe/London")));

	                // Use regex to find matches and split
	                Matcher matcher = pattern.matcher(temperatureStr);
	                double temperature = 0;
	                String unit = "°C";  // Default unit

	                if (matcher.matches()) {
	                    temperature = Double.parseDouble(matcher.group(1));
	                    unit = matcher.group(2).trim().isEmpty() ? "°C" : matcher.group(2).trim();
	                }
	                
	                // Create RDF triples
	         //       model.add(vf.createIRI(namespace, sensorId), vf.createIRI(namespace, "hasTimestamp"), vf.createLiteral(timestamp.toString()));
	                model.add(vf.createIRI(namespace, sensorId), vf.createIRI(namespace, "hasTemperature"), vf.createLiteral(temperature));
	        //        model.add(vf.createIRI(namespace, sensorId), vf.createIRI(namespace, "hasHumidity"), vf.createLiteral(Double.parseDouble(humidity)));
	            }

	            // Print the model (or store it in a repository)
	            model.forEach(statement -> System.out.println(statement));
	         
	            System.out.println("CSV Headers: " + headers);
	            parser.close();
	            fileReader.close();
	        } catch (Exception e) {
	            e.printStackTrace();
	            return "{\"result\":\"error uploading data\"}";
	        }
		
		return json;
	}

}
