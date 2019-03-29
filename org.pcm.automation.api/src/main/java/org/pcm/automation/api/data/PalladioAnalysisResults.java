package org.pcm.automation.api.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.measure.Measure;
import javax.measure.quantity.Duration;

import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.metricspec.MetricDescription;
import org.pcm.automation.api.data.json.JsonAnalysisResults;
import org.pcm.automation.api.data.json.JsonTuple;

public class PalladioAnalysisResults {
	private Map<MeasuringPoint, MeasuringPointResults> results;

	public PalladioAnalysisResults() {
		this.results = new HashMap<>();
	}

	public Set<Entry<MeasuringPoint, MeasuringPointResults>> entries() {
		return results.entrySet();
	}

	public void addLongs(MeasuringPoint p, List<Measure<Long, Duration>> longs, MetricDescription metric) {
		init(p, metric);
		this.results.get(p).applyLongValues(longs);
	}

	public void addDoubles(MeasuringPoint p, List<Measure<Double, Duration>> doubles, MetricDescription metric) {
		init(p, metric);
		this.results.get(p).applyDoubleValues(doubles);
	}

	public Map<MeasuringPoint, MeasuringPointResults> getResults() {
		return results;
	}

	private void init(MeasuringPoint p, MetricDescription metric) {
		if (!this.results.containsKey(p)) {
			this.results.put(p, new MeasuringPointResults(metric));
		}
	}

	public JsonAnalysisResults toJSONResults() {
		JsonAnalysisResults results = new JsonAnalysisResults();
		this.results.entrySet().forEach(e -> {
			results.getServiceResults()
					.add(JsonTuple.of(e.getKey().getStringRepresentation(), e.getValue().toJSONFormat()));
		});
		return results;
	}

}
