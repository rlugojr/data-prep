/*  ============================================================================

 Copyright (C) 2006-2016 Talend Inc. - www.talend.com

 This source code is available under agreement available at
 https://github.com/Talend/data-prep/blob/master/LICENSE

 You should have received a copy of the agreement
 along with this program; if not, write to Talend SA
 9 rue Pages 92150 Suresnes, France

 ============================================================================*/

import InventoryListCtrl from './inventory-list-controller';

/**
 * Inventory list container
 * @restrict E
 */
const InventoryListContainer = {
	template: `
		<div ng-if="$ctrl.isLoading"
		     class="fetch-loader">
	        <span class="fetch-loader-icon continuous-rotate"><icon name="'talend-dataprep'"></icon></span>
	        <span translate-once="LOADING"></span>
		</div>
		<pure-list
			ng-if="!$ctrl.isLoading"
			id="$ctrl.id"
			display-mode="$ctrl.displayMode"
			list="$ctrl.listProps"
			toolbar="$ctrl.toolbarProps"
			watch-depth="reference"
		/>
	`,
	bindings: {
		id: '<',
		displayMode: '<',
		folders: '<',
		isLoading: '<',
		items: '<',
		sortBy: '<',
		sortDesc: '<',
		viewKey: '<',
		folderViewKey: '<',
	},
	controller: InventoryListCtrl,
};

export default InventoryListContainer;
