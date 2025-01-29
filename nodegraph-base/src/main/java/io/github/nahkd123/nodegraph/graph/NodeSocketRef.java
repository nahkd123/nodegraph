package io.github.nahkd123.nodegraph.graph;

import io.github.nahkd123.nodegraph.socket.Socket;

public record NodeSocketRef<S, E, V>(NodeInstance<S, E> node, Socket<V> socket) {
}
