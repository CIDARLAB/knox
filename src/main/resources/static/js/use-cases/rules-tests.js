// Tests for GOLDBAR Rules

// Test for do not repeat A
let testPassR = [
    // True Passes
    "(A then B then C)",
    "(B then A then C)",
    "(C then B then A)",
    "(B then C then B)",
    "(A)",
    "(C)"
];
let testFailR = [
    // True Fails
    "(A then B then A)",
    "(A then B then C then C then A)",
    "(A then A)",
    "(B then A then C then A)",
    "(C then A then A then B)"
];

// Test for A before B
let testPassB = [
    // True Passes
    "(A then B)",
    "(C)",
    "(A)",
    "(B)",
    "(A then C then B)",
    "(C then A then B)",
    "(A then B then C)",
    "(A then A then B)",
    "(A then B then B)",
    "(C then A then B then B)",
    "(C then A then A then B)",
    "(C then A then C then C then B)",
    "(A then A then C)",
    "(C then A then A)",
    "(B then B then C)",
    "(C then B then B)"
];
let testFailB = [
    // True Fails
    "(B then A)",
    "(B then C then A)",
    "(A then B then A)",
    "(C then B then A then B)",
    "(C then B then A)"
];

// Test for A and B together
let testPassT = [
    // True Passes
    "(C)",
    "(A then B)",
    "(B then A)",
    "(C then B then A)",
    "(C then A then B)",
    "(A then C then B)",
    "(B then C then A)",
    "(C then A then A then B)",
    "(C then A then B then B)",
    "(C then B then B then A)",
    "(A then C then B then C then A then C)"
];
let testFailT = [
    // True Fails
    "(A)",
    "(B)",
    "(A then A)",
    "(B then B)",
    "(A then C then A)",
    "(B then C then B)",
    "(C then C then C then B)",
    "(C then C then C then A)"
];

// Test for A not followed by B
let testPassI = [
    // True Passes
    "(A)",
    "(B)",
    "(C)",
    "(B then A)",
    "(A then C)",
    "(A then A)",
    "(B then B)",
    "(A then C then B)",
    "(C then B then A)",
    "(B then C then A)"
];
let testFailI = [
    // True Fails
    "(A then B)",
    "(C then A then B)",
    "(B then A then B)",
    "(A then C then A then B)",
    "(A then B then A then C)",
    "(C then B then A then B then C then A)"
];

// Test for A must be followed by B
let testPassF = [
    // True Passes
    "(A then B)",
    "(C)",
    "(B then A then B)",
    "(A then B then C)",
    "(C then A then B)",
    "(C then B then A then B)",
    "(B then B)",
    "(A then B then B)"
];
let testFailF = [
    // True Fails
    "(A)",
    "(B then A)",
    "(A then A)",
    "(C then A then C)",
    "(B then A then A)",
    "(A then C then A then A)",
    "(C then B then C then B then C then A)",
    "(C then B then A)",
    "(A then B then A)"
];

// Test for A only comes after B
let testPassA = [
    // True Passes
    "(B then A)",
    "(C)",
    "(B then A then B)",
    "(B then B then A)",
    "(C then B then A)",
    "(C then B then A then B then A)",
    "(C then B then A then B)",
    "(B then B)"
];
let testFailA = [
    // True Fails
    "(A)",
    "(A then A)",
    "(C then A then C)",
    "(B then A then A)",
    "(A then C then A then A)",
    "(C then B then C then B then C then A)",
    "(A then B then A)",
    "(A then B then C)"
];

// Test for must include A
let testPassM = [
    // True Passes
    "(A then B then B then B)",
    "(A then A then B then A)",
    "(B then B then B then A)",
    "(B then A then B then A)",
    "(A then B then C)",
    "(A then A then B then A then A)",
    "(A)"
];
let testFailM = [
    // True Fails
    "(B then C)",
    "(C then B then B)",
    "(B then B)",
    "(B)"
];

// Test for not include A
let testPassNI = [
    // True Passes
    "(B then B then B)",
    "(B then C)",
    "(B then B then B then T1)",
    "(B then C then B then C)",
    "(C then B then C)",
    "(T1 then T2 then B then C then C)",
    "(B)"
];
let testFailNI = [
    // True Fails
    "(B then A)",
    "(C then A then B)",
    "(A then B)",
    "(A)"
];

// Test for end with A
let testPassE = [
    // True Passes
    "(A)",
    "(C then A)",
    "(A then B then A)",
    "(B then B then B then A)",
    "(A then A then B then A then A)"];
let testFailE =[
    // True Fails
    "(A then B then B then B)",
    "(A then B then C)",
    "(B then C)",
    "(B)"
];

// Test for start with A
let testPassS = [
    // True Passes
    "(A)",
    "(A then C)",
    "(A then B then A)",
    "(A then A then B then A then A)"
];
let testFailS =[
    // True Fails
    "(B then A then B then B)",
    "(C then A then C)",
    "(B then A then A then B)",
    "(B)"
];

// Test for A and B not Together (Not Orthogonal)
let testPassO = [
    // True Passes
    "(A then A)",
    "(B then B)",
    "(A)",
    "(B)",
    "(A then C)",
    "(C then A)",
    "(B then C)",
    "(C then B)",
    "(A then C then A)",
    "(B then C then B)",
    "(B then B then B)",
    "(A then A then A)",
    "(A then C then C)",
    "(B then C then C)",
    "(C then C then A then A then C",
    "(C then C then B then B then C"
];
let testFailO = [
    // True Fails
    "(A then B)",
    "(B then A)",
    "(A then C then B)",
    "(B then C then A)",
    "(C then A then B)",
    "(C then B then A)",
    "(C then A then C then B)",
    "(C then B then C then A)",
    "(A then A then B then B)",
    "(B then B then A then A)",
    "(A then C then B then B)",
    "(B then A then B then C)"
];

// Test for designs with only 4 parts
let testPassN = [
    // True Passes
    "(A then B then C then A)",
    "(A then A then A then A)",
    "(A then B then A then B)"
];
let testFailN = [
    // True Fails
    "(A then B then C)",
    "(A then A)",
    "(A then B then A then B then C)",
    "(A)"
];

// Test for Leaky Terminators
let testPassL = [
    // True Passes
    "(T3)",
    "(T1 then T3)",
    "(P1 then A then T1 then P2 then B then T3)"
];
let testFailL = [
    // True Fails
    "(A then T3 then B then T1)",
    "(B then T3 then T2)"
];

// Test for promoter road blocking
let testPassP = [
    // True Passes
    "(P3 then A then T1)",
    "(P3 then P1 then B then T3)",
    "(P3 then A then T2 then P3 then P1 then B then T1)"
];
let testFailP = [
    // True Fails
    "(P1 then P3 then A)",
    "(P1 then A then T1 then P2 then P3 then C then T3)"
];


export let ruleTests = {
    "R": [testPassR, testFailR],
    "B": [testPassB, testFailB],
    "T": [testPassT, testFailT],
    "I": [testPassI, testFailI],
    "F": [testPassF, testFailF],
    "A": [testPassA, testFailA],
    "M": [testPassM, testFailM],
    "NI": [testPassNI, testFailNI],
    "E": [testPassE, testFailE],
    "S": [testPassS, testFailS],
    "O": [testPassO, testFailO],
    "N": [testPassN, testFailN], 
    "L": [testPassL, testFailL],
    "P": [testPassP, testFailP]
};
