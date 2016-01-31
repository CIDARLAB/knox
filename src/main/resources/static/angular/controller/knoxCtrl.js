function knoxCtrl($scope) {

    $scope.graphs = [];
    $scope.isSpaceGraph = true;

    $scope.spaceID = "test1";
    $scope.nodeID = "n1";

	$scope.removeGraphSVG = function(index) {
		d3.select("#graph" + index).select("svg").remove();
	};

    $scope.toggleGraphStyle = function() {
        var i;
        for (i = 0; i < $scope.graphs.length; i++) {
            $scope.removeGraphSVG(i);
            if ($scope.isSpaceGraph) {
                $scope.appendVersionControlGraphSVG($scope.graphs[i].branch, i, 1110, 300);
            } else {
                $scope.appendDesignSpaceGraphSVG($scope.graphs[i].space, i, 1110, 300);
            }
        }
        $scope.isSpaceGraph = !$scope.isSpaceGraph;
    };

	$scope.appendDesignSpaceGraphSVG = function(graph, index, width, height) {
		var force = d3.layout.force()
	            .charge(-250)
                .linkDistance(60)
                .size([width, height]);

        var drag = force.drag()
                .on("dragstart", function (d) {
                    d3.select(this).classed("fixed", d.fixed = true);
                });

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

        force.nodes(graph.nodes).links(graph.links).start();

        var link = svg.selectAll(".link")
                .data(graph.links).enter()
                .append("path").attr("class", "link");

       	var componentLinks = [];
        for (i = 0; i < graph.links.length; i++) {
        	if (graph.links[i].componentRoles) {
        		componentLinks.push(graph.links[i]);
        	}
        }

        var icon = svg.append("g").selectAll("g")
                .data(componentLinks).enter().append("g");

        icon.append("image").attr("xlink:href", function (d) {
                    return "image/" + d.componentRoles[0] + ".png";
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
                .on("dblclick", function (d) {
                    d3.select(this).classed("fixed", d.fixed = false);
                })
                .call(drag);

        // html title attribute
        node.append("title")
                .text(function (d) { return d.nodeID; });

        // force feed algo ticks
        force.on("tick", function() {

            link.attr('d', function(d) {
                var deltaX = d.target.x - d.source.x,
                    deltaY = d.target.y - d.source.y,
                    dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY),
                    normX = deltaX / dist,
                    normY = deltaY / dist,
                    sourcePadding = 12,
                    targetPadding = 12,
                    sourceX = d.source.x + normX*sourcePadding,
                    sourceY = d.source.y + normY*sourcePadding,
                    targetX = d.target.x - normX*targetPadding,
                    targetY = d.target.y - normY*targetPadding;
                return 'M' + sourceX + ',' + sourceY + 'L' + targetX + ',' + targetY;
            });

            icon.attr("transform", function(d) {
                return "translate(" + (d.target.x + d.source.x)/2 + "," + (d.target.y + d.source.y)/2 + ")";
            });

            node.attr("cx", function(d) { return d.x; })
                    .attr("cy", function(d) { return d.y; });

        });
	};

    $scope.appendVersionControlGraphSVG = function(graph, index, width, height) {
        var force = d3.layout.force()
                .charge(-250).linkDistance(60).size([width, height]);

        var drag = force.drag()
                .on("dragstart", function (d) {
                    d3.select(this).classed("fixed", d.fixed = true);
                });

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

        force.nodes(graph.nodes).links(graph.links).start();

        var link = svg.selectAll(".link")
                .data(graph.links).enter()
                .append("path").attr("class", "link");

        var node = svg.selectAll(".node")
                .data(graph.nodes).enter()
                .append("rect")
                .attr("class", function (d) {
                    return "node " + d.knoxClass;
                })
                .attr("width", 60)
                .attr("height", 20)
                .on("dblclick", function (d) {
                    d3.select(this).classed("fixed", d.fixed = false);
                })
                .call(drag);

        var text = svg.selectAll("text.label")
                .data(graph.nodes).enter()
                .append("text")
                .attr("class", "label")
                .attr("text-anchor", "middle")
                .attr("fill", "black")
                .text(function(d) { return d.knoxID; });

        // force feed algo ticks
        force.on("tick", function() {

            link.attr('d', function(d) {
                var deltaX = d.target.x - d.source.x,
                    deltaY = d.target.y - d.source.y,
                    dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY),
                    normX = deltaX / dist,
                    normY = deltaY / dist,
                    xPadding = 33,
                    yPadding = 13,
                    sourceX = d.source.x + normX*xPadding,
                    sourceY = d.source.y + normY*yPadding,
                    targetX = d.target.x - normX*xPadding,
                    targetY = d.target.y - normY*yPadding;
                return 'M' + sourceX + ',' + sourceY + 'L' + targetX + ',' + targetY;
            });

            node.attr("x", function(d) { return d.x - 30; })
                    .attr("y", function(d) { return d.y - 10 });

            text.attr("transform", function(d) {
                return "translate(" + d.x + "," + (d.y + 3) + ")";
            });

        });
    };

	$scope.graphDesignSpace = function(targetID) {
		if (targetID) {
            var query = "?targetSpaceID=" + encodeURIComponent(targetID);

            d3.json("/designSpace/graph/d3" + query, function(error, spaceGraph) {
                if (!error && spaceGraph.spaceID) {
                    d3.json("/branch/graph/d3" + query, function(error, branchGraph) {
                        if (!error && branchGraph.spaceID) {
                            var i;
                            for (i = 0; i < $scope.graphs.length; i++) {
                                $scope.removeGraphSVG(i);
                            }

                            $scope.graphs.unshift({spaceID: spaceGraph.spaceID, space: spaceGraph, branch: branchGraph});
                            $scope.graphs = $scope.graphs.slice(0, 2);

                            for (i = 0; i < $scope.graphs.length; i++) {
                                if ($scope.isSpaceGraph) {
                                    $scope.appendDesignSpaceGraphSVG($scope.graphs[i].space, i, 1110, 300);
                                } else {
                                    $scope.appendVersionControlGraphSVG($scope.graphs[i].branch, i, 1110, 300);
                                }
                            }
                         }
                    });
                }
            });
		}
	};

    $scope.deleteDesignSpace = function(targetID) {
        if (targetID) {
            var query = "?targetSpaceID=" + encodeURIComponent(targetID);

            d3.xhr("/designSpace" + query).send("DELETE", function(error, request) {
                if (!error) {

                    if ($scope.graphs.length > 1) {
                        $scope.removeGraphSVG(1);
                    }
                    if ($scope.graphs.length > 0 && $scope.graphs[0].spaceID === targetID) {
                        $scope.removeGraphSVG(0);
                        if ($scope.graphs.length > 1) { 
                            if ($scope.graphs[1].spaceID === targetID) {
                                $scope.graphs = [];
                            } else {
                                if ($scope.isSpaceGraph) {
                                    $scope.appendDesignSpaceGraphSVG($scope.graphs[1].space, 0, 1110, 300);
                                } else {
                                    $scope.appendVersionControlGraphSVG($scope.graphs[1].branch, 0, 1110, 300);
                                }
                                $scope.graphs[0] = $scope.graphs[1];
                                $scope.graphs = $scope.graphs.slice(0, 1);
                            }
                        } else {
                            $scope.graphs = [];
                        }
                    } else if ($scope.graphs.length > 1 && $scope.graphs[1].spaceID === targetID) {
                        $scope.graphs = $scope.graphs.slice(0, 1);
                    }

                }
            });
        }
    };

    $scope.joinDesignSpaces = function(inputID1, inputID2, outputID) {
        if (inputID1 && inputID2 && outputID && outputID !== inputID1 && outputID !== inputID2) {
            var query = "?inputSpaceID1=" + encodeURIComponent(inputID1) + "&inputSpaceID2=" + encodeURIComponent(inputID2) 
                    + "&outputSpaceID=" + encodeURIComponent(outputID);

            d3.xhr("/designSpace/join" + query).post(function(error, request) {
                if (!error) {

                    $scope.graphDesignSpace(outputID);

                }
            });
        }
    };

    $scope.orDesignSpaces = function(inputID1, inputID2, outputID) {
        if (inputID1 && inputID2 && outputID && outputID !== inputID1 && outputID !== inputID2) {
            var query = "?inputSpaceID1=" + encodeURIComponent(inputID1) + "&inputSpaceID2=" + encodeURIComponent(inputID2) 
                    + "&outputSpaceID=" + encodeURIComponent(outputID);

            d3.xhr("/designSpace/or" + query).post(function(error, request) {
                if (!error) {

                    $scope.graphDesignSpace(outputID);

                }
            });
        }
    };

    $scope.andDesignSpaces = function(inputID1, inputID2, outputID) {
        if (inputID1 && inputID2 && outputID && outputID !== inputID1 && outputID !== inputID2) {
            var query = "?inputSpaceID1=" + encodeURIComponent(inputID1) + "&inputSpaceID2=" + encodeURIComponent(inputID2) 
                    + "&outputSpaceID=" + encodeURIComponent(outputID);

            d3.xhr("/designSpace/and" + query).post(function(error, request) {
                if (!error) {

                    $scope.graphDesignSpace(outputID);

                }
            });
        }
    };

    $scope.insertDesignSpace = function(inputID1, inputID2, nodeID, outputID) {
        if (inputID1 && inputID2 && nodeID && outputID && outputID !== inputID1 && outputID !== inputID2) {
            var query = "?inputSpaceID1=" + encodeURIComponent(inputID1) + "&inputSpaceID2=" + encodeURIComponent(inputID2) 
                    + "&targetNodeID=" + encodeURIComponent(nodeID) + "&outputSpaceID=" + encodeURIComponent(outputID);

            d3.xhr("/designSpace/insert" + query).post(function(error, request) {
                if (!error) {

                    $scope.graphDesignSpace(outputID);

                }
            });
        }
    };

}