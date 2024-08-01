

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
  SAMPLE: "/designSpace/sample",
  SCORE: "/designSpace/score",
  BESTPATH: "/designSpace/bestPath",
  BESTPATHSCORE: "/designSpace/bestPathScore",
  CREATESAMPLESPACE: "/designSpace/createSampleSpace",

  D3HISTORY: "/branch/graph/d3",
  CHECKOUT: "/branch/checkout",
  COMMIT: "/branch/commitTo",
  RESET: "/branch/reset",
  REVERT: "/branch/revert",

  BRANCH: "/branch", //post vs delete
  DESIGN: "/designSpace", //post vs delete

  SBOL: "/sbol/exportCombinatorial",
  GOLDBAR: "/goldbarSBOL/import"
};

export const operators = {
  JOIN: 'join',
  OR: 'or',
  AND: 'and',
  MERGE: 'merge',
  REPEAT: 'repeat'
};

export const enumerate = {
  ENUMERATE: 'enumerate',
  SAMPLE: 'sample',
  CREATESAMPLESPACE: 'create sample space'
}


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

export function enumerateDesigns(id, numDesigns, minLength, maxLength, isWeighted, callback){
  let query = "?targetSpaceID=" + encodeURIComponent(id) + "&bfs=true";
  query += "&numDesigns=" + encodeURIComponent(numDesigns)
  query += "&minLength=" + encodeURIComponent(minLength)
  query += "&maxLength=" + encodeURIComponent(maxLength)
  query += "&isWeighted=" + encodeURIComponent(isWeighted)
  d3.json(endpoints.ENUMERATE + query, callback);
}

export function sampleDesigns(id, numDesigns, length, isWeighted, callback){
  let query = "?targetSpaceID=" + encodeURIComponent(id);
  query += "&numDesigns=" + encodeURIComponent(numDesigns)
  query += "&length=" + encodeURIComponent(length)
  query += "&isWeighted=" + encodeURIComponent(isWeighted)
  d3.json(endpoints.SAMPLE + query, callback);
}

export function getGraphScore(id, callback){
  let query = "?targetSpaceID=" + encodeURIComponent(id);
  d3.json(endpoints.SCORE + query, callback);
}

export function getBestPath(id, callback){
  let query = "?targetSpaceID=" + encodeURIComponent(id);
  d3.json(endpoints.BESTPATH + query, callback);
}

export function getBestPathScore(id, callback){
  let query = "?targetSpaceID=" + encodeURIComponent(id);
  d3.json(endpoints.BESTPATHSCORE + query, callback);
}

export function createSampleSpace(id, callback){
  let query = "?targetSpaceID=" + encodeURIComponent(id);
  d3.json(endpoints.CREATESAMPLESPACE + query, callback);
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

// export function getDate() {
//   let d = new Date();
//   let month = '' + (d.getMonth() + 1);
//   let day = '' + d.getDate();
//   let year = d.getFullYear();
//
//   if (month.length < 2) month = '0' + month;
//   if (day.length < 2) day = '0' + day;
//
//   return [year, month, day].join('-');
// }


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

export function importGoldbarSBOL(sbolDoc){
  let query = "?"
  query += encodeQueryParameter("sbolDoc", sbolDoc, query)

  let request = new XMLHttpRequest();
  request.open("POST", endpoints.GOLDBAR + query, false);
  request.send(null);

}


export function downloadSBOL(text, filename) {
  let element = document.createElement('a');
  element.setAttribute('href', 'data:application/xml,' + encodeURIComponent(text));
  element.setAttribute('download', filename);
  element.style.display = 'none';
  document.body.appendChild(element);
  element.click();
  document.body.removeChild(element);
}


export function exportDesign(){

  let request = new XMLHttpRequest();
  let query = "?";
  query += encodeQueryParameter("targetSpaceID", currentSpace, query);
  query += encodeQueryParameter("namespace", "http://knox.org", query);
  request.open("GET", endpoints.SBOL + query, false);
  request.send(null);

  // on success
  if (request.status >= 200 && request.status < 300) {
    let designNameArray = currentSpace.split("_");
    let designName = designNameArray.join("_");

    let res = JSON.parse(request.response);

    let langText = res[0];
    let categories = res[1];
    let numDesigns = 1;
    let cycleDepth = 1;

    try {
      let result = constellation.goldbar(designName, langText, categories, numDesigns, cycleDepth, "EDGE").sbol;
      downloadSBOL(result, "knox_" + designName + "_sbol.xml");
    } catch(error) {
      swalError(error.message);
    }

    swalSuccess();
    visualizeDesignAndHistory(currentSpace);
  } else {
    swalError("Failed to download");
  }
}
