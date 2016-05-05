function ngFileSelect() {
    return {
        controller: 'uploadCtrl',
        link: function($scope, element) {
            element.bind("change", function(e) {
                $scope.file = (e.srcElement || e.target).files[0];
                $scope.getFile();
            })
        }
    };
}