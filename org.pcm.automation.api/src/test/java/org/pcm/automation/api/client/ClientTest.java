package org.pcm.automation.api.client;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.pcm.automation.api.data.json.JsonAnalysisResults;
import org.pcm.automation.api.data.json.JsonServiceResults;
import org.pcm.automation.api.util.PalladioAutomationUtil;

public class ClientTest {

	@org.junit.BeforeClass
	public static void init() {
		PCMRestClient.initDependencies();
	}

	@Test
	public void test() {
		PCMRestClient client = new PCMRestClient("127.0.0.1:8080/");
		assertTrue(client.isReachable(3000));

		client.clear();

		// create session & pure json results
		SimulationClient simClient = client.prepareSimulation().setRepository(CocomeExample.repo)
				.setAllocation(CocomeExample.allocation).setResourceEnvironment(CocomeExample.env)
				.setSystem(CocomeExample.sys).setUsageModel(CocomeExample.usage).upload();
		JsonAnalysisResults results = simClient.startBlocking();
		simClient.clear();

		System.out.println(results.getServiceResults().size());

		// convert it in data about seffs
		JsonServiceResults serviceResults = PalladioAutomationUtil.getServiceAnalysisResults(CocomeExample.repo,
				CocomeExample.usage, CocomeExample.sys, results);
		System.out.println(serviceResults.getTrackedSEFFs().size());
	}

}
