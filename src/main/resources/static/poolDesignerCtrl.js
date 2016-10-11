function poolDesignerCtrl($scope) {
    $scope.searchSpaceID = "";
    $scope.deleteSpaceID = "";

    $scope.specNodes = [];
    $scope.poolNodes = [];

    $scope.specNode = function(pool) {
        this.pool = pool;
        this.labelColor = "";
        this.backgroundColor = "";
    };

    $scope.poolNode = function(pool) {
        this.pool = pool;
        this.labelColor = "color:#ffffff";
        this.backgroundColor = "background-color:#787878";
    };

    $scope.moveSpecNodeToTop = function(node) {
        if (node.labelcolor.length > 0) {
            $scope.poolNodes.splice($scope.specNodes.indexOf(node), 1);

            $scope.poolNodes.unshift(node);
        } else {
            $scope.specNodes.splice($scope.specNodes.indexOf(node), 1);

            $scope.specNodes.unshift(node);
        }
    };

    $scope.createSpecNode = function() {
         $scope.specNodes.push(new $scope.specNode("[promoter][ribosome_entry_site][CDS][terminator]"));
    }

    $scope.removeNode = function(node) {
        node.remove();
    };

    $scope.designPools = function() {
        var poolSpecs = [];

        var i;

        for (i = 0; i < $scope.specNodes.length; i++) {
            poolSpecs[i] = $scope.specNodes[i].pool;

            console.log(poolSpecs[i]);
        }

        d3.json("/design/pool").post(JSON.stringify(poolSpecs),
            function(error, result) {
                if (error) {
                    sweetAlert("Error", JSON.parse(error.response).message, "error");
                } else {
                    var pool;

                    var i;

                    for (i = 0; i < result.length; i++) {
                        pool = JSON.stringify(result[i]);

                        console.log(pool);

                        $scope.poolNodes[i] = new $scope.poolNode(pool.substring(1, pool.length - 1).replace(/\"/g, ""));

                        console.log($scope.poolNodes[i].pool);

                        console.log("------");
                    }
                }
            }
        );
    }

    $scope.deleteAll = function() {
        d3.xhr("/delete/all").post(function(error, request) {
            if (error) {
                sweetAlert("Error", JSON.parse(error.response).message, "error");
            }
        });
    };

}