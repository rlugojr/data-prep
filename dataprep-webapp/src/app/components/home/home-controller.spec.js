/*  ============================================================================

  Copyright (C) 2006-2016 Talend Inc. - www.talend.com

  This source code is available under agreement available at
  https://github.com/Talend/data-prep/blob/master/LICENSE

  You should have received a copy of the agreement
  along with this program; if not, write to Talend SA
  9 rue Pages 92150 Suresnes, France

  ============================================================================*/

describe('Home controller', function () {
    'use strict';

    var ctrl, createController, scope, StateMock;
    var DATA_INVENTORY_PANEL_KEY = 'org.talend.dataprep.data_inventory_panel_display';

    beforeEach(angular.mock.module('data-prep.home', function ($provide) {
        StateMock = {
            dataset: {
                uploadingDatasets:[]
            }
        };
        $provide.constant('state', StateMock);
    }));

    beforeEach(inject(function ($rootScope, $controller) {
        scope = $rootScope.$new();

        createController = function () {
            return $controller('HomeCtrl', {
                $scope: scope
            });
        };
    }));

    afterEach(inject(function($window) {
        $window.localStorage.removeItem(DATA_INVENTORY_PANEL_KEY);
    }));

    it('should init upload list to empty array', function () {
        //when
        ctrl = createController();

        //then
        expect(ctrl.uploadingDatasets).toEqual([]);
    });

    it('should init right panel state with value from local storage', inject(function ($window) {
        //given
       $window.localStorage.setItem(DATA_INVENTORY_PANEL_KEY, 'true');

        //when
        ctrl = createController();

        //then
        expect(ctrl.showRightPanel).toBe(true);
    }));

    describe('with created controller', function () {
        var uploadDefer;

        beforeEach(inject(function (StateService, $q) {

            ctrl = createController();

            uploadDefer = $q.defer();
            uploadDefer.promise.progress = function (callback) {
                uploadDefer.progressCb = callback;
                return uploadDefer.promise;
            };
        }));

        describe('right panel management', function() {
            it('should toggle right panel flag', inject(function () {
                //given
                expect(ctrl.showRightPanel).toBe(false);

                //when
                ctrl.toggleRightPanel();

                //then
                expect(ctrl.showRightPanel).toBe(true);

                //when
                ctrl.toggleRightPanel();

                //then
                expect(ctrl.showRightPanel).toBe(false);
            }));

            it('should save toggled state in local storage', inject(function ($window) {
                //given
                expect(JSON.parse($window.localStorage.getItem(DATA_INVENTORY_PANEL_KEY))).toBeFalsy();

                //when
                ctrl.toggleRightPanel();

                //then
                expect(JSON.parse($window.localStorage.getItem(DATA_INVENTORY_PANEL_KEY))).toBeTruthy();
                //when
                ctrl.toggleRightPanel();

                //then
                expect(JSON.parse($window.localStorage.getItem(DATA_INVENTORY_PANEL_KEY))).toBeFalsy();
            }));

            it('should update right panel icon', inject(function () {
                //given
                expect(ctrl.showRightPanelIcon).toBe('u');

                //when
                ctrl.toggleRightPanel();

                //then
                expect(ctrl.showRightPanelIcon).toBe('t');

                //when
                ctrl.toggleRightPanel();

                //then
                expect(ctrl.showRightPanelIcon).toBe('u');
            }));
        });
    });
});
