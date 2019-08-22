package org.pcm.automation.api.data.json;

public enum ESimulatorType {
	SIMUCOM("simucom"), SIMULIZAR("simulizar");

	private String string;

	private ESimulatorType(String type) {
		this.string = type;
	}

	@Override
	public String toString() {
		return this.string;
	}

	public static ESimulatorType fromString(String text) {
		for (ESimulatorType b : ESimulatorType.values()) {
			if (b.string.equalsIgnoreCase(text)) {
				return b;
			}
		}
		return null;
	}
}
