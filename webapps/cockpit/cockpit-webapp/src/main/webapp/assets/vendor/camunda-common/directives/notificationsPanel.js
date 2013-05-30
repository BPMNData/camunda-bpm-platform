
ngDefine('camunda.common.directives', [ 'jquery' ], function(module, $) {

  var notificationsTemplate =
'<div class="notifications">' +
'  <div ng-repeat="notification in notifications" class="alert" ng-class="notificationClass(notification)">' +
'    <button type="button" class="close" ng-click="removeNotification(notification)">&times;</button>' +
'    <strong>{{ notification.status }}:</strong> <span ng-bind-html="notification.message"></span>' +
'  </div>' +
'</div>';

 var Directive =  function(Notifications, $filter) {
    return {
      scope: {
        filter: "=notificationsFilter"
      },
      template: notificationsTemplate,
      link: function(scope, element, attrs, $destroy) {

        var filter = scope.filter;

        function matchesFilter(notification) {
          if (!filter) {
            return true;
          }

          return !!$filter('filter')([ notification ], filter).length;
        };

        var notifications = scope.notifications = [];

        var consumer = {
          add: function(notification) {
            if (matchesFilter(notification)) {
              notifications.push(notification);
              return true;
            } else {
              return false;
            }
          },
          remove: function(notification) {
            var idx = notifications.indexOf(notification);
            if (idx != -1) {
              notifications.splice(idx, 1);
            }
          }
        };

        Notifications.registerConsumer(consumer);

        scope.removeNotification = function(notification) {
          notifications.splice(notifications.indexOf(notification), 1);
        };

        scope.notificationClass = function(notification) {
          var classes = [ "error", "success", "warning", "information" ];

          var type = "information";

          if (classes.indexOf(notification.type) != -1) {
            type = notification.type;
          }

          return "alert-" + type;
        };

        scope.$on($destroy, function() {
          Notifications.unregisterConsumer(consumer);
        });
      }
    }
  };

  module
    .directive("notificationsPanel", Directive);

    return module;

});
