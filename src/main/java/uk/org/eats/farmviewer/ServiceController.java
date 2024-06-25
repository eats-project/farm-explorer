package uk.org.eats.farmviewer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
		
		System.out.println(payload);

		String result = GraphDBUtils.addJsonLD(payload, ConstantsDB.METHOD_PLANS);
		Gson gson = new Gson();
		return gson.toJson(result);

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
		String result = SPARQLQueries.linkMethodToAsset (assetIRI, methodIRI);
		
		return result;

	}
	
	@GetMapping("/getAssetsGraph")
	@ResponseBody
	public String getAssetsGraph() throws Exception {
		return GraphDBUtils.dumpGraphAsJsonLd (ConstantsDB.ASSETS_NAMED_GRAPH_IRI);

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
	
	
	@PostMapping("/evaluateTrace")
	@ResponseBody
	public String evaluateTrace(@RequestBody String payload, Model model) {

		SPARQLQueries.executeQueriesFromCSV(model, payload, semModel);
		Gson gson = new Gson();
		System.out.println("-----------------------------------------");
		System.out.println(gson.toJson(model));
		return gson.toJson(model);

	}

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

	@PostMapping("/cf_info_all")
	public String cf_info_all(@RequestBody String assetIRI) {

		Gson gson = new Gson();
		return gson.toJson(SPARQLQueries.getCFInfo_All(assetIRI));
	}
}
