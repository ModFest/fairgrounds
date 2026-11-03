package net.modfest.platform.data;

import com.mojang.serialization.DataResult;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface DataOrThrowable<T extends @Nullable Object> {
	static <T extends @Nullable Object> DataOrThrowable<T> of(DataResult<T> data) {
		if (data.isError()) return error(data.error().orElseThrow(() -> new IllegalStateException("Data is error but has no error")).message());
		if (data.isSuccess()) return data(data.result().orElseThrow(() -> new IllegalStateException("Data is success but has no data")));
		return throwable("Data is neither error or success", IllegalArgumentException::new);
	}

	static <T extends @Nullable Object> DataOrThrowable<T> data(T data) {
		return new Data<>(data);
	}

	static <T extends @Nullable Object> DataOrThrowable<T> error(String message) {
		return new Error<>(message, null);
	}

	static <T extends @Nullable Object> DataOrThrowable<T> throwable(String message, Throwable throwable) {
		return new Error<>(message, throwable);
	}

	static <T extends @Nullable Object> DataOrThrowable<T> throwable(String message, Function<String, Throwable> throwable) {
		return new Error<>(message, throwable.apply(message));
	}

	T data();
	Error<T> error();

	default T getOrThrow() {
		return getOrThrow(IllegalStateException::new);
	}

	<E extends Throwable> T getOrThrow(BiFunction<String, @Nullable Throwable, E> throwable) throws E;

	boolean isSuccess();
	default boolean isError() {
		return !isSuccess();
	}

	default void stderr(Logger logger, String message) {
		if (!this.isError()) return;

		var error = this.error();
		if (error.throwable() == null) {
			logger.error("{}; {}", message, error.message());
		} else {
			logger.error("{}; {}", message, error.message(), error.throwable());
		}
	}

	default DataOrThrowable<T> ifSuccess(Consumer<? super T> ifSuccess) {
		if (isSuccess()) ifSuccess.accept(data());
		return this;
	}

	default DataOrThrowable<T> ifError(Consumer<? super Error<T>> ifError) {
		if (isError()) ifError.accept(error());
		return this;
	}

	record Data<T extends @Nullable Object>(T value) implements DataOrThrowable<T> {
		@Override
		public T data() {
			return value;
		}

		@Override
		public Error<T> error() {
			throw new UnsupportedOperationException("Data is not error");
		}

		@Override
		public boolean isSuccess() {
			return true;
		}

		@Override
		public <E extends Throwable> T getOrThrow(BiFunction<String, Throwable, E> throwable) {
			return value;
		}
	}

	record Error<T extends @Nullable Object>(String message, @Nullable Throwable throwable) implements DataOrThrowable<T> {
		@Override
		public T data() {
			return null;
		}

		@Override
		public Error<T> error() {
			return this;
		}

		@Override
		public boolean isSuccess() {
			return false;
		}

		@Override
		public <E extends Throwable> T getOrThrow(BiFunction<String, @Nullable Throwable, E> throwable) throws E {
			throw throwable.apply(message, this.throwable);
		}
	}
}
