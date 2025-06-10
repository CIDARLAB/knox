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

const THEME = 'ambiance';

let sbolDoc;

const editors = {
  "specEditor": CodeMirror.fromTextArea(document.getElementById('goldbar-input-0'), {
    lineNumbers: true,
    lineWrapping:true
  }),
  "catEditor": CodeMirror.fromTextArea(document.getElementById('categories-0'), {
    lineNumbers: true,
    lineWrapping:true
  })
};

editors.specEditor.setOption("theme", THEME);
editors.catEditor.setOption("theme", THEME);

const exploreBtnIDs = {
  delete: "#delete-btn",
  combine: "#combine-btn",
  list: "#list-btn",
  sbol: "#sbol-btn",
  score: "#score-btn",
  bestPath: "#bestpath-btn"
  // save: "#save-btn",
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
  // deleteBtn.tooltipster({
  //   content: $('#delete-branch-tooltip'),
  //   multiple: true,
  //   side: 'left',
  //   interactive: true,
  //   theme: 'tooltipster-noir'
  // });
  // deleteBtn.tooltipster({
  //   content: $('#reset-commit-tooltip'),
  //   multiple: true,
  //   side: 'bottom',
  //   interactive: true,
  //   theme: 'tooltipster-noir'
  // });
  // deleteBtn.tooltipster({
  //   content: $('#revert-commit-tooltip'),
  //   multiple: true,
  //   side: 'right',
  //   interactive: true,
  //   theme: 'tooltipster-noir'
  // });

  // let saveBtn = $('#save-btn');
  // saveBtn.tooltipster({
  //   content: $('#make-commit-tooltip'),
  //   side: 'top',
  //   interactive: true,
  //   theme: 'tooltipster-noir'
  // });
  // saveBtn.tooltipster({
  //   content: $('#make-branch-tooltip'),
  //   multiple: true,
  //   side: 'bottom',
  //   interactive: true,
  //   theme: 'tooltipster-noir'
  // });

  let listBtn = $('#list-btn');
  listBtn.tooltipster({
    content: $('#enumerate-designs-tooltip'),
    side: 'top',
    interactive: true,
    theme: 'tooltipster-noir'
  });

  let bestPathBtn = $('#bestpath-btn');
  bestPathBtn.tooltipster({
    content: $('#best-path-tooltip'),
    side: 'top',
    interactive: true,
    theme: 'tooltipster-noir'
  });
  

  let scoreBtn = $('#score-btn');
  scoreBtn.tooltipster({
    content: $('#graph-score-tooltip'),
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

  let sbolBtn = $('#sbol-btn');
  sbolBtn.tooltipster({
    content: $('#export-sbol-tooltip'),
    side: 'top',
    interactive: true,
    theme: 'tooltipster-noir'
  });
}


$('#export-sbol-tooltip').click(() => {
  endpoint.exportDesign();
});

$('#enumerate-designs-tooltip').click(() => {
  let div = document.createElement('div');
  
  // enumerate dropdown
  let enumerateDiv = document.createElement('div');
  let enumerateDropdown = makeEnumerateDropdown();
  makeDiv(enumerateDiv, enumerateDropdown, 'Options: ');

  // num designs div
  let numDesignsDiv = document.createElement('div');
  let numDesignsInput = document.createElement('input');
  numDesignsInput.setAttribute("type", "number");
  numDesignsInput.setAttribute("value", "0");
  numDesignsInput.setAttribute("min", "0");
  makeDiv(numDesignsDiv, numDesignsInput, 'Number of Designs (0 means all possible): ');

  // min length div
  let minLengthDiv = document.createElement('div');
  let minLengthInput = document.createElement('input');
  minLengthInput.setAttribute("type", "number");
  minLengthInput.setAttribute("value", "0");
  minLengthInput.setAttribute("min", "0");
  makeDiv(minLengthDiv, minLengthInput, 'Minimum Length of Designs: ');

  // max length div
  let maxLengthDiv = document.createElement('div');
  let maxLengthInput = document.createElement('input');
  maxLengthInput.setAttribute("type", "number");
  maxLengthInput.setAttribute("value", "0");
  maxLengthInput.setAttribute("min", "0");
  makeDiv(maxLengthDiv, maxLengthInput, 'Maximum Length of Designs (0 means no Max): ');

  // is weighted div
  let isWeightedDiv = document.createElement('div');
  let isWeightedInput = document.createElement('input');
  isWeightedInput.setAttribute("type", "checkbox");
  isWeightedInput.checked = "true";
  makeDiv(isWeightedDiv, isWeightedInput, 'Is Space Weighted?: ');

  // is sample space div
  let isSampleSpaceDiv = document.createElement('div');
  let isSampleSpaceInput = document.createElement('input');
  isSampleSpaceInput.setAttribute("type", "checkbox");
  makeDiv(isSampleSpaceDiv, isSampleSpaceInput, 'Is Sample Space?: ');

  // allow duplicates div
  let allowDuplicatesDiv = document.createElement('div');
  let allowDuplicatesInput = document.createElement('input');
  allowDuplicatesInput.setAttribute("type", "checkbox");
  makeDiv(allowDuplicatesDiv, allowDuplicatesInput, 'Allow Duplicates?: ');

  // append all
  div.appendChild(enumerateDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(numDesignsDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(isWeightedDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(maxLengthDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(minLengthDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(isSampleSpaceDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(allowDuplicatesDiv);
  div.appendChild(document.createElement('br'));

  numDesignsDiv.style.visibility = 'hidden';
  isWeightedDiv.style.visibility = 'hidden';
  maxLengthDiv.style.visibility = 'hidden';
  minLengthDiv.style.visibility = 'hidden';
  isSampleSpaceDiv.style.visibility='hidden';
  allowDuplicatesDiv.style.visibility='hidden';

  $(enumerateDropdown).change(function() {
    if(this.value === endpoint.enumerate.ENUMERATE){
      numDesignsDiv.style.visibility = 'visible';
      isWeightedDiv.style.visibility = 'visible';
      maxLengthDiv.style.visibility = 'visible';
      minLengthDiv.style.visibility = 'visible';
      isSampleSpaceDiv.style.visibility='visible';
      allowDuplicatesDiv.style.visibility='visible';
    }
    if(this.value === endpoint.enumerate.SAMPLE){
      numDesignsDiv.style.visibility = 'visible';
      isWeightedDiv.style.visibility = 'visible';
      maxLengthDiv.style.visibility = 'visible';
      minLengthDiv.style.visibility = 'visible';
      isSampleSpaceDiv.style.visibility='visible';
      allowDuplicatesDiv.style.visibility='hidden';
    }
    if(this.value === endpoint.enumerate.CREATESAMPLESPACE){
      numDesignsDiv.style.visibility = 'hidden';
      isWeightedDiv.style.visibility = 'hidden';
      maxLengthDiv.style.visibility = 'hidden';
      minLengthDiv.style.visibility = 'hidden';
      isSampleSpaceDiv.style.visibility='hidden';
      allowDuplicatesDiv.style.visibility='hidden';
    }
    if(this.value === endpoint.enumerate.PARTANALYTICS){
      numDesignsDiv.style.visibility = 'hidden';
      isWeightedDiv.style.visibility = 'hidden';
      maxLengthDiv.style.visibility = 'hidden';
      minLengthDiv.style.visibility = 'hidden';
      isSampleSpaceDiv.style.visibility='hidden';
      allowDuplicatesDiv.style.visibility='hidden';
    }
  });

  swal({
    title: "Enumerate / Sample",
    buttons: true,
    content: div
  }).then((confirm) => {
    if (confirm) {

      let loadingDiv = document.createElement('div');
      let div = document.createElement('div');

      let numDesigns = numDesignsInput.value;
      let maxLength = maxLengthInput.value;
      let minLength = minLengthInput.value;
      let isWeighted = isWeightedInput.value;
      let isSampleSpace = isSampleSpaceInput.value;
      let allowDuplicates = allowDuplicatesInput.value;

      if (isSampleSpaceInput.checked) {
        isSampleSpace = "true";
      } else {
        isSampleSpace = "false";
      }

      if (isWeightedInput.checked) {
        isWeighted = "true";
      } else {
        isWeighted = "false";
      }

      if (allowDuplicatesInput.checked) {
        allowDuplicates = "true"
      } else {
        allowDuplicates = "false"
      }

      switch (enumerateDropdown.value) {
        case endpoint.enumerate.ENUMERATE:

          div.style.height = "inherit";
          div.style.overflow = "scroll";

          // loading div
          loadingDiv.appendChild(document.createTextNode("Loading..."));

          //append all
          div.appendChild(loadingDiv);

          swal({
            title: "Enumerated Designs",
            content: div,
            className: "enumeration-swal"
          });
          if (allowDuplicates === "true") {
            endpoint.enumerateDesignsList(currentSpace, numDesigns, minLength, maxLength, isWeighted, isSampleSpace, (err, data) => {
              if (err) {
                swalError("Enumeration error: " + JSON.stringify(err));
              } else {
                div.removeChild(loadingDiv);
                let para = document.createElement("p");
                para.appendChild(document.createTextNode("Allow Duplicates: True"))
                para.appendChild(document.createElement('br'));
                para.appendChild(document.createTextNode("Number of Designs: " + data.length.toString()))
                para.appendChild(document.createElement('br'));
                if (isSampleSpace === "true") {
                  para.appendChild(document.createTextNode("[part1, part2, ..., partn, probability]"))
                } else if (isWeighted === "true") {
                  para.appendChild(document.createTextNode("[part1, part2, ..., partn, average weight of parts]"))
                } else {
                  para.appendChild(document.createTextNode("[part1, part2, ..., partn]"))
                }
                para.appendChild(document.createElement('br'));
                para.appendChild(document.createElement('br'));
                data.map((list) => {
                  para.appendChild(document.createTextNode("["));
                  const length = list.length;
                  let placedFirstPart = false;
                  list.map((element, i) => {
                    if (splitElementID(element.isBlank) !== "true") {

                      if (placedFirstPart === true) {
                        para.appendChild(document.createTextNode(","));
                      } else {
                        placedFirstPart = true;
                      }
                      para.appendChild(document.createTextNode(splitElementID(element.id)));
                    }

                    // append stats
                    if (length === i+1 && isSampleSpace === "true"){
                      para.appendChild(document.createTextNode(","));
                      para.appendChild(document.createTextNode(element.probability));
                    } else if (length === i+1 && isWeighted === "true") {
                      para.appendChild(document.createTextNode(","));
                      para.appendChild(document.createTextNode(element.average_weight));
                    }
                  });
                  para.appendChild(document.createTextNode("]"));
                  para.appendChild(document.createElement('br'));
                });
          
                div.appendChild(para);
              }
            });

          } else {
            endpoint.enumerateDesignsSet(currentSpace, numDesigns, minLength, maxLength, isWeighted, isSampleSpace, (err, data) => {
              if (err) {
                swalError("Enumeration error: " + JSON.stringify(err));
              } else {
                div.removeChild(loadingDiv);
                let para = document.createElement("p");
                para.appendChild(document.createTextNode("Allow Duplicates: False"))
                para.appendChild(document.createElement('br'));
                para.appendChild(document.createTextNode("Number of Designs: " + data.length.toString()))
                para.appendChild(document.createElement('br'));
                if (isSampleSpace === "true") {
                  para.appendChild(document.createTextNode("[part1, part2, ..., partn, probability]"))
                } else if (isWeighted === "true") {
                  para.appendChild(document.createTextNode("[part1, part2, ..., partn, average weight of parts]"))
                } else {
                  para.appendChild(document.createTextNode("[part1, part2, ..., partn]"))
                }
                para.appendChild(document.createElement('br'));
                para.appendChild(document.createElement('br'));
                data.map((list) => {
                  para.appendChild(document.createTextNode("["));
                  const length = list.length;
                  let placedFirstPart = false;
                  list.map((element, i) => {
                    if (splitElementID(element.isBlank) !== "true") {

                      if (placedFirstPart === true) {
                        para.appendChild(document.createTextNode(","));
                      } else {
                        placedFirstPart = true;
                      }
                      para.appendChild(document.createTextNode(splitElementID(element.id)));
                    }

                    // append stats
                    if (length === i+1 && isSampleSpace === "true"){
                      para.appendChild(document.createTextNode(","));
                      para.appendChild(document.createTextNode(element.probability));
                    } else if (length === i+1 && isWeighted === "true") {
                      para.appendChild(document.createTextNode(","));
                      para.appendChild(document.createTextNode(element.average_weight));
                    }
                  });
                  para.appendChild(document.createTextNode("]"));
                  para.appendChild(document.createElement('br'));
                });
          
                div.appendChild(para);
              }
            });
            
          }
          
          break;

        case endpoint.enumerate.SAMPLE:

          div.style.height = "inherit";
          div.style.overflow = "scroll";

          // loading div
          loadingDiv.appendChild(document.createTextNode("Loading..."));

          //append all
          div.appendChild(loadingDiv);

          swal({
            title: "Sampled Designs",
            content: div,
            className: "enumeration-swal"
          });
          endpoint.sampleDesigns(currentSpace, numDesigns, minLength, maxLength, isWeighted, isSampleSpace, (err, data) => {
            if (err) {
              swalError("Sampling error: " + JSON.stringify(err));
            } else {
              div.removeChild(loadingDiv);
              let para = document.createElement("p");

              if (isSampleSpace === "true" || isWeighted === "true") {
                para.appendChild(document.createTextNode("[part1, part2, ..., partn, probability]"))
              } else {
                para.appendChild(document.createTextNode("[part1, part2, ..., partn]"))
              }

              para.appendChild(document.createElement('br'));
              para.appendChild(document.createElement('br'));
              data.map((list) => {
                para.appendChild(document.createTextNode("["));
                const length = list.length;
                list.map((element, i) => {
                  para.appendChild(document.createTextNode(splitElementID(element)));
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
          break;

        case endpoint.enumerate.CREATESAMPLESPACE:
          endpoint.createSampleSpace(currentSpace, (err, data) => {
            if (err) {
              swalError("Error While Creating Sample Space: " + JSON.stringify(err));
            } else {
              if (data === true) {
                swalSuccess("Sample Space Successfully Created")
              } else {
                swalSuccess("Sample Space Already Created")
              }
            }
          });
          break; 

        case endpoint.enumerate.PARTANALYTICS:

          div.style.height = "inherit";
          div.style.overflow = "scroll";

          // loading div
          loadingDiv.appendChild(document.createTextNode("Loading..."));

          //append all
          div.appendChild(loadingDiv);

          swal({
            title: "Part Analytics",
            content: div,
            className: "enumeration-swal"
          });
          endpoint.getPartAnalytics(currentSpace, (err, data) => {
            if (err) {
              swalError("Part Analytics ERROR: " + JSON.stringify(err));
            } else {
              div.removeChild(loadingDiv);
              let para = document.createElement("p");

              para.appendChild(document.createTextNode("MAP<String, MAP<String, Object>>"))

              para.appendChild(document.createElement('br'));
              para.appendChild(document.createElement('br'));
              
              Object.entries(data).forEach(([key, innerMap]) => {
              para.appendChild(document.createTextNode(`${key}: {`));

              Object.entries(innerMap).forEach(([innerKey, value]) => {
                  para.appendChild(document.createTextNode(`${innerKey}: ${JSON.stringify(value)}, `));
                  para.appendChild(document.createElement('br'));
              });

              para.appendChild(document.createTextNode("}"));
              para.appendChild(document.createElement('br'));
              para.appendChild(document.createElement('br'));
              });
        
              div.appendChild(para);
            }
          });
          break;
      }
    }
  }); 

});

function makeEnumerateDropdown(){
  let enumerateDropdown = document.createElement('select');
  let enumerateOption = new Option("please select", "", true, true);
  enumerateOption.disabled = true;
  enumerateDropdown.appendChild(enumerateOption);
  for(let key in endpoint.enumerate){
    enumerateDropdown.appendChild(new Option(endpoint.enumerate[key]));
  }

  return enumerateDropdown;
}

$('#best-path-tooltip').click(() => {
  let div = document.createElement('div');
  div.style.height = "inherit";
  div.style.overflow = "scroll";

  // loading div
  let loadingDiv = document.createElement('div');
  loadingDiv.appendChild(document.createTextNode("Loading..."));

  //append all
  div.appendChild(loadingDiv);

  swal({
    title: "Best Paths",
    content: div,
    className: "score-swal"
  });

  endpoint.getBestPath(currentSpace, (err, data1) => {
    if (err) {
      swalError("Graph error: " + JSON.stringify(err));
    } else {

      if (data1.length === 0) {
        swalError("Graph error: design space contains cycle")
      } else {
        div.removeChild(loadingDiv);
        let para = document.createElement("p");

        data1.map((list) => {
          para.appendChild(document.createElement('br'));
          para.appendChild(document.createTextNode('Total Weight of All Non-Blank Edges:'));
          para.appendChild(document.createElement('br'));

          para.appendChild(document.createTextNode(list[0].pathScore));
          para.appendChild(document.createElement('br'));
          para.appendChild(document.createElement('br'));

          para.appendChild(document.createTextNode("["));
          const length = list.length;
          list.map((element, i) => {
            para.appendChild(document.createTextNode(element.id));
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
    }
  });

});

$('#graph-score-tooltip').click(() => {
  let div = document.createElement('div');
  div.style.height = "inherit";
  
  // loading div
  let loadingDiv = document.createElement('div');
  loadingDiv.appendChild(document.createTextNode("Loading..."));

  //append all
  div.appendChild(loadingDiv);

  swal({
    title: "Graph Score",
    content: div,
    className: "score-swal"
  }); 

  endpoint.getGraphScore(currentSpace, (err, data) => {
    if (err) {
      swalError("Score error: " + JSON.stringify(err));
    } else {
      div.removeChild(loadingDiv);

      let para = document.createElement("p");
      para.appendChild(document.createTextNode('Total Weight of All Non-Blank Edges:'));
      para.appendChild(document.createElement('br'));
      
      para.appendChild(document.createTextNode(data[0]));
      para.appendChild(document.createElement('br'));

      para.appendChild(document.createTextNode('Total Weight of All Edges:'));
      para.appendChild(document.createElement('br'));
      
      para.appendChild(document.createTextNode(data[1]));
      para.appendChild(document.createElement('br'));

      para.appendChild(document.createTextNode('Average Weight of All Parts:'));
      para.appendChild(document.createElement('br'));
      
      para.appendChild(document.createTextNode(data[2]));
      para.appendChild(document.createElement('br'));

      para.appendChild(document.createTextNode('Average Weight of All Parts and Blank Edges:'));
      para.appendChild(document.createElement('br'));
      
      para.appendChild(document.createTextNode(data[3]));
      para.appendChild(document.createElement('br'));

      
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

  //weight tolerance div
  let weightTolDiv = document.createElement('div');
  let weightToleranceDropdown = makeWeightToleranceDropdown();
  makeDiv(weightTolDiv, weightToleranceDropdown, 'Weight Tolerance: ');

  //append all
  div.appendChild(inputDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(outputDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(operatorDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(tolDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(weightTolDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(optDiv);
  div.appendChild(document.createElement('br'));

  tolDiv.style.visibility = 'hidden';
  weightTolDiv.style.visibility = 'hidden';
  optDiv.style.visibility = 'hidden';

  $(operatorDropdown).change(function() {
    if(this.value === endpoint.operators.JOIN){
      tolDiv.style.visibility = 'hidden';
      weightTolDiv.style.visibility = 'hidden';
      optDiv.style.visibility = 'hidden';
    }
    if(this.value === endpoint.operators.OR){
      tolDiv.style.visibility = 'hidden';
      weightTolDiv.style.visibility = 'hidden';
      optDiv.style.visibility = 'hidden';
    }
    if(this.value === endpoint.operators.REPEAT){
      tolDiv.style.visibility = 'hidden';
      weightTolDiv.style.visibility = 'hidden';
      optDiv.style.visibility = 'visible';
    }
    if(this.value === endpoint.operators.AND){
      tolDiv.style.visibility = 'visible';
      weightTolDiv.style.visibility = 'hidden';
      optDiv.style.visibility = 'hidden';
      toleranceDropdown.value = 1;
    }
    if(this.value === endpoint.operators.MERGE){
      tolDiv.style.visibility = 'visible';
      weightTolDiv.style.visibility = 'visible';
      optDiv.style.visibility = 'hidden';
      toleranceDropdown.value = 0;
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
      let weightTolerance = weightToleranceDropdown.value;

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
          endpoint.designSpaceMerge(inputSpaces, outputSpace, tolerance, weightTolerance);
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
  toleranceDropdown.appendChild(new Option("4"));

  return toleranceDropdown;
}

function makeWeightToleranceDropdown(){
  let weightToleranceDropdown = document.createElement('select');
  weightToleranceDropdown.setAttribute("id", "weight-tolerance-dropdown");
  weightToleranceDropdown.appendChild(new Option("0", "0", true, true));
  weightToleranceDropdown.appendChild(new Option("1"));
  weightToleranceDropdown.appendChild(new Option("2"));

  return weightToleranceDropdown;
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

// $('#delete-branch-tooltip').click(() => {
//   // create DOM object to add to alert
//   let dropdown = document.createElement("select");
//   let branchOption = new Option("Branches", "", true, true);
//   branchOption.disabled = true;
//   dropdown.appendChild(branchOption);
//   populateBranchSelector(historyNodes, $(dropdown));
//   swal({
//     title: "Really delete?",
//     text: "Select the branch you want to delete (you cannot delete the current active branch)",
//     icon: "warning",
//     buttons: true,
//     content: dropdown
//   }).then((confirm) => {
//     if (confirm) {
//       let branchName = $(dropdown)[0].options[$(dropdown)[0].selectedIndex].value;
//       endpoint.deleteBranch(branchName);
//     }
//   });
// });

// $('#reset-commit-tooltip').click(() => {
//   swal({
//     title: "Really reset?",
//     text: "No history of the commit will remain (If you want to preserve history, use revert). ",
//     icon: "warning",
//     buttons: true
//   }).then((confirm) => {
//     if (confirm) {
//       endpoint.resetCommit();
//     }
//   });
// });

// $('#revert-commit-tooltip').click(() => {
//   swal({
//     title: "Really revert?",
//     text: "A new commit will be made from the previous design.",
//     icon: "warning",
//     buttons: true
//   }).then((confirm) => {
//     if (confirm) {
//       endpoint.revertCommit();
//     }
//   });
// });

// $('#make-commit-tooltip').click(() => {
//   endpoint.makeCommit();
// });

// $('#make-branch-tooltip').click(() => {
//   swal({
//     title: "Create branch",
//     text: "Enter a unique branch name",
//     content: "input",
//     buttons: true
//   }).then((branchName) => {
//     if (branchName){
//       endpoint.makeNewBranch(branchName);
//     }
//   });
// });

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
 * GOLDBAR Functions
 ************************/

// Download Only
$("#goldbarDownloadSbolBtn").click(function() {

  $('#spinner').removeClass('hidden'); // show spinner

  // Inputs
  let specification = editors.specEditor.getValue();
  let categories = editors.catEditor.getValue();
  let designName = document.getElementById('designNameInput').value;

  //replace all spaces and special characters for SBOL
  designName = designName.replace(/[^A-Z0-9]/ig, "_");

  // Constants
  const REPRESENTATION = 'EDGE';
  let numDesigns = 1;
  let maxCycles = 1;
  
  $.post('http://localhost:8082/postSpecs', {
    "designName": designName,
    "specification": specification,
    "categories": categories,
    "numDesigns": numDesigns,
    "maxCycles": maxCycles,
    "number": "2.0",
    "name": "specificationname",
    "clientid": "userid",
    "representation": REPRESENTATION
  }, function (data) {
    
    sbolDoc = data.sbol;

    try {
      endpoint.downloadSBOL(sbolDoc, "knox_" + designName + "_sbol.xml");
      $("#spinner").addClass('hidden');
      swalSuccess();
    } catch (error) {
      $("#spinner").addClass('hidden');
      swalError("Failed to Download");
    }

  }).fail((response) => {
    alert("Is Constellation-js running on Port:8082?");
    $("#spinner").addClass('hidden');
  });
});

// Import Only
$("#goldbarSubmitBtn").click(function() {

  $('#spinner').removeClass('hidden'); // show spinner

  // Inputs
  let specification = editors.specEditor.getValue();
  let categories = editors.catEditor.getValue();
  let designName = document.getElementById('designNameInput').value;
  let weight = document.getElementById('weightInput').value;

  //replace all spaces and special characters for SBOL
  designName = designName.replace(/[^A-Z0-9]/ig, "_");

  // Constants
  const REPRESENTATION = 'EDGE';
  let numDesigns = 1;
  let maxCycles = 1;
  
  $.post('http://localhost:8082/postSpecs', {
    "designName": designName,
    "specification": specification,
    "categories": categories,
    "numDesigns": numDesigns,
    "maxCycles": maxCycles,
    "number": "2.0",
    "name": "specificationname",
    "clientid": "userid",
    "representation": REPRESENTATION
  }, function (data) {
    
    sbolDoc = data.sbol;

    try {
      endpoint.importGoldbarSBOL(sbolDoc, weight);
      $("#spinner").addClass('hidden');
      swalSuccess("GOLDBAR Sucessfully Imported");
    } catch (error) {
      $("#spinner").addClass('hidden');
      swalError("Failed to Import");
    }

  }).fail((response) => {
    alert("Is Constellation-js running on Port:8082?");
    $("#spinner").addClass('hidden');
  });
});

// Import and Download
$("#goldbarSubmitAndDownloadBtn").click(function() {

  $('#spinner').removeClass('hidden'); // show spinner

  // Inputs
  let specification = editors.specEditor.getValue();
  let categories = editors.catEditor.getValue();
  let designName = document.getElementById('designNameInput').value;
  let weight = document.getElementById('weightInput').value;

  //replace all spaces and special characters for SBOL
  designName = designName.replace(/[^A-Z0-9]/ig, "_");

  // Constants
  const REPRESENTATION = 'EDGE';
  let numDesigns = 1;
  let maxCycles = 1;
  
  $.post('http://localhost:8082/postSpecs', {
    "designName": designName,
    "specification": specification,
    "categories": categories,
    "numDesigns": numDesigns,
    "maxCycles": maxCycles,
    "number": "2.0",
    "name": "specificationname",
    "clientid": "userid",
    "representation": REPRESENTATION
  }, function (data) {
    
    sbolDoc = data.sbol;

    try {
      endpoint.downloadSBOL(sbolDoc, "knox_" + designName + "_sbol.xml");
      endpoint.importGoldbarSBOL(sbolDoc, weight);
      $("#spinner").addClass('hidden');
      swalSuccess("GOLDBAR Sucessfully Imported and SBOL Downloaded");
    } catch (error) {
      $("#spinner").addClass('hidden');
      swalError();
    }

  }).fail((response) => {
    alert("Is Constellation-js running on Port:8082?");
    $("#spinner").addClass('hidden');
  });
});

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