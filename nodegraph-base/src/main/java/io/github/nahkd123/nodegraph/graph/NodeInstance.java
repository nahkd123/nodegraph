package io.github.nahkd123.nodegraph.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import io.github.nahkd123.nodegraph.node.Node;
import io.github.nahkd123.nodegraph.socket.InputSocket;

/**
 * <p>
 * Node instance contains the initial default value for node's sockets and
 * editor data. In the future, node instance may store user-controlled
 * properties.
 * </p>
 * 
 * @param <S> Type of the internal node states object.
 * @param <E> Type of environment object.
 */
public class NodeInstance<S, E> {
	private Node<S, E> node;
	private NodeEditorData editorData;
	private Map<InputSocket<?>, ?> sockets = new HashMap<>();

	/**
	 * <p>
	 * Create a new node instance with optional initial default values for certain
	 * sockets.
	 * </p>
	 * 
	 * @param node       The node.
	 * @param editorData The editor data.
	 * @param sockets    An optional collection of socket-value pair to set as
	 *                   initial value.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public NodeInstance(Node<S, E> node, NodeEditorData editorData, Collection<Map.Entry<InputSocket<?>, ?>> sockets) {
		this.node = node;
		this.editorData = editorData;
		if (sockets != null) sockets.forEach(entry -> ((Map) this.sockets).put(entry.getKey(), entry.getValue()));
	}

	public NodeInstance(Node<S, E> node, NodeEditorData editorData) {
		this(node, editorData, null);
	}

	public Node<S, E> getNode() { return node; }

	public NodeEditorData getEditorData() { return editorData; }

	public void setEditorData(NodeEditorData editorData) { this.editorData = editorData; }

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <V> V getInitialValue(InputSocket<V> socket) {
		return (V) ((Map) sockets).computeIfAbsent(socket, s -> (socket instanceof InputSocket<V> input
			? input.defaultValue()
			: null));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <V> void setInitialValue(InputSocket<V> socket, V initialValue) {
		((Map) sockets).put(socket, initialValue);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public NodeInstance<S, E> copy() {
		return new NodeInstance<>(node, editorData.copy(), (Collection) sockets.entrySet());
	}
}
