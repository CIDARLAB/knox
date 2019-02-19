import Target from './target.js';

/******************
 * GLOBAL VARIABLES
 ******************/
let completionSet = new Set();
let currentSpace;
let layouts = {
  combineModal: null,
  listModal: null
};
const targets = {
  search: new Target("#search-svg"),
  history: new Target("#history-svg")
};
const extensions = {
  D3: "/designSpace/graph/d3",
  List: "/designSpace/list",
  history: "/branch/graph/d3",
  branch: "/branch", //post vs delete
  checkout: "/branch/checkout",
  commit: "/branch/commitTo",
  reset: "/branch/reset"
};
const exploreBtnIDs = {
  delete: "#delete-btn",
  combine: "#combine-btn",
  list: "#list-btn",
  commit: "#commit-btn"
};
export const knoxClass = {
  HEAD: "Head",
  BRANCH: "Branch",
  COMMIT: "Commit"
};

/********************
 * WINDOW FUNCTIONS
 ********************/
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

  $("#navigation-bar").on("click", "*", clearAllPages);
  $("#brand").click(clearAllPages);
  $(".navbar-collapse ul li a").click(function() {
    $(".navbar-toggle:visible").click();
  });

  $("#search-autocomplete").hide();
  $("#search-tb").on("input", function() {
    refreshCompletions("#search-tb", "#search-autocomplete", onSearchSubmit);
  });
  $("#search-tb").focus(function() {
    updateAutocompleteVisibility("#search-autocomplete");
  });
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

  // change version history visualization when
  // value changes in the drop down
  $("#branch-selector").change(function() {
    visualizeHistory(currentSpace);
  });

  $("body").scrollspy({
    target: ".navbar-fixed-top",
    offset: 51
  });

  $("#mainNav").affix({
    offset: {
      top: 100
    }
  });
};

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

// Utility for disabling navigation features.
// Exposes the function disableTabs.
function disableTabs() {
  $(document).keydown(function (e) {
    var keycode1 = (e.keyCode ? e.keyCode : e.which);
    if (keycode1 === 0 || keycode1 === 9) {
      e.preventDefault();
      e.stopPropagation();
    }
  });
}

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

function clearAllPages() {
  Object.keys(targets).map((key, _) => { targets[key].clear(); });
  $("#search-tb").val("");
  $("#search-autocomplete").empty();
  $("#combine-tb-lhs").val("");
  $("#combine-tb-rhs").val("");
  hideExplorePageBtns();
}

/********************
 * KNOX FUNCTIONS
 ********************/
// callback is of the form: function(err, jsonObj)
function getGraph (id, callback){
  var query = "?targetSpaceID=" + encodeURIComponent(id);
  d3.json(extensions.D3 + query, callback);
}

function getHistory (id, callback){
  var query = "?targetSpaceID=" + encodeURIComponent(id);
  d3.json(extensions.history + query, callback);
}

function listDesignSpaces (callback){
  d3.json(extensions.List, callback);
}


/************************
 * AUTOCOMPLETE FUNCTIONS
 ************************/
function updateAutocompleteVisibility(id) {
  var autoCmpl = $(id);
  if (autoCmpl.children().length > 0) {
    autoCmpl.show();
  } else {
    autoCmpl.hide();
  }
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

// Implementation of autocomplete. The webapp requests a list of
// all design spaces from Knox and caches it. The api exposes one
// a couple of functions, one to update the list of completions
// with the server, and one that takes a phrase and does string
// matching to return a list of design spaces with similar names.

function populateAutocompleteList(callback) {
    listDesignSpaces((err, data) => {
        if (err) {
            swal("Unable top populate autocomplete list!", "Are you sure Knox and Neo4j are running?");
        } else {
            completionSet = new Set();
            data.map((element) => { completionSet.add(element); });
            if (callback) callback();
        }
    });
}

// FIXME: A prefix tree could be more efficient.
function suggestCompletions (phrase){
    var results = [];
    completionSet.forEach((element) => {
        if (element.indexOf(phrase) !== -1) {
            results.push(element);
        }
    });
    return results;
}

function onSearchSubmit(spaceid) {
    getGraph(spaceid, (err, data) => {
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

  visualizeHistory(spaceid);
}

/******************
 * VERSION HISTORY
 ******************/
function visualizeHistory(spaceid){
  getHistory(spaceid, (err, data) => {
    if (err) {
      swal("Graph lookup failed!", "error status: " + JSON.stringify(err));
    } else {
      targets.history.clear();
      targets.history.setHistory(data);
      populateBranchSelector(data.nodes);
    }
  });
}

function populateBranchSelector(nodes){
  let branchSelector = $('#branch-selector');

  // clear options
  branchSelector.find('option').not(':first').remove();

  //repopulate
  let branches = nodes.filter(obj => obj.knoxClass === knoxClass.BRANCH);
  $.each(branches, function(i, b) {
    branchSelector.append($('<option></option>').val(b.knoxID).html(b.knoxID));
  });
}



/******************
 * BUTTON FUNCTIONS
 ******************/
function encodeQueryParameter(parameterName, parameterValue, query) {
  if (query.length > 1) {
    return "&" + parameterName + "=" + encodeURIComponent(parameterValue);
  } else {
    return parameterName + "=" + encodeURIComponent(parameterValue);
  }
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
    var query = "/designSpace/enumerate?targetSpaceID="
        + currentSpace + "&bfs=true";
    d3.json(query, (err, data) => {
        if (err) {
            window.alert(err);
        } else {
            const celHeight = 80;
            const celWidth = 50;
            var svg = document.getElementById("swal-svg");
            var loading = document.getElementById("swal-loading");
            loading.parentNode.removeChild(loading);
            var yPitch = 3.1*celHeight;
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
                    svgimg.setAttribute("id", "testimg2");
                    svgimg.setAttributeNS(
                        "http://www.w3.org/1999/xlink",
                        "href", "./img/sbol/" + element.roles[0] + ".svg");
                    svgimg.setAttribute("x", "" + pen.x);
                    svgimg.setAttribute("y", "" + pen.y);
                    svg.appendChild(svgimg);
                    var svgtext =
                        document.createElementNS(
                            "http://www.w3.org/2000/svg", "text");
                    svgtext.setAttribute("height", "100");
                    svgtext.setAttribute("width", "100");
                    svgtext.setAttribute("id", "testimg2");
                    svgtext.setAttribute("font-family", "sans-serif");
                    svgtext.setAttribute("font-size", "20px");
                    svgtext.setAttribute("fill", "black");
                    svgtext.textContent = element.id;
                    svgtext.setAttribute("x", "" + (pen.x + 0.85*celWidth));
                    if (element.roles[0] === "CDS") {
                        svgtext.setAttribute("y", "" + (pen.y + 1.1*celHeight));
                    } else {
                        svgtext.setAttribute("y", "" + (pen.y + celHeight));
                    }
                    svg.appendChild(svgtext);
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
                request.open("POST", "/designSpace/or" + query, false);
                break;

            case "Repeat":
                query += encodeQueryParameter("isOptional", $("#swal-cardinality").val(), query);
                request.open("POST", "/designSpace/repeat" + query, false);
                break;

            case "AND":
                query += encodeQueryParameter("tolerance", $("#swal-tolerance").val(), query);
                query += encodeQueryParameter("isComplete", $("#swal-complete").val(), query);
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



