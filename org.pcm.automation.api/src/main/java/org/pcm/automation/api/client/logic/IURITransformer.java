package org.pcm.automation.api.client.logic;

import org.eclipse.emf.common.util.URI;

public interface IURITransformer {
	public URI transform(URI uri);

	public void installRule(URI uri);
}
