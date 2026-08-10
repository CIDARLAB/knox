import Target from './target.js';
import * as endpoint from "./endpoints.js";
import {and0GOLDBAR, and1GOLDBAR_NORB, and1GOLDBAR_RB, and2GOLDBAR} from "./use-cases/cello-AND-circuits.js";
import {celloCategories} from "./use-cases/cello-categories.js";
import {rebeccamycinGOLDBAR, biosynthesisCategories} from "./use-cases/rebeccamycin.js";
import {reverseGOLDBAR} from "./use-cases/reverse.js";
import {exampleCategories, concreteGOLDBAR, abstractGOLDBAR, concreteAndAbstractGOLDBAR} from "./use-cases/small-example.js";
import {exampleRules, ruleCategories} from "./use-cases/rules-examples.js";
import {ruleTests} from './use-cases/rules-tests.js';
import {modelConfigs} from './model-configs/modelconfig.js';

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
//const GRAMMAR_DEF = [{'Seq':[{'Then':[['Exp'],'.',['Seq']]},{'Then':[['Exp'],'then',['Seq']]},{'':[['Exp']]}]},{'Exp':[{'Or':[['Term'],'or',['Exp']]},{'And0':[['Term'],'and0',['Exp']]}, {'And1':[['Term'],'and1',['Exp']]}, {'And2':[['Term'],'and2',['Exp']]},{'Merge':[['Term'],'merge',['Exp']]},{'':[['Term']]}]},{'Term':[{'OneOrMore':['one-or-more',['Term']]},{'ZeroOrMore':['zero-or-more',['Term']]},{'ZeroOrOne':['zero-or-one',['Term']]}, {'ReverseComp':['reverse-comp',['Term']]}, {'ForwardOrReverse':['forward-or-reverse',['Term']]},{'ZeroOrOneSBOL':['zero-or-one-sbol',['Term']]},{'ZeroOrMoreSBOL':['zero-or-more-sbol',['Term']]},{'':['{',['Seq'],'}']},{'':['(',['Seq'],')']},{'Atom':[{'RegExp':'([A-Za-z0-9]|-|_)+'}]}]}];

const exploreBtnIDs = {
  delete: "#delete-btn",
  combine: "#combine-btn",
  list: "#list-btn",
  sbol: "#sbol-btn",
  score: "#score-btn",
  bestPath: "#bestpath-btn",
  group: "#group-btn",
  rename: "#rename-btn",
  show: "#show-btn",
  goldbar: "#goldbar-btn",
  // save: "#save-btn",
};

const experimentBtnIDs = {
  createExperiment: "#create-experiment-btn",
  ml: "#ml-experiment-btn",
  exportExperiment: "#export-experiment-btn",
  deleteExperiment: "#delete-experiment-btn"
};

const allBtnIDs = {
  ...exploreBtnIDs,
  ...experimentBtnIDs,
  deleteGroup: "#deleteGroup-btn",
  ruleEval: "#rule-eval-btn",
  openTable: "#table-btn",
  seqCompiler: "#seqcompiler-btn"
};

export const knoxClass = {
  HEAD: "Head",
  BRANCH: "Branch",
  COMMIT: "Commit"
};

let historyNodes;
export let currentSpace;
export let currentGroupID;
export let currentExperimentID;
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

window.addEventListener('DOMContentLoaded', injectAIChatBox);

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
      getGroupID(currentSpace);
    }
  });
  visualizeHistory(spaceid);

  //autoshow history
  if(panelNum === 1){
    $('#vh-toggle-button').click();
  }
}

export function showGroupInfo(groupID) {
  // Set currentGroupID
  currentGroupID = groupID

  hideExplorePageBtns();

  endpoint.listGroupSpaceIDs(currentGroupID, (err, spaceids) => {
    if (err) {
      swalError(JSON.stringify(err));
    } else {
      // Construct Circle Graph for Group
      let graph = {"nodes":[], "links":[]};

      // Add center node (groupID)
      graph.nodes.push({id: currentGroupID, nodeTypes: ["start"]});

      // Append Number of Designs
      graph.nodes.push({id: `Total Designs: ${spaceids.length}`, nodeTypes: ["start"]})
      graph.links.push({source: 0, target: 1, show: true, componentRoles: [], componentIDs: [], weight: []}) // Make Link

      // Add spaceIDs from groupID
      spaceids.forEach((spaceid, index) => {
        let limit = 25;
        if (index < limit) {
          graph.nodes.push({id: spaceid, nodeTypes: []}) // Make spaceID Node
          graph.links.push({source: index+1, target: index+2, show: true, componentRoles: [], componentIDs: [], weight: []}) // Make Link
        }
      });

      if (graph.nodes.length > 3) {
        graph.links.push({source: graph.nodes.length - 1, target: 2, show: true, componentRoles: [], componentIDs: [], weight: []}) // Make Link
      }
      
      targets.search.clear();
      targets.search.setGraph(graph);
      $("#search-tb").blur();
      $("#search-autocomplete").blur();
    }
  });
}

export function showExperimentInfo(experimentID) {
  targets.search.clear();
  $("#search-tb").blur();
  $("#search-autocomplete").blur();

  // Set currentExperimentID
  currentExperimentID = experimentID
  const panel = document.getElementById("experiment-info-panel");
  if (!panel) {
    swalError("Missing #experiment-info-panel");
    return;
  }

  panel.innerHTML = "<p>Loading experiment...</p>";

  endpoint.getExperiment(experimentID, (err, experiment) => {
    if (err || !experiment) {
      swalError("Failed to load experiment: " + JSON.stringify(err || experimentID));
      panel.innerHTML = "<p>Failed to load experiment.</p>";
      return;
    }

    renderExperimentTabs(panel, experiment);
    loadExperimentTab(panel, "designs", experiment);

    // Navigate to Experiments tab
    $('a[href="#experiments"]').click();
    window.scrollTo(0, document.getElementById('experiments').offsetTop);
  });
}

function renderExperimentTabs(panel, experiment) {
  panel.innerHTML = `
    <div class="experiment-header">
      <h3>Experiment Name: ${escapeHtml(experiment.experimentName || "Experiment")}</h3>
      <p>Description: ${escapeHtml(experiment.experimentDescription || "")}</p>
    </div>

    <ul class="nav nav-tabs" id="experiment-tabs">
      <li class="active"><a href="#" data-tab="designs">Designs</a></li>
      <li><a href="#" data-tab="rules">Rules</a></li>
      <li><a href="#" data-tab="rulesToEval">RulesToEval</a></li>
      <li><a href="#" data-tab="parts">Parts</a></li>
      <li><a href="#" data-tab="jobs">Jobs</a></li>
    </ul>

    <div class="tab-pane">
      <div id="experiment-table-container"></div>
    </div>
  `;

  panel.querySelectorAll("#experiment-tabs a").forEach(link => {
    link.addEventListener("click", (event) => {
      event.preventDefault();

      panel.querySelectorAll("#experiment-tabs li").forEach(li => li.classList.remove("active"));
      link.parentElement.classList.add("active");

      loadExperimentTab(panel, link.dataset.tab, experiment);
    });
  });
}

function loadExperimentTab(panel, tabName, experiment) {
  const container = panel.querySelector("#experiment-table-container");
  container.innerHTML = "<p>Loading...</p>";

  if (tabName === "designs") {
    endpoint.listGroupSpaceIDs(experiment.designsGroupID || "", (err, data) => {
      if (err) {
        container.innerHTML = "<p>Failed to load designs.</p>";
        return;
      }
      const count = (data || []).length;
      const groupID = experiment.designsGroupID || "N/A";
      
      // Add summary info
      let summary = `<div style="margin-bottom: 15px; padding: 10px; background-color: #f5f5f5; border-radius: 4px;">
        <strong>Group ID:</strong> ${escapeHtml(groupID)} | <strong>Total Designs:</strong> ${count}
      </div>`;
      
      container.innerHTML = summary;
      renderSimpleTable(container, "Design Space ID", data || []);
    });
    return;
  }

  if (tabName === "rules") {
    endpoint.listGroupSpaceIDs(experiment.rulesGroupID || "", (err, data) => {
      if (err) {
        container.innerHTML = "<p>Failed to load rules.</p>";
        return;
      }
      const count = (data || []).length;
      const groupID = experiment.rulesGroupID || "N/A";
      
      // Add summary info
      let summary = `<div style="margin-bottom: 15px; padding: 10px; background-color: #f5f5f5; border-radius: 4px;">
        <strong>Group ID:</strong> ${escapeHtml(groupID)} | <strong>Total Rules:</strong> ${count}
      </div>`;
      
      container.innerHTML = summary;
      renderSimpleTable(container, "Rule Space ID", data || []);
    });
    return;
  }

  if (tabName === "rulesToEval") {
    endpoint.listGroupSpaceIDs(experiment.rulesToEvalGroupID || "", (err, data) => {
      if (err) {
        container.innerHTML = "<p>Failed to load rules to eval.</p>";
        return;
      }
      const count = (data || []).length;
      const groupID = experiment.rulesToEvalGroupID || "N/A";
      
      // Add summary info
      let summary = `<div style="margin-bottom: 15px; padding: 10px; background-color: #f5f5f5; border-radius: 4px;">
        <strong>Group ID:</strong> ${escapeHtml(groupID)} | <strong>Total Rules to Eval:</strong> ${count}
      </div>`;
      
      container.innerHTML = summary;
      renderSimpleTable(container, "RulesToEval Space ID", data || []);
    });
    return;
  }

  if (tabName === "parts") {
    const componentIDs = experiment.componentIDs || [];
    const componentRoles = experiment.componentRoles || [];
    const componentSequences = experiment.componentSequences || [];
    const componentDescriptions = experiment.componentDescriptions || [];

    const count = componentIDs.length;
    const groupID = experiment.partLibraryName || "N/A";
    
    // Build rows pairing IDs and Roles
    const rows = componentIDs.map((id, index) => ({
      id: id,
      role: componentRoles[index] || "",
      sequence: componentSequences[index] || "",
      description: componentDescriptions[index] || "",
      index: index + 1
    }));

    // Add summary info
    let summary = `<div style="margin-bottom: 15px; padding: 10px; background-color: #f5f5f5; border-radius: 4px;">
      <strong>Name:</strong> ${escapeHtml(groupID)} | <strong>Total Parts:</strong> ${count}
    </div>`;
    
    container.innerHTML = summary;
    
    renderPartsTable(container, rows);
    return;
  }

  if (tabName === "jobs") {
    renderJobsTable(container, experiment.jobs || [], experiment);
  }
}

function renderJobsTable(container, jobs, experimentData = null) {
  const refreshButton = `
    <div style="display: flex; justify-content: flex-end; margin-bottom: 10px;">
      <button type="button" class="btn btn-default btn-sm refresh-jobs-btn">
        <span class="glyphicon glyphicon-refresh"></span> Refresh
      </button>
    </div>
  `;

  const reloadJobs = () => {
    if (!currentExperimentID) return;

    container.innerHTML = "<p>Loading...</p>";
    endpoint.getExperiment(currentExperimentID, (err, experiment) => {
      if (err) {
        container.innerHTML = `${refreshButton}<p>Failed to refresh jobs.</p>`;
        return;
      }
      renderJobsTable(container, experiment.jobs || [], experiment);
    });
  };

  if (!jobs || jobs.length === 0) {
    container.innerHTML = `${refreshButton}<p>No jobs submitted yet.</p>`;
    container.querySelector('.refresh-jobs-btn')?.addEventListener('click', reloadJobs);
    return;
  }

  const rows = jobs.map(job => {
    const jobID = String(job.jobID || "");
    return `
      <tr>
        <td>
          <button
            type="button"
            class="btn btn-link btn-xs job-delete-btn"
            title="Delete job"
            data-job-id="${escapeHtml(jobID)}"
            style="padding: 0; color: #d9534f;"
          >
            <span class="glyphicon glyphicon-trash"></span>
          </button>
        </td>
        <td>${escapeHtml(String(job.runName || ""))}</td>
        <td>${escapeHtml(String(job.model || ""))}</td>
        <td>${escapeHtml(String(job.status || ""))}</td>
        <td>${escapeHtml(String(job.mlflowRunID || ""))}</td>
        <td>${escapeHtml(String(job.errorMessage || ""))}</td>
      </tr>
    `;
  }).join("");

  container.innerHTML = `${refreshButton}
    <div class="table-wrap">
      <table class="table table-striped">
        <thead>
          <tr>
            <th style="width: 40px;"></th>
            <th>Run Name</th>
            <th>Model</th>
            <th>Status</th>
            <th>MLflow Run ID</th>
            <th>Error</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>
    </div>
  `;

  container.querySelector('.refresh-jobs-btn')?.addEventListener('click', reloadJobs);

  container.querySelectorAll(".job-delete-btn").forEach(button => {
    button.addEventListener("click", () => {
      const jobID = button.getAttribute("data-job-id");
      if (!jobID) return;

      swal({
        title: "Delete this job?",
        text: "This cannot be undone.",
        icon: "warning",
        buttons: true
      }).then((confirm) => {
        if (!confirm) return;

        endpoint.deleteJob(jobID, () => {
          showExperimentInfo(currentExperimentID);
        });
      });
    });
  });
}

function renderSimpleTable(container, header, rows, emptyMessage = "No records found.") {
  if (!rows || rows.length === 0) {
    container.innerHTML += `<p>${escapeHtml(emptyMessage)}</p>`;
    return;
  }

  const body = rows.map(row => `
    <tr>
      <td>${escapeHtml(String(row))}</td>
    </tr>
  `).join("");

  container.innerHTML += `
    <div class="table-wrap">
      <table class="table table-striped">
        <thead>
          <tr><th>${escapeHtml(header)}</th></tr>
        </thead>
        <tbody>${body}</tbody>
      </table>
    </div>
  `;
}

function renderPartsTable(container, rows, emptyMessage = "No parts data.") {
  if (!rows || rows.length === 0) {
    container.innerHTML += `<p>${escapeHtml(emptyMessage)}</p>`;
    return;
  }

  const body = rows.map(row => `
    <tr>
      <td>${escapeHtml(String(row.id))}</td>
      <td>${escapeHtml(String(row.role))}</td>
      <td><div class="sequence-cell">${escapeHtml(String(row.sequence))}</div></td>
      <td>${escapeHtml(String(row.description))}</td>
      <td>${escapeHtml(String(row.index))}</td>
    </tr>
  `).join("");

  container.innerHTML += `
    <div class="table-wrap">
      <table class="table table-striped">
        <thead>
          <tr><th>Component ID</th><th>Component Role</th><th>Sequence</th><th>Description</th><th>Index</th></tr>
        </thead>
        <tbody>${body}</tbody>
      </table>
    </div>
  `;
}

function escapeHtml(value) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

// TODO: Clear Experiment Dashboard

function getGroupID(inputSpace) {
  endpoint.getGroupID(inputSpace, (err, data) => {
    if (err) {
      swalError("Get Group ID error: " + JSON.stringify(err));
      
    } else {
      currentGroupID = data.groupID;
    }
  });
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
    case "http://identifiers.org/so/SO:0002223":
      return sbolpath + "spacer.svg";
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

/*****************************
 * GOLDBAR Use Cases FUNCTIONS
 *****************************/
$('#concrete').on('click', function() {
  document.getElementById('designNameInput').value = "concrete-example";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(concreteGOLDBAR);
  editors.catEditor.setValue(JSON.stringify(exampleCategories));
});

$('#abstract').on('click', function() {
  document.getElementById('designNameInput').value = "abstract-example";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(abstractGOLDBAR);
  editors.catEditor.setValue(JSON.stringify(exampleCategories));
});

$('#concreteAndAbstract').on('click', function() {
  document.getElementById('designNameInput').value = "concrete-and-abstract-example";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(concreteAndAbstractGOLDBAR);
  editors.catEditor.setValue(JSON.stringify(exampleCategories));
});
$('#and0-option').on('click', function() {
  document.getElementById('designNameInput').value = "cello-AND-example";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(and0GOLDBAR);
  editors.catEditor.setValue(JSON.stringify(celloCategories));
});

$('#and1-option-norb').on('click', function() {
  document.getElementById('designNameInput').value = "cello-no-roadblocking-example";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(and1GOLDBAR_NORB);
  editors.catEditor.setValue(JSON.stringify(celloCategories));
});

$('#and1-option-rb').on('click', function() {
  document.getElementById('designNameInput').value = "cello-roadblocking-example";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(and1GOLDBAR_RB);
  editors.catEditor.setValue(JSON.stringify(celloCategories));
});

$('#and2-option').on('click', function() {
  document.getElementById('designNameInput').value = "cello-AND-example2";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(and2GOLDBAR);
  editors.catEditor.setValue(JSON.stringify(celloCategories));
});

$('#merge-option').on('click', function() {
  document.getElementById('designNameInput').value = "rebeccamycin-example";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(rebeccamycinGOLDBAR);
  editors.catEditor.setValue(JSON.stringify(biosynthesisCategories));
});

$('#reverse-option').on('click', function() {
  document.getElementById('designNameInput').value = "reverse-complement-example";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(reverseGOLDBAR);
  editors.catEditor.setValue(JSON.stringify(celloCategories));
});

let debugCats = '{"promoter": {"promoter": ["BBa_R0040", "BBa_J23100"]},\n ' +
  '"ribosomeBindingSite": {"ribosomeBindingSite": ["BBa_B0032", "BBa_B0034"]}, \n' +
  '"cds": {"cds": ["BBa_E0040", "BBa_E1010"]},\n"nonCodingRna": {"nonCodingRna": ["BBa_F0010"]},\n' +
  '"terminator": {"terminator": ["BBa_B0010"]}}'

$('#oOM-option').on('click', function() {
  document.getElementById('designNameInput').value = "one-or-more-exampleI";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue('one-or-more (promoter or ribosomeBindingSite) then (zero-or-more cds) then terminator');
  editors.catEditor.setValue(debugCats);
});

$('#oOM-option2').on('click', function() {
  document.getElementById('designNameInput').value = "one-or-more-exampleII";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue('one-or-more (promoter then zero-or-one(ribosomeBindingSite) then cds then terminator)');
  editors.catEditor.setValue(debugCats);
});

$('#doNotRepeatRule').on('click', function() {
  document.getElementById('designNameInput').value = "do-not-repeat-A";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(exampleRules.R);
  editors.catEditor.setValue(JSON.stringify(ruleCategories));
});

$('#beforeRule').on('click', function() {
  document.getElementById('designNameInput').value = "A-before-B";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(exampleRules.B);
  editors.catEditor.setValue(JSON.stringify(ruleCategories));
});

$('#togetherRule').on('click', function() {
  document.getElementById('designNameInput').value = "A-and-B-together";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(exampleRules.T);
  editors.catEditor.setValue(JSON.stringify(ruleCategories));
});

$('#pjiRule').on('click', function() {
  document.getElementById('designNameInput').value = "A-notFollowedBy-B";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(exampleRules.I);
  editors.catEditor.setValue(JSON.stringify(ruleCategories));
});

$('#fRule').on('click', function() {
  document.getElementById('designNameInput').value = "A-FollowedBy-B";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(exampleRules.F);
  editors.catEditor.setValue(JSON.stringify(ruleCategories));
});

$('#aRule').on('click', function() {
  document.getElementById('designNameInput').value = "A-onlyAfter-B";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(exampleRules.A);
  editors.catEditor.setValue(JSON.stringify(ruleCategories));
});

$('#mustIncludeRule').on('click', function() {
  document.getElementById('designNameInput').value = "mustInclude-A";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(exampleRules.M);
  editors.catEditor.setValue(JSON.stringify(ruleCategories));
});

$('#notIncludeRule').on('click', function() {
  document.getElementById('designNameInput').value = "notInclude-A";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(exampleRules.NI);
  editors.catEditor.setValue(JSON.stringify(ruleCategories));
});

$('#endRule').on('click', function() {
  document.getElementById('designNameInput').value = "mustEndWith-A";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(exampleRules.E);
  editors.catEditor.setValue(JSON.stringify(ruleCategories));
});

$('#startRule').on('click', function() {
  document.getElementById('designNameInput').value = "mustStartWith-A";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(exampleRules.S);
  editors.catEditor.setValue(JSON.stringify(ruleCategories));
});

$('#notOrthogonalRule').on('click', function() {
  document.getElementById('designNameInput').value = "A-and-B-notTogether";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(exampleRules.O);
  editors.catEditor.setValue(JSON.stringify(ruleCategories));
});

$('#lengthRule').on('click', function() {
  document.getElementById('designNameInput').value = "length-4-parts";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(exampleRules.N);
  editors.catEditor.setValue(JSON.stringify(ruleCategories));
});

$('#leakyRule').on('click', function() {
  document.getElementById('designNameInput').value = "leakyTerminators";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(exampleRules.L);
  editors.catEditor.setValue(JSON.stringify(ruleCategories));
});

$('#roadBlockingRule').on('click', function() {
  document.getElementById('designNameInput').value = "no-promoter-roadBlocking";
  document.getElementById('groupIDInput').value = "example";
  editors.specEditor.setValue(exampleRules.P);
  editors.catEditor.setValue(JSON.stringify(ruleCategories));
});

export function setGOLDBARTab(goldbar, categories, designName) {
  document.getElementById('designNameInput').value = designName + "_Rules";
  document.getElementById('groupIDInput').value = "goldbar_generator";
  editors.specEditor.setValue(JSON.stringify(goldbar));
  editors.catEditor.setValue(categories);
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

  let deleteGroupBtn = $('#deleteGroup-btn');
  deleteGroupBtn.tooltipster({
    content: $('#delete-group-tooltip'),
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

  let groupBtn = $('#group-btn');
  groupBtn.tooltipster({
    content: $('#graph-group-tooltip'),
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

  let renameBtn = $('#rename-btn');
  renameBtn.tooltipster({
    content: $('#rename-design-space-tooltip'),
    side: 'top',
    interactive: true,
    theme: 'tooltipster-noir'
  });

  let showBtn = $('#show-btn');
  showBtn.tooltipster({
    content: $('#show-sbol-tooltips'),
    side: 'top',
    interactive: true,
    theme: 'tooltipster-noir'
  });

  let tableBtn = $('#table-btn');
  tableBtn.tooltipster({
    content: $('#table-tooltip'),
    side: 'top',
    interactive: true,
    theme: 'tooltipster-noir'
  });

  let ruleEvalBtn = $('#rule-eval-btn');
  ruleEvalBtn.tooltipster({
    content: $('#rule-eval-tooltip'),
    side: 'top',
    interactive: true,
    theme: 'tooltipster-noir'
  });

  let goldbarBtn = $('#goldbar-btn');
  goldbarBtn.tooltipster({
    content: $('#goldbar-tooltip'),
    side: 'top',
    interactive: true,
    theme: 'tooltipster-noir'
  });

  let experimentBtn = $('#create-experiment-btn');
  experimentBtn.tooltipster({
    content: $('#create-experiment-tooltip'),
    side: 'top',
    interactive: true,
    theme: 'tooltipster-noir'
  });

  let experimentLearningBtn = $('#ml-experiment-btn');
  experimentLearningBtn.tooltipster({
    content: $('#ml-experiment-tooltip'),
    side: 'top',
    interactive: true,
    theme: 'tooltipster-noir'
  });

  let exportExperimentBtn = $('#export-experiment-btn');
  exportExperimentBtn.tooltipster({
    content: $('#export-experiment-tooltip'),
    side: 'top',
    interactive: true,
    theme: 'tooltipster-noir'
  });

  let deleteExperimentBtn = $('#delete-experiment-btn');
  deleteExperimentBtn.tooltipster({
    content: $('#delete-experiment-tooltip'),
    side: 'top',
    interactive: true,
    theme: 'tooltipster-noir'
  });

  let seqCompilerBtn = $('#seqcompiler-btn');
  seqCompilerBtn.tooltipster({
    content: $('#seqcompiler-tooltip'),
    side: 'top',
    interactive: true,
    theme: 'tooltipster-noir'
  });
}

function bindTooltipToButton(buttonSelector, tooltipSelector) {
  $(tooltipSelector).trigger('click');

  $(buttonSelector).on('click', function (e) {
    e.preventDefault();
    e.stopPropagation();
    $(tooltipSelector).trigger('click');
  });
}

bindTooltipToButton(allBtnIDs.openTable, '#table-tooltip');
$('#table-tooltip').click((e) => {
  window.open('/purityTable.html', '_blank');
});

bindTooltipToButton(allBtnIDs.show, '#show-sbol-tooltips');
$('#show-sbol-tooltips').click(() => {
  targets.search.showTooltips();
});

//bindTooltipToButton(allBtnIDs.export, '#export-sbol-tooltip');
$('#export-sbol-tooltip').click(() => {
  endpoint.exportDesign();
});

bindTooltipToButton(allBtnIDs.rename, '#rename-design-space-tooltip');
$('#rename-design-space-tooltip').click(() => {
  let div = document.createElement('div');

  // current spaceID
  let currentSpaceIDDiv = document.createElement('div');
  makeDiv(currentSpaceIDDiv, document.createTextNode(currentSpace), "Current Space ID: ");

  // rename spaceID div
  let renameDiv = document.createElement('div');
  let renameInput = document.createElement('input');
  makeDiv(renameDiv, renameInput, 'New Space ID: ');

  // append all
  div.appendChild(currentSpaceIDDiv)
  div.appendChild(document.createElement('br'));
  div.appendChild(document.createElement('br'));
  div.appendChild(renameDiv);
  div.appendChild(document.createElement('br'));

  swal({
    title: "Space ID",
    buttons: true,
    content: div
  }).then((confirm) => {
    if (confirm) {

      let newSpaceID = renameInput.value;

      endpoint.renameDesignSpace(currentSpace, newSpaceID, (err, data) => {
        if (err && !isEmptyObject(err)) {
          swalError("Rename failed: " + JSON.stringify(err));
        } else if (JSON.stringify(data.result).startsWith('"ERROR')) {
          document.getElementById('search-tb').value = newSpaceID;
          visualizeDesignAndHistory(newSpaceID);
          swalError("Rename failed: " + JSON.stringify(data.result));
        } else {
          document.getElementById('search-tb').value = newSpaceID;
          visualizeDesignAndHistory(newSpaceID);
          swalSuccess(JSON.stringify(data.result));
        }
      });
    }
  });
});

// Individual Groups
bindTooltipToButton(allBtnIDs.group, '#graph-group-tooltip');
$('#graph-group-tooltip').click(() => {
  let div = document.createElement('div');

  // current groupID
  let currentGroupDiv = document.createElement('div');
  makeDiv(currentGroupDiv, document.createTextNode(currentGroupID), "Current Group ID: ");

  // graph group info dropdown
  let groupInfoDiv = document.createElement('div');
  let groupInfoDivDropdown = makeGroupInfoDropdown();
  makeDiv(groupInfoDiv, groupInfoDivDropdown, 'Options: ');

  // group ID div
  let groupDiv = document.createElement('div');
  let groupIDInput = document.createElement('input');
  makeDiv(groupDiv, groupIDInput, 'Group ID: ');

  // append all
  div.appendChild(currentGroupDiv)
  div.appendChild(document.createElement('br'));
  div.appendChild(document.createElement('br'));
  div.appendChild(groupInfoDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(groupDiv);
  div.appendChild(document.createElement('br'));

  setRowVisible(groupDiv, false);

  $(groupInfoDivDropdown).change(function() {
    if(this.value === endpoint.groupInfo.GETGROUPSIZE){
      setRowVisible(groupDiv, false);
    }
    if(this.value === endpoint.groupInfo.SETGROUPID){
      setRowVisible(groupDiv, true);
    }
  });

  swal({
    title: "Graph Group Info",
    buttons: true,
    content: div
  }).then((confirm) => {
    if (confirm) {
      
      let loadingDiv = document.createElement('div');
      let div = document.createElement('div');

      let groupID = groupIDInput.value;

      switch (groupInfoDivDropdown.value) {
        case endpoint.groupInfo.SETGROUPID:
          endpoint.setGroupID(groupID);
          break;
      }

      switch (groupInfoDivDropdown.value) {
        case endpoint.groupInfo.GETGROUPSIZE:
          let div = document.createElement('div');
          swal({
            title: "Group Size for " + currentGroupID,
            content: div,
            className: "enumeration-swal"
          });
          endpoint.getGroupSize(currentGroupID, (err, data) => {
            if (err) {
              swalError("Get Group Size error: " + JSON.stringify(err));
            } else {
              let para = document.createElement("p");
              para.appendChild(document.createTextNode(data.groupSize));
              div.appendChild(para);
            }
          });
          break;
      }
    }
  });

});

function makeGroupInfoDropdown(){
  let groupInfoDropdown = document.createElement('select');
  let groupInfoOption = new Option("please select", "", true, true);
  groupInfoOption.disabled = true;
  groupInfoDropdown.appendChild(groupInfoOption);
  for(let key in endpoint.groupInfo){
    groupInfoDropdown.appendChild(new Option(endpoint.groupInfo[key]));
  }

  return groupInfoDropdown;
}

bindTooltipToButton(allBtnIDs.list, '#enumerate-designs-tooltip');
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
  makeDiv(numDesignsDiv, numDesignsInput, 'Number of Designs (0 means all possible for Enumerate): ');

  // min length div
  let minLengthDiv = document.createElement('div');
  let minLengthInput = document.createElement('input');
  minLengthInput.setAttribute("type", "number");
  minLengthInput.setAttribute("value", "1");
  minLengthInput.setAttribute("min", "0");
  makeDiv(minLengthDiv, minLengthInput, 'Minimum Length of Designs: ');

  // max length div
  let maxLengthDiv = document.createElement('div');
  let maxLengthInput = document.createElement('input');
  maxLengthInput.setAttribute("type", "number");
  maxLengthInput.setAttribute("value", "0");
  maxLengthInput.setAttribute("min", "0");
  makeDiv(maxLengthDiv, maxLengthInput, 'Maximum Length of Designs (0 means no Max): ');

  // max cycles div
  let maxCyclesDiv = document.createElement('div');
  let maxCyclesInput = document.createElement('input');
  maxCyclesInput.setAttribute("type", "number");
  maxCyclesInput.setAttribute("value", "0");
  maxCyclesInput.setAttribute("min", "0");
  makeDiv(maxCyclesDiv, maxCyclesInput, 'Maximum Cycles: ');

  // is weighted div
  let isWeightedDiv = document.createElement('div');
  let isWeightedInput = document.createElement('input');
  isWeightedInput.setAttribute("type", "checkbox");
  //isWeightedInput.checked = "true";
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

  // allow bfs or dfs div
  let BFSDiv = document.createElement('div');
  let BFSInput = document.createElement('input');
  BFSInput.setAttribute("type", "checkbox");
  makeDiv(BFSDiv, BFSInput, 'BFS? (otherwise DFS): ');

  // group ID div
  let groupDiv = document.createElement('div');
  let groupIDInput = document.createElement('input');
  makeDiv(groupDiv, groupIDInput, 'Group ID: ');

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
  div.appendChild(maxCyclesDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(isSampleSpaceDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(allowDuplicatesDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(BFSDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(groupDiv);
  div.appendChild(document.createElement('br'));

  setRowVisible(numDesignsDiv, false);
  setRowVisible(isWeightedDiv, false);
  setRowVisible(maxLengthDiv, false);
  setRowVisible(minLengthDiv, false);
  setRowVisible(maxCyclesDiv, false);
  setRowVisible(isSampleSpaceDiv, false);
  setRowVisible(allowDuplicatesDiv, false);
  setRowVisible(BFSDiv, false);
  setRowVisible(groupDiv, false);

  $(enumerateDropdown).change(function() {
    if(this.value === endpoint.enumerate.ENUMERATE){
      setRowVisible(numDesignsDiv, true);
      numDesignsInput.value = "0";
      setRowVisible(isWeightedDiv, true);
      setRowVisible(maxLengthDiv, true);
      setRowVisible(minLengthDiv, true);
      setRowVisible(maxCyclesDiv, true);
      setRowVisible(isSampleSpaceDiv, true);
      setRowVisible(allowDuplicatesDiv, true);
      setRowVisible(BFSDiv, true);
      setRowVisible(groupDiv, false);
    }
    if(this.value === endpoint.enumerate.SAMPLE){
      setRowVisible(numDesignsDiv, true);
      numDesignsInput.value = "1";
      setRowVisible(isWeightedDiv, true);
      setRowVisible(maxLengthDiv, true);
      setRowVisible(minLengthDiv, true);
      setRowVisible(maxCyclesDiv, false);
      setRowVisible(isSampleSpaceDiv, true);
      setRowVisible(allowDuplicatesDiv, false);
      setRowVisible(BFSDiv, false);
      setRowVisible(groupDiv, false);
    }
    if(this.value === endpoint.enumerate.CREATESAMPLESPACE){
      setRowVisible(numDesignsDiv, false);
      setRowVisible(isWeightedDiv, false);
      setRowVisible(maxLengthDiv, false);
      setRowVisible(minLengthDiv, false);
      setRowVisible(maxCyclesDiv, false);
      setRowVisible(isSampleSpaceDiv, false);
      setRowVisible(allowDuplicatesDiv, false);
      setRowVisible(BFSDiv, false);
      setRowVisible(groupDiv, true);
    }
    if(this.value === endpoint.enumerate.PARTANALYTICS){
      setRowVisible(numDesignsDiv, false);
      setRowVisible(isWeightedDiv, false);
      setRowVisible(maxLengthDiv, false);
      setRowVisible(minLengthDiv, false);
      setRowVisible(maxCyclesDiv, false);
      setRowVisible(isSampleSpaceDiv, false);
      setRowVisible(allowDuplicatesDiv, false);
      setRowVisible(BFSDiv, false);
      setRowVisible(groupDiv, false);
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
      let maxCycles = maxCyclesInput.value;
      let isWeighted = isWeightedInput.value;
      let isSampleSpace = isSampleSpaceInput.value;
      let allowDuplicates = allowDuplicatesInput.value;
      let bfs = BFSInput.value;
      let groupID = groupIDInput.value;

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

      if (BFSInput.checked) {
        bfs = "true"
      } else {
        bfs = "false"
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
          endpoint.enumerateDesigns(currentSpace, numDesigns, minLength, maxLength, maxCycles, bfs, isWeighted, isSampleSpace, allowDuplicates, (err, data) => {
            if (err) {
              swalError("Enumeration error: " + JSON.stringify(err));
            } else {
              div.removeChild(loadingDiv);
              
              // add export button
              const exportBtn = document.createElement('button');
              exportBtn.textContent = "Export All to CSV";
              exportBtn.onclick = function() {
                endpoint.enumerateCSV(currentSpace, numDesigns, minLength, maxLength, maxCycles, bfs, isWeighted, isSampleSpace, allowDuplicates);
              };
              div.appendChild(exportBtn);

              let para = document.createElement("p");
              para.appendChild(document.createElement('br'));
              para.appendChild(document.createElement('br'));
              para.appendChild(document.createTextNode("Allow Duplicates: " + allowDuplicates.toString()));
              para.appendChild(document.createElement('br'));
              para.appendChild(document.createTextNode("Number of Designs: " + data.numDesigns.toString()))
              para.appendChild(document.createElement('br'));

              // limit number of designs shown to DESIGN_LIMIT
              const DESIGN_LIMIT = data.designs.length;
              if (data.numDesigns > DESIGN_LIMIT) {
                para.appendChild(document.createElement('br'));
                para.appendChild(document.createElement('br'));
                para.appendChild(document.createTextNode("Only showing up to " + DESIGN_LIMIT + " designs"));
                para.appendChild(document.createElement('br'));
                para.appendChild(document.createElement('br'));
              }

              if (isSampleSpace === "true") {
                para.appendChild(document.createTextNode("[part1, part2, ..., partn, probability]"))
              } else if (isWeighted === "true") {
                para.appendChild(document.createTextNode("[part1, part2, ..., partn, average weight of parts]"))
              } else {
                para.appendChild(document.createTextNode("[part1, part2, ..., partn]"))
              }
              para.appendChild(document.createElement('br'));
              para.appendChild(document.createElement('br'));
              data.designs.map((list) => {
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

                    if (element.orientation === "reverseComplement") {
                      para.appendChild(document.createTextNode(splitElementID(element.id) + "_REVERSE"));
                    } else {
                      para.appendChild(document.createTextNode(splitElementID(element.id)));
                    }
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
          endpoint.createSampleSpace(currentSpace, groupID, (err, data) => {
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

bindTooltipToButton(allBtnIDs.bestPath, '#best-path-tooltip');
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

bindTooltipToButton(allBtnIDs.score, '#graph-score-tooltip');
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

bindTooltipToButton(allBtnIDs.goldbar, '#goldbar-tooltip');
$('#goldbar-tooltip').click(() => {
  let div = document.createElement('div');
  div.style.height = "inherit";
  
  // loading div
  let loadingDiv = document.createElement('div');
  loadingDiv.appendChild(document.createTextNode("Loading..."));

  //append all
  div.appendChild(loadingDiv);

  swal({
    title: "GOLDBAR",
    content: div,
    className: "goldbar-swal"
  }); 

  endpoint.getGoldbar(currentSpace, (err, data) => {
    if (err) {
      swalError("GOLDBAR error: " + JSON.stringify(err));
    } else {
      div.removeChild(loadingDiv);

      let para = document.createElement("p");
      para.appendChild(document.createTextNode("Current Space: " + currentSpace));
      para.appendChild(document.createElement('br'));
      para.appendChild(document.createElement('br'));
      para.appendChild(document.createTextNode(data.goldbar));
      para.appendChild(document.createElement('br'));
      
      div.appendChild(para);
    }
  });

});

bindTooltipToButton(allBtnIDs.combine, '#apply-operators-tooltip');
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

  // group ID div
  let groupDiv = document.createElement('div');
  let groupIDInput = document.createElement('input');
  makeDiv(groupDiv, groupIDInput, 'Output group ID: ');
  if (currentGroupID) {
    groupIDInput.value = currentGroupID;
  }

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

  //reverse orientation div
  let reverseOrientationDiv = document.createElement('div');
  let reverseOrientationDropdown = makeReverseOrientationDropdown();
  makeDiv(reverseOrientationDiv, reverseOrientationDropdown, 'Reverse Part Orientations?: ');

  //append all
  div.appendChild(inputDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(outputDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(groupDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(operatorDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(tolDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(weightTolDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(optDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(reverseOrientationDiv);
  div.appendChild(document.createElement('br'));

  setRowVisible(tolDiv, false);
  setRowVisible(weightTolDiv, false);
  setRowVisible(optDiv, false);
  setRowVisible(reverseOrientationDiv, false);

  $(operatorDropdown).change(function() {
    if(this.value === endpoint.operators.JOIN){
      setRowVisible(inputDiv, true);
      setRowVisible(tolDiv, false);
      setRowVisible(weightTolDiv, false);
      setRowVisible(optDiv, false);
      setRowVisible(reverseOrientationDiv, false);
    }
    if(this.value === endpoint.operators.OR){
      setRowVisible(inputDiv, true);
      setRowVisible(tolDiv, false);
      setRowVisible(weightTolDiv, false);
      setRowVisible(optDiv, false);
      setRowVisible(reverseOrientationDiv, false);
    }
    if(this.value === endpoint.operators.REPEAT){
      setRowVisible(inputDiv, true);
      setRowVisible(tolDiv, false);
      setRowVisible(weightTolDiv, false);
      setRowVisible(optDiv, true);
      setRowVisible(reverseOrientationDiv, false);
    }
    if(this.value === endpoint.operators.AND){
      setRowVisible(inputDiv, true);
      setRowVisible(tolDiv, true);
      setRowVisible(weightTolDiv, false);
      setRowVisible(optDiv, false);
      toleranceDropdown.value = 1;
      setRowVisible(reverseOrientationDiv, false);
    }
    if(this.value === endpoint.operators.MERGE){
      setRowVisible(inputDiv, true);
      setRowVisible(tolDiv, true);
      setRowVisible(weightTolDiv, true);
      setRowVisible(optDiv, false);
      toleranceDropdown.value = 0;
      setRowVisible(reverseOrientationDiv, false);
    }
    if(this.value === endpoint.operators.WEIGHT){
      setRowVisible(inputDiv, true);
      setRowVisible(tolDiv, true);
      setRowVisible(weightTolDiv, true);
      setRowVisible(optDiv, false);
      toleranceDropdown.value = 0;
      setRowVisible(reverseOrientationDiv, false);
    }
    if(this.value === endpoint.operators.REVERSE){
      setRowVisible(inputDiv, false);
      setRowVisible(tolDiv, false);
      setRowVisible(weightTolDiv, false);
      setRowVisible(optDiv, false);
      toleranceDropdown.value = 0;
      setRowVisible(reverseOrientationDiv, true);
    }
  });

  swal({
    title: "Apply Operator",
    text: "Current space: " + currentSpace,
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
      let reverseOrientation = reverseOrientationDropdown.value;
      let groupID = groupIDInput.value;

      switch (operatorDropdown.value) {
        case endpoint.operators.JOIN:
          endpoint.designSpaceJoin(inputSpaces, outputSpace, groupID);
          break;

        case endpoint.operators.OR:
          endpoint.designSpaceOr(inputSpaces, outputSpace, groupID);
          break;

        case endpoint.operators.REPEAT:
          endpoint.designSpaceRepeat(inputSpaces, outputSpace, groupID, isOptional);
          break;

        case endpoint.operators.AND:
          endpoint.designSpaceAnd(inputSpaces, outputSpace, groupID, tolerance, isComplete);
          break;

        case endpoint.operators.MERGE:
          endpoint.designSpaceMerge(inputSpaces, outputSpace, groupID, tolerance, weightTolerance);
          break;

        case endpoint.operators.WEIGHT:
          endpoint.designSpaceWeight(inputSpaces, outputSpace, groupID, tolerance, weightTolerance);
          break;

        case endpoint.operators.REVERSE:
          endpoint.designSpaceReverse(currentSpace, outputSpace, groupID, reverseOrientation);
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
  weightToleranceDropdown.appendChild(new Option("3"));
  weightToleranceDropdown.appendChild(new Option("4"));
  weightToleranceDropdown.appendChild(new Option("5"));
  weightToleranceDropdown.appendChild(new Option("6"));
  weightToleranceDropdown.appendChild(new Option("7"));

  return weightToleranceDropdown;
}

function makeCompleteDropdown(){
  let completeDropdown = document.createElement('select');
  completeDropdown.setAttribute("id", "complete-dropdown");
  completeDropdown.appendChild(new Option("True", true, true, true));
  completeDropdown.appendChild(new Option("False", false));

  return completeDropdown;
}

function makeReverseOrientationDropdown(){
  let reverseOrientation = document.createElement('select');
  reverseOrientation.setAttribute("id", "reverse-orientation");
  reverseOrientation.appendChild(new Option("True", true, true, true));
  reverseOrientation.appendChild(new Option("False", false));

  return reverseOrientation;
}

function makeDiv(div, input, title){
  div.appendChild(document.createTextNode(title));
  div.appendChild(input);
}

bindTooltipToButton(allBtnIDs.ruleEval, '#rule-eval-tooltip');
$('#rule-eval-tooltip').click(() => {
  let div = document.createElement('div');

  // Evaluation Name div
  let evaluationNameDiv = document.createElement('div');
  let evaluationNameInput = document.createElement('input');
  makeDiv(evaluationNameDiv, evaluationNameInput, 'Evaluation Name: ');

  // designGroupID div
  let designGroupIDDiv = document.createElement('div');
  let designGroupIDInput = document.createElement('input');
  makeDiv(designGroupIDDiv, designGroupIDInput, 'Design Group ID: ');

  // rulesGroupID div
  let rulesGroupIDDiv = document.createElement('div');
  let rulesGroupIDInput = document.createElement('input');
  makeDiv(rulesGroupIDDiv, rulesGroupIDInput, 'Rules Group ID: ');

  // Labeling Method div
  let labelingMethodDiv = document.createElement('div');
  let labelingMethodDropdown = makeLabelingMethodDropdown();
  makeDiv(labelingMethodDiv, labelingMethodDropdown, 'Labeling Method: ');

  //append all created divs to the main div
  div.appendChild(evaluationNameDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(designGroupIDDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(rulesGroupIDDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(labelingMethodDiv);
  div.appendChild(document.createElement('br'));

  swal({
    title: "Rule Evaluation",
    buttons: true,
    content: div
  }).then((confirm) => {
    if (!confirm) return;
    
    let evaluationName = evaluationNameInput.value;
    let designGroupID = designGroupIDInput.value;
    let rulesGroupID = rulesGroupIDInput.value;
    let labelingMethod = labelingMethodDropdown.value;

    endpoint.evaluateRules(evaluationName, designGroupID, rulesGroupID, labelingMethod);
  });
});

function makeLabelingMethodDropdown(){
  let modelDropdown = document.createElement('select');
  modelDropdown.setAttribute("id", "model-dropdown");
  modelDropdown.appendChild(new Option("Median", "median", true, true));
  modelDropdown.appendChild(new Option("Mean", "mean"));
  modelDropdown.appendChild(new Option("Sign", "sign"));
  return modelDropdown;
}

bindTooltipToButton(allBtnIDs.delete, '#delete-design-tooltip');
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

bindTooltipToButton(allBtnIDs.deleteGroup, '#delete-group-tooltip');
$('#delete-group-tooltip').click(() => {
  swal({
    title: "Really delete?",
    text: "You will not be able to recover the data!\nGroup ID: " + currentGroupID,
    icon: "warning",
    buttons: true
  }).then((confirm) => {
    if (confirm) {
      endpoint.deleteDesignGroup(currentGroupID);
    }
  });
});

bindTooltipToButton(allBtnIDs.createExperiment, '#create-experiment-tooltip');
$('#create-experiment-tooltip').click(() => {
  let div = document.createElement('div');

  // Experiment Name div
  let experimentNameDiv = document.createElement('div');
  let experimentNameInput = document.createElement('input');
  makeDiv(experimentNameDiv, experimentNameInput, 'Experiment Name: ');

  // Experiment Description div
  let experimentDescriptionDiv = document.createElement('div');
  let experimentDescriptionInput = document.createElement('input');
  makeDiv(experimentDescriptionDiv, experimentDescriptionInput, 'Experiment Description: ');

  // Designs Group ID div
  let designsGroupIDDiv = document.createElement('div');
  let designsGroupIDInput = document.createElement('input');
  makeDiv(designsGroupIDDiv, designsGroupIDInput, 'Designs Group ID: ');

  // Rules Group ID div
  let rulesGroupIDDiv = document.createElement('div');
  let rulesGroupIDInput = document.createElement('input');
  makeDiv(rulesGroupIDDiv, rulesGroupIDInput, 'Rules Group ID: ');

  // Rules To Eval Group ID div
  let rulesToEvalGroupIDDiv = document.createElement('div');
  let rulesToEvalGroupIDInput = document.createElement('input');
  makeDiv(rulesToEvalGroupIDDiv, rulesToEvalGroupIDInput, 'Rules To Eval Group ID: ');

  // Rule Evaluation Name Div
  let ruleEvaluationNameDiv = document.createElement('div');
  let ruleEvaluationNameInput = document.createElement('input');
  makeDiv(ruleEvaluationNameDiv, ruleEvaluationNameInput, 'Rule Evaluation: ');

  // Part Library File div
  let partLibraryNameDiv = document.createElement('div');
  let partLibraryNameInput = document.createElement('input');
  makeDiv(partLibraryNameDiv, partLibraryNameInput, 'Part Library: ');


  // Append all divs to the main div
  div.appendChild(experimentNameDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(experimentDescriptionDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(designsGroupIDDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(rulesGroupIDDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(rulesToEvalGroupIDDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(ruleEvaluationNameDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(partLibraryNameDiv);
  div.appendChild(document.createElement('br'));


  swal({
    title: "Create Experiment",
    // text: "Current Experiment: " + currentExperimentID,
    buttons: true,
    content: div
  }).then((confirm) => {
    if (confirm) {
      let experimentName = experimentNameInput.value;
      let experimentDescription = experimentDescriptionInput.value;
      let designsGroupID = designsGroupIDInput.value;
      let rulesGroupID = rulesGroupIDInput.value;
      let rulesToEvalGroupID = rulesToEvalGroupIDInput.value;
      let ruleEvaluationName = ruleEvaluationNameInput.value;
      let partLibraryName = partLibraryNameInput.value;
      if (experimentName) {
        endpoint.createExperiment(experimentName, experimentDescription, designsGroupID, rulesGroupID, rulesToEvalGroupID, ruleEvaluationName, partLibraryName, (err, data) => {
          if (!err) {
            showExperimentInfo(experimentName);
          }
        });
      }
    }
  });
});

bindTooltipToButton(allBtnIDs.deleteExperiment, '#delete-experiment-tooltip');
$('#delete-experiment-tooltip').click(() => {
  if (!currentExperimentID) {
    swalError("No experiment is currently open. Please open an experiment first.");
    return;
  }

  swal({
    title: "Really delete?",
    text: "You will not be able to recover the data!",
    icon: "warning",
    buttons: true
  }).then((confirm) => {
    if (confirm) {
      endpoint.deleteExperiment(currentExperimentID);
    }
  });
});

bindTooltipToButton(allBtnIDs.ml, '#ml-experiment-tooltip');
$('#ml-experiment-tooltip').click(() => {
  // if currentExperimentID is null or undefined, then swal error
  if (!currentExperimentID) {
    swalError("No experiment is currently open. Please open an experiment first.");
    return;
  }

  let div = document.createElement('div');
  div.style.maxHeight = "60vh";
  div.style.overflowY = "auto";
  div.style.overflowX = "hidden";
  div.style.paddingRight = "8px";

  // ML Action Dropdown
  let mlActionDiv = document.createElement('div');
  let mlActionDropdown = makeMLActionDropdown();
  makeDiv(mlActionDiv, mlActionDropdown, 'ML Action: ');

  // Model Dropdown
  let modelDiv = document.createElement('div');
  let modelDropdown = makeModelDropdown();
  makeDiv(modelDiv, modelDropdown, 'Model: ');

  // Run Name div
  let runNameDiv = document.createElement('div');
  let runNameInput = document.createElement('input');
  makeDiv(runNameDiv, runNameInput, 'Run Name: ');

  // num classes div
  let numClassesDiv = document.createElement('div');
  let numClassesInput = document.createElement('input');
  numClassesInput.setAttribute("type", "number");
  numClassesInput.setAttribute("value", "2");
  numClassesInput.setAttribute("min", "2");
  numClassesInput.setAttribute("step", "1");
  makeDiv(numClassesDiv, numClassesInput, 'Num Classes (Classification): ');

  // train ratio div
  let trainRatioDiv = document.createElement('div');
  let trainRatioInput = document.createElement('input');
  trainRatioInput.setAttribute("type", "number");
  trainRatioInput.setAttribute("value", "0.8");
  trainRatioInput.setAttribute("min", "0");
  trainRatioInput.setAttribute("max", "1");
  trainRatioInput.setAttribute("step", "0.05");
  makeDiv(trainRatioDiv, trainRatioInput, 'Train Ratio: ');

  // val ratio div
  let valRatioDiv = document.createElement('div');
  let valRatioInput = document.createElement('input');
  valRatioInput.setAttribute("type", "number");
  valRatioInput.setAttribute("value", "0.1");
  valRatioInput.setAttribute("min", "0");
  valRatioInput.setAttribute("max", "0.2");
  valRatioInput.setAttribute("step", "0.05");
  makeDiv(valRatioDiv, valRatioInput, 'Validation Ratio: ');

  // test ratio div
  let testRatioDiv = document.createElement('div');
  let testRatioInput = document.createElement('input');
  testRatioInput.setAttribute("type", "number");
  testRatioInput.setAttribute("value", "0.1");
  testRatioInput.setAttribute("min", "0");
  testRatioInput.setAttribute("max", "0.2");
  testRatioInput.setAttribute("step", "0.05");
  makeDiv(testRatioDiv, testRatioInput, 'Test Ratio: ');

  // Seed div
  let seedDiv = document.createElement('div');
  let seedInput = document.createElement('input');
  seedInput.setAttribute("type", "number");
  seedInput.setAttribute("value", "42");
  seedInput.setAttribute("step", "1");
  makeDiv(seedDiv, seedInput, 'Seed: ');

  let configDiv = document.createElement('div');
  let configTextarea = document.createElement("textarea");
  configTextarea.rows = 8;
  configTextarea.style.width = "100%";
  configTextarea.value = JSON.stringify(getDefaultConfigForModel(modelDropdown.value), null, 2);
  configDiv.appendChild(configTextarea);

  function buildConfigFromUI() {
    try {
      const parsed = JSON.parse(configTextarea.value || "{}");
      if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) {
        throw new Error("Config must be a JSON object.");
      }
      return parsed;
    } catch (e) {
      swalError("Invalid config JSON: " + e.message);
      return null;
    }
  }

  //append all
  div.appendChild(mlActionDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(modelDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(runNameDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(numClassesDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(trainRatioDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(valRatioDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(testRatioDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(seedDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(configDiv);
  div.appendChild(document.createElement('br'));

  setRowVisible(runNameDiv, false);
  setRowVisible(numClassesDiv, false);
  setRowVisible(seedDiv, false);
  setRowVisible(trainRatioDiv, false);
  setRowVisible(valRatioDiv, false);
  setRowVisible(testRatioDiv, false);
  setRowVisible(configDiv, false);

  $(mlActionDropdown).change(function() {
    if (this.value === endpoint.mlActions.TRAIN){
      setRowVisible(runNameDiv, true);
      setRowVisible(seedDiv, true);
      setRowVisible(numClassesDiv, true);
      setRowVisible(trainRatioDiv, true);
      setRowVisible(valRatioDiv, true);
      setRowVisible(testRatioDiv, true);
      setRowVisible(configDiv, true);
    }
    if (this.value === endpoint.mlActions.PREDICT){
      setRowVisible(runNameDiv, false);
      setRowVisible(numClassesDiv, false);
      setRowVisible(seedDiv, false);
      setRowVisible(trainRatioDiv, false);
      setRowVisible(valRatioDiv, false);
      setRowVisible(testRatioDiv, false);
      setRowVisible(configDiv, false);
    }
  });

  $(modelDropdown).change(function() {
    configTextarea.value = JSON.stringify(getDefaultConfigForModel(modelDropdown.value), null, 2);

    if (this.value === endpoint.mlModels.EBM || this.value === endpoint.mlModels.RANDOM_FOREST || this.value === endpoint.mlModels.XGBOOST){
      setRowVisible(valRatioDiv, false);
      trainRatioInput.value = 0.8;
      valRatioInput.value = 0;
      testRatioInput.value = 0.2;
    }

    if (this.value === endpoint.mlModels.TRANSFORMER || this.value === endpoint.mlModels.GNN || this.value === endpoint.mlModels.MLP){
      setRowVisible(valRatioDiv, true);
      trainRatioInput.value = 0.8;
      valRatioInput.value = 0.1;
      testRatioInput.value = 0.1;
    }

  });

  swal({
    title: "Machine Learning",
    text: "Current Experiment: " + currentExperimentID,
    buttons: true,
    content: div
  }).then((confirm) => {
    if (!confirm) return;

    const experiment = currentExperimentID;
    
    const runName = runNameInput.value;
    if (!runName) {
      swalError("Run Name is required.");
      return;
    }

    const model = modelDropdown.value;
    if (!model) {
      swalError("Please select a Model.");
      return;
    }

    const config = buildConfigFromUI();
    const task = "regression"; // default task is regression, can be changed later if needed
    
    const trainRatio = Number(trainRatioInput.value);
    const valRatio = Number(valRatioInput.value);
    const testRatio = Number(testRatioInput.value);
    const seed = Number(seedInput.value);
    // Numeric validation
    if (![trainRatio, valRatio, testRatio, seed].every(Number.isFinite)) {
      swalError("Train/Validation/Test ratios and Seed must be valid numbers.");
      return;
    }

    // Ratio bounds
    if (trainRatio < 0 || valRatio < 0 || testRatio < 0 || trainRatio > 1 || valRatio > 1 || testRatio > 1) {
      swalError("Train/Validation/Test ratios must each be between 0 and 1.");
      return;
    }

    // Sum check with floating-point tolerance
    const ratioSum = trainRatio + valRatio + testRatio;
    if (Math.abs(ratioSum - 1.0) > 1e-9) {
      swalError("Train + Validation + Test ratios must sum to 1.0.");
      return;
    }

    // Seed integer check
    if (!Number.isInteger(seed)) {
      swalError("Seed must be an integer.");
      return;
    }

    // ML Action validation
    if (!mlActionDropdown.value) {
      swalError("Please select an ML Action.");
      return;
    }

    switch (mlActionDropdown.value) {
      case endpoint.mlActions.TRAIN:
        endpoint.mlJobSubmit(mlActionDropdown.value, experiment, runName, model, config, task, trainRatio, valRatio, testRatio, seed, (err, data) => {
          if (!err) {
            showExperimentInfo(experiment);
          }
        });
        break;

      default:
        swalError("Unsupported ML Action: " + mlActionDropdown.value);
        break;
    }
  });
});

function makeMLActionDropdown(){
  let mlActionDropdown = document.createElement('select');
  let mlActionOption = new Option("ML Actions", "", true, true);
  mlActionOption.disabled = true;
  mlActionDropdown.appendChild(mlActionOption);

  for(let key in endpoint.mlActions){
    mlActionDropdown.appendChild(new Option(endpoint.mlActions[key]));
  }
  
  return mlActionDropdown;
}

function makeModelDropdown(){
  let modelDropdown = document.createElement('select');
  modelDropdown.setAttribute("id", "model-dropdown");
  modelDropdown.appendChild(new Option("Model", "", true, true));
  modelDropdown.appendChild(new Option("Graph Neural Network", "gnn"));
  modelDropdown.appendChild(new Option("Transformer", "transformer"));
  modelDropdown.appendChild(new Option("Multilayer Perceptron", "mlp"));
  modelDropdown.appendChild(new Option("Random Forest", "random_forest"));
  modelDropdown.appendChild(new Option("XGBoost", "xgboost"));
  modelDropdown.appendChild(new Option("Explainable Boosting Machine", "ebm"));
  return modelDropdown;
}

function getDefaultConfigForModel(model) {
  return modelConfigs[model] || {};
}

bindTooltipToButton(allBtnIDs.seqCompiler, '#seqcompiler-tooltip');
$('#seqcompiler-tooltip').click(() => {
  let div = document.createElement('div');
  div.style.maxHeight = "60vh";
  div.style.overflowY = "auto";
  div.style.overflowX = "hidden";
  div.style.paddingRight = "8px";

  // Space ID div
  let spaceIDDiv = document.createElement('div');
  let spaceIDInput = document.createElement('input');
  makeDiv(spaceIDDiv, spaceIDInput, 'Space ID: ');

  // Group ID div
  let groupIDDiv = document.createElement('div');
  let groupIDInput = document.createElement('input');
  makeDiv(groupIDDiv, groupIDInput, 'Group ID: ');

  // Weight div
  let weightDiv = document.createElement('div');
  let weightInput = document.createElement('input');
  weightInput.setAttribute("type", "number");
  weightInput.setAttribute("step", "0.01");
  weightInput.setAttribute("value", "0.0");
  makeDiv(weightDiv, weightInput, 'Weight: ');

  // Name Div
  let nameDiv = document.createElement('div');
  let nameInput = document.createElement('input');
  makeDiv(nameDiv, nameInput, 'Name: ');

  // Rz Div
  let RzDiv = document.createElement('div');
  let RzInput = document.createElement('input');
  RzInput.setAttribute("value", "Ro");
  makeDiv(RzDiv, RzInput, 'Rz: ');

  // L Div
  let LDiv = document.createElement('div');
  let LInput = document.createElement('input');
  LInput.setAttribute("value", "L");
  makeDiv(LDiv, LInput, 'L: ');

  // term Div
  let termDiv = document.createElement('div');
  let termInput = document.createElement('input');
  termInput.setAttribute("value", "T7t");
  makeDiv(termDiv, termInput, 'term: ');

  // hp5 Div
  let hp5Div = document.createElement('div');
  let hp5Input = document.createElement('input');
  hp5Input.setAttribute("value", "5hp");
  makeDiv(hp5Div, hp5Input, 'hp5: ');

  // prom Div
  let promDiv = document.createElement('div');
  let promInput = document.createElement('input');
  promInput.setAttribute("value", "T7p");
  makeDiv(promDiv, promInput, 'prom: ');

  // eI Div
  let eIDiv = document.createElement('div');
  let eIInput = document.createElement('input');
  makeDiv(eIDiv, eIInput, 'eI: ');

  // eO Div
  let eODiv = document.createElement('div');
  let eOInput = document.createElement('input');
  makeDiv(eODiv, eOInput, 'eO: ');

  // s Div
  let sDiv = document.createElement('div');
  let sInput = document.createElement('input');
  makeDiv(sDiv, sInput, 's: ');

  // invert Div
  let invertDiv = document.createElement('div');
  let invertInput = document.createElement('input');
  invertInput.setAttribute("type", "number");
  invertInput.setAttribute("step", "1");
  invertInput.setAttribute("value", "0");
  invertInput.setAttribute("min", "0");
  invertInput.setAttribute("max", "1");
  makeDiv(invertDiv, invertInput, 'invert: ');

  // invL Div
  let invLDiv = document.createElement('div');
  let invLInput = document.createElement('input');
  invLInput.setAttribute("value", "A");
  makeDiv(invLDiv, invLInput, 'invL: ');

  // agL Div
  let agLDiv = document.createElement('div');
  let agLInput = document.createElement('input');
  agLInput.setAttribute("value", "TA");
  makeDiv(agLDiv, agLInput, 'agL: ');

  // AGiloop Div
  let AGiloopDiv = document.createElement('div');
  let AGiloopInput = document.createElement('input');
  AGiloopInput.setAttribute("type", "number");
  AGiloopInput.setAttribute("step", "1");
  AGiloopInput.setAttribute("value", "5");
  makeDiv(AGiloopDiv, AGiloopInput, 'AGiloop: ');

  // otype Div
  let otypeDiv = document.createElement('div');
  let otypeInput = document.createElement('input');
  otypeInput.setAttribute("type", "number");
  otypeInput.setAttribute("step", "1");
  otypeInput.setAttribute("value", "1");
  makeDiv(otypeDiv, otypeInput, 'otype: ');

  // rna Div
  let rnaDiv = document.createElement('div');
  let rnaInput = document.createElement('input');
  rnaInput.setAttribute("type", "number");
  rnaInput.setAttribute("step", "1");
  rnaInput.setAttribute("value", "0");
  rnaInput.setAttribute("min", "0");
  rnaInput.setAttribute("max", "1");
  makeDiv(rnaDiv, rnaInput, 'rna: ');

  // us Div
  let usDiv = document.createElement('div');
  let usInput = document.createElement('input');
  makeDiv(usDiv, usInput, 'us (comma-separated): ');

  // ds Div
  let dsDiv = document.createElement('div');
  let dsInput = document.createElement('input');
  makeDiv(dsDiv, dsInput, 'ds (comma-separated): ');

  // temp_len Div
  let temp_lenDiv = document.createElement('div');
  let temp_lenInput = document.createElement('input');
  temp_lenInput.setAttribute("type", "number");
  temp_lenInput.setAttribute("step", "1");
  temp_lenInput.setAttribute("value", "0");
  makeDiv(temp_lenDiv, temp_lenInput, 'temp_len: ');

  // cp Div
  let cpDiv = document.createElement('div'); 
  let cpInput = document.createElement('input');
  makeDiv(cpDiv, cpInput, 'cp: ');

  // n Div
  let nDiv = document.createElement('div');
  let nInput = document.createElement('input');
  makeDiv(nDiv, nInput, 'n: ');

  // c Div
  let cDiv = document.createElement('div');
  let cInput = document.createElement('input');
  cInput.setAttribute("type", "number");
  cInput.setAttribute("step", "1");
  cInput.setAttribute("value", "0");
  cInput.setAttribute("min", "0");
  makeDiv(cDiv, cInput, 'c: ');

  // d Div
  let dDiv = document.createElement('div');
  let dInput = document.createElement('input');
  makeDiv(dDiv, dInput, 'd: ');

  // CDS Div
  let CDSDiv = document.createElement('div');
  let CDSInput = document.createElement('input');
  makeDiv(CDSDiv, CDSInput, 'CDS: ');

  // rflap Div
  let rflapDiv = document.createElement('div');
  let rflapInput = document.createElement('input');
  makeDiv(rflapDiv, rflapInput, 'rflap: ');

  // download GenBank file div
  let downloadGenbankDiv = document.createElement('div');
  let downloadGenbankCheckbox = document.createElement('input');
  downloadGenbankCheckbox.setAttribute("type", "checkbox");
  downloadGenbankCheckbox.checked = true;
  downloadGenbankDiv.appendChild(downloadGenbankCheckbox);
  downloadGenbankDiv.appendChild(document.createTextNode(' Download GenBank file after compilation'));

  // Append all divs to the main div
  div.appendChild(spaceIDDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(groupIDDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(weightDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(nameDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(RzDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(LDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(termDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(hp5Div);
  div.appendChild(document.createElement('br'));
  div.appendChild(promDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(eIDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(eODiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(sDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(invertDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(invLDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(agLDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(AGiloopDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(otypeDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(rnaDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(usDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(dsDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(temp_lenDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(cpDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(nDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(cDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(dDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(CDSDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(rflapDiv);
  div.appendChild(document.createElement('br'));
  div.appendChild(downloadGenbankDiv);

  swal({
    title: "Machine Learning",
    text: "Current Experiment: " + currentExperimentID,
    buttons: true,
    content: div
  }).then((confirm) => {
    if (!confirm) return;

    const spaceID = spaceIDInput.value;
    const groupID = groupIDInput.value;
    const weight = Number(weightInput.value);

    const name = nameInput.value;
    const Rz = RzInput.value;
    const L = LInput.value;
    const term = termInput.value;
    const hp5 = hp5Input.value;
    const prom = promInput.value;
    const eI = eIInput.value.trim();
    const eO = eOInput.value.trim();
    const s = sInput.value.trim();
    const invert = Number(invertInput.value);
    const invL = invLInput.value;
    const agL = agLInput.value;
    const AGiloop = Number(AGiloopInput.value);
    const otype = Number(otypeInput.value);
    const rna = Number(rnaInput.value);
    const us = (usInput.value.trim() !== "") ? usInput.value.split(',').map(s => s.trim()).filter(s => s.length > 0) : [];
    const ds = (dsInput.value.trim() !== "") ? dsInput.value.split(',').map(s => s.trim()).filter(s => s.length > 0) : [];
    const temp_len = Number(temp_lenInput.value);
    const cp = cpInput.value.trim();
    const n = nInput.value.trim();
    const c = Number(cInput.value);
    const d = dInput.value.trim();
    const CDS = CDSInput.value.trim();
    const rflap = rflapInput.value.trim();

    const downloadGenbank = downloadGenbankCheckbox.checked;

    endpoint.seqCompilerCompile(
      spaceID, 
      groupID, 
      weight, 
      name, 
      Rz, L, term, hp5, prom, eI, eO, s, invert, invL, agL, AGiloop, otype, rna, us, ds, temp_len, cp, n, c, d, CDS, rflap,
      downloadGenbank
    );

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

function setRowVisible(rowDiv, show) {
  rowDiv.style.display = show ? "" : "none";

  // Also hide/show the spacer <br> inserted right after the row
  const spacer = rowDiv.nextElementSibling;
  if (spacer && spacer.tagName === "BR") {
    spacer.style.display = show ? "" : "none";
  }
}

export function swalError(errorMsg){
  swal({
    title: "Error!",
    text: errorMsg,
    icon: "error"
  });
}

export function swalSuccess(msg = ""){
  swal({
    title: "Success!",
    text: msg,
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
/*
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
*/

// Do GOLDBAR in Knox
$("#testRulesBtn").click(async function () {

  let ruleOutcomes = {};
  let weight = 0;
  let tolerance = 1;
  let isComplete = true;
  let designName = "test_Rule_";
  let groupID = "testing_rules"

  endpoint.deleteDesignGroup(groupID);

  for (let rule of Object.keys(exampleRules)) {
    // Space names
    let ruleSpaceName = designName + rule + "_ruleSpace";
    let passSpaceName = designName + rule + "_passSpace";
    let failSpaceName = designName + rule + "_failSpace";
    let outputPass = designName + rule + "_and1_Pass";
    let outputFail = designName + rule + "_and1_Fail";

    // Create new designs
    submitGoldbar(exampleRules[rule], JSON.stringify(ruleCategories), ruleSpaceName, groupID, weight);
    submitGoldbar(ruleTests[rule][0].join(" or "), JSON.stringify(ruleCategories), passSpaceName, groupID, weight);
    submitGoldbar(ruleTests[rule][1].join(" or "), JSON.stringify(ruleCategories), failSpaceName, groupID, weight);

    // AND operations
    endpoint.designSpaceAnd([ruleSpaceName, passSpaceName], outputPass, groupID, tolerance, isComplete);
    endpoint.designSpaceAnd([ruleSpaceName, failSpaceName], outputFail, groupID, tolerance, isComplete);

    // Process results
    const result = await processRuleAsync(rule, outputPass, outputFail, passSpaceName);
    ruleOutcomes[rule] = result;
  }

  document.getElementById('designNameInput').value = "testing-results";
  document.getElementById('groupIDInput').value = "testing-results";
  let results = Object.entries(ruleOutcomes)
    .map(([rule, data]) => `${rule}: ${JSON.stringify(data)}\n\n`)
    .join("");

  editors.specEditor.setValue(results);
  editors.catEditor.setValue(JSON.stringify(ruleCategories));
});

async function processRuleAsync(rule, passOutput, failOutput, passSpace) {
  try {
    const [designsOutputPass, designsOutputFail, designsPass] = await Promise.all([
      quickEnumerateAsync(passOutput),
      quickEnumerateAsync(failOutput),
      quickEnumerateAsync(passSpace)
    ]);

    let outcome = "pass";
    let failedPassDesigns = [];
    let failedFailedDesigns = [];

    if (!deepEqualUnordered(designsOutputPass, designsPass)) {
      outcome = "fail - Passes not equal";
      failedPassDesigns = getUniqueElements(designsOutputPass, designsPass);
    }

    if (designsOutputFail.length > 0) {
      outcome = "fail - Fails not eliminated";
      failedFailedDesigns = designsOutputFail;
    }

    if (designsOutputPass.length > ruleTests[rule][0].length) {
      outcome = "fail - Pass too many designs";
      failedPassDesigns = designsOutputPass;
    }

    return {
      outcome,
      designsOutputPass,
      designsPass,
      failedPass: failedPassDesigns,
      failedFailed: failedFailedDesigns
    };
  } catch (error) {
    console.error(`Error in rule ${rule}:`, error);
    return { outcome: "error", error: error.message };
  }
}

function quickEnumerateAsync(inputSpace) {
  return new Promise((resolve, reject) => {
    quickEnumerate(inputSpace, (err, designs) => {
      if (err) reject(err);
      else resolve(designs);
    });
  });
}

function quickEnumerate(inputSpace, callback) {
  let numDesigns = 0;
  let minLength = 0;
  let maxLength = 0;
  let maxCycles = 0;
  let bfs = false;
  let isWeighted = false;
  let isSampleSpace = false;
  let allowDuplicates = true;

  endpoint.enumerateDesigns(
    inputSpace,
    numDesigns,
    minLength,
    maxLength,
    maxCycles,
    bfs,
    isWeighted,
    isSampleSpace,
    allowDuplicates,
    (err, data) => {
      if (err) {
        swalError("Enumeration error: " + JSON.stringify(err));
        callback(err, null);
      } else {
        const designs = data.designs.map(list =>
          list
            .filter(element => element.isBlank)
            .map(element => element.id)
        );
        callback(null, designs);
      }
    }
  )};


function deepEqualUnordered(arr1, arr2) {
  if (arr1.length !== arr2.length) return false;

  const normalize = arr =>
    arr.map(inner => [...inner].sort()).map(JSON.stringify).sort();

  const norm1 = normalize(arr1);
  const norm2 = normalize(arr2);

  for (let i = 0; i < norm1.length; i++) {
    if (norm1[i] !== norm2[i]) return false;
  }

  return true;
}

function getUniqueElements(arr1, arr2) {
  const uniqueToArr1 = arr1.filter(item => !arr2.includes(item));
  const uniqueToArr2 = arr2.filter(item => !arr1.includes(item));
  return [...uniqueToArr1, ...uniqueToArr2];
}

$("#goldbarImportBtn").click(function() {

  $('#spinner').removeClass('hidden'); // show spinner

  // Inputs
  let specification = editors.specEditor.getValue();
  let categories = editors.catEditor.getValue();
  let designName = document.getElementById('designNameInput').value;
  let groupID = document.getElementById('groupIDInput').value;
  let weight = document.getElementById('weightInput').value;

  //replace all spaces and special characters for SBOL
  designName = designName.replace(/[^A-Z0-9]/ig, "_");

  submitGoldbar(specification, categories, designName, groupID, weight);
  $('#spinner').addClass('hidden'); // remove spinner
});

export function submitGoldbar(specification, categories, designName, groupID, weight) {
  let parsed, pcategories
  [parsed, pcategories] = getParsedGOLDBARAndCategories(specification, categories);
  endpoint.importGoldbar(parsed, JSON.stringify(pcategories), designName, groupID, weight)
}

export function getParsedGOLDBARAndCategories(specification, categories) {
  let parsed, error, pcategories;
  error = "";
  try {
    parsed = parseGoldbar(specification);
    $("#spinner").addClass('hidden');
  } catch {
    error = error + "Error parsing GOLDBAR";
    swalError(error);
    $("#spinner").addClass('hidden');
  }

  try {
    pcategories = parseCategories(categories);
  } catch {
    error = error + "Error parsing categories";
    swalError(error);
    $("#spinner").addClass('hidden');
  }

  if (Object.entries(pcategories).length === 0 && pcategories.constructor === Object) {
    swalError("No Categories");
  }

  return [parsed, pcategories];
}

export function parseCategories(categories) {
  categories = categories.trim();
  categories = categories.replace('\t', ' ');
  return JSON.parse(categories);
}

export function parseGoldbar(langText) {
  langText = String(langText).replace('\t', ' ');
  langText = langText.trim();
  return langText
}

export function verifyRules(inputSpaces, outputSpace, groupID) {
  let tolerance = 1;
  let isComplete = true;

  endpoint.designSpaceAnd(inputSpaces, outputSpace, groupID, tolerance, isComplete)
}

/************************
 * SEARCH BAR FUNCTIONS
 ************************/
$("#search-tb").on("input", function() {
  let visualizeFunction = chooseVisualizeFunction();
  refreshCompletions("#search-tb", "#search-autocomplete", visualizeFunction);
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
    let visualizeFunction = chooseVisualizeFunction();
    populateAutocompleteList(() => {
      refreshCompletions("#search-tb", "#search-autocomplete", visualizeFunction);
    });
  }
});

$("#search-type").on("change", () => {
  //$("search-tb").val() = "";
  document.getElementById('search-tb').value = "";
  let visualizeFunction = chooseVisualizeFunction();
  populateAutocompleteList(() => {
    refreshCompletions("#search-tb", "#search-autocomplete", visualizeFunction);
  });
});

function chooseVisualizeFunction() {
  const searchType = $("#search-type").val();

  let visualizeFunction;
  if (searchType === "space") {
    visualizeFunction = visualizeDesignAndHistory;
  } else if (searchType === "group") {
    visualizeFunction = showGroupInfo;
  } else if (searchType === "experiment") {
    visualizeFunction = showExperimentInfo;
  }

  return visualizeFunction;
}

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
  
  var completions = suggestCompletions(val).slice(0, 50); // Displays at most
  completions.map((elem) => {
    var div = makeAutocompleteRow(elem, val);
    div.onclick = () => {
      $(textInputId).val(elem);
      refreshCompletions(textInputId, textCompletionsId);
      onSubmit(elem);
    };
    autoCmpl.append(div);
  });
  
  updateAutocompleteVisibility(textCompletionsId);
}

// Implementation of autocomplete. The webapp requests a list of
// all design spaces from Knox and caches it. The api exposes one
// a couple of functions, one to update the list of completions
// with the server, and one that takes a phrase and does string
// matching to return a list of design spaces with similar names.

function populateAutocompleteList(callback) {
    const searchType = $("#search-type").val();

    let fetchFunction;
    if (searchType === "space") {
      fetchFunction = endpoint.listDesignSpaces;
    } else if (searchType === "group") {
      fetchFunction = endpoint.listGroups;
    } else if (searchType === "experiment") {
      fetchFunction = endpoint.listExperiments;
    }

    fetchFunction((err, data) => {
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
        if (element.indexOf(phrase) !== -1 || phrase === "") {
            results.push(element);
        }
    });
    return results;
}

// --- AI Chat Box Injection ---
export function injectAIChatBox() {
  // Only inject once
  if (document.getElementById('ai-chat-btn')) return;

  // Chat Button
  const chatBtn = document.createElement('div');
  chatBtn.id = 'ai-chat-btn';
  chatBtn.innerHTML = `<button class="btn btn-primary" onclick="toggleChatBox()">AI Chat</button>`;
  document.body.appendChild(chatBtn);

  // Chat Box
  const chatBox = document.createElement('div');
  chatBox.id = 'ai-chat-box';
  chatBox.style.display = 'none';
  chatBox.innerHTML = `
    <div class="ai-chat-header">
      <span>AI Chat</span>
      <button class="ai-chat-clear" onclick="clearChat()" title="Clear chat" style="margin-right:8px;">&#128465;</button>
      <button class="ai-chat-close" onclick="toggleChatBox()">&times;</button>
    </div>
    <div id="chat-messages" class="ai-chat-messages"></div>
    <div class="ai-chat-input-area">
      <form id="chat-form">
        <input id="prompt" name="prompt" type="text" class="form-control ai-chat-input" placeholder="Type your message..." autocomplete="off">
        <button class="btn btn-success ai-chat-send" type="submit">Send</button>
      </form>
    </div>
  `;
  document.body.appendChild(chatBox);

  // Add event listeners
  document.getElementById('chat-form').onsubmit = sendChatMessage;
}

// --- AI Chat Box Logic ---
let isFirstToggle = true;
window.toggleChatBox = function() {
  const box = document.getElementById('ai-chat-box');
  box.style.display = (box.style.display === 'none' || box.style.display === '') ? 'block' : 'none';
  if (isFirstToggle) {
    appendMessage('AI', initialMessage);
    isFirstToggle = false;
  }
};

window.clearChat = function() {
  const msgDiv = document.getElementById('chat-messages');
  msgDiv.innerHTML = '';
  isFirstToggle = true;
  promptHistory = [];
  isFirstMessage = true;
};

window.appendMessage = function(sender, text) {
  const msgDiv = document.getElementById('chat-messages');
  const msg = document.createElement('div');
  msg.style.marginBottom = '12px';
  msg.innerHTML = `<strong>${sender}:</strong> ${text.replace(/\n/g, '<br><br>')}`;
  msgDiv.appendChild(msg);
  msgDiv.scrollTop = msgDiv.scrollHeight;
};

window.sendChatMessage = async function(event) {
  event.preventDefault();
  const input = document.getElementById('prompt');
  let prompt = input.value.trim();
  if (!prompt) return;
  appendMessage('You', prompt);

  let promptToSend = promptContext 
    + "\nOur Chat History:\n" 
    + promptHistory.join("\n") 
    + "\nCurrent Context: " + getCurrentContext()
    + "\nMe: " + prompt;

  addToPromptHistory("Me: " + prompt);

  input.value = '';
  try {
    const response = await fetch('/agent?prompt=' + encodeURIComponent(promptToSend), {
      method: 'POST',
      headers: {'Content-Type': 'text/plain'},
      body: promptToSend
    });
    let aiReply = await response.text();
    let cleanedReply = checkKeyForKeywords(aiReply);
    appendMessage('AI', cleanedReply);
    addToPromptHistory("AI: " + cleanedReply);
  } catch (e) {
    appendMessage('AI', 'Sorry, there was an error.');
  }
};

function checkKeyForKeywords(aiReply) {
  let cleanedReply = aiReply;

  // Define keyword patterns and their handlers
  const keywordHandlers = [
    {
      regex: /VISUALIZE_DESIGN_SPACE\((.*)\)/,
      handler: (spaceId) => {
        document.getElementById('search-tb').value = spaceId;
        visualizeDesignAndHistory(spaceId);
      }
    },
    // Add more keyword handlers here as needed
    // {
    //   regex: /ANOTHER_KEYWORD\(([^)]+)\)/,
    //   handler: (param) => { ... }
    // }
  ];

  for (const {regex, handler} of keywordHandlers) {
    const match = aiReply.match(regex);
    if (match) {
      handler(match[1]);
      cleanedReply = cleanedReply.replace(regex, '').replace(/\s+$/, '');
      break; // Only handle the first matching keyword
    }
  }

  return cleanedReply;
}

let promptHistory = [];
function addToPromptHistory(prompt) {
  promptHistory.push(prompt);
  if (promptHistory.length > 10) {
    promptHistory.shift();  // Remove oldest prompt if we have more than 10
  }
}

function getCurrentContext() {
  return `currently viewed design spaceid is ${currentSpace || "null"}. `
  + `currently viewed groupid is ${currentGroupID || "null"}. `
}

let initialMessage = 
  "Welcome to Knox! I am your AI assistant. I can help you with questions about GOLDBAR, combinatorial design, and how to use Knox.\n"
  + "You can ask me to retrieve information about design spaces and groups.\n"
  + "I can also apply operators to design spaces for you.\n"
  + "Feel free to ask me anything related to these topics!";

let promptContext = "You are an AI assistant (agent) in Knox (with access to certain tools), a design tool for part-based DNA combinatorial libraries that are represented as graphs in Neo4j.\n"
  + "Knox implements GOLDBAR: Grammars for Combinatorial Biological Design Assembly"
  //+ "\nHere is an example GOLDBAR rule:\n"
  //+ "Do Not Repeat part A: " + JSON.stringify(exampleRules.R, null, 2)
  + "\nYou can help the user with questions about GOLDBAR, combinatorial design, and how to use Knox."
  //+ " Try to keep your answers brief and to the point."
  //+ " Make sure to confirm with the user before applying any operations to design spaces."
  + "\nKeywords for frontend actions (Always use keywords when sent by a Tool) and (only use one keyword at end) and (always place keywords at the end):\n"
  + "VISUALIZE_DESIGN_SPACE(spaceid) - use this keyword to visualize a design space\n";
