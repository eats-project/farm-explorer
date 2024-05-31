package uk.org.eats.farmviewer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
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

@org.springframework.stereotype.Controller

@RestController
public class ServiceController {

	public static Repository repository;
	OntModel semModel;

	@Autowired
	public void setUpKG() {
		

		semModel = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM_RDFS_INF);

		// read some ontologies so we have the labels for units, etc.
		semModel.read(Constants.QUDT_UNITS);
		semModel.read(Constants.ECFO);
		semModel.read(Constants.PECO);		
	}

	
	@PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                 @RequestParam("dataType") String dataType) {
        try {
            BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
            CSVParser parser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
            List<String> headers = parser.getHeaderNames();

            System.out.println("Data Type: " + dataType);
            System.out.println("CSV Headers: " + headers);
            parser.close();
            fileReader.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"result\":\"error uploading data\"}";
        }
        
        return "{\"result\":\"data recieved\"}";
    }
	
	
	@PostMapping("/getLinksToEmissionSources")
	@ResponseBody
	public String getLinksToEmissionSources(@RequestBody String payload) {

		semModel.read(Constants.WIKIDATA_LABELS,"Turtle");

		ArrayList<Double[][]> result = SPARQLQueries.getLinksToEmissionSources(payload, semModel);
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
	public String getDatatTransformations(@RequestBody String payload) {
		
		semModel.read(Constants.WIKIDATA_LABELS,"Turtle");

		ArrayList<HashMap<String, String>> result = SPARQLQueries.getDatatTransformations(payload, semModel);
		Gson gson = new Gson();
		return gson.toJson(result);

	}

	@GetMapping("/cf_info")
	public String cf_info(@RequestParam String cf_iri) {

		
		return SPARQLQueries.getCFInfo(cf_iri);

	}

	@GetMapping("/cf_info_all")
	public String cf_info_alternative(@RequestParam String region) {

		Gson gson = new Gson();
		return gson.toJson(SPARQLQueries.getCFInfo_All(region, semModel));
	}
}
