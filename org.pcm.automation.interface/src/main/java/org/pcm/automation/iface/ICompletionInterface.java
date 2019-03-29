package org.pcm.automation.iface;

import org.pcm.automation.api.data.PalladioAnalysisResults;

@FunctionalInterface
public interface ICompletionInterface {
	public void finish(PalladioAnalysisResults results);
}
