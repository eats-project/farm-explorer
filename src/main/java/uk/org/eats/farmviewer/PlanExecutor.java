package uk.org.eats.farmviewer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryProvider;

import uk.org.eats.graphdb.ConstantsDB;
import uk.org.eats.graphdb.GraphDBUtils;

public class PlanExecutor {

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final IRI hasInput = vf.createIRI("http://example.org/hasInput");
    private static final IRI hasOutput = vf.createIRI("http://example.org/hasOutput");
    private static final IRI stepType = vf.createIRI("http://example.org/stepType");

    private Set<Value> calculatedVariables = new HashSet<>();
    private Set<Resource> stepsExecuted = new HashSet<>();

    public static void main(String[] args) {
        //new PlanExecutor().executeWorkflow();
    	new PlanExecutor().testRetrieval();
    }
    
    private void testRetrieval () {
    	
    	String assetIRI = "<urn:ngsi-ld:AgriFarm:Great%20Farm:Actuator:Irrigation%20Rig>";
    	Repository repo = GraphDBUtils.getFabricRepository(GraphDBUtils.getRepositoryManager());
    	RepositoryConnection conn = repo.getConnection();
        
    	ValueFactory vf = conn.getValueFactory();
    	IRI hasAssignedMethodPredicate = vf.createIRI("https://eats.org.uk/hasAssignedMethod");
    	
        IRI namedGraph = vf.createIRI("http://example.com/fixedAsset");
    	
    	 HashSet <Value> assignedPlans = new HashSet <Value> ();
     	
    	//get all the plans associated with the asset
    	 try (RepositoryResult<Statement> statements = conn.getStatements(null, hasAssignedMethodPredicate, null, vf.createIRI(ConstantsDB.METHOD_PLANS))) {
    			 // Iterate over the statements
             while (statements.hasNext()) {      	
            	 Statement statement = statements.next();
            	 assignedPlans.add(statement.getObject());
                 System.out.println("Asset: " + statement.getSubject());
                 System.out.println("Plan: " + statement.getObject());
             }}
    	 
    	//execute workflow for each plan 
    	 Iterator<Value> it = assignedPlans.iterator();
         while (it.hasNext()) {
        	 Value methodPlan = it.next();
             retrieveData (methodPlan,assetIRI,namedGraph);
            
         }
         
         conn.close();
    }
    
    private void retrieveData(Value methodPlan, String assetIRI, IRI namedGraph) {
    	Repository repo = GraphDBUtils.getFabricRepository(GraphDBUtils.getRepositoryManager());
    	RepositoryConnection conn = repo.getConnection();
    	
    	HashMap <String,String> resultQueries = new HashMap <String, String> ();
    	
    	ValueFactory vf = conn.getValueFactory();
    	IRI isElementOfPlan = vf.createIRI("https://w3id.org/ep-plan#isElementOfPlan");
    	
    	try (RepositoryResult<Statement> statements = conn.getStatements(null, isElementOfPlan, null, vf.createIRI(ConstantsDB.METHOD_PLANS))) {
			 // Iterate over the statements
        while (statements.hasNext()) {      	
       	 Statement statement = statements.next();
       	
            System.out.println("Asset: " + statement.getSubject());
            System.out.println("Plan: " + statement.getObject());
            
            //find sparql queries
            IRI hasConstraintImplementation = vf.createIRI("https://w3id.org/ep-plan#hasConstraintImplementation");
            
            Resource constraintIRI = statement.getSubject();
            
            try (RepositoryResult<Statement> queryObjects = conn.getStatements(constraintIRI, hasConstraintImplementation, null, vf.createIRI(ConstantsDB.METHOD_PLANS))) {
            	//there should be only one
            	if (queryObjects.hasNext()) {
            		
            		 try (RepositoryResult<Statement> query = conn.getStatements(vf.createIRI(queryObjects.next().getObject().toString()), RDF.VALUE, null, vf.createIRI(ConstantsDB.METHOD_PLANS))) {
            			 if (query.hasNext()) {
            					String queryString= query.next().getObject().toString();
            			 				System.out.println("QUERY >>>>>>>>");
            			 				System.out.println(queryString);
            			 				queryString = queryString.replaceAll("\\$Asset", assetIRI);
            			 				
            			 				queryString = queryString.replaceAll("^\"|\"$", "");
            			 				System.out.println("QUERY Transformed >>>>>>>>");
            			 				System.out.println(queryString);
            			 				if (!queryString.contains("$Output")) {
            			 				    GraphQuery graphQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString.replaceAll("\n", " "));
            			 				    Model model = QueryResults.asModel(graphQuery.evaluate());
            			 				    // Store the model in a named graph
            			 				    conn.add(model, namedGraph);
            			 				}
            			 				
            			 				else {
            			 					//Execute result queries later
            			 					IRI constraints = vf.createIRI("https://w3id.org/ep-plan#constrains");
            			 					
            			 					try (RepositoryResult<Statement> steps = conn.getStatements(constraintIRI,constraints, null, vf.createIRI(ConstantsDB.METHOD_PLANS))) {
            			 						 if (steps.hasNext()) {
            			 							 resultQueries.put(steps.next().getObject().toString(), queryString);
            			 						 }
            			 					
            			 					}
            			 				}
            		 }
            		 }
            	}
            }
            
        }}
    	
    	System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    	
    	System.out.println(resultQueries.keySet());
    	
    	conn.close();
    
    }

    private void executeWorkflow() {
    	Repository repo = GraphDBUtils.getFabricRepository(GraphDBUtils.getRepositoryManager());
    	RepositoryConnection conn = repo.getConnection();
            boolean workflowActive = true;
            while (workflowActive) {
                workflowActive = false;
                RepositoryResult<Statement> steps = conn.getStatements(null, RDF.TYPE, null);
                
                
                
                
                while (steps.hasNext()) {
                    Resource step = steps.next().getSubject();
                    if (!stepsExecuted.contains(step) && canExecuteStep(step, conn)) {
                        executeStep(step, conn);
                        workflowActive = true; // Keep the loop going if we executed a step
                    }
                }
            }
     
        System.out.println("Workflow completed");
    }

    private boolean canExecuteStep(Resource step, RepositoryConnection conn) {
        RepositoryResult<Statement> inputs = conn.getStatements(step, hasInput, null);
        while (inputs.hasNext()) {
            Value input = inputs.next().getObject();
            if (!calculatedVariables.contains(input)) {
                return false;
            }
        }
        return true;
    }

    private void executeStep(Resource step, RepositoryConnection conn) {
        RepositoryResult<Statement> outputs = conn.getStatements(step, hasOutput, null);
        while (outputs.hasNext()) {
            Value output = outputs.next().getObject();
            calculatedVariables.add(output);
        }
        stepsExecuted.add(step);
        System.out.println("Executed " + step.stringValue());
    }
	
	
}
