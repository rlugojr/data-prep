/*  ============================================================================

 Copyright (C) 2006-2016 Talend Inc. - www.talend.com

 This source code is available under agreement available at
 https://github.com/Talend/data-prep/blob/master/LICENSE

 You should have received a copy of the agreement
 along with this program; if not, write to Talend SA
 9 rue Pages 92150 Suresnes, France

 ============================================================================*/

import DatasetPicker from './dataset-picker-component';

(() => {

    /**
     * @ngdoc object
     * @name data-prep.preparation-picker
     * @description This module contains the preparation picker form
     * @requires data-prep.services.preparation
     */

    angular.module('data-prep.dataset-picker',
        [
            'data-prep.services.preparation'
        ])
        .component('datasetPicker', DatasetPicker);
})();
