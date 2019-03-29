package org.pcm.automation.api.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.entity.ContentType;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.pcm.automation.api.data.ESimulationPart;
import org.pcm.automation.api.data.ESimulationState;
import org.pcm.automation.api.data.json.JsonAnalysisResults;
import org.pcm.automation.api.util.PcmUtils;

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

	// TODO generalize to transitive closure full
	public SimulationClient upload() {
		// transitive closure of models and send
		Set<URI> resourcesClosure = new HashSet<>();
		List<EObject> modelList = new ArrayList<>();

		// add models
		modelList.add(inMemoryAlloc);
		modelList.add(inMemoryEnv);
		modelList.add(inMemoryRepo);
		modelList.add(inMemorySystem);
		modelList.add(inMemoryUsagemodel);

		// add resources
		modelList.forEach(model -> resourcesClosure.add(model.eResource().getURI()));

		// build closure
		// TODO full closure
		modelList.forEach(model -> allCrossReferences(model).stream().forEach(ref -> {
			resourcesClosure.add(ref.getURI());
		}));

		// generate file names for each
		Map<URI, URI> fileNameMapping = new HashMap<>();
		// TODO create enum for file paths
		fileNameMapping.put(inMemoryAlloc.eResource().getURI(), URI.createFileURI("temp.allocation"));
		fileNameMapping.put(inMemoryEnv.eResource().getURI(), URI.createFileURI("temp.resourceenvironment"));
		fileNameMapping.put(inMemoryRepo.eResource().getURI(), URI.createFileURI("temp.repository"));
		fileNameMapping.put(inMemorySystem.eResource().getURI(), URI.createFileURI("temp.system"));
		fileNameMapping.put(inMemoryUsagemodel.eResource().getURI(), URI.createFileURI("temp.usagemodel"));

		// additional files
		Set<URI> remainingOnly = new HashSet<>();
		int currentFileId = 0;
		for (URI other : resourcesClosure) {
			if (!fileNameMapping.containsKey(other)) {
				remainingOnly.add(other);
				String additionalPath = "additional/model" + String.valueOf(currentFileId++) + ".model";
				fileNameMapping.put(other, URI.createFileURI(additionalPath));
			}
		}

		// modify links
		// TODO transitive
		for (EObject model : modelList) {
			// TODO code duplication
			if (model.eCrossReferences().size() > 0) {
				model.eCrossReferences().forEach(cr -> {
					if (cr != null && cr.eResource() != null && fileNameMapping.containsKey(cr.eResource().getURI())) {
						// replace
						cr.eResource().setURI(fileNameMapping.get(cr.eResource().getURI()));
					}
				});
			}

			model.eResource().setURI(fileNameMapping.get(model.eResource().getURI()));

			model.eAllContents().forEachRemaining(el -> {
				el.eCrossReferences().forEach(cr -> {
					if (cr != null && cr.eResource() != null && fileNameMapping.containsKey(cr.eResource().getURI())) {
						// replace
						cr.eResource().setURI(fileNameMapping.get(cr.eResource().getURI()));
					}
				});
			});
		}

		// write them to the server
		setModel(inMemoryAlloc, ESimulationPart.ALLOCATION);
		setModel(inMemoryEnv, ESimulationPart.RESOURCE_ENVIRONMENT);
		setModel(inMemoryRepo, ESimulationPart.REPOSITORY);
		setModel(inMemorySystem, ESimulationPart.SYSTEM);
		setModel(inMemoryUsagemodel, ESimulationPart.USAGE_MODEL);

		// write additionals
		for (URI resource : remainingOnly) {
			String fName = fileNameMapping.get(resource).lastSegment();
			setAdditional(resource, "additional/" + fName);
		}

		return this;
	}

	private String integrateId(String url) {
		return url.replaceAll("\\{id\\}", this.id);
	}

	private void resetTimeouts() {
		Unirest.setTimeouts(10000, 60000);
	}

	private List<Resource> allCrossReferences(EObject obj) {
		Set<Resource> references = new HashSet<>();
		obj.eAllContents().forEachRemaining(e -> {
			if (e.eCrossReferences().size() > 0) {
				e.eCrossReferences().forEach(cr -> {
					references.add(cr.eResource());
				});
			}
		});

		// remove own and nulls
		references.remove(null);
		references.remove(obj.eResource());

		// remove pathmaps
		return references.stream().filter(ref -> ref.getURI().toString().startsWith("file:"))
				.collect(Collectors.toList());
	}

	private boolean setAdditional(URI uri, String fName) {
		try {
			File tempFile = new File(uri.toFileString());

			Unirest.post(this.baseUrl + integrateId(SET_ADDITIONAL_URL))
					.field("file", new FileInputStream(tempFile), ContentType.APPLICATION_OCTET_STREAM, fName)
					.asString().getBody();

			return true;
		} catch (IOException | UnirestException e) {
			return false;
		}
	}

	private boolean setAdditional(EObject obj) {
		try {
			File tempFile = File.createTempFile("temp", ".model");
			PcmUtils.saveToFile(obj, tempFile.getAbsolutePath());

			Unirest.post(this.baseUrl + integrateId(SET_ADDITIONAL_URL)).field("file", new FileInputStream(tempFile),
					ContentType.APPLICATION_OCTET_STREAM, tempFile.getName()).asString().getBody();
			tempFile.delete();

			return true;
		} catch (IOException | UnirestException e) {
			return false;
		}
	}

	private <T extends EObject> boolean setModel(T obj, ESimulationPart part) {
		String orgFileName = obj.eResource().getURI().lastSegment();

		try {
			File tempFile = File.createTempFile("pcm_repo", ".model");
			PcmUtils.saveToFile(obj, tempFile.getAbsolutePath());

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
