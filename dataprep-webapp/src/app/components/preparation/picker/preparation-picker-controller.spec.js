/*  ============================================================================

 Copyright (C) 2006-2016 Talend Inc. - www.talend.com

 This source code is available under agreement available at
 https://github.com/Talend/data-prep/blob/master/LICENSE

 You should have received a copy of the agreement
 along with this program; if not, write to Talend SA
 9 rue Pages 92150 Suresnes, France

 ============================================================================*/

describe('Preparation Picker controller', () => {

    const datasets = [
        {id: 'de3cc32a-b624-484e-b8e7-dab9061a009c', name: 'my dataset'},
        {id: '4d0a2718-bec6-4614-ad6c-8b3b326ff6c9', name: 'my second dataset'},
        {id: '555a2718-bec6-4614-ad6c-8b3b326ff6c7', name: 'my second dataset (1)'}
    ];

    let compatiblePreps = [
        {
            prepration: {
                'id': 'ab136cbf0923a7f11bea713adb74ecf919e05cfa',
                'dataSetId': 'de3cc32a-b624-484e-b8e7-dab9061a009c',
                'author': 'anonymousUser',
                'creationDate': 1427447300300,
                'steps': [
                    '35890aabcf9115e4309d4ce93367bf5e4e77b82a',
                    '4ff5d9a6ca2e75ebe3579740a4297fbdb9b7894f',
                    '8a1c49d1b64270482e8db8232357c6815615b7cf',
                    '599725f0e1331d5f8aae24f22cd1ec768b10348d'
                ]
            },
            dataset: datasets[0]
        },
        {
            prepration: {
                'id': 'fbaa18e82e913e97e5f0e9d40f04413412be1126',
                'dataSetId': '4d0a2718-bec6-4614-ad6c-8b3b326ff6c9',
                'author': 'anonymousUser',
                'creationDate': 1427447330693,
                'steps': [
                    '47e2444dd1301120b539804507fd307072294048',
                    'ae1aebf4b3fa9b983c895486612c02c766305410',
                    '24dcd68f2117b9f93662cb58cc31bf36d6e2867a',
                    '599725f0e1331d5f8aae24f22cd1ec768b10348d'
                ]
            },
            dataset: datasets[1]
        }
    ];

    let createController, scope, ctrl, stateMock;

    beforeEach(angular.mock.module('data-prep.preparation-picker', ($provide) => {
        stateMock = {
            inventory: {
                datasets: datasets
            },
            playground: {
                preparation: null,
                dataset: {
                    id: 'dsId',
                    name: 'myDsName'
                }
            }
        };
        $provide.constant('state', stateMock);
    }));

    beforeEach(inject(($rootScope, $componentController) => {
        scope = $rootScope.$new();

        createController = () => {
            return $componentController('preparationPicker', {$scope: scope});
        };
    }));

    beforeEach(() => {
        ctrl = createController();
    });

    describe('initialization', () => {

        it('should call fetch compatible preparations callback', inject(($q, DatasetService) => {
            //given
            spyOn(DatasetService, 'getCompatiblePreparations').and.returnValue($q.when());

            //when
            ctrl.$onInit();

            //then
            expect(DatasetService.getCompatiblePreparations).toHaveBeenCalledWith(stateMock.playground.dataset.id);
        }));

        it('should fetch and populate the compatible preparations starting from a dataset', inject(($q, DatasetService) => {
            //given
            spyOn(DatasetService, 'getCompatiblePreparations').and.returnValue($q.when(compatiblePreps));

            //when
            ctrl.$onInit();
            expect(ctrl.isFetchingPreparations).toBe(true);
            scope.$digest();

            //then
            expect(ctrl.candidatePreparations).toBe(compatiblePreps);
            expect(ctrl.isFetchingPreparations).toBe(false);
        }));
    });

    describe('select a preparation', () => {
        it('should call application process function', inject(($q, PlaygroundService) => {
            //given
            const chosenPreparation = compatiblePreps[0];
            spyOn(PlaygroundService, 'applyPreparationToDataset').and.returnValue($q.when(true));
            ctrl.selectedPreparation = chosenPreparation.preparation;
            ctrl.datasetId = 'myDsName';
            ctrl.newPreparationName = 'new Name';

            //when
            ctrl.selectPreparation(chosenPreparation);

            //then
            expect(PlaygroundService.applyPreparationToDataset).toHaveBeenCalledWith(ctrl.selectedPreparation.id, ctrl.dataset.id);
        }));

        it('should redirect to the new preparation', inject(($q, $state, PlaygroundService) => {
            //given
            const chosenPreparation = compatiblePreps[0];
            spyOn(PlaygroundService, 'applyPreparationToDataset').and.returnValue($q.when('updatedPreparationId'));
            spyOn($state, 'go').and.returnValue();
            ctrl.selectedPreparation = chosenPreparation.preparation;
            ctrl.datasetId = 'myDsName';
            ctrl.newPreparationName = 'new Name';

            //when
            ctrl.selectPreparation(chosenPreparation);
            scope.$digest();

            //then
            expect($state.go).toHaveBeenCalledWith('playground.preparation', {prepid: 'updatedPreparationId'}, {reload: true});
        }));
    });
});