package org.pcm.automation.api.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.metricspec.MetricDescription;
import org.palladiosimulator.pcm.core.composition.AssemblyConnector;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.composition.ComposedStructure;
import org.palladiosimulator.pcm.core.composition.Connector;
import org.palladiosimulator.pcm.core.composition.RequiredDelegationConnector;
import org.palladiosimulator.pcm.repository.OperationRequiredRole;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.seff.AbstractAction;
import org.palladiosimulator.pcm.seff.ExternalCallAction;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.pcm.usagemodel.EntryLevelSystemCall;
import org.palladiosimulator.pcm.usagemodel.UsageModel;

// TODO refactoring
public class PalladioAutomationUtil {
	private static final Pattern ASSEMBLY_CTX_PATTERN = Pattern.compile("AssemblyCtx: (.*),");
	private static final Pattern CALL_ID_PATTERN = Pattern.compile("CallID: (.*)>");
	private static final Pattern ENTRY_LEVEL_SYSTEM_PATTERN = Pattern.compile("EntryLevelSystemCall id: (.*) ");

	public static Pair<ResourceDemandingSEFF, AssemblyContext> getSeffByAssemblySignature(AssemblyContext ctx,
			OperationRequiredRole reqRole, OperationSignature sig) {
		AssemblyContext providing = getContextProvidingRole(ctx, reqRole);
		if (providing != null) {
			return Pair.of(PcmUtils
					.getObjects(providing.getEncapsulatedComponent__AssemblyContext(), ResourceDemandingSEFF.class)
					.stream().filter(seff -> {
						return seff.getDescribedService__SEFF().getId().equals(sig.getId());
					}).findFirst().orElse(null), providing);
		}
		return null;
	}

	// TODO check the logic here don't know if its fully correct (see inner todo)
	public static AssemblyContext getContextProvidingRole(AssemblyContext ctx, OperationRequiredRole role) {
		ComposedStructure parentStructure = ctx.getParentStructure__AssemblyContext();
		for (Connector connector : parentStructure.getConnectors__ComposedStructure()) {
			if (connector instanceof AssemblyConnector) {
				AssemblyConnector assConnector = (AssemblyConnector) connector;
				if (assConnector.getRequiringAssemblyContext_AssemblyConnector().equals(ctx)) {
					if (assConnector.getProvidedRole_AssemblyConnector().getProvidedInterface__OperationProvidedRole()
							.getId().equals(role.getRequiredInterface__OperationRequiredRole().getId())) {
						return ((AssemblyConnector) connector).getProvidingAssemblyContext_AssemblyConnector();
					}
				}
			} else if (connector instanceof RequiredDelegationConnector && ((RequiredDelegationConnector) connector)
					.getOuterRequiredRole_RequiredDelegationConnector().equals(role)) {
				// TODO this branch is not tested
				AssemblyContext innerCtx = getContextProvidingRole(
						((RequiredDelegationConnector) connector).getAssemblyContext_RequiredDelegationConnector(),
						role);
				if (innerCtx != null) {
					return innerCtx;
				}
			}
		}

		return null;
	}

	public static Pair<ResourceDemandingSEFF, AssemblyContext> getSeffByMeasuringPoint(Repository repository,
			UsageModel usageModel, System system, MeasuringPoint point, MetricDescription metric) {
		if (getMetricType(metric) == MetricType.RESPONSE_TIME) {
			Matcher assemblyMatcher = ASSEMBLY_CTX_PATTERN.matcher(point.getStringRepresentation());
			Matcher callIdMatcher = CALL_ID_PATTERN.matcher(point.getStringRepresentation());

			if (assemblyMatcher.find() && callIdMatcher.find()) {
				// get belonging action
				AbstractAction belongingAction = PcmUtils.getElementById(repository, AbstractAction.class,
						callIdMatcher.group(1));
				// this stays until i exactly know if the assembly context is relevant or not
				AssemblyContext ctx = PcmUtils.getElementById(system, AssemblyContext.class, assemblyMatcher.group(1));
				if (belongingAction != null && ctx != null) {
					if (belongingAction instanceof ExternalCallAction) {
						return getSeffByAssemblySignature(ctx,
								((ExternalCallAction) belongingAction).getRole_ExternalService(),
								((ExternalCallAction) belongingAction).getCalledService_ExternalService());
					}
				}
			} else {
				// is entry level system call?
				Matcher entryCallMatcher = ENTRY_LEVEL_SYSTEM_PATTERN.matcher(point.getStringRepresentation());
				if (entryCallMatcher.find()) {
					EntryLevelSystemCall entryCall = PcmUtils.getElementById(usageModel, EntryLevelSystemCall.class,
							entryCallMatcher.group(1));
					if (entryCall == null) {
						return null;
					}
					return PcmUtils.getSeffByProvidedRoleAndSignature(system,
							entryCall.getOperationSignature__EntryLevelSystemCall(),
							entryCall.getProvidedRole_EntryLevelSystemCall());
				}
			}
		}

		return null;
	}

	public static MetricType getMetricType(MetricDescription desc) {
		return MetricType.fromId(desc.getId());
	}

}
