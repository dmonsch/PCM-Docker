package org.pcm.automation.api.client.logic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;

public class TransitiveModelTransformer {
	// base
	private List<EObject> models;

	// transitive
	private List<EObject> transitiveClosure;

	public TransitiveModelTransformer(EObject... models) {
		this.models = Stream.of(models).collect(Collectors.toList());
	}

	public void transformURIs(IURITransformer transformer) {
		if (transitiveClosure == null) {
			this.buildTransitiveClosure();
		}

		for (EObject model : transitiveClosure) {
			// itself
			model.eResource().setURI(transformer.transform(model.eResource().getURI()));

			// all crosses
			allCrossReferences(model)
					.forEach(cr -> cr.eResource().setURI(transformer.transform(cr.eResource().getURI())));
		}
	}

	public void buildTransitiveClosure() {
		Set<URI> resourcesClosure = new HashSet<>();
		List<EObject> resourcesClosed = new ArrayList<>();

		// add all ex models
		models.forEach(m -> {
			resourcesClosed.add(m);
		});

		// first filter
		filterResources(resourcesClosed, resourcesClosure);

		// build transitive closure
		for (EObject model : models) {
			closeTransitive(resourcesClosed, resourcesClosure, model);
		}

		// set closure
		this.transitiveClosure = resourcesClosed;
	}

	private void closeTransitive(List<EObject> container, Set<URI> uriContainer, EObject search) {
		List<EObject> crossReferences = allCrossReferences(search);
		int sizeBefore = container.size();
		container.addAll(filterResources(crossReferences, uriContainer));
		int sizeAdded = container.size() - sizeBefore;

		if (sizeAdded > 0) {
			crossReferences.forEach(cf -> {
				closeTransitive(container, uriContainer, cf);
			});
		}
	}

	private List<EObject> filterResources(List<EObject> res, Set<URI> already) {
		List<EObject> result = new ArrayList<>();
		for (EObject r : res) {
			if (!already.contains(r.eResource().getURI())) {
				already.add(r.eResource().getURI());
				result.add(r);
			}
		}
		return result;
	}

	private List<EObject> allCrossReferences(EObject obj) {
		Set<EObject> references = new HashSet<>();

		// direct ones
		obj.eCrossReferences().forEach(cr -> {
			references.add(cr);
		});

		// of all child contents
		obj.eAllContents().forEachRemaining(e -> {
			if (e.eCrossReferences().size() > 0) {
				e.eCrossReferences().forEach(cr -> {
					references.add(cr);
				});
			}
		});

		// remove own and nulls
		references.remove(null);
		references.remove(obj);

		// remove pathmaps
		return references.stream()
				.filter(ref -> ref.eResource() != null && ref.eResource().getURI().toString().startsWith("file:"))
				.collect(Collectors.toList());
	}

}
