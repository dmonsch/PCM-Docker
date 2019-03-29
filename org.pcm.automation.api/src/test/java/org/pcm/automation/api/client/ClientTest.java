package org.pcm.automation.api.client;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;
import org.palladiosimulator.experimentautomation.application.tooladapter.simucom.model.SimucomtooladapterPackage;
import org.palladiosimulator.experimentautomation.application.tooladapter.simulizar.model.SimulizartooladapterPackage;
import org.palladiosimulator.experimentautomation.experiments.ExperimentsPackage;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;
import org.palladiosimulator.pcmmeasuringpoint.PcmmeasuringpointPackage;
import org.pcm.automation.api.data.json.JsonAnalysisResults;
import org.pcm.automation.api.util.PcmUtils;

public class ClientTest {

	@org.junit.BeforeClass
	public static void init() {
		PcmUtils.loadPCMModels();

		ExperimentsPackage.eINSTANCE.eClass();
		SimucomtooladapterPackage.eINSTANCE.eClass();
		SimulizartooladapterPackage.eINSTANCE.eClass();
		MonitorRepositoryPackage.eINSTANCE.eClass();
		MeasuringpointPackage.eINSTANCE.eClass();
		MonitorRepositoryPackage.eINSTANCE.eClass();
		PcmmeasuringpointPackage.eINSTANCE.eClass();
	}

	@Test
	public void test() {
		PCMRestClient client = new PCMRestClient("127.0.0.1:8080/");
		assertTrue(client.isReachable(3000));

		client.clear();

		JsonAnalysisResults results = client.prepareSimulation().setRepository(CocomeExample.repo)
				.setAllocation(CocomeExample.allocation).setResourceEnvironment(CocomeExample.env)
				.setSystem(CocomeExample.sys).setUsageModel(CocomeExample.usage).upload().startBlocking();
		System.out.println(results.getServiceResults().size());
	}

}
