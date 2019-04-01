package org.pcm.automation.api.data.json;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.palladiosimulator.pcm.seff.ServiceEffectSpecification;

public class JsonServiceResults {

	private Map<ResourceDemandingSEFF, Map<AssemblyContext, List<JsonMeasuringPointResults>>> serviceResults;

	public JsonServiceResults() {
		this.serviceResults = new HashMap<>();
	}

	public Set<ResourceDemandingSEFF> getTrackedSEFFs() {
		return serviceResults.keySet();
	}

	public List<JsonMeasuringPointResults> getResultsBySEFF(ServiceEffectSpecification spec) {
		if (!serviceResults.containsKey(spec))
			return null;

		return serviceResults.get(spec).entrySet().stream().map(entry -> entry.getValue())
				.flatMap(list -> list.stream()).collect(Collectors.toList());
	}

	public Map<ResourceDemandingSEFF, Map<AssemblyContext, List<JsonMeasuringPointResults>>> getServiceResults() {
		return serviceResults;
	}

}
