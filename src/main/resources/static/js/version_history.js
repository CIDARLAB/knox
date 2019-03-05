
import {targets, currentSpace, encodeQueryParameter, clearAllPages, visualizeDesignAndHistory} from "./knox.js";

let currentBranch;
export let historyNodes;
const endpoints = {
  history: "/branch/graph/d3",
  branch: "/branch", //post vs delete
  checkout: "/branch/checkout",
  commit: "/branch/commitTo",
  reset: "/branch/reset",
  revert: "/branch/revert",
  design: "/designSpace" //post vs delete
};
export const knoxClass = {
  HEAD: "Head",
  BRANCH: "Branch",
  COMMIT: "Commit"
};

function getHistory (id, callback){
  let query = "?targetSpaceID=" + encodeURIComponent(id);
  d3.json(endpoints.history + query, callback);
}

export function visualizeHistory(spaceid){
  getHistory(spaceid, (err, data) => {
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

/**
 * Populate dropdown in the Version History column
 * with all the branches associated with a design space
 * @param nodes history nodes
 * @param branchSelector
 */
export function populateBranchSelector(nodes, branchSelector){
  branchSelector.find('option').not(':first').remove();

  //repopulate
  let branches = nodes.filter(obj => obj.knoxClass === knoxClass.BRANCH);
  $.each(branches, function(i, b) {
    branchSelector.append($('<option></option>').val(b.knoxID).html(b.knoxID));
  });
}

/**
 * Check out branch that the user selected
 * from the dropdown menu
 * Refresh both design space and history
 */
export function checkoutBranch(){
  let branchSelector = $('#branch-selector');
  let branchName = branchSelector[0].options[branchSelector[0].selectedIndex].value;

  let request = new XMLHttpRequest();
  let query = "?";
  query += encodeQueryParameter("targetSpaceID", currentSpace, query);
  query += encodeQueryParameter("targetBranchID", branchName, query);
  request.open("POST", endpoints.checkout + query, false);
  request.send(null);

  // on success
  if (request.status >= 200 && request.status < 300) {
    currentBranch = branchName;
    visualizeDesignAndHistory(currentSpace);
  }
}

/**
 * Creates new branch and refresh history
 */
export function makeNewBranch(branchName){
  let request = new XMLHttpRequest();
  let query = "?";

  query += encodeQueryParameter("targetSpaceID", currentSpace, query);
  query += encodeQueryParameter("outputBranchID", branchName, query);
  request.open("POST", endpoints.branch + query, false);
  request.send(null);

  // on success
  if (request.status >= 200 && request.status < 300) {
    currentBranch = branchName;
    swalSuccess();
    visualizeHistory(currentSpace);
  } else {
    swalError("Failed to create new branch");
  }
}

/**
 * Deletes user specified branch and refreshes history
 * Will throw error if user tries to delete current branch
 */
export function deleteBranch(branchName){
  let request = new XMLHttpRequest();
  let query = "?";
  query += encodeQueryParameter("targetSpaceID", currentSpace, query);
  query += encodeQueryParameter("targetBranchID", branchName, query);
  request.open("DELETE", endpoints.branch + query, false);
  request.send(null);

  // on success
  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
    visualizeHistory(currentSpace);
  } else {
    swalError("Failed to delete branch");
  }
}

/**
 * Creates new commit and refresh visualization
 */
export function makeCommit(){
  let request = new XMLHttpRequest();
  let query = "?";
  query += encodeQueryParameter("targetSpaceID", currentSpace, query);
  query += encodeQueryParameter("targetBranchID", currentBranch, query);
  console.log(query);
  request.open("POST", endpoints.commit + query, false);
  request.send(null);

  // on success
  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
    visualizeHistory(currentSpace);
  } else {
    swalError("Failed to create new commit");
  }
}

/**
 * Resets current commit and refresh visualization
 * No history of the commit remains
 */
export function resetCommit(){
  let request = new XMLHttpRequest();
  let query = "?";
  query += encodeQueryParameter("targetSpaceID", currentSpace, query);
  query += encodeQueryParameter("targetBranchID", currentBranch, query);
  // todo commit path
  console.log(query);
  request.open("POST", endpoints.reset + query, false);
  request.send(null);

  // on success
  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
    visualizeDesignAndHistory(currentSpace);
  } else {
    swalError("Failed to reset commit");
  }
}

/**
 * Creates a new commit from 2 commits ago and refresh visualization
 * History is preserved
 */
export function revertCommit(){
  let request = new XMLHttpRequest();
  let query = "?";
  query += encodeQueryParameter("targetSpaceID", currentSpace, query);
  query += encodeQueryParameter("targetBranchID", currentBranch, query);
  // todo commit path
  console.log(query);
  request.open("POST", endpoints.revert + query, false);
  request.send(null);

  // on success
  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
    visualizeDesignAndHistory(currentSpace);
  } else {
    swalError("Failed to revert commit");
  }
}

/**
 * Deletes the design space and clears design space svg
 */
export function deleteDesign(){
  let request = new XMLHttpRequest();
  let query = "?targetSpaceID=" + currentSpace;
  request.open("DELETE", endpoints.design + query, false);
  request.send(null);

  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
    clearAllPages();
  } else {
    swalError("Failed to delete design space " + currentSpace);
  }
}



function swalError(errorMsg){
  swal({
    title: "Error!",
    text: errorMsg,
    icon: "error"
  });
}

function swalSuccess(){
  swal({
    title: "Success!",
    icon: "success"
  });
}