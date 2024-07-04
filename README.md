Note: This software is work in progress/proof of concept. Expect messy code. Not production ready. 

# How to run

Prerequisites

Java 11 +,
Maven,
Git, 
Graph DB Free download from <a href="https://graphdb.ontotext.com/">here</a> or Graph DB Docker image from <a href="https://hub.docker.com/r/ontotext/graphdb/tags">here</a>

## Set up Graph DB
Launch GraphDB, by default, you can access GraphDB from a browser on localhost:7200.

## Set up Graph DB (Docker)

Note: if you use version below 10 then you need a license

````
docker pull ontotext/graphdb:10.6.4
````

````
docker run -p 127.0.0.1:7200:7200 --name graphdb-instance-name -t ontotext/graphdb:10.6.4
````

You should be able to access GraphDB from a browser on localhost:7200.

## Farm Explorer

````
gh repo clone eats-project/farm-explorer
````

then cd into the project directory and run 

````
mvn spring-boot:run
````

The app will create a new repository in your GraphDB instance called FarmExplorer and populates the graphs with example data used in the demo.

Go to localhost:3060 and you should see the home page of the Farm Explorer


![image](https://github.com/eats-project/farm-explorer/assets/4025828/9336b4b1-0a4c-4bba-8441-aa3ea86cedbf)

Click on the farm name to view details and on the building icon to expand the list

![image](https://github.com/eats-project/farm-explorer/assets/4025828/3b8c29cb-ce2b-4802-9879-38d421c45b49)

![image](https://github.com/eats-project/farm-explorer/assets/4025828/5bded7ce-0dab-49c4-8b9f-150045f0a25d)


## Demo 

<a href="https://w3id.org/tec-toolkit/ISWC-2024-demo">https://w3id.org/tec-toolkit/ISWC-2024-demo</a>
