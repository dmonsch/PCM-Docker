package org.pcm.automation.api.client;

import org.pcm.automation.api.data.json.JsonAnalysisResults;

@FunctionalInterface
public interface ISimulationResultListener<T> {
	public void onResult(JsonAnalysisResults results);
}
