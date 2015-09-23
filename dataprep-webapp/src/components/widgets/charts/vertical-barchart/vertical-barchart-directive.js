(function () {
	'use strict';

	/**
	 * @ngdoc directive
	 * @name talend.widget.directive:verticalBarchart
	 * @description This directive renders the vertical bar chart.
	 * @restrict E
	 * @usage
	 * <vertical-barchart id="vBarChart"
	 *	 width="320"
	 *	 height="400"
	 *	 on-click="columnProfileCtrl.vBarchartClickFn"
	 *	 visu-data="columnProfileCtrl.histogram.numData"
	 *	 value-field="{{columnProfileCtrl.histogram.key}}"
	 *	 key-field="data"
	 *	 existing-filter="columnProfileCtrl.histogram.existingFilter"
	 *	 key-label="{{columnProfileCtrl.histogram.label}}">
 	 * </vertical-barchart>
	 * */

	function VerticalBarchart() {
		return {
			restrict: 'E',
			scope: {
				onClick: '&',
				visuData: '=',
				keyField: '@',
				valueField: '@',
				keyLabel:'@',
				existingFilter:'='
			},
			link: function (scope, element, attrs) {
				var xField = scope.keyField;//data
				var yField = scope.valueField;
				var labelTooltip = scope.keyLabel;
				var existingFilter = scope.existingFilter;
				var renderTimeout, updateBarsTimeout;
				var tip;
				scope.finishedRendering = false;

				function renderVBarchart(statData) {
					var container = attrs.id;
					var width = +attrs.width;
					var height = +attrs.height;

					//the left and right margins MUST be the same as the rangeSlider ones
					var margin = {top: 20,right: 20,bottom: 10,left: 15},
						w = width - margin.left - margin.right,
						h = height - margin.top - margin.bottom;

					var x = d3.scale.ordinal()
						.rangeRoundBands([0, w], 0.2);
					var y = d3.scale.linear()
						.range([h, 0]);

					tip = d3.tip()
						.attr('class', 'vertical-barchart-cls d3-tip')
						.offset([0, -11])
						.direction('w')
						.html(function(d) {
							return 	'<strong>'+labelTooltip+':</strong> <span style="color:yellow">' + d[yField] + '</span>'+
								'<br/>'+
								'<br/>'+
								'<strong>Range:</strong> <span style="color:yellow">['+ d[xField] + ']</span>';
						});

					var svg = d3.select('#'+container).append('svg')
						.attr('class', 'vertical-barchart-cls')
						.attr('width', w + margin.left + margin.right)
						.attr('height', h + margin.top + margin.bottom)
						.append('g')
						.attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');

					svg.call(tip);

					// Parse numbers, and sort by value.
					statData.forEach(function (d) {
						d[yField] = +d[yField];
					});

					// Set the scale domain.
					x.domain(statData.map(function(d) {
						return d[xField];
					}));
					y.domain([0, d3.max(statData, function(d) {
						return d[yField];
					})]);

					svg.append('g').selectAll('.bar')
						.data(statData)
						.enter().append('rect')
						.attr('class', 'bar')
						.attr('x', function(d) {
							return x(d[xField]);
						})
						.attr('width', x.rangeBand())
						.attr('y', function() {
							return y(0);
						})
						.attr('height', 0)
						.transition().ease('cubic').delay(function(d,i){
							return i*10;
						})
						.attr('height', function(d) {
							return h - y(d[yField]);
						})
						.attr('y', function(d) {
							return y(d[yField]);
						});

					scope.buckets = d3.selectAll('rect.bar');
					/****************** Horizontal grid **********************/
					var hGrid = svg.append('g')
						.attr('class', 'grid')
						.call(d3.svg.axis()
							.scale(y)
							.orient('right')
							.tickSize(w, 0, 0)
							.tickFormat(d3.format(',d'))
							.ticks(10)
					);

					hGrid.selectAll('.tick text')
						.attr('y', -5)
						.attr('x',w/2)
						.attr('dy', '.15em')
						.style('text-anchor', 'middle');

					/********************* Y axis label *********************/
					svg.append('g')
						.append('text')
						.attr('x', -h/2)
						.attr('y', -2)
						.attr('transform', 'rotate(-90)')
						.style('text-anchor', 'middle')
						.text(labelTooltip);

					/************background Rect*********/
					var bgBar = svg.selectAll('g.bg-rect')
						.data(statData)
						.enter().append('g')
						.attr('transform', function (d) {
							return 'translate('+ (x(d[xField]) - 2) +', 0)';
						});

					bgBar.append('rect')
						.attr('width', x.rangeBand() + 4)
						.attr('height', h)
						.attr('class', 'bg-rect')
						.style('opacity', 0)
						.attr('z-index', 100)
						.on('mouseenter', function (d) {
							d3.select(this).style('opacity', 0.4);
							tip.show(d);
						})
						.on('mouseleave', function (d) {
							d3.select(this).style('opacity', 0);
							tip.hide(d);
						})
						.on('click', function (d) {
							scope.onClick()(d);
						});
					scope.finishedRendering = true;
				}

				function updateBarsLookFeel (){
					if(existingFilter){
						scope.buckets.transition()
							.delay(function(d,i){
								return i*10;
							})
							.style('opacity', function(d,i) {
								if(((d.data)[0] >= existingFilter[0] && (d.data)[1] <= existingFilter[1]) ||
									((d.data)[0] < existingFilter[1] && (d.data)[1] > existingFilter[0])
								){
									if(d3.select(scope.buckets[0][i]).style('opacity')==='.4'){
										return '1';
									}
								}
								else {
									return '.4';
								}
							});
					}
				}

				scope.$watch('visuData',
					function (statData) {
						element.empty();
						//because the tooltip is not a child of the vertical barchart element
						d3.selectAll('.vertical-barchart-cls.d3-tip').remove();
						if (statData) {
							scope.finishedRendering = false;
							clearTimeout(renderTimeout);
							renderTimeout = setTimeout(renderVBarchart.bind(this, statData), 100);
						}
					}
				);

				scope.$watch('existingFilter',
					function (newFilter) {
						if (newFilter) {
							var wait = 600;
							if(scope.finishedRendering){
								wait = 100;
							}
							clearTimeout(updateBarsTimeout);
							updateBarsTimeout = setTimeout(function(){
								existingFilter = newFilter;
								updateBarsLookFeel();
							}, wait);
						}
					}
				);
			}
		};
	}

	angular.module('talend.widget')
		.directive('verticalBarchart', VerticalBarchart);
})();