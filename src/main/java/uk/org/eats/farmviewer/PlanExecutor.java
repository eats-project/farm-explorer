package uk.org.eats.farmviewer;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryProvider;

public class PlanExecutor {

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final IRI hasInput = vf.createIRI("http://example.org/hasInput");
    private static final IRI hasOutput = vf.createIRI("http://example.org/hasOutput");
    private static final IRI stepType = vf.createIRI("http://example.org/stepType");

    private Set<Value> calculatedVariables = new HashSet<>();
    private Set<Resource> stepsExecuted = new HashSet<>();

    public static void main(String[] args) {
        new PlanExecutor().executeWorkflow();
    }

    private void executeWorkflow() {
        Repository repo = RepositoryProvider.getRepository("http://localhost:8080/rdf4j-server/repositories/yourrepo");
        try (RepositoryConnection conn = repo.getConnection()) {
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
