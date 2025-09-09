import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

// 6. 最小生成樹 (MST) 演算法 - Kruskal
public class KruskalMST {
	public static List<Edge> findMST(List<Point> vertices, VisibilityChecker checker, Map<Point, Node> pointToNodeMap) {
		List<Edge> allEdges = new ArrayList<>();

		for (int i = 0; i < vertices.size(); i++) {
			for (int j = i + 1; j < vertices.size(); j++) {
				Point p1 = vertices.get(i);
				Point p2 = vertices.get(j);

				if (checker.isVisible(p1, p2)) {
					double distance = p1.distanceTo(p2);
					double weight = distance * distance;
					Node n1 = pointToNodeMap.get(p1);
					Node n2 = pointToNodeMap.get(p2);

					if (n1 != null && n2 != null) {
						allEdges.add(new Edge(n1, n2, weight));
					}
				}
			}
		}

		allEdges.sort(Comparator.comparingDouble(e -> e.weight));
		List<Edge> mst = new ArrayList<>();
		UnionFind unionFind = new UnionFind(new ArrayList<>(pointToNodeMap.values()));

		for (Edge edge : allEdges) {
			if (unionFind.union(edge.source, edge.target)) {
				mst.add(edge);
			}
		}
		return mst;
	}
}