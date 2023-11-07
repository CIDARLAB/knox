
import {currentSpace,
  currentBranch,
  setcurrentBranch,
  encodeQueryParameter,
  clearAllPages,
  visualizeDesignAndHistory,
  visualizeHistory,
  swalSuccess,
  swalError
} from "./knox.js";

const endpoints = {
  D3: "/graph/d3",
  LIST: "/designSpace/list",
  ENUMERATE: "/designSpace/enumerate",
  SCORE: "/designSpace/score",

  D3HISTORY: "/branch/graph/d3",
  CHECKOUT: "/branch/checkout",
  COMMIT: "/branch/commitTo",
  RESET: "/branch/reset",
  REVERT: "/branch/revert",

  BRANCH: "/branch", //post vs delete
  DESIGN: "/designSpace", //post vs delete
};

export const operators = {
  JOIN: 'join',
  OR: 'or',
  AND: 'and',
  MERGE: 'merge',
  REPEAT: 'repeat'
};


/************************
 * D3 ENDPOINT FUNCTIONS
 ************************/
// callback is of the form: function(err, jsonObj)
export function getGraph (id, callback){
  var query = "?targetSpaceID=" + encodeURIComponent(id);
  d3.json(endpoints.DESIGN+endpoints.D3 + query, callback);
}

export function listDesignSpaces (callback){
  d3.json(endpoints.LIST, callback);
}

export function getHistory (id, callback){
  let query = "?targetSpaceID=" + encodeURIComponent(id);
  d3.json(endpoints.BRANCH+endpoints.D3 + query, callback);
}

export function enumerateDesigns(id, callback){
  let query = "?targetSpaceID=" + encodeURIComponent(id) + "&bfs=true";
  d3.json(endpoints.ENUMERATE + query, callback);
}

export function getGraphScore(id, callback){
  let query = "?targetSpaceID=" + encodeURIComponent(id);
  d3.json(endpoints.SCORE + query, callback);
}

/***************************
 * VERSION HISTORY ENDPOINTS
 ***************************/

/**
 * Check out branch that the user selected
 * from the dropdown menu
 * Refresh both design space and history
 */
export function checkoutBranch(branchName){
  let request = new XMLHttpRequest();
  let query = "?";
  query += encodeQueryParameter("targetSpaceID", currentSpace, query);
  query += encodeQueryParameter("targetBranchID", branchName, query);
  request.open("POST", endpoints.CHECKOUT + query, false);
  request.send(null);

  // on success
  if (request.status >= 200 && request.status < 300) {
    setcurrentBranch(branchName);
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
  request.open("POST", endpoints.BRANCH + query, false);
  request.send(null);

  // on success
  if (request.status >= 200 && request.status < 300) {
    setcurrentBranch(branchName);
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
  request.open("DELETE", endpoints.BRANCH + query, false);
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
  request.open("POST", endpoints.COMMIT + query, false);
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
  request.open("POST", endpoints.RESET + query, false);
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
  request.open("POST", endpoints.REVERT + query, false);
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
  request.open("DELETE", endpoints.DESIGN + query, false);
  request.send(null);

  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
    clearAllPages();
  } else {
    swalError("Failed to delete design space " + currentSpace);
  }
}

/************************
 * DESIGN SPACE ENDPOINTS
 ************************/
export function designSpaceJoin(inputSpaces, outputSpace){
  let query = "?";
  query += encodeQueryParameter("inputSpaceIDs", inputSpaces, query);
  query += encodeQueryParameter("outputSpaceID", outputSpace, query);

  let request = new XMLHttpRequest();
  request.open("POST", endpoints.DESIGN + "/" + operators.JOIN + query, false);
  request.send(null);
  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
  } else {
    swalError(request.response);
  }
}

export function designSpaceOr(inputSpaces, outputSpace){
  let query = "?";
  query += encodeQueryParameter("inputSpaceIDs", inputSpaces, query);
  query += encodeQueryParameter("outputSpaceID", outputSpace, query);

  let request = new XMLHttpRequest();
  request.open("POST", endpoints.DESIGN + "/" + operators.OR + query, false);
  request.send(null);
  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
  } else {
    swalError(request.response);
  }
}

export function designSpaceRepeat(inputSpaces, outputSpace, isOptional){
  let query = "?";
  query += encodeQueryParameter("inputSpaceIDs", inputSpaces, query);
  query += encodeQueryParameter("outputSpaceID", outputSpace, query);
  query += encodeQueryParameter("isOptional", isOptional, query);

  let request = new XMLHttpRequest();
  request.open("POST", endpoints.DESIGN + "/" + operators.REPEAT + query, false);
  request.send(null);
  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
  } else {
    swalError(request.response);
  }
}

export function designSpaceAnd(inputSpaces, outputSpace, tolerance, isComplete){
  let query = "?";
  query += encodeQueryParameter("inputSpaceIDs", inputSpaces, query);
  query += encodeQueryParameter("outputSpaceID", outputSpace, query);
  query += encodeQueryParameter("tolerance", tolerance, query);
  query += encodeQueryParameter("isComplete", isComplete, query);

  let request = new XMLHttpRequest();
  request.open("POST", endpoints.DESIGN + "/" + operators.AND + query, false);
  request.send(null);
  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
  } else {
    swalError(request.response);
  }
}

export function designSpaceMerge(inputSpaces, outputSpace, tolerance){
  let query = "?";
  query += encodeQueryParameter("inputSpaceIDs", inputSpaces, query);
  query += encodeQueryParameter("outputSpaceID", outputSpace, query);
  query += encodeQueryParameter("tolerance", tolerance, query);

  let request = new XMLHttpRequest();
  request.open("POST", endpoints.DESIGN + "/" + operators.MERGE + query, false);
  request.send(null);
  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
  } else {
    swalError(request.response);
  }
}