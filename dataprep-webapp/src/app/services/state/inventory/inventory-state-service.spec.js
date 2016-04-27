/*  ============================================================================

 Copyright (C) 2006-2016 Talend Inc. - www.talend.com

 This source code is available under agreement available at
 https://github.com/Talend/data-prep/blob/master/LICENSE

 You should have received a copy of the agreement
 along with this program; if not, write to Talend SA
 9 rue Pages 92150 Suresnes, France

 ============================================================================*/

describe('Inventory state service', function () {
    'use strict';

    let datasets, preparations, currentFolderContent;

    beforeEach(angular.mock.module('data-prep.services.state'));

    beforeEach(() => {
        datasets = [
            {
                'id': 'de3cc32a-b624-484e-b8e7-dab9061a009c',
                'name': 'customers_jso_light',
                'author': 'anonymousUser',
                'records': 15,
                'nbLinesHeader': 1,
                'nbLinesFooter': 0,
                'created': '03-30-2015 08:06'
            },
            {
                'id': '3b21388c-f54a-4334-9bef-748912d0806f',
                'name': 'customers_jso',
                'author': 'anonymousUser',
                'records': 1000,
                'nbLinesHeader': 1,
                'nbLinesFooter': 0,
                'created': '03-30-2015 07:35'
            }
        ];
        preparations = [
            {
                'id': 'ab136cbf0923a7f11bea713adb74ecf919e05cfa',
                'dataSetId': 'de3cc32a-b624-484e-b8e7-dab9061a009c',
                'author': 'anonymousUser',
                'creationDate': 1427447300300
            },
            {
                'id': 'fbaa18e82e913e97e5f0e9d40f04413412be1126',
                'dataSetId': '3b21388c-f54a-4334-9bef-748912d0806f',
                'author': 'anonymousUser',
                'creationDate': 1427447330693
            }
        ];
        currentFolderContent = {
            'folders': [
                {
                    'id': 'lookups/simple_lookup'
                }
            ],
            'datasets': [
                {
                    'id': '3b21388c-f54a-4334-9bef-748912d0806f',
                    'name': 'customers_jso',
                    'author': 'anonymousUser',
                    'records': 1000,
                    'nbLinesHeader': 1,
                    'nbLinesFooter': 0,
                    'created': '03-30-2015 07:35'
                }
            ]
        };
    });

    describe('preparations', () => {
        it('should set preparations', inject((inventoryState, InventoryStateService) => {
            //given
            inventoryState.preparations = null;
            inventoryState.datasets = null;
            inventoryState.currentFolderContent = {};

            //when
            InventoryStateService.setPreparations(preparations);

            //then
            expect(inventoryState.preparations).toBe(preparations);
        }));

        it('should consolidate preparations and datasets', inject((inventoryState, InventoryStateService) => {
            //given
            inventoryState.preparations = null;
            inventoryState.datasets = datasets;
            inventoryState.currentFolderContent = currentFolderContent;

            //when
            InventoryStateService.setPreparations(preparations);

            //then
            expect(inventoryState.datasets[0].preparations[0]).toBe(preparations[0]);
            expect(inventoryState.datasets[1].preparations[0]).toBe(preparations[1]);

            expect(inventoryState.currentFolderContent.datasets[0].preparations[0]).toBe(preparations[1]);

            expect(inventoryState.preparations[0].dataset).toBe(datasets[0]);
            expect(inventoryState.preparations[1].dataset).toBe(datasets[1]);
        }));

        it('should remove a preparation from preparations list', inject((inventoryState, InventoryStateService) => {
            //given
            inventoryState.preparations = preparations;

            //when
            InventoryStateService.removePreparation(preparations[0]);

            //then
            expect(inventoryState.preparations.length).toBe(1);
            expect(inventoryState.preparations[0].id).toBe('fbaa18e82e913e97e5f0e9d40f04413412be1126');
        }));
    });

    describe('datasets', () => {
        it('should set datasets', inject((inventoryState, InventoryStateService) => {
            //given
            inventoryState.preparations = null;
            inventoryState.datasets = null;
            inventoryState.currentFolderContent = {};

            //when
            InventoryStateService.setDatasets(datasets);

            //then
            expect(inventoryState.datasets).toBe(datasets);
        }));

        it('should consolidate preparations and datasets', inject((inventoryState, InventoryStateService) => {
            //given
            inventoryState.preparations = preparations;
            inventoryState.datasets = null;
            inventoryState.currentFolderContent = currentFolderContent;

            //when
            InventoryStateService.setDatasets(datasets);

            //then
            expect(inventoryState.datasets[0].preparations[0]).toBe(preparations[0]);
            expect(inventoryState.datasets[1].preparations[0]).toBe(preparations[1]);

            expect(inventoryState.currentFolderContent.datasets[0].preparations[0]).toBe(preparations[1]);

            expect(inventoryState.preparations[0].dataset).toBe(datasets[0]);
            expect(inventoryState.preparations[1].dataset).toBe(datasets[1]);
        }));

        it('should remove a dataset from datasets list', inject((inventoryState, InventoryStateService) => {
            //given
            inventoryState.datasets = datasets;

            //when
            InventoryStateService.removeDataset(datasets[0]);

            //then
            expect(inventoryState.datasets.length).toBe(1);
            expect(inventoryState.datasets[0].id).toBe('3b21388c-f54a-4334-9bef-748912d0806f');
        }));
    });

    describe('folder', () => {
        it('should set currentFolder', inject((inventoryState, InventoryStateService) => {
            //given
            inventoryState.currentFolder = null;
            const currentFolder = {
                'id': 'lookups',
                'path': 'lookups',
                'name': 'lookups',
                'creationDate': 1448880133000,
                'modificationDate': 1448880133000
            };

            //when
            InventoryStateService.setCurrentFolder(currentFolder);

            //then
            expect(inventoryState.currentFolder).toBe(currentFolder);
        }));

        it('should set currentFolderContent', inject((inventoryState, InventoryStateService) => {
            //given
            inventoryState.currentFolderContent = null;

            //when
            InventoryStateService.setCurrentFolderContent(currentFolderContent);

            //then
            expect(inventoryState.currentFolderContent).toBe(currentFolderContent);
        }));

        it('should consolidate current folder datasets', inject((inventoryState, InventoryStateService) => {
            //given
            inventoryState.currentFolderContent = null;
            inventoryState.preparations = preparations;

            //when
            InventoryStateService.setCurrentFolderContent(currentFolderContent);

            //then
            expect(inventoryState.currentFolderContent.datasets[0].preparations[0]).toBe(preparations[1]);
        }));

        it('should set foldersStack', inject((inventoryState, InventoryStateService) => {
            //given
            inventoryState.foldersStack = [];
            const foldersStack = [
                { 'id': '', 'path': '', 'name': 'Home' },
                { 'id': 'lookups', 'path': 'lookups', 'name': 'lookups' }
            ];

            //when
            InventoryStateService.setFoldersStack(foldersStack);

            //then
            expect(inventoryState.foldersStack).toBe(foldersStack);
        }));

        it('should set menuChildren', inject((inventoryState, InventoryStateService) => {
            //given
            inventoryState.menuChildren = [];
            const menuChildren = [
                {
                    'id': 'lookups/simple_lookup',
                    'path': 'lookups/simple_lookup',
                    'name': 'simple_lookup',
                    'creationDate': 1448880158000,
                    'modificationDate': 1448880158000
                }
            ];

            //when
            InventoryStateService.setMenuChildren(menuChildren);

            //then
            expect(inventoryState.menuChildren).toBe(menuChildren);
        }));
    });

    describe('sort', () => {
        it('should set sort', inject((inventoryState, InventoryStateService) => {
            //given
            inventoryState.sort = '';

            //when
            InventoryStateService.setSort('name');

            //then
            expect(inventoryState.sort).toBe('name');
        }));

        it('should set order', inject((inventoryState, InventoryStateService) => {
            //given
            inventoryState.order = '';

            //when
            InventoryStateService.setOrder('desc');

            //then
            expect(inventoryState.order).toBe('desc');
        }));
    });

    describe('update dataset name', () => {
        it('should update name in all dataset list', inject((inventoryState, InventoryStateService) => {
            // given
            inventoryState.datasets = [
                { id: '1', name: 'toto' },
                { id: '2', name: 'tata' },
                { id: '3', name: 'titi' },
                { id: '4', name: 'tutu' },
            ];

            // when
            InventoryStateService.setDatasetName('2', 'tonton');

            // then
            expect(inventoryState.datasets[1].name).toBe('tonton');
        }));

        it('should update name in folder content list', inject((inventoryState, InventoryStateService) => {
            // given
            inventoryState.currentFolderContent = {
                datasets: [
                    { id: '1', name: 'toto' },
                    { id: '2', name: 'tata' },
                    { id: '3', name: 'titi' },
                    { id: '4', name: 'tutu' },
                ],
            };

            // when
            InventoryStateService.setDatasetName('2', 'tonton');

            // then
            expect(inventoryState.currentFolderContent.datasets[1].name).toBe('tonton');
        }));
    });
});