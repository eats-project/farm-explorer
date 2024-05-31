package uk.org.eats.graphdb;

import java.util.UUID;

public class Constants {
	
	
	public static final String EPPLAN_NAMESPACE = "https://w3id.org/ep-plan#";

	public static final  String  DEFAULT_NAMED_GRAPH_NAMESPACE ="https://eats.org.uk/NamedGraph/";
	
	public static final String OBSERVATIONS_NAMED_GRAPH_IRI ="https://eats.org.uk/Observations/";
	public static final String ASSETS_NAMED_GRAPH_IRI ="https://eats.org.uk/Assets/";

	public static final String GRAPH_DB_URL  = "http://localhost:7200";
	
	public static final String DEFAULT_INSTANCE_NAMESPACE  = "https://eats.org.uk/InstanceData/";
	
	public static final String FABRIC_REPOSITORY_NAME = "FarmExplorer"; 
	
	public static final String PROV_NAMESPACE = "http://www.w3.org/ns/prov#";
	
	public static final String PREFIXES = " Prefix ep-plan: <"+EPPLAN_NAMESPACE+">  Prefix prov: <"+PROV_NAMESPACE+"> PREFIX owl: <http://www.w3.org/2002/07/owl#> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#> PREFIX mls: <http://www.w3.org/ns/mls#> PREFIX dcterms: <http://purl.org/dc/terms/>";

	public static final String OWL_NAMESPACE = "http://www.w3.org/2002/07/owl#";
	
}