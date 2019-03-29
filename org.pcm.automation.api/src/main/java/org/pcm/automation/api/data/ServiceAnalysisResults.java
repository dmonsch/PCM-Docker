package org.pcm.automation.api.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.palladiosimulator.pcm.seff.ServiceEffectSpecification;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.pcm.automation.api.util.MetricType;
import org.pcm.automation.api.util.PalladioAutomationUtil;

public class ServiceAnalysisResults {

	private Map<ResourceDemandingSEFF, Map<AssemblyContext, List<MeasuringPointResults>>> serviceResults;

	public static ServiceAnalysisResults buildFromAnalysisResults(Repository repo, System system, UsageModel usage,
			PalladioAnalysisResults results) {
		ServiceAnalysisResults build = new ServiceAnalysisResults();

		results.entries().forEach(res -> {
			if (PalladioAutomationUtil
					.getMetricType(res.getValue().getMetricDescription()) == MetricType.RESPONSE_TIME) {
				Pair<ResourceDemandingSEFF, AssemblyContext> mappedSeff = PalladioAutomationUtil
						.getSeffByMeasuringPoint(repo, usage, system, res.getKey(),
								res.getValue().getMetricDescription());

				if (mappedSeff != null) {
					if (!build.serviceResults.containsKey(mappedSeff.getLeft())) {
						build.serviceResults.put(mappedSeff.getLeft(), new HashMap<>());
					}

					Map<AssemblyContext, List<MeasuringPointResults>> innerResults = build.serviceResults
							.get(mappedSeff.getLeft());
					if (!innerResults.containsKey(mappedSeff.getRight())) {
						innerResults.put(mappedSeff.getRight(), new ArrayList<>());
					}

					innerResults.get(mappedSeff.getRight()).add(res.getValue());
				}
			}
		});

		return build;
	}

	public ServiceAnalysisResults() {
		this.serviceResults = new HashMap<>();
	}

	public Set<ResourceDemandingSEFF> getTrackedSEFFs() {
		return serviceResults.keySet();
	}

	public List<MeasuringPointResults> getResultsBySEFF(ServiceEffectSpecification spec) {
		if (!serviceResults.containsKey(spec))
			return null;

		return serviceResults.get(spec).entrySet().stream().map(entry -> entry.getValue())
				.flatMap(list -> list.stream()).collect(Collectors.toList());
	}

	public Map<ResourceDemandingSEFF, Map<AssemblyContext, List<MeasuringPointResults>>> getServiceResults() {
		return serviceResults;
	}

}
