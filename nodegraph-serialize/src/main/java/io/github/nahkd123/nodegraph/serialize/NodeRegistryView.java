package io.github.nahkd123.nodegraph.serialize;

import io.github.nahkd123.nodegraph.node.Node;

/**
 * <p>
 * A read-only registry view to query IDs for serialization.
 * </p>
 * 
 * @param <E> Type of environment object.
 */
public interface NodeRegistryView<E> {
	Node<?, E> getNodeFromId(String id);

	String getIdFromNode(Node<?, E> node);
}
