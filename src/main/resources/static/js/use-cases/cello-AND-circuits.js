
// one-or-more(<PhlF TU> or <SrpR TU> or <AmtR TU>)
let oneOrMoreTU =
  'one-or-more((' +
  '(pSrpR then pAmtR) or ' +
  '(pAmtR then pSrpR) then RiboJ53 then P3 then PhlF then ECK120033737) or ' +
  '(pTet then RiboJ10 then S2 then SrpR then ECK120029600) or ' +
  '(pTac then BydvJ then A1 then AmtR then L3S2P55))';

let exactlyOnePhlf =
  'zero-or-more(Cello_minus_PhlF) then PhlF then zero-or-more(Cello_minus_PhlF)';

let exactlyOneSrpr =
  'zero-or-more(Cello_minus_SrpR) then SrpR then zero-or-more(Cello_minus_SrpR)';

let exactlyOneAmtr =
  'zero-or-more(Cello_minus_AmtR) then AmtR then zero-or-more(Cello_minus_AmtR)';

// Architecture for Cello-like circuit with no roadblocking
// one-or-more(<TU with no roadblocking>)
let noRoadblocking =
  'one-or-more(any_promoter then zero-or-one(Cello_promoter_2) then ' +
  'any_ribozyme then any_RBS then any_CDS then any_terminator)';

// Architecture for Cello-like circuit with roadblocking
// zero-or-more(<TU with no roadblocking>) then <TU with roadblocking> then zero-or-more(<TU with roadblocking> or <TU with no roadblocking>)
let withRoadblocking =
  'zero-or-more(any_promoter then zero-or-one(Cello_promoter_2) then ' +
  'any_ribozyme then any_RBS then any_CDS then any_terminator) then ' +
  'any_promoter then Cello_RB_promoter then any_ribozyme then any_RBS then any_CDS then any_terminator then ' +
  'zero-or-more((any_promoter then Cello_RB_promoter then any_ribozyme then any_RBS then any_CDS then any_terminator) or ' +
  '(any_promoter then zero-or-one(Cello_promoter_2) then any_ribozyme then any_RBS then any_CDS then any_terminator))';

/*******************************
 * AND tolerance=0: Refinement *
 *******************************/
// Cello AND circuit with single copy number TUs in any order
export let and0GOLDBAR = `(${oneOrMoreTU}) and0 (${exactlyOnePhlf}) and0 (${exactlyOneSrpr}) and0 (${exactlyOneAmtr})`;


/*****************************************
 * AND tolerance=1: Querying, Validation *
 *****************************************/
// Cello AND circuit variants with NO roadblocking
export let and1GOLDBAR_NORB = `(${noRoadblocking}) and1 ((${oneOrMoreTU}) and0 (${exactlyOnePhlf}) and0 (${exactlyOneSrpr}) and0 (${exactlyOneAmtr}))`;

// Cello AND circuit variants WITH roadblocking
export let and1GOLDBAR_RB = `(${withRoadblocking}) and1 ((${oneOrMoreTU}) and0 (${exactlyOnePhlf}) and0 (${exactlyOneSrpr}) and0 (${exactlyOneAmtr}))`;


/**********************************************
 * AND tolerance=2: Composition/Instantiation *
 **********************************************/
// Cello circuit with variable copy number PhlF TU
let variablePhlf = 'one-or-more((((pSrpR then pAmtR) or (pAmtR then pSrpR)) then RiboJ53 then P3 then PhlF then ECK120033737) or ' +
'(any_promoter then zero-or-one(any_promoter) then any_ribozyme then any_RBS then any_CDS then any_terminator))';

// Cello circuit with variable copy number SrpR TU
let variableSrpr = 'one-or-more((pTet then RiboJ10 then S2 then SrpR then ECK120029600) or ' +
'(any_promoter then zero-or-one(any_promoter) then any_ribozyme then any_RBS then any_CDS then any_terminator))';

// Cello circuit with variable copy number AmtR TU
let variableAmtr = 'one-or-more((pTac then BydvJ then A1 then AmtR then L3S2P55) or ' +
'(any_promoter then zero-or-one(any_promoter) then any_ribozyme then any_RBS then any_CDS then any_terminator))';

export let and2GOLDBAR = `(${variablePhlf} and2 ${variableSrpr}) and1 ${variableAmtr}`;
