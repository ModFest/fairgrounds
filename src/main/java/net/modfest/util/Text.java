package net.modfest.util;


import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Arrays;

public class Text {
	public static MutableComponent compound(Component component1, String component2) {
		return Component.empty().append(component1).append(component2);
	}

	public static MutableComponent compound(String component1, Component component2) {
		return Component.empty().append(component1).append(component2);
	}

	public static MutableComponent compound(String... texts) {
		// Transform to component array and use it with addAll to take advantage of the array syscalls
		Component[] components = new Component[texts.length];
		for (int i = 0; i < texts.length; i++) {
			components[i] = Component.literal(texts[i]);
		}

		return compound(components);
	}

	public static MutableComponent compound(Component... components) {
		MutableComponent component = Component.empty();
		component.getSiblings().addAll(Arrays.asList(components));
		return component;
	}

	public static MutableComponent append(String base, String... texts) {
		return append(Component.literal(base), texts);
	}

	public static MutableComponent append(String base, Component... components) {
		return append(Component.literal(base), components);
	}

	public static MutableComponent append(MutableComponent base, String... texts) {
		// Transform to component array and use it with addAll to take advantage of the array syscalls
		Component[] components = new Component[texts.length];
		for (int i = 0; i < texts.length; i++) {
			components[i] = Component.literal(texts[i]);
		}

		return append(base, components);
	}

	public static MutableComponent append(MutableComponent base, Component... components) {
		base.getSiblings().addAll(Arrays.asList(components));
		return base;
	}
}
