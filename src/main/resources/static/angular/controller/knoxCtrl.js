function knoxCtrl($scope) {

    $scope.graphs = [];
    $scope.graphType = "ds";
    $scope.isDSGraph = true;

    $scope.spaceID = "";
    $scope.nodeID1 = "";
    $scope.nodeID2 = "";
    $scope.branchID = "";

	$scope.removeGraphSVG = function(index) {
		d3.select("#graph" + index).select("svg").remove();
	};

    $scope.switchToGraphType = function(graphType) {
        var i;
        for (i = 0; i < $scope.graphs.length; i++) {
            $scope.removeGraphSVG(i);
            if (graphType === "ds") {
                $scope.appendDSGraphSVG($scope.graphs[i].ds, i, 1110, 300);
                $scope.isDSGraph = true;
            } else {
                $scope.appendVCGraphSVG($scope.graphs[i].vc, i, 1110, 300);
                $scope.isDSGraph = false;
            }
        }
    };

	$scope.appendDSGraphSVG = function(graph, index, width, height) {
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

    $scope.appendVCGraphSVG = function(graph, index, width, height) {
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
                var yPadding = 12,
                    sourceX = d.source.x,
                    sourceY = d.source.y + yPadding,
                    targetX = d.target.x,
                    targetY = d.target.y - yPadding;
                return 'M' + sourceX + ',' + sourceY + 'L' + targetX + ',' + targetY;
            });

            node.attr("x", function(d) { return d.x - 30; })
                    .attr("y", function(d) { return d.y - 10 });

            text.attr("transform", function(d) {
                return "translate(" + d.x + "," + (d.y + 3) + ")";
            });

        });
    };

    $scope.graphDesignSpace = function(targetSpaceID) {
        if (targetSpaceID) {
            var query = "?targetSpaceID=" + encodeURIComponent(targetSpaceID);

            d3.json("/designSpace/graph/d3" + query, function(error, dsGraph) {
                if (error) {

                    sweetAlert("Error", error.responseText, "error");

                } else if (dsGraph.spaceID) {
                    d3.json("/branch/graph/d3" + query, function(error, vcGraph) {
                        if (error) {

                            sweetAlert("Error", error.responseText, "error");

                        } else if (vcGraph.spaceID) {
                            var targetI = -1;
                            var i;
                            for (i = 0; i < $scope.graphs.length; i++) {
                                if ($scope.graphs[i].spaceID === targetSpaceID) {
                                    targetI = i;
                                }
                            }

                            if (targetI >= 0) {
                                $scope.removeGraphSVG(targetI);
                                
                                $scope.graphs[targetI] = {spaceID: dsGraph.spaceID, ds: dsGraph, vc: vcGraph};
                               
                                if ($scope.isDSGraph) {
                                    $scope.appendDSGraphSVG($scope.graphs[targetI].ds, targetI, 1110, 300);
                                } else {
                                    $scope.appendVCGraphSVG($scope.graphs[targetI].vc, targetI, 1110, 300);
                                }
                            } else {
                                for (i = 0; i < $scope.graphs.length; i++) {
                                    $scope.removeGraphSVG(i);
                                }

                                $scope.graphs.unshift({spaceID: dsGraph.spaceID, ds: dsGraph, vc: vcGraph});
                                $scope.graphs = $scope.graphs.slice(0, 2);
                               
                                for (i = 0; i < $scope.graphs.length; i++) {
                                    if ($scope.isDSGraph) {
                                        $scope.appendDSGraphSVG($scope.graphs[i].ds, i, 1110, 300);
                                    } else {
                                        $scope.appendVCGraphSVG($scope.graphs[i].vc, i, 1110, 300);
                                    }
                                }
                            }
                         }
                    });
                }
            });
        }
    };

    $scope.joinDesignSpaces = function(inputSpaceID1, inputSpaceID2, outputSpaceID) {
        if (inputSpaceID1 && inputSpaceID2 && outputSpaceID && outputSpaceID !== inputSpaceID1 && outputSpaceID !== inputSpaceID2) {
            var query = "?inputSpaceID1=" + encodeURIComponent(inputSpaceID1) + "&inputSpaceID2=" + encodeURIComponent(inputSpaceID2) 
                    + "&outputSpaceID=" + encodeURIComponent(outputSpaceID);

            d3.xhr("/designSpace/join" + query).post(function(error, request) {
                if (error) {

                    sweetAlert("Error", error.responseText, "error");

                } else {

                    $scope.graphDesignSpace(outputSpaceID);

                }
            });
        }
    };

    $scope.orDesignSpaces = function(inputSpaceID1, inputSpaceID2, outputSpaceID) {
        if (inputSpaceID1 && inputSpaceID2 && outputSpaceID && outputSpaceID !== inputSpaceID1 && outputSpaceID !== inputSpaceID2) {
            var query = "?inputSpaceID1=" + encodeURIComponent(inputSpaceID1) + "&inputSpaceID2=" + encodeURIComponent(inputSpaceID2) 
                    + "&outputSpaceID=" + encodeURIComponent(outputSpaceID);

            d3.xhr("/designSpace/or" + query).post(function(error, request) {
                if (error) {

                    sweetAlert("Error", error.responseText, "error");

                } else {

                    $scope.graphDesignSpace(outputSpaceID);

                }
            });
        }
    };

    $scope.andDesignSpaces = function(inputSpaceID1, inputSpaceID2, outputSpaceID) {
        if (inputSpaceID1 && inputSpaceID2 && outputSpaceID && outputSpaceID !== inputSpaceID1 && outputSpaceID !== inputSpaceID2) {
            var query = "?inputSpaceID1=" + encodeURIComponent(inputSpaceID1) + "&inputSpaceID2=" + encodeURIComponent(inputSpaceID2) 
                    + "&outputSpaceID=" + encodeURIComponent(outputSpaceID);

            d3.xhr("/designSpace/and" + query).post(function(error, request) {
                if (error) {

                    sweetAlert("Error", error.responseText, "error");

                } else {

                    $scope.graphDesignSpace(outputSpaceID);

                }
            });
        }
    };

    $scope.createDesignSpace = function(outputSpaceID) {
        var query = "?outputSpaceID=" + encodeURIComponent(outputSpaceID);

        d3.xhr("/designSpace" + query).post(function(error, request) {
            if (error) {

                sweetAlert("Error", error.responseText, "error");

            } else {

                $scope.graphDesignSpace(outputSpaceID);

            }
        });
    };

    $scope.deleteDesignSpace = function(targetSpaceID) {
        if (targetSpaceID) {
            var query = "?targetSpaceID=" + encodeURIComponent(targetSpaceID);

            d3.xhr("/designSpace" + query).send("DELETE", function(error, request) {
                if (error) {

                    sweetAlert("Error", error.responseText, "error");

                } else {

                    if ($scope.graphs.length > 1) {
                        $scope.removeGraphSVG(1);
                    }
                    if ($scope.graphs.length > 0 && $scope.graphs[0].spaceID === targetSpaceID) {
                        $scope.removeGraphSVG(0);
                        if ($scope.graphs.length > 1) { 
                            if ($scope.graphs[1].spaceID === targetSpaceID) {
                                $scope.graphs = [];
                            } else {
                                if ($scope.isDSGraph) {
                                    $scope.appendDSGraphSVG($scope.graphs[1].ds, 0, 1110, 300);
                                } else {
                                    $scope.appendVCGraphSVG($scope.graphs[1].vc, 0, 1110, 300);
                                }
                                $scope.graphs[0] = $scope.graphs[1];
                                $scope.graphs = $scope.graphs.slice(0, 1);
                            }
                        } else {
                            $scope.graphs = [];
                        }
                    } else if ($scope.graphs.length > 1 && $scope.graphs[1].spaceID === targetSpaceID) {
                        $scope.graphs = $scope.graphs.slice(0, 1);
                    }

                }
            });
        }
    };

    $scope.checkoutBranch = function(targetSpaceID, targetBranchID) {
        if (targetSpaceID && targetBranchID) {
            var query = "?targetSpaceID=" + encodeURIComponent(targetSpaceID) + "&targetBranchID=" + encodeURIComponent(targetBranchID);

            d3.xhr("/branch/checkout" + query).send("PUT", function(error, request) {
                if (error) {

                    sweetAlert("Error", error.responseText, "error");

                } else {

                    $scope.graphDesignSpace(targetSpaceID);

                }
            });
        }
    };

    $scope.commitToHead = function(targetSpaceID) {
        if (targetSpaceID) {
            var query = "?targetSpaceID=" + encodeURIComponent(targetSpaceID);

            d3.xhr("/branch/commitToHead" + query).post(function(error, request) {
                if (error) {

                    sweetAlert("Error", error.responseText, "error");

                } else {

                    $scope.graphDesignSpace(targetSpaceID);

                }
            });
        }
    };

    $scope.createBranch = function(targetSpaceID, outputBranchID) {
        if (targetSpaceID && outputBranchID) {
            var query = "?targetSpaceID=" + encodeURIComponent(targetSpaceID) + "&outputBranchID=" + encodeURIComponent(outputBranchID);

            d3.xhr("/branch" + query).post(function(error, request) {
                if (error) {

                    sweetAlert("Error", error.responseText, "error");

                } else {

                    $scope.graphDesignSpace(targetSpaceID);

                }
            });
        }
    };

    $scope.deleteBranch = function(targetSpaceID, targetBranchID) {
        if (targetSpaceID && targetBranchID) {
            var query = "?targetSpaceID=" + encodeURIComponent(targetSpaceID) + "&targetBranchID=" + encodeURIComponent(targetBranchID);

            d3.xhr("/branch" + query).send("DELETE", function(error, request) {
                if (error) {

                    sweetAlert("Error", error.responseText, "error");

                } else {

                    $scope.graphDesignSpace(targetSpaceID);

                }
            });
        }
    };

    $scope.createNode = function(targetSpaceID) {
        if (targetSpaceID && outputNodeID) {
            var query = "?targetSpaceID=" + encodeURIComponent(targetSpaceID);

            d3.xhr("/node" + query).post(function(error, request) {
                if (error) {

                    sweetAlert("Error", error.responseText, "error");

                } else {

                    $scope.graphDesignSpace(targetSpaceID);

                }
            });
        }
    };

    $scope.createEdge = function(targetSpaceID, targetTailID, targetHeadID) {
        if (targetSpaceID && targetTailID && targetHeadID && targetTailID !== targetHeadID) {
            var query = "?targetSpaceID=" + encodeURIComponent(targetSpaceID) + "&targetTailID=" + encodeURIComponent(targetTailID) 
                    + "&targetHeadID=" + encodeURIComponent(targetHeadID);

            d3.xhr("/edge" + query).post(function(error, request) {
                if (error) {

                    sweetAlert("Error", error.responseText, "error");

                } else {

                    $scope.graphDesignSpace(targetSpaceID);

                }
            });
        }
    };

    $scope.insertDesignSpace = function(inputSpaceID1, inputSpaceID2, targetNodeID, outputSpaceID) {
        if (inputSpaceID1 && inputSpaceID2 && targetNodeID && outputSpaceID && outputSpaceID !== inputSpaceID1 && outputSpaceID !== inputSpaceID2) {
            var query = "?inputSpaceID1=" + encodeURIComponent(inputSpaceID1) + "&inputSpaceID2=" + encodeURIComponent(inputSpaceID2) 
                    + "&targetNodeID=" + encodeURIComponent(targetNodeID) + "&outputSpaceID=" + encodeURIComponent(outputSpaceID);

            d3.xhr("/designSpace/insert" + query).post(function(error, request) {
                if (error) {

                    sweetAlert("Error", error.responseText, "error");

                } else {

                    $scope.graphDesignSpace(outputSpaceID);

                }
            });
        }
    };

}