/*  ============================================================================

  Copyright (C) 2006-2016 Talend Inc. - www.talend.com

  This source code is available under agreement available at
  https://github.com/Talend/data-prep/blob/master/LICENSE

  You should have received a copy of the agreement
  along with this program; if not, write to Talend SA
  9 rue Pages 92150 Suresnes, France

  ============================================================================*/

describe('Preparation list controller', function () {
    'use strict';

    var createController, scope;

    beforeEach(angular.mock.module('data-prep.preparation-list'));

    beforeEach(inject(function ($state, $q, $rootScope, $controller, PreparationService, MessageService, StateService) {
        scope = $rootScope.$new();

        createController = function () {
            return $controller('PreparationListCtrl', {
                $scope: scope
            });
        };

        spyOn($rootScope, '$emit').and.returnValue();

        spyOn(PreparationService, 'clone').and.returnValue($q.when(true));
        spyOn(PreparationService, 'delete').and.returnValue($q.when(true));
        spyOn(PreparationService, 'setName').and.returnValue($q.when(true));
        spyOn(StateService, 'setPreviousRoute').and.returnValue();
        spyOn(MessageService, 'success').and.returnValue(null);
        spyOn(MessageService, 'error').and.returnValue(null);
        spyOn($state, 'go').and.returnValue();
    }));

    afterEach(inject(function ($stateParams) {
        $stateParams.prepid = null;
    }));

    describe('load preparation', () => {
        it('should set preparation list route as back page', inject(function ($state, StateService) {
            //given
            var ctrl = createController();
            var preparation = {
                id: 'de618c62ef97b3a95b5c171bc077ffe22e1d6f79',
                dataSetId: 'dacd45cf-5bd0-4768-a9b7-f6c199581efc',
                author: 'anonymousUser',
                creationDate: 1427460984585,
                steps: [
                    '228c16230de53de5992eb44c7aba362ac714ab1c'
                ],
                actions: []
            };
            //when
            ctrl.load(preparation);
            scope.$digest();

            //then
            expect(StateService.setPreviousRoute).toHaveBeenCalledWith('nav.index.preparations');
        }));

        it('should redirect to preparation playground', inject(function ($state) {
            //given
            var ctrl = createController();
            var preparation = {
                id: 'de618c62ef97b3a95b5c171bc077ffe22e1d6f79',
                dataSetId: 'dacd45cf-5bd0-4768-a9b7-f6c199581efc',
                author: 'anonymousUser',
                creationDate: 1427460984585,
                steps: [
                    '228c16230de53de5992eb44c7aba362ac714ab1c'
                ],
                actions: []
            };
            //when
            ctrl.load(preparation);
            scope.$digest();

            //then
            expect($state.go).toHaveBeenCalledWith('playground.preparation', {prepid: preparation.id});
        }));
    });

    describe('remove', () => {
        it('should remove preparation', inject(function ($q, TalendConfirmService, PreparationService) {
            //given
            spyOn(TalendConfirmService, 'confirm').and.returnValue($q.when(true));

            var ctrl = createController();
            var preparation = {
                id: 'de618c62ef97b3a95b5c171bc077ffe22e1d6f79',
                name: 'my preparation'
            };

            //when
            ctrl.remove(preparation);
            scope.$digest();

            //then
            expect(TalendConfirmService.confirm).toHaveBeenCalledWith({disableEnter: true}, ['DELETE_PERMANENTLY', 'NO_UNDONE_CONFIRM'], {
                type: 'preparation',
                name: preparation.name
            });
            expect(PreparationService.delete).toHaveBeenCalledWith(preparation);
        }));

        it('should show success message on confirm', inject(function ($q, TalendConfirmService, MessageService) {
            //given
            spyOn(TalendConfirmService, 'confirm').and.returnValue($q.when(true));

            var ctrl = createController();
            var preparation = {
                id: 'de618c62ef97b3a95b5c171bc077ffe22e1d6f79',
                name: 'my preparation'
            };

            //when
            ctrl.remove(preparation);
            scope.$digest();

            //then
            expect(MessageService.success).toHaveBeenCalledWith('REMOVE_SUCCESS_TITLE', 'REMOVE_SUCCESS', {
                type: 'preparation',
                name: preparation.name
            });
        }));

        it('should do nothing on delete dismiss', inject(function ($q, TalendConfirmService, PreparationService, MessageService) {
            //given
            spyOn(TalendConfirmService, 'confirm').and.returnValue($q.reject(null));

            var ctrl = createController();
            var preparation = {
                id: 'de618c62ef97b3a95b5c171bc077ffe22e1d6f79',
                name: 'my preparation'
            };

            //when
            ctrl.remove(preparation);
            scope.$digest();

            //then
            expect(TalendConfirmService.confirm).toHaveBeenCalledWith({disableEnter: true}, ['DELETE_PERMANENTLY', 'NO_UNDONE_CONFIRM'], {
                type: 'preparation',
                name: preparation.name
            });
            expect(PreparationService.delete).not.toHaveBeenCalled();
            expect(MessageService.success).not.toHaveBeenCalled();
        }));
    });

    describe('rename', function () {

        it('should call preparation service to rename the preparation', inject(function (PreparationService) {
            //given
            var ctrl = createController();
            var preparation = {id: 'foo_beer', name: 'my old name'};
            var name = 'new preparation name';

            //when
            ctrl.rename(preparation, name);
            scope.$digest();

            //then
            expect(PreparationService.setName).toHaveBeenCalledWith(preparation.id, name);
        }));

        it('should show success message on success', inject(function (MessageService) {
            //given
            var ctrl = createController();
            var preparation = {id: 'foo_beer', name: 'my old name'};
            var name = 'new preparation name';

            //when
            ctrl.rename(preparation, name);
            scope.$digest();

            //then
            expect(MessageService.success).toHaveBeenCalledWith('PREPARATION_RENAME_SUCCESS_TITLE', 'PREPARATION_RENAME_SUCCESS');
        }));

        it('should manage loader screen', inject(function ($rootScope) {
            //given
            var ctrl = createController();
            var preparation = {id: 'foo_beer', name: 'my old name'};

            expect($rootScope.$emit).not.toHaveBeenCalled();

            //when
            ctrl.rename(preparation, 'new preparation name');
            expect($rootScope.$emit).toHaveBeenCalledWith('talend.loading.start');
            scope.$digest();

            //then
            expect($rootScope.$emit).toHaveBeenCalledWith('talend.loading.stop');
        }));

        it('should not call preparation service to rename the preparation with empty name', inject(function (PreparationService, MessageService) {
            //given

            var ctrl = createController();
            var preparation = {id: 'foo_beer', name: 'my old name'};
            var name = '';

            //when
            ctrl.rename(preparation, name);
            scope.$digest();

            //then
            expect(PreparationService.setName).not.toHaveBeenCalled();
            expect(MessageService.success).not.toHaveBeenCalled();
        }));

        it('should not call preparation service to rename the preparation with null name', inject(function (PreparationService, MessageService) {
            //given

            var ctrl = createController();
            var preparation = {id: 'foo_beer', name: 'my old name'};

            //when
            ctrl.rename(preparation);
            scope.$digest();

            //then
            expect(PreparationService.setName).not.toHaveBeenCalled();
            expect(MessageService.success).not.toHaveBeenCalled();
        }));
    });

    describe('clone', function () {

        it('should call preparation service to clone the preparation', inject(function (PreparationService) {
            //given
            var ctrl = createController();
            var preparation = {id: 'foo_beer'};

            //when
            ctrl.clone(preparation);
            scope.$digest();

            //then
            expect(PreparationService.clone).toHaveBeenCalledWith(preparation.id);
        }));

        it('should show message on success', inject(function (MessageService) {
            //given
            var ctrl = createController();
            var preparation = {id: 'foo_beer'};

            expect(MessageService.success).not.toHaveBeenCalled();

            //when
            ctrl.clone(preparation);
            scope.$digest();

            //then
            expect(MessageService.success).toHaveBeenCalledWith('PREPARATION_COPYING_SUCCESS_TITLE', 'PREPARATION_COPYING_SUCCESS');
        }));

        it('should manage loader screen', inject(function ($rootScope) {
            //given
            var ctrl = createController();
            var preparation = {id: 'foo_beer'};

            expect($rootScope.$emit).not.toHaveBeenCalled();

            //when
            ctrl.clone(preparation);
            expect($rootScope.$emit).toHaveBeenCalledWith('talend.loading.start');
            scope.$digest();

            //then
            expect($rootScope.$emit).toHaveBeenCalledWith('talend.loading.stop');
        }));
    });
});
