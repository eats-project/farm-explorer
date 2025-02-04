package uk.org.eats.templates;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Utils {
	
	
	    public static String convertTimestamp (String dateStr) {
	    	
	    	if (dateStr.endsWith(" BST")) {
                dateStr = dateStr.replace(" BST", " Europe/London");
            }
	        
	        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm:ss a z", Locale.ENGLISH);
	        
	        ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateStr, formatter);
	       // String rdfTimestampUTC = zonedDateTime.withZoneSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
	       // String rdfTimestampUTC = zonedDateTime.parse(dateStr, formatter);
	        String rdfTimestampBST = zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	        
	       // System.out.println("UTC Timestamp for RDF: " + rdfTimestampUTC);
	       // System.out.println("BST Timestamp for RDF: " + rdfTimestampBST);
	        
	        return (rdfTimestampBST);
	    }
	

}
