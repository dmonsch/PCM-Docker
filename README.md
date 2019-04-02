# PCM-Docker
This repository provides a Docker image which exposes a simple REST interface. The REST interface can be used to trigger simulations of a Palladio Component Model (PCM) and to obtain the results of the simulations.

## Setup
The setup process is relatively easy, you only need to create a Docker container. The Dockerfile is located under [Dockerfile](https://github.com/dmonsch/PCM-Docker/tree/master/docker).
1. Build the Docker image with ``docker build -t pcm-docker .``
2. Start a container with the image ``docker run --name pcm -p 8080:8080 pcm-docker``
3. Use the REST interface with the API client explained below or read the documentation.

## Code Example

```java
PCMRestClient client = new PCMRestClient("127.0.0.1:8080/");
if (client.isReachable(3000)) {
	client.clear();
	
	// create session & pure json results
	SimulationClient simClient = client.prepareSimulation().setRepository(CocomeExample.repo)
			.setAllocation(CocomeExample.allocation).setResourceEnvironment(CocomeExample.env)
			.setSystem(CocomeExample.sys).setUsageModel(CocomeExample.usage).upload();
	JsonAnalysisResults results = simClient.startBlocking();
	simClient.clear();
	
	// do something with the results

	// convert it in data about seffs
	JsonServiceResults serviceResults = PalladioAutomationUtil.getServiceAnalysisResults(CocomeExample.repo,
		CocomeExample.usage, CocomeExample.sys, results);
	
	// do something with infos about SEFF response times
}
```

## Limitations
If you need a lightweight client or an client for different languages please consult the documentation of the exposed REST interface.
// TODO: REST documentation