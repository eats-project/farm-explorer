package uk.org.eats.graphdb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import org.eclipse.rdf4j.model.IRI;
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
import org.eclipse.rdf4j.rio.RDFParseException;
import org.springframework.util.ResourceUtils;

import uk.org.eats.farmviewer.ServiceController;







public class CheckAndSetUpGraphDB {



public static void checkRepositorySetUp () throws ClientProtocolException, IOException {
	
	RepositoryManager repositoryManager = new RemoteRepositoryManager( "http://localhost:7200" );
	// Get the repository from repository manager, note the repository id set in configuration .ttl file
	//https://graphdb.ontotext.com/documentation/free/configuring-a-repository.html#configure-a-repository-programmatically
	
	Repository repository = GraphDBUtils.getFabricRepository(repositoryManager);
	
		
    if (repository==null) {
      	//HAndle if there  is no repository
    	System.out.println ("Creating  Farm Explorer Repository");
    	
    	CloseableHttpClient httpClient = HttpClients.createDefault();
    	HttpPost configureRepository = new HttpPost("http://localhost:7200/rest/repositories");
    	MultipartEntityBuilder builder = MultipartEntityBuilder.create();

    	// This attaches the file to the POST:
    	File f = ResourceUtils.getFile("classpath:RepositoryConfig.ttl");   	
    	builder.addBinaryBody(
    	    "config",
    	    new FileInputStream(f),
    	    ContentType.APPLICATION_OCTET_STREAM,
    	    f.getName()
    	);

    	HttpEntity multipart = builder.build();
    	configureRepository.setEntity(multipart);
    	CloseableHttpResponse response = httpClient.execute(configureRepository);
    	HttpEntity responseEntity = response.getEntity();
    	//to do - check if it worked
    	repository = GraphDBUtils.getFabricRepository(repositoryManager);
    	
    	
    	
    }
	
	
	
    if (repository!=null) {
    
    	//set up connection for service controll as well 
    	ServiceController.repository = repository;
    	
    	
    	ValueFactory f = repository.getValueFactory();

	// Open a connection to this repository
	RepositoryConnection conn = repository.getConnection();	
	
	
	
	if (!GraphDBUtils.checkResourcePresent (conn.getContextIDs(), Constants.OBSERVATIONS_NAMED_GRAPH_IRI)) {
		System.out.println ("Adding the default Observations named graph") ;
		
		IRI context = f.createIRI(Constants.OBSERVATIONS_NAMED_GRAPH_IRI);
		conn.add(f.createIRI(Constants.OBSERVATIONS_NAMED_GRAPH_IRI), RDFS.LABEL, f.createLiteral("Observations"), context); 		
	}
	
	if (!GraphDBUtils.checkResourcePresent (conn.getContextIDs(), Constants.ASSETS_NAMED_GRAPH_IRI)) {
		System.out.println ("Adding the default Assets named graph") ;
		
		IRI context = f.createIRI(Constants.ASSETS_NAMED_GRAPH_IRI);
		conn.add(f.createIRI(Constants.ASSETS_NAMED_GRAPH_IRI), RDFS.LABEL, f.createLiteral("Assets"), context); 		
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