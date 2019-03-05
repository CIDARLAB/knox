import Target from './target.js';
import * as endpoint from "./endpoints.js";

/******************
 * GLOBAL VARIABLES
 ******************/
let completionSet = new Set();
let layouts = {
  combineModal: null,
  listModal: null,
  saveModal: null,
  deleteModal: null,
};
export const targets = {
  search: new Target("#search-svg"),
  history: new Target("#history-svg")
};

const exploreBtnIDs = {
  delete: "#delete-btn",
  combine: "#combine-btn",
  list: "#list-btn",
  save: "#save-btn",
};
export const knoxClass = {
  HEAD: "Head",
  BRANCH: "Branch",
  COMMIT: "Commit"
};

let historyNodes;
export let currentSpace;
export let currentBranch;
export function setcurrentBranch(branchName){
  currentBranch = branchName;
}

/********************
 * WINDOW FUNCTIONS
 ********************/
window.onload = function() {
  addTooltips();
  disableTabs();
  hideExplorePageBtns();
  getModals();

  $("#search-autocomplete").hide();

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

function getModals(){
  $.ajax({
    url: "/layouts/combine-modal.html",
    success: (result) => { layouts.combineModal = result; }
  });
  $.ajax({
    url: "/layouts/list-modal.html",
    success: (result) => { layouts.listModal = result; }
  });
  $.ajax({
    url: "/layouts/save-modal.html",
    success: (result) => { layouts.saveModal = result; }
  });
  $.ajax({
    url: "/layouts/delete-modal.html",
    success: (result) => { layouts.deleteModal = result; }
  });
}

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

export function hideExplorePageBtns() {
  Object.keys(exploreBtnIDs).map((id, _) => {
    $(exploreBtnIDs[id]).hide();
  });
}

function showExplorePageBtns() {
  Object.keys(exploreBtnIDs).map((id, _) => {
    $(exploreBtnIDs[id]).show();
  });
}

export function clearAllPages() {
  Object.keys(targets).map((key, _) => { targets[key].clear(); });
  $("#search-tb").val("");
  $("#search-autocomplete").empty();
  $("#combine-tb-lhs").val("");
  $("#combine-tb-rhs").val("");
  hideExplorePageBtns();
  $('#branch-selector').find('option').not(':first').remove();
  $('#vh-sidebar').hide();
  $('#vh-toggle-button').hide();
}

export function visualizeDesignAndHistory(spaceid) {
  endpoint.getGraph(spaceid, (err, data) => {
    if (err) {
      swalError(JSON.stringify(err));
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

export function visualizeHistory(spaceid){
  endpoint.getHistory(spaceid, (err, data) => {
    if (err) {
      swalError("Graph error status: " + JSON.stringify(err));
    } else {
      targets.history.clear();
      targets.history.setHistory(data);
      currentBranch = data.links[0].target.knoxID;
      historyNodes = data.nodes;
      populateBranchSelector(data.nodes, $('#branch-selector'));
      $('#vh-sidebar').show();
      $('#vh-toggle-button').show();
    }
  });
}

export function populateBranchSelector(nodes, branchSelector){
  branchSelector.find('option').not(':first').remove();

  //repopulate
  let branches = nodes.filter(obj => obj.knoxClass === knoxClass.BRANCH);
  $.each(branches, function(i, b) {
    branchSelector.append($('<option></option>').val(b.knoxID).html(b.knoxID));
  });
}


/*********************
 * TOOLTIPS FUNCTIONS
 *********************/
function addTooltips(){
  $('#vh-toggle-button').tooltipster({
    content: "See version history"
  });

  let deleteBtn = $('#delete-btn');
  deleteBtn.tooltipster({
    content: $('#delete-design-tooltip'),
    side: 'top',
    interactive: true,
    theme: 'tooltipster-noir'
  });
  deleteBtn.tooltipster({
    content: $('#delete-branch-tooltip'),
    multiple: true,
    side: 'left',
    interactive: true,
    theme: 'tooltipster-noir'
  });
  deleteBtn.tooltipster({
    content: $('#reset-commit-tooltip'),
    multiple: true,
    side: 'bottom',
    interactive: true,
    theme: 'tooltipster-noir'
  });
  deleteBtn.tooltipster({
    content: $('#revert-commit-tooltip'),
    multiple: true,
    side: 'right',
    interactive: true,
    theme: 'tooltipster-noir'
  });

  let saveBtn = $('#save-btn');
  saveBtn.tooltipster({
    content: $('#make-commit-tooltip'),
    side: 'top',
    interactive: true,
    theme: 'tooltipster-noir'
  });
  saveBtn.tooltipster({
    content: $('#make-branch-tooltip'),
    multiple: true,
    side: 'bottom',
    interactive: true,
    theme: 'tooltipster-noir'
  });

  let listBtn = $('#list-btn');
  listBtn.tooltipster({
    content: $('#enumerate-designs-tooltip'),
    side: 'top',
    interactive: true,
    theme: 'tooltipster-noir'
  });

  let operatorBtn = $('#combine-btn');
  operatorBtn.tooltipster({
    content: $('#apply-operators-tooltip'),
    side: 'top',
    interactive: true,
    theme: 'tooltipster-noir'
  });
}

$('#enumerate-designs-tooltip').click(() => {

});

$('#apply-operators-tooltip').click(() => {

});

$('#delete-design-tooltip').click(() => {
  swal({
    title: "Really delete?",
    text: "You will not be able to recover the data!",
    icon: "warning",
    buttons: true
  }).then((confirm) => {
    if (confirm) {
      endpoint.deleteDesign();
    }
  });
});

$('#delete-branch-tooltip').click(() => {
  // create DOM object to add to alert
  let dropdown = document.createElement("select");
  let branchOption = new Option("Branches", "", true, true);
  branchOption.disabled = true;
  dropdown.appendChild(branchOption);
  populateBranchSelector(historyNodes, $(dropdown));
  swal({
    title: "Really delete?",
    text: "Select the branch you want to delete (you cannot delete the current active branch)",
    icon: "warning",
    buttons: true,
    content: dropdown
  }).then((confirm) => {
    if (confirm) {
      let branchName = $(dropdown)[0].options[$(dropdown)[0].selectedIndex].value;
      endpoint.deleteBranch(branchName);
    }
  });
});

$('#reset-commit-tooltip').click(() => {
  swal({
    title: "Really reset?",
    text: "No history of the commit will remain (If you want to preserve history, use revert). ",
    icon: "warning",
    buttons: true
  }).then((confirm) => {
    if (confirm) {
      endpoint.resetCommit();
    }
  });
});

$('#revert-commit-tooltip').click(() => {
  swal({
    title: "Really revert?",
    text: "A new commit will be made from the previous design.",
    icon: "warning",
    buttons: true
  }).then((confirm) => {
    if (confirm) {
      endpoint.revertCommit();
    }
  });
});

$('#make-commit-tooltip').click(() => {
  endpoint.makeCommit();
});

$('#make-branch-tooltip').click(() => {
  swal({
    title: "Create branch",
    text: "Enter a unique branch name",
    content: "input",
    buttons: true
  }).then((branchName) => {
    if (branchName){
      endpoint.makeNewBranch(branchName);
    }
  });
});

export function swalError(errorMsg){
  swal({
    title: "Error!",
    text: errorMsg,
    icon: "error"
  });
}

export function swalSuccess(){
  swal({
    title: "Success!",
    icon: "success"
  });
}


/**************************
 * VERSION HISTORY SIDEBAR
 **************************/
// change version history visualization when
// value changes in the drop down
$("#branch-selector").change(function() {
  let branchName = this.value;
  endpoint.checkoutBranch(branchName);
  visualizeHistory(currentSpace);
});

$('#vh-toggle-button').click(function() {
  if (panelNum === 1) {
    $('#vh-toggle-button span').addClass('fa-chevron-left');
    $('#vh-toggle-button span').removeClass('fa-chevron-right');

    // show VH sidebar
    $('#vh-sidebar').animate({
      left: 0,
    });
    // move toggle button dynamically with the sidebar
    $('#vh-toggle-button').animate({
      left: 400,
    });
    // Update tooltip title
    $('#vh-toggle-button').tooltipster('content', 'Hide version history');
    panelNum = 2;
  } else {
    $('#vh-toggle-button span').removeClass('fa-chevron-left');
    $('#vh-toggle-button span').addClass('fa-chevron-right');

    // hide VH sidebar
    $('#vh-sidebar').animate({
      left: -380,
    });
    // move toggle button dynamically with the sidebar
    $('#vh-toggle-button').animate({
      left: 20,
    });
    // Update tooltip title
    $('#vh-toggle-button').tooltipster('content', 'See version history');

    panelNum = 1;
  }
});

/************************
 * NAVIGATION FUNCTIONS
 ************************/
$("#brand").click(
  clearAllPages
);

/************************
 * SEARCH BAR FUNCTIONS
 ************************/
$("#search-tb").on("input", function() {
  refreshCompletions("#search-tb", "#search-autocomplete", visualizeDesignAndHistory);
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
      refreshCompletions("#search-tb", "#search-autocomplete", visualizeDesignAndHistory);
    });
  }
});

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
    endpoint.listDesignSpaces((err, data) => {
        if (err) {
            swalError("Are you sure Knox and Neo4j are running?");
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



/******************
 * BUTTON FUNCTIONS
 ******************/
export function encodeQueryParameter(parameterName, parameterValue, query) {
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
    }).then((isconfirm) => {
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



