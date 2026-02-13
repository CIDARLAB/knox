let doNotRepeatRule = 
    "(zero-or-more(any_except_A) then A then zero-or-more(any_except_A)) or (zero-or-more(any_except_A))";

let beforeRule = 
    "(zero-or-more(any_except_B) then A then zero-or-more(any_except_A)) or (zero-or-more(any_except_A))";

let togetherRule = 
    "((zero-or-more(any_part_concrete) then A then zero-or-more(any_except_AandB) then B then zero-or-more(any_except_A)) or " + 
    "(zero-or-more(any_part_concrete) then B then zero-or-more(any_except_AandB) then A then zero-or-more(any_except_B)) or " +
    "(zero-or-more(any_except_AandB)))";

let pjiRule = 
    "(zero-or-more((any_except_A) or (A then any_except_AandB))) then zero-or-more(A)";

let mustIncludeRule = 
    "one-or-more(zero-or-more(any_except_A) then A) then zero-or-more(any_except_A)";

let notIncludeRule = 
    "zero-or-more(any_except_A)";

let endRule = 
    "zero-or-more(any_part_concrete) then A";

let startRule = 
    "A then zero-or-more(any_part_concrete)";

let notOrthogonalRule = 
    "(zero-or-more(any_except_AandB)) or " +
    "(zero-or-more(any_except_B) then A then zero-or-more(any_except_AandB)) or " +
    "(zero-or-more(any_except_A) then B then zero-or-more(any_except_AandB))";

let lengthRule = 
    "any_part_concrete then any_part_concrete then any_part_concrete then any_part_concrete";

let leakyRule = 
    "zero-or-one(reverse-comp(terminator_leaky)) then " +
    "zero-or-more(forward-or-reverse(any_except_terminator_leaky)) then " + 
    "zero-or-one(terminator_leaky)";

// TODO: Correct Rule
let roadBlockingRule = 
    "zero-or-more(any_except_promoter) then zero-or-more(promoter then zero-or-one(any_promoter_except_roadBlocking) then one-or-more(any_except_promoter))";

export let exampleRules = {
    "R": doNotRepeatRule,
    "B": beforeRule,
    "T": togetherRule,
    "I": pjiRule,
    "M": mustIncludeRule,
    "NI": notIncludeRule,
    "E": endRule,
    "S": startRule,
    "O": notOrthogonalRule,
    "N": lengthRule,
    "L": leakyRule,
    "P": roadBlockingRule
};

export let ruleCategories = {
    "any_part_concrete": {"cds": ["A", "B", "C"], "terminator": ["T1", "T2", "T3"], "promoter": ["P1", "P2", "P3"]},
    "A": {"cds": ["A"]},
    "B": {"cds": ["B"]},
    "any_except_A": {"cds": ["B", "C"], "terminator": ["T1", "T2", "T3"], "promoter": ["P1", "P2", "P3"]},
    "any_except_B": {"cds": ["A", "C"], "terminator": ["T1", "T2", "T3"], "promoter": ["P1", "P2", "P3"]},
    "any_except_AandB": {"cds": ["C"], "terminator": ["T1", "T2", "T3"], "promoter": ["P1", "P2", "P3"]},
    "any_except_terminator_leaky": {"cds": ["A", "B", "C"], "terminator": ["T1", "T2"], "promoter": ["P1", "P2", "P3"]},
    "terminator": {"terminator": ["T1", "T2", "T3"]},
    "any_except_roadBlockingPromoter": {"promoter": ["P1", "P2"], "cds": ["A", "B", "C"], "terminator": ["T1", "T2", "T3"]},
    "promoter": {"promoter": ["P1", "P2", "P3"]},
    "terminator_leaky": {"terminator": ["T3"]},
    "roadBlockingPromoter": {"promoter": ["P3"]},
    "any_promoter_except_roadBlocking": {"promoter": ["P1", "P2"]},
    "any_except_promoter": {"cds": ["A", "B", "C"], "terminator": ["T1", "T2", "T3"]},

    // Extras Not used in rules above
    "C": {"cds": ["C"]},
    "P1": {"promoter": ["P1"]},
    "P2": {"promoter": ["P2"]},
    "P3": {"promoter": ["P3"]},
    "T1": {"terminator": ["T1"]},
    "T2": {"terminator": ["T2"]},
    "T3": {"terminator": ["T3"]}
};