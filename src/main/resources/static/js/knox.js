
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

// Utility for disabling navigation features.
// Exposes the function disableScroll and disableTabs.
(function($) {
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

    function disableTabs() {
        $(document).keydown(function (e) {
            var keycode1 = (e.keyCode ? e.keyCode : e.which);
            if (keycode1 == 0 || keycode1 == 9) {
                e.preventDefault();
                e.stopPropagation();
            }
        });
    }

    window.disableScroll = disableScroll;

    window.disableTabs = disableTabs;
})(jQuery);

(function($) {
    "use strict";

    function Target(id) {
        this.layout = null;
        this.id = id;
        $(id).width($(id).parent().width());
        $(id).height($(id).parent().height());
    }
    
    Target.prototype = {
        setGraph: function(graph) {
            var svg = d3.select(this.id);
            svg.append("defs").append("marker")
	        .attr("id", "endArrow")
	        .attr("viewBox", "0 -5 10 10")
	        .attr("refX", 6)
	        .attr("markerWidth", 6)
	        .attr("markerHeight", 6)
	        .attr("orient", "auto")
	        .append("path")
	        .attr("d", "M0,-5L10,0L0,5")
	        .attr("fill", "#999")
                .attr("opacity", "0.5");
            var force = (this.layout = d3.layout.force());
            force.charge(-400).linkDistance(100);
            force.nodes(graph.nodes).links(graph.links).size([
                $(this.id).parent().width(), $(this.id).parent().height()
            ]).start();
            
            var linksEnter = svg.selectAll(".link")
                .data(graph.links)
                .enter();
            
            var links = linksEnter.append("path")
                .attr("class", "link");
            
            var nodesEnter = svg.selectAll(".node")
                .data(graph.nodes)
                .enter();

            var circles = nodesEnter.append("circle")
                .attr("class", function(d) {
                    switch (d.nodeType) {
                    case "start": return "start-node";
                    case "accept": return "accept-node";
                    default: return "node";
                    }
                })
                .attr("r", 7).call(force.drag);

            const sbolImgSize = 50;
            
            var images = linksEnter.append("svg:image")
                .attr("xlink:href", (d) => {
                    if (d.hasOwnProperty("componentRoles")) {
                        const sbolpath = "./img/sbol/";
                        var role = d["componentRoles"][0];
                        switch (role) {
                        case "promoter":
                        case "terminator":
                        case "ribosome_entry_site":
                        case "CDS":
                        case "restriction_enzyme_assembly_scar":
                        case "restriction_enzyme_recognition_site":
                        case "protein_stability_element":
                        case "blunt_end_restriction_enzyme_clevage_site":
                        case "ribonuclease_site":
                        case "restriction_enzyme_five_prime_single_strand_overhang":
                        case "ribosome_entry_site":
                        case "five_prime_sticky_end_restriction_enzyme_cleavage_site":
                        case "RNA_stability_element":
                        case "insulator":
                        case "signature":
                        case "operator":
                        case "origin_of_replication":
                        case "restriction_enzyme_three_prime_single_strand_overhang":
                        case "primer_binding_site":
                        case "three_prime_sticky_end_restriction_enzyme_cleavage_site":
                        case "protease_site":
                            return sbolpath + role + ".svg";

                            // Special Cases:
                        case "ribozyme":
                            return sbolpath + "rna_stability_element.svg";

                        default:
                            return sbolpath + "user_defined.svg";
                        };
                    }
                    return "";
                }).attr("height", sbolImgSize)
                .attr("width", sbolImgSize)
                .attr("class", "sbol-gfx");
            
            force.on("tick", function () {
                links.attr('d', function(d) {
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
                circles.attr("cx", function (d) {
                    return d.x;
                }).attr("cy", function (d) {
                    return d.y;
                });
                images.attr("x", function (d) {
                    return (d.source.x + d.target.x) / 2 - sbolImgSize / 2;
                }).attr("y", function (d) {
                    return (d.source.y + d.target.y) / 2 - sbolImgSize / 2;
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
        disableTabs();
    };

    (function() {
        var completionSet;
        
        function populateAutocompleteList(callback) {
            knox.listDesignSpaces((err, data) => {
                if (err){
                    console.log("Error: unable to populate autocomplete list");
                } else {
                    completionSet = new Set();
                    data.map((element) => { completionSet.add(element); });
                    if (callback) callback();
                }
            });
        }

        window.populateAutocompleteList = populateAutocompleteList;

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
        $("#search-autocomplete").empty();
        $("#combine-tb-lhs").val("");
        $("#combine-tb-rhs").val("");
    }
    
    $("#navigation-bar").on("click", "*", clearAllPages);
    $("#brand").click(clearAllPages);

    function updateAutocompleteVisibility(id) {
        var autoCmpl = $(id);
        if (autoCmpl.children().length > 0) {
            autoCmpl.show();
        } else {
            autoCmpl.hide();
        }
    }

    function onSearchSubmit(queryString) {
        knox.getGraph(queryString, (err, data) => {
            if (err) {
                window.alert(err);
            } else {
                targets.search.clear();
                targets.search.setGraph(data);
                $("#search-tb").blur();
                $("#search-autocomplete").blur();
            }
        });
    }
    
    function makeAutocompleteRow(text, substr) {
        var div = document.createElement("div");
        var textRep = text.replace(substr, "," + substr + ",");
        var tokens = textRep.split(",");
        div.className = "autocomplete-entry";
        tokens.map((token) => {
            var textNode;
            if (token === substr) {
                textNode = document.createElement("strong");
            } else {
                textNode = document.createElement("span");
            }
            textNode.innerHTML = token;
            div.appendChild(textNode);
        });
        return div;
    }

    $("#search-tb").click(() => {
        // Currently autocomplete runs when the user clicks on an unfocused
        // textbox that supports completion. I think that this is reasonable,
        // but if you find that it is a performance issue you may want to change
        // the Knox webapp so that it populates the completion list once on
        // startup, and then only when some event triggers the creation of a new
        // graph.
        if (!$(this).is(":focus")) {
            populateAutocompleteList(() => {
                refreshCompletions("#search-tb", "#search-autocomplete", onSearchSubmit);
            });
        }
    });

    function refreshCompletions(textInputId, textCompletionsId, onSubmit) {
        var autoCmpl = $(textCompletionsId);
        autoCmpl.empty();
        var val = $(textInputId).val();
        if (val !== "") {
            var completions = suggestCompletions(val);
            completions.map((elem) => {
                var div = makeAutocompleteRow(elem, val);
                div.onclick = () => {
                    $(textInputId).val(elem);
                    refreshCompletions(textInputId, textCompletionsId);
                    onSubmit(elem);
                };
                autoCmpl.append(div);
            });
        }
        updateAutocompleteVisibility(textCompletionsId);
    }
    
    $("#search-tb").on("input", function() {
        refreshCompletions("#search-tb", "#search-autocomplete", onSearchSubmit);
    });

    $("#search-autocomplete").hide();
    
    $("#search-tb").focus(function() {
        updateAutocompleteVisibility("#search-autocomplete");
    });

    window.onclick = function(event) {
        if (!event.target.matches("#search-autocomplete")
            && !event.target.matches("#search-tb")) {
            $("#search-autocomplete").hide();
        }
    };

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
