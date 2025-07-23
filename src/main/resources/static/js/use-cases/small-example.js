export let concreteGOLDBAR = 
    'P1 then RBS1 then A then T1';

export let abstractGOLDBAR = 
    'promoter_abstract then rbs_abstract then cds_abstract then terminator_abstract';

export let concreteAndAbstractGOLDBAR = 
    'promoter_abstract then RBS3 then D then T2';

export let exampleCategories = {
    "promoter": {"promoter": ["P1", "P2", "P3"]},
    "promoter_abstract": {"promoter": []},
    "rbs": {"ribosomeBindingSite": ["RBS1", "RBS2", "RBS3"]},
    "rbs_abstract": {"ribosomeBindingSite": []},
    "cds": {"cds": ["A", "B", "C", "D"]},
    "cds_abstract": {"cds": []},
    "terminator": {"terminator": ["T1", "T2"]},
    "terminator_abstract": {"terminator": []},
    "P1": {"promoter": ["P1"]},
    "P2": {"promoter": ["P2"]},
    "P3": {"promoter": ["P3"]},
    "RBS1": {"ribosomeBindingSite": ["RBS1"]},
    "RBS2": {"ribosomeBindingSite": ["RBS2"]},
    "RBS3": {"ribosomeBindingSite": ["RBS3"]},
    "A": {"cds": ["A"]},
    "B": {"cds": ["B"]},
    "C": {"cds": ["C"]},
    "D": {"cds": ["D"]},
    "T1": {"terminator": ["T1"]},
    "T2": {"terminator": ["T2"]},
    "any_part_abstract": {"promoter": [], "cds": [], "ribsomeBindingSite": [], "terminator": []},
    "any_part_concrete": {"promoter": ["P1", "P2", "P3"], "cds": ["A", "B", "C", "D"], "ribsomeBindingSite": ["RBS1", "RBS2", "RBS3"], "terminator": ["T1", "T2"]}
};