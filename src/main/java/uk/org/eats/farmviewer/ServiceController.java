package uk.org.eats.farmviewer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import  org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.Gson;

import uk.org.eats.graphdb.ConstantsDB;
import uk.org.eats.graphdb.ConstraintQueryUpdate;
import uk.org.eats.graphdb.GraphDBUtils;
import uk.org.eats.templates.ElectricityData;
import uk.org.eats.templates.WaterFlowData;


@org.springframework.stereotype.Controller

@RestController
public class ServiceController {

	
	public static Repository repository;
	OntModel semModel;

	@Autowired
	public void setUpKG() {
		
		System.out.println ("Setting Up Repository");
		semModel = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM_RDFS_INF);

		// read some ontologies so we have the labels for units, etc.
		//semModel.read(Constants.QUDT_UNITS);
		semModel.read(Constants.ECFO);
		semModel.read(Constants.PECO);	
		System.out.println ("Set Up Repositor Complete");
	}
	
	 public boolean isDemoMode() {
	        Properties properties = new Properties();
	        String propertyFileName = "application.properties";

	        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propertyFileName)) {
	            if (inputStream != null) {
	                properties.load(inputStream);
	                String demoModeValue = properties.getProperty("demo.mode");
	                // Return true if the value is "true" (case insensitive), otherwise false
	                
	                return Boolean.parseBoolean(demoModeValue);
	            } else {
	                System.err.println("Property file '" + propertyFileName + "' not found in the classpath.");
	            }
	        } catch (IOException e) {
	            System.err.println("Exception: " + e.getMessage());
	        }

	        // Return false by default if there was an issue reading the property
	        return false;
	    }

	
	@PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                 @RequestParam("dataType") String dataType) {
		
		System.out.println(dataType);
		ArrayList<HashMap <String,String>> list = SPARQLQueries.getSensorType(dataType);
		

       
		String payload = null;
		
		if (checkIfTypePresent(list,"https://www.robeau.tech/en/")) {
		
        payload = WaterFlowData.parseDataInJSONLD(file,dataType);
		
		}
		
		if (checkIfTypePresent(list,"https://enless-wireless.com/en/products/pulse-sensor-led-optical-reader-lora/")) {
			
	        payload = ElectricityData.parseDataInJSONLD(file,dataType);
			
			}
		String result = "{}";
		if (payload != null) {
		   result = GraphDBUtils.addJsonLD(payload, ConstantsDB.OBSERVATIONS_NAMED_GRAPH_IRI);
		}
		Gson gson = new Gson();
		return gson.toJson(result);
    }
	
	private boolean checkIfTypePresent (ArrayList<HashMap <String,String>> list, String type) {
		
		for (int i=0;i<list.size();i++) {
			if (list.get(i).containsValue(type)) {
				return true;
			}
		}
		
		return false;
	}
	
	@PostMapping("/saveCalculationMethod")
	@ResponseBody
	public String saveCalculationMethod(@RequestBody String payload) {
		
		if (isDemoMode()) {
			return "{\"warning\":\"operation not allowed in demo mode\"}";
		}
		
		System.out.println(payload);

		String result = GraphDBUtils.addJsonLD(payload, ConstantsDB.METHOD_PLANS);
		Gson gson = new Gson();
		return gson.toJson(result);

	}
	
	@PostMapping("/saveQueryUpdate")
	@ResponseBody
	public String saveQueryUpdate(@RequestBody String query) {
		
		if (isDemoMode()) {
			return "{\"warning\":\"operation not allowed in demo mode\"}";
		}
		
		System.out.println(query);
		Gson gson = new Gson();
		ConstraintQueryUpdate update = gson.fromJson(query,ConstraintQueryUpdate.class);
		GraphDBUtils.updateConstraint(update, ConstantsDB.METHOD_PLANS);
		//
		//returnConstraintQueryUpdate gson.toJson(result);
		return "{\"result\":\"update saved\"}";
		
	}
	
	@PostMapping("/addNewAsset")
	@ResponseBody
	public String addNewAsset(@RequestBody String payload) {

		String result = GraphDBUtils.addJsonLD(payload, ConstantsDB.ASSETS_NAMED_GRAPH_IRI);
		Gson gson = new Gson();
		return gson.toJson(result);

	}
	
	@GetMapping("/deleteAssets")
	@ResponseBody
	public String deleteAssets() {

		String repsonse = GraphDBUtils.clearNamedGraph(ConstantsDB.ASSETS_NAMED_GRAPH_IRI, "Assets");
		
		
		return repsonse;

	}
	
	@GetMapping("/deleteObservations")
	@ResponseBody
	public String deleteObservations() {

		String repsonse = GraphDBUtils.clearNamedGraph(ConstantsDB.OBSERVATIONS_NAMED_GRAPH_IRI, "Observations");
		
		
		return repsonse;

	}
	
	@PostMapping("/runSparqlQuery")
	@ResponseBody
	public String runSparqlQuery(@RequestBody String payload) {

		if (isDemoMode()) {
			return "{\"warning\":\"operation not allowed in demo mode\"}";
		}
		
		HashMap<String, String> result = SPARQLQueries.runSparqlQuery(payload);
		Gson gson = new Gson();
		return gson.toJson(result);

	}
	
	
	@GetMapping("/getFarms")
	@ResponseBody
	public String getFarms() {

		ArrayList<HashMap<String, String>> result = SPARQLQueries.getFarms();
		Gson gson = new Gson();
		return gson.toJson(result);

	}
	
	@GetMapping("/getFarmDetails")
	@ResponseBody
	public String getFarmDetails(@RequestParam String farmIri) {

		ArrayList<HashMap<String, String>> result = SPARQLQueries.getFarmDetails(farmIri);
		Gson gson = new Gson();
		return gson.toJson(result);

	}
	
	@GetMapping("/getSensors")
	@ResponseBody
	public String getSensors() {

		ArrayList<HashMap<String, String>> result = SPARQLQueries.getSensors();
		Gson gson = new Gson();
		return gson.toJson(result);

	}
	
	@GetMapping("/getSensorDetails")
	@ResponseBody
	public String getSensorDetails(@RequestParam String sensorIri) {

		ArrayList<HashMap<String, String>> result = SPARQLQueries.getSensorDetails(sensorIri);
		Gson gson = new Gson();
		return gson.toJson(result);

	}
	
	@GetMapping("/getActuatorDetails")
	@ResponseBody
	public String getActuatorDetails(@RequestParam String sensorIri) {

		ArrayList<HashMap<String, String>> result = SPARQLQueries.getActuatorDetails(sensorIri);
		Gson gson = new Gson();
		return gson.toJson(result);

	}
	
	@GetMapping("/evaluateTrace")
	@ResponseBody
	public String evaluateTrace(@RequestParam String assetIRI, Model model) {

		SPARQLQueries.executeQueriesFromCSV(model, assetIRI);
		Gson gson = new Gson();
		System.out.println("-----------------------------------------");
		System.out.println(gson.toJson(model));
		return gson.toJson(model);
	}
	
	@GetMapping("/getAgriParcelDetails")
	@ResponseBody
	public String getAgriParcelDetails(@RequestParam String parcelIri) {

		ArrayList<HashMap<String, String>> result = SPARQLQueries.getAgriParcelDetails(parcelIri);
		Gson gson = new Gson();
		return gson.toJson(result);

	}
	
	@GetMapping("/getEmissionResults")
	@ResponseBody
	public String getEmissionResults(@RequestParam String assetIri) {

		ArrayList<HashMap<String, String>> result = SPARQLQueries.getEmissionResults(assetIri);
		Gson gson = new Gson();
		return gson.toJson(result);

	}
	
	@GetMapping("/getCalculationFormulas")
	@ResponseBody
	public String getCalculationFromulas() {

		ArrayList<HashMap<String, String>> result = SPARQLQueries.getCalculationFromulas();
		Gson gson = new Gson();
	
		return gson.toJson(result);

	}
	
	@GetMapping("/getAssetsForEmissionCalculationMethods")
	@ResponseBody
	public String getAssetsForEmissionCalculationMethods() throws Exception {
		ArrayList<HashMap<String, String>> result = SPARQLQueries.getAssetsForEmissionCalculationMethods ();
		Gson gson = new Gson();	
		return gson.toJson(result);

	}
	
	@GetMapping("/getSensorCoordinates")
	@ResponseBody
	public String getSensorCoordinates(@RequestParam String sensorIri) throws Exception {
		ArrayList<HashMap<String, String>> result = SPARQLQueries.getSensorCoordinates (sensorIri);
		Gson gson = new Gson();	
		return gson.toJson(result);

	}
	
	@GetMapping("/getAvailableCalculationMethods")
	@ResponseBody
	public String getAvailableCalculationMethods() throws Exception {
		ArrayList<HashMap<String, String>> result = SPARQLQueries.getAvailableCalculationMethods ();
		Gson gson = new Gson();	
		return gson.toJson(result);

	}
	
	@GetMapping("/getAssignedCalculationMethods")
	@ResponseBody
	public String getAssignedCalculationMethods(@RequestParam String assetIRI) throws Exception {
		ArrayList<HashMap<String, String>> result = SPARQLQueries.getAssignedCalculationMethods (assetIRI);
		Gson gson = new Gson();	
		return gson.toJson(result);

	}
	
	@GetMapping("/linkMethodToAsset")
	@ResponseBody
	public String linkMethodToAsset(@RequestParam String assetIRI, String methodIRI) throws Exception {
		
		if (isDemoMode()) {
			return "{\"warning\":\"operation not allowed in demo mode\"}";
		}
		
		String result = SPARQLQueries.linkMethodToAsset (assetIRI, methodIRI);
		
		return result;

	}
	
	@GetMapping("/getAssetsGraph")
	@ResponseBody
	public String getAssetsGraph() throws Exception {
		return GraphDBUtils.dumpGraphAsJsonLd (ConstantsDB.ASSETS_NAMED_GRAPH_IRI);

	}
	
	@GetMapping("/getManualObservationsGroupings")
	@ResponseBody
	public String getManualObservationsGroupings() throws Exception {
		
		ArrayList<HashMap<String, String>> result = SPARQLQueries.getManualObservationsGroupings ();
		
		Gson gson = new Gson();	
		return gson.toJson(result);

	}
	
	
	@PostMapping("/getLinksToEmissionSources")
	@ResponseBody
	public String getLinksToEmissionSources(@RequestBody String payload) {

		semModel.read(Constants.WIKIDATA_LABELS,"Turtle");

		ArrayList<Double[][]> result = SPARQLQueries.getLinksToEmissionSources(payload, semModel);
		Gson gson = new Gson();
		return gson.toJson(result);

	}
	
	@GetMapping("/getSensorData")
	@ResponseBody
	public String getSensorData(@RequestParam String sensorIRI) {

		System.out.println("getSensorData Called " + sensorIRI);

		ArrayList<HashMap<String, String>> result = SPARQLQueries.getSensorData(sensorIRI);
		Gson gson = new Gson();
		return gson.toJson(result);

	}
	
	
	
	@GetMapping("/getManualObservationsData")
	@ResponseBody
	public String getManualObservationsData(@RequestParam String label) {

		System.out.println("getManualObservationsData Called " + label);

		ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>> ();
		try {
			result = SPARQLQueries.getManualObservationsData(label);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Gson gson = new Gson();
		return gson.toJson(result);

	}
	
	
	@GetMapping("/getManualSensorDetails")
	@ResponseBody
	public String getManualSensorDetails(@RequestParam String label) {

		System.out.println("getManualSensorDetails Called " + label);

		ArrayList<HashMap<String, String>> result = new ArrayList<HashMap<String, String>> ();
		try {
			result = SPARQLQueries.getManualSensorDetails(label);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Gson gson = new Gson();
		return gson.toJson(result);

	}
	
	
	
	/*
	@PostMapping("/evaluateTrace")
	@ResponseBody
	public String evaluateTrace(@RequestBody String payload, Model model) {

		SPARQLQueries.executeQueriesFromCSV(model, payload, semModel);
		Gson gson = new Gson();
		System.out.println("-----------------------------------------");
		System.out.println(gson.toJson(model));
		return gson.toJson(model);

	}*/

	@PostMapping("/getDataTransformations")
	@ResponseBody
	public String getDatatTransformations(@RequestBody String assetIRI) {
		
	//	semModel.read(Constants.WIKIDATA_LABELS,"Turtle");

		ArrayList<HashMap<String, String>> result = SPARQLQueries.getDatatTransformations(assetIRI);
		Gson gson = new Gson();
		return gson.toJson(result);

	}

	@GetMapping("/cf_info")
	public String cf_info(@RequestParam String cf_iri) {

		
		return SPARQLQueries.getCFInfo(cf_iri);

	}
	
	@GetMapping("/calculateFootprint")
	public String calculateFootprint(@RequestParam String assetIri) {
		
		GraphDBUtils.dropGraph (assetIri+":CarbonExecutionTrace");
		return "{\"resul\":\""+new PlanExecutor().executeWorkflow(assetIri)+"\"}";

	}
	
	@GetMapping("/getProvenanceTrace")
	public String getProvenanceTrace(@RequestParam String assetIri) {
		
		return GraphDBUtils.getGraphAsJsonLD (assetIri+":CarbonExecutionTrace");

	}

	@PostMapping("/cf_info_all")
	public String cf_info_all(@RequestBody String assetIRI) {

		Gson gson = new Gson();
		return gson.toJson(SPARQLQueries.getCFInfo_All(assetIRI));
	}
}
