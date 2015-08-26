(function () {
    'use strict';

    /**
     * @ngdoc directive
     * @name talend.widget.directive:horizontalBarchart
     * @description This directive renders the horizontal bar chart.
     * @restrict E
     * @usage
     *     <horizontal-barchart id="barChart"
     *             width="250"
     *             height="400"
     *             on-click="columnProfileCtrl.barchartClickFn"
     *             visu-data="columnProfileCtrl.processedData"
     *             key-field="occurrences"
     *             value-field="data"
     *         ></horizontal-barchart>
     * */

    function HorizontalBarchart() {
        return {
            restrict: 'E',
            scope: {
                onClick: '&',
                visuData: '=',
                keyField: '@',
                valueField: '@'
            },
            link: function (scope, element, attrs) {
                var xField = scope.keyField;//occurences
                var yField = scope.valueField;
                var renderTimeout;
				var tip;

                function renderBarchart(statData) {
                    var container = attrs.id;
                    var width = +attrs.width;
                    var height = Math.ceil(((+attrs.height) / 15) * (statData.length + 1));

                    var m = [15, 10, 10, 10],
                        w = width - m[1] - m[3],
                        h = height - m[0] - m[2];

                    var x = d3.scale.linear().range([0, w]),
                        y = d3.scale.ordinal().rangeRoundBands([0, h], 0.18);

                    var xAxis = d3.svg.axis().scale(x).tickFormat(d3.format('d')).orient('top').tickSize(-h).ticks(Math.abs(x.range()[1] - x.range()[0]) / 50),
                        yAxis = d3.svg.axis().scale(y).orient('left').tickSize(0);

					tip = d3.tip()
						.attr('class', 'horizontal-barchart-cls d3-tip')
						.offset([-10, 0])
						.html(function(d) {
							return 	'<strong>Occurrences:</strong> <span style="color:yellow">' + d[xField] + '</span>'+
									'<br/>'+
									'<br/>'+
									'<strong>Record:</strong> <span style="color:yellow">'+ d[yField] + '</span>';
						});

                    var svg = d3.select('#' + container).append('svg')
                        .attr('class', 'horizontal-barchart-cls')
                        .attr('width', w + m[1] + m[3])
                        .attr('height', h + m[0] + m[2])
                        .append('g')
                        .attr('transform', 'translate(' + m[3] + ',' + m[0] + ')');

                    svg.call(tip);

                    // Parse numbers, and sort by value.
                    statData.forEach(function (d) {
                        d[xField] = +d[xField];
                    });

                    // Set the scale domain.
                    x.domain([0, d3.max(statData, function (d) {
                        return d[xField];
                    })]);
                    y.domain(statData.map(function (d) {
                        return d[yField];
                    }));

                    var bar = svg.selectAll('g.bar')
                        .data(statData)
                        .enter().append('g')
                        .attr('class', 'bar')
                        .attr('transform', function (d) {
                            return 'translate(0,' + y(d[yField]) + ')';
                        });

					bar.append('rect')
						.attr('width', x(0))
						.attr('height', y.rangeBand())
						.transition().delay(function (d,i){ return i * 30;})
						.attr('width', function(d) { return x(d[xField]);});

                    svg.append('g')
                        .attr('class', 'x axis')
                        .call(xAxis);

                    svg.append('g')
                        .attr('class', 'y axis')
                        .call(yAxis);

					bar.append('foreignObject')
						.attr('width', w)
						.attr('height', y.rangeBand())
						.append('xhtml:div')
						.attr('class', 'foreign-object-body')
						.html(function(d){
							return d[yField] ? d[yField]:'(EMPTY)';
						});

                    /************btgrect*********/
                    var bgBar = svg.selectAll('g.bg-rect')
                        .data(statData)
                        .enter().append('g')
                        .attr('transform', function (d) {
                            return 'translate(0,' + (y(d[yField]) - 2) + ')';
                        });

                    bgBar.append('rect')
                        .attr('width', w + 100)
                        .attr('height', y.rangeBand() + 4)
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
                }

                scope.$watch('visuData',
                    function (statData) {
                        element.empty();
						if(tip){
							tip.hide();
						}
                        if (statData) {
                            clearTimeout(renderTimeout);
                            renderTimeout = setTimeout(renderBarchart.bind(this, statData), 100);
                        }
                    });
            }
        };
    }

    angular.module('talend.widget')
        .directive('horizontalBarchart', HorizontalBarchart);
})();