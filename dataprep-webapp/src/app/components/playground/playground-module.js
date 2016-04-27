/*  ============================================================================

 Copyright (C) 2006-2016 Talend Inc. - www.talend.com

 This source code is available under agreement available at
 https://github.com/Talend/data-prep/blob/master/LICENSE

 You should have received a copy of the agreement
 along with this program; if not, write to Talend SA
 9 rue Pages 92150 Suresnes, France

 ============================================================================*/

import PlaygroundCtrl from './playground-controller';
import Playground from './playground-directive';
import PlaygroundHeader from './header/playground-header-component';

(() => {
    'use strict';

    /**
     * @ngdoc object
     * @name data-prep.playground
     * @description This module contains the controller and directives to manage the playground
     * @requires pascalprecht.translate
     * @requires talend.widget
     * @requires ui.router
     * @requires data-prep.datagrid
     * @requires data-prep.dataset-parameters
     * @requires data-prep.documentation-search
     * @requires data-prep.export
     * @requires data-prep.filter-bar
     * @requires data-prep.history-control
     * @requires data-prep.lookup
     * @requires data-prep.preparation-picker
     * @requires data-prep.recipe
     * @requires data-prep.services.onboarding
     * @requires data-prep.services.playground
     * @requires data-prep.services.preparation
     * @requires data-prep.services.recipe
     * @requires data-prep.services.state
     * @requires data-prep.suggestions-stats
     */
    angular.module('data-prep.playground',
        [
                "pascalprecht.translate",
                "talend.widget",
                "ui.router",
                "data-prep.datagrid",
                "data-prep.dataset-parameters",
                "data-prep.documentation-search",
                "data-prep.export",
                "data-prep.filter-bar",
                "data-prep.history-control",
                "data-prep.lookup",
                "data-prep.preparation-picker",
                "data-prep.recipe",
                "data-prep.services.onboarding",
                "data-prep.services.playground",
                "data-prep.services.preparation",
                "data-prep.services.recipe",
                "data-prep.services.state",
                "data-prep.suggestions-stats"
        ])
        .controller('PlaygroundCtrl', PlaygroundCtrl)
        .directive('playground', Playground)
        .component('playgroundHeader', PlaygroundHeader);
})();
