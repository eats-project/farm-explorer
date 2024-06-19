package uk.org.eats.farmviewer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.util.FileUtils;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.ui.Model;
import com.google.gson.JsonElement;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import uk.org.eats.graphdb.ConstantsDB;
import uk.org.eats.graphdb.GraphDBUtils;


public class SPARQLQueries {

	public static void executeQueriesFromCSV(Model modelResults, String payload, OntModel semModel) {

		ResourceLoader resourceLoader = new DefaultResourceLoader();
		Resource queriesFile = resourceLoader.getResource("/validation/queries.csv");

		Reader reader;

		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM_RDFS_INF);

		model.read(new ByteArrayInputStream(payload.getBytes()), null, "JSON-LD");
		System.out.println("Prov trace model size: " + model.size());
		System.out.println(payload);
		//model.add(semModel);

		try {
			reader = new InputStreamReader(queriesFile.getInputStream());
			try (CSVReader csvReader = new CSVReader(reader)) {
				List<String[]> r = csvReader.readAll();
				System.out.println("Running validation queries");
				for (int i = 0; i < r.size(); i++) {
					System.out.println("Evaluating: " + r.get(i)[1]);
					Query query = QueryFactory.create(Constants.PREFIXES + " " + r.get(i)[1].replaceAll("\uFEFF", ""));
					System.out.println(query);
					QueryExecution qExe = QueryExecutionFactory.create(query, model);
					ResultSet results = qExe.execSelect();

					ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();

					while (results.hasNext()) {
						QuerySolution sol = results.next();
						Iterator<String> it = sol.varNames();
						HashMap<String, String> map = new HashMap<String, String>();
						while (it.hasNext()) {
							String varName = it.next();
							map.put(varName, sol.get(varName).toString());
						}

						list.add(map);
					}

					System.out.println("Result");
					System.out.println(list);
					modelResults.addAttribute(r.get(i)[0], list);

				}

			} catch (IOException | CsvException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	public static String getCFInfo(String cf_iri) {

		ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
/*
		Query query = QueryFactory.create(Constants.PREFIXES
				+ " Select distinct ?id ?sourceUnit ?targetUnit ?source ?value ?emissionTarget ?emissionSource ?applicableLocation ?applicablePeriodStart ?applicablePeriodEnd "
				+ "WHERE { " + "SERVICE " + Constants.CF_ENDPOINT + " " + "{ ?id a ecfo:EmissionConversionFactor. "
				+ "OPTIONAL {?id ecfo:hasSourceUnit/rdfs:label ?sourceUnit.} "
				+ "OPTIONAL {?id ecfo:hasTargetUnit/rdfs:label ?targetUnit.} "
				+ "OPTIONAL {?id ecfo:hasEmissionTarget ?emissionTarget.} "
				+ "OPTIONAL {?id ecfo:hasEmissionSource ?emissionSource.} "
				+ "OPTIONAL {?id prov:wasDerivedFrom ?source.} " + "OPTIONAL {?id rdf:value ?value.} "
				+ "OPTIONAL {?id ecfo:hasApplicableLocation ?loc. ?loc rdfs:label ?applicableLocation. } "
				+ "OPTIONAL {?id ecfo:hasApplicablePeriod/time:hasEnd/time:inXSDDate ?applicablePeriodEnd.} "
				+ "OPTIONAL {?id ecfo:hasApplicablePeriod/time:hasBeginning/time:inXSDDate ?applicablePeriodStart.}} "
				+ "VALUES ?id { <" + cf_iri + ">}  }");
*/
		Query query = QueryFactory.create(Constants.PREFIXES
				+"Construct {\n" + 
				"    ?id ?p ?o.\n" + 
				
				"    ?o2 ?p3 ?o3.\n" + 
				"    ?o3 ?p4 ?o4.\n" + 
				"}\n" + 
				"\n" + 
				"Where {\n" + 
			
				"        ?id ?p ?o.\n" + 
				"        Optional {\n" + 
				"        ?id ecfo:hasApplicablePeriod ?o2.\n" + 
				"            ?o2 ?p3 ?o3.\n" + 
				"            ?o3 ?p4 ?o4.\n" + 
				"    }\n" + 
				"    Values ?id {<"+cf_iri+">}\n" + 
				"}");
		QueryExecution qExe = QueryExecution.service(Constants.CF_ENDPOINT_URL).query(query).build();
		org.apache.jena.rdf.model.Model results = qExe.execConstruct();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		results.write(ps, "JSON-LD");
		/*

		ResultSet results = qExe.execSelect();

		while (results.hasNext()) {
			QuerySolution sol = results.next();
			Iterator<String> it = sol.varNames();
			HashMap<String, String> map = new HashMap<String, String>();
			while (it.hasNext()) {
				String varName = it.next();
				map.put(varName, sol.get(varName).toString());
			}

			list.add(map);
		}
		
		return list;
		
		*/
		String result = baos.toString();
		ps.flush();
		return result;
		
	}

	public static ArrayList<HashMap<String, String>> getCFInfo_All(String region, OntModel semModel) {

		ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();

		Query query = QueryFactory.create(Constants.PREFIXES
				+ "  SELECT DISTINCT  ?id ?sourceUnit ?targetUnit ?source ?value ?applicableLocation ?applicablePeriodStart ?applicablePeriodEnd ?emissionTargetSymbol\n" + 
				"WHERE\n" + 
			
				"      { ?id  rdf:type  ecfo:EmissionConversionFactor .\n" + 
				"        ?id ecfo:hasTargetUnit ?targetUnitInst.\n" + 
				"            ?targetUnitInst    rdfs:label \"kilogram\"@en.\n" + 
				"            ?id    ecfo:hasEmissionTarget/rdfs:label ?emissionTargetSymbol. \n" + 
			
				"            ?id ecfo:hasApplicableLocation/rdfs:label \""+region+"\"@en .\n" + 
				"            ?id  ecfo:hasScope ecfo:Scope2.\n" + 
				"            ?id ecfo:hasTag <https://w3id.org/ecfkg/i/UK%20electricity>. \n" + 

				"            ?id ecfo:hasEmissionTarget <http://www.wikidata.org/entity/Q1933140>.         \n" + 
		
				"         OPTIONAL\n" + 
				"        {?id ecfo:hasSourceUnit ?sourceUnitInst.}\n" + 
				"        \n" + 
				"        OPTIONAL\n" + 
				"          { ?id  ecfo:hasApplicableLocation  ?location }\n" + 
				"        \n" + 
				"        OPTIONAL\n" + 
				"          { ?id (ecfo:hasApplicablePeriod/time:hasBeginning)/time:inXSDDate ?applicablePeriodStart }\n" + 
				"        OPTIONAL\n" + 
				"          { ?id (ecfo:hasApplicablePeriod/time:hasEnd)/time:inXSDDate ?applicablePeriodEnd }\n" + 
				"        \n" + 
				"        OPTIONAL\n" + 
				"          { ?id  prov:wasDerivedFrom  ?source }\n" + 
				"        OPTIONAL\n" + 
				"          { ?id  rdf:value  ?value }\n" + 
			
			
				
				"       OPTIONAL\n" + 
				"          {\n" + 
				"       ?location  rdfs:label  ?applicableLocation\n" + 
		
				"        }   \n" + 
				"        Optional {\n" + 
				"             ?targetUnitInst  qudt:abbreviation  ?targetUnit.\n" + 
				"        }\n" + 
				"        Optional {\n" + 
				"             ?sourceUnitInst  qudt:abbreviation  ?sourceUnit.\n" + 
				"      }\n" + 

			
				"  }\n" + 
				"ORDER BY DESC(?applicablePeriodEnd)");

		System.out.println("Emission Conversion Factor Query");
		System.out.println(query);

		// having only SPARQL query with two Service parts doesn't work if at least an
		// empy model isn't passed as well - weird...
		//QueryExecution qExe = QueryExecutionFactory.create(query, ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM_RDFS_INF));
		QueryExecution qExe = QueryExecution.service(Constants.CF_ENDPOINT_URL).query(query).build();
		
		ResultSet results = qExe.execSelect();

		while (results.hasNext()) {
			QuerySolution sol = results.next();
			Iterator<String> it = sol.varNames();
			HashMap<String, String> map = new HashMap<String, String>();
			while (it.hasNext()) {
				String varName = it.next();
				map.put(varName, sol.get(varName).toString());
			}
			list.add(map);
		}
		System.out.println("Result");
		System.out.println(list);

		return list;
	}

	public static  ArrayList<HashMap <String,String>> getDatatTransformations(String assetIRI) {


		String queryString = Constants.PREFIXES
				+ " SELECT DISTINCT  ?activityLabel ?inputLabel   ?inputValue ?inputUnitLabel ?inputQuantityKindL ?outputLabel ?outputValue  ?outputUnitLabel ?outputQuantityKindL\n" + 
				"FROM <"+assetIRI+":CarbonExecutionTrace> " +
				"FROM <"+assetIRI+":CarbonExecutionTrace> " +
				"FROM <"+ConstantsDB.CONVERSION_FACTORS+">"+
				"WHERE\n" + 
				"  { ?activity  rdf:type   peco:EmissionCalculationActivity ;\n" + 
				"              rdfs:label  ?activityLabel ;\n" + 
				"              prov:used   ?input\n" + 
				"      { ?input  rdf:type              peco:EmissionCalculationEntity ;\n" + 
				"                rdfs:label            ?inputLabel ;\n" + 
				"                qudt:value            ?inputValue ;\n" + 
				"                qudt:hasQuantityKind  ?inputQuantityKind ;\n" + 
				"                qudt:unit             ?inputUnit.\n" + 
				"            ?inputUnit  qudt:abbreviation  ?inputUnitLabel.  \n" + 
				"            ?inputQuantityKind rdfs:label  ?inputQuantityKindL.\n" + 
				"             FILTER ( lang(?inputUnitLabel) = \"en\" )\n" + 
				"          }\n" + 
				"     \n" + 
				"    UNION\n" + 
				"     \n" + 
				"          { ?input  rdf:value  ?inputValue .\n" + 
				"            ?input ecfo:hasTargetUnit/qudt:abbreviation ?inputUnitLabel .\n" + 
				"            ?input ecfo:hasEmissionTarget/rdfs:label ?inputQuantityKindL.\n" + 
				"            ?input rdfs:label ?inputLabel.\n" + 
				"            FILTER ( lang(?inputQuantityKindL) = \"en\" )\n" + 
				"          }\n" + 
				"        \n" + 
				"     \n" + 
				"    ?output  rdf:type              peco:EmissionCalculationEntity ;\n" + 
				"             prov:wasGeneratedBy   ?activity ;\n" + 
				"             rdfs:label            ?outputLabel ;\n" + 
				"             qudt:value            ?outputValue ;\n" + 
				"             qudt:hasQuantityKind  ?outputQuantityKind ;\n" + 
				"             qudt:unit             ?outputUnit\n" + 
				"    OPTIONAL\n" + 
				"          { ?outputUnit  qudt:abbreviation  ?outputUnitLabel\n" + 
				"            FILTER ( lang(?outputUnitLabel) = \"en\" )\n" + 
				"          }\n" + 
				"        OPTIONAL\n" + 
				"          { ?outputQuantityKind\n" + 
				"                      rdfs:label  ?outputQuantityKindL\n" + 
				"            FILTER ( lang(?outputUnitLabel) = \"en\" )\n" + 
				"          }\n" + 
				"     \n" + 
				"  } ";
        System.out.println(queryString);
		return  runTupleQueryListResult(queryString);
	}

	public static ArrayList<Double[][]> getLinksToEmissionSources(String payload, OntModel semModel) {
		ArrayList<Double[][]> list = new ArrayList <Double[][]> ();
		
		Double[][] coordinates = new Double [2][2];
		coordinates [0][0] = 56.249324;
		coordinates [0][1] = -2.654500;
		coordinates [1][0] = 56.249100;
		coordinates [1][1] = -2.652925;
		
	
		list.add(coordinates);
		return list;
	}

	public static ArrayList<HashMap<String, String>> getFarms() {
		

		String queryString = " Prefix smart:<https://smartdatamodels.org/dataModel.Agrifood/> Prefix smart_base:<https://smartdatamodels.org/> SELECT DISTINCT ?iri ?label FROM <"+ConstantsDB.ASSETS_NAMED_GRAPH_IRI+"> WHERE { ?iri a smart:AgriFarm; smart_base:name ?label} ";
		
			   
		return runTupleQueryListResult (queryString);
	}

	public static ArrayList<HashMap <String,String>>  getSensorData(String sensorIRI) {
		
		String queryString = " Prefix sosa:<http://www.w3.org/ns/sosa/>  Prefix ssn:<http://www.w3.org/ns/ssn/> "
							+ "Prefix qudt:<http://qudt.org/schema/qudt/>"
							+ "Prefix smart:<https://smartdatamodels.org/dataModel.Agrifood/> "
							+ "Prefix smart_base:<https://smartdatamodels.org/> "
							+ "SELECT DISTINCT ?result ?time FROM <"+ConstantsDB.OBSERVATIONS_NAMED_GRAPH_IRI+"> "
									+ "WHERE { ?obs sosa:madeBySensor <"+sensorIRI+">; sosa:hasResult ?resultObj; sosa:resultTime ?time. ?resultObj qudt:value ?result} ORDER BY ASC(?time) ";
		
		return runTupleQueryListResult (queryString);
	}
	
	
	private static HashMap <String,String> runTupleQuerySingleResult (String query) {
		
      Repository repo = GraphDBUtils.getFabricRepository(GraphDBUtils.getRepositoryManager());
	  RepositoryConnection conn = repo.getConnection();
		
      HashMap <String,String > map = new  HashMap <String,String >  ();
		
	    System.out.println(query);
		TupleQuery tupleQuery = conn.prepareTupleQuery(query);
			   try (TupleQueryResult result = tupleQuery.evaluate()) {
				      while (result.hasNext()) {  // iterate over the result
				         BindingSet bindingSet = result.next();
				         Set <String> bindings = bindingSet.getBindingNames();  
					    	
					    	Iterator it2 = bindings.iterator();
					    	while (it2.hasNext()) {
					    		String key = (String) it2.next();
					    		if (bindingSet.getValue(key)!=null)
					    		map.put(key, bindingSet.getValue(key).toString()) ;
					    	}  
				      }
				   
			   }
		return map;
		
	}
	
	private static ArrayList<HashMap <String,String>> runTupleQueryListResult (String query) {
		
	      Repository repo = GraphDBUtils.getFabricRepository(GraphDBUtils.getRepositoryManager());
		  RepositoryConnection conn = repo.getConnection();
			
	     
	      ArrayList<HashMap <String,String>> list = new ArrayList<HashMap <String,String>> ();
			
		    System.out.println(query);
			TupleQuery tupleQuery = conn.prepareTupleQuery(query);
				   try (TupleQueryResult result = tupleQuery.evaluate()) {
					      while (result.hasNext()) {  // iterate over the result
					    	 HashMap <String,String > map = new  HashMap <String,String >  ();
					         BindingSet bindingSet = result.next();
					         Set <String> bindings = bindingSet.getBindingNames();  
						    	
						    	Iterator it2 = bindings.iterator();
						    	while (it2.hasNext()) {
						    		String key = (String) it2.next();
						    		if (bindingSet.getValue(key)!=null)
						    		map.put(key, bindingSet.getValue(key).toString()) ;
						    		
						    	}  
						    	list.add(map);
					      }
					   
				   }
			return list;
			
		}


	public static ArrayList<HashMap <String,String>> getSensors() {
	     String queryString = " Prefix sosa:<http://www.w3.org/ns/sosa/> Prefix smart:<https://smartdatamodels.org/dataModel.Agrifood/> Prefix smart_base:<https://smartdatamodels.org/> SELECT DISTINCT ?iri ?label FROM <"+ConstantsDB.ASSETS_NAMED_GRAPH_IRI+"> WHERE {?iri a sosa:Sensor;smart_base:name ?label }  ";
		
		return runTupleQueryListResult (queryString);

	}
	
	public static ArrayList<HashMap <String,String>> getSensorType(String sensorIri) {
	     String queryString = " Prefix sosa:<http://www.w3.org/ns/sosa/> Prefix smart:<https://smartdatamodels.org/dataModel.Agrifood/> Prefix smart_base:<https://smartdatamodels.org/> SELECT DISTINCT ?type FROM <"+ConstantsDB.ASSETS_NAMED_GRAPH_IRI+"> WHERE {<"+sensorIri+"> a ?type. }  ";
		
		return runTupleQueryListResult (queryString);

	}

	public static HashMap<String, String> runSparqlQuery(String payload) {
		// TODO Auto-generated method stub
		return runTupleQuerySingleResult (payload);
	}

	public static ArrayList<HashMap <String,String>>  getAssetsForEmissionCalculationMethods() {
		String queryString = "Prefix sosa:<http://www.w3.org/ns/sosa/>\n"
				+ "Prefix ssn:<http://www.w3.org/ns/ssn/>\n"
				+ "Prefix qudt:<http://qudt.org/schema/qudt/>\n"
				+ "PREFIX  ep-plan: <https://w3id.org/ep-plan#>\n"
				+ "prefix smart-core:<https://smartdatamodels.org/>\n"
				+ "prefix smart-data:<https://smartdatamodels.org/dataModel.Agrifood/>\n"
				+ "\n"
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
				+ "SELECT *\n"
				+ "FROM <"+ConstantsDB.ASSETS_NAMED_GRAPH_IRI+">\n"
				+ "\n"
				+ "WHERE\n"
				+ "{\n"
				+ "    {  ?asset a sosa:Actuator; smart-core:name ?label.}\n"
				+ "    UNION\n"
				+ "    {\n"
				+ "     ?asset a smart-data:AgriParcel; smart-core:name ?label.\n"
				+ "    }\n"
				+ "}\n"
				+ "";
		return runTupleQueryListResult(queryString);
	}

	public static ArrayList<HashMap<String, String>> getAvailableCalculationMethods() {
		String queryString = "PREFIX  ep-plan: <https://w3id.org/ep-plan#>\n"
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
				+ "SELECT *\n"
				+ "FROM <"+ConstantsDB.METHOD_PLANS+">\n"
				+ "\n"
				+ "WHERE\n"
				+ "{\n"
				+ "    ?method a ep-plan:Plan.\n"
				+ "    ?method rdfs:label ?label.\n"
				+ "}\n"
				+ "";
		return runTupleQueryListResult(queryString);
	}

	public static ArrayList<HashMap<String, String>> getAssignedCalculationMethods(String assetIRI) {
		String queryString = "PREFIX  ep-plan: <https://w3id.org/ep-plan#>\n"
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
				+ "PREFIX eats:<https://eats.org.uk/>\n"
				+ "\n"
				+ "SELECT *\n"
				+ "FROM <"+ConstantsDB.METHOD_PLANS+">\n"
				+ "\n"
				+ "WHERE\n"
				+ "{\n"
				+ "    ?method a ep-plan:Plan.\n"
				+ "    ?method rdfs:label ?label.\n"
				+ "    <"+assetIRI+"> eats:hasAssignedMethod ?method.\n"
				+ "}";
		return runTupleQueryListResult(queryString);
	}

	public static String linkMethodToAsset(String assetIRI, String methodIRI) {
		
		Repository repo = GraphDBUtils.getFabricRepository(GraphDBUtils.getRepositoryManager());
		RepositoryConnection conn = repo.getConnection();
		
		String queryString = "PREFIX eats:<https://eats.org.uk/>\n"
				+ "\n"
				+ "INSERT DATA {   \n"
				+ "    Graph <"+ConstantsDB.METHOD_PLANS+"> {\n"
				+ "     <"+assetIRI+">    eats:hasAssignedMethod  <"+methodIRI+">;      \n"
				+ "    }\n"
				+ "}";
		
		Update query = conn.prepareUpdate(queryString);
		System.out.println (queryString);
		   
		query.execute();
		return "added triples ";
	}

}
