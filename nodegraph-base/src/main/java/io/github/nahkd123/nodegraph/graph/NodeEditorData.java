package io.github.nahkd123.nodegraph.graph;

/**
 * <p>
 * Node editor data are used to render the node in editor.
 * </p>
 */
public class NodeEditorData {
	private String displayName;
	private int x;
	private int y;
	private int width;
	private int height;
	private boolean expanded;

	public NodeEditorData(String displayName, int x, int y, int width, int height, boolean expanded) {
		this.displayName = displayName;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.expanded = expanded;
	}

	public String getDisplayName() { return displayName; }

	public void setDisplayName(String displayName) { this.displayName = displayName; }

	public int getX() { return x; }

	public void setX(int x) { this.x = x; }

	public int getY() { return y; }

	public void setY(int y) { this.y = y; }

	public int getWidth() { return width; }

	public void setWidth(int width) { this.width = width; }

	public int getHeight() { return height; }

	public void setHeight(int height) { this.height = height; }

	public boolean isExpanded() { return expanded; }

	public void setExpanded(boolean expanded) { this.expanded = expanded; }

	public NodeEditorData copy() {
		return new NodeEditorData(displayName, x, y, width, height, expanded);
	}
}
