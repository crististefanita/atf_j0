package atf.reporting.browser.lifecycle;

import java.util.function.Consumer;

/** A lexical SDK lease retains the original, concrete SDK value and its bounded disposer. */
public final class BrowserLease<T> implements AutoCloseable {
    private final T value;
    private final Consumer<T> disposer;
    private boolean closed;

    public BrowserLease(T value, Consumer<T> disposer) { this.value = value; this.disposer = disposer; }
    public T value() { return value; }
    @Override public void close() { if (!closed) { closed = true; disposer.accept(value); } }
}
