function knoxCtrl($scope) {
	var width = 800, height = 800;

    var force = d3.layout.force()
            .charge(-250).linkDistance(60).size([width, height]);

    var svg = d3.select("#graph").append("svg")
            .attr("width", "100%").attr("height", "100%")
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

    d3.json("/findDesignSpace", function(error, graph) {
        if (error) return;

        force.nodes(graph.nodes).links(graph.links).start();

        var link = svg.selectAll(".link")
                .data(graph.links).enter()
                .append("path").attr("class", "link");

       	var componentLinks = [];
        var i;
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
                .text(function (d) { return d.displayID; })

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
    });

	// $scope.designSpaceID = "test";

	// $scope.findDesignSpace = function() {
 //        $.get("/graph?title=" + encodeURIComponent($scope.designSpaceID),
 //            function (data) {
 //                if (!data) return;
 //                data = data["_embedded"].movies;
 //                data.forEach(function (movie) {
 //                    $("<tr><td class='movie'>" + movie.title + "</td><td>" + movie.released + "</td><td>" + movie.tagline + "</td></tr>").appendTo(t)
 //                            .click(function() { showMovie($(this).find("td.movie").text());})
 //                });
 //                showMovie(data[0].title);
 //            }, "json");
 //        return false;
	// };
}