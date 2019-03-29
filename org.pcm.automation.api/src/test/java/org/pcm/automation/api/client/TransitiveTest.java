package org.pcm.automation.api.client;

import org.junit.Test;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;
import org.palladiosimulator.experimentautomation.application.tooladapter.simucom.model.SimucomtooladapterPackage;
import org.palladiosimulator.experimentautomation.application.tooladapter.simulizar.model.SimulizartooladapterPackage;
import org.palladiosimulator.experimentautomation.experiments.ExperimentsPackage;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;
import org.palladiosimulator.pcmmeasuringpoint.PcmmeasuringpointPackage;
import org.pcm.automation.api.client.logic.TransitiveModelTransformer;
import org.pcm.automation.api.util.PcmUtils;

public class TransitiveTest {

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
		TransitiveModelTransformer modelT = new TransitiveModelTransformer(CocomeExample.allocation, CocomeExample.env,
				CocomeExample.repo, CocomeExample.sys, CocomeExample.usage);
		modelT.buildTransitiveClosure();
	}

}
