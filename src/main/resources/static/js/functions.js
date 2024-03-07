//round to two decimal places
function roundTwoDecimal (num) {
	return Math.round((num + Number.EPSILON) * 100) / 100
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

					// Connect the polygon with the point by drawing a line from one of the polygon's vertices to the point
                    let polylineCoords = [data[0][0], [56.249124, -2.652966]]; // Connecting the first vertex of the polygon with the point
                    L.polyline(polylineCoords, {color: 'red'}).addTo(mapLayer);

					
				}
				)
						 .catch(function(error) { 
							 console.log ("cannot link sources");                       // catch
                console.log (error);
            });
       }