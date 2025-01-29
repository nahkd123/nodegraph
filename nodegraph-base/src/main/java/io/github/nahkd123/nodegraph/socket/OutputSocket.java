package io.github.nahkd123.nodegraph.socket;

/**
 * <p>
 * Represent a socket that produce outputs for other nodes or to requester.
 * </p>
 * 
 * @param <V> Type of value that this socket will produce.
 */
public record OutputSocket<V>(Class<V> type, String name) implements Socket<V> {
}
