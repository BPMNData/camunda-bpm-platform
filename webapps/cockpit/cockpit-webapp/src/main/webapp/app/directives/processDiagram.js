'use strict';

ngDefine('cockpit.directives', [ 
                                 'angular',
                                 'jquery',
                                 'bpmn/Bpmn',
                                 'bootstrap-slider',
                                 'jquery-overscroll',
                                 'jquery-mousewheel',
                                 'dojo/domReady!'
                                 ], function(module, angular, $, Bpmn) {
  
  function DirectiveController($scope, $element, $attrs, $filter, ProcessDiagramService) {

    var activityStatistics = null;
    var bpmnRenderer = null;
    $scope.zoomLevel = null;
    
    $scope.$watch('processDefinitionId', function () {
      loadProcessDiagram();
    });
    
    $scope.$watch(function() { return bpmnRenderer; }, function(newValue) {
      annotate();
    });
    
    $scope.$on('$destroy', function() {
      bpmnRenderer = null;
    });
    
    $scope.$watch('zoomLevel', function(newZoomLevel) {
      if (!!newZoomLevel && !!bpmnRenderer) {
        removeOverscroll();
        bpmnRenderer.zoom(newZoomLevel);
        overscroll();
      }
    });
    
    function loadProcessDiagram() {
      // set id of element
      var elementId = 'processDiagram_' + $scope.processDefinitionId.replace(/:/g, '_');
      $element.attr('id', elementId);
      
      // clear innerHTML of element
      $element.empty();
      
      // get the bpmn20xml
      ProcessDiagramService.getBpmn20Xml($scope.processDefinitionId)
      .then(
          function(data) {
            if (!!$scope.miniature && $scope.miniature === true) {
              renderMiniatureProcessDiagram(data.bpmn20Xml);
            } else {
              renderProcessDiagram(data.bpmn20Xml);
            }
          }
      );
    }
    
    function renderProcessDiagram (bpmn20Xml) {
      
      $element.addClass('process-diagram');
      
      bpmnRenderer = new Bpmn();
      bpmnRenderer.render(bpmn20Xml, {
        diagramElement : $element.attr('id')
      });
      
      $scope.zoomLevel = 1;
      
      $element.mousewheel(function(event, delta) {
        $scope.$apply(function() {
          $scope.zoomLevel = calculateZoomLevel(delta);
        });
      });
    }
    
    function renderMiniatureProcessDiagram (bpmn20Xml) {
      bpmnRenderer = new Bpmn();
      bpmnRenderer.render(bpmn20Xml, {
        diagramElement : $element.attr('id'),
        width : parseInt($element.parent().css("min-width")),
        height : $element.parent().height(),
      });
    }
    
    function overscroll() {
      $element.overscroll({captureWheel:false});
    }
    
    function removeOverscroll() {
      $element.removeOverscroll();
    }
    
    function calculateZoomLevel (delta) {
      var minZoomLevelMin = 0.1;
      var maxZoomLevelMax = 5;
      var zoomSteps = 10;

      var newZoomLevel = $scope.zoomLevel + Math.round((delta * 100)/ zoomSteps) / 100;

      if (newZoomLevel > maxZoomLevelMax) {
        newZoomLevel = maxZoomLevelMax;
      } else if (newZoomLevel < minZoomLevelMin) {
        newZoomLevel = minZoomLevelMin;
      }

      return newZoomLevel;
    };

    function annotate() {
      if (bpmnRenderer) {
        
        if (activityStatistics) {
          doAnnotateWithActivityStatistics(activityStatistics);
        }
      }
    }
    
    function doAnnotateWithActivityStatistics(activityStaticstics) {
      var shortenNumberFilter = $filter('shortenNumber');
      
      angular.forEach(activityStatistics, function (currentActivityStatistics) {
        var instances = shortenNumberFilter(currentActivityStatistics.instances);
        bpmnRenderer.annotate(currentActivityStatistics.id, '<p class="currentActivityCount">' + instances + '</p>');
      });
    }
    
    this.getRenderer = function () {
      return bpmnRenderer;
    };
    
    this.annotateWithActivityStatistics = function (statistics) {
      activityStatistics = statistics;
      annotate();
    };
    
  }
  
  var Directive = function (ProcessDiagramService) {
    return {
      restrict: 'EAC',
      scope: {
        processDefinitionId: '=',
        miniature: '=',
      },
      controller: DirectiveController 
    };
  };

  Directive.$inject = [ 'ProcessDiagramService' ];
  
  module
    .directive('processDiagram', Directive);
  
});
