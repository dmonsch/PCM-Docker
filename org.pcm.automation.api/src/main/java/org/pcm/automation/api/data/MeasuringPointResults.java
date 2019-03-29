package org.pcm.automation.api.data;

import java.util.List;
import java.util.stream.Collectors;

import javax.measure.Measure;
import javax.measure.quantity.Duration;

import org.palladiosimulator.metricspec.MetricDescription;
import org.pcm.automation.api.data.json.JsonMeasuringPointResults;

public class MeasuringPointResults {
	private List<Measure<Double, Duration>> xValues;
	private List<Measure<Double, Duration>> yValues;

	private MetricDescription metricDescription;

	public MeasuringPointResults(MetricDescription desc) {
		this.xValues = null;
		this.yValues = null;

		this.metricDescription = desc;
	}

	public void applyLongValues(List<Measure<Long, Duration>> values) {
		this.applyDoubleValues(values.parallelStream().map(ms -> Measure.valueOf((double) ms.getValue(), ms.getUnit()))
				.collect(Collectors.toList()));
	}

	public void applyDoubleValues(List<Measure<Double, Duration>> values) {
		if (yValues == null) {
			yValues = values;
		} else {
			xValues = yValues;
			yValues = values;
		}
	}

	public List<Measure<Double, Duration>> getXValues() {
		return xValues;
	}

	public List<Measure<Double, Duration>> getYValues() {
		return yValues;
	}

	public MetricDescription getMetricDescription() {
		return metricDescription;
	}

	public JsonMeasuringPointResults toJSONFormat() {
		JsonMeasuringPointResults conv = new JsonMeasuringPointResults();

		conv.setMetricDescription(metricDescription.getTextualDescription());
		conv.setMetricId(metricDescription.getId());
		conv.setMetricName(metricDescription.getName());

		conv.setxValues(xValues.stream().map(x -> x.getValue()).collect(Collectors.toList()));
		conv.setyValues(yValues.stream().map(y -> y.getValue()).collect(Collectors.toList()));

		return conv;
	}

}
