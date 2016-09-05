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

  app.controller('HeaderController', ['$scope', '$q', '$location', 'BerekeningenService', 'GlossaryService', HeaderController]);
  app.controller('DashboardController', ['$scope', '$q', 'BerekeningenService', 'GlossaryService', 'UsedByService', DashboardController]);
  app.controller('BerekeningController', ['$scope', '$routeParams', '$q', 'BerekeningenService', 'GlossaryService', 'UsedByService', BerekeningController]);

  app.factory('GlossaryService', ['$http', GlossaryService]);
  app.factory('BerekeningenService', ['$http', '$q', BerekeningenService]);
  app.factory('UsedByService', ['$q', 'GlossaryService', 'BerekeningenService', UsedByService]);

  function BerekeningController($scope, $routeParams, $q, BerekeningenService, GlossaryService, UsedByService) {
    var nameComponents = $routeParams.id.split('.');
    $scope.berekeningId = $routeParams.id;
    $scope.berekeningName = nameComponents.pop();
    $scope.berekeningPackage = nameComponents;
    $scope.showLabels = true;
    $scope.showConditions = true;
    $scope.showEvaluations = true;
    $scope.showGraphAndSource = 'graphOnly';
    $scope.berekeningInputs = [];
    $scope.berekeningOutputs = [];

    var getBerekeningPromise = BerekeningenService.getBerekening($scope.berekeningId).then(function(berekening) {
      $scope.berekening = berekening;
      $scope.selectedNode = undefined;
      $scope.hoveredNode = undefined;

      initBerekeningGraph(document.getElementById('berekeningenGraafChart'), berekening, GlossaryService.dictionary(), $scope, 'selectedNode', 'hoveredNode');
    }, function() {
      console.error('No berekening found');
    });

    $q.all([getBerekeningPromise, GlossaryService.loaded()]).then(function() {
      $scope.berekening.inputs.forEach(function(i) { $scope.berekeningInputs.push(GlossaryService.dictionary()[$scope.berekening.nodes[i].name]); });
      $scope.berekening.outputs.forEach(function(i) { $scope.berekeningOutputs.push(GlossaryService.dictionary()[$scope.berekening.nodes[i].name]); });
    });

    $scope.activeFact = undefined;

    $scope.selectFact = function(f) {
      $scope.activeFact = f;
    };

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

    $scope.factSearch = '';
    $scope.resetSearch = function() {
      $scope.factSearch = '';
    };
  }

  var NOT_SELECTED_A_BEREKENING = '--not-selected--';
  function HeaderController($scope, $q, $location, BerekeningenService, GlossaryService) {
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

    $q.all([BerekeningenService.loaded(), GlossaryService.loaded()]).then(function() {
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

  function BerekeningenService($http, $q) {
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

})();
