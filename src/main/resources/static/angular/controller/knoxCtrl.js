function knoxCtrl($scope) {

    $scope.spaceGraphs = [];
    $scope.branchGraphs = [];
    $scope.isSpaceGraph = true;

    $scope.spaceID = "test1";
    $scope.nodeID = "n1";

	$scope.removeGraphSVG = function(index) {
		d3.select("#graph" + index).select("svg").remove();
	};

    $scope.toggleGraphStyle = function() {
        var i;
        for (i = 0; i < $scope.spaceGraphs.length; i++) {
            $scope.removeGraphSVG(i);
            if ($scope.isSpaceGraph) {
                $scope.populateVersionControlGraph($scope.spaceGraphs[i], i, 1110, 300);
            } else {
                $scope.appendDesignSpaceGraphSVG($scope.spaceGraphs[i], i, 1110, 300);
            }
        }
        $scope.isSpaceGraph = !$scope.isSpaceGraph;
    };

	$scope.appendDesignSpaceGraphSVG = function(graph, index, width, height) {
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

    $scope.populateVersionControlGraph = function(graph, index, width, height) {
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
            if (graph.nodes[i].knoxClass === "Head" && !graph.nodes[i].fixed) {
                graph.nodes[i].x = width/3;
                graph.nodes[i].y = height/2;
                graph.nodes[i].fixed = true;
            }
        }

        force.nodes(graph.nodes).links(graph.links).start();

        var link = svg.selectAll(".link")
                .data(graph.links).enter()
                .append("path").attr("class", "link");

        var node = svg.selectAll(".node")
                .data(graph.nodes).enter()
                .append("square")
                .attr("class", function (d) {
                    return "node " + d.knoxClass;
                })
                .attr("r", 10)
                .call(force.drag);

        // html title attribute
        node.append("title")
                .text(function (d) { return d.knoxID; })

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

            node.attr("cx", function(d) { return d.x; })
                    .attr("cy", function(d) { return d.y; });

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
                            for (i = 0; i < $scope.spaceGraphs.length; i++) {
                                $scope.removeGraphSVG(i);
                            }

                            $scope.spaceGraphs.unshift(spaceGraph);
                            $scope.spaceGraphs = $scope.spaceGraphs.slice(0, 2);

                            $scope.branchGraphs.unshift(branchGraph);
                            $scope.branchGraphs = $scope.spaceGraphs.slice(0, 2);

                            for (i = 0; i < $scope.spaceGraphs.length; i++) {
                                if ($scope.isSpaceGraph) {
                                    $scope.appendDesignSpaceGraphSVG($scope.spaceGraphs[i], i, 1110, 300);
                                } else {
                                    $scope.populateVersionControlGraph($scope.branchGraphs[i], i, 1110, 300);
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

                    if ($scope.spaceGraphs.length > 1) {
                        $scope.removeGraphSVG(1);
                    }
                    if ($scope.spaceGraphs.length > 0 && $scope.spaceGraphs[0].spaceID === targetID) {
                        $scope.removeGraphSVG(0);
                        if ($scope.spaceGraphs.length > 1) { 
                            if ($scope.spaceGraphs[1].spaceID === targetID) {
                                $scope.spaceGraphs = [];
                            } else {
                                $scope.appendDesignSpaceGraphSVG($scope.spaceGraphs[1], 0, 1110, 300);
                                $scope.spaceGraphs[0] = $scope.spaceGraphs[1];
                                $scope.spaceGraphs = $scope.spaceGraphs.slice(0, 1);
                            }
                        } else {
                            $scope.spaceGraphs = [];
                        }
                    } else if ($scope.spaceGraphs.length > 1 && $scope.spaceGraphs[1].spaceID === targetID) {
                        $scope.spaceGraphs = $scope.spaceGraphs.slice(0, 1);
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