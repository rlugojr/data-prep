/*  ============================================================================

 Copyright (C) 2006-2016 Talend Inc. - www.talend.com

 This source code is available under agreement available at
 https://github.com/Talend/data-prep/blob/master/LICENSE

 You should have received a copy of the agreement
 along with this program; if not, write to Talend SA
 9 rue Pages 92150 Suresnes, France

 ============================================================================*/

'use strict';

const sortList = [
    {id: 'name', name: 'NAME_SORT', property: 'name'},
    {id: 'date', name: 'DATE_SORT', property: 'created'}
];

const orderList = [
    {id: 'asc', name: 'ASC_ORDER'},
    {id: 'desc', name: 'DESC_ORDER'}
];

export const inventoryState = {
    preparations: null,
    datasets: null,
    sortList: sortList,
    orderList: orderList,
    sort: sortList[1],
    order: orderList[1],

    currentFolder: {id: '', path: ''}, // currentFolder is initialized with root value
    currentFolderContent: {},
    foldersStack: [],
    menuChildren: []
};


export function InventoryStateService() {
    return {
        setPreparations: setPreparations,
        removePreparation: removePreparation,

        setDatasets: setDatasets,
        removeDataset: removeDataset,
        setDatasetName: setDatasetName,

        setCurrentFolder: setCurrentFolder,
        setCurrentFolderContent: setCurrentFolderContent,
        setFoldersStack: setFoldersStack,
        setMenuChildren: setMenuChildren,

        setSort: setSort,
        setOrder: setOrder
    };

    /**
     * @ngdoc method
     * @name consolidateDatasets
     * @methodOf data-prep.services.state.service:InventoryStateService
     * @param {array} datasets The datasets list to consolidate
     * @description Set the preparations list in each dataset to consolidate
     */
    function consolidateDatasets(datasets) {
        if(!datasets || !inventoryState.preparations) {
            return;
        }

        var preparationsByDataset = _.groupBy(inventoryState.preparations, 'dataSetId');
        _.forEach(datasets, function (dataset) {
            var preparations = preparationsByDataset[dataset.id] || [];
            dataset.preparations = _.sortByOrder(preparations, 'lastModificationDate', false);
        });
    }

    /**
     * @ngdoc method
     * @name consolidatePreparations
     * @methodOf data-prep.services.state.service:InventoryStateService
     * @param {array} preparations The preparations list to consolidate
     * @description Set the dataset in each preparation to consolidate
     */
    function consolidatePreparations(preparations) {
        if(!preparations || !inventoryState.datasets) {
            return;
        }

        _.forEach(preparations, function (prep) {
            prep.dataset = _.find(inventoryState.datasets, {id: prep.dataSetId});
        });
    }

    /**
     * @ngdoc method
     * @name consolidate
     * @methodOf data-prep.services.state.service:InventoryStateService
     * @description Consolidate preparations, datasets and the current folder datasets
     */
    function consolidate() {
        consolidatePreparations(inventoryState.preparations);
        consolidateDatasets(inventoryState.datasets);
        consolidateDatasets(inventoryState.currentFolderContent.datasets);
    }

    /**
     * @ngdoc method
     * @name setPreparations
     * @methodOf data-prep.services.state.service:InventoryStateService
     * @param {array} preparations The preparations list
     * @description Set preparations in Inventory
     */
    function setPreparations(preparations) {
        inventoryState.preparations = preparations;
        consolidate();
    }

    /**
     * @ngdoc method
     * @name removePreparation
     * @methodOf data-prep.services.state.service:InventoryStateService
     * @param {object} preparation The preparation
     * @description Remove a preparation
     */
    function removePreparation(preparation) {
        inventoryState.preparations = _.reject(inventoryState.preparations, {id: preparation.id});
    }

    /**
     * @ngdoc method
     * @name setDatasets
     * @methodOf data-prep.services.state.service:InventoryStateService
     * @param {array} datasets The datasets list
     * @description Set datasets in Inventory
     */
    function setDatasets(datasets) {
        inventoryState.datasets = datasets;
        consolidate();
    }

    /**
     * @ngdoc method
     * @name removeDataset
     * @methodOf data-prep.services.state.service:InventoryStateService
     * @param {object} dataset The dataset
     * @description Remove a dataset
     */
    function removeDataset(dataset) {
        inventoryState.datasets = _.reject(inventoryState.datasets, {id: dataset.id});
    }

    /**
     * @ngdoc method
     * @name setDatasetName
     * @methodOf data-prep.services.state.service:InventoryStateService
     * @param {string} datasetId The dataset id
     * @param {string} name The dataset name
     * @description Change the dataset name in folder and datasets list
     */
    function setDatasetName(datasetId, name) {
        if(inventoryState.datasets) {
            const dataset = _.find(inventoryState.datasets, {id: datasetId});
            dataset.name = name;
        }

        if(inventoryState.currentFolderContent && inventoryState.currentFolderContent.datasets) {
            const dataset = _.find(inventoryState.currentFolderContent.datasets, {id: datasetId});
            if(dataset) {
                dataset.name = name;
            }
        }
    }

    /**
     * @ngdoc method
     * @name setCurrentFolder
     * @methodOf data-prep.services.state.service:InventoryStateService
     * @param {object} folder The current folder
     * @description Update the current folder
     */
    function setCurrentFolder(folder) {
        inventoryState.currentFolder = folder;
    }

    /**
     * @ngdoc method
     * @name setCurrentFolderContent
     * @methodOf data-prep.services.state.service:InventoryStateService
     * @param {object} content The content of the current folder
     */
    function setCurrentFolderContent(content) {
        inventoryState.currentFolderContent = content;
        consolidateDatasets(inventoryState.currentFolderContent.datasets);
    }

    /**
     * @ngdoc method
     * @name setFoldersStack
     * @methodOf data-prep.services.state.service:InventoryStateService
     * @param {object} stack The current folders stack
     */
    function setFoldersStack(stack) {
        inventoryState.foldersStack = stack;
    }

    /**
     * @ngdoc method
     * @name setMenuChildren
     * @methodOf data-prep.services.state.service:InventoryStateService
     * @param {array} children The current children of the current menu entry
     */
    function setMenuChildren(children) {
        inventoryState.menuChildren = children;
    }

    /**
     * @ngdoc method
     * @name setSort
     * @methodOf data-prep.services.state.service:InventoryStateService
     * @param {object} sort The sort type
     * @description Set rhe sort type
     */
    function setSort(sort) {
        inventoryState.sort = sort;
    }

    /**
     * @ngdoc method
     * @name setOrder
     * @methodOf data-prep.services.state.service:InventoryStateService
     * @param {object} order The order
     * @description Set the order
     */
    function setOrder(order) {
        inventoryState.order = order;
    }
}