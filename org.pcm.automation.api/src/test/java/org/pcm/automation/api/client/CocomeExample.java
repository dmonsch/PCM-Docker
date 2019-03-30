package org.pcm.automation.api.client;

import java.io.File;

import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.pcm.automation.api.client.util.EMFUtil;

public class CocomeExample {
	public static UsageModel usage = EMFUtil.readFromFile(new File("cocome/cocome.usagemodel").getAbsolutePath(),
			UsageModel.class);
	public static Allocation allocation = EMFUtil.readFromFile(new File("cocome/cocome.allocation").getAbsolutePath(),
			Allocation.class);
	public static Repository repo = EMFUtil.readFromFile(new File("cocome/cocome.repository").getAbsolutePath(),
			Repository.class);
	public static org.palladiosimulator.pcm.system.System sys = EMFUtil.readFromFile(
			new File("cocome/cocome.system").getAbsolutePath(), org.palladiosimulator.pcm.system.System.class);
	public static ResourceEnvironment env = EMFUtil
			.readFromFile(new File("cocome/cocome.resourceenvironment").getAbsolutePath(), ResourceEnvironment.class);
}