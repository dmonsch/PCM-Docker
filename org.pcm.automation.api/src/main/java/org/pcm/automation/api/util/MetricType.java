package org.pcm.automation.api.util;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum MetricType {
	RESOURCE_DEMAND("_sefjUeJCEeO6l86uYUhhyw"), RESPONSE_TIME("_mZb3MdoLEeO-WvSDaR6unQ"), ACTIVE_STATS(
			"_-buIceJDEeO6l86uYUhhyw");

	private String value;
	private static Map<String, MetricType> typeMapping;

	private MetricType(String value) {
		this.value = value;
	}

	static {
		Map<String, MetricType> map = new ConcurrentHashMap<String, MetricType>();
		for (MetricType instance : MetricType.values()) {
			map.put(instance.value, instance);
		}
		typeMapping = Collections.unmodifiableMap(map);
	}

	public static MetricType fromId(String id) {
		return typeMapping.get(id);
	}
}
