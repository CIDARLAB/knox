function knoxCtrl($scope) {

	$scope.graph1 = {id: "graph1"};
	$scope.graph2 = {id: "graph2"};

	var initializeGraph = function(graph, width, height) {
	    graph.force = d3.layout.force()
	            .charge(-250).linkDistance(60).size([width, height]);

	    graph.svg = d3.select("#" + graph.id).append("svg")
	            .attr("width", "100%").attr("height", "100%")
	            .attr("pointer-events", "all");

	    graph.svg.append('defs').append('marker')
	            .attr('id', 'endArrow')
	            .attr('viewBox', '0 -5 10 10')
	            .attr('refX', 6)
	            .attr('markerWidth', 6)
	            .attr('markerHeight', 6)
	            .attr('orient', 'auto')
	        .append('path')
	            .attr('d', 'M0,-5L10,0L0,5')
	            .attr('fill', '#000');

	    graph.width = width;
	    graph.height = height;
	};

	initializeGraph($scope.graph1, 540, 540);
	initializeGraph($scope.graph2, 540, 540);

	$scope.designSpaceID1 = "test1";
	$scope.designSpaceID2 = "test2";

	$scope.findDesignSpace = function(id, graph) {

		var query = "";
		if (id) {
			query = "?id=" + encodeURIComponent(id);
		}

		d3.json("/findDesignSpace" + query, function(error, result) {
	        if (error) return;

	        var i;
	        for (i = 0; i < result.nodes.length; i++) {
	        	if (result.nodes[i].nodeType === "start") {
	        		result.nodes[i].x = graph.width/2;
			        result.nodes[i].y = graph.height/2;
			        result.nodes[i].fixed = true;
	        	}
	        }
   
	        graph.force.nodes(result.nodes).links(result.links).start();

	        var link = graph.svg.selectAll(".link")
	                .data(result.links).enter()
	                .append("path").attr("class", "link");

	       	var componentLinks = [];
	        var i;
	        for (i = 0; i < result.links.length; i++) {
	        	if (result.links[i].componentRole) {
	        		componentLinks.push(result.links[i]);
	        	}
	        }

	        var icon = graph.svg.append("g").selectAll("g")
	                .data(componentLinks).enter().append("g");

	        icon.append("image").attr("xlink:href", function (d) {
	                    return "image/" + d.componentRole + ".png";
	                })
	                .attr("x", -15)
	                .attr("y", -15)
	                .attr("width", 30).attr("height", 30)
	                .attr("class", "type-icon");

	        var node = graph.svg.selectAll(".node")
	                .data(result.nodes).enter()
	                .append("circle")
	                .attr("class", function (d) {
	                    return "node " + ((d.nodeType) ? d.nodeType:"inner");
	                })
	                .attr("r", 10)
	                .call(graph.force.drag);

	        // html title attribute
	        node.append("title")
	                .text(function (d) { return d.displayID; })

	        // force feed algo ticks
	        graph.force.on("tick", function() {

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
		});
	};

}