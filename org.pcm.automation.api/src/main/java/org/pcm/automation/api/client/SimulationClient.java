package org.pcm.automation.api.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.http.entity.ContentType;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.pcm.automation.api.client.logic.TransitiveModelTransformer;
import org.pcm.automation.api.client.util.EMFUtil;
import org.pcm.automation.api.data.ESimulationPart;
import org.pcm.automation.api.data.ESimulationState;
import org.pcm.automation.api.data.json.JsonAnalysisResults;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class SimulationClient {
	private static final String STATE_URL = "state/{id}";
	private static final String SET_URL = "set/{id}/";
	private static final String CLEAR_URL = "clear/{id}";
	private static final String SET_ADDITIONAL_URL = "set/{id}/additional";
	private static final String START_URL_BLOCKING = "start/{id}/blocking";

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private String baseUrl;
	private String id;

	// internal data holders
	private Repository inMemoryRepo;
	private System inMemorySystem;
	private ResourceEnvironment inMemoryEnv;
	private Allocation inMemoryAlloc;
	private UsageModel inMemoryUsagemodel;

	public SimulationClient(String baseUrl, String id) {
		this.baseUrl = baseUrl;
		this.id = id;
	}

	public JsonAnalysisResults startBlocking() {
		Unirest.setTimeouts(360000, 3600000);
		try {
			String result = Unirest.get(this.baseUrl + integrateId(START_URL_BLOCKING)).asString().getBody();
			return JSON_MAPPER.readValue(result, JsonAnalysisResults.class);
		} catch (UnirestException | IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			resetTimeouts();
		}
	}

	public ESimulationState getState() {
		try {
			return ESimulationState.fromString(Unirest.get(this.baseUrl + integrateId(STATE_URL)).asString().getBody());
		} catch (UnirestException e) {
			return null;
		}
	}

	public SimulationClient clear() {
		try {
			Unirest.get(this.baseUrl + integrateId(CLEAR_URL)).asString().getBody();
		} catch (UnirestException e) {
		}
		return this;
	}

	public SimulationClient setAllocation(Allocation allocation) {
		this.inMemoryAlloc = allocation;
		return this;
	}

	public SimulationClient setResourceEnvironment(ResourceEnvironment resenv) {
		this.inMemoryEnv = resenv;
		return this;
	}

	public SimulationClient setUsageModel(UsageModel usagemodel) {
		this.inMemoryUsagemodel = usagemodel;
		return this;
	}

	public SimulationClient setSystem(System system) {
		this.inMemorySystem = system;
		return this;
	}

	public SimulationClient setRepository(Repository repository) {
		this.inMemoryRepo = repository;
		return this;
	}

	public SimulationClient upload() {
		// create transformer
		TransitiveModelTransformer transformer = new TransitiveModelTransformer(inMemoryAlloc, inMemoryEnv,
				inMemoryRepo, inMemorySystem, inMemoryUsagemodel);

		// create uri transformer
		DefaultURITransformer uriTransformer = new DefaultURITransformer(inMemoryRepo, inMemorySystem, inMemoryAlloc,
				inMemoryEnv, inMemoryUsagemodel);

		// transform everything
		transformer.transformURIs(uriTransformer);

		// write them to the server
		setModel(inMemoryAlloc, ESimulationPart.ALLOCATION);
		setModel(inMemoryEnv, ESimulationPart.RESOURCE_ENVIRONMENT);
		setModel(inMemoryRepo, ESimulationPart.REPOSITORY);
		setModel(inMemorySystem, ESimulationPart.SYSTEM);
		setModel(inMemoryUsagemodel, ESimulationPart.USAGE_MODEL);

		// write additionals
		for (URI additional : transformer.getTransitives()) {
			EObject model = transformer.getModelByURI(additional);
			setAdditional(model, model.eResource().getURI().toFileString());
		}

		return this;
	}

	private String integrateId(String url) {
		return url.replaceAll("\\{id\\}", this.id);
	}

	private void resetTimeouts() {
		Unirest.setTimeouts(10000, 60000);
	}

	private boolean setAdditional(EObject obj, String fName) {
		try {
			File tempFile = File.createTempFile("pcm_repo", ".model");
			EMFUtil.saveToFile(obj, tempFile.getAbsolutePath());

			Unirest.post(this.baseUrl + integrateId(SET_ADDITIONAL_URL))
					.field("file", new FileInputStream(tempFile), ContentType.APPLICATION_OCTET_STREAM, fName)
					.asString().getBody();

			return true;
		} catch (IOException | UnirestException e) {
			return false;
		}
	}

	private <T extends EObject> boolean setModel(T obj, ESimulationPart part) {
		String orgFileName = obj.eResource().getURI().lastSegment();

		try {
			File tempFile = File.createTempFile("pcm_repo", ".model");
			EMFUtil.saveToFile(obj, tempFile.getAbsolutePath());

			Unirest.post(this.baseUrl + integrateId(SET_URL) + part.toString())
					.field("file", new FileInputStream(tempFile), ContentType.APPLICATION_OCTET_STREAM, orgFileName)
					.asString().getBody();
			tempFile.delete();

			return true;
		} catch (IOException | UnirestException e) {
			return false;
		}
	}

}
