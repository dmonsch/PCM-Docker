package org.pcm.automation.api.client.util;

import java.io.IOException;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

public class EMFUtil {

	/**
	 * Reads a Model from file with a given class
	 * 
	 * @param path  file path
	 * @param clazz model type class
	 * @return parsed model
	 */
	public static <T> T readFromFile(String path, Class<T> clazz) {
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());

		URI filePathUri = org.eclipse.emf.common.util.URI.createFileURI(path);

		Resource resource = resourceSet.getResource(filePathUri, true);
		return clazz.cast(resource.getContents().get(0));
	}

	/**
	 * Saves a model to file
	 * 
	 * @param model model to save
	 * @param path  path for the file
	 */
	public static <T extends EObject> void saveToFile(T model, String path) {
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

}
