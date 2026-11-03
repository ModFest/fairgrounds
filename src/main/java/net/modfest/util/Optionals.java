package net.modfest.util;

import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

public class Optionals {
	public static  <T, R> Function<T, Optional<R>> wrap(Function<T, @Nullable R> function) {
		return val -> Optional.ofNullable(function.apply(val));
	}
}
