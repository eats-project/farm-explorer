package uk.org.eats.graphdb;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.eclipse.rdf4j.sparqlbuilder.core.query.DeleteDataQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.springframework.util.ResourceUtils;

public class GraphDBUtils {

	
 public static Repository getFabricRepository (RepositoryManager repositoryManager ) {
	repositoryManager.init();
	Repository repository = repositoryManager.getRepository(ConstantsDB.FABRIC_REPOSITORY_NAME);
	return repository;
}
 
 
 public static RepositoryManager getRepositoryManager () {
	 
	 return new RemoteRepositoryManager( ConstantsDB.GRAPH_DB_URL  ); 
 }
 

	
 public static boolean checkResourcePresent (RepositoryResult <Resource> res, String iriToLookFor) {
		boolean present = false; 
	    Iterator <Resource> it = res.iterator();
		
		while (it.hasNext()) {
			
			Resource element = it.next(); 
			if (element.stringValue().equals(iriToLookFor) ) {
				present = true;
			}
		}

	return present;
	}

 public static boolean checkNamespacePresent (RepositoryResult <Namespace> res, String iriToLookFor) {
		boolean present = false; 
	    Iterator <Namespace> it = res.iterator();
		
		while (it.hasNext()) {
			
			Namespace element = it.next(); 
			if (element.getName().equals(iriToLookFor) ) {
				present = true;
			}
		}

	return present;
	}
 
 public static void addDefaultSystemComponentNamedGraphOntologies (Resource planNamedGraphContext, RepositoryConnection conn) throws RDFParseException, RepositoryException, IOException {
		
	    System.out.println("Loading default components from local copy of ontologies <not implemented>");
		/*
	    File file = ResourceUtils.getFile("classpath:sao.ttl"); 
		conn.add(file, null, RDFFormat.TURTLE,planNamedGraphContext);
		
		file = ResourceUtils.getFile("classpath:rains.ttl"); 
		conn.add(file, null, RDFFormat.TURTLE,planNamedGraphContext);
		*/
		
	}

public static String addJsonLD(String payload, String assetsNamedGraphIri) {
	
	Repository repo = getFabricRepository(getRepositoryManager());
	RepositoryConnection conn = repo.getConnection();
	ValueFactory f = repo.getValueFactory();
	Model results = null;
	
	try {  
		    results = Rio.parse(new StringReader(payload), null, RDFFormat.JSONLD);
	}
	catch (IOException e) {
		  // handle IO problems (e.g. the file could not be read)
			System.out.println(e.getLocalizedMessage());
		}
		catch (RDFParseException e) {
		  // handle unrecoverable parse error
			System.out.println(e.getLocalizedMessage());
		}
		catch (RDFHandlerException e) {
		  // handle a problem encountered by the RDFHandler
			System.out.println(e.getLocalizedMessage());
		}
	
	
	
	if (results!=null) {
		System.out.println("adding model size" +results.size() );
		System.out.println("adding into named graph" +assetsNamedGraphIri );
	conn.add(results.getStatements(null, null, null, (Resource)null), f.createIRI(assetsNamedGraphIri));
	return "addJsonLD: success";
	}
	else {
		System.out.println("Parsing content to be saved failed ");
		return "addJsonLD: error";
	}
	
}


public static String clearNamedGraph(String namedGraphIri) {
	
	Repository repo = getFabricRepository(getRepositoryManager());
	RepositoryConnection conn = repo.getConnection();
	ValueFactory f = repo.getValueFactory();
	
	String queryString = "clear graph  <"+ ConstantsDB.ASSETS_NAMED_GRAPH_IRI +">";
	
	System.out.println ("Deleting populated "+namedGraphIri+" named graph") ;
	
	Update query = conn.prepareUpdate(queryString);

	query.execute();

	System.out.println ("Adding empty "+namedGraphIri+" named graph") ;
	
	IRI context = f.createIRI(ConstantsDB.ASSETS_NAMED_GRAPH_IRI);
	conn.add(f.createIRI(ConstantsDB.ASSETS_NAMED_GRAPH_IRI), RDFS.LABEL, f.createLiteral("Assets"), context); 
	
	
	return "clearNamedGraph: need to handle response server side";
}
 

public static String dumpGraphAsJsonLd(String namedGraphIri) throws Exception {
	Repository repo = getFabricRepository(getRepositoryManager());
	RepositoryConnection conn = repo.getConnection();
    
	OutputStream output = new OutputStream() {
	    private StringBuilder string = new StringBuilder();

	    @Override
	    public void write(int b) throws IOException {
	        this.string.append((char) b );
	    }

	    //Netbeans IDE automatically overrides this toString()
	    public String toString() {
	        return this.string.toString();
	    }
	};
	
    RDFWriter writer = Rio.createWriter(RDFFormat.JSONLD, output);
    writer.getWriterConfig().set(JSONLDSettings.COMPACT_ARRAYS, true);
    writer.getWriterConfig().set(JSONLDSettings.HIERARCHICAL_VIEW, true);
  
    String query = "CONSTRUCT { ?subject ?predicate ?object . } FROM  <" + namedGraphIri + ">" + " WHERE { ?subject ?predicate ?object . }";
    Model model = QueryResults.asModel(conn.prepareGraphQuery(query).evaluate());
    try {
    	  writer.startRDF();
    	  for (Statement st: model) {
    	    writer.handleStatement(st);
    	  }
    	  writer.endRDF();
    	}
    	catch (RDFHandlerException e) {
    	 // oh no, do something!
    	}
    finally {
    	output.close();
    }
    	
    
    
    System.out.println(query);
   
    System.out.println(output.toString());
    return output.toString();
  }
 
}