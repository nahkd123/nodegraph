package io.github.nahkd123.nodegraph.socket;

/**
 * <p>
 * Represent a socket in the node. This socket interface is not meant to be
 * implemented directly by library consumer; use either {@link InputSocket} or
 * {@link OutputSocket}.
 * </p>
 * <p>
 * When using custom value, the value <b>must</b> be immutable (for example,
 * boxed primitives or record objects).
 * </p>
 * 
 * @param <V> Type of value that this socket can accept as input or produce as
 *            output.
 * @see InputSocket
 * @see OutputSocket
 */
public sealed interface Socket<V> permits InputSocket, OutputSocket {
	/**
	 * <p>
	 * Get the type of the value. GUI will use this information to create graphics
	 * for the socket, as well as avoiding user from connecting 2 sockets with
	 * different type.
	 * </p>
	 * 
	 * @return The type of the value.
	 */
	Class<V> type();

	/**
	 * <p>
	 * Get the name of this socket. The name must be unique under node's domain (for
	 * example, you can have 2 sockets located on 2 different node that uses the
	 * same {@code id} name, but you can't have 2 sockets in a single node with same
	 * name).
	 * </p>
	 * 
	 * @return The name of this socket.
	 */
	String name();
}
