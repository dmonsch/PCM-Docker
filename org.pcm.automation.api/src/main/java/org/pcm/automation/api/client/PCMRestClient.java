package org.pcm.automation.api.client;

import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;
import org.palladiosimulator.pcmmeasuringpoint.PcmmeasuringpointPackage;
import org.pcm.automation.api.util.PcmUtils;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

/**
 * Used to interact with the REST interface of the PCM Docker. It also corrects
 * internal model references.
 * 
 * @author David Monschei
 *
 */
public class PCMRestClient {
	private static final String PING_URL = "ping";
	private static final String CLEAR_URL = "clear";
	private static final String PREPARE_URL = "prepare";

	private String baseUrl;

	public static void initDependencies() {
		PcmUtils.loadPCMModels();

		MonitorRepositoryPackage.eINSTANCE.eClass();
		MeasuringpointPackage.eINSTANCE.eClass();
		MonitorRepositoryPackage.eINSTANCE.eClass();
		PcmmeasuringpointPackage.eINSTANCE.eClass();
	}

	public PCMRestClient(String baseUrl) {
		this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
		if (!this.baseUrl.startsWith("http://")) {
			this.baseUrl = "http://" + this.baseUrl;
		}
	}

	/**
	 * Makes a full clean, this means all analysis results and states are removed.
	 * Be aware: this can break currently running simulations.
	 */
	public void clear() {
		try {
			Unirest.get(this.baseUrl + CLEAR_URL).asString().getBody();
		} catch (UnirestException e) {
		}
	}

	/**
	 * Determines whether the backend is reachable.
	 * 
	 * @param timeout the time to wait for a response
	 * @return true if the backend is reachable, false if not
	 */
	public boolean isReachable(long timeout) {
		Unirest.setTimeouts(timeout, timeout);
		boolean reach;
		try {
			reach = Unirest.get(this.baseUrl + PING_URL).asString().getBody().equals("true");
		} catch (UnirestException e) {
			reach = false;
		}
		resetTimeouts();
		return reach;
	}

	/**
	 * Prepares a simulation and returns a client to configure and start it.
	 * 
	 * @return instance of {@link SimulationClient} which can be used to execute a
	 *         simulation.
	 */
	public SimulationClient prepareSimulation() {
		try {
			return new SimulationClient(this.baseUrl, Unirest.get(this.baseUrl + PREPARE_URL).asString().getBody());
		} catch (UnirestException e) {
			return null;
		}
	}

	private void resetTimeouts() {
		Unirest.setTimeouts(10000, 60000);
	}

}
