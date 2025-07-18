
export function simplify(parsed) {
  parsed = simplifyRevComp(parsed);
  parsed = simplifyTree(parsed);
  return parsed;
}

/**
 * Simplifies GOLDBAR syntax by replacing nested operators with one equivalent operator when possible
 * @param parsed Parsed GOLDBAR syntax
 * @returns {*} Simplified GOLDBAR
 */
export function simplifyTree(parsed) {

  if (parsed.Atom) {
    return parsed;
  } else if (parsed.Or) {
    // simplify Or
    let exp1 = simplifyTree(parsed.Or[0]);
    let exp2 = simplifyTree(parsed.Or[1]);
    return simplifyOr(exp1, exp2);
  } else if (parsed.Then) {
    // simplify Then
    let exp1 = simplifyTree(parsed.Then[0]);
    let exp2 = simplifyTree(parsed.Then[1]);
    return simplifyThen(exp1, exp2);
  } else if (parsed.OneOrMore) {
    // simplify OneOrMore
    let exp = simplifyTree(parsed.OneOrMore[0]);
    return simplifyOneOrMore(exp);
  } else if (parsed.ZeroOrMore) {
    // simplify ZeroOrMore
    let exp = simplifyTree(parsed.ZeroOrMore[0]);
    return simplifyZeroOrMore(exp);
  } else if (parsed.ZeroOrOne) {
    let exp = simplifyTree(parsed.ZeroOrOne[0]);
    return simplifyZeroOrOne(exp);
  }
  return parsed;
}

export function simplifyOr(exp1, exp2) {
  // ... or ...
  // if (equalExps(exp1, exp2)) {
  if (exp1.Atom && exp2.Atom && equalExps(exp1, exp2)) {
    return exp1;
  } else if (exp1.OneOrMore && exp2.OneOrMore && equalExps(exp1.OneOrMore, exp2.OneOrMore)) {
    // one-or-more ... or one-or-more ... --> one-or-more ...
    return exp1;
  } else if (exp1.OneOrMore && exp2.ZeroOrMore && equalExps(exp1.OneOrMore, exp2.ZeroOrMore)) {
    // one-or-more ... or zero-or-more ... --> zero-or-more ...
    return exp2;
  } else if (exp1.OneOrMore && exp2.ZeroOrOne && equalExps(exp1.OneOrMore, exp2.ZeroOrOne)) {
    // one-or-more ... or zero-or-one ... --> zero-or-more ...
    return ZeroOrMore(exp2.ZeroOrOne[0]);
  }  else if (exp1.OneOrMore && equalExps(exp1.OneOrMore[0], exp2)) {
    // one-or-more ... or ... --> one-or-more ...
    return exp1;
  } else if (exp2.OneOrMore && equalExps(exp1, exp2.OneOrMore[0])) {
    // ... or one-or-more --> one-or-more ...
    return exp2;
  } else if (exp1.ZeroOrMore && exp2.OneOrMore && equalExps(exp1.ZeroOrMore, exp2.OneOrMore)) {
    // zero-or-more ... or one-or-more ... --> zero-or-more ...
    return exp1;
  } else if (exp1.ZeroOrMore && exp2.ZeroOrMore && equalExps(exp1.ZeroOrMore, exp2.ZeroOrMore)) {
    // zero-or-more ... or zero-or-more ... --> zero-or-more ...
    return exp1;
  } else if (exp1.ZeroOrMore && exp2.ZeroOrOne && equalExps(exp1.ZeroOrMore, exp2.ZeroOrOne)) {
    // zero-or-more ... or zero-or-one ... --> zero-or-more ...
    return exp1;
  } else if (exp1.ZeroOrMore && equalExps(exp1.ZeroOrMore[0], exp2)) {
    // zero-or-more ... or ... --> zero-or-more ...
    return exp1;
  } else if (exp2.ZeroOrMore && equalExps(exp1, exp2.ZeroOrMore[0])) {
    // ... or ZeroOrMore ... --> zero-or-more ...
    return exp2;
  }  else if (exp1.ZeroOrOne && exp2.OneOrMore && equalExps(exp1.ZeroOrOne, exp2.OneOrMore)) {
    // ZeroOrOne a OR OneOrMore a --> ZeroOrMore a
    return ZeroOrMore(exp1.ZeroOrOne[0]);
  } else if (exp1.ZeroOrOne && exp2.ZeroOrMore && equalExps(exp1.ZeroOrOne, exp2.ZeroOrMore)) {
    // ZeroOrOne a OR ZeroOrMore a --> ZeroOrMore a
    return exp2;
  } else if (exp1.ZeroOrOne && exp2.ZeroOrOne && !equalExps(exp1, exp2)) {
    // ZeroOrOne a OR ZeroOrOne b --> ZeroOrOne (a OR b)
    return ZeroOrOne(Or(exp1.ZeroOrOne, exp2.ZeroOrOne));
  } else if (exp1.ZeroOrOne && exp2.ZeroOrOne && equalExps(exp1, exp2)) {
    // ZeroOrOne a OR ZeroOrOne a --> ZeroOrOne a
    return exp1;
  } else if (exp1.ZeroOrOne && equalExps(exp1.ZeroOrOne[0], exp2)) {
    // ZeroOrOne a OR a --> ZeroOrOne a
    return exp1;
  } else if (exp2.ZeroOrOne && equalExps(exp2.ZeroOrOne[0], exp1)) {
    // a OR ZeroOrOne a --> ZeroOrOne a
    return exp2;
  }
  return Or(exp1, exp2);
}

export function simplifyThen(exp1, exp2) {
  if (exp1.OneOrMore && exp2.OneOrMore && equalExps(exp1.OneOrMore, exp2.OneOrMore)) {
    // one-or-more ... then one-or-more ... --> ... then one-or-more ...
    return Then(exp1.OneOrMore[0], exp2);
  } else if (exp1.OneOrMore && exp2.ZeroOrMore && equalExps(exp1.OneOrMore, exp2.ZeroOrMore)) {
    // one-or-more ... then zero-or-more ... --> one-or-more ...
    return exp1;
  } else if (exp1.OneOrMore && exp2.ZeroOrOne && equalExps(exp1.OneOrMore, exp2.ZeroOrOne)) {
    // one-or-more ... then zero-or-one ... --> one-or-more ...
    return exp1;
  } else if (exp1.ZeroOrMore && exp2.OneOrMore && equalExps(exp1.ZeroOrMore, exp2.OneOrMore)) {
    // zero-or-more ... then one-or-more ... --> one-or-more ...
    return exp2;
  } else if (exp1.ZeroOrMore && exp2.ZeroOrMore && equalExps(exp1.ZeroOrMore, exp2.ZeroOrMore)) {
    // zero-or-more ... then zero-or-more ... --> zero-or-more ...
    return exp1;
  }  else if (exp1.ZeroOrMore && exp2.ZeroOrOne && equalExps(exp1.ZeroOrMore, exp2.ZeroOrOne)) {
    // zero-or-more ... then zero-or-more ... --> zero-or-more ...
    return exp1;
  } else if (exp1.ZeroOrMore && equalExps(exp1.ZeroOrMore[0], exp2)) {
    // zero-or-more ... then ... --> one-or-more ...
    return OneOrMore(exp2);
  } else if (exp2.ZeroOrMore && equalExps(exp1, exp2.ZeroOrMore[0])) {
    // atom then zero-or-more atom --> one-or-more atom
    return OneOrMore(exp1);
  } else if (exp1.ZeroOrOne && exp2.OneOrMore && equalExps(exp1.ZeroOrOne, exp2.OneOrMore)) {
    return exp2;
  } else if (exp1.ZeroOrOne && exp2.ZeroOrMore && equalExps(exp1.ZeroOrOne, exp2.ZeroOrMore)) {
    return exp2;
  }
  return Then(exp1, exp2);
}

export function simplifyOneOrMore(exp) {
  // one-or-more(zero-or-more) => zero-or-more
  // one-or-more(one-or-more) => one-or-more
  // hence, we just return inner exp
  if (exp.ZeroOrMore || exp.OneOrMore) {
    return exp;
  } else if (exp.ZeroOrOne) {
    return ZeroOrMore(exp.ZeroOrOne[0]);
  } else {
    return OneOrMore(exp);
  }
}

export function simplifyZeroOrMore(exp) {
  if (exp.OneOrMore) {
    return ZeroOrMore(exp.OneOrMore[0]);
  } else if (exp.ZeroOrMore) {
    return exp;
  } else if (exp.ZeroOrOne) {
    return ZeroOrMore(exp.ZeroOrOne[0]);
  } else {
    return ZeroOrMore(exp);
  }
}

export function simplifyZeroOrOne(exp) {
  if (exp.OneOrMore) {
    return ZeroOrMore(exp.OneOrMore[0]);
  } else if (exp.ZeroOrMore || exp.ZeroOrOne) {
    return exp;
  }
  return ZeroOrOne(exp);
}

export function equalExps(exp1, exp2) {
  return JSON.stringify(exp1) === JSON.stringify((exp2));
}

export function Or(exp1, exp2) {
  return {Or: [exp1, exp2]};
}

export function Then(exp1, exp2) {
  return {Then: [exp1, exp2]};
}

export function OneOrMore(exp) {
  return {OneOrMore: [exp]};
}

export function ZeroOrMore(exp) {
  return {ZeroOrMore: [exp]};
}

export function ZeroOrOne(exp) {
  return {ZeroOrOne: [exp]};
}

export function ForwardOrReverse(exp) {
  return {ForwardOrReverse: [exp]};
}

/**
 * Pushes all the ReverseComp operators down to the Atom level
 * and replaces RevComp(Atom(atom)) with RevComp(atom)
 * @param parsed
 */
export function simplifyRevComp(parsed) {
  if (parsed.Atom) {
    return parsed;
  }
  if (parsed.ReverseComp) {
    // replace the RevComp with the top level from applyRevComp
    let ret = applyRevComp(parsed.ReverseComp);
    return ret[0];
  } else {
    let op = Object.keys(parsed)[0];
    let simplified = [];
    for (let part of parsed[op]) {
      simplified.push(simplifyRevComp(part));
    }
    parsed[op] = simplified;
    return parsed;
  }
}

/**
 * Applies the RevComp operator recursively until it reaches the atom level
 * @param rcExp
 * @return {Array}
 */
export function applyRevComp(rcExp) {
  let applied = [];
  for (let part of rcExp) {
    // if an atom is being reverse complemented, just return a ReverseComp of that part
    if (part.Atom) {
      applied.push({ReverseComp: [part.Atom]});
    } else if (part.ReverseComp) { // if a reverse complemented part is being reverse complemented, cancel it out
      applied.push(part.ReverseComp[0]);
    } else if (part.Then) { // if a THEN is being RC-ed, switch the order and apply to each part
      let swapped = [part.Then[1], part.Then[0]];
      part.Then = applyRevComp(swapped);
      applied.push(part);
    } else { // if something else is being reverse complemented, apply it to the sub parts until you hit the atoms
      let op = Object.keys(part)[0];
      part[op] = applyRevComp(part[op]);
      applied.push(part);
    }
  }
  return applied;
}
