/*  ============================================================================

  Copyright (C) 2006-2016 Talend Inc. - www.talend.com

  This source code is available under agreement available at
  https://github.com/Talend/data-prep/blob/master/LICENSE

  You should have received a copy of the agreement
  along with this program; if not, write to Talend SA
  9 rue Pages 92150 Suresnes, France

  ============================================================================*/

/**
 * @ngdoc directive
 * @name data-prep.quality-bar.directive:QualityBar
 * @description Quality bar tooltip
 * @restrict E
 * @usage
 <quality-bar
     quality="quality"
     enterAnimation="enableEnterAnimation"
     isTrusted="true"
     hasMenu="true">
 </talend-tooltip>
 * @param {object} quality {empty: number, invalid: number, valid: number} The quality values
 * @param {string} enterAnimation Do not animate on enter if this flag === 'false'
 * @param {string} isTrusted Show empty quality bar if column has no statistics frequency table yet
 * @param {string} hasMenu Do not show the menu if hasMenu === 'false'
 */
export default function QualityBar($timeout) {
    'ngInject';

    return {
        restrict: 'E',
        templateUrl: 'app/components/widgets/quality-bar/quality-bar.html',
        scope: {
            enterAnimation: '@',
            quality: '=',
            isTrusted: '=',
            hasMenu: '='
        },
        transclude: {
            'valid-quality-bar-menu': '?validMenuItems',
            'empty-quality-bar-menu': '?emptyMenuItems',
            'invalid-quality-bar-menu': '?invalidMenuItems'
        },
        bindToController: true,
        controller: 'QualityBarCtrl',
        controllerAs: 'qualityBarCtrl',
        link: function (scope, iElement, iAttrs, ctrl) {
            var initializing = true;

            /**
             * @ngdoc method
             * @name enableTransition
             * @methodOf talend.widget.directive:QualityBar
             * @description [PRIVATE] Enable animation
             */
            var enableTransition = function enableTransition() {
                ctrl.blockTransition = false;
            };

            /**
             * @ngdoc method
             * @name refreshBarsWithAnimation
             * @methodOf talend.widget.directive:QualityBar
             * @description [PRIVATE] Block animation, reset width to 0 and calculate the new width with animation enabling
             */
            var refreshBarsWithAnimation = function refreshBarsWithAnimation() {
                ctrl.width = {
                    invalid: 0,
                    empty: 0,
                    valid: 0
                };

                enableTransition();

                $timeout(function () {
                    ctrl.computePercent();
                    ctrl.computeQualityWidth();
                    scope.$digest();
                }, 300, false);
            };

            /**
             * @ngdoc method
             * @name refreshBars
             * @methodOf talend.widget.directive:QualityBar
             * @description [PRIVATE] Refresh the quality bars infos (percent and width)
             * When enterAnimation === 'false', we do NOT animate on first render
             */
            var refreshBars = function refreshBars() {
                //Do NOT animate on first values and enterAnimation is false
                if (initializing && ctrl.enterAnimation === 'false') {
                    initializing = false;
                    ctrl.blockTransition = true;
                    ctrl.computePercent();
                    ctrl.computeQualityWidth();
                }
                else {
                    refreshBarsWithAnimation();
                }
            };

            scope.$watch(ctrl.hashQuality, refreshBars);
        }
    };
}