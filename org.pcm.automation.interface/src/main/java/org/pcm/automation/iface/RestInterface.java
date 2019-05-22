package org.pcm.automation.iface;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.pcm.automation.api.data.ESimulationPart;
import org.pcm.automation.api.data.PalladioAnalysisResults;
import org.pcm.automation.core.HeadlessExecutor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class RestInterface implements InitializingBean {
	// files & folders
	private static final File TEMP_FOLDER = new File("pcm/");

	// models
	private Map<String, PCMSimulationState> stateMapping;

	// executing
	private HeadlessExecutor executor;
	private ObjectMapper objectMapper;

	// properties
	@Value("${javaPath:java}")
	private String javaPath;

	@Value("${eclipsePath:eclipse/}")
	private String eclipsePath;

	@GetMapping("/prepare")
	public String prepareSimulation() {
		String generatedId = UUID.randomUUID().toString();
		File parentFolder = new File(TEMP_FOLDER, generatedId);
		parentFolder.mkdirs();
		stateMapping.put(generatedId, new PCMSimulationState(generatedId, parentFolder));
		return generatedId;
	}

	@GetMapping("/state/{id}")
	public String getSimulationState(@PathVariable String id) {
		if (stateMapping.containsKey(id)) {
			return stateMapping.get(id).getState().name();
		}
		return "null";
	}

	@GetMapping("/ping")
	public String reachable() {
		return "true";
	}

	@GetMapping("/clear")
	public synchronized String clear() {
		this.stateMapping.clear();
		try {
			FileUtils.deleteDirectory(TEMP_FOLDER);
			return "true";
		} catch (IOException e) {
			return "false";
		}
	}

	@GetMapping("/clear/{id}")
	public synchronized String clear(@PathVariable String id) {
		if (stateMapping.containsKey(id)) {
			String res = stateMapping.get(id).clear() ? "true" : "false";
			stateMapping.remove(id);
			return res;
		}
		return "false";
	}

	@GetMapping("/start/{id}/blocking")
	public synchronized String startBlocking(@PathVariable String id) {
		if (stateMapping.containsKey(id)) {
			PCMSimulationState state = stateMapping.get(id);

			state.startSimulation(this.executor);
			state.waitForFinish();
			PalladioAnalysisResults results = state.getAnalysisResults();
			try {
				return objectMapper.writeValueAsString(results.toJSONResults());
			} catch (JsonProcessingException e) {
				return "{}";
			}
		}
		return "{}";
	}

	@PostMapping("/set/{id}/additional")
	public synchronized void setAdditionalHook(@PathVariable String id, @RequestParam("file") MultipartFile file) {
		if (stateMapping.containsKey(id)) {
			stateMapping.get(id).addFile(file);
		}
	}

	@PostMapping("/set/{id}/{type}")
	public synchronized void setPart(@PathVariable String id, @PathVariable String type,
			@RequestParam("file") MultipartFile file) {
		if (stateMapping.containsKey(id)) {
			ESimulationPart part = ESimulationPart.fromString(type);
			PCMSimulationState state = stateMapping.get(id);

			if (part != null && file != null) {
				state.putMapping(part, state.addFile(file));
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.executor = new HeadlessExecutor(javaPath, eclipsePath);
		this.objectMapper = new ObjectMapper();
		this.stateMapping = new HashMap<>();
	}

}
