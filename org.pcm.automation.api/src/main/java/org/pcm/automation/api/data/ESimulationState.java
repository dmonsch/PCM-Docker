package org.pcm.automation.api.data;

public enum ESimulationState {
	READY("READY"), RUNNING("RUNNING");
	
	private String string;
	
	private ESimulationState(String data) {
		this.string = data;
	}
	
	@Override
	public String toString() {
		return this.string;
	}
	
	public static ESimulationState fromString(String text) {
        for (ESimulationState b : ESimulationState.values()) {
            if (b.string.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }
}
