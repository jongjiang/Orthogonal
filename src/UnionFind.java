import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 7. Union-Find 資料結構 (用於 Kruskal)
class UnionFind {
	private final Map<Node, Node> parent = new HashMap<>();

	public UnionFind(List<Node> nodes) {
		for (Node node : nodes) {
			parent.put(node, node);
		}
	}

	public Node find(Node i) {
		if (parent.get(i) == i) {
			return i;
		}
		Node root = find(parent.get(i));
		parent.put(i, root);
		return root;
	}

	public boolean union(Node i, Node j) {
		Node rootI = find(i);
		Node rootJ = find(j);
		if (!rootI.equals(rootJ)) {
			parent.put(rootI, rootJ);
			return true;
		}
		return false;
	}
}