/*  ============================================================================

  Copyright (C) 2006-2016 Talend Inc. - www.talend.com

  This source code is available under agreement available at
  https://github.com/Talend/data-prep/blob/master/LICENSE

  You should have received a copy of the agreement
  along with this program; if not, write to Talend SA
  9 rue Pages 92150 Suresnes, France

  ============================================================================*/

describe('Datagrid header controller', function () {
    'use strict';

    var createController, scope;
    var column = {
        id: '0001',
        name: 'Original name',
        type: 'string'
    };

    var transformationsMock = function () {
        var transformations = [
            {
                'name': 'uppercase',
                'category': 'case',
                'actionScope': [],
                'items': null,
                'parameters': null
            },
            {
                'name': 'rename',
                'category': 'column_metadata',
                'actionScope': ['column_metadata'],
                'items': null,
                'parameters': null
            },
            {
                'name': 'lowercase',
                'category': 'case',
                'actionScope': [],
                'items': null,
                'parameters': null
            },
            {
                'name': 'withParam',
                'category': 'case',
                'actionScope': [],
                'items': null,
                'parameters': [
                    {
                        'name': 'param',
                        'type': 'string',
                        'default': '.',
                        'inputType': 'text'
                    }
                ]
            },
            {
                'name': 'split',
                'category': 'column_metadata',
                'actionScope': ['column_metadata'],
                'parameters': null,
                'items': [
                    {
                        'name': 'mode',
                        'values': [
                            {
                                'name': 'noparam'
                            },
                            {
                                'name': 'regex',
                                'parameters': [
                                    {
                                        'name': 'regexp',
                                        'type': 'string',
                                        'default': '.',
                                        'inputType': 'text'
                                    }
                                ]
                            },
                            {
                                'name': 'index',
                                'parameters': [
                                    {
                                        'name': 'index',
                                        'type': 'integer',
                                        'default': '5',
                                        'inputType': 'number'
                                    }
                                ]
                            },
                            {
                                'name': 'threeParams',
                                'parameters': [
                                    {
                                        'name': 'index',
                                        'type': 'numeric',
                                        'default': '5',
                                        'inputType': 'number'
                                    },
                                    {
                                        'name': 'index2',
                                        'type': 'float',
                                        'default': '5',
                                        'inputType': 'number'
                                    },
                                    {
                                        'name': 'index3',
                                        'type': 'double',
                                        'default': '5',
                                        'inputType': 'number'
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
        ];
        return {
            allTransformations : transformations
        };
    };

    beforeEach(angular.mock.module('data-prep.datagrid-header'));

    beforeEach(inject(function ($rootScope, $controller) {
        scope = $rootScope.$new();

        createController = function () {
            let ctrlFn = $controller('DatagridHeaderCtrl', {
                $scope: scope
            }, true);

            ctrlFn.instance.column = column;
            const ctrl = ctrlFn();
            scope.$digest();
            return ctrl;
        };
    }));

    describe('with transformation list success', function () {
        beforeEach(inject(function ($q, TransformationCacheService) {
            spyOn(TransformationCacheService, 'getColumnTransformations').and.returnValue($q.when(transformationsMock()));
        }));

        it('should filter and init only "column_metadata" category', inject(function ($rootScope, TransformationCacheService) {
            //given
            var ctrl = createController();

            //when
            ctrl.initTransformations();
            $rootScope.$digest();

            //then
            expect(TransformationCacheService.getColumnTransformations).toHaveBeenCalledWith(column, true);
            expect(ctrl.transformations.length).toBe(2);
            expect(ctrl.transformations[0].name).toBe('rename');
            expect(ctrl.transformations[1].name).toBe('split');
        }));

        it('should not get transformations if transformations are already initiated', inject(function ($rootScope, TransformationCacheService) {
            //given
            let ctrl = createController();
            ctrl.initTransformations();
            $rootScope.$digest();

            //when
            ctrl.initTransformations();
            $rootScope.$digest();

            //then
            expect(TransformationCacheService.getColumnTransformations.calls.count()).toBe(1);
        }));

        it('should retrieve transformation list when a column changes', inject(function ($rootScope, TransformationCacheService) {
            //given
            let ctrl = createController();
            ctrl.initTransformations();
            $rootScope.$digest();

            //when
            ctrl.column = {
                id: '0001',
                name: 'New name',
                type: 'string'
            };
            $rootScope.$digest();

            ctrl.initTransformations();

            //then
            expect(TransformationCacheService.getColumnTransformations.calls.count()).toBe(2);
        }));
    });

    describe('with transformation list error', function () {
        beforeEach(inject(function ($q, TransformationCacheService) {
            spyOn(TransformationCacheService, 'getColumnTransformations').and.returnValue($q.reject('server error'));
        }));

        it('should change inProgress and error flags', inject(function ($rootScope) {
            //given
            var ctrl = createController();
            expect(ctrl.transformationsRetrieveError).toBeFalsy();
            expect(ctrl.initTransformationsInProgress).toBeFalsy();

            ctrl.transformationsRetrieveError = true;

            //when
            ctrl.initTransformations();
            expect(ctrl.initTransformationsInProgress).toBeTruthy();
            expect(ctrl.transformationsRetrieveError).toBeFalsy();
            $rootScope.$digest();

            //then
            expect(ctrl.transformationsRetrieveError).toBeTruthy();
            expect(ctrl.initTransformationsInProgress).toBeFalsy();
        }));
    });

    describe('update column name', function () {

        beforeEach(inject(function ($q, PlaygroundService) {
            spyOn(PlaygroundService, 'appendStep').and.returnValue($q.when(true));
        }));

        it('should update column name', inject(function (PlaygroundService) {
            //given
            var ctrl = createController();
            ctrl.newName = 'new name';

            //when
            ctrl.updateColumnName();

            //then
            expect(PlaygroundService.appendStep).toHaveBeenCalledWith('rename_column', {
                new_column_name: 'new name',
                scope: 'column',
                column_id: '0001',
                column_name: 'Original name'
            });
        }));

        it('should turn off edition mode after name update', function () {
            //given
            var ctrl = createController();
            ctrl.newName = 'new name';
            ctrl.isEditMode = true;

            //when
            ctrl.updateColumnName();
            scope.$apply();

            //then
            expect(ctrl.isEditMode).toBe(false);
        });

        it('should return true when name has changed and is not empty', function () {
            //given
            var ctrl = createController();
            ctrl.newName = 'new name';

            //when
            var hasChanged = ctrl.nameHasChanged();

            //then
            expect(hasChanged).toBe(true);
        });

        it('should return false when name is unchanged', function () {
            //given
            var ctrl = createController();
            ctrl.setEditMode(true);
            ctrl.newName = 'Original name';

            //when
            var hasChanged = ctrl.nameHasChanged();

            //then
            expect(hasChanged).toBeFalsy();
        });

        it('should return false when name is falsy', function () {
            //given
            var ctrl = createController();
            ctrl.setEditMode(true);
            ctrl.newName = '';

            //when
            var hasChanged = ctrl.nameHasChanged();

            //then
            expect(hasChanged).toBeFalsy();
        });

        it('should update edition mode to true', function () {
            //given
            var ctrl = createController();
            expect(ctrl.isEditMode).toBeFalsy();

            //when
            ctrl.setEditMode(true);

            //then
            expect(ctrl.isEditMode).toBeTruthy();
        });

        it('should update edition mode to false', function () {
            //given
            var ctrl = createController();
            ctrl.isEditMode = true;

            //when
            ctrl.setEditMode(false);

            //then
            expect(ctrl.isEditMode).toBe(false);
        });

        it('should reset name when edition mode is set to true', function () {
            //given
            var ctrl = createController();
            ctrl.newName = 'new name';

            //when
            ctrl.setEditMode(true);

            //then
            expect(ctrl.newName).toBe('Original name');
        });
    });

});
