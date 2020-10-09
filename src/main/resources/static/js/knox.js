import Target from './target.js';
import * as endpoint from "./endpoints.js";

/******************
 * GLOBAL VARIABLES
 ******************/
let panelNum = 1; //closed
let completionSet = new Set();
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

/*********************
 * HELPER FUNCTIONS
 *********************/
/**
 * Determine and add 'show', 'optional', and 'reverseOrient' flags to each link
 * @param graph design space graph
 */
export function condenseVisualization(graph){
  let sourceTargetMap = {};

  for(let i=0; i<graph.links.length; i++) {
    // add optional flag to all links
    graph.links[i].optional = false; //optional links show dashed lines
    graph.links[i].show = true; //will not be rendered if false
    graph.links[i].hasReverseOrient = false;

    //get all source/target pairs
    let sourceNode = graph.links[i].source.toString();
    let targetNode = graph.links[i].target.toString();
    let stPairNum = sourceNode + targetNode;

    if(!(stPairNum in sourceTargetMap)){
      sourceTargetMap[stPairNum] = i; //save index
    }
    else{
      let dupLink1 = graph.links[sourceTargetMap[stPairNum]];
      let dupLink2 = graph.links[i];

      if(dupLink1.componentIDs.length && dupLink2.componentIDs.length){

        //check ID equality
        let sortedComponentIDs1 = dupLink1.componentIDs.sort();
        let sortedComponentIDs2 = dupLink2.componentIDs.sort();
        if(sortedComponentIDs1.length !== sortedComponentIDs2.length ||
          sortedComponentIDs1.every(function(value, index) {
            return value !== sortedComponentIDs2[index]
          })){
          continue;
        }

        //check role equality
        let sortedRoles1 = dupLink1.componentRoles.sort();
        let sortedRoles2 = dupLink2.componentRoles.sort();
        if(sortedRoles1.length !== sortedRoles2.length ||
          sortedRoles1.every(function(value, index) {
            return value !== sortedRoles2[index]
          })){
          continue;
        }

        // check orientation
        if(dupLink1.orientation === "INLINE"){
          dupLink1.hasReverseOrient = true;
          dupLink2.hasReverseOrient = true;
          dupLink2.show = false;
        } else {
          dupLink1.hasReverseOrient = true;
          dupLink2.hasReverseOrient = true;
          dupLink1.show = false;
        }
      }

      else if(dupLink1.componentIDs.length){
        dupLink1.optional = true;
        dupLink2.show = false;
      } else {
        dupLink2.optional = true;
        dupLink1.show = false;
      }

      if(dupLink1.orientation === 'NONE'){
        sourceTargetMap[stPairNum] = i; //save new index
      }
    }
  }
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

  //autoshow history
  if(panelNum === 1){
    $('#vh-toggle-button').click();
  }
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
    let text = b.knoxID;
    if (text.length > 20) {
      text = text.substring(0, 19) + '...';
    }

    let option = $('<option></option>').val(b.knoxID).html(text);
    if (b.knoxID === currentBranch){
      option.prop('disabled', true);
    }
    branchSelector.append(option);
  });
}

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

//split by / for SBOL identity
//this may not work well with other import formats
export function splitElementID(elementID){
  let splitArr = elementID.split('/');
  return splitArr.length > 1 ? splitArr[splitArr.length-2] : elementID;
}

export function getSBOLImage(role){
  const sbolpath = "./img/sbol/";
  switch (role) {
    case "http://identifiers.org/so/SO:0000031":
      return sbolpath + "aptamer.svg";
    case "http://identifiers.org/so/SO:0001953":
      return sbolpath + "assembly-scar.svg";
    case "http://identifiers.org/so/SO:0001691":
      return sbolpath + "blunt-restriction-site.svg";
    case "http://identifiers.org/so/SO:0000316":
      return sbolpath + "cds.svg";
    case "http://identifiers.org/so/SO:0001955":
      return sbolpath + "dna-stability-element.svg";
    case "http://identifiers.org/so/SO:0000804":
      return sbolpath + "engineered-region.svg";
    case "http://identifiers.org/so/SO:0001932":
      return sbolpath + "five-prime-overhang.svg";
    case "http://identifiers.org/so/SO:0001975":
      return sbolpath + "five-prime-sticky-restriction-site.svg";
    case "http://identifiers.org/so/SO:0000627":
      return sbolpath + "insulator.svg";
    // There is no SBOL visual glyph mapped to the SO term ribozyme,
    // but RiboJ is listed as a prototypical example of the insulator glyph.
    case "http://identifiers.org/so/SO:0000374":
      return sbolpath + "insulator.svg";
    case "http://identifiers.org/so/SO:0001263":
    case "http://identifiers.org/so/SO:0000834":
      return sbolpath + "ncrna.svg";
    case "http://identifiers.org/so/SO:0001688":
    case "http://identifiers.org/so/SO:0001687":
      return sbolpath + "nuclease-site.svg";
    case "http://identifiers.org/so/SO:0000057":
    case "http://identifiers.org/so/SO:0000409":
      return sbolpath + "operator.svg";
    case "http://identifiers.org/so/SO:0000296":
      return sbolpath + "origin-of-replication.svg";
    case "http://identifiers.org/so/SO:0000724":
      return sbolpath + "origin-of-transfer.svg";
    case "http://identifiers.org/so/SO:0000553":
      return sbolpath + "polyA.svg";
    case "http://identifiers.org/so/SO:0005850":
      return sbolpath + "primer-binding-site.svg";
    case "http://identifiers.org/so/SO:0000167":
      return sbolpath + "promoter.svg";
    case "http://identifiers.org/so/SO:0001956":
      return sbolpath + "protease-site.svg";
    case "http://identifiers.org/so/SO:0001546":
      return sbolpath + "protein-stability-element.svg";
    case "http://identifiers.org/so/SO:0001977":
      return sbolpath + "ribonuclease-site.svg";
    case "http://identifiers.org/so/SO:0000139":
      return sbolpath + "ribosome-entry-site.svg";
    case "http://identifiers.org/so/SO:0001979":
      return sbolpath + "rna-stability-element.svg";
    case "http://identifiers.org/so/SO:0001978":
      return sbolpath + "signature.svg";
    case "http://identifiers.org/so/SO:0000299":
      return sbolpath + "specific-recombination-site.svg";
    case "http://identifiers.org/so/SO:0000141":
      return sbolpath + "terminator.svg";
    case "http://identifiers.org/so/SO:0001933":
      return sbolpath + "three-prime-overhang.svg";
    case "http://identifiers.org/so/SO:0001976":
      return sbolpath + "three-prime-sticky-restriction-site.svg";
    case "http://identifiers.org/so/SO:0000616":
      return sbolpath + "transcription-end.svg";
    case "http://identifiers.org/so/SO:0000319":
    case "http://identifiers.org/so/SO:0000327":
      return sbolpath + "translation-end.svg";

    default:
      return sbolpath + "no-glyph-assigned.svg";
  }
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
  let div = document.createElement('div');
  div.style.height = "inherit";
  div.style.overflow = "scroll";

  // loading div
  let loadingDiv = document.createElement('div');
  loadingDiv.appendChild(document.createTextNode("Loading..."));

  //append all
  div.appendChild(loadingDiv);

  swal({
    title: "Designs",
    content: div,
    className: "enumeration-swal"
  });

  endpoint.enumerateDesigns(currentSpace, (err, data) => {
    if (err) {
      swalError("Enumeration error: " + JSON.stringify(err));
    } else {
      div.removeChild(loadingDiv);
      let para = document.createElement("p");
      data.map((list) => {
        para.appendChild(document.createTextNode("["));
        const length = list.length;
        list.map((element, i) => {
          para.appendChild(document.createTextNode(splitElementID(element.id)));
          //append comma if there are more elements
          if (length !== i+1){
            para.appendChild(document.createTextNode(","));
          }
        });
        para.appendChild(document.createTextNode("]"));
        para.appendChild(document.createElement('br'));
      });

      div.appendChild(para);
    }
  });

});

$('#apply-operators-tooltip').click(() => {
  let div = document.createElement('div');

  //input space
  let inputDiv = document.createElement('div');
  let inputSpaceInput = document.createElement('input');
  inputSpaceInput.setAttribute("placeholder", "delimit with comma");
  makeDiv(inputDiv, inputSpaceInput, 'Combine with: ');

  //output space
  let outputDiv = document.createElement('div');
  let outputSpaceInput = document.createElement('input');
  makeDiv(outputDiv, outputSpaceInput, 'Output space ID: ');

  //operator dropdown
  let operatorDiv = document.createElement('div');
  let operatorDropdown = makeOperatorDropdown();
  makeDiv(operatorDiv, operatorDropdown, 'Operator: ');

  //optional div
  let optDiv = document.createElement('div');
  let optionalDropdown = makeOptionalDropdown();
  makeDiv(optDiv, optionalDropdown, 'Cardinality: ');

  //complete div
  let comDiv = document.createElement('div');
  let completeDropdown = makeCompleteDropdown();
  makeDiv(comDiv, completeDropdown, 'Complete Matches Only: ');

  //tolerance div
  let tolDiv = document.createElement('div');
  let toleranceDropdown = makeToleranceDropdown();
  makeDiv(tolDiv, toleranceDropdown, 'Tolerance: ');

  //append all
  div.appendChild(inputDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(outputDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(operatorDiv);
  div.appendChild(document.createElement('br'));

  $(operatorDropdown).change(function() {
    if(this.value === endpoint.operators.REPEAT){
      if(div.contains(tolDiv)){
        div.removeChild(tolDiv);
      }
      if(div.contains(comDiv)){
        div.removeChild(comDiv);
      }
      div.appendChild(optDiv);
    }
    if(this.value === endpoint.operators.AND){
      if(div.contains(optDiv)){
        div.removeChild(optDiv);
      }
      div.appendChild(tolDiv);
      div.appendChild(comDiv);
    }
    if(this.value === endpoint.operators.MERGE){
      if(div.contains(optDiv)){
        div.removeChild(optDiv);
      }
      if(div.contains(comDiv)){
        div.removeChild(comDiv);
      }
      div.appendChild(tolDiv);
    }
  });

  swal({
    title: "Apply Operator",
    buttons: true,
    content: div
  }).then((confirm) => {
    if (confirm) {
      let inputSpaces = [currentSpace];
      let combineWithSpaces = inputSpaceInput.value.split(",");
      for (let i = 0; i < combineWithSpaces.length; i++) {
        if (combineWithSpaces[i].trim().length > 0) {
          inputSpaces.push(combineWithSpaces[i].trim());
        }
      }
      let outputSpace = outputSpaceInput.value;
      let isOptional = optionalDropdown.value;
      let tolerance = toleranceDropdown.value;
      let isComplete = completeDropdown.value;

      switch (operatorDropdown.value) {
        case endpoint.operators.JOIN:
          endpoint.designSpaceJoin(inputSpaces, outputSpace);
          break;

        case endpoint.operators.OR:
          endpoint.designSpaceOr(inputSpaces, outputSpace);
          break;

        case endpoint.operators.REPEAT:
          endpoint.designSpaceRepeat(inputSpaces, outputSpace, isOptional);
          break;

        case endpoint.operators.AND:
          endpoint.designSpaceAnd(inputSpaces, outputSpace, tolerance, isComplete);
          break;

        case endpoint.operators.MERGE:
          endpoint.designSpaceMerge(inputSpaces, outputSpace, tolerance);
          break;
      }
    }
  });
});

function makeOperatorDropdown(){
  let operatorDropdown = document.createElement('select');
  let operatorOption = new Option("Operations", "", true, true);
  operatorOption.disabled = true;
  operatorDropdown.appendChild(operatorOption);
  for(let key in endpoint.operators){
    operatorDropdown.appendChild(new Option(endpoint.operators[key]));
  }

  return operatorDropdown;
}

function makeOptionalDropdown(){
  let optionalDropdown = document.createElement('select');
  optionalDropdown.setAttribute("id", "optional-dropdown");
  optionalDropdown.appendChild(new Option("one-or-more", false, true, true));
  optionalDropdown.appendChild(new Option("zero-or-more", true));

  return optionalDropdown;
}

function makeToleranceDropdown(){
  let toleranceDropdown = document.createElement('select');
  toleranceDropdown.setAttribute("id", "tolerance-dropdown");
  toleranceDropdown.appendChild(new Option("0", "0", true, true));
  toleranceDropdown.appendChild(new Option("1"));
  toleranceDropdown.appendChild(new Option("2"));
  toleranceDropdown.appendChild(new Option("3"));

  return toleranceDropdown;
}

function makeCompleteDropdown(){
  let completeDropdown = document.createElement('select');
  completeDropdown.setAttribute("id", "complete-dropdown");
  completeDropdown.appendChild(new Option("True", true, true, true));
  completeDropdown.appendChild(new Option("False", false));

  return completeDropdown;
}

function makeDiv(div, input, title){
  div.appendChild(document.createTextNode(title));
  div.appendChild(input);
}

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