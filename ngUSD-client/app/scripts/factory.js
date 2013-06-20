'use strict';

var RestServerPath = "http://localhost:port/ngusd/rest";
var RestServerPort = ":8050";

NgUsdClientApp.config(function ($httpProvider) {
    $httpProvider.defaults.headers.common['Accept'] = 'application/json';
});

NgUsdClientApp.factory('BranchService', function ($resource) {

    var branchService = $resource(RestServerPath + '/branches', {
        port: RestServerPort
    }, {});

    branchService.findAllBranches = function (fn) {
        return branchService.query(fn);
    };

    return branchService;
});

NgUsdClientApp.factory('UseCaseService', function ($resource) {
    var useCaseService = $resource(RestServerPath + '/branches/:branchName/builds/:buildName/usecases/:usecaseName',
        {branchName: '@branchName',
            buildName: '@buildName',
            usecaseName: '@usecaseName',
            port: RestServerPort}, {});

    useCaseService.findAllUseCases = function (branchName, buildName) {
        return useCaseService.query({'branchName': branchName, 'buildName': buildName});
    }
    useCaseService.getUseCase = function (branchName, buildName, usecaseName, fn, error) {
        return useCaseService.get({'branchName': branchName, 'buildName': buildName, 'usecaseName': usecaseName}, fn, function (response) {

        });
    }
    return useCaseService;

});


NgUsdClientApp.factory('PagesService', function ($resource) {
    var useCaseService = $resource(RestServerPath + '/branches/:branchName/builds/:buildName/usecases/:usecaseName/scenarios/:scenarioName/pages/:pageName',
        {branchName: '@branchName',
            buildName: '@buildName',
            usecaseName: '@usecaseName',
            scenarioName: '@scenarioName',
            pageName: '@pageName',
            port: RestServerPort}, {});

    return useCaseService;

});

NgUsdClientApp.factory('BuildStateService', function ($resource) {
    var buildStateService = $resource(RestServerPath + '/configuration/buildstates/:state',
        {state: '@state',
            port: RestServerPort},
        {
            ListBuildStates: { method: "GET", params: {} }
        });

    return buildStateService;

});