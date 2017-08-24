(function() {
  var app =  angular.module('berekeningen', ['ngRoute', 'ngSanitize', 'ui.bootstrap']);

  app.config(['$routeProvider', function($routeProvider) {
    $routeProvider.
      when('/dashboard', {
        templateUrl: 'views/dashboard.html',
        controller: 'DashboardController'
      }).
      when('/berekeningen/:id', {
        templateUrl: 'views/berekening.html',
        controller: 'BerekeningController'
      }).
      otherwise({
        redirectTo: '/dashboard'
      })
  }]);

  app.filter('javatype', function() {
    return function(input) {
      if (input) {
        var elems = input.split('.');
        return elems[elems.length - 1];
      }
      else {
        return '';
      }
    };
  });

  app.filter('scalatype', function() {
    return function(input) {
      if (input) {
        var elems = input.split('.');
        var name = elems[elems.length - 1];
        if (name[name.length - 1] == '$') {
          name = name.substr(0, name.length - 1);
        }
        return name;
      }
      else {
        return '';
      }
    };
  });

  app.filter('usages', function() {
    var signIn = '<i class="fa fa-fw fa-sign-in" title="Gebruikt als invoer"></i>';
    var signOut = '<i class="fa fa-fw fa-sign-out" title="Gebruikt als uitvoer"></i>';
    var signInter = '<i class="fa fa-fw fa-ellipsis-h" title="Gebruikt als tussenresultaat"></i>';

    return function(input) {
      return ((input.input) ? signIn : '') + ((input.intermediate) ? signInter : '') + ((input.output) ? signOut : '')
    };
  });
  
  var functionDefinitions = {
    "SubBerekening": {
      "title": "SubBerekening",
      "template": "functies/SubBerekening.html",
    },
    "VervangLooptijden": {
      "title": "VervangLooptijden",
      "template": "functies/VervangLooptijden.html",
    },
    "totaalVan": {
      "title": "totaal van",
      "template": "functies/totaalVan.html"
    },
    "AfgekaptOp100Euro": {
      "title": "AfgekaptOp100Euro",
      "template": "functies/AfgekaptOp100Euro.html"
    },
    "AnnuiteitHoofdsommen": {
      "title": "AnnuiteitHoofdsommen van",
      "template": "functies/AnnuiteitHoofdsommen.html"
    },
    "AnnuiteitJaarBedrag": {
      "title": "AnnuiteitJaarBedrag",
      "template": "functies/AnnuiteitJaarBedrag.html"
    },
    "Kaasschaaf": {
      "title": "Kaasschaaf",
      "template": "functies/Kaasschaaf.html"
    },
    "eerste": {
      "title": "eerste",
      "template": "functies/eerste.html"
    },
    "prikkenInTabel": {
      "title": "prikkenInTabel",
      "template": "functies/prikkenInTabel.html"
    },
    "BepaalInkomenWegingsFactor": {
      "title": "BepaalInkomenWegingsFactor",
      "template": "functies/BepaalInkomenWegingsFactor.html"
    },
    "gecombineerdMaximum": {
      "title": "gecombineerdMaximum",
      "template": "functies/gecombineerdMaximum.html"
    },
    "gecombineerdMinimum": {
      "title": "gecombineerdMinimum",
      "template": "functies/gecombineerdMinimum.html"
    },
  };
  
  app.directive('functiePopup', ['$compile', function($compile) {
    return function($scope, element, attrs) {
      var functionDefinition = functionDefinitions[attrs.functiePopup];

      if (functionDefinition) {
        element.attr('uib-popover-template', "'" + functionDefinition.template + "'");
        element.attr('popover-title', 'Functie: ' + functionDefinition.title);
        element.attr('popover-trigger', 'outsideClick');
        element.removeAttr('functie-popup');
        element.removeAttr('functiePopup');
        element.removeAttr('data-functie-opup');
        $compile(element)($scope);
      }
      else {
        console.warn('Unable to find Function Definition for function: ' + attrs.functiePopup);
      }
    }
  }]);

  app.controller('HeaderController', ['$scope', '$q', '$location', 'BerekeningenService', 'GlossaryService', 'ExecutionService', HeaderController]);
  app.controller('DashboardController', ['$scope', '$q', 'BerekeningenService', 'GlossaryService', 'UsedByService', DashboardController]);
  app.controller('BerekeningController', ['$scope', '$routeParams', '$q', 'BerekeningenService', 'GlossaryService', 'UsedByService', 'ExecutionService', BerekeningController]);

  app.factory('GlossaryService', ['$http', GlossaryService]);
  app.factory('BerekeningenService', ['$http', '$q', '$filter', BerekeningenService]);
  app.factory('UsedByService', ['$q', 'GlossaryService', 'BerekeningenService', UsedByService]);
  app.factory('ExecutionService', ['$http', ExecutionService]);

  function BerekeningController($scope, $routeParams, $q, BerekeningenService, GlossaryService, UsedByService, ExecutionService) {
    var nameComponents = $routeParams.id.split('.');
    $scope.berekeningId = $routeParams.id;
    $scope.berekeningName = nameComponents.pop();
    $scope.berekeningPackage = nameComponents;
    $scope.toggles = {
      showLabels: true,
      showConditions: true,
      showEvaluations: true,
      showGraphAndSource: 'graphOnly',
      showIntermediates: false,
      showExecutionConfig: false,
      hasExecutionHistory: false,
      showLoadingIndicator: false,
      showInputFields: true
    };
    $scope.berekeningInputs = [];
    $scope.berekeningOutputs = [];
    $scope.berekeningIntermediates = [];
    $scope.context = {};
    $scope.originalInput = {};
    $scope.endpoints = ExecutionService.endpoints();
    $scope.selectedEndpoint;

    ExecutionService.loaded().then(function() {
      if (ExecutionService.endpoints().length > 0) {
        $scope.selectedEndpoint = ExecutionService.endpoints()[0];
      }
    });

    var getBerekeningPromise = BerekeningenService.getBerekening($scope.berekeningId).then(function(berekening) {
      $scope.berekening = berekening;
      $scope.selectedNode = undefined;
      $scope.hoveredNode = undefined;

      initBerekeningGraph(document.getElementById('berekeningenGraafChart'), berekening, GlossaryService.dictionary(), $scope, 'selectedNode', 'hoveredNode');
    }, function(err) {
      console.error('No berekening found', err);
    });

    $q.all([getBerekeningPromise, GlossaryService.loaded()]).then(function() {
      $scope.berekening.inputs.forEach(function(i) { $scope.berekeningInputs.push(GlossaryService.dictionary()[$scope.berekening.nodes[i].name]); });
      $scope.berekening.outputs.forEach(function(i) { $scope.berekeningOutputs.push(GlossaryService.dictionary()[$scope.berekening.nodes[i].name]); });
      $scope.berekening.intermediates.forEach(function(i) { $scope.berekeningIntermediates.push(GlossaryService.dictionary()[$scope.berekening.nodes[i].name]); });
    });

    $scope.activeFact = undefined;

    $scope.selectFact = function(f) {
      $scope.activeFact = f;
    };

    $scope.calculate = {
      hasNoValuesInContext: !angular.equals($scope.context, {}),
      hasIntermediateValues: false,
      hasOutputValues: false,
      lastResultSuccess: false,
      lastResultErrors: {},
      execute: function() {
        $scope.toggles.hasExecutionHistory = true;
        $scope.toggles.showLoadingIndicator = true;
        sanitizeContext($scope.context);
        copyContextValuesFromSource($scope.originalInput, $scope.context);
        ExecutionService.execute($scope.selectedEndpoint, $scope.context).then(function(response) {
          $scope.toggles.showLoadingIndicator = false;
          $scope.calculate.lastResultSuccess = response.status === 200;
          $scope.calculate.lastResultErrors = [];
          if (response.data) {
            if ( response.status === 200 ) {
              copyContextValuesFromSource($scope.context, response.data.facts);
            }
            else {
              response.data.obj.forEach(function(errorMessage) {
                $scope.calculate.lastResultErrors.push(errorMessage.msg);
              });
            }
          }
        }).catch(function(err) {
          $scope.toggles.showLoadingIndicator = false;
          $scope.calculate.lastResultSuccess = false;
          $scope.calculate.lastResultErrors = [
            'Algemene fout bij uitvoeren servicer: ' + err
          ];
        });
      },
      resetOutputs: function() {
        $scope.berekening.outputs.forEach(function(i) {
          var factName = $scope.berekening.nodes[i].name;
          if ($scope.context.hasOwnProperty(factName)) {
            delete $scope.context[factName];
          }
        });
      },
      resetIntermediates: function() {
        $scope.berekening.intermediates.forEach(function(i) {
          var factName = $scope.berekening.nodes[i].name;
          if ($scope.context.hasOwnProperty(factName)) {
            delete $scope.context[factName];
          }
        });
      },
      resetAll: function() {
        copyContextValuesFromSource($scope.context, {});
        copyContextValuesFromSource($scope.originalInput, {});
      }
    };

    $scope.$watch(function() {
      var oldContext = {}, contextChanged = false, changeCounter = 0;
      return function() {
        contextChanged = !angular.equals($scope.context, oldContext);
        copyContextValuesFromSource(oldContext, $scope.context);
        console.log('Checking context', contextChanged, changeCounter);
        return contextChanged ? ++changeCounter : changeCounter;
      };
    }(), function() {
      $scope.calculate.hasNoValuesInContext = angular.equals($scope.context, {});
      $scope.calculate.hasIntermediateValues = false;
      $scope.calculate.hasOutputValues = false;

      $scope.intermediates.forEach(function(i) {
        if ($scope.context.hasOwnProperty($scope.berekening.nodes[i].name)) {
          $scope.calculate.hasIntermediateValues = true;
        }
      });

      $scope.outputs.forEach(function(i) {
        if ($scope.context.hasOwnProperty($scope.berekening.nodes[i].name)) {
          $scope.calculate.hasOutputValues = true;
        }
      });
    });

    function copyContextValuesFromSource(target, template) {
      for ( var p in target ) {
        if (target.hasOwnProperty(p) && !template.hasOwnProperty(p)) {
          delete target[p];
        }
      }
      for ( var p in template ) {
        if (template.hasOwnProperty(p)) {
          target[p] = template[p];
        }
      }
    }

    function sanitizeContext(target) {
      for ( var p in target ) {
        if (target.hasOwnProperty(p) && target[p] === '') {
          delete target[p];
        }
      }
    }

    $scope.$watch(function() {
      return '' + $scope.selectedNode + ':' + $scope.hoveredNode;
    }, function() {
      var factToFind = undefined;

      if ($scope.selectedNode) {
        factToFind = $scope.selectedNode;
      }
      else if ($scope.hoveredNode) {
        factToFind = $scope.hoveredNode;
      }

      if (factToFind) {
        $scope.activeFact = GlossaryService.dictionary()[factToFind];
      }
    });
  }

  function DashboardController($scope, $q, BerekeningenService, GlossaryService, UsedByService) {
    $scope.jumobtronHidden = false;
    $scope.hideJumbotron = function() { $scope.jumobtronHidden = true; };

    $scope.facts = GlossaryService.items();
    $scope.activeFact = undefined;

    $scope.selectFact = function(f) {
      $scope.activeFact = f;
    };

    $scope.factSearch = {name:''};
    $scope.resetSearch = function() {
      $scope.factSearch.name = '';
    };
  }

  var NOT_SELECTED_A_BEREKENING = '--not-selected--';
  function HeaderController($scope, $q, $location, BerekeningenService, GlossaryService, ExecutionService) {
    $scope.stillLoading = true;
    $scope.berekeningen = BerekeningenService.berekeningen();
    $scope.selectedBerekening = NOT_SELECTED_A_BEREKENING;

    $scope.jumpToBerekening = function() {
      if ($scope.selectedBerekening !== NOT_SELECTED_A_BEREKENING) {
        $location.url('/berekeningen/' + $scope.selectedBerekening);
        $scope.selectedBerekening = NOT_SELECTED_A_BEREKENING;
      }
    };

    $scope.goHome = function() {
      $location.url('/');
    };

    $q.all([BerekeningenService.loaded(), GlossaryService.loaded(), ExecutionService.loaded()]).then(function() {
      $scope.stillLoading = false;
    });
  }

  function GlossaryService($http) {
    var allFacts = [{ name: 'Loading...' }];
    var squashedGlossaries = {};
    var glossaries = {};

    var loadingPromise = reloadGlossary();

    var svc = {
      items: function() {
        return allFacts;
      },
      dictionary: function() {
        return squashedGlossaries;
      },
      loaded: function() {
        return loadingPromise;
      },
      glossaries: function() {
        return glossaries;
      }
    };

    return svc;

    function reloadGlossary() {
      return $http({
        method: 'GET',
        url: 'api/glossaries'
      }).then(function(response) {
        receiveGlossary(response.data);
      }, function(errorResponse) {
        console.error('Error retrieving glossary');
      });
    }

    function receiveGlossary(fs) {
      var newFacts = [];
      squashedGlossaries = {};
      glossaries = {};

      for (var g in fs ) {
        if ( fs.hasOwnProperty(g) ) {
          var glossary = fs[g];

          glossaries[g] = glossary;

          for ( var p in glossary ) {
            if ( glossary.hasOwnProperty(p) ) {
              var fact = glossary[p];
              fact.usedIn = {};
              fact.containedIn = g;
              fact.id = 'fact-' + g + '-' + p;
              fact.glossary = g;
              newFacts.push(fact);
              squashedGlossaries[fact.name] = fact;
            }
          }
        }
      }

      // Replace the contents of the glossary array, so people can bind to it
      allFacts.splice.apply(allFacts, [0, allFacts.length].concat(newFacts));
    }
  }

  function BerekeningenService($http, $q, $filter) {
    var allBerekeningen = [{ name: 'Loading...' }];
    var berekeningen = {};
    var loadingPromise = reloadBerekeningen();

    var svc = {
      berekeningen: function() {
        return allBerekeningen;
      },
      getBerekening: function(name) {
        var deferred = $q.defer();

        loadingPromise.then(function() {
          if (berekeningen[name]) {
            deferred.resolve(berekeningen[name]);
          }
          else {
            deferred.reject();
          }
        });

        return deferred.promise;
      },
      loaded: function() {
        return loadingPromise
      }
    };

    return svc;

    function reloadBerekeningen() {
      return $http({
        method: 'GET',
        url: 'api/derivations'
      }).then(function(response) {
        receiveBerekeningen(response.data);
      }, function(errorResponse) {
        console.error('Error retrieving berekeningen');
      });
    }

    function receiveBerekeningen(bs) {
      var newBerekeningen = [];
      berekeningen = {};

      for ( var p in bs ) {
        if ( bs.hasOwnProperty(p) ) {
          var berekening = bs[p];
          berekening.name = p;
          berekening.displayName = $filter('scalatype')(p);

          newBerekeningen.push(bs[p]);
          berekeningen[p] = bs[p];
        }
      }

      // Replace the contents of the allBerekeningen array, so people can bind to it
      allBerekeningen.splice.apply(allBerekeningen, [0, allBerekeningen.length].concat(newBerekeningen));
    }
  }

  function UsedByService($q, GlossaryService, BerekeningenService) {
    $q.all([GlossaryService.loaded(), BerekeningenService.loaded()]).then(function() {
      var alleBerekeningen = BerekeningenService.berekeningen();
      var glossary = GlossaryService.dictionary();

      var updaterFunction = function(inputNodeIndex) {
        var inputFact = this.berekening.nodes[inputNodeIndex].name;
        if (!glossary[inputFact].usedIn.hasOwnProperty(this.berekening.name)) {
          glossary[inputFact].usedIn[this.berekening.name] = {
            berekeningName: this.berekening.name,
            input: false,
            output: false,
            intermediate: false
          };
        }

        glossary[inputFact].usedIn[this.berekening.name][this.usageType] = true;
      };

      alleBerekeningen.forEach(function(b) {
        b.inputs.forEach(updaterFunction, { berekening: b, usageType: 'input' });
        b.outputs.forEach(updaterFunction, { berekening: b, usageType: 'output' });
        b.intermediates.forEach(updaterFunction, { berekening: b, usageType: 'intermediate' });
      });

    });

    return {};
  }

  function ExecutionService($http) {
    var availableEndpoints = [];
    var loadingPromise = reloadEndpoints();

    return {
      endpoints: function() {
        return availableEndpoints;
      },
      loaded: function() {
        return loadingPromise
      },
      execute: function(endpoint, context) {
        return callService(endpoint, context);
      }
    };

    function reloadEndpoints() {
      return $http({
        method: 'GET',
        url: 'api/meta/config'
      }).then(function(response) {
        availableEndpoints = response.data.endpoints;
      }, function(err) {
        console.error('Error retrieving configuration', err);
      });
    }

    function callService(url, context) {
      return $http({
        method: 'POST',
        url: url,
        data: context
      }).then(function(response) {
        return response;
      }, function(err) {
        console.error('Error calling into service at url', url, 'Error message:', err);
        return err;
      });
    }

  }

})();
