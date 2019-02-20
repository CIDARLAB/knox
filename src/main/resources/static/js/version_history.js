
import {targets, currentSpace, encodeQueryParameter, hideExplorePageBtns} from "./knox.js";

let currentBranch;
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
      swal("Graph lookup failed!", "error status: " + JSON.stringify(err));
    } else {
      targets.history.clear();
      targets.history.setHistory(data);
      currentBranch = data.links[0].target.knoxID;
      populateBranchSelector(data.nodes);
    }
  });
}

/**
 * Populate dropdown in the Version History column
 * with all the branches associated with a design space
 * @param nodes
 */
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

/**
 * Check out branch that the user selected
 * from the dropdown menu
 */
export function checkoutBranch(){
  let branchSelector = $('#branch-selector');
  let branchName = branchSelector[0].options[branchSelector[0].selectedIndex].value;

  let request = new XMLHttpRequest();
  let query = "?";
  query += encodeQueryParameter("targetSpaceID", currentSpace, query);
  query += encodeQueryParameter("outputBranchID", branchName, query);
  request.open("POST", endpoints.branch + query, false);
  request.send(null);

  // on success
  if (request.status >= 200 && request.status < 300) {
    currentBranch = branchName;
  }
}

/**
 * Creates new branch and refresh visualization
 */
export function makeNewBranch(){
  console.log("new branch");

  let request = new XMLHttpRequest();
  let query = "?";
  let branchName = $("#new-branch-name").val();

  query += encodeQueryParameter("targetSpaceID", currentSpace, query);
  query += encodeQueryParameter("outputBranchID", branchName, query);
  request.open("POST", endpoints.branch + query, false);
  request.send(null);

  console.log(request.status);
  // on success
  if (request.status >= 200 && request.status < 300) {
    currentBranch = branchName;
    console.log(currentBranch);
    requestSuccess();
  } else {
    swal("Error: Failed to create new branch");
  }
}

/**
 * Deletes current branch and refresh visualization
 */
export function deleteBranch(){
  let request = new XMLHttpRequest();
  let query = "?";
  query += encodeQueryParameter("targetSpaceID", currentSpace, query);
  query += encodeQueryParameter("targetBranchID", currentBranch, query);
  request.open("DELETE", endpoints.branch + query, false);
  request.send(null);

  // on success
  if (request.status >= 200 && request.status < 300) {
    requestSuccess();
  } else {
    swal("Error: Failed to create new commit");
  }
}

/**
 * Creates new commit and refresh visualization
 */
export function makeCommit(){
  console.log("new commit");

  let request = new XMLHttpRequest();
  let query = "?";
  query += encodeQueryParameter("targetSpaceID", currentSpace, query);
  query += encodeQueryParameter("targetBranchID", currentBranch, query);
  console.log(query);
  request.open("POST", endpoints.commit + query, false);
  request.send(null);

  // on success
  if (request.status >= 200 && request.status < 300) {
    requestSuccess();
  } else {
    swal("Error: Failed to create new commit");
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
    requestSuccess();
  } else {
    swal("Error: Failed to reset commit");
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
    requestSuccess();
  } else {
    swal("Error: Failed to reset commit");
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
    requestSuccess();
    targets.search.clear();
    hideExplorePageBtns();
  } else {
    swal("Error: Failed to delete design space " + currentSpace + ".");
  }
}

function requestSuccess(){
  visualizeHistory(currentSpace);
  swal({
    title: "Success!",
    confirmButtonColor: "#F05F40",
    type: "success"
  });
}