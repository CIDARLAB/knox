function knoxCtrl($scope) {

	$scope.clearGraph = function(index) {
		d3.select("#graph" + index).select("svg").remove();
	};

	$scope.populateGraph = function(graph, index, width, height) {
		var force = d3.layout.force()
	            .charge(-250).linkDistance(60).size([width, height]);

	    var svg = d3.select("#graph" + index).append("svg")
	            .attr("width", "100%").attr("height", "50%")
	            .attr("pointer-events", "all");

	    svg.append('defs').append('marker')
	            .attr('id', 'endArrow')
	            .attr('viewBox', '0 -5 10 10')
	            .attr('refX', 6)
	            .attr('markerWidth', 6)
	            .attr('markerHeight', 6)
	            .attr('orient', 'auto')
	        .append('path')
	            .attr('d', 'M0,-5L10,0L0,5')
	            .attr('fill', '#000');

		var i;
        for (i = 0; i < graph.nodes.length; i++) {
        	if (graph.nodes[i].nodeType === "start" && !graph.nodes[i].fixed) {
        		graph.nodes[i].x = width/3;
		        graph.nodes[i].y = height/2;
		        graph.nodes[i].fixed = true;
        	}
        }

        force.nodes(graph.nodes).links(graph.links).start();

        var link = svg.selectAll(".link")
                .data(graph.links).enter()
                .append("path").attr("class", "link");

       	var componentLinks = [];
        for (i = 0; i < graph.links.length; i++) {
        	if (graph.links[i].componentRole) {
        		componentLinks.push(graph.links[i]);
        	}
        }

        var icon = svg.append("g").selectAll("g")
                .data(componentLinks).enter().append("g");

        icon.append("image").attr("xlink:href", function (d) {
                    return "image/" + d.componentRole + ".png";
                })
                .attr("x", -15)
                .attr("y", -15)
                .attr("width", 30).attr("height", 30)
                .attr("class", "type-icon");

        var node = svg.selectAll(".node")
                .data(graph.nodes).enter()
                .append("circle")
                .attr("class", function (d) {
                    return "node " + ((d.nodeType) ? d.nodeType:"inner");
                })
                .attr("r", 10)
                .call(force.drag);

        // html title attribute
        node.append("title")
                .text(function (d) { return d.nodeID; })

        // force feed algo ticks
        force.on("tick", function() {

            link.attr('d', function(d) {
                var deltaX = d.target.x - d.source.x,
                    deltaY = d.target.y - d.source.y,
                    dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY),
                    normX = deltaX / dist,
                    normY = deltaY / dist,
                    sourcePadding = d.left ? 17 : 12,
                    targetPadding = d.right ? 17 : 12,
                    sourceX = d.source.x + (sourcePadding * normX),
                    sourceY = d.source.y + (sourcePadding * normY),
                    targetX = d.target.x - (targetPadding * normX),
                    targetY = d.target.y - (targetPadding * normY);
                return 'M' + sourceX + ',' + sourceY + 'L' + targetX + ',' + targetY;
            });

            icon.attr("transform", function(d) {
                return "translate(" + (d.target.x + d.source.x)/2 + "," + (d.target.y + d.source.y)/2 + ")";
            });

            node.attr("cx", function(d) { return d.x; })
                    .attr("cy", function(d) { return d.y; });

        });
	};

	$scope.graphs = [];

	$scope.targetID = "test1";
    $scope.targetNodeID = "";

	$scope.graphDesignSpace = function(targetID) {
		if (targetID) {
            var query = "?targetID=" + encodeURIComponent(targetID);

            d3.json("/d3GraphDesignSpace" + query, function(error, graph) {
                if (error) return;

                if (graph.spaceID) {
                    var i;
                    for (i = 0; i < $scope.graphs.length; i++) {
                        $scope.clearGraph(i);
                    }

                    $scope.graphs.unshift(graph);
                    $scope.graphs = $scope.graphs.slice(0, 2);

                    for (i = 0; i < $scope.graphs.length; i++) {
                        $scope.populateGraph($scope.graphs[i], i, 1110, 300);
                    }
                }
            });
		}
	};

    $scope.deleteDesignSpace = function(targetID) {
        if (targetID) {
            var query = "?targetID=" + encodeURIComponent(targetID);

            d3.json("/deleteDesignSpace" + query, function(error, target) {
                if (error) return;

                if (target.spaceID) {
                    $scope.clearGraph(1);
                    if ($scope.graphs[0].spaceID === target.spaceID) {
                        $scope.clearGraph(0);
                        if ($scope.graphs[1].spaceID === target.spaceID) {
                            $scope.graphs = [];
                        } else {
                            $scope.populateGraph($scope.graphs[1], 0, 1110, 300);
                            $scope.graphs[0] = $scope.graphs[1];
                            $scope.graphs = $scope.graphs.slice(0, 1);
                        }
                    } else if ($scope.graphs[1].spaceID === target.spaceID) {
                        $scope.graphs = $scope.graphs.slice(0, 1);
                    }
                }
            });
        }
    };

    $scope.joinDesignSpaces = function(inputID1, inputID2, outputID) {
        if (inputID1 && inputID2 && outputID && outputID !== inputID1 && outputID !== inputID2) {
            var query = "?inputID1=" + encodeURIComponent(inputID1) + "&inputID2=" + encodeURIComponent(inputID2) 
                    + "&outputID=" + encodeURIComponent(outputID);

            d3.json("/joinDesignSpaces" + query, function(error, output) {
                if (error) return;

                if (output.spaceID) {
                    $scope.graphDesignSpace(output.spaceID);
                }
            });
        }
    };

    $scope.orDesignSpaces = function(inputID1, inputID2, outputID) {
        if (inputID1 && inputID2 && outputID && outputID !== inputID1 && outputID !== inputID2) {
            var query = "?inputID1=" + encodeURIComponent(inputID1) + "&inputID2=" + encodeURIComponent(inputID2) 
                    + "&outputID=" + encodeURIComponent(outputID);

            d3.json("/orDesignSpaces" + query, function(error, output) {
                if (error) return;

                if (output.spaceID) {
                    $scope.graphDesignSpace(output.spaceID);
                }
            });
        }
    };

    $scope.andDesignSpaces = function(inputID1, inputID2, outputID) {
        if (inputID1 && inputID2 && outputID && outputID !== inputID1 && outputID !== inputID2) {
            var query = "?inputID1=" + encodeURIComponent(inputID1) + "&inputID2=" + encodeURIComponent(inputID2) 
                    + "&outputID=" + encodeURIComponent(outputID);

            d3.json("/andDesignSpaces" + query, function(error, output) {
                if (error) return;

                if (output.spaceID) {
                    $scope.graphDesignSpace(output.spaceID);
                }
            });
        }
    };

    $scope.insertDesignSpace = function(inputID, targetNodeID, outputID) {
        if (inputID && targetNodeID && outputID && outputID !== inputID) {
            var query = "?inputID=" + encodeURIComponent(inputID) + "&targetNodeID=" + encodeURIComponent(targetNodeID) 
                    + "&outputID=" + encodeURIComponent(outputID);

            d3.json("/insertDesignSpace" + query, function(error, graph) {
                if (error) return;

                if (graph.spaceID) {
                    $scope.graphDesignSpace(graph.spaceID);
                }
            });
        }
    };

}