package org.pcm.automation.iface;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.palladiosimulator.experimentautomation.experiments.ExperimentRepository;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;
import org.palladiosimulator.servicelevelobjective.ServicelevelObjectiveFactory;
import org.pcm.automation.api.data.ESimulationPart;
import org.pcm.automation.api.data.ESimulationState;
import org.pcm.automation.api.data.PalladioAnalysisResults;
import org.pcm.automation.api.util.PcmUtils;
import org.pcm.automation.core.ExperimentBuilder;
import org.pcm.automation.core.HeadlessExecutor;
import org.springframework.web.multipart.MultipartFile;

public class PCMSimulationState implements ICompletionInterface {

	private Map<ESimulationPart, File> simulationPartMapping;

	private Repository inMemoryRepository;
	private System inMemorySystem;
	private Allocation inMemoryAllocation;
	private ResourceEnvironment inMemoryEnv;
	private UsageModel inMemoryUsage;
	private ServiceLevelObjectiveRepository inMemorySlos;

	private ExperimentRepository currentConfiguration;
	private ESimulationState state;

	private String id;
	private File parentFolder;

	private Thread currentThread;
	private List<ICompletionInterface> listeners;

	private PalladioAnalysisResults analysisResults;

	public PCMSimulationState(String id, File parentFolder) {
		this.simulationPartMapping = new HashMap<>();
		this.state = ESimulationState.READY;
		this.id = id;
		this.parentFolder = parentFolder;
		this.listeners = new ArrayList<>();
		this.listeners.add(this);
	}

	// METHODS WITH LOGIC
	@Override
	public void finish(PalladioAnalysisResults results) {
		this.analysisResults = results;
	}

	public File addFile(MultipartFile file) {
		File targetLocationFile = new File(this.parentFolder, file.getOriginalFilename());
		Path targetLocation = targetLocationFile.toPath();

		targetLocationFile.getParentFile().mkdirs();

		try {
			Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return targetLocationFile;
	}

	public void waitForFinish() {
		try {
			currentThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void startSimulation(HeadlessExecutor executor) {
		if (this.state == ESimulationState.READY) {
			this.syncExperimentConfiguration(); // sync it

			this.state = ESimulationState.RUNNING;
			currentThread = new Thread(() -> {
				try {
					PalladioAnalysisResults results = executor.run(currentConfiguration);
					this.listeners.forEach(list -> {
						list.finish(results);
					});
				} catch (IOException e) {
					// TODO i dont know if we see this
					e.printStackTrace();
				} finally {
					this.state = ESimulationState.READY;
				}
			});
			currentThread.start();
		}
	}

	public boolean clear() {
		try {
			FileUtils.deleteDirectory(parentFolder);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public void putMapping(ESimulationPart part, File model) {
		this.simulationPartMapping.put(part, model);
	}

	// GETTERS & SETTERS
	public Map<ESimulationPart, File> getSimulationPartMapping() {
		return simulationPartMapping;
	}

	public ESimulationState getState() {
		return state;
	}

	public void setState(ESimulationState state) {
		this.state = state;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<ICompletionInterface> getListeners() {
		return listeners;
	}

	public void setListeners(List<ICompletionInterface> listeners) {
		this.listeners = listeners;
	}

	public PalladioAnalysisResults getAnalysisResults() {
		return analysisResults;
	}

	// PRIVATES WITH LOGIC
	private void syncExperimentConfiguration() {
		createSloRepositoryIfNotExists();

		File repositoryFile = simulationPartMapping.get(ESimulationPart.REPOSITORY);
		File allocationFile = simulationPartMapping.get(ESimulationPart.ALLOCATION);
		File environmentFile = simulationPartMapping.get(ESimulationPart.RESOURCE_ENVIRONMENT);
		File usageModelFile = simulationPartMapping.get(ESimulationPart.USAGE_MODEL);
		File systemFile = simulationPartMapping.get(ESimulationPart.SYSTEM);
		File sloFile = simulationPartMapping.get(ESimulationPart.SERVICE_LEVEL_OBJECTIVES);

		if (repositoryFile == null || allocationFile == null || environmentFile == null || usageModelFile == null
				|| systemFile == null) {
			return;
		}

		if (sloFile == null) {
			sloFile = createSloRepositoryIfNotExists();
		}

		this.inMemoryAllocation = PcmUtils.readFromFile(allocationFile.getAbsolutePath(), Allocation.class);
		this.inMemoryEnv = PcmUtils.readFromFile(environmentFile.getAbsolutePath(), ResourceEnvironment.class);
		this.inMemoryRepository = PcmUtils.readFromFile(repositoryFile.getAbsolutePath(), Repository.class);
		this.inMemorySystem = PcmUtils.readFromFile(systemFile.getAbsolutePath(), System.class);
		this.inMemoryUsage = PcmUtils.readFromFile(usageModelFile.getAbsolutePath(), UsageModel.class);

		this.inMemorySlos = PcmUtils.readFromFile(sloFile.getAbsolutePath(), ServiceLevelObjectiveRepository.class);

		// @formatter:off
		this.currentConfiguration = 
			ExperimentBuilder.create()
				.experiment()
					.name("EXP")
					.desc("PCM Docker")
					
					.allocation(inMemoryAllocation)
					.env(inMemoryEnv)
					.repository(inMemoryRepository)
					.usagemodel(inMemoryUsage)
					.system(inMemorySystem)
					
					.simucom(100)
					.reps(3)
					.measurementtime(360000)
					
					.slos(inMemorySlos)
				.finish()
			.build();
		// @formatter:on
	}

	private File createSloRepositoryIfNotExists() {
		File tempPath = new File(parentFolder, "empty.slo");
		ServiceLevelObjectiveRepository repo = ServicelevelObjectiveFactory.eINSTANCE
				.createServiceLevelObjectiveRepository();
		PcmUtils.saveToFile(repo, tempPath.getAbsolutePath());
		return tempPath;
	}

}
