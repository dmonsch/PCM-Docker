package org.pcm.automation.api.client;

import org.eclipse.emf.common.util.URI;
import org.junit.Test;
import org.pcm.automation.api.client.logic.TransitiveModelTransformer;

public class TransitiveTest {

	@org.junit.BeforeClass
	public static void init() {
		PCMRestClient.initDependencies();
	}

	@Test
	public void test() {
		TransitiveModelTransformer modelT = new TransitiveModelTransformer(CocomeExample.allocation, CocomeExample.env,
				CocomeExample.repo, CocomeExample.sys, CocomeExample.usage);
		modelT.buildTransitiveClosure();
		modelT.transformURIs(new DefaultURITransformer(CocomeExample.repo, CocomeExample.sys, CocomeExample.allocation,
				CocomeExample.env, CocomeExample.usage));

		for (URI uri : modelT.getTransitives()) {
			if (modelT.getModelByURI(uri) == null) {
				System.out.println("Error here");
			}
		}
	}

}
