/*  ============================================================================

 Copyright (C) 2006-2016 Talend Inc. - www.talend.com

 This source code is available under agreement available at
 https://github.com/Talend/data-prep/blob/master/LICENSE

 You should have received a copy of the agreement
 along with this program; if not, write to Talend SA
 9 rue Pages 92150 Suresnes, France

 ============================================================================*/

describe('dataset Picker controller', () => {

    const compatibleDatasets = [
        {id: 'de3cc32a-b624-484e-b8e7-dab9061a009c', name: 'my dataset'},
        {id: '4d0a2718-bec6-4614-ad6c-8b3b326ff6c9', name: 'my second dataset'},
        {id: '555a2718-bec6-4614-ad6c-8b3b326ff6c7', name: 'my second dataset (1)'}
    ];

    let createController, scope, ctrl, stateMock;

    beforeEach(angular.mock.module('data-prep.dataset-picker'));

    beforeEach(inject(($rootScope, $componentController) => {
        scope = $rootScope.$new();

        createController = () => {
            return $componentController('datasetPicker',
                {$scope: scope},
                {
                    displayPicker: true,
                    preparation: {
                        'id': 'fbaa18e82e913e97e5f0e9d40f04413412be1126',
                        'dataSetId': '4d0a2718-bec6-4614-ad6c-8b3b326ff6c9'
                    }
                });
        };
    }));

    beforeEach(() => {
        ctrl = createController();
    });

    describe('initialization', () => {

        it('should call fetch compatible preparations callback', inject(($q, PreparationService) => {
            //given
            spyOn(PreparationService, 'getCandidateDatasets').and.returnValue($q.when());

            //when
            ctrl.$onInit();

            //then
            expect(PreparationService.getCandidateDatasets).toHaveBeenCalledWith(ctrl.preparation.id);
        }));

        it('should fetch and populate the compatible preparations starting from a dataset', inject(($q, PreparationService) => {
            //given
            spyOn(PreparationService, 'getCandidateDatasets').and.returnValue($q.when(compatibleDatasets));

            //when
            ctrl.$onInit();
            expect(ctrl.isFetchingDatasets).toBe(true);
            scope.$digest();

            //then
            expect(ctrl.candidateDatasets).toBe(compatibleDatasets);
            expect(ctrl.isFetchingDatasets).toBe(false);
        }));
    });

    describe('select a dataset', inject(($q, $rootScope, PreparationService) => {
        const chosenDatasetId = 'datasetid';
        const prepid = 'prepid';

        beforeEach('', inject(() => {
            spyOn(PreparationService, 'update').and.returnValue($q.when(true));
            spyOn(PreparationService, 'refreshPreparations').and.returnValue($q.when(true));
            spyOn($rootScope, '$emit').and.returnValue();
        }));

        it('should show spinner while updating', inject(($q, $rootScope, PreparationService) => {
            //when
            ctrl.replaceBaseDataset(chosenDatasetId, prepid);
            expect($rootScope.$emit).toHaveBeenCalledWith('talend.loading.start');
            scope.$digest();

            //then
            expect($rootScope.$emit).toHaveBeenCalledWith('talend.loading.stop');
        }));

        it('should call update function', inject(($q, $rootScope, PreparationService) => {
            //when
            ctrl.replaceBaseDataset(chosenDatasetId, prepid);

            //then
            expect(PreparationService.update).toHaveBeenCalledWith(prepid, {dataSetId: chosenDatasetId});
        }));

        it('should call refresh function', inject(($q, $rootScope, PreparationService) => {
            //when
            ctrl.replaceBaseDataset(chosenDatasetId, prepid);
            scope.$digest();

            //then
            expect(PreparationService.refreshPreparations).toHaveBeenCalled();
        }));
    }));
});