const twoDigits = n => Number(Number(n).toFixed(2));

//round to two decimal places
function roundTwoDecimal (num) {
	return Math.round((num + Number.EPSILON) * 100) / 100
}

function htmlToNode(htmlString) {
  const element = document.createElement("div"); // Create a temporary container element
  element.innerHTML = htmlString;  // Set the HTML content
  return element.firstChild; // Return the first child element (assuming it's the desired node)
}

function createBrowserList(items ) {
	
	console.log("creating list");
	
	let list = document.getElementById ("browserList");
	let counter = 0;
	let itemNode ='';
	
	items.forEach(function (item) {
	  
	   if (item['@type']&&item['@type'].includes("https://smartdatamodels.org/dataModel.Agrifood/AgriFarm")) {
	   
	   	itemNode = itemNode + `<li id="item-${item['@id']}"  onclick="getDetailsPane('farm','${item['@id']}');highlightSelected('item-${item['@id']}');" class="list-group-item"> 
	   	                       <a href="#" data-bs-toggle="collapse" data-bs-target="#collapse-sublist-${counter}"><i class="fas fa-building"></i></a>  ${item['https://smartdatamodels.org/name'][0]['@value']}
	   	                       </li><ul id="collapse-sublist-${counter}" class="list-group collapse">`
	   	
	   	 	itemNode = itemNode + `<div class="list-divider">Farm Emissions Sources</div>`
	   	if (item['https://smartdatamodels.org/dataModel.Agrifood/hasAgriParcel']) {
	   	let agriParcels = item['https://smartdatamodels.org/dataModel.Agrifood/hasAgriParcel'];
	   	
	   	agriParcels.forEach(function (agriParcel) {
		    console.log("adding parcel");
			itemNode = itemNode + `<li id="item-${agriParcel['@id']}" onclick="getDetailsPane('parcel','${agriParcel['@id']}');highlightSelected('item-${agriParcel['@id']}');" class="list-group-item"> <i class="fas fa-seedling"></i> ${agriParcel['https://smartdatamodels.org/name'][0]['@value']}
			                      </li>`
		    console.log(itemNode);
		});
		}
		
		if (item['https://smartdatamodels.org/dataModel.Agrifood/hasDevice']) {
	   	let devices = item['https://smartdatamodels.org/dataModel.Agrifood/hasDevice'];
	   	
	   	devices.forEach(function (device) {
		    console.log("adding device");
		    
		     if (device['@type'].includes("http://www.w3.org/ns/sosa/Actuator")) {
		    
			itemNode = itemNode + `<li id="item-${device['@id']}" onclick="getDetailsPane('actuator','${device['@id']}');highlightSelected('item-${device['@id']}');" class="list-group-item"> <i class="fas fa-toggle-on"></i> ${device['https://smartdatamodels.org/name'][0]['@value']}
			                       
			                       </li>`
	
		    }
		    
		}
		);
		
		}
		
			itemNode = itemNode + `<div class="list-divider">Automated Observations</div>`
		if (item['https://smartdatamodels.org/dataModel.Agrifood/hasDevice']) {
	   	let devices = item['https://smartdatamodels.org/dataModel.Agrifood/hasDevice'];
	   	
	   	devices.forEach(function (device) {
		    console.log("adding device");
		    
		    if (device['@type'].includes("http://www.w3.org/ns/sosa/Sensor")) {
		    
			itemNode = itemNode + `<li id="item-${device['@id']}" onclick="getDetailsPane('sensor','${device['@id']}');highlightSelected('item-${device['@id']}');" class="list-group-item"> <i class="fas fa-tachometer-alt"></i> ${device['https://smartdatamodels.org/name'][0]['@value']}</li>`
			                      
		    }
		    
		    
		});
		
		
		}
		
		
	   
	  
	   
	   itemNode = itemNode + `<div class="list-divider">Manual Observations</div>`
	   counter++;
	   
	   }
	});
	
	
	
	list.innerHTML = itemNode;
	
	
	
	
	/*
  <li class="list-group-item">
    <a href="#" data-bs-toggle="collapse" data-bs-target="#collapse-sublist-1">Item 2 (with sublist)</a>
    <ul id="collapse-sublist-1" class="list-group collapse">
      <li class="list-group-item">Sublist Item 1</li>
      <li class="list-group-item">Sublist Item 2</li>
    </ul>
  </li>
  <li class="list-group-item">Item 3</li>
	*/

    }


 function drawItemMarkers(items ) {
	          items.forEach(function (item) {
		
		      if (item['@type']&&item['@type'].includes("https://smartdatamodels.org/dataModel.Agrifood/AgriFarm")) {
			
			 // draw agri parcels 
			 
			 if (item['https://smartdatamodels.org/dataModel.Agrifood/hasAgriParcel']) {
		       let agriParcels = item['https://smartdatamodels.org/dataModel.Agrifood/hasAgriParcel'];
	   	
	   	       agriParcels.forEach(function (agriParcel) {
		          //TO DO add handling for multiple agri parcels on farm (currently only the first is selected) update code for handling look up of details 
 	              var coordinates = agriParcel['http://www.opengis.net/ont/geosparql#hasGeometry'][0]['http://www.opengis.net/ont/geosparql#asWKT'][0]['@value'].match(/-?\d+\.\d+/g).map(Number);
	              var deviceMarker = L.circleMarker( L.latLng(parseFloat(coordinates[0]),parseFloat(coordinates[1])), {
	                  radius: 8,
	                  color: 'blue',
	                  fillColor: 'red',
	                  fillOpacity: 0.8
	              }).addTo(map);

					let popupContent = ""; 

					if (item['@type'].includes("https://smartdatamodels.org/dataModel.Agrifood/AgriParcel")) {
						popupContent +=  '<strong>' + agriParcel ['https://smartdatamodels.org/name'][0]['@value'] + '</strong><br>';					
						deviceMarker.on('click', function () {	
			                       // calculateTunnelFootprint ();
                          });
					}
					else { 
						popupContent +=  '<strong> ' + item.name + '</strong>';
					}

	         	      // Add a popup with device information
					let popup = L.popup({
  						closeOnClick: false,
  						autoClose: false
						}).setContent(popupContent);

					deviceMarker.bindPopup(popup);
				   	
				   });	
				}
				
				//End agri parcel loop 
				}
				
				
				if (item['@type']&&item['@type'].includes("https://smartdatamodels.org/dataModel.Agrifood/AgriFarm")) {
			
			 // draw agri parcels 
			 
			 if (item['https://smartdatamodels.org/dataModel.Agrifood/hasDevice']) {
		       let devices = item['https://smartdatamodels.org/dataModel.Agrifood/hasDevice'];
	   	
	   	       devices.forEach(function (device) {
		          //TO DO add handling for multiple agri parcels on farm (currently only the first is selected) update code for handling look up of details 
 	              var coordinates = device['http://www.opengis.net/ont/geosparql#hasGeometry'][0]['http://www.opengis.net/ont/geosparql#asWKT'][0]['@value'].match(/-?\d+\.\d+/g).map(Number);
	              var deviceMarker = L.circleMarker( L.latLng(parseFloat(coordinates[0]),parseFloat(coordinates[1])), {
	                  radius: 8,
	                  color: 'blue',
	                  fillColor: 'red',
	                  fillOpacity: 0.8
	              }).addTo(map);

					let popupContent = ""; 

					if (item['@type'].includes("http://www.w3.org/ns/sosa/Sensor")) {
						popupContent +=  '<strong>' + device ['https://smartdatamodels.org/name'][0]['@value'] + '</strong><br>';					
						deviceMarker.on('click', function () {	
			                       // calculateTunnelFootprint ();
                          });
					}
					else { 
							popupContent +=  '<strong>' + device ['https://smartdatamodels.org/name'][0]['@value'] + '</strong><br>';					
				
					}

	         	      // Add a popup with device information
					let popup = L.popup({
  						closeOnClick: false,
  						autoClose: false
						}).setContent(popupContent);

					deviceMarker.bindPopup(popup);
				   	
				   });	
				}
				
				//End agri parcel loop 
				}
				
				
				//end outer loop
				});
				
				
}

//link entities on the map participation in the carbon footprint calculations 
function linkObservationFoI (mapLayer, provTrace) {

			fetch('/getLinksToEmissionSources',{
				method: 'POST',
				mode: "cors", // no-cors, *cors, same-origin
				body: provTrace
				,
				headers: {
					'Content-type': 'application/json; charset=UTF-8',
					'Access-Control-Allow-Origin': '*'
				},
			})
				.then((response) =>
					response.json()
				)
				.then((data) => {

					console.log(data)
					console.log(data[0][0]);
					console.log(data[0][1]);
                    

					// Connect the polygon with the point by drawing a line from one of the polygon's vertices to the point
                    let polylineCoords = [[data[0][0], data[0][1]]]; // Connecting the first vertex of the polygon with the point
                   
                   
                    
                    L.polyline(polylineCoords, {color: 'red'}).addTo(mapLayer);

					
				}
				)
						 .catch(function(error) { 
							 console.log ("cannot link sources");                       // catch
                console.log (error);
            });
       }
       
 function calculateFootprint (assetIRI) {
	
	let planExists = true;
	
	console.log(assetIRI)
	
	let panel = document.getElementById ('carbonFootprint');
	

	
	if (planExists)
	{
		 window.parent.postMessage({
        type: 'startedFootprintCalcualtion',
        text: "true"
    }, '*'); 
		fetch('/getAssignedCalculationMethods?assetIRI='+encodeURIComponent(assetIRI))
		.then((response) =>
			response.json()
		)
		.then((data) => {
			
			console.log(data)
			if (data.length >0) {
				
			
			
	     fetch('/calculateFootprint?assetIri='+encodeURIComponent(assetIRI))
		.then((response) =>
			response.json()
		)
		.then((data) => {
			console.log(data);
			console.log("Populating provenance trace for " + assetIRI)
		pupulateProvenanceTable (assetIRI);
			});
			
			}
			
			else {
				alert ("No emission calculation method is linked to the asset"); 
				window.parent.postMessage({
        type: 'canceledFootprintCalcualtion',
        text: "true"
    }, '*'); 
			}
			
			
			
			});
		
		
		
		
		
		
	}
	
	else {
		panel.innerHTML = `
		
		There are currently no existing calcualtion methods for this asset stored in the system. 
		
		 <!-- Button trigger modal -->
        <button type="button" class="btn btn-primary" onclick="updateIframeAndShowModal('${assetIRI}')">
        Create Calcualtion Method
        </button>
		`
	}
	
 }      
       
       
 function calculateTunnelFootprint () {
	
	console.log('calculating')
	
		let waterQuantity = estimateWaterQuantity(); 
	    let fertiliserQuantitiy = estimateFertiliserQuantity(waterQuantity.value, 0.01); 
		let electricity = estimateElectricity ({}, waterQuantity.value); 
			
	fetch('/cf_info_all?region=United Kingdom')
		.then((response) =>
			response.json()
		)
		.then((CF_data) => {
			console.log(CF_data)
			if (!CF_data[0].value) {
				alert ("Sorry we could not retrieve Conversion Factor matching this location.")				
			}
			
			const electricity_cf = removeLiteralType(CF_data[0].value);
			let carbonFootprintPanel = document.getElementById('carbonFootprint');
			console.log(electricity_cf);	
			carbonFootprintPanel.innerHTML  = `		
	<img src='./img/water.png'> ${waterQuantity.value} liters <br><br>
    <img src='./img/fertiliser.png'> ${fertiliserQuantitiy} liters ( 448 kg CO2e ) <br><br>
	<img src='./img/electricity.png'> ${roundTwoDecimal(electricity)} kWh ( ${roundTwoDecimal(electricity*electricity_cf)} kg CO2e ) <br><br>	<hr>
	<img src='./img/co2.png'> ${roundTwoDecimal(electricity*electricity_cf)+ 448} kg CO2e  <br><br>
			`;		
			
			pupulateProvenanceTable ();	
			});
			
			console.log ("linking"); 
			
			linkObservationFoI (lineLayer, generateJsonLDstring(graphLD))		
	
	}
	
function pupulateProvenanceTable (assetIRI) {
	
	
	        
					// PRINT TRANSFORMATIONS TABLE
			fetch('/getDataTransformations', {
				method: 'POST',
				mode: "cors", // no-cors, *cors, same-origin
				body: assetIRI
				,
				headers: {
					'Content-type': 'application/json; charset=UTF-8',
					'Access-Control-Allow-Origin': '*'
				},
			})
				.then((response) =>
					response.json()
				)
				.then((data) => {

					console.log(data)

					let list = {};

					for (i = 0; i < data.length; i++) {
						list[data[i].activityLabel] = {};
						list[data[i].activityLabel]["input"] = []
						list[data[i].activityLabel]["output"] = []
					}

					for (var prop in list) {
						for (i = 0; i < data.length; i++) {
							if (data[i].activityLabel == prop) {
								let input = data[i].inputLabel + " - " + removeLiteralType(data[i].inputValue) + "" + removeLiteralType(data[i].inputUnitLabel) + " [" + removeLiteralType(data[i].inputQuantityKindL) + "]<hr>"
								list[prop]["input"].push(input);

								let output = data[i].outputLabel + " - " + removeLiteralType(data[i].outputValue) + "" + removeLiteralType(data[i].outputUnitLabel) + " [" + removeLiteralType(data[i].outputQuantityKindL) + "]<hr>"
								if (!list[prop]["output"].includes(output)) {

									list[prop]["output"].push(output);
								}

							}
						}
					}

					let html_string = "";
					for (var prop in list) {

						html_string = html_string + "<tr>"


						html_string = html_string + "<td>" + prop + "</td>"
						html_string = html_string + "<td>";
						for (i = 0; i < list[prop]["input"].length; i++) {
							html_string = html_string + list[prop]["input"][i]
						}
						html_string = html_string + "</td>";

						html_string = html_string + "<td>";
						for (i = 0; i < list[prop]["output"].length; i++) {
							html_string = html_string + list[prop]["output"][i]
						}
						html_string = html_string + "</td>";


						html_string = html_string + "</tr>"



					}
					//console.log(html_string)
					//let table_body = document.getElementById('data_transformations_table_body');
					//table_body.innerHTML = html_string;
					
					 window.parent.postMessage({
        type: 'provenanceTable',
        text: html_string
    }, '*'); 
				}
				)
						 .catch(function(error) {                        // catch
     alert ("The calcualtor could not retrieve the calculation provenance trace. ");
  });
  
  //Print CF table

console.log("Fetching CF ")
			//document.getElementById('score_value').innerHTML = "<br> for " + energy + "kWh"
fetch('/cf_info_all?', {
				method: 'POST',
				mode: "cors", // no-cors, *cors, same-origin
				body: assetIRI
				,
				headers: {
					'Content-type': 'application/json; charset=UTF-8',
					'Access-Control-Allow-Origin': '*'
				},
			}).then((response) =>
			response.json()
		)
		.then((CF_data) => {
			
			console.log(CF_data)

			let html_string = "";

			for (i = 0; i < CF_data.length; i++) {
				if (i == 0) {
					html_string = html_string + '<tr style="background-color:#CCF6D3 ;">'
				}
				else {
					html_string = html_string + "<tr>"
				}

				html_string = html_string + "<td>" + removeLiteralType(CF_data[i].sourceUnit) + "</td>"
				html_string = html_string + "<td>" + removeLiteralType(CF_data[i].targetUnit) + "</td>"

				html_string = html_string + "<td>" + removeLiteralType(CF_data[i].applicablePeriodStart) + "</td>"
				html_string = html_string + "<td>" + removeLiteralType(CF_data[i].applicablePeriodEnd) + "</td>"
				html_string = html_string + "<td>" + removeLiteralType(CF_data[i].applicableLocation) + "</td>"



				html_string = html_string + "<td>" + removeLiteralType(CF_data[i].value) + "</td>"


                html_string = html_string + "<td><a href=\"" + CF_data[i].id + "\">link</a></td>"
				
				if (CF_data[i].source) {
				html_string = html_string + "<td><a href=\"" + CF_data[i].source + "\">link</a></td>"
				} else {
					html_string = html_string + "<td>no value</td>"
				}
				
				//html_string = html_string + "<td>" + twoDigits(energy * twoDigits(CF_data[i].value.split("^")[0])) + "" + removeLiteralType(CF_data[i].targetUnit) + " ["+removeLiteralType(CF_data[i].emissionTargetSymbol)+"]</td>"
				html_string = html_string + "</tr>"



			}
			console.log(html_string)
			//let table_body = document.getElementById('cf_table_body');
			//table_body.innerHTML = html_string;

 window.parent.postMessage({
        type: 'provenanceTableCF',
        text: html_string
    }, '*'); 

		
		fetch('/getEmissionResults?assetIri='+encodeURIComponent(assetIRI)).then((response) =>
			response.json()
		)
		.then((CF_data) => {
			
			console.log(CF_data)

			let html_string = "";

			for (i = 0; i < CF_data.length; i++) {
				if (i == 0) {
					html_string = html_string + '<tr style="background-color:#CCF6D3 ;">'
				}
				else {
					html_string = html_string + "<tr>"
				}

				html_string = html_string + "<td>" + removeLiteralType(CF_data[i].plan) + "</td>"
				html_string = html_string + "<td>" + removeLiteralType(CF_data[i].value) + "</td>"

				html_string = html_string + "<td>" + removeLiteralType(CF_data[i].unit) + " of " + removeLiteralType(CF_data[i].quantityKind) + "</td>"
				
				
				
				//html_string = html_string + "<td>" + twoDigits(energy * twoDigits(CF_data[i].value.split("^")[0])) + "" + removeLiteralType(CF_data[i].targetUnit) + " ["+removeLiteralType(CF_data[i].emissionTargetSymbol)+"]</td>"
				html_string = html_string + "</tr>"


			}
			console.log(html_string)
			//let table_body = document.getElementById('cf_table_body');
			//table_body.innerHTML = html_string;

 window.parent.postMessage({
        type: 'provenanceResults',
        text: html_string
    }, '*'); 
 window.parent.postMessage({
        type: 'endedFootprintCalcualtion',
        text: "true"
    }, '*'); 
});		
		
});
  
  
  
  
}

function highlightSelected(itemId) {
    // Remove active class from all items
    document.querySelectorAll('.list-group-item').forEach(item => {
        item.classList.remove('active');
    });

    // Add active class to the clicked item
    const selectedItem = document.getElementById(itemId);
    if (selectedItem) {
        selectedItem.classList.add('active');
    }
}