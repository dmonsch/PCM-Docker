package org.pcm.automation.api.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;

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
import org.pcm.automation.api.data.json.ESimulatorType;
import org.pcm.automation.api.data.json.JsonAnalysisResults;
import org.pcm.automation.api.data.json.JsonSimulationConfiguration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

public class SimulationClient {
	private static final String STATE_URL = "state/{id}";
	private static final String SET_URL = "set/{id}/";
	private static final String CLEAR_URL = "clear/{id}";
	private static final String SET_ADDITIONAL_URL = "set/{id}/additional";
	private static final String SET_CONFIG_URL = "set/{id}/config";
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

	private JsonSimulationConfiguration currentConfiguration;

	private ObjectMapper objectMapper;

	public SimulationClient(String baseUrl, String id) {
		this.baseUrl = baseUrl;
		this.id = id;

		this.currentConfiguration = new JsonSimulationConfiguration();
		this.objectMapper = new ObjectMapper();
		objectMapper.registerModule(new Jdk8Module());
	}

	public void startAsync(ISimulationResultListener<JsonAnalysisResults> callback) {
		Unirest.get(this.baseUrl + integrateId(START_URL_BLOCKING)).asStringAsync(new Callback<String>() {
			@Override
			public void completed(HttpResponse<String> response) {
				try {
					JsonAnalysisResults res = JSON_MAPPER.readValue(response.getBody(), JsonAnalysisResults.class);
					callback.onResult(res);
				} catch (IOException e) {
					callback.onResult(null);
				}
			}

			@Override
			public void failed(UnirestException e) {
				callback.onResult(null);
			}

			@Override
			public void cancelled() {
				callback.onResult(null);
			}
		});
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

	public SimulationClient setSimulator(ESimulatorType type) {
		this.currentConfiguration.setSimulator(Optional.of(type));
		return this;
	}

	public SimulationClient setMeasurements(int measurements) {
		this.currentConfiguration.setMeasurements(Optional.of(measurements));
		return this;
	}

	public SimulationClient setRepetitions(int repetitions) {
		this.currentConfiguration.setRepetitions(Optional.of(repetitions));
		return this;
	}

	public SimulationClient setMeasurementTime(int time) {
		this.currentConfiguration.setTime(Optional.of(time));
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
		// write configuration
		uploadConfiguration();

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

	private void uploadConfiguration() {
		try {
			Unirest.post(this.baseUrl + integrateId(SET_CONFIG_URL))
					.field("configJson", objectMapper.writeValueAsString(currentConfiguration)).asString().getBody();
		} catch (JsonProcessingException | UnirestException e) {
			e.printStackTrace();
		}
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
