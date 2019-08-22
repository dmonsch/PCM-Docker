package org.pcm.automation.core;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.unit.Unit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.tools.ant.types.Commandline;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.palladiosimulator.edp2.models.ExperimentData.DataSeries;
import org.palladiosimulator.edp2.models.ExperimentData.DoubleBinaryMeasurements;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentGroup;
import org.palladiosimulator.edp2.models.ExperimentData.LongBinaryMeasurements;
import org.palladiosimulator.edp2.models.ExperimentData.Measurement;
import org.palladiosimulator.edp2.models.ExperimentData.MeasurementRange;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;
import org.palladiosimulator.experimentautomation.abstractsimulation.AbstractsimulationFactory;
import org.palladiosimulator.experimentautomation.abstractsimulation.FileDatasource;
import org.palladiosimulator.experimentautomation.application.tooladapter.simucom.model.SimuComConfiguration;
import org.palladiosimulator.experimentautomation.application.tooladapter.simulizar.model.SimuLizarConfiguration;
import org.palladiosimulator.experimentautomation.experiments.ExperimentRepository;
import org.palladiosimulator.pcmmeasuringpoint.PcmmeasuringpointPackage;
import org.pcm.automation.api.data.PalladioAnalysisResults;

public class HeadlessExecutor {

	private static final Unit<Duration> S_UNIT = javax.measure.unit.SI.SECOND;

	private static final String[] STATIC_ARGS = new String[] { SystemUtils.IS_OS_MAC ? "-XstartOnFirstThread" : "",
			"-Declipse.p2.data.area=@config.dir/p2", "-Declipse.pde.launch=true", "-Dfile.encoding=UTF-8" };

	// very static maybe exchange this with dynamic search of equinox
	private static final String EQUINOX_OFFSET = "plugins" + File.separator
			+ "org.eclipse.equinox.launcher_1.4.0.v20161219-1356.jar";
	private static final String EQUINOX_MAIN = "org.eclipse.equinox.launcher.Main";
	private static final String PCM_APPLICATION = "org.palladiosimulator.experimentautomation.application";

	// OS DEPENDENT STRING -> BUILD THIS AUTOMATICALLY (windows: -os win32 -ws win32
	// -arch x86_64 ..
	private static final String ECL_APP_STATIC_MACOS = "-os macosx -ws cocoa -arch x86_64 -nl de_DE -consoleLog -clean -noSplash";
	private static final String ECL_APP_STATIC_WINDOWS = "-os win32 -ws win32 -arch x86_64 -nl de_DE -consoleLog -clean -noSplash";
	private static final String ECL_APP_STATIC_LINUX = "-os linux -ws gtk -arch x86_64 -nl de_DE -consoleLog -clean -noSplash";

	private String javaPath;
	private String eclipsePath;
	private String eclAppStatic;

	public HeadlessExecutor(String javaPath, String eclipsePath) {
		this.javaPath = javaPath;
		this.eclipsePath = eclipsePath;

		if (SystemUtils.IS_OS_WINDOWS) {
			this.eclAppStatic = ECL_APP_STATIC_WINDOWS;
		} else if (SystemUtils.IS_OS_MAC) {
			this.eclAppStatic = ECL_APP_STATIC_MACOS;
		} else {
			this.eclAppStatic = ECL_APP_STATIC_LINUX;
		}
	}

	public PalladioAnalysisResults run(ExperimentRepository repository) throws IOException {
		// init pathmaps
		initPathmaps();

		// init results
		PalladioAnalysisResults results = null;

		// create experiment result folder
		Path directory = Files.createTempDirectory("result");
		modifyDatasources(repository, directory);

		// create experiment file
		File temp = File.createTempFile("exp", ".experiment");
		saveToFile(repository, temp.getAbsolutePath());

		// build command
		String fullCommand = buildCommand(temp);

		// execute command
		if (executeCommandBlocking(fullCommand)) {
			// parse results
			results = parseResults(directory);
		}

		// finally delete temp files
		temp.delete();
		FileUtils.deleteDirectory(directory.toFile());

		// return the results of the analysis
		return results;
	}

	private PalladioAnalysisResults parseResults(Path directory) {
		PalladioAnalysisResults results = new PalladioAnalysisResults();

		MeasuringpointPackage.eINSTANCE.eClass();
		PcmmeasuringpointPackage.eINSTANCE.eClass();

		File[] dict = directory.toFile().listFiles(file -> FilenameUtils.getExtension(file.getName()).equals("edp2"));
		if (dict.length == 1) {
			boolean repoOpened = true;

			ExperimentGroup group = readFromFile(dict[0].getAbsolutePath(), ExperimentGroup.class);
			if (repoOpened) {
				loadMeasurements(group.getExperimentSettings().get(0).getExperimentRuns().get(0).getMeasurement(),
						results, trimURI(directory));
			}
		}

		return results;
	}

	private void loadMeasurements(List<Measurement> measurements, PalladioAnalysisResults result, URI directoryURI) {
		for (Measurement measurement : measurements) {
			MeasuringPoint belongingPoint = measurement.getMeasuringType().getMeasuringPoint();
			for (MeasurementRange range : measurement.getMeasurementRanges()) {
				for (DataSeries series : range.getRawMeasurements().getDataSeries()) {
					if (series instanceof LongBinaryMeasurements) {
						result.addLongs(belongingPoint, getLongMeasures(directoryURI, series.getValuesUuid()),
								measurement.getMeasuringType().getMetric());
					} else if (series instanceof DoubleBinaryMeasurements) {
						result.addDoubles(belongingPoint, getDoubleMeasures(directoryURI, series.getValuesUuid()),
								measurement.getMeasuringType().getMetric());
					}
				}
			}
		}
	}

	private List<Measure<Double, Duration>> getDoubleMeasures(URI dir, String uuid) {
		URI nUri = dir.appendSegment(uuid + ".edp2bin");
		File measurementFile = new File(nUri.path());

		try {
			byte[] readBytes = FileUtils.readFileToByteArray(measurementFile);
			Double[] readDoubles = deserializeDouble(readBytes);
			return Stream.of(readDoubles).map(d -> (Measure<Double, Duration>) Measure.valueOf(d, S_UNIT))
					.collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return new ArrayList<>();
	}

	private List<Measure<Long, Duration>> getLongMeasures(URI dir, String uuid) {
		URI nUri = dir.appendSegment(uuid + ".edp2bin");
		File measurementFile = new File(nUri.path());

		try {
			byte[] readBytes = FileUtils.readFileToByteArray(measurementFile);
			Long[] readLongs = deserializeLong(readBytes);
			return Stream.of(readLongs).map(l -> (Measure<Long, Duration>) Measure.valueOf(l, S_UNIT))
					.collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return new ArrayList<>();
	}

	private URI trimURI(Path dict) {
		URI directoryURI = URI.createURI(dict.toUri().toString());
		if (directoryURI.hasTrailingPathSeparator()) {
			return directoryURI.trimSegments(1);
		}
		return directoryURI;
	}

	private void modifyDatasources(ExperimentRepository repository, Path directory) {
		// modify all data sources to file because we want to parse them
		FileDatasource ds = AbstractsimulationFactory.eINSTANCE.createFileDatasource();
		ds.setLocation(directory.toString() + File.separator);

		repository.eAllContents().forEachRemaining(obj -> {
			if (obj instanceof SimuLizarConfiguration) {
				SimuLizarConfiguration config = (SimuLizarConfiguration) obj;
				config.setDatasource(ds);
			} else if (obj instanceof SimuComConfiguration) {
				SimuComConfiguration config = (SimuComConfiguration) obj;
				config.setDatasource(ds);
			}
		});
	}

	private String buildCommand(File expFile) {
		// build parts of command
		String commandJava = javaPath + " " + String.join(" ", STATIC_ARGS);
		String commandEclipse = "-classpath \"" + eclipsePath + EQUINOX_OFFSET + "\" " + EQUINOX_MAIN;
		commandEclipse += " -application " + PCM_APPLICATION;

		String commandExperiments = eclAppStatic + expFile.getAbsolutePath();

		// full command
		return commandJava + " " + commandEclipse + " " + commandExperiments;
	}

	private boolean executeCommandBlocking(String command) {
		ProcessBuilder builder = new ProcessBuilder(splitCommand(command));
		builder.redirectOutput(Redirect.INHERIT);

		try {
			builder.start().waitFor();
			return true;
		} catch (InterruptedException | IOException e) {
			return false;
		}
	}

	private String[] splitCommand(String cmd) {
		return Commandline.translateCommandline(cmd);
	}

	private <T> T readFromFile(String path, Class<T> clazz) {
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());

		URI filePathUri = org.eclipse.emf.common.util.URI.createFileURI(path);

		Resource resource = resourceSet.getResource(filePathUri, true);
		return clazz.cast(resource.getContents().get(0));
	}

	private void saveToFile(EObject model, String path) {
		URI writeModelURI = URI.createFileURI(path);

		final Resource.Factory.Registry resourceRegistry = Resource.Factory.Registry.INSTANCE;
		final Map<String, Object> map = resourceRegistry.getExtensionToFactoryMap();
		map.put("*", new XMIResourceFactoryImpl());

		final ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.setResourceFactoryRegistry(resourceRegistry);

		final Resource resource = resourceSet.createResource(writeModelURI);
		resource.getContents().add(model);
		try {
			resource.save(null);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private void initPathmaps() {
		final String metricSpecModel = "models/commonMetrics.metricspec";
		final URL url = getClass().getClassLoader().getResource(metricSpecModel);
		if (url == null) {
			throw new RuntimeException("Error getting common metric definitions");
		}
		String urlString = url.toString();
		if (!urlString.endsWith(metricSpecModel)) {
			throw new RuntimeException("Error getting common metric definitions. Got: " + urlString);
		}
		urlString = urlString.substring(0, urlString.length() - metricSpecModel.length() - 1);
		final URI uri = URI.createURI(urlString);
		final URI target = uri.appendSegment("models").appendSegment("");
		URIConverter.URI_MAP.put(URI.createURI("pathmap://METRIC_SPEC_MODELS/"), target);

		final Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
		final Map<String, Object> m = reg.getExtensionToFactoryMap();
		m.put("metricspec", new XMIResourceFactoryImpl());
	}

	private Double[] deserializeDouble(byte[] bytes) {
		Double[] doubles = new Double[(int) (bytes.length / getElementLength())];
		int blockPos = 0;
		for (int j = 0; j < doubles.length; j++) {
			long l = 0;
			for (int i = 7; i >= 0; i--) {
				l = l << 8;
				l |= bytes[blockPos + i] < 0 ? 256 + bytes[blockPos + i] : bytes[blockPos + i];
			}
			blockPos += 8;
			doubles[j] = Double.longBitsToDouble(l);
		}
		return doubles;
	}

	private Long[] deserializeLong(byte[] bytes) {
		Long[] longs = new Long[(int) (bytes.length / getElementLength())];
		int blockPos = 0;
		for (int j = 0; j < longs.length; j++) {
			long l = 0;
			for (int i = 7; i >= 0; i--) {
				l = l << 8;
				l |= bytes[blockPos + i] < 0 ? 256 + bytes[blockPos + i] : bytes[blockPos + i];
			}
			blockPos += 8;
			longs[j] = l;
		}
		return longs;
	}

	private long getElementLength() {
		return 8;
	}

}
