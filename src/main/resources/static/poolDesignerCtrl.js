function poolDesignerCtrl($scope) {

    $scope.graphs = [];
    $scope.graphType = "ds1";
    $scope.prevGraphType = "ds1";
    $scope.isDSGraph = true;

    $scope.searchSpaceID = "";
    $scope.deleteSpaceID = "";

    $scope.specNodes = [];
    $scope.poolNodes = [];

    $scope.specNode = function(poolSpec) {
        this.poolSpec = poolSpec;
        this.labelColor = "";
        this.backgroundColor = "";
    };

    $scope.poolNode = function(poolSpec) {
        this.poolSpec = poolSpec;
        this.labelColor = "color:#ffffff";
        this.backgroundColor = "background-color:#787878";
    };

    $scope.moveSpecNodeToTop = function(specNode) {
        $scope.lNodes.splice($scope.specNodes.indexOf(specNode), 1);

        $scope.lNodes.unshift(specNode);
    };

    $scope.addSpecNode = function() {
         $scope.specNodes.push(new $scope.specNode("{promoter}{ribozyme}{ribosome_entry_site}{CDS}{terminator}"));
    }

    $scope.removeSpecNode = function(specNode) {
        specNode.remove();
    };

    $scope.deleteDesignSpace = function(targetSpaceID) {
        if (targetSpaceID) {
            var query = "?targetSpaceID=" + encodeURIComponent(targetSpaceID);

            d3.xhr("/designSpace" + query).send("DELETE", function(error, request) {
                if (error) {

                    sweetAlert("Error", error.responseText, "error");

                } else {

                    if ($scope.graphs.length > 1) {
                        $scope.removeGraphSVG(1);
                    }
                    if ($scope.graphs.length > 0 && $scope.graphs[0].spaceID === targetSpaceID) {
                        $scope.removeGraphSVG(0);
                        if ($scope.graphs.length > 1) { 
                            if ($scope.graphs[1].spaceID === targetSpaceID) {
                                $scope.graphs = [];
                            } else {
                                if ($scope.isDSGraph) {
                                    $scope.appendDSGraphSVG($scope.graphs[1].ds, 0, 1110, 300);
                                } else {
                                    $scope.appendVCGraphSVG($scope.graphs[1].vc, 0, 1110, 300);
                                }
                                $scope.graphs[0] = $scope.graphs[1];
                                $scope.graphs = $scope.graphs.slice(0, 1);
                            }
                        } else {
                            $scope.graphs = [];
                        }
                    } else if ($scope.graphs.length > 1 && $scope.graphs[1].spaceID === targetSpaceID) {
                        $scope.graphs = $scope.graphs.slice(0, 1);
                    }

                }
            });
        }
    };

    $scope.encodeQueryParameter = function(parameterName, parameterValue, query) {
        if (query.length > 1) {
            return "&" + parameterName + "=" + encodeURIComponent(parameterValue);
        } else {
            return parameterName + "=" + encodeURIComponent(parameterValue);
        }
    };

}