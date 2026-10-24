package net.fabricmc.fabric.api.event;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.Function;

public final class EventFactory {
	private EventFactory() {
	}

	public static <T> Event<T> createArrayBacked(Class<? super T> type, Function<T[], T> invokerFactory) {
		return new ArrayBackedEvent<>(type, invokerFactory);
	}

	public static <T> Event<T> createArrayBacked(Class<T> type, T emptyInvoker, Function<T[], T> invokerFactory) {
		return new ArrayBackedEvent<>(type, listeners -> listeners.length == 0 ? emptyInvoker : invokerFactory.apply(listeners));
	}

	private static final class ArrayBackedEvent<T> extends Event<T> {
		private final Function<T[], T> invokerFactory;
		private T[] listeners;

		@SuppressWarnings("unchecked")
		private ArrayBackedEvent(Class<? super T> type, Function<T[], T> invokerFactory) {
			this.invokerFactory = invokerFactory;
			this.listeners = (T[]) Array.newInstance(type, 0);
			this.invoker = invokerFactory.apply(this.listeners);
		}

		@Override
		public synchronized void register(T listener) {
			T[] grown = Arrays.copyOf(this.listeners, this.listeners.length + 1);
			grown[grown.length - 1] = listener;
			this.listeners = grown;
			this.invoker = this.invokerFactory.apply(grown);
		}
	}
}
