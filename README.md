# NodeGraph
![Tag Badge](https://img.shields.io/github/v/tag/nahkd123/nodegraph)

Node graph system library for Java (like Blender shader nodes).

## Modules
- `nodegraph-base`: NodeGraph itself. Contains an interface for implementing your own node, `NodeGraph` for connecting nodes and evaluating the graph.
- `nodegraph-serialize`: Graph serialization module that (de)serialize from `DataInput` or to `DataOutput`.

> [!NOTE]
> NodeGraph does not provides any node implementation out of the box at this moment; you will have to implement them yourself. I'm unsure which kind of node that will be used the most.

## Using NodeGraph
[![](https://jitpack.io/v/nahkd123/nodegraph.svg)](https://jitpack.io/#nahkd123/nodegraph)

### Maven
```xml
<repositories>
	<repository>
	    <id>jitpack.io</id>
	    <url>https://jitpack.io</url>
	</repository>
</repositories>
<dependencies>
    <dependency>
        <groupId>com.github.nahkd123.nodegraph</groupId>
        <artifactId>nodegraph-base</artifactId>
        <version>[TAG]</version>
    </dependency>
    <!-- TODO: also include nodegraph-serialize -->
</dependencies>
```

## Example
### Implementing add node (add 2 values)
```java
public class AddNode implements Node<Void, Void> {
    // If you don't plan on using generics or make the node type hold global
    // states, you may consider making a singleton
    public final static AddNode NODE = new AddNode();

    // Define inputs and outputs
    // Make them public and final so you can reference the socket when connecting
    public final InputSocket<Number> inputA = new InputSocket<>(Number.class, "inputA", 0);
    public final InputSocket<Number> inputB = new InputSocket<>(Number.class, "inputB", 0);
    public final OutputSocket<Number> output = new OutputSocket<>(Number.class, "output");

    @Override
    public List<Socket<?>> getSockets() { return List.of(inputA, inputB, output); }

    // The 1st generic is for holding node states. Since our add node does not
    // need to store any states while processing, we can just return null
    @Override
    public Void initialize() {
        return null;
    }

    @Override
    public void process(NodeProcessContext<Void, Void> context) {
        double a = context.get(inputA).doubleValue();
        double b = context.get(inputB).doubleValue();
        context.set(output, a + b);
    }
}
```

### Making a graph
```java
NodeGraph<Void> domain = new NodeGraph<>();
NodeInstance<Void, Void> a = domain.addInstance(new NodeInstance<>(AddNode.NODE, null));
NodeInstance<Void, Void> b = domain.addInstance(new NodeInstance<>(AddNode.NODE, null));

// Set initial values for input sockets
// Initial values will be used if there is no incoming connection
a.setInitialValue(addNode.inputA, 1);
a.setInitialValue(addNode.inputB, 2);
b.setInitialValue(addNode.inputA, 3);
b.setInitialValue(addNode.inputB, 4);

// Current node configuration:
// +-----------+                 +-----------+
// | A         |                 | B         |
// +-----------+                 +-----------+
// inputA (1)  |                 inputA (3)  |
// inputB (2)  |                 inputB (4)  |
// |      output                 |      output
// +-----------+                 +-----------+

// Connect output of A to first input of B
domain.connect(a, addNode.output, b, addNode.inputA);

// Current node configuration after connected:
// +-----------+                 +-----------+
// | A         |                 | B         |
// +-----------+                 +-----------+
// inputA (1)  |        /------> inputA      |
// inputB (2)  |        |        inputB (4)  |
// |      output -------/        |      output
// +-----------+                 +-----------+
```

### Evaluating a node in a graph
```java
// An evaulation round holds the node states if the node is reachable from
// target node. For example, if you connect node A to node B and evaluate node B
// it will store the states of A and B. If there's node C but not connected to
// node B (either directly or indirectly), it will not store the states of C.

EvaluationRound<Void> eval = domain.newEvalRound(null);
eval.eval(a).get(AddNode.NODE.output); // 1 + 2 = 3
eval.eval(b).get(AddNode.NODE.output); // (1 + 2) + 4 = 7

// The states will not be removed from evaluation round, allowing you to call it
// multiple times. This is useful for building simulation system, where each
// evaluation update the internal states.
```

## License
MIT License.