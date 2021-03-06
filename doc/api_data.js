define({ "api": [
  {
    "type": "post",
    "url": "/branch/and",
    "title": "AND",
    "name": "andBranches",
    "group": "Branch",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String[]",
            "optional": false,
            "field": "targetSpaceID",
            "description": "<p>ID for the target design space containing the input branches to be AND-ed.</p>"
          },
          {
            "group": "Parameter",
            "type": "String[]",
            "optional": false,
            "field": "inputSpaceIDs",
            "description": "<p>IDs for the input branches to be AND-ed.</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": true,
            "field": "outputSpaceID",
            "description": "<p>ID for the output branch resulting from AND. If omitted, then the result is stored in the first input branch.</p>"
          },
          {
            "group": "Parameter",
            "type": "Integer",
            "allowedValues": [
              "0",
              "1",
              "2",
              "3",
              "4"
            ],
            "optional": false,
            "field": "tolerance",
            "defaultValue": "1",
            "description": "<p>This parameter determines the criteria by which edges are matched. If tolerance = 0, then matching edges must be labeled with the same component IDs and roles. If tolerance = 1 or 2, then matching edges must share at least one component ID and role. If tolerance = 3, then matching edges must be labeled with the same component roles. If tolerance = 4, then matching edges must share at least one component role. In any case, matching edges must be labeled with the same orientation. If tolerance &lt;= 1, then labels on matching edges are intersected; otherwise, they are unioned.</p>"
          },
          {
            "group": "Parameter",
            "type": "Boolean",
            "optional": false,
            "field": "isComplete",
            "defaultValue": "true",
            "description": "<p>If true, then only edges belonging to paths for designs common to all input design spaces are retained.</p>"
          },
          {
            "group": "Parameter",
            "type": "String[]",
            "optional": true,
            "field": "roles",
            "description": "<p>If specified, then only edges labeled with at least one of these roles will be AND-ed.</p>"
          }
        ]
      }
    },
    "description": "<p>Intersects designs from input branches. Based on tensor product of graphs.</p>",
    "version": "0.0.0",
    "filename": "./src/main/java/knox/spring/data/neo4j/controller/KnoxController.java",
    "groupTitle": "Branch"
  },
  {
    "type": "post",
    "url": "/branch",
    "title": "Checkout",
    "name": "checkoutBranch",
    "group": "Branch",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "targetSpaceID",
            "description": "<p>ID for the target design space.</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "targetBranchID",
            "description": "<p>ID for the target branch.</p>"
          }
        ]
      }
    },
    "description": "<p>Checks out the target branch as the new head, copying its latest committed snapshot to the contents of the target design space.</p>",
    "version": "0.0.0",
    "filename": "./src/main/java/knox/spring/data/neo4j/controller/KnoxController.java",
    "groupTitle": "Branch"
  },
  {
    "type": "post",
    "url": "/branch",
    "title": "CommitTo",
    "name": "commitToBranch",
    "group": "Branch",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "targetSpaceID",
            "description": "<p>ID for the target design space.</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "targetBranchID",
            "description": "<p>ID for the target branch.</p>"
          }
        ]
      }
    },
    "description": "<p>Commits a snapshot of the target design space to the target branch.</p>",
    "version": "0.0.0",
    "filename": "./src/main/java/knox/spring/data/neo4j/controller/KnoxController.java",
    "groupTitle": "Branch"
  },
  {
    "type": "post",
    "url": "/branch",
    "title": "Create",
    "name": "createBranch",
    "group": "Branch",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "targetSpaceID",
            "description": "<p>ID for the target design space.</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "outputBranchID",
            "description": "<p>ID for the output branch.</p>"
          }
        ]
      }
    },
    "description": "<p>Creates a new branch that is a copy of the current head branch.</p>",
    "version": "0.0.0",
    "filename": "./src/main/java/knox/spring/data/neo4j/controller/KnoxController.java",
    "groupTitle": "Branch"
  },
  {
    "type": "delete",
    "url": "/branch",
    "title": "Delete",
    "name": "deleteBranch",
    "group": "Branch",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "targetSpaceID",
            "description": "<p>ID for the target design space.</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "targetBranchID",
            "description": "<p>ID for the target branch.</p>"
          }
        ]
      }
    },
    "description": "<p>Deletes the target branch.</p>",
    "version": "0.0.0",
    "filename": "./src/main/java/knox/spring/data/neo4j/controller/KnoxController.java",
    "groupTitle": "Branch"
  },
  {
    "type": "post",
    "url": "/branch/join",
    "title": "Join",
    "name": "joinBranches",
    "group": "Branch",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String[]",
            "optional": false,
            "field": "targetSpaceID",
            "description": "<p>ID for the target design space containing the input branches to be joined.</p>"
          },
          {
            "group": "Parameter",
            "type": "String[]",
            "optional": false,
            "field": "inputBranchIDs",
            "description": "<p>IDs for the input branches to be joined.</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": true,
            "field": "outputBranchID",
            "description": "<p>ID for the output branch resulting from Join. If omitted, then the result is stored in the first input branch.</p>"
          }
        ]
      }
    },
    "description": "<p>Concatenates designs from input branches.</p>",
    "version": "0.0.0",
    "filename": "./src/main/java/knox/spring/data/neo4j/controller/KnoxController.java",
    "groupTitle": "Branch"
  },
  {
    "type": "post",
    "url": "/branch/merge",
    "title": "Merge",
    "name": "mergeBranches",
    "group": "Branch",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String[]",
            "optional": false,
            "field": "targetSpaceID",
            "description": "<p>ID for the target design space containing the branches to be merged.</p>"
          },
          {
            "group": "Parameter",
            "type": "String[]",
            "optional": false,
            "field": "inputSpaceIDs",
            "description": "<p>IDs for the input branches to be merged.</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": true,
            "field": "outputSpaceID",
            "description": "<p>ID for the output branch resulting from Merge. If omitted, then the result is stored in the first input branch.</p>"
          },
          {
            "group": "Parameter",
            "type": "Integer",
            "allowedValues": [
              "0",
              "1",
              "2",
              "3",
              "4"
            ],
            "optional": false,
            "field": "tolerance",
            "defaultValue": "1",
            "description": "<p>This parameter determines the criteria by which edges are matched. If tolerance = 0, then matching edges must be labeled with the same component IDs and roles. If tolerance = 1 or 2, then matching edges must share at least one component ID and role. If tolerance = 3, then matching edges must be labeled with the same component roles. If tolerance = 4, then matching edges must share at least one component role. In any case, matching edges must be labeled with the same orientation. If tolerance &lt;= 1, then labels on matching edges are intersected; otherwise, they are unioned.</p>"
          },
          {
            "group": "Parameter",
            "type": "Boolean",
            "optional": false,
            "field": "isComplete",
            "defaultValue": "false",
            "description": "<p>If true, then only edges belonging to paths for designs common to all input design spaces are retained.</p>"
          },
          {
            "group": "Parameter",
            "type": "String[]",
            "optional": true,
            "field": "roles",
            "description": "<p>If specified, then only edges labeled with at least one of these roles will be merged.</p>"
          }
        ]
      }
    },
    "description": "<p>Merges designs from input design spaces. Based on strong product of graphs.</p>",
    "version": "0.0.0",
    "filename": "./src/main/java/knox/spring/data/neo4j/controller/KnoxController.java",
    "groupTitle": "Branch"
  },
  {
    "type": "post",
    "url": "/branch/or",
    "title": "OR",
    "name": "orBranches",
    "group": "Branch",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String[]",
            "optional": false,
            "field": "targetSpaceID",
            "description": "<p>ID for the target design space containing the input branches to be OR-ed.</p>"
          },
          {
            "group": "Parameter",
            "type": "String[]",
            "optional": false,
            "field": "inputBranchIDs",
            "description": "<p>IDs for the input branches to be OR-ed.</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": true,
            "field": "outputBranchID",
            "description": "<p>ID for the output branch resulting from OR. If omitted, then the result is stored in the first input branch.</p>"
          }
        ]
      }
    },
    "description": "<p>Unions designs from input branches.</p>",
    "version": "0.0.0",
    "filename": "./src/main/java/knox/spring/data/neo4j/controller/KnoxController.java",
    "groupTitle": "Branch"
  },
  {
    "type": "post",
    "url": "/branch/repeat",
    "title": "Repeat",
    "name": "repeatBranches",
    "group": "Branch",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String[]",
            "optional": false,
            "field": "targetSpaceID",
            "description": "<p>ID for the target design space containing the input branches to be repeated.</p>"
          },
          {
            "group": "Parameter",
            "type": "String[]",
            "optional": false,
            "field": "inputBranchIDs",
            "description": "<p>IDs for the input branches to be repeated.</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": true,
            "field": "outputBranchID",
            "description": "<p>ID for the output branch resulting from Repeat. If omitted, then the result is stored in the first input branch.</p>"
          },
          {
            "group": "Parameter",
            "type": "Boolean",
            "optional": false,
            "field": "isOptional",
            "defaultValue": "false",
            "description": "<p>If true, then designs from the input branches are repeated zero-or-more times; otherwise, they are repeated one-or-more times.</p>"
          }
        ]
      }
    },
    "description": "<p>Concatenates and then repeats designs from input branches either zero-or-more or one-or-more times.</p>",
    "version": "0.0.0",
    "filename": "./src/main/java/knox/spring/data/neo4j/controller/KnoxController.java",
    "groupTitle": "Branch"
  },
  {
    "type": "post",
    "url": "/branch",
    "title": "Reset",
    "name": "resetBranch",
    "group": "Branch",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "targetSpaceID",
            "description": "<p>ID for the target design space.</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": true,
            "field": "targetBranchID",
            "description": "<p>ID for the target branch. If omitted, the head branch is reset.</p>"
          },
          {
            "group": "Parameter",
            "type": "String[]",
            "optional": false,
            "field": "commitPath",
            "description": "<p>List of commit IDs beginning with latest commit and ending with the commit to which the target branch is to be reset.</p>"
          }
        ]
      }
    },
    "description": "<p>Resets the target branch to a previously committed snapshot. No record of the reset is preserved.</p>",
    "version": "0.0.0",
    "filename": "./src/main/java/knox/spring/data/neo4j/controller/KnoxController.java",
    "groupTitle": "Branch"
  },
  {
    "type": "post",
    "url": "/branch",
    "title": "Revert",
    "name": "revertBranch",
    "group": "Branch",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "targetSpaceID",
            "description": "<p>ID for the target design space.</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": true,
            "field": "targetBranchID",
            "description": "<p>ID for the target branch. If omitted, the head branch is reverted.</p>"
          },
          {
            "group": "Parameter",
            "type": "String[]",
            "optional": false,
            "field": "commitPath",
            "description": "<p>List of commit IDs beginning with latest commit and ending with the commit to which the target branch is to be reverted.</p>"
          }
        ]
      }
    },
    "description": "<p>Reverts the target branch to a previously committed snapshot by copying the contents of the latter to a new latest commit on the target branch. This preserves a record of the reversion.</p>",
    "version": "0.0.0",
    "filename": "./src/main/java/knox/spring/data/neo4j/controller/KnoxController.java",
    "groupTitle": "Branch"
  },
  {
    "success": {
      "fields": {
        "Success 200": [
          {
            "group": "Success 200",
            "optional": false,
            "field": "varname1",
            "description": "<p>No type.</p>"
          },
          {
            "group": "Success 200",
            "type": "String",
            "optional": false,
            "field": "varname2",
            "description": "<p>With type.</p>"
          }
        ]
      }
    },
    "type": "",
    "url": "",
    "version": "0.0.0",
    "filename": "./doc/main.js",
    "group": "C__Users_nroehner_git_knox_doc_main_js",
    "groupTitle": "C__Users_nroehner_git_knox_doc_main_js",
    "name": ""
  },
  {
    "type": "post",
    "url": "/designSpace/and",
    "title": "AND",
    "name": "andDesignSpaces",
    "group": "DesignSpace",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String[]",
            "optional": false,
            "field": "inputSpaceIDs",
            "description": "<p>IDs for the input design spaces to be AND-ed.</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": true,
            "field": "outputSpaceID",
            "description": "<p>ID for the output design space resulting from AND. If omitted, then the result is stored in the first input design space.</p>"
          },
          {
            "group": "Parameter",
            "type": "Integer",
            "allowedValues": [
              "0",
              "1",
              "2",
              "3",
              "4"
            ],
            "optional": false,
            "field": "tolerance",
            "defaultValue": "1",
            "description": "<p>This parameter determines the criteria by which edges are matched. If tolerance = 0, then matching edges must be labeled with the same component IDs and roles. If tolerance = 1 or 2, then matching edges must share at least one component ID and role. If tolerance = 3, then matching edges must be labeled with the same component roles. If tolerance = 4, then matching edges must share at least one component role. In any case, matching edges must be labeled with the same orientation. If tolerance &lt;= 1, then labels on matching edges are intersected; otherwise, they are unioned.</p>"
          },
          {
            "group": "Parameter",
            "type": "Boolean",
            "optional": false,
            "field": "isComplete",
            "defaultValue": "true",
            "description": "<p>If true, then only the matching edges that belong to paths for designs common to all input design spaces are retained.</p>"
          },
          {
            "group": "Parameter",
            "type": "String[]",
            "optional": true,
            "field": "roles",
            "description": "<p>If specified, then only edges labeled with at least one of these roles will be AND-ed.</p>"
          }
        ]
      }
    },
    "description": "<p>Intersects designs from input design spaces. Based on tensor product of graphs.</p>",
    "version": "0.0.0",
    "filename": "./src/main/java/knox/spring/data/neo4j/controller/KnoxController.java",
    "groupTitle": "DesignSpace"
  },
  {
    "type": "post",
    "url": "/designSpace/delete",
    "title": "Delete",
    "name": "deletDesignSpace",
    "group": "DesignSpace",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String",
            "optional": false,
            "field": "targetSpaceID",
            "description": "<p>ID for the target design space to be deleted.</p>"
          }
        ]
      }
    },
    "description": "<p>Deletes the target design space.</p>",
    "version": "0.0.0",
    "filename": "./src/main/java/knox/spring/data/neo4j/controller/KnoxController.java",
    "groupTitle": "DesignSpace"
  },
  {
    "type": "post",
    "url": "/designSpace/join",
    "title": "Join",
    "name": "joinDesignSpaces",
    "group": "DesignSpace",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String[]",
            "optional": false,
            "field": "inputSpaceIDs",
            "description": "<p>IDs for the input design spaces to be joined.</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": true,
            "field": "outputSpaceID",
            "description": "<p>ID for the output design space resulting from Join. If omitted, then the result is stored in the first input design space.</p>"
          }
        ]
      }
    },
    "description": "<p>Concatenates designs from input design spaces.</p>",
    "version": "0.0.0",
    "filename": "./src/main/java/knox/spring/data/neo4j/controller/KnoxController.java",
    "groupTitle": "DesignSpace"
  },
  {
    "type": "post",
    "url": "/designSpace/merge",
    "title": "Merge",
    "name": "mergeDesignSpaces",
    "group": "DesignSpace",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String[]",
            "optional": false,
            "field": "inputSpaceIDs",
            "description": "<p>IDs for the input design spaces to be merged.</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": true,
            "field": "outputSpaceID",
            "description": "<p>ID for the output design space resulting from Merge.  If omitted, then the result is stored in the first input design space.</p>"
          },
          {
            "group": "Parameter",
            "type": "Integer",
            "allowedValues": [
              "0",
              "1",
              "2",
              "3",
              "4"
            ],
            "optional": false,
            "field": "tolerance",
            "defaultValue": "1",
            "description": "<p>This parameter determines the criteria by which edges are matched. If tolerance = 0, then matching edges must be labeled with the same component IDs and roles. If tolerance = 1 or 2, then matching edges must share at least one component ID and role. If tolerance = 3, then matching edges must be labeled with the same component roles. If tolerance = 4, then matching edges must share at least one component role. In any case, matching edges must be labeled with the same orientation. If tolerance &lt;= 1, then labels on matching edges are intersected; otherwise, they are unioned.</p>"
          },
          {
            "group": "Parameter",
            "type": "Boolean",
            "optional": false,
            "field": "isComplete",
            "defaultValue": "false",
            "description": "<p>If true, then only the matching edges that belong to paths for designs common to all input design spaces are retained prior to adding the non-matching edges from these spaces.</p>"
          },
          {
            "group": "Parameter",
            "type": "String[]",
            "optional": true,
            "field": "roles",
            "description": "<p>If specified, then only edges labeled with at least one of these roles will be AND-ed.</p>"
          }
        ]
      }
    },
    "description": "<p>Merges designs from input design spaces. Based on strong product of graphs.</p>",
    "version": "0.0.0",
    "filename": "./src/main/java/knox/spring/data/neo4j/controller/KnoxController.java",
    "groupTitle": "DesignSpace"
  },
  {
    "type": "post",
    "url": "/designSpace/or",
    "title": "OR",
    "name": "orDesignSpaces",
    "group": "DesignSpace",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String[]",
            "optional": false,
            "field": "inputSpaceIDs",
            "description": "<p>IDs for the input design spaces to be OR-ed.</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": true,
            "field": "outputSpaceID",
            "description": "<p>ID for the output design space resulting from OR.  If omitted, then the result is stored in the first input design space.</p>"
          }
        ]
      }
    },
    "description": "<p>Unions designs from input design spaces.</p>",
    "version": "0.0.0",
    "filename": "./src/main/java/knox/spring/data/neo4j/controller/KnoxController.java",
    "groupTitle": "DesignSpace"
  },
  {
    "type": "post",
    "url": "/designSpace/repeat",
    "title": "Repeat",
    "name": "repeatDesignSpaces",
    "group": "DesignSpace",
    "parameter": {
      "fields": {
        "Parameter": [
          {
            "group": "Parameter",
            "type": "String[]",
            "optional": false,
            "field": "inputSpaceIDs",
            "description": "<p>IDs for the input design spaces to be repeated.</p>"
          },
          {
            "group": "Parameter",
            "type": "String",
            "optional": true,
            "field": "outputSpaceID",
            "description": "<p>ID for the output design space resulting from Repeat. If omitted, then the result is stored in the first input design space.</p>"
          },
          {
            "group": "Parameter",
            "type": "Boolean",
            "optional": false,
            "field": "isOptional",
            "defaultValue": "false",
            "description": "<p>If true, then designs from the input spaces are repeated zero-or-more times; otherwise, they are repeated one-or-more times.</p>"
          }
        ]
      }
    },
    "description": "<p>Concatenates and then repeats designs from input design spaces either zero-or-more or one-or-more times.</p>",
    "version": "0.0.0",
    "filename": "./src/main/java/knox/spring/data/neo4j/controller/KnoxController.java",
    "groupTitle": "DesignSpace"
  }
] });
