package net.modfest.platform;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class JsonBodyHandler implements HttpResponse.BodyHandler<JsonElement> {
	private final HttpResponse.BodyHandler<InputStream> inner = HttpResponse.BodyHandlers.ofInputStream();

	@Override
	public HttpResponse.BodySubscriber<JsonElement> apply(HttpResponse.ResponseInfo responseInfo) {
		return new Subscriber(inner.apply(responseInfo));
	}

	private record Subscriber(HttpResponse.BodySubscriber<InputStream> inner) implements HttpResponse.BodySubscriber<JsonElement> {
		@Override
		public CompletionStage<JsonElement> getBody() {
			return inner.getBody().thenApply(stream -> JsonParser.parseReader(new InputStreamReader(stream)));
		}

		@Override
		public void onSubscribe(Flow.Subscription subscription) {
			inner.onSubscribe(subscription);
		}

		@Override
		public void onNext(List<ByteBuffer> item) {
			inner.onNext(item);
		}

		@Override
		public void onError(Throwable throwable) {
			inner.onError(throwable);
		}

		@Override
		public void onComplete() {
			inner.onComplete();
		}
	}
}
