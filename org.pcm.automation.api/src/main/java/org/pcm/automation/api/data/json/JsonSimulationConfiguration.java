package org.pcm.automation.api.data.json;

import java.util.Optional;

public class JsonSimulationConfiguration {

	private Optional<Integer> measurements = Optional.empty();
	private Optional<ESimulatorType> simulator = Optional.empty();
	private Optional<Integer> repetitions = Optional.empty();
	private Optional<Integer> time = Optional.empty();

	public Optional<Integer> getMeasurements() {
		return measurements;
	}

	public void setMeasurements(Optional<Integer> measurements) {
		this.measurements = measurements;
	}

	public Optional<ESimulatorType> getSimulator() {
		return simulator;
	}

	public void setSimulator(Optional<ESimulatorType> simulator) {
		this.simulator = simulator;
	}

	public Optional<Integer> getRepetitions() {
		return repetitions;
	}

	public void setRepetitions(Optional<Integer> repetitions) {
		this.repetitions = repetitions;
	}

	public Optional<Integer> getTime() {
		return time;
	}

	public void setTime(Optional<Integer> time) {
		this.time = time;
	}

}
