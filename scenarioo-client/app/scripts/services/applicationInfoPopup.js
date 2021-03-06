/* scenarioo-client
 * Copyright (C) 2014, scenarioo.org Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('scenarioo.services').factory('ScApplicationInfoPopup', function ($cookieStore, $modal) {

    var PREVIOUSLY_VISITED_COOKIE_NAME = 'scenariooPreviouslyVisited';

    function showApplicationInfoPopupIfRequired() {
        if (userVisitsAppForTheFirstTime() === true) {
            showApplicationInfoPopup();
        }

        function userVisitsAppForTheFirstTime() {
            if ($cookieStore.get(PREVIOUSLY_VISITED_COOKIE_NAME)) {
                return false;
            }
            $cookieStore.put(PREVIOUSLY_VISITED_COOKIE_NAME, true);
            return true;
        }
    }

    function showApplicationInfoPopup(tabValue) {
        $modal.open({
            templateUrl: 'views/applicationInfoPopup.html',
            controller: 'ApplicationInfoCtrl',
            windowClass: 'modal-small',
            backdropFade: true,
            resolve: {
                tabValue: function () { return tabValue; }
            }
        });
    }

    return {
        PREVIOUSLY_VISITED_COOKIE_NAME: PREVIOUSLY_VISITED_COOKIE_NAME,

        showApplicationInfoPopupIfRequired: showApplicationInfoPopupIfRequired,

        showApplicationInfoPopup: showApplicationInfoPopup
    };

}).controller('ApplicationInfoCtrl', function ($scope, $modalInstance, Config, tabValue) {
    $scope.$watch(function () {
        return Config.applicationInformation();
    }, function (applicationInformation) {
        $scope.applicationInformation = applicationInformation;
    });

    $scope.tabValue = tabValue;

    $scope.closeInfoModal = function () {
        $modalInstance.dismiss('cancel');
    };
});
