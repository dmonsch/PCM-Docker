package org.pcm.automation.api.util;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.palladiosimulator.pcm.PcmPackage;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.composition.ProvidedDelegationConnector;
import org.palladiosimulator.pcm.core.entity.InterfaceProvidingEntity;
import org.palladiosimulator.pcm.repository.OperationProvidedRole;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.repository.RepositoryPackage;
import org.palladiosimulator.pcm.resourcetype.ResourcetypePackage;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.palladiosimulator.pcm.seff.ServiceEffectSpecification;
import org.palladiosimulator.pcm.system.System;

import de.uka.ipd.sdq.identifier.Identifier;

/**
 * PCM specific utility functions.
 * 
 * @author JP
 * @author David Monschein
 *
 */
public class PcmUtils {

	/**
	 * Gets all objects in a {@link Repository} of a specific type.
	 * 
	 * @param <T>      The type of the objects to find.
	 * @param pcmModel The repository which is searched.
	 * @param type     The type of the objects to find.
	 * @return A list of all found objects or an empty list.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends EObject> List<T> getObjects(final EObject pcmModel, final Class<T> type) {
		List<T> results = new ArrayList<>();
		TreeIterator<EObject> it = pcmModel.eAllContents();
		while (it.hasNext()) {
			EObject eo = it.next();
			if (type.isInstance(eo)) {
				results.add((T) eo);
			}
		}
		return results;
	}

	/**
	 * Resolves a SEFF with given System, Signature and Provided Role Searches the
	 * delegation and resolves the assembly to which the role is delegated
	 * 
	 * @param system system link
	 * @param sig    signature
	 * @param role   provided role
	 * @return pair (left = SEFF, right = providing assembly)
	 */
	public static Pair<ResourceDemandingSEFF, AssemblyContext> getSeffByProvidedRoleAndSignature(System system,
			OperationSignature sig, OperationProvidedRole role) {
		ProvidedDelegationConnector innerDelegator = getObjects(system, ProvidedDelegationConnector.class).stream()
				.filter(del -> {
					return del.getOuterProvidedRole_ProvidedDelegationConnector().getId().equals(role.getId());
				}).findFirst().orElse(null);

		if (innerDelegator != null) {
			InterfaceProvidingEntity innerEntity = innerDelegator.getInnerProvidedRole_ProvidedDelegationConnector()
					.getProvidingEntity_ProvidedRole();

			return Pair.of(getObjects(innerEntity, ResourceDemandingSEFF.class).stream()
					.filter(seff -> seff.getDescribedService__SEFF().getId().equals(sig.getId())).findFirst()
					.orElse(null), innerDelegator.getAssemblyContext_ProvidedDelegationConnector());
		}

		return null;
	}

	/**
	 * Gets all provided operations by a system.
	 * 
	 * @param system the system
	 * @return set of all provided operations by the passed system
	 */
	public static Set<OperationSignature> getProvidedOperations(System system) {
		return getObjects(system, OperationProvidedRole.class).stream()
				.map(role -> role.getProvidedInterface__OperationProvidedRole().getSignatures__OperationInterface())
				.flatMap(list -> list.stream()).collect(Collectors.toSet());
	}

	/**
	 * Gets an element with a given ID.
	 * 
	 * @param obj   the object which child's should be examined
	 * @param clazz type of the element
	 * @param id    id of the element
	 * @return found element or null if it could not be found
	 */
	public static <T extends Identifier> T getElementById(EObject obj, Class<T> clazz, String id) {
		return getObjects(obj, clazz).stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null);
	}

	/**
	 * Gets the provided role of a system which contains a given SEFF
	 * 
	 * @param sys  system
	 * @param seff SEFF
	 * @return provided role which contains the SEFF or null if not available
	 */
	public static OperationProvidedRole getProvidedRole(System sys, ServiceEffectSpecification seff) {
		String seffSigId = seff.getDescribedService__SEFF().getId();
		return getObjects(sys, OperationProvidedRole.class).stream().filter(role -> {
			return role.getProvidedInterface__OperationProvidedRole().getSignatures__OperationInterface().stream()
					.anyMatch(sig -> sig.getId().equals(seffSigId));
		}).findFirst().orElse(null);
	}

	/**
	 * Visits all common PCM package classes to load them.
	 */
	public static void loadPCMModels() {
		RepositoryPackage.eINSTANCE.eClass();
		PcmPackage.eINSTANCE.eClass();
		ResourcetypePackage.eINSTANCE.eClass();

		initPathmaps();
	}

	private static void initPathmaps() {
		final String metricSpecModel = "models/Palladio.resourcetype";
		final URL url = PcmUtils.class.getClassLoader().getResource(metricSpecModel);
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
		URIConverter.URI_MAP.put(URI.createURI("pathmap://PCM_MODELS/"), target);

		final Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
		final Map<String, Object> m = reg.getExtensionToFactoryMap();
		m.put("resourcetype", new XMIResourceFactoryImpl());
	}
}
