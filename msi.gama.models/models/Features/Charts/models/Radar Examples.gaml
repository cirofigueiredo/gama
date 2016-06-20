/**
* Name: Radar Examples
* Author: Philippe Caillou
* Description: A demonstration of charts composed of radars
* Tags: gui, chart
*/
model radars


global
{
}

experiment "Different radars" type: gui
{
	float minimum_cycle_duration <- 0.2;
	output
	{
		display "nice_radar_chart"
		{
			chart "Nice Cumulative Radar Chart" type: radar background: # darkblue color: # lightgreen axes: # lightgreen title_font: 'Serif' title_font_size: 32.0 title_font_style:
			'italic' tick_font: 'Monospaced' tick_font_size: 14 tick_font_style: 'bold' label_font: 'Arial' label_font_size: 18 label_font_style: 'bold' legend_font: 'SanSerif'
			legend_font_size: 14 legend_font_style: 'bold'
			{
				data "BCC" value: 10 * cos(100 * cycle) accumulate_values: true color: # yellow;
				data "ABC" value: 10 * sin(100 * cycle) accumulate_values: true color: # lightgreen;
				data "BCD" value: (cycle mod 10) accumulate_values: true color: # red;
			}

		}

		display "bounded_cumulative_radar_chart" type: java2D
		{
			chart "bounded_cumulative_bar_chart" type: radar x_serie_labels: ("cycle" + cycle) x_range: 10
			{
				data "BCC" value: cos(100 * cycle) * cycle * cycle accumulate_values: true color: # yellow;
				data "ABC" value: cycle * cycle accumulate_values: true color: # blue;
				data "BCD" value: cycle + 1 accumulate_values: true marker_shape: marker_circle;
			}

		}

		display "data_non_cumulative_bar_chart" type: java2D
		{
			chart "data_non_cumulative_bar_chart" type: radar x_serie_labels: ["axeCos", "axeSin", "axeCosSin"] series_label_position: xaxis
			{
				data "Cycle" value: [1 + cos(cycle), 1 + sin(1 * cycle), 1 + cos(1 * cycle) * sin(cycle)] color: # yellow;
				data "2Cycle" value: [1 + cos(1 * cycle * 2), 1 + sin(1 * cycle * 2), 1 + cos(1 * cycle * 2) * sin(1 * cycle * 2)] color: # blue;
				data "5Cycle" value: [1 + cos(1 * cycle * 5), 1 + sin(1 * cycle * 5), 1 + cos(1 * cycle * 5) * sin(1 * cycle * 5)] color: # red;
			}

		}

		display "datalist_radar_chart" type: java2D
		{
			chart "datalist_bar" type: radar series_label_position: onchart
			{
				datalist legend: ["A", "B", "C"] accumulate_values: true value: [1 + sin(cycle), 1 + cos(100 * cycle), 1 + cos(100 * (cycle + 30))] color: [# green, # black, # purple];
			}

		}

		display "onvalue_cumulative_bar_chart" type: java2D
		{
			chart "onvalue_cumulative_bar_chart" type: radar series_label_position: yaxis x_label: "my_time_label"
			{
				data "unique data value" value: cos(cycle * 10) accumulate_values: true color: # red;
			}

		}

		display "data_cumulative_style_chart" type: java2D
		{
			chart "Style Cumulative chart" type: radar style: stack
			{
				data "Step" value: cos(100 * cycle + 40) accumulate_values: true color: # blue;
				data "Bar" value: cos(100 * cycle + 60) accumulate_values: true color: # green;
				data "Line" value: cos(100 * cycle) accumulate_values: true color: # orange;
				data "Dot" value: cos(100 * cycle) * 0.3 accumulate_values: true color: # red;
			}

		}

	}

}