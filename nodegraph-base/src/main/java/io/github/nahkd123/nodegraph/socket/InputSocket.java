package io.github.nahkd123.nodegraph.socket;

/**
 * <p>
 * Represent an input socket that accept input from either incoming connection
 * or user-controlled initial value.
 * </p>
 * 
 * @param <V> Type of value that this socket will consume.
 */
public record InputSocket<V>(Class<V> type, String name, V defaultValue) implements Socket<V> {
}
