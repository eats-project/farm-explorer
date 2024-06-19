package uk.org.eats.farmviewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryProvider;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import org.slf4j.LoggerFactory;

import uk.org.eats.graphdb.ConstantsDB;
import uk.org.eats.graphdb.GraphDBUtils;

public class PlanExecutor {

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final IRI hasInput = vf.createIRI("http://example.org/hasInput");
    private static final IRI hasOutput = vf.createIRI("http://example.org/hasOutput");
    private static final IRI stepType = vf.createIRI("http://example.org/stepType");

    private Set<Value> calculatedVariables = new HashSet<>();
    private Set<Resource> stepsExecuted = new HashSet<>();
    private Model plans_repo; 
    private Model executionTrace = new LinkedHashModel();
    private SailRepository local_plans_repository;
    HashMap <String,String> resultQueries = new HashMap <String, String> ();

	private IRI assetIRI,namedGraph ;

    public static void main(String[] args) {
        //new PlanExecutor().executeWorkflow();
    	 Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
         rootLogger.setLevel(Level.INFO);
    	new PlanExecutor().executeWorkflow( "urn:ngsi-ld:AgriFarm:Great%20Farm:Actuator:Irrigation%20Rig");
    }
    
    private void inputDataRetrieval () {
    	
    
    	Repository repo = GraphDBUtils.getFabricRepository(GraphDBUtils.getRepositoryManager());
    	RepositoryConnection conn = repo.getConnection();
    	
    	 
    	ValueFactory vf = conn.getValueFactory();
    	
    	try (RepositoryResult<Statement> statements = conn.getStatements(null, null, null,  vf.createIRI(ConstantsDB.METHOD_PLANS))) {
             plans_repo = QueryResults.asModel(statements);  // Convert the result to a Model
            
             //local repo so we can also query with SPARQL
             local_plans_repository = new SailRepository(new MemoryStore());
             local_plans_repository.init();
             RepositoryConnection local_conn = local_plans_repository.getConnection();
             local_conn.add(plans_repo);
            		 
    	}
    		
    	conn.close();	
       
    	IRI hasAssignedMethodPredicate = vf.createIRI("https://eats.org.uk/hasAssignedMethod");
    	
       
    	
    	 HashSet <Value> assignedPlans = new HashSet <Value> ();
     	
    	//get all the plans associated with the asset
    	 Iterable<Statement> statements = plans_repo.getStatements(null, hasAssignedMethodPredicate, null, vf.createIRI(ConstantsDB.METHOD_PLANS)); 
    			 // Iterate over the statements
    	 for (Statement statement : statements) {
            	 assignedPlans.add(statement.getObject());
             }
    	 
    	//execute workflow for each plan 
    	 Iterator<Value> it = assignedPlans.iterator();
         while (it.hasNext()) {
        	 
        	 Value methodPlan = it.next();
             retrieveData (methodPlan,assetIRI,namedGraph);
            
         }
         
        
    }
    
    private void retrieveData(Value methodPlan, IRI assetIRI, IRI namedGraph) {
    	Repository repo = GraphDBUtils.getFabricRepository(GraphDBUtils.getRepositoryManager());
    	RepositoryConnection conn = local_plans_repository.getConnection();
    	RepositoryConnection global_conn = repo.getConnection();
    	
    	
    	
    	ValueFactory vf = SimpleValueFactory.getInstance();
    	IRI isElementOfPlan = vf.createIRI("https://w3id.org/ep-plan#isElementOfPlan");
    	
    	 Iterable<Statement> statements = plans_repo.getStatements(null, isElementOfPlan, null, vf.createIRI(ConstantsDB.METHOD_PLANS));
			 // Iterate over the statements
    	 for (Statement statement : statements) {
       	
            System.out.println("Asset: " + statement.getSubject());
            System.out.println("Plan: " + statement.getObject());
            
            //find sparql queries
            IRI hasConstraintImplementation = vf.createIRI("https://w3id.org/ep-plan#hasConstraintImplementation");
            
            Resource constraintIRI = statement.getSubject();
            
            Iterable<Statement> queryObjects = plans_repo.getStatements(constraintIRI, hasConstraintImplementation, null, vf.createIRI(ConstantsDB.METHOD_PLANS));
            	//there should be only one
            	if (queryObjects.iterator().hasNext()) {
            		
            		 Iterable<Statement> query = plans_repo.getStatements(vf.createIRI(queryObjects.iterator().next().getObject().toString()), RDF.VALUE, null, vf.createIRI(ConstantsDB.METHOD_PLANS));
            			 if (query.iterator().hasNext()) {
            					String queryString= query.iterator().next().getObject().toString();
            			 				//System.out.println("QUERY >>>>>>>>");
            			 				//System.out.println(queryString);
            			 				queryString = queryString.replaceAll("\\$Asset", "<"+assetIRI.toString()+">");
            			 				
            			 				queryString = queryString.replaceAll("^\"|\"$", "");
            			 				//System.out.println("QUERY Transformed >>>>>>>>");
            			 				//System.out.println(queryString);
            			 				if (!queryString.contains("$Output")) {
            			 				GraphQuery graphQuery = global_conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString.replaceAll("\n", " "));
            			 				executionTrace.addAll(QueryResults.asModel(graphQuery.evaluate()));
            			 			    System.out.println("Model size" + executionTrace.size()); 
            			 				   
            			 				}
            			 				
            			 				else {
            			 					//Execute result queries later
            			 					IRI constraints = vf.createIRI("https://w3id.org/ep-plan#constrains");
            			 					
            			 					Iterable <Statement> steps = conn.getStatements(constraintIRI,constraints, null, vf.createIRI(ConstantsDB.METHOD_PLANS));
            			 						 if (steps.iterator().hasNext()) {
            			 							 resultQueries.put(steps.iterator().next().getObject().toString(), queryString);
            			 						 }
            			 					
            			 					
            			 				}
            		 }
            		 
            	}
            
            
        }
    	 
    	//we should have all inputs so we can execute steps
    	
    	System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    	
    	System.out.println(resultQueries.keySet());
    	
    	
    	
    	conn.close();
    
    }

    private void executeWorkflow(String assetIRI) {
    	
    	  this.assetIRI = vf.createIRI(assetIRI);
    	
            boolean workflowActive = true;
            
            namedGraph = vf.createIRI(assetIRI +":CarbonExecutionTrace");
            
            inputDataRetrieval ();
           
            
            while (workflowActive) {
            	
                workflowActive = false;
                IRI stepType = vf.createIRI("https://w3id.org/ep-plan#Step");
               
                Iterable <Statement> steps = plans_repo.getStatements(null, RDF.TYPE, stepType);
                System.out.println("In outer loop execute step");
               Iterator <Statement> it = steps.iterator();
                while (it.hasNext()) {
                	
                	System.out.println("In loop execute step");
                    Resource step = it.next().getSubject();
                    
                    ArrayList <Float> input_values = canExecuteStep(step);
                    System.out.println("Input values check " + (input_values != null));
                    if (!stepsExecuted.contains(step) && (input_values != null)) {
                    	System.out.println("--> Executing Step " + step);
                    	workflowActive =   executeStep(step,input_values);// Keep the loop going if we successfully executed a step
                    }
                  
                }
               
            }
     
        System.out.println("Workflow completed");
        
        //save the execution trace
        saveExecutionTrace ();
    }
    
    private void saveExecutionTrace () {
    	
    	Repository repo = GraphDBUtils.getFabricRepository(GraphDBUtils.getRepositoryManager());
		RepositoryConnection global_conn = repo.getConnection();
		System.out.println ("adding to named grpah "+ namedGraph);
		
		global_conn.add(executionTrace, namedGraph);
		
		//infer missing prov links 
		String queryString = "PREFIX  prov: <http://www.w3.org/ns/prov#>\n"
				+ "PREFIX ep-plan: <https://w3id.org/ep-plan#>\n"
				+ "PREFIX eats: <https://eats.org.uk/>\n"
				+ "PREFIX peco:<https://w3id.org/peco#>\n"
				+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
				+ "\n"
				+ "Construct {\n"
				+ "    ?activity ep-plan:correspondsToStep ?step.\n"
				+ "    ?activity a peco:EmissionCalculationActivity.\n"
				+ "    ?activity prov:used ?input.\n"
				+ "    ?input rdfs:label ?inputVarLabel.\n"
				+ "    ?input a peco:EmissionCalculationEntity.\n"
				+ "    ?activity rdfs:label ?stepLabel.\n"
				+ "    ?output prov:wasGeneratedBy ?activity.\n"
				+ "    ?output rdfs:label ?outputVarLabel.\n"
				+ "    ?output a peco:EmissionCalculationEntity.\n"
				+ "}\n"
				+ "FROM <https://eats.org.uk/MethodPlans/>\n"
				+ "FROM <urn:ngsi-ld:AgriFarm:Great%20Farm:Actuator:Irrigation%20Rig:CarbonExecutionTrace>\n"
				+ "\n"
				+ "WHERE {   \n"
				+ "    ?step a ep-plan:Step;rdfs:label ?stepLabel.\n"
				+ "    ?step ep-plan:isElementOfPlan ?plan.\n"
				+ "    ?inputVariable ep-plan:isInputVariableOf ?step; \n"
				+ "                   rdfs:label ?inputVarLabel.\n"
				+ "    ?input ep-plan:correspondsToVariable ?inputVariable. \n"
				+ "    ?outputVariable ep-plan:isOutputVariableOf ?step.\n"
				+ "    ?output ep-plan:correspondsToVariable ?outputVariable; \n"
				+ "            rdfs:label ?outputVarLabel.\n"
				+ "    \n"
				+ "    BIND (IRI(CONCAT(\"urn:ngsi-ld:AgriFarm:EmissionCalculationActivity:\", STRAFTER(STR(UUID()), \"uuid:\"))) AS ?activity)     \n"
				+ "}\n"
				+ "\n"
				+ "";
		GraphQuery graphQuery = global_conn.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
		
		global_conn.add(QueryResults.asModel(graphQuery.evaluate()),namedGraph);
		   
		
    	
		global_conn.close();
    }

    private ArrayList <Float> canExecuteStep(Resource step) {
    	RepositoryConnection conn = local_plans_repository.getConnection();
    	
    	ArrayList <Float> input_values = new  ArrayList <Float> (); 
    	
    	IRI isInputVariableOf = vf.createIRI("https://w3id.org/ep-plan#isInputVariableOf");
    	
        RepositoryResult<Statement> inputs = conn.getStatements(null, isInputVariableOf, step);

        System.out.println ("Step " + step);
        System.out.println ("Found Inputs " + inputs);
        
        while (inputs.hasNext()) {
            Resource input = inputs.next().getSubject();
            
            System.out.println ("Found Input " + input);
            //Check if we have qudt values for all Entities Corresponding to each input variable in the execution trace
            
                IRI correspondsToVariable = vf.createIRI("https://w3id.org/ep-plan#correspondsToVariable");
                Iterable <Statement> entitiy = executionTrace.getStatements(null, correspondsToVariable, input);
                
                
                System.out.println ("Found Entitiy " + entitiy);
                if (!entitiy.iterator().hasNext()) {
                	System.out.println ("no linked variables returning null");
                    return null;
                }
                else {
                  System.out.println ("Found Entity");
                  Resource entityIRI = entitiy.iterator().next().getSubject();
                  IRI qudt_value = vf.createIRI("http://qudt.org/schema/qudt/value");
                  IRI rdf_value = vf.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#value");
                  Iterable <Statement> entitiy_qudt_values = executionTrace.getStatements(entityIRI, qudt_value, null);
                  if (entitiy_qudt_values.iterator().hasNext()) {
                	  System.out.println ("Found Value "+ entitiy_qudt_values.iterator().next().getObject());
                	  Literal value = (Literal) entitiy_qudt_values.iterator().next().getObject();
                	  input_values.add(value.floatValue());
                  }
                  else {
                	  Iterable <Statement> entitiy_rdf_values = executionTrace.getStatements(entityIRI, rdf_value, null);
                  
                      if (!entitiy_rdf_values.iterator().hasNext()) {
                	     return null;
                      }
                      else {
                    	  System.out.println ("Found Value "+ entitiy_rdf_values.iterator().next().getObject());
                    	  Literal value = (Literal) entitiy_rdf_values.iterator().next().getObject();
                    	  input_values.add(value.floatValue());       	  
                      }  
                  }  
                }
            
           
            
        }
        return input_values;
    }

    private boolean executeStep(Resource step, ArrayList <Float> input_values) {
    	RepositoryConnection conn = local_plans_repository.getConnection();
        
    	Iterable <Statement> types = plans_repo.getStatements(step, RDF.TYPE, null);
    	
    	System.out.println("Executing " + step.stringValue());
    	Iterator <Statement> it = types.iterator();
    	while (it.hasNext()) {
            String type = it.next().getObject().toString();
            System.out.println("Type " + type);
            if (type.equals("http://www.openmath.org/cd/arith1#times")&&input_values.size()>1) {
            	float result = 1; 
            	for (int i =0;i<input_values.size();i++) {
            		result = result * input_values.get(i);
            	}
            	
            	System.out.println("Calculated multiplication  " + result);
             	System.out.println("Calculated multiplication  " + step);
            	executeStepOutputQuery (step.toString(),result);
            	stepsExecuted.add(step);
            	return true;
            }
            	
            if (type.equals("http://www.openmath.org/cd/arith1#plus")&&input_values.size()>1) {
            	float result = 0; 
            	for (int i =0;i<input_values.size();i++) {
            		result = result + input_values.get(i);
            	}
            	
            	System.out.println("Calculated addition  " + result);
            	executeStepOutputQuery (step.toString(),result);
            	stepsExecuted.add(step);
            	return true;
            }
            
            
        }
    	
  
        
        return false;
    }
    
	private void executeStepOutputQuery (String stepIRI, float outputValue) {
		
		Repository repo = GraphDBUtils.getFabricRepository(GraphDBUtils.getRepositoryManager());
		RepositoryConnection global_conn = repo.getConnection();
		
		String queryString = resultQueries.get(stepIRI);
		System.out.println(resultQueries.keySet());
		queryString = queryString.replaceAll("\\$Output", Float.toString(outputValue));
		queryString = queryString.replaceAll("\n", " ");
	
		
		GraphQuery graphQuery = global_conn.prepareGraphQuery(QueryLanguage.SPARQL,queryString );
		System.out.println(queryString);
		executionTrace.addAll(QueryResults.asModel(graphQuery.evaluate()));
		
        global_conn.close();
	
	}
	
	
}
