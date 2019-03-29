package org.pcm.automation.api.data.json;

import java.util.List;

public class JsonMeasuringPointResults {
	private List<Double> xValues;
	private List<Double> yValues;

	private String metricDescription;
	private String metricId;
	private String metricName;

	public List<Double> getxValues() {
		return xValues;
	}

	public void setxValues(List<Double> xValues) {
		this.xValues = xValues;
	}

	public List<Double> getyValues() {
		return yValues;
	}

	public void setyValues(List<Double> yValues) {
		this.yValues = yValues;
	}

	public String getMetricDescription() {
		return metricDescription;
	}

	public void setMetricDescription(String metricDescription) {
		this.metricDescription = metricDescription;
	}

	public String getMetricId() {
		return metricId;
	}

	public void setMetricId(String metricId) {
		this.metricId = metricId;
	}

	public String getMetricName() {
		return metricName;
	}

	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

}
