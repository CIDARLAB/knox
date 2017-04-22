
// Wrapper JavaScript interface for the Knox API
(function() {
    const extensions = {
        D3: "/designSpace/graph/d3",
        List: "/list"
    };
    
    window.knox = {
        // callback is of the form: function(err, jsonObj)
        getGraph: (id, callback) => {
            var query = "?targetSpaceID=" + encodeURIComponent(id);
            d3.json(extensions.D3 + query, callback);
        },

        listDesignSpaces: (callback) => {
            d3.json(extensions.List, callback);
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

    var d3Force = {
        addLinksToSvg: (svg, graph) => {
            return svg.selectAll(".link")
                .data(graph.links)
                .enter()
                .append("g")
                .attr("class", "link")
                .append("line")
                .attr("class", "link-line");
        },

        addTextLabelsToLinks: (svg, graph) => {
            return svg.selectAll(".link")
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
        },

        addNodesToSvg: (svg, graph) => {
            return svg.selectAll(".node")
                .data(graph.nodes)
                .enter().append("circle")
                .attr("class", "node")
                .attr("r", 5);
        }
    };

    function Target(id) {
        this.layout = null;
        this.id = id;
        $(id).width($(id).parent().width());
        $(id).height($(id).parent().height());
    }
    
    Target.prototype = {
        setGraph: function(graph) {
            var force = (this.layout = d3.layout.force());
            force.charge(-400).linkDistance(100);
            force.nodes(graph.nodes).links(graph.links).size([
                $(this.id).parent().width(), $(this.id).parent().height()
            ]).start();
            var svg = d3.select(this.id);
            var links = d3Force.addLinksToSvg(svg, graph);
            var linkText = d3Force.addTextLabelsToLinks(svg, graph);
            var nodes = d3Force.addNodesToSvg(svg, graph).call(force.drag);
            force.on("tick", function () {
                links.attr("x1", function (d) {
                    return d.source.x;
                }).attr("y1", function (d) {
                    return d.source.y;
                }).attr("x2", function (d) {
                    return d.target.x;
                }).attr("y2", function (d) {
                    return d.target.y;
                });
                nodes.attr("cx", function (d) {
                    return d.x;
                }).attr("cy", function (d) {
                    return d.y;
                });
                linkText.attr("x", function(d) {
                    return ((d.source.x + d.target.x) / 2);
                }).attr("y", function(d) {
                    return ((d.source.y + d.target.y) / 2);
                });
            });
        },

        clear: function() {
            d3.select(this.id).selectAll("*").remove();
            delete this.layout;
        },
        
        removeGraph: function(id) {
            delete this.layout;
        },

        expandToFillParent() {
            var width = $(this.id).parent().width();
            var height = $(this.id).parent().height();
            if (this.layout) {
                this.layout.size([width, height]);
                this.layout.start();
            }
            $(this.id).width($(this.id).parent().width());
            $(this.id).height($(this.id).parent().height());
        }
    };
    
    var targets = {
        search: new Target("#search-svg"),
        combine: new Target("#combine-svg")
    };

    window.onload = function() {
        disableScroll();
    };

    (function() {
        var completionSet;
        
        function populateAutocompleteList() {
            knox.listDesignSpaces((err, data) => {
                if (err){
                    console.log("error");
                } else {
                    completionSet = new Set();
                    data.map((element) => { completionSet.add(element); });
                    console.log(completionSet);
                }
            });
        }

        populateAutocompleteList();
        
        window.setInterval(populateAutocompleteList, 30000);

        // FIXME: A prefix tree could be more efficient.
        window.suggestCompletions = (phrase) => {
            var results = [];
            completionSet.forEach((element) => {
                if (element.indexOf(phrase) !== -1) {
                    results.push(element);
                }
            });
            return results;
        };
    })();
    
    function clearAllPages() {
        Object.keys(targets).map((key, _) => { targets[key].clear(); });
        $("#search-tb").val("");
        $("#combine-tb-lhs").val("");
        $("#combine-tb-rhs").val("");
    }
    
    $("#navigation-bar").on("click", "*", clearAllPages);
    $("#brand").click(clearAllPages);

    $("#search-tb").on("input", function() {
        // console.log(JSON.stringify(suggestCompletions($(this).val())));
    });
    
    $("#search-tb").keydown(function(e) {
        const submitKeyCode = 13;
        if ((e.keyCode || e.which) == submitKeyCode) {
            knox.getGraph(this.value, (err, data) => {
                if (err) {
                    window.alert(err);
                } else {
                    targets.search.clear();
                    targets.search.setGraph(data);
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
