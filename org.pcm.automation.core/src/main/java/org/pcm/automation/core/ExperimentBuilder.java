package org.pcm.automation.core;

import org.palladiosimulator.experimentautomation.abstractsimulation.AbstractsimulationFactory;
import org.palladiosimulator.experimentautomation.abstractsimulation.MeasurementCountStopCondition;
import org.palladiosimulator.experimentautomation.abstractsimulation.SimTimeStopCondition;
import org.palladiosimulator.experimentautomation.application.tooladapter.simucom.model.SimuComConfiguration;
import org.palladiosimulator.experimentautomation.application.tooladapter.simucom.model.SimucomtooladapterFactory;
import org.palladiosimulator.experimentautomation.application.tooladapter.simulizar.model.SimuLizarConfiguration;
import org.palladiosimulator.experimentautomation.application.tooladapter.simulizar.model.SimulizartooladapterFactory;
import org.palladiosimulator.experimentautomation.experiments.Experiment;
import org.palladiosimulator.experimentautomation.experiments.ExperimentRepository;
import org.palladiosimulator.experimentautomation.experiments.ExperimentsFactory;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;

public class ExperimentBuilder {

	public static ExperimentBuilder.BuilderRepo create() {
		return new BuilderRepo();
	}

	public static class BuilderRepo {
		private ExperimentRepository repo;

		public BuilderRepo() {
			this.repo = ExperimentsFactory.eINSTANCE.createExperimentRepository();
		}

		public BuilderExperiment experiment() {
			return new BuilderExperiment(this);
		}

		public ExperimentRepository build() {
			return repo;
		}

		protected void add(Experiment exp) {
			this.repo.getExperiments().add(exp);
		}
	}

	public static class BuilderExperiment {
		private Experiment experiment;
		private BuilderRepo enclosing;

		public BuilderExperiment(BuilderRepo enclosing) {
			this.experiment = ExperimentsFactory.eINSTANCE.createExperiment();
			this.enclosing = enclosing;

			this.experiment.setExperimentDesign(ExperimentsFactory.eINSTANCE.createFullFactorialDesign());
			this.experiment.setResponseMeasurement(ExperimentsFactory.eINSTANCE.createProfilingMeasurement());

			this.experiment.setInitialModel(ExperimentsFactory.eINSTANCE.createInitialModel());
		}

		public BuilderExperiment simulizar(int measurements) {
			SimuLizarConfiguration config = SimulizartooladapterFactory.eINSTANCE.createSimuLizarConfiguration();

			MeasurementCountStopCondition cond = AbstractsimulationFactory.eINSTANCE
					.createMeasurementCountStopCondition();
			cond.setMeasurementCount(measurements);
			config.getStopConditions().add(cond);

			this.experiment.getToolConfiguration().add(config);

			return this;
		}

		public BuilderExperiment simucom(int measurements) {
			SimuComConfiguration config = SimucomtooladapterFactory.eINSTANCE.createSimuComConfiguration();

			MeasurementCountStopCondition cond = AbstractsimulationFactory.eINSTANCE
					.createMeasurementCountStopCondition();
			cond.setMeasurementCount(measurements);
			config.getStopConditions().add(cond);

			this.experiment.getToolConfiguration().add(config);

			return this;
		}

		public BuilderExperiment measurements(int measures) {
			MeasurementCountStopCondition cond = AbstractsimulationFactory.eINSTANCE
					.createMeasurementCountStopCondition();
			cond.setMeasurementCount(measures);
			this.experiment.getStopConditions().add(cond);
			return this;
		}

		public BuilderExperiment monitorrepository(MonitorRepository mrepo) {
			this.experiment.getInitialModel().setMonitorRepository(mrepo);
			return this;
		}

		public BuilderExperiment measurementtime(int time) {
			SimTimeStopCondition stopCond = AbstractsimulationFactory.eINSTANCE.createSimTimeStopCondition();
			stopCond.setSimulationTime(time);
			this.experiment.getStopConditions().add(stopCond);
			return this;
		}

		public BuilderExperiment desc(String desc) {
			experiment.setDescription(desc);
			return this;
		}

		public BuilderExperiment name(String name) {
			experiment.setName(name);
			return this;
		}

		public BuilderExperiment reps(int reps) {
			experiment.setRepetitions(reps);
			return this;
		}

		public BuilderExperiment usagemodel(UsageModel model) {
			experiment.getInitialModel().setUsageModel(model);
			return this;
		}

		public BuilderExperiment system(org.palladiosimulator.pcm.system.System system) {
			experiment.getInitialModel().setSystem(system);
			return this;
		}

		public BuilderExperiment env(ResourceEnvironment env) {
			experiment.getInitialModel().setResourceEnvironment(env);
			return this;
		}

		public BuilderExperiment allocation(Allocation alloc) {
			experiment.getInitialModel().setAllocation(alloc);
			return this;
		}

		public BuilderExperiment slos(ServiceLevelObjectiveRepository rep) {
			experiment.getInitialModel().setServiceLevelObjectives(rep);
			return this;
		}

		public BuilderExperiment repository(Repository repo) {
			experiment.getInitialModel().setRepository(repo);
			return this;
		}

		public BuilderRepo finish() {
			enclosing.add(experiment);
			return enclosing;
		}
	}
}
