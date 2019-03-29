package org.pcm.automation.api.data.json;

import java.util.ArrayList;
import java.util.List;

public class JsonAnalysisResults {

	private List<JsonTuple<String, JsonMeasuringPointResults>> serviceResults;

	public JsonAnalysisResults() {
		this.serviceResults = new ArrayList<>();
	}

	public List<JsonTuple<String, JsonMeasuringPointResults>> getServiceResults() {
		return serviceResults;
	}

	public void setServiceResults(List<JsonTuple<String, JsonMeasuringPointResults>> serviceResults) {
		this.serviceResults = serviceResults;
	}

}
