function knoxCtrl($scope) {

    $scope.graphs = [];
    $scope.graphType = "ds7";
    $scope.prevGraphType = "ds7";
    $scope.isDSGraph = true;
    $scope.isCombinationMode = false;
    $scope.isCreationMode = false;
    $scope.isDeletionMode = false;
    $scope.isQueryMode = true;
    $scope.isSBOLMode = false;
    $scope.isEugeneMode = false;
    $scope.isCSVMode = false;

    $scope.searchSpaceID = "";
    $scope.inputSpaceIDs = "";
    $scope.outputSpaceID = "";
    $scope.insertNodeID1 = "";

    $scope.spaceID = "";
    $scope.nodeID1 = "";
    $scope.nodeID2 = "";
    $scope.compIDs = "";
    $scope.compRoles = "";

    $scope.deleteSpaceID = "";
    $scope.deleteNodeID1 = "";
    $scope.deleteNodeID2 = "";

    $scope.querySpaceIDs = "";
    $scope.queriedSpaceIDs = "";

    $scope.branchID = "";
    $scope.commitPath = "";
    $scope.inputBranchIDs = "";
    $scope.inputBranchID1 = "";
    $scope.inputBranchID2 = "";
    $scope.outputBranchID = "";
    $scope.insertNodeID2 = "";

	$scope.removeGraphSVG = function(index) {
		d3.select("#graph" + index).select("svg").remove();
	};

    $scope.switchToGraphType = function(graphType) {
        if (graphType.slice(0, 2) !== $scope.prevGraphType.slice(0, 2)) {
            var i;
            for (i = 0; i < $scope.graphs.length; i++) {
                $scope.removeGraphSVG(i);
                if (graphType === "vc") {
                    $scope.appendVCGraphSVG($scope.graphs[i].vc, i, 1110, 300);
                } else {
                    $scope.appendDSGraphSVG($scope.graphs[i].ds, i, 1110, 300);
                }
            }
            $scope.prevGraphType = graphType;
        }
        if (graphType === "vc") {
            $scope.isDSGraph = false;
            $scope.isCombinationMode = false;
            $scope.isCreationMode = false;
            $scope.isDeletionMode = false;
            $scope.isSBOLMode = false;
            $scope.isCSVMode = false;
            $scope.isEugeneMode = false;
            $scope.isQueryMode = false;
        } else if (graphType === "ds1") {
            $scope.isDSGraph = true;
            $scope.isCombinationMode = true;
            $scope.isCreationMode = false;
            $scope.isDeletionMode = false;
            $scope.isSBOLMode = false;
            $scope.isCSVMode = false;
            $scope.isEugeneMode = false;
            $scope.isQueryMode = false;
        } else if (graphType === "ds2") {
            $scope.isDSGraph = true;
            $scope.isCombinationMode = false;
            $scope.isCreationMode = true;
            $scope.isDeletionMode = false;
            $scope.isSBOLMode = false;
            $scope.isCSVMode = false;
            $scope.isEugeneMode = false;
            $scope.isQueryMode = false;
        } else if (graphType === "ds3") {
            $scope.isDSGraph = true;
            $scope.isCombinationMode = false;
            $scope.isCreationMode = false;
            $scope.isDeletionMode = true;
            $scope.isSBOLMode = false;
            $scope.isCSVMode = false;
            $scope.isEugeneMode = false;
            $scope.isQueryMode = false;
        } else if (graphType === "ds4") {
            $scope.isDSGraph = true;
            $scope.isCombinationMode = false;
            $scope.isCreationMode = false;
            $scope.isDeletionMode = false;
            $scope.isSBOLMode = true;
            $scope.isCSVMode = false;
            $scope.isEugeneMode = false;
            $scope.isQueryMode = false;
        } else if (graphType === "ds5") {
            $scope.isDSGraph = true;
            $scope.isCombinationMode = false;
            $scope.isCreationMode = false;
            $scope.isDeletionMode = false;
            $scope.isSBOLMode = false;
            $scope.isCSVMode = true;
            $scope.isEugeneMode = false;
            $scope.isQueryMode = false;
        } else if (graphType === "ds6") {
            $scope.isDSGraph = true;
            $scope.isCombinationMode = false;
            $scope.isCreationMode = false;
            $scope.isDeletionMode = false;
            $scope.isSBOLMode = false;
            $scope.isCSVMode = false;
            $scope.isEugeneMode = true;
            $scope.isQueryMode = false;
        } else if (graphType === "ds7") {
            $scope.isDSGraph = true;
            $scope.isCombinationMode = false;
            $scope.isCreationMode = false;
            $scope.isDeletionMode = false;
            $scope.isSBOLMode = false;
            $scope.isCSVMode = false;
            $scope.isEugeneMode = false;
            $scope.isQueryMode = true;
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
        	if (graph.links[i].componentRoles && graph.links[i].componentRoles.length > 0) {
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

    $scope.joinDesignSpaces = function(inputSpaceIDs, outputSpaceID) {
        var query = "?";

        if (inputSpaceIDs) {
            inputSpaceIDs = inputSpaceIDs.split(",");
            
            query += $scope.encodeQueryParameter("inputSpaceIDs", inputSpaceIDs, query);
        }

        if (outputSpaceID) {
            query += $scope.encodeQueryParameter("outputSpaceID", outputSpaceID, query);
        }

        d3.xhr("/designSpace/join" + query).post(function(error, request) {
            if (error) {
                sweetAlert("Error", JSON.parse(error.response).message, "error");
            } else if (!outputSpaceID) {
                $scope.graphDesignSpace(inputSpaceIDs[0]);
            } else {
                $scope.graphDesignSpace(outputSpaceID);
            }
        });
    };

    $scope.orDesignSpaces = function(inputSpaceIDs, outputSpaceID) {
        var query = "?";

        if (inputSpaceIDs) {
            inputSpaceIDs = inputSpaceIDs.split(",");
            
            query += $scope.encodeQueryParameter("inputSpaceIDs", inputSpaceIDs, query);
        }

        if (outputSpaceID) {
            query += $scope.encodeQueryParameter("outputSpaceID", outputSpaceID, query);
        }

        d3.xhr("/designSpace/or" + query).post(function(error, request) {
            if (error) {
                sweetAlert("Error", JSON.parse(error.response).message, "error");
            } else if (!outputSpaceID) {
                $scope.graphDesignSpace(inputSpaceIDs[0]);
            } else {
                $scope.graphDesignSpace(outputSpaceID);
            }
        });
    };

    $scope.andDesignSpaces = function(inputSpaceIDs, outputSpaceID) {
        var query = "?";

        if (inputSpaceIDs) {
            inputSpaceIDs = inputSpaceIDs.split(",");
            
            query += $scope.encodeQueryParameter("inputSpaceIDs", inputSpaceIDs, query);
        }

        if (outputSpaceID) {
            query += $scope.encodeQueryParameter("outputSpaceID", outputSpaceID, query);
        }

        d3.xhr("/designSpace/and" + query).post(function(error, request) {
            if (error) {
                sweetAlert("Error", error.responseText, "error");
            } else if (!outputSpaceID) {
                $scope.graphDesignSpace(inputSpaceIDs[0]);
            } else {
                $scope.graphDesignSpace(outputSpaceID);
            }
        });
    };

    $scope.matchDesignSpaces = function(querySpaceIDs, queriedSpaceIDs) {
        var query = "?";

        if (querySpaceIDs) {
            query += $scope.encodeQueryParameter("querySpaceIDs", querySpaceIDs.split(","), query);
        }

        if (queriedSpaceIDs) {
            query += $scope.encodeQueryParameter("queriedSpaceIDs", queriedSpaceIDs.split(","), query);
        }

        d3.json("/designSpace/match" + query, function(error, matches) {
            if (error) {
                sweetAlert("Error", JSON.parse(error.response).message, "error");
            } else {
                console.log(JSON.stringify(matches));
            }
        });
    };

    $scope.partitionDesignSpace = function(inputSpaceIDs, outputSpacePrefix) {
        var query = "?";

        if (inputSpaceIDs) {
            inputSpaceIDs = inputSpaceIDs.split(",");

            if (inputSpaceIDs.length > 0) {
                query += $scope.encodeQueryParameter("inputSpaceID", inputSpaceIDs[0], query);
            }
        }

        if (outputSpacePrefix) {
            query += $scope.encodeQueryParameter("outputSpacePrefix", outputSpacePrefix, query);
        }

        d3.xhr("/designSpace/partition" + query).post(function(error, request) {
            if (error) {
                sweetAlert("Error", JSON.parse(error.response).message, "error");
            } else if (!outputSpaceID) {
                $scope.graphDesignSpace(outputSpacePrefix + "0");
            } else {
                $scope.graphDesignSpace(outputSpaceID);
            }
        });
    };

    $scope.mergeDesignSpaces = function(inputSpaceIDs, outputSpaceID, strength) {
        var query = "?";

        if (inputSpaceIDs) {
            query += $scope.encodeQueryParameter("inputSpaceIDs", inputSpaceIDs.split(","), query);
        }

        if (outputSpaceID) {
            query += $scope.encodeQueryParameter("outputSpaceID", outputSpaceID, query);
        }

        if (strength) {
            query += $scope.encodeQueryParameter("strength", strength, query);
        }

        d3.xhr("/designSpace/merge" + query).post(function(error, request) {
            if (error) {
                sweetAlert("Error", JSON.parse(error.response).message, "error");
            } else if (!outputSpaceID) {
                $scope.graphDesignSpace(inputSpaceIDs[0]);
            } else {
                $scope.graphDesignSpace(outputSpaceID);
            }
        });
    };

    $scope.unionDesignSpaces = function(inputSpaceIDs, outputSpaceID) {
        var query = "?";

        if (inputSpaceIDs) {
            inputSpaceIDs = inputSpaceIDs.split(",");
            
            query += $scope.encodeQueryParameter("inputSpaceIDs", inputSpaceIDs, query);
        }

        if (outputSpaceID) {
            query += $scope.encodeQueryParameter("outputSpaceID", outputSpaceID, query);
        }

        d3.xhr("/designSpace/union" + query).post(function(error, request) {
            if (error) {
                sweetAlert("Error", error.responseText, "error");
            } else if (!outputSpaceID) {
                $scope.graphDesignSpace(inputSpaceIDs[0]);
            } else {
                $scope.graphDesignSpace(outputSpaceID);
            }
        });
    };

    $scope.minimizeDesignSpace = function(targetSpaceID) {
        var query = "?";

        if (targetSpaceID) {
            query += $scope.encodeQueryParameter("targetSpaceID", targetSpaceID, query);
        }

        d3.xhr("/designSpace/minimize" + query).post(function(error, request) {
            if (error) {
                sweetAlert("Error", JSON.parse(error.response).message, "error");
            } else {
                $scope.graphDesignSpace(targetSpaceID);
            }
        });
    };

    $scope.createDesignSpace = function(outputSpaceID, compIDs, compRoles) {
        var query = "?";

        if (outputSpaceID) {
            query += $scope.encodeQueryParameter("outputSpaceID", outputSpaceID, query);
        }

        if (compIDs && compRoles) {
            query += $scope.encodeQueryParameter("componentIDs", compIDs.split(","), query);
            query += $scope.encodeQueryParameter("componentRoles", compRoles.split(","), query);
        }

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

    $scope.createNode = function(targetSpaceID) {
        if (targetSpaceID) {
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

    $scope.createEdge = function(targetSpaceID, targetTailID, targetHeadID, compIDs, compRoles) {
        var query = "?";

        if (targetSpaceID) {
            query += $scope.encodeQueryParameter("targetSpaceID", targetSpaceID, query);
        }

        if (targetTailID) {
            query += $scope.encodeQueryParameter("targetTailID", targetTailID, query);
        }

        if (targetHeadID) {
            query += $scope.encodeQueryParameter("targetHeadID", targetHeadID, query);
        }

        if (compIDs && compRoles) {
            query += $scope.encodeQueryParameter("componentIDs", compIDs.split(","), query);
            query += $scope.encodeQueryParameter("componentRoles", compRoles.split(","), query);
        }

        d3.xhr("/edge" + query).post(function(error, request) {
            if (error) {
                sweetAlert("Error", error.responseText, "error");
            } else {
                $scope.graphDesignSpace(targetSpaceID);
            }
        });
    };

    $scope.deleteEdge = function(targetSpaceID, targetTailID, targetHeadID) {
        var query = "?";

        if (targetSpaceID) {
            query += $scope.encodeQueryParameter("targetSpaceID", targetSpaceID, query);
        }

        if (targetTailID) {
            query += $scope.encodeQueryParameter("targetTailID", targetTailID, query);
        }

        if (targetHeadID) {
            query += $scope.encodeQueryParameter("targetHeadID", targetHeadID, query);
        }

        d3.xhr("/edge" + query).send("DELETE", function(error, request) {
            if (error) {
                sweetAlert("Error", error.responseText, "error");
            } else {
                $scope.graphDesignSpace(targetSpaceID);
            }
        });
    };

    $scope.deleteNode = function(targetSpaceID, targetNodeID) {
        var query = "?";

        if (targetSpaceID) {
            query += $scope.encodeQueryParameter("targetSpaceID", targetSpaceID, query);
        }

        if (targetNodeID) {
            query += $scope.encodeQueryParameter("targetNodeID", targetNodeID, query);
        }

        d3.xhr("/node" + query).send("DELETE", function(error, request) {
            if (error) {
                sweetAlert("Error", error.responseText, "error");
            } else {
                $scope.graphDesignSpace(targetSpaceID);
            }
        });
    };

    $scope.insertDesignSpace = function(inputSpaceIDs, targetNodeID, outputSpaceID) {
        var query = "?";

        if (inputSpaceIDs.length > 0) {
            query += $scope.encodeQueryParameter("inputSpaceID1", inputSpaceIDs[0], query);
        }

        if (inputSpaceIDs.length > 1) {
            query += $scope.encodeQueryParameter("inputSpaceID2", inputSpaceIDs[1], query);
        }

        if (targetNodeID) {
            query += $scope.encodeQueryParameter("targetNodeID", targetNodeID, query);
        }

        if (outputSpaceID) {
            query += $scope.encodeQueryParameter("outputSpaceID", outputSpaceID, query);
        }

        d3.xhr("/designSpace/insert" + query).post(function(error, request) {
            if (error) {
                sweetAlert("Error", JSON.parse(error.response).message, "error");
            } else if (!outputSpaceID) {
                $scope.graphDesignSpace(inputSpaceIDs[0]);
            } else {
                $scope.graphDesignSpace(outputSpaceID);
            }
        });
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

    $scope.commitToBranch = function(targetSpaceID, targetBranchID) {
        var query = "?";

        if (targetSpaceID) {
            query += $scope.encodeQueryParameter("targetSpaceID", targetSpaceID, query);
        }

        if (targetBranchID) {
            query += $scope.encodeQueryParameter("targetBranchID", targetBranchID, query);
        }
        
        d3.xhr("/branch/commitTo" + query).post(function(error, request) {
            if (error) {
                sweetAlert("Error", error.responseText, "error");
            } else {
                $scope.graphDesignSpace(targetSpaceID);
            }
        });
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

    $scope.revertBranch = function(targetSpaceID, targetBranchID, commitPath) {
        var query = "?";

        if (targetSpaceID) {
            query += $scope.encodeQueryParameter("targetSpaceID", targetSpaceID, query);
        }

        if (targetBranchID) {
            query += $scope.encodeQueryParameter("targetBranchID", targetBranchID, query);
        }

        if (commitPath) {
            commitPath = commitPath.split(",");

            query += $scope.encodeQueryParameter("commitPath", commitPath, query);
        }

        d3.xhr("/branch/revert" + query).post(function(error, request) {
            if (error) {
                sweetAlert("Error", JSON.parse(error.response).message, "error");
            } else {
                $scope.graphDesignSpace(targetSpaceID);
            }
        });
    };

    $scope.resetBranch = function(targetSpaceID, targetBranchID, commitPath) {
        var query = "?";

        if (targetSpaceID) {
            query += $scope.encodeQueryParameter("targetSpaceID", targetSpaceID, query);
        }

        if (targetBranchID) {
            query += $scope.encodeQueryParameter("targetBranchID", targetBranchID, query);
        }

        if (commitPath) {
            commitPath = commitPath.split(",");

            query += $scope.encodeQueryParameter("commitPath", commitPath, query);
        }

        d3.xhr("/branch/reset" + query).post(function(error, request) {
            if (error) {
                sweetAlert("Error", JSON.parse(error.response).message, "error");
            } else {
                $scope.graphDesignSpace(targetSpaceID);
            }
        });
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

    $scope.insertBranch = function(targetSpaceID, inputBranchID1, inputBranchID2, targetNodeID, outputBranchID) {
        var query = "?";

        if (targetSpaceID) {
            query += $scope.encodeQueryParameter("targetSpaceID", targetSpaceID, query);
        }

        if (inputBranchID1) {
            query += $scope.encodeQueryParameter("inputBranchID1", inputBranchID1, query);
        }

        if (inputBranchID2) {
            query += $scope.encodeQueryParameter("inputBranchID2", inputBranchID2, query);
        }

        if (targetNodeID) {
            query += $scope.encodeQueryParameter("targetNodeID", targetNodeID, query);
        }

        if (outputBranchID) {
            query += $scope.encodeQueryParameter("outputBranchID", outputBranchID, query);
        }

        d3.xhr("/branch/insert" + query).post(function(error, request) {
            if (error) {
                sweetAlert("Error", JSON.parse(error.response).message, "error");
            }  else {
                $scope.graphDesignSpace(targetSpaceID);
            }
        });
    };

    $scope.joinBranches = function(targetSpaceID, inputBranchIDs, outputBranchID) {
        var query = "?";

        if (targetSpaceID) {
            query += $scope.encodeQueryParameter("targetSpaceID", targetSpaceID, query);
        }

        if (inputBranchIDs) {
            query += $scope.encodeQueryParameter("inputBranchIDs", inputBranchIDs.split(","), query);
        }

        if (outputBranchID) {
            query += $scope.encodeQueryParameter("outputBranchID", outputBranchID, query);
        }

        d3.xhr("/branch/join" + query).post(function(error, request) {
            if (error) {
                sweetAlert("Error", JSON.parse(error.response).message, "error");
            }  else {
                $scope.graphDesignSpace(targetSpaceID);
            }
        });
    };

    $scope.orBranches = function(targetSpaceID, inputBranchIDs, outputBranchID) {
        var query = "?";

        if (targetSpaceID) {
            query += $scope.encodeQueryParameter("targetSpaceID", targetSpaceID, query);
        }

        if (inputBranchIDs) {
            query += $scope.encodeQueryParameter("inputBranchIDs", inputBranchIDs.split(","), query);
        }

        if (outputBranchID) {
            query += $scope.encodeQueryParameter("outputBranchID", outputBranchID, query);
        }

        d3.xhr("/branch/or" + query).post(function(error, request) {
            if (error) {
                sweetAlert("Error", JSON.parse(error.response).message, "error");
            } else {
                $scope.graphDesignSpace(targetSpaceID);
            }
        });
    };

    $scope.andBranches = function(targetSpaceID, inputBranchID1, inputBranchID2, outputBranchID) {
        var query = "?";

        if (targetSpaceID) {
            query += $scope.encodeQueryParameter("targetSpaceID", targetSpaceID, query);
        }

        if (inputBranchID1 && inputBranchID2) {
            query += $scope.encodeQueryParameter("inputBranchIDs", [inputBranchID1, inputBranchID2], query);
        }

        if (outputBranchID) {
            query += $scope.encodeQueryParameter("outputBranchID", outputBranchID, query);
        }

        d3.xhr("/branch/and" + query).post(function(error, request) {
            if (error) {
                sweetAlert("Error", error.responseText, "error");
            } else {
                $scope.graphDesignSpace(targetSpaceID);
            }
        });
    };

    $scope.mergeBranches = function(targetSpaceID, inputBranchID1, inputBranchID2, outputBranchID) {
        var query = "?";

        if (targetSpaceID) {
            query += $scope.encodeQueryParameter("targetSpaceID", targetSpaceID, query);
        }

        if (inputBranchID1 && inputBranchID2) {
            query += $scope.encodeQueryParameter("inputBranchIDs", [inputBranchID1, inputBranchID2], query);
        }

        if (outputBranchID) {
            query += $scope.encodeQueryParameter("outputBranchID", outputBranchID, query);
        }

        d3.xhr("/branch/merge" + query).post(function(error, request) {
            if (error) {
                sweetAlert("Error", JSON.parse(error.response).message, "error");
            } else {
                $scope.graphDesignSpace(targetSpaceID);
            }
        });
    };

    $scope.encodeQueryParameter = function(parameterName, parameterValue, query) {
        if (query.length > 1) {
            return "&" + parameterName + "=" + encodeURIComponent(parameterValue);
        } else {
            return parameterName + "=" + encodeURIComponent(parameterValue);
        }
    };

}