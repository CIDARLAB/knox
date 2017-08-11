
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
// Exposes the function disableTabs.
(function($) {
    function disableTabs() {
        $(document).keydown(function (e) {
            var keycode1 = (e.keyCode ? e.keyCode : e.which);
            if (keycode1 == 0 || keycode1 == 9) {
                e.preventDefault();
                e.stopPropagation();
            }
        });
    }

    window.disableTabs = disableTabs;
})(jQuery);

(function($) {
    "use strict";

    // The target class observes an SVG element on the page, and
    // provides methods for setting and clearing graph data. A variable
    // called 'targets' holds all the svg rendering targets on the page.
    function Target(id) {
        this.layout = null;
        this.id = id;
        $(id).width($(id).parent().width());
        $(id).height($(id).parent().height());
    }

    // function translateRole(role) {
    //     switch (role) {
    //     case "ribozyme":
    //         return "RNA_stability_element";
    //     }
    //     return role;
    // }
    
    Target.prototype = {
        setGraph: function(graph) {
            var zoom = d3.behavior.zoom()
	        .scaleExtent([1, 10])
	        .on("zoom", () => {
                    svg.attr("transform", "translate(" +
                             d3.event.translate + ")scale(" + d3.event.scale + ")");
                });

            var svg = d3.select(this.id).call(zoom).append("svg:g");
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
            force.drag().on("dragstart", () => {
                d3.event.sourceEvent.stopPropagation();
            });
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
                    if (d.nodeTypes.length == 0) {
                        return "node";
                    } else if (d.nodeTypes.indexOf("start") >= 0) {
                        return "start-node";
                    } else if (d.nodeTypes.indexOf("accept") >= 0) {
                        return "accept-node";
                    }
                })
                .attr("r", 7).call(force.drag);
            
            const sbolImgSize = 50;
            
            var images = linksEnter.append("svg:image")
                .attr("xlink:href", (d) => {
                    if (d.hasOwnProperty("componentRoles")) {
                        const sbolpath = "./img/sbol/";
                        if (d["componentRoles"].length > 0) {
                            var role = d["componentRoles"][0];
                            switch (role) {
                            case "promoter":
                            case "terminator":
                            case "CDS":
                            case "restriction_enzyme_assembly_scar":
                            case "restriction_enzyme_recognition_site":
                            case "protein_stability_element":
                            case "blunt_end_restriction_enzyme_cleavage_site":
                            case "ribonuclease_site":
                            case "restriction_enzyme_five_prime_single_strand_overhang":
                            case "ribosome_entry_site":
                            case "five_prime_sticky_end_restriction_enzyme_cleavage_site":
                            case "RNA_stability_element":
                            case "ribozyme":
                            case "insulator":
                            case "signature":
                            case "operator":
                            case "origin_of_replication":
                            case "restriction_enzyme_three_prime_single_strand_overhang":
                            case "primer_binding_site":
                            case "three_prime_sticky_end_restriction_enzyme_cleavage_site":
                            case "protease_site":
                                return sbolpath + role + ".svg";

                            default:
                                return sbolpath + "user_defined.svg";
                            };
                        }
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
                    sourceX = d.source.x + normX * sourcePadding,
                    sourceY = d.source.y + normY * sourcePadding,
                    targetX = d.target.x - normX * targetPadding,
                    targetY = d.target.y - normY * targetPadding;
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
        search: new Target("#search-svg")
    };

    var layouts = {
        combineModal: null,
        listModal: null
    };
    
    window.onload = function() {
        disableTabs();
        hideExplorePageBtns();
        $.ajax({
            url: "/layouts/combine-modal.html",
            success: (result) => { layouts.combineModal = result; }
        });
        $.ajax({
            url: "/layouts/list-modal.html",
            success: (result) => { layouts.listModal = result; }
        });
    };

    // Implementation of autocomplete. The webapp requests a list of
    // all design spaces from Knox and caches it. The api exposes one
    // a couple of functions, one to update the list of completions
    // with the server, and one that takes a phrase and does string
    // matching to return a list of design spaces with similar names.
    (function() {
        var completionSet = new Set();
        
        function populateAutocompleteList(callback) {
            knox.listDesignSpaces((err, data) => {
                if (err) {
                    swal("Unable top populate autocomplete list!", "Are you sure Knox and Neo4j are running?");
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
        hideExplorePageBtns();
    }
    
    $("#navigation-bar").on("click", "*", clearAllPages);
    $("#brand").click(clearAllPages);

     function encodeQueryParameter(parameterName, parameterValue, query) {
        if (query.length > 1) {
            return "&" + parameterName + "=" + encodeURIComponent(parameterValue);
        } else {
            return parameterName + "=" + encodeURIComponent(parameterValue);
        }
    };
    
    function updateAutocompleteVisibility(id) {
        var autoCmpl = $(id);
        if (autoCmpl.children().length > 0) {
            autoCmpl.show();
        } else {
            autoCmpl.hide();
        }
    }

    var exploreBtnIDs = {
        delete: "#delete-btn",
        combine: "#combine-btn",
        list: "#list-btn"
    };

    function hideExplorePageBtns() {
        Object.keys(exploreBtnIDs).map((id, _) => {
            $(exploreBtnIDs[id]).hide();
        });
    }

    function showExplorePageBtns() {
        Object.keys(exploreBtnIDs).map((id, _) => {
            $(exploreBtnIDs[id]).show();
        });
    }

    var currentSpace;
    
    function onSearchSubmit(spaceid) {
        knox.getGraph(spaceid, (err, data) => {
            if (err) {
                swal("Graph lookup failed!", "error status: " + JSON.stringify(err));
            } else {
                targets.search.clear();
                targets.search.setGraph(data);
                $("#search-tb").blur();
                $("#search-autocomplete").blur();
                showExplorePageBtns();
                currentSpace = spaceid;
            }
        });
    }

    function longestListLength(listoflists) {
        var maxLength = 0;
        listoflists.map((list) => {
            if (list.length > maxLength) {
                maxLength = list.length;
            }
        });
        return maxLength;
    }

    $(exploreBtnIDs.list).click(() => {
        swal({
            title: "Pathways",
            html: true,
            text: layouts.listModal,
            animation: false,
            confirmButtonColor: "#F05F40"
        });
        var query = "/enumerate?targetSpaceID="
            + currentSpace + "&bfs=true";
        d3.json(query, (err, data) => {
            if (err) {
                window.alert(err);
            } else {
                const celHeight = 60;
                const celWidth = 50;
                var svg = document.getElementById("swal-svg");
                var loading = document.getElementById("swal-loading");
                loading.parentNode.removeChild(loading);
                var yPitch = (data.length + 1) * celHeight;
                var xPitch = (longestListLength(data) + 1) * celWidth;
                svg.setAttribute("xmlns:xlink","http://www.w3.org/1999/xlink");
                svg.setAttribute("height", yPitch);
                svg.setAttribute("width", xPitch);
                var pen = { x: 0, y: 0 };
                data.map((list) => {
                    list.map((element) => {
                        var svgimg =
                            document.createElementNS(
                                "http://www.w3.org/2000/svg", "image");
                        svgimg.setAttribute("height", "100");
                        svgimg.setAttribute("width", "100");
                        svgimg.setAttribute("id" ,"testimg2");
                        svgimg.setAttributeNS(
                            "http://www.w3.org/1999/xlink",
                            "href", "./img/sbol/" + element + ".svg");
                        svgimg.setAttribute("x", "" + pen.x);
                        svgimg.setAttribute("y", "" + pen.y);
                        svg.appendChild(svgimg);
                        pen.x += celWidth;
                    });
                    var line = document.createElementNS("http://www.w3.org/2000/svg", "line");
                    line.setAttribute("stroke", "black");
                    line.setAttribute("stroke-width", "4");
                    line.setAttribute("x1", "" + 0);
                    line.setAttribute("y1", "" + (pen.y + celWidth));
                    line.setAttribute("x2", "" + (pen.x + celWidth));
                    line.setAttribute("y2", "" + (pen.y + celWidth));
                    svg.appendChild(line);

                    pen.y += celHeight;
                    pen.x = 0;
                });
            }
        });
    });

    $(exploreBtnIDs.combine).click(() => {
        swal({
            title: "Apply Operator",
            html: true,
            animation: false,
            showCancelButton: true,
            closeOnConfirm: false,
            text: layouts.combineModal,
            confirmButtonColor: "#F05F40"
        }, function(isconfirm) {
            if (isconfirm) {
                var lhs = currentSpace;
                var rhs = $("#swal-combine-with").val().split(",");
                var lrhs = [lhs];
                var i;
                for (i = 0; i < rhs.length; i++) {
                    if (rhs[i].length > 0) {
                        lrhs.push(rhs[i]);
                    }
                }
                var query = "?";
                query += encodeQueryParameter("inputSpaceIDs", lrhs, query);
                query += encodeQueryParameter("outputSpaceID", $("#swal-output").val(), query);
                var request = new XMLHttpRequest();
                switch ($("#swal-select").val()) {
                case "Join":
                    request.open("POST", "/designSpace/join" + query, false);
                    break;

                case "OR":
                    query += encodeQueryParameter("isClosed", $("#swal-closed").val(), query);
                    request.open("POST", "/designSpace/or" + query, false);
                    break;

                case "Repeat":
                    query += encodeQueryParameter("isOptional", $("#swal-cardinality").val(), query);
                    request.open("POST", "/designSpace/repeat" + query, false);
                    break;

                case "AND":
                    query += encodeQueryParameter("tolerance", $("#swal-tolerance").val(), query);
                    request.open("POST", "/designSpace/and" + query, false);
                    break;

                case "Merge":
                    query += encodeQueryParameter("tolerance", $("#swal-tolerance").val(), query);
                    request.open("POST", "/designSpace/merge" + query, false);
                    break;
                }
                request.send(null);
                if (request.status >= 200 && request.status < 300) {
                    swal({
                        title: "Success!",
                        confirmButtonColor: "#F05F40",
                        type: "success"
                    });
                } else {
                    swal("Error: Operation failed with error: " + request.response);
                }
            }
        });
    });
    
    $(exploreBtnIDs.delete).click(() => {
        swal({
            title: "Really delete?",
            text: "You will not be able to recover the data!",
            type: "warning",
            showCancelButton: true,
            closeOnConfirm: false,
            confirmButtonColor: "#F05F40",
            confirmButtonText: "OK"
        }, function(isconfirm) {
            if (isconfirm) {
                var query = "?targetSpaceID=" + currentSpace;
                var request = new XMLHttpRequest();
                request.open("DELETE", "/designSpace" + query, false);
                request.send(null);
                if (request.status >= 200 && request.status < 300) {
                    swal({
                        title: "Deleted!",
                        confirmButtonColor: "#F05F40",
                        type: "success"
                    });
                    targets.search.clear();
                    hideExplorePageBtns();
                } else {
                    swal("Error: Failed to delete design space " + currentSpace + ".");
                }
            }
        });
    });
    
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
