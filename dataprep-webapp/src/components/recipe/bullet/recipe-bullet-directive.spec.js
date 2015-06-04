describe('recipeBullet directive', function() {
	'use strict';
	
	var createElement, element, scope, steps;

	function getSvgElementAttribute(elementName, elementIndex, attr) {
		var allSvg = element.find('svg');
		var result = [];
		allSvg.each(function(index) {
			result.push(allSvg.eq(index).find(elementName).eq(elementIndex).attr(attr));
		});

		return result;
	}

	function getTopCablesClasses() {
		return getSvgElementAttribute('path', 0, 'class');
	}

	function getBottomCablesClasses() {
		return getSvgElementAttribute('path', 1, 'class');
	}

	function getTopCablesDimensions() {
		return getSvgElementAttribute('path', 0, 'd');
	}

	function getBottomCablesDimensions() {
		return getSvgElementAttribute('path', 1, 'd');
	}

	function getCircleYPosition() {
		return getSvgElementAttribute('circle', 0, 'cy');
	}

	function getCircleClasses() {
		return getSvgElementAttribute('circle', 0, 'class');
	}
	
	beforeEach(module('data-prep.recipe-bullet'));
	beforeEach(module('htmlTemplates'));
	beforeEach(inject(function($rootScope, $compile, $timeout, RecipeService, RecipeBulletService) {
		steps = [
			{column:{id:'col2'},transformation:{name:'uppercase',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:false},
			{column:{id:'col1'},transformation:{name:'lowerercase',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:false},
			{column:{id:'col3'},transformation:{name:'negate',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:true},
			{column:{id:'col4'},transformation:{name:'propercase',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:true},
			{column:{id:'col1'},transformation:{name:'rename',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:true}
		];

		createElement = function() {
			scope = $rootScope.$new();
			_.forEach(steps, function(step, index) {
				scope['step' + index] = step;
			});

			element = angular.element(
				'<div class="recipe">' +
				'	<div style="height: 50px;"><recipe-bullet step="step0"></recipe-bullet></div>' +
				'	<div style="height: 40px;"><recipe-bullet step="step1"></recipe-bullet></div>' +
				'	<div style="height: 30px;"><recipe-bullet step="step2"></recipe-bullet></div>' +
				'	<div style="height: 20px;"><recipe-bullet step="step3"></recipe-bullet></div>' +
				'	<div style="height: 10px;"><recipe-bullet step="step4"></recipe-bullet></div>' +
				'</div>');

			angular.element('body').append(element);
			$compile(element)(scope);
			scope.$digest();
			$timeout.flush();
		};

		spyOn(RecipeService, 'isFirstStep').and.callFake(function(step) {
			return step === steps[0];
		});
		spyOn(RecipeService, 'isLastStep').and.callFake(function(step) {
			return step === steps[4];
		});
		spyOn(RecipeService, 'getActiveThresholdStepIndex').and.callFake(function() {
			return 1;
		});
		spyOn(RecipeService, 'getStepIndex').and.callFake(function(step) {
			return steps.indexOf(step);
		});
		spyOn(RecipeBulletService, 'stepHoverStart').and.returnValue();
		spyOn(RecipeBulletService, 'stepHoverEnd').and.returnValue();
	}));

	afterEach(inject(function(RecipeService ) {
		scope.$destroy();
		element.remove();
		RecipeService.reset();
	}));

	it('should render all bullets', function() {
		//when
		createElement();

		//then
		expect(element.find('svg').length).toBe(5);
		expect(element.find('.all-svg-cls').eq(0).attr('class').indexOf('maillon-circle') > -1).toBe(true);
		expect(element.find('.all-svg-cls').eq(1).attr('class').indexOf('maillon-circle') > -1).toBe(true);
		expect(element.find('.all-svg-cls').eq(2).attr('class').indexOf('maillon-circle-disabled') > -1).toBe(true);
		expect(element.find('.all-svg-cls').eq(3).attr('class').indexOf('maillon-circle-disabled') > -1).toBe(true);
		expect(element.find('.all-svg-cls').eq(4).attr('class').indexOf('maillon-circle-disabled') > -1).toBe(true);
	});

	it('should hide top cable on first step only', function() {
		//when
		createElement();

		//then
		var classes = getTopCablesClasses();

		expect(classes[0] && classes[0].indexOf('ng-hide') > -1).toBeTruthy();
		for(var i = 1; i < 5; i++) {
			expect(classes[i] && classes[i].indexOf('ng-hide') > -1).toBeFalsy();
		}
	});

	it('should hide bottom cable on last step only', function() {
		//when
		createElement();

		//then
		var classes = getBottomCablesClasses();

		for(var i = 0; i < 4; i++) {
			expect(classes[i] && classes[i].indexOf('ng-hide') > -1).toBeFalsy();
		}
		expect(classes[4] && classes[4].indexOf('ng-hide') > -1).toBeTruthy();
	});

	it('should init circle position', function() {
		//when
		createElement();

		//then
		var positions = getCircleYPosition();
		expect(positions[0]).toBe('30');
		expect(positions[1]).toBe('25');
		expect(positions[2]).toBe('20');
		expect(positions[3]).toBe('15');
		expect(positions[4]).toBe('10');
	});

	it('should init top cable dimensions', function() {
		//when
		createElement();

		//then
		var dimensions = getTopCablesDimensions();
		expect(dimensions[0]).toBe('M 15 0 L 15 20 Z');
		expect(dimensions[1]).toBe('M 15 0 L 15 15 Z');
		expect(dimensions[2]).toBe('M 15 0 L 15 10 Z');
		expect(dimensions[3]).toBe('M 15 0 L 15 5 Z');
		expect(dimensions[4]).toBe('M 15 0 L 15 0 Z');
	});

	it('should init bottom cable dimensions', function() {
		//when
		createElement();

		//then
		var dimensions = getBottomCablesDimensions();
		expect(dimensions[0]).toBe('M 15 42 L 15 60 Z');
		expect(dimensions[1]).toBe('M 15 37 L 15 50 Z');
		expect(dimensions[2]).toBe('M 15 32 L 15 40 Z');
		expect(dimensions[3]).toBe('M 15 27 L 15 30 Z');
		expect(dimensions[4]).toBe('M 15 22 L 15 20 Z');
	});

	it('should call hover start actions on mouseover', inject(function(RecipeBulletService) {
		//given
		createElement();
		var event = new angular.element.Event('mouseenter');

		//when
		element.find('recipe-bullet').eq(3).trigger(event);

		//then
		expect(RecipeBulletService.stepHoverStart).toHaveBeenCalled();
	}));

	it('should set "inactive hover class" from threshold to inactive step on mouseover', function() {
		//given
		createElement();
		var event = new angular.element.Event('mouseenter');

		//when
		element.find('recipe-bullet').eq(3).trigger(event);

		//then
		var classes = getCircleClasses();
		expect(classes[0]).toBeFalsy();
		expect(classes[1]).toBeFalsy();
		expect(classes[2]).toBe('maillon-circle-disabled-hovered');
		expect(classes[3]).toBe('maillon-circle-disabled-hovered');
		expect(classes[4]).toBeFalsy();
	});

	it('should set "active hover class" from active step to the end on mouseover', function() {
		//given
		createElement();
		var event = new angular.element.Event('mouseenter');

		//when
		element.find('recipe-bullet').eq(1).trigger(event);

		//then
		var classes = getCircleClasses();
		expect(classes[0]).toBeFalsy();
		expect(classes[1]).toBe('maillon-circle-enabled-hovered');
		expect(classes[2]).toBe('maillon-circle-enabled-hovered');
		expect(classes[3]).toBe('maillon-circle-enabled-hovered');
		expect(classes[4]).toBe('maillon-circle-enabled-hovered');
	});


//
//	it('onmouseenter test on Launch, all the elements are active', function() {
//		//given
//		var step1 = {column:{id:'col2'},transformation:{name:'uppercase',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:false, rank:0};
//		var step2 = {column:{id:'col1'},transformation:{name:'lowerercase',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:false, rank:1};
//		var step3 = {column:{id:'col3'},transformation:{name:'negate',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:false, rank:2};
//		var step4 = {column:{id:'col4'},transformation:{name:'propercase',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:false, rank:3};
//		var step5 = {column:{id:'col1'},transformation:{name:'rename',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:false, rank:4};
//		createElement([step1,step2,step3,step4,step5]);
//		//create event
//		var event = angular.element.Event('mouseenter');
//
//		//enable angular.element('.svg-all-cls') selection
//		var body = angular.element('body');
//		body.append(element);
//		body.append(angular.element("<svg class='tout-svg-cls'><path d='M 15 0 L 15 30 A' /> <circle cx=15 cy=15 r=10 /></svg>"));
//		mochStepHoverStart(0);
//
//		//then
//		//when trigger mouseenter on 1st element
//		//console.log(element[0],"---------------------");
//
//		element.eq(0).trigger(event);
//		scope.$digest();
//
//		//assert
//		var nbreEnaHov = angular.element('.maillon-circle-enabled-hovered').length;
//		expect(nbreEnaHov).toBe(5);
//	});
//
//	it('onmouseenter test when all the elements are inactive', function() {
//		//given
//		var step1 = {column:{id:'col2'},transformation:{name:'uppercase',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:true, rank:0};
//		var step2 = {column:{id:'col1'},transformation:{name:'lowerercase',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:true, rank:1};
//		var step3 = {column:{id:'col3'},transformation:{name:'negate',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:true, rank:2};
//		var step4 = {column:{id:'col4'},transformation:{name:'propercase',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:true, rank:3};
//		var step5 = {column:{id:'col1'},transformation:{name:'rename',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:true, rank:4};
//		createElement([step1,step2,step3,step4,step5]);
//		mochStepHoverStart(2);
//		//create event
//		var event = angular.element.Event('mouseenter');
//
//		//because the only way to get all the bullets disabled is to click on the 1st bullet
//		spyOn(recipeBulletCtrl.recipeService, 'getActiveThresholdStepIndexOnLaunch').and.returnValue(-1);
//
//		//enable angular.element('.svg-all-cls') selection
//		var body = angular.element('body');
//		body.append(element);
//		//then
//		//when trigger mouseenter on 1st element
//		element.eq(2).trigger(event);
//		scope.$digest();
//
//		//assert
//		var nbreDisaHov = angular.element('.maillon-circle-disabled-hovered').length;
//		expect(nbreDisaHov).toBe(3);
//
//		var rightClassName = element.eq(0).find('circle')[0].getAttribute("class")
//			=== element.eq(1).find('circle')[0].getAttribute("class")
//			&& element.eq(2).find('circle')[0].getAttribute("class")
//			=== "maillon-circle-disabled-hovered";
//		expect(rightClassName).toBe(true);
//	});
//
//	it('onmouseenter test when 3 first bullets are active 2 are inactive and the 2nd was hovered', function() {
//		//given
//		var step1 = {column:{id:'col2'},transformation:{name:'uppercase',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:false, rank:0};
//		var step2 = {column:{id:'col1'},transformation:{name:'lowerercase',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:false, rank:1};
//		var step3 = {column:{id:'col3'},transformation:{name:'negate',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:false, rank:2};
//		var step4 = {column:{id:'col4'},transformation:{name:'propercase',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:true, rank:3};
//		var step5 = {column:{id:'col1'},transformation:{name:'rename',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:true, rank:4};
//		createElement([step1,step2,step3,step4,step5]);
//		mochStepHoverStart(1);
//		//create event
//		var event = angular.element.Event('mouseenter');
//
//		//because the only way to get all the bullets disabled is to click on the 1st bullet
//		spyOn(recipeBulletCtrl.recipeService, 'getActiveThresholdStepIndexOnLaunch').and.returnValue(2);
//
//		//enable angular.element('.svg-all-cls') selection
//		var body = angular.element('body');
//		body.append(element);
//		//then
//		//when trigger mouseenter on 1st element
//		element.eq(1).trigger(event);
//		scope.$digest();
//
//		//assert
//		var nbreDisaHov = angular.element('.maillon-circle-enabled-hovered').length;
//		expect(nbreDisaHov).toBe(2);
//
//		var rightClassName = element.eq(1).find('circle')[0].getAttribute("class")
//			=== element.eq(2).find('circle')[0].getAttribute("class")
//			&& element.eq(2).find('circle')[0].getAttribute("class")
//			=== "maillon-circle-enabled-hovered";
//		expect(rightClassName).toBe(true);
//	});
//
//
//	it('onmouseenter test when 3 first bullets are active 2 are inactive and the last one was hovered', function() {
//		//given
//		var step1 = {column:{id:'col2'},transformation:{name:'uppercase',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:false, rank:0};
//		var step2 = {column:{id:'col1'},transformation:{name:'lowerercase',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:false, rank:1};
//		var step3 = {column:{id:'col3'},transformation:{name:'negate',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:false, rank:2};
//		var step4 = {column:{id:'col4'},transformation:{name:'propercase',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:true, rank:3};
//		var step5 = {column:{id:'col1'},transformation:{name:'rename',label:'To uppercase',category:'case',parameters:[],items:[]},inactive:true, rank:4};
//		createElement([step1,step2,step3,step4,step5]);
//		mochStepHoverStart(4);
//		//create event
//		var event = angular.element.Event('mouseenter');
//
//		//because the only way to get all the bullets disabled is to click on the 1st bullet
//		spyOn(recipeBulletCtrl.recipeService, 'getActiveThresholdStepIndexOnLaunch').and.returnValue(2);
//
//		//enable angular.element('.svg-all-cls') selection
//		var body = angular.element('body');
//		body.append(element);
//		//then
//		//when trigger mouseenter on 1st element
//		element.eq(4).trigger(event);
//		scope.$digest();
//
//		//assert
//		var nbreDisaHov = angular.element('.maillon-circle-disabled-hovered').length;
//		expect(nbreDisaHov).toBe(2);
//
//		var rightClassName = element.eq(3).find('circle')[0].getAttribute("class")
//			=== element.eq(4).find('circle')[0].getAttribute("class")
//			&& element.eq(4).find('circle')[0].getAttribute("class")
//			=== "maillon-circle-disabled-hovered";
//		expect(rightClassName).toBe(true);
//	});


});