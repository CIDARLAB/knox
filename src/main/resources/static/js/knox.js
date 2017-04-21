
// Wrapper JavaScript interface for the Knox API
(function() {
    const extensions = {
        D3: "/designSpace/graph/d3"
    };
    
    window.knox = {
        // callback is of the form: function(err, jsonObj)
        getGraph: function(id, callback) {
            var query = "?targetSpaceID=" + encodeURIComponent(id);
            d3.json(extensions.D3 + query, callback);
        }
    };
})();

// Utility for disabling scrolling.
// Exposes the function disableScroll
(function() {
    // left: 37, up: 38, right: 39, down: 40,
    // spacebar: 32, pageup: 33, pagedown: 34, end: 35, home: 36
    var keys = {37: 1, 38: 1, 39: 1, 40: 1};

    function preventDefault(e) {
        e = e || window.event;
        if (e.preventDefault) {
            e.preventDefault();
        }
        e.returnValue = false;
    }

    function preventDefaultForScrollKeys(e) {
        if (keys[e.keyCode]) {
            preventDefault(e);
            return false;
        }
        return true;
    }

    function disableScroll() {
        if (window.addEventListener) {
            window.addEventListener("DOMMouseScroll", preventDefault, false);
        }
        window.onwheel = preventDefault;
        window.onmousewheel = document.onmousewheel = preventDefault;
        window.ontouchmove  = preventDefault;
        document.onkeydown  = preventDefaultForScrollKeys;
    }

    window.disableScroll = disableScroll;
})();

(function($) {
    "use strict";

    function Target(id) {
        this.layouts = {};
        this.id = id;
        $(id).width($(id).parent().width());
        $(id).height($(id).parent().height());
    }
    
    Target.prototype = {
        appendGraph: function(id, graph) {
            var force = (this.layouts[id] = d3.layout.force());
            force.charge(-400).linkDistance(100);
            force.nodes(graph.nodes).links(graph.links).size([
                $(this.id).parent().width(), $(this.id).parent().height()
            ]).start();
            var svg = d3.select(this.id);
            var link = svg.selectAll(".link")
                .data(graph.links)
                .enter()
                .append("g")
                .attr("class", "link")
                .append("line")
                .attr("class", "link-line");

            var linkText = svg.selectAll(".link")
                .append("text")
                .attr("class", "link-label")
                .attr("font-family", "Open Sans")
                .attr("fill", "Black")
                .style("font", "normal 14px Open Sans")
                .attr("dy", ".35em")
                .attr("text-anchor", "middle")
                .text(function(d) {
                    if (d.hasOwnProperty("componentRoles")) {
                        return d.componentRoles[0];
                    } else {
                        return "";
                    }
                });
            
            var node = svg.selectAll(".node")
                .data(graph.nodes)
                .enter().append("circle")
                .attr("class", "node")
                .attr("r", 5)
                .call(force.drag);

            force.on("tick", function () {
                link.attr("x1", function (d) {
                    return d.source.x;
                }).attr("y1", function (d) {
                    return d.source.y;
                }).attr("x2", function (d) {
                    return d.target.x;
                }).attr("y2", function (d) {
                    return d.target.y;
                });
                node.attr("cx", function (d) {
                    return d.x;
                }).attr("cy", function (d) {
                    return d.y;
                });
                linkText.attr("x", function(d) {
                    return ((d.source.x + d.target.x)/2);
                }).attr("y", function(d) {
                    return ((d.source.y + d.target.y)/2);
                });
            });
        },

        clear: function() {
            d3.select(this.id).selectAll("*").remove();
            Object.keys(this.layouts).map((key, _) => {
                delete this.layouts[key];
            });
        },
        
        removeGraph: function(id) {
            delete this.layouts[id];
        },

        expandToFillParent() {
            Object.keys(this.layouts).map((key, _) => {
                var layout = this.layouts[key];
                var width = $(this.id).parent().width();
                var height = $(this.id).parent().height();
                layout.size([width, height]);
                layout.start();
            });
            $(this.id).width($(this.id).parent().width());
            $(this.id).height($(this.id).parent().height());
        }
    };
    
    var targets = {
        search: new Target("#search-svg")
    };
    
    window.onload = function() {
        disableScroll();  
    };
    
    $("#search-tb").keydown(function(e) {
        const submitKeyCode = 13;
        if ((e.keyCode || e.which) == submitKeyCode) {
            var graphName = this.value;
            knox.getGraph(graphName, function(err, data) {
                if (err) {
                    window.alert(err);
                } else {
                    targets.search.clear();
                    targets.search.appendGraph(graphName, data);
                }
            });
        }
    });

    window.onresize = function(e) {
        var currentHash = window.location.hash.substr(1);
        var currentSection = document.getElementById(currentHash);
        if (currentSection) {
            window.scrollTo(0, currentSection.offsetTop);
        }
        Object.keys(targets).map((key, _) => {
            targets[key].expandToFillParent();
        });
    };
    
    $("body").scrollspy({
        target: ".navbar-fixed-top",
        offset: 51
    });

    $(".navbar-collapse ul li a").click(function() {
        $(".navbar-toggle:visible").click();
    });

    $("#mainNav").affix({
        offset: {
            top: 100
        }
    });
})(jQuery);
