package uk.org.eats.graphdb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.UUID;


import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.util.ResourceUtils;

import uk.org.eats.farmviewer.ServiceController;







public class CheckAndSetUpGraphDB {

private static  boolean configureRepository (String fileName) throws ClientProtocolException, IOException {
	CloseableHttpClient httpClient = HttpClients.createDefault();
	MultipartEntityBuilder builder = MultipartEntityBuilder.create();
	// This attaches the file to the POST:
	File f = ResourceUtils.getFile("classpath:"+fileName);   	
	builder.addBinaryBody(
	    "config",
	    new FileInputStream(f),
	    ContentType.APPLICATION_OCTET_STREAM,
	    f.getName()
	);
	HttpEntity multipart = builder.build();
	HttpPost configureRepository = new HttpPost("http://localhost:7200/rest/repositories");
	configureRepository.setEntity(multipart);
	

	CloseableHttpResponse response = httpClient.execute(configureRepository);
	HttpEntity responseEntity = response.getEntity();
	
	
	// Check the response status and log response details
    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode == 200 || statusCode == 201) {
        System.out.println("Repository created successfully.");
        // Close resources
        EntityUtils.consume(responseEntity);
        response.close();
        httpClient.close();
        return true;
    } else {
        // Close resources
        EntityUtils.consume(responseEntity);
        response.close();
        httpClient.close();
        return false;
    }

   
	
}

public static void checkRepositorySetUp () throws ClientProtocolException, IOException {
	
	RepositoryManager repositoryManager = new RemoteRepositoryManager( "http://localhost:7200" );
	// Get the repository from repository manager, note the repository id set in configuration .ttl file
	//https://graphdb.ontotext.com/documentation/free/configuring-a-repository.html#configure-a-repository-programmatically
	
	Repository repository = GraphDBUtils.getFabricRepository(repositoryManager);
	
	
	
	
		
    if (repository==null) {
      	//HAndle if there  is no repository
    	System.out.println ("Creating  Farm Explorer Repository ...");
    	
    	
    	if (configureRepository("RepositoryConfig.ttl")) {
    		System.out.println ("Farm Explorer Repository for GRAPH DB SE Created"); 	
    	}
    	else if (configureRepository("RepositoryConfigFree.ttl)")) {
    		System.out.println ("Farm Explorer Repository for GRAPH DB Free Created");
    	}
    	
    	else {
    		System.out.println ("Could not create a GRAPH DB repository - check versions and Sail type in the config file.");
    	}

    	
       
    	repository = GraphDBUtils.getFabricRepository(repositoryManager);
    	
    	
    	
    }
	
	
	
    if (repository!=null) {
    
    	//set up connection for service controll as well 
    	ServiceController.repository = repository;
    	
    	
    	ValueFactory f = repository.getValueFactory();
    	System.out.println ("Connecting to repository") ;
	// Open a connection to this repository
	RepositoryConnection conn = repository.getConnection();	
	
	
	//add additional labels 
	File file2 = ResourceUtils.getFile("classpath:data/sensor_labels.ttl");
	 InputStream input2 = new FileInputStream(file2);
	    Model model2 = Rio.parse(input2, "", RDFFormat.TURTLE);

	            // Start a transaction to add the data to the named graph
	            conn.begin();
	            conn.add(model2, f.createIRI(ConstantsDB.ASSETS_NAMED_GRAPH_IRI));
	            conn.commit();  // Commit transaction to finalize changes
	
	
	if (!GraphDBUtils.checkResourcePresent (conn.getContextIDs(), ConstantsDB.OBSERVATIONS_NAMED_GRAPH_IRI)) {
		System.out.println ("Adding the default Observations named graph") ;
		
		IRI context = f.createIRI(ConstantsDB.OBSERVATIONS_NAMED_GRAPH_IRI);
		conn.add(f.createIRI(ConstantsDB.OBSERVATIONS_NAMED_GRAPH_IRI), RDFS.LABEL, f.createLiteral("Observations"), context); 		
	}
	
	if (!GraphDBUtils.checkResourcePresent (conn.getContextIDs(), ConstantsDB.ASSETS_NAMED_GRAPH_IRI)) {
		System.out.println ("Adding the default Assets named graph") ;
		
		IRI context = f.createIRI(ConstantsDB.ASSETS_NAMED_GRAPH_IRI);
		conn.add(f.createIRI(ConstantsDB.ASSETS_NAMED_GRAPH_IRI), RDFS.LABEL, f.createLiteral("Assets"), context); 
		
		//add additional labels 
		File file = ResourceUtils.getFile("classpath:data/sensor_labels.ttl");
		 InputStream input = new FileInputStream(file);
		    Model model = Rio.parse(input, "", RDFFormat.TURTLE);

		            // Start a transaction to add the data to the named graph
		            conn.begin();
		            conn.add(model, context);
		            conn.commit();  // Commit transaction to finalize changes
	}
	
	if (!GraphDBUtils.checkResourcePresent (conn.getContextIDs(), ConstantsDB.METHOD_PLANS)) {
		System.out.println ("Adding the default Method Plans named graph") ;
		
		IRI context = f.createIRI(ConstantsDB.METHOD_PLANS);
		conn.add(f.createIRI(ConstantsDB.METHOD_PLANS), RDFS.LABEL, f.createLiteral("Method Plans"), context); 		
	}
	
	if (!GraphDBUtils.checkResourcePresent (conn.getContextIDs(), ConstantsDB.CONVERSION_FACTORS)) {
		System.out.println ("Adding the default CONVERSION FACTORS named graph") ;
		
		IRI context = f.createIRI(ConstantsDB.CONVERSION_FACTORS);
		conn.add(f.createIRI(ConstantsDB.CONVERSION_FACTORS), RDFS.LABEL, f.createLiteral("Emissions Conversion Factors"), context); 
		File file = ResourceUtils.getFile("classpath:data/cf_2022.ttl");   	
		File wikiLabels = ResourceUtils.getFile("classpath:data/wikidata_labels.ttl");   	
		   
	     
	    InputStream input = new FileInputStream(file);
	    Model model = Rio.parse(input, "", RDFFormat.TURTLE);
	    
	   input = new FileInputStream(wikiLabels);
	    model.addAll(Rio.parse(input, "", RDFFormat.TURTLE));

	            // Start a transaction to add the data to the named graph
	            conn.begin();
	            conn.add(model, context);
	            conn.commit();  // Commit transaction to finalize changes
	       
	}
	

	
	
	
	
	/*
	// Get all statements in the context
	IRI context = f.createIRI(Constants.SYSTEMS_NAMED_GRAPH_IRI);
	try (RepositoryResult<Statement> result = conn.getStatements(null, null, null, context)) {
	   while (result.hasNext()) {
	      Statement st = result.next();
	    
	   }
	}
	*/
	
	//conn.clear(f.createIRI("https://rainsproject.org/PlanComponentLibrary"));
		
	// ... use the repository

	// Shutdown connection, repository and manager
	conn.close();
	repository.shutDown();
	repositoryManager.shutDown();
	
	
	
    }
    
    else {
    	
    	//HAndle if there  is still no repository
    	 System.out.println("Could not connet to the GraphDB repository. ");
    	 System.exit (0);
    	 
    }
	
}







}