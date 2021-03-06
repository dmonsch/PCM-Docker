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
import org.pcm.automation.api.client.util.EMFUtil;
import org.pcm.automation.api.data.ESimulationPart;
import org.pcm.automation.api.data.ESimulationState;
import org.pcm.automation.api.data.PalladioAnalysisResults;
import org.pcm.automation.api.data.json.ESimulatorType;
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

	// DEFAULT SETTINGS
	private int measurements = 100;
	private ESimulatorType simulator = ESimulatorType.SIMUCOM;
	private int repetitions = 1;
	private int measurementTime = 20000;

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

	public void setMeasurements(int measurements) {
		this.measurements = measurements;
	}

	public void setSimulator(ESimulatorType simulator) {
		this.simulator = simulator;
	}

	public void setRepetitions(int repetitions) {
		this.repetitions = repetitions;
	}

	public void setMeasurementTime(int measurementTime) {
		this.measurementTime = measurementTime;
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

		this.inMemoryAllocation = EMFUtil.readFromFile(allocationFile.getAbsolutePath(), Allocation.class);
		this.inMemoryEnv = EMFUtil.readFromFile(environmentFile.getAbsolutePath(), ResourceEnvironment.class);
		this.inMemoryRepository = EMFUtil.readFromFile(repositoryFile.getAbsolutePath(), Repository.class);
		this.inMemorySystem = EMFUtil.readFromFile(systemFile.getAbsolutePath(), System.class);
		this.inMemoryUsage = EMFUtil.readFromFile(usageModelFile.getAbsolutePath(), UsageModel.class);

		this.inMemorySlos = EMFUtil.readFromFile(sloFile.getAbsolutePath(), ServiceLevelObjectiveRepository.class);

		// @formatter:off
		ExperimentBuilder.BuilderExperiment builder = 
			ExperimentBuilder.create()
				.experiment()
					.name("EXP")
					.desc("PCM Docker")
					
					.allocation(inMemoryAllocation)
					.env(inMemoryEnv)
					.repository(inMemoryRepository)
					.usagemodel(inMemoryUsage)
					.system(inMemorySystem)
					.reps(this.repetitions)
					.measurementtime(this.measurementTime)
					
					.slos(inMemorySlos);
		// @formatter:on

		if (this.simulator == ESimulatorType.SIMUCOM) {
			builder.simucom(this.measurements);
		} else if (this.simulator == ESimulatorType.SIMULIZAR) {
			builder.simulizar(this.measurements);
		}

		this.currentConfiguration = builder.finish().build();
	}

	private File createSloRepositoryIfNotExists() {
		File tempPath = new File(parentFolder, "empty.slo");
		ServiceLevelObjectiveRepository repo = ServicelevelObjectiveFactory.eINSTANCE
				.createServiceLevelObjectiveRepository();
		EMFUtil.saveToFile(repo, tempPath.getAbsolutePath());
		return tempPath;
	}

}
