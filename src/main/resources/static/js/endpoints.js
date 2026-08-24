

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
  LISTGROUPIDS: "/designGroup/list",
  LISTGROUPSPACEIDS: "/designGroup/listSpaces",
  LISTEXPERIMENTS: "/experiment/list",
  LISTPARTLIBRARIES: "/partLibrary/list",
  PARTLIBRARYCATEGORIES: "/partLibrary/categories",
  D3GRAPHPARTLIBRARY: "/partLibrary/graph/d3",
  PARTLIBRARY: "/partLibrary",  //delete
  EXPERIMENT: "/experiment",  //post vs get vs delete
  ENUMERATE: "/designSpace/enumerate",
  ENUMERATECSV: "/designSpace/enumerateCSV",
  SAMPLE: "/designSpace/sample",
  SCORE: "/designSpace/score",
  BESTPATH: "/designSpace/bestPath",
  CREATESAMPLESPACE: "/designSpace/createSampleSpace",
  PARTANALYTICS: "/designSpace/partAnalytics",

  RULEEVAL: "/rule/evaluate",
  LISTEVALUATION: "/rule/listEvaluations",

  RENAME: "/designSpace/rename",
  SETGROUPID: "/designSpace/setGroupID",
  GETGROUPID: "/designSpace/getGroupID",
  GETGROUPSIZE: "/designSpace/getGroupSize",

  D3HISTORY: "/branch/graph/d3",
  CHECKOUT: "/branch/checkout",
  COMMIT: "/branch/commitTo",
  RESET: "/branch/reset",
  REVERT: "/branch/revert",

  BRANCH: "/branch", //post vs delete
  DESIGN: "/designSpace", //post vs delete
  DELETEGROUP: "/designSpace/deleteGroup",

  SBOL: "/sbol/exportCombinatorial",
  GOLDBARSBOL: "/goldbarSBOL/import",
  GOLDBAR: "/goldbar/import",
  GETGOLDBAR: "/goldbar",

  RUNJOB: "/job/submit",
  JOB: "/job",  //get vs delete,
  STOPTUNE: "/job/stopTuning",

  SEQCOMPILER: "/seqcompiler/compile"  //post
};

export const operators = {
  JOIN: 'join',
  OR: 'or',
  AND: 'and',
  MERGE: 'merge',
  REPEAT: 'repeat',
  WEIGHT: 'weight',
  REVERSE: 'reverse'
};

export const mlActions = {
  TRAIN: 'train',
  PREDICT: 'predict',
  TUNE: 'tune',
  // EVALUATE: 'evaluate'
};

export const mlTasks = {
  REGRESSION: 'regression',
  CLASSIFICATION: 'classification',
  MULTICLASSCLASSIFICATION: 'multiclass_classification'
};

export const mlModels = {
  GNN: 'gnn',
  TRANSFORMER: 'transformer',
  MLP: 'mlp',
  RANDOM_FOREST: 'random_forest',
  XGBOOST: 'xgboost',
  EBM: 'ebm'
};

export const enumerate = {
  ENUMERATE: 'enumerate',
  SAMPLE: 'sample',
  CREATESAMPLESPACE: 'create sample space',
  PARTANALYTICS: "part analytics"
}

export const groupInfo = {
  SETGROUPID: 'set group id',
  GETGROUPSIZE: 'get group size'
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

export function listEvaluations (callback){
  d3.json(endpoints.LISTEVALUATION, callback);
}

export function listGroups (callback){
  d3.json(endpoints.LISTGROUPIDS, callback);
}

export function listGroupSpaceIDs (groupID, callback){
  let query = "?groupID=" + encodeURIComponent(groupID);
  d3.json(endpoints.LISTGROUPSPACEIDS + query, callback);
}

export function listExperiments (callback){
  d3.json(endpoints.LISTEXPERIMENTS, callback);
}

export function listPartLibraries (callback){
  d3.json(endpoints.LISTPARTLIBRARIES, callback);
}

export function partLibraryCategories (partLibraryName, callback){
  let query = "?partLibraryName=" + encodeURIComponent(partLibraryName);
  d3.json(endpoints.PARTLIBRARYCATEGORIES + query, callback);
}

export function d3GraphPartLibrary (partLibraryName, callback){
  let query = "?partLibraryName=" + encodeURIComponent(partLibraryName);
  d3.json(endpoints.D3GRAPHPARTLIBRARY + query, callback);
}

export function getHistory (id, callback){
  let query = "?targetSpaceID=" + encodeURIComponent(id);
  d3.json(endpoints.BRANCH+endpoints.D3 + query, callback);
}

export function enumerateDesigns(id, numDesigns, minLength, maxLength, maxCycles, bfs, isWeighted, isSampleSpace, allowDuplicates, callback){
  let query = "?targetSpaceID=" + encodeURIComponent(id);
  query += "&numDesigns=" + encodeURIComponent(numDesigns);
  query += "&minLength=" + encodeURIComponent(minLength);
  query += "&maxLength=" + encodeURIComponent(maxLength);
  query += "&maxCycles=" + encodeURIComponent(maxCycles);
  query += "&bfs=" + encodeURIComponent(bfs);
  query += "&isWeighted=" + encodeURIComponent(isWeighted);
  query += "&isSampleSpace=" + encodeURIComponent(isSampleSpace);
  query += "&allowDuplicates=" + encodeURIComponent(allowDuplicates);
  d3.json(endpoints.ENUMERATE + query, callback);
}

export function sampleDesigns(id, numDesigns, minLength, maxLength, isWeighted, isSampleSpace, callback){
  let query = "?targetSpaceID=" + encodeURIComponent(id);
  query += "&numDesigns=" + encodeURIComponent(numDesigns);
  query += "&minLength=" + encodeURIComponent(minLength);
  query += "&maxLength=" + encodeURIComponent(maxLength);
  query += "&isWeighted=" + encodeURIComponent(isWeighted);
  query += "&isSampleSpace=" + encodeURIComponent(isSampleSpace);
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

export function createSampleSpace(id, groupID, callback){
  let query = "?targetSpaceID=" + encodeURIComponent(id);
  query += "?groupID=" + encodeURIComponent(groupID);
  d3.json(endpoints.CREATESAMPLESPACE + query, callback);
}

export function getPartAnalytics(id, callback){
  let query = "?targetSpaceID=" + encodeURIComponent(id);
  d3.json(endpoints.PARTANALYTICS + query, callback);
}

/**
 * Gets GroupID for a Design Space
 */
export function getGroupID(id, callback){
  let query = "?targetSpaceID=" + encodeURIComponent(id);
  d3.json(endpoints.GETGROUPID + query, callback);
}

/**
 * Gets Group Size for a groupID
 */
export function getGroupSize(groupID, callback){
  let query = "?groupID=" + encodeURIComponent(groupID);
  d3.json(endpoints.GETGROUPSIZE + query, callback);
}

/**
 * Rename Design Space
 */
export function renameDesignSpace(id, newSpaceID, callback){
  let query = "?targetSpaceID=" + encodeURIComponent(id);
  query += "&newSpaceID=" + encodeURIComponent(newSpaceID);
  d3.json(endpoints.RENAME + query, callback);
}

/**
 * Gets GOLDBAR for a Design Space
 */
export function getGoldbar(id, callback){
  let query = "?targetSpaceID=" + encodeURIComponent(id);
  d3.json(endpoints.GETGOLDBAR + query, callback);
}

export function getExperiment(experimentName, callback) {
  const query = "?experimentName=" + encodeURIComponent(experimentName);
  d3.json(endpoints.EXPERIMENT + query, callback);
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

/**
 * Deletes specific design space and clears design space svg
 */
export function deleteThisDesign(inputSpace, response){
  let request = new XMLHttpRequest();
  let query = "?targetSpaceID=" + inputSpace;
  request.open("DELETE", endpoints.DESIGN + query, false);
  request.send(null);
  
  if (response) {
    if (request.status >= 200 && request.status < 300) {
      swalSuccess();
      clearAllPages();
    } else {
      swalError("Failed to delete design space " + inputSpace);
    }
  }
}

/**
 * Deletes all designs space from groupID and clears design space svg
 */
export function deleteDesignGroup(groupID){
  let request = new XMLHttpRequest();
  let query = "?groupID=" + groupID;
  request.open("DELETE", endpoints.DELETEGROUP + query, false);
  request.send(null);
  
  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
    clearAllPages();
  } else {
    swalError("Failed to delete design space group " + groupID);
  }
}

/**
 * Deletes the part library
 */
export function deletePartLibrary(partLibraryName){
  let request = new XMLHttpRequest();
  let query = "?partLibraryName=" + partLibraryName;
  request.open("DELETE", endpoints.PARTLIBRARY + query, false);
  request.send(null);

  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
    clearAllPages();
  } else {
    swalError("Failed to delete part library " + partLibraryName + ". It may be in use by an active experiment.");
  }
}

/**
 * Deletes the experiment
 */
export function deleteExperiment(experimentName){
  let request = new XMLHttpRequest();
  let query = "?experimentName=" + experimentName;
  request.open("DELETE", endpoints.EXPERIMENT + query, false);
  request.send(null);

  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
    clearAllPages();
  } else {
    swalError("Failed to delete experiment " + experimentName);
  }
}

/**
 * Creates the experiment
 */
export function createExperiment(experimentName, description, designsGroupID, rulesGroupID, rulesToEvalGroupID, ruleEvaluationName, partLibraryName, callback){
  let request = new XMLHttpRequest();
  let query = "?experimentName=" + experimentName;
  query += "&description=" + encodeURIComponent(description);
  query += "&designsGroupID=" + encodeURIComponent(designsGroupID);
  query += "&rulesGroupID=" + encodeURIComponent(rulesGroupID);
  query += "&rulesToEvalGroupID=" + encodeURIComponent(rulesToEvalGroupID);
  query += "&ruleEvaluationName=" + encodeURIComponent(ruleEvaluationName);
  query += "&partLibraryName=" + encodeURIComponent(partLibraryName);
  request.open("POST", endpoints.EXPERIMENT + query, false);
  request.send(null);

  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
    clearAllPages();
    if (callback) callback(null, { experimentName, description, request: request.response });
  } else {
    const err = new Error("Failed to create experiment " + experimentName);
    swalError(err.message);
    if (callback) callback(err);
  }
}

/**
 * Deletes the Job
 */
export function deleteJob(jobID, onSuccess) {
  let request = new XMLHttpRequest();
  let query = "?jobID=" + jobID;
  request.open("DELETE", endpoints.JOB + query, false);
  request.send(null);

  if (request.status >= 200 && request.status < 300) {
    if (onSuccess) onSuccess();
    swalSuccess();
  } else {
    swalError("Failed to delete job " + jobID);
  }
}

/**
 * Sets GroupID for a Design Space
 */
export function setGroupID(groupID){
  let query = "?";
  query += encodeQueryParameter("targetSpaceID", currentSpace, query);
  query += encodeQueryParameter("groupID", groupID, query);

  let request = new XMLHttpRequest();
  request.open("POST", endpoints.SETGROUPID + query, false);
  request.send(null);
  
  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
    clearAllPages();
  } else {
    swalError("Failed to set design space group " + groupID);
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
export function designSpaceJoin(inputSpaces, outputSpace, groupID){
  let query = "?";
  query += encodeQueryParameter("inputSpaceIDs", inputSpaces, query);
  query += encodeQueryParameter("outputSpaceID", outputSpace, query);
  query += encodeQueryParameter("groupID", groupID, query);

  let request = new XMLHttpRequest();
  request.open("POST", endpoints.DESIGN + "/" + operators.JOIN + query, false);
  request.send(null);
  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
  } else {
    swalError(request.response);
  }
}

export function designSpaceOr(inputSpaces, outputSpace, groupID){
  let query = "?";
  query += encodeQueryParameter("inputSpaceIDs", inputSpaces, query);
  query += encodeQueryParameter("outputSpaceID", outputSpace, query);
  query += encodeQueryParameter("groupID", groupID, query);

  let request = new XMLHttpRequest();
  request.open("POST", endpoints.DESIGN + "/" + operators.OR + query, false);
  request.send(null);
  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
  } else {
    swalError(request.response);
  }
}

export function designSpaceRepeat(inputSpaces, outputSpace, groupID, isOptional){
  let query = "?";
  query += encodeQueryParameter("inputSpaceIDs", inputSpaces, query);
  query += encodeQueryParameter("outputSpaceID", outputSpace, query);
  query += encodeQueryParameter("groupID", groupID, query);
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

export function designSpaceAnd(inputSpaces, outputSpace, groupID, tolerance, isComplete){
  let query = "?";
  query += encodeQueryParameter("inputSpaceIDs", inputSpaces, query);
  query += encodeQueryParameter("outputSpaceID", outputSpace, query);
  query += encodeQueryParameter("groupID", groupID, query);
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

export function designSpaceMerge(inputSpaces, outputSpace, groupID, tolerance, weightTolerance){
  let query = "?";
  query += encodeQueryParameter("inputSpaceIDs", inputSpaces, query);
  query += encodeQueryParameter("outputSpaceID", outputSpace, query);
  query += encodeQueryParameter("groupID", groupID, query);
  query += encodeQueryParameter("tolerance", tolerance, query);
  query += encodeQueryParameter("weightTolerance", weightTolerance, query);

  let request = new XMLHttpRequest();
  request.open("POST", endpoints.DESIGN + "/" + operators.MERGE + query, false);
  request.send(null);
  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
  } else {
    swalError(request.response);
  }
}

export function designSpaceWeight(inputSpaces, outputSpace, groupID, tolerance, weightTolerance){
  let query = "?";
  query += encodeQueryParameter("inputSpaceIDs", inputSpaces, query);
  query += encodeQueryParameter("outputSpaceID", outputSpace, query);
  query += encodeQueryParameter("groupID", groupID, query);
  query += encodeQueryParameter("tolerance", tolerance, query);
  query += encodeQueryParameter("weightTolerance", weightTolerance, query);

  let request = new XMLHttpRequest();
  request.open("POST", endpoints.DESIGN + "/" + operators.WEIGHT + query, false);
  request.send(null);
  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
  } else {
    swalError(request.response);
  }
}

export function designSpaceReverse(inputSpace, outputSpace, groupID, reverseOrientation){
  let query = "?";
  query += encodeQueryParameter("inputSpaceID", inputSpace, query);
  query += encodeQueryParameter("outputSpaceID", outputSpace, query);
  query += encodeQueryParameter("groupID", groupID, query);
  query += encodeQueryParameter("reverseOrientation", reverseOrientation, query);

  let request = new XMLHttpRequest();
  request.open("POST", endpoints.DESIGN + "/" + operators.REVERSE + query, false);
  request.send(null);
  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
  } else {
    swalError(request.response);
  }
}

export function importGoldbarSBOL(sbolDoc, groupID, weight){
  let query = "?"
  query += encodeQueryParameter("sbolDoc", sbolDoc, query)
  query += encodeQueryParameter("groupID", groupID, query);
  query += encodeQueryParameter("weight", weight, query)

  let request = new XMLHttpRequest();
  request.open("POST", endpoints.GOLDBARSBOL + query, false);
  request.send(null);

}


export function importGoldbar(goldbar, categories, outputSpace, groupID, weight){
  let query = "?"
  query += encodeQueryParameter("goldbar", goldbar, query)
  query += encodeQueryParameter("categories", categories, query)
  query += encodeQueryParameter("outputSpaceID", outputSpace, query)
  query += encodeQueryParameter("groupID", groupID, query);
  query += encodeQueryParameter("weight", weight, query)

  let request = new XMLHttpRequest();
  request.open("POST", endpoints.GOLDBAR + query, false);
  request.send(null);
  if (request.status >= 200 && request.status < 300) {
    swalSuccess();
  } else {
    swalError(request.response);
  }

}

export function evaluateRules(evaluationName, designGroupID, rulesGroupID, labelingMethod, callback) {
  let query = "?";
  query += encodeQueryParameter("evaluationName", evaluationName, query);
  query += encodeQueryParameter("designGroupID", designGroupID, query);
  query += encodeQueryParameter("rulesGroupID", rulesGroupID, query);
  query += encodeQueryParameter("labelingMethod", labelingMethod, query);

  fetch(endpoints.RULEEVAL + query, { method: "POST" })
  
  .then(async (response) => {
    if (!response.ok) {
      throw new Error(await response.text());
    }
    return response.json();
  })
  
  .then((data) => {
    swalSuccess("Rule evaluation submitted: " + data.evaluationName);
    if (callback) callback(null, data);
  })
  
  .catch((err) => {
    swalError("Failed to run rule evaluation: " + err.message);
    if (callback) callback(err);
  });
}

export function trainModelSubmit(experimentName, runName, model, config, task, trainRatio, valRatio, testRatio, seed, callback) {
  let query = "?";
  query += encodeQueryParameter("experimentName", experimentName, query);
  query += encodeQueryParameter("runName", runName, query);
  query += encodeQueryParameter("model", model, query);
  query += encodeQueryParameter("config", JSON.stringify(config), query);
  query += encodeQueryParameter("task", task, query);
  query += encodeQueryParameter("trainRatio", trainRatio, query);
  query += encodeQueryParameter("valRatio", valRatio, query);
  query += encodeQueryParameter("testRatio", testRatio, query);
  query += encodeQueryParameter("seed", seed, query);

  fetch(endpoints.TRAIN + "/" + model + "/submit" + query, { method: "POST" })
  
  .then(async (response) => {
    if (!response.ok) {
      throw new Error(await response.text());
    }
    return response.json();
  })
  
  .then((data) => {
    swalSuccess("Training job submitted: " + data.jobId);
    if (callback) callback(null, data);
  })
  
  .catch((err) => {
    swalError("Failed to submit training job: " + err.message);
    if (callback) callback(err);
  });
}

export function mlJobSubmit(action, experimentName, runName, model, config, task, trainRatio, valRatio, testRatio, seed, callback) {
  let query = "?";
  query += encodeQueryParameter("action", action, query);
  query += encodeQueryParameter("experimentName", experimentName, query);
  query += encodeQueryParameter("runName", runName, query);
  query += encodeQueryParameter("model", model, query);
  query += encodeQueryParameter("config", JSON.stringify(config), query);
  query += encodeQueryParameter("task", task, query);
  query += encodeQueryParameter("trainRatio", trainRatio, query);
  query += encodeQueryParameter("valRatio", valRatio, query);
  query += encodeQueryParameter("testRatio", testRatio, query);
  query += encodeQueryParameter("seed", seed, query);

  fetch(endpoints.RUNJOB + query, { method: "POST" })
  
  .then(async (response) => {
    if (!response.ok) {
      throw new Error(await response.text());
    }
    return response.json();
  })
  
  .then((data) => {
    swalSuccess("Training job submitted: " + data.jobId);
    if (callback) callback(null, data);
  })
  
  .catch((err) => {
    swalError("Failed to submit training job: " + err.message);
    if (callback) callback(err);
  });
}

export function stopTune(callback) {
  fetch(endpoints.STOPTUNE, { method: "POST" })
    .then(async (response) => {
      if (!response.ok) {
        throw new Error(await response.text());
      }
      return response.json();
    })
    .then((data) => {
      swalSuccess("Tuning job stopped");
      if (callback) callback(null, data);
    })
    .catch((err) => {
      swalError("Failed to stop tuning job: " + err.message);
      if (callback) callback(err);
    });
}

export function seqCompilerCompile(spaceID, groupID, weight, name, Rz, L, term, hp5, prom, eI, eO, s, invert, invL, agL, AGiloop, otype, rna, us, ds, temp_len, cp, n, c, d, CDS, rflap, downloadGenbank, callback) {
  let query = "?";
  query += encodeQueryParameter("spaceID", spaceID, query);
  query += encodeQueryParameter("groupID", groupID, query);
  query += encodeQueryParameter("weight", weight, query);

  query += encodeQueryParameter("name", name, query);
  query += encodeQueryParameter("Rz", Rz, query);
  query += encodeQueryParameter("L", L, query);
  query += encodeQueryParameter("term", term, query);
  query += encodeQueryParameter("hp5", hp5, query);
  query += encodeQueryParameter("prom", prom, query);
  query += encodeQueryParameter("eI", eI, query);
  query += encodeQueryParameter("eO", eO, query);
  query += encodeQueryParameter("s", s, query);
  query += encodeQueryParameter("invert", invert, query);
  query += encodeQueryParameter("invL", invL, query);
  query += encodeQueryParameter("agL", agL, query);
  query += encodeQueryParameter("AGiloop", AGiloop, query);
  query += encodeQueryParameter("otype", otype, query);
  query += encodeQueryParameter("rna", rna, query);
  query += encodeQueryParameter("us", us, query);
  query += encodeQueryParameter("ds", ds, query);
  query += encodeQueryParameter("temp_len", temp_len, query);
  query += encodeQueryParameter("cp", cp, query);
  query += encodeQueryParameter("n", n, query);
  query += encodeQueryParameter("c", c, query);
  query += encodeQueryParameter("d", d, query);
  query += encodeQueryParameter("CDS", CDS, query);
  query += encodeQueryParameter("rflap", rflap, query);

  fetch(endpoints.SEQCOMPILER + query, { method: "POST" })
    .then(async (response) => {
      if (!response.ok) {
        throw new Error(await response.text());
      }
      return response.json();
    })
    .then((data) => {
      const genbankText =
        data.genbank_text ??
        (Number(rna) === 1 ? data.genbank_rna : data.genbank_dna);

      if (downloadGenbank && genbankText && genbankText.trim().length > 0) {
        const blob = new Blob([genbankText], { type: "text/plain;charset=utf-8" });
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = (spaceID || "compiled_sequence") + ".gbk";
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
      }

      swalSuccess("Sequence compiled successfully.");
      if (callback) callback(null, data);
    })
    .catch((err) => {
      swalError("Failed to compile sequence: " + err.message);
      if (callback) callback(err);
    });
}

export function enumerateCSV(id, numDesigns, minLength, maxLength, maxCycles, bfs, isWeighted, isSampleSpace, allowDuplicates, callback){
  let query = "?targetSpaceID=" + encodeURIComponent(id);
  query += "&numDesigns=" + encodeURIComponent(numDesigns);
  query += "&minLength=" + encodeURIComponent(minLength);
  query += "&maxLength=" + encodeURIComponent(maxLength);
  query += "&maxCycles=" + encodeURIComponent(maxCycles);
  query += "&bfs=" + encodeURIComponent(bfs);
  query += "&isWeighted=" + encodeURIComponent(isWeighted);
  query += "&isSampleSpace=" + encodeURIComponent(isSampleSpace);
  query += "&allowDuplicates=" + encodeURIComponent(allowDuplicates);
  
  fetch(endpoints.ENUMERATECSV + query)
    .then(response => {
      if (!response.ok) throw new Error("CSV export failed");
      return response.blob();
    })
    .then(blob => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = id + "_enumerated_designs.csv";
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    })
    .catch(err => swalError("CSV export error: " + err.message));
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
