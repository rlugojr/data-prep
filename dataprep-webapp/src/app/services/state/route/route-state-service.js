/*  ============================================================================

 Copyright (C) 2006-2016 Talend Inc. - www.talend.com

 This source code is available under agreement available at
 https://github.com/Talend/data-prep/blob/master/LICENSE

 You should have received a copy of the agreement
 along with this program; if not, write to Talend SA
 9 rue Pages 92150 Suresnes, France

 ============================================================================*/

export const routeState = {
    previous: 'nav.index.preparations',
    previousOptions: null,
    next: 'nav.index.preparations',
    nextOptions: null
};

export class RouteStateService {
    setPrevious(route, routeOptions) {
        if(!route) {
            return;
        }
        routeState.previous = route;
        routeState.previousOptions = routeOptions;
    }

    setNext(route, routeOptions) {
        if(!route) {
            return;
        }
        routeState.next = route;
        routeState.nextOptions = routeOptions;
    }

    resetPrevious() {
        routeState.previous = 'nav.index.preparations';
        routeState.previousOptions = null;
    }

    resetNext() {
        routeState.next = 'nav.index.preparations';
        routeState.nextOptions = null;
    }

    reset() {
        this.resetPrevious();
        this.resetNext();
    }
}