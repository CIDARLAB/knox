
// <rebFH operon> then <rebOD operon> then <rebCP operon> then <rebT operon>
let rebeccamycinAglyconBiosynthesisGeneCluster =
  'PT7_a then RiboJ then rebF then rebH then ECK120033736 then ' +
  'PT7_b then RiboJ10 then rebO then rebD then ECK120029600 then ' +
  'PT7_c then RiboJ51 then rebC then rebP then ECK120033737 then ' +
  'PT7_d then AraJ then rebT then galK then rmlA then L3S2P55';

// <rebG TU> then <rebODCPM operon> then <rebRFUH operon> then <rebT TU>
let rebeccamycinBiosynthesisGeneCluster =
  'p1 then rebG then t1 then ' +
  'p2 then rebO then rebD then rebC then rebP then rebM then t2 then ' +
  'p3 then rebR then rebF then rebU then rebH then t3 then ' +
  'p4 then rebT then t4';

export let rebeccamycinGOLDBAR = `(${rebeccamycinAglyconBiosynthesisGeneCluster}) merge (${rebeccamycinBiosynthesisGeneCluster})`;

export let biosynthesisCategories = {
  "PT7_a":{
    "promoter": [
      "PT7_WTa",
      "PT7_3a",
      "PT7_1a"
    ]
  },
  "PT7_b":{
    "promoter": [
      "PT7_WTb",
      "PT7_3b",
      "PT7_1b"
    ]
  },
  "PT7_c":{
    "promoter": [
      "PT7_WTc",
      "PT7_1c"
    ]
  },
  "PT7_d":{
    "promoter": [
      "PT7_WTd",
      "PT7_3d",
      "PT7_1d"
    ]
  },
  "p1":{
    "promoter": [
      "p1"
    ]
  },
  "p2":{
    "promoter": [
      "p2"
    ]
  },
  "p3":{
    "promoter": [
      "p3"
    ]
  },
  "p4":{
    "promoter": [
      "p4"
    ]
  },
  "RiboJ":{
    "ribozyme": [
      "RiboJ"
    ]
  },
  "RiboJ10":{
    "ribozyme": [
      "RiboJ10"
    ]
  },
  "RiboJ51":{
    "ribozyme": [
      "RiboJ51"
    ]
  },
  "AraJ":{
    "ribozyme": [
      "AraJ"
    ]
  },
  "rebG":{
    "cds": [
      "rebG"
    ]
  },
  "rebU":{
    "cds": [
      "rebU"
    ]
  },
  "rebR":{
    "cds": [
      "rebR"
    ]
  },
  "rebF":{
    "cds": [
      "rebF"
    ]
  },
  "rebH":{
    "cds": [
      "rebH"
    ]
  },
  "rebO":{
    "cds": [
      "rebO"
    ]
  },
  "rebD":{
    "cds": [
      "rebD"
    ]
  },
  "rebC":{
    "cds": [
      "rebC"
    ]
  },
  "rebP":{
    "cds": [
      "rebP"
    ]
  },
  "rebM":{
    "cds": [
      "rebM"
    ]
  },
  "rebT":{
    "cds": [
      "rebT"
    ]
  },
  "galK":{
    "cds": [
      "galK"
    ]
  },
  "rmlA":{
    "cds": [
      "rmlA"
    ]
  },
  "ECK120033736":{
    "terminator": [
      "ECK120033736"
    ]
  },
  "ECK120029600":{
    "terminator": [
      "ECK120029600"
    ]
  },
  "ECK120033737":{
    "terminator": [
      "ECK120033737"
    ]
  },
  "L3S2P55":{
    "terminator": [
      "L3S2P55"
    ]
  },
  "t1":{
    "terminator": [
      "t1"
    ]
  },
  "t2":{
    "terminator": [
      "t2"
    ]
  },
  "t3":{
    "terminator": [
      "t3"
    ]
  },
  "t4":{
    "terminator": [
      "t4"
    ]
  }
};
