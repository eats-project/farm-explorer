//round to two decimal places
function roundTwoDecimal (num) {
	return Math.round((num + Number.EPSILON) * 100) / 100
}

 function drawItemMarkers(items ) {
	          items.forEach(function (item) {
		
		          //TO DO add handling for multiple agri parcels on farm (currently only the first is selected) update code for handling look up of details 
 	              var coordinates = item['https://smartdatamodels.org/dataModel.Agrifood/hasAgriParcel'][0]['http://www.opengis.net/ont/geosparql#hasGeometry'][0]['http://www.opengis.net/ont/geosparql#asWKT'][0]['@value'].match(/-?\d+\.\d+/g).map(Number);
	              var deviceMarker = L.circleMarker( L.latLng(parseFloat(coordinates[0]),parseFloat(coordinates[1])), {
	                  radius: 8,
	                  color: 'blue',
	                  fillColor: 'red',
	                  fillOpacity: 0.8
	              }).addTo(map);

					let popupContent = ""; 

					if (item['@type'].includes("AgriParcel")) {
						popupContent +=  '<strong>' + item.name + '</strong><br>';
						popupContent +=  '<strong> Description:</strong> ' + item.description +'<br>';
						
						popupContent +=  '<strong> Plant type:</strong> ' + item.hasAgriCrop.name +'<br>';
						popupContent +=  '<strong> Status:</strong> ' + item.cropStatus +'<br>';
						
						popupContent +=  '<strong> Last Planted At:</strong> ' + item.lastPlantedAt +'<br>';
						
						  deviceMarker.on('click', function () {	
								
			                        calculateTunnelFootprint ();
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
       
 function calculateTunnelFootprint () {
	
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
	
function pupulateProvenanceTable () {
					// PRINT TRANSFORMATIONS TABLE
			fetch('/getDataTransformations', {
				method: 'POST',
				mode: "cors", // no-cors, *cors, same-origin
				body: generateJsonLDstring(graphLD)
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
					console.log(html_string)
					let table_body = document.getElementById('data_transformations_table_body');
					table_body.innerHTML = html_string;
				}
				)
						 .catch(function(error) {                        // catch
     alert ("The calcualtor could not retrieve the calculation provenance trace. ");
  });
}