//8. 樹狀佈局計算器：計算每個節點在面板上的 (x, y) 座標（加入方案A：後處理壓縮）

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class TreeLayoutCalculator {

	/**
	 * 計算所有節點的位置，用於樹狀佈局顯示 內含「方案A：後處理壓縮（horizontal compaction）」
	 *
	 * @param pointToNodeMap 點到節點的映射
	 * @param edges          MST 邊集合
	 * @param panelWidth     面板寬度
	 * @param panelHeight    面板高度
	 * @param specifiedRoot  指定根節點 (可為 null)
	 * @return Node -> Point 座標的映射
	 */
	public static Map<Node, Point> calculateLayout(Map<Point, Node> pointToNodeMap, List<Edge> edges, int panelWidth, int panelHeight, Node specifiedRoot) {

		// 1) 建立鄰接表：每個節點 → 鄰居清單
		Map<Node, List<Node>> adjacencyList = new HashMap<>();
		for (Edge edge : edges) {
			adjacencyList.computeIfAbsent(edge.source, k -> new ArrayList<>()).add(edge.target);
			adjacencyList.computeIfAbsent(edge.target, k -> new ArrayList<>()).add(edge.source);
		}

		// 2) 儲存每個節點的最終座標
		Map<Node, Point> layoutMap = new HashMap<>();

		// 3) 用於 BFS 記錄已拜訪過的節點
		Set<Node> visited = new HashSet<>();

		// 4) 儲存每棵樹的根節點
		List<Node> roots = new ArrayList<>();

		// 5) 所有節點
		Set<Node> allNodes = new HashSet<>(pointToNodeMap.values());

		// 6) 如果有指定根節點，先處理它（把同一連通元件標記 visited）
		if (specifiedRoot != null && allNodes.contains(specifiedRoot)) {
			roots.add(specifiedRoot);
			markComponent(specifiedRoot, adjacencyList, visited);
		}

		// 7) 其他未拜訪節點（其他連通元件）也當作新樹的根
		for (Node node : allNodes) {
			if (!visited.contains(node)) {
				roots.add(node);
				markComponent(node, adjacencyList, visited);
			}
		}

		// 8) 佈局起點與參數
		double currentX = 250.0;
		double startY = 20;
		double minGap = 20.0; // 後處理壓縮時的最小水平間距（可依字型大小調整）

		// 9) 逐一處理每棵樹
		for (Node root : roots) {
			// 9.1 由 MST 建立以 root 為根的單向樹
			Map<Node, List<Node>> tree = buildTreeFromGraph(root, adjacencyList);

			// 9.2 計算每個節點的子樹寬度
			Map<Node, Double> subtreeWidths = new HashMap<>();
			calculateSubtreeWidths(root, tree, subtreeWidths);

			// 9.3 計算整棵樹的最大深度（只用來估算高度置中）
			Map<Node, Integer> depths = new HashMap<>();
			int maxDepth = calculateDepth(root, tree, depths);

			// 9.4 計算整棵樹的總寬度
			double treeWidth = subtreeWidths.getOrDefault(root, 50.0);

			// 9.5 計算水平方向的壓縮比例 (spacingFactor)
			double availableWidth = 50; // panelWidth - 100.0; // 依原始程式保留
			double spacingFactor = 1.0;
			if (treeWidth > 0 && treeWidth < availableWidth) {
				spacingFactor = availableWidth / treeWidth;
			}

			// 9.6 垂直置中
			double finalY = startY;
			double treeHeight = maxDepth * 50;
			double availableHeight = panelHeight - startY - 20;
			if (treeHeight < availableHeight) {
				finalY += (availableHeight - treeHeight) / 2.0;
			}

			// 9.7 根據壓縮後寬度調整起始 X，使整棵樹居中
			double startX = currentX + 450 + (availableWidth - treeWidth * spacingFactor) / 2.0;

			// 9.8 指定節點座標（原演算法）
			assignPositions(root, tree, layoutMap, startX, finalY, subtreeWidths, spacingFactor);

			// 9.9 方案A：後處理壓縮（將各子樹在不重疊下盡量靠攏）
			Map<Node, Integer> depthMap = new HashMap<>();
			computeDepths(root, tree, 0, depthMap);
			horizontalCompaction(root, tree, layoutMap, depthMap, minGap);

			// 9.10 更新下一棵樹的起始 X（以壓縮前 treeWidth 為準；若希望考慮壓縮後寬度，可在此改為 computeWidth）
			currentX += treeWidth * spacingFactor;
		}

		// （可選）將所有 x 平移，讓最小 x >= 0
		normalizeToNonNegative(layoutMap);

		return layoutMap;
	}

	/** BFS 標記從 root 出發的所有連通節點 */
	private static void markComponent(Node root, Map<Node, List<Node>> graph, Set<Node> visited) {
		Queue<Node> q = new LinkedList<>();
		q.add(root);
		visited.add(root);
		while (!q.isEmpty()) {
			Node u = q.poll();
			for (Node v : graph.getOrDefault(u, Collections.emptyList())) {
				if (!visited.contains(v)) {
					visited.add(v);
					q.add(v);
				}
			}
		}
	}

	/** 計算節點深度（回傳最大深度） */
	private static int calculateDepth(Node node, Map<Node, List<Node>> tree, Map<Node, Integer> depths) {
		if (depths.containsKey(node))
			return depths.get(node);
		List<Node> children = tree.getOrDefault(node, new ArrayList<>());
		if (children.isEmpty()) {
			depths.put(node, 1);
			return 1;
		}
		int maxChildDepth = 0;
		for (Node child : children) {
			maxChildDepth = Math.max(maxChildDepth, calculateDepth(child, tree, depths));
		}
		depths.put(node, 1 + maxChildDepth);
		return 1 + maxChildDepth;
	}

	/** 自上而下標記深度（用於後處理壓縮的輪廓疊合） */
	private static void computeDepths(Node node, Map<Node, List<Node>> tree, int depth, Map<Node, Integer> depthMap) {
		depthMap.put(node, depth);
		for (Node child : tree.getOrDefault(node, List.of())) {
			computeDepths(child, tree, depth + 1, depthMap);
		}
	}

	/** 分配節點座標（原有演算法） */
	private static void assignPositions(Node node, Map<Node, List<Node>> tree, Map<Node, Point> layoutMap, double x, double y, Map<Node, Double> subtreeWidths, double spacingFactor) {
		// 先處理葉節點：沒有子節點時直接放置在傳入的 x
		List<Node> children = tree.getOrDefault(node, new ArrayList<>());
		if (children.isEmpty()) {
			layoutMap.put(node, new Point(x, y));
			return;
		}

		// 1) 先根據原本演算法，使用「父節點暫時的 x」來排好所有子節點
		double childrenTotalWidth = children.stream().mapToDouble(c -> subtreeWidths.getOrDefault(c, 50.0)).sum();
		double currentChildX = x - (childrenTotalWidth * spacingFactor / 2.0);

		for (Node child : children) {
			double childWidth = subtreeWidths.getOrDefault(child, 50.0) * spacingFactor;
			double childX = currentChildX + (childWidth / 2.0);
			assignPositions(child, tree, layoutMap, childX, y + 50, subtreeWidths, spacingFactor);
			currentChildX += childWidth;
		}

		// 2) 所有子節點都有了 x：將「母節點的 x」設為「下一層相鄰子節點 x 的平均值」
		double sum = 0.0;
		for (Node child : children) {
			sum += layoutMap.get(child).x;
		}
		double avgX = sum / children.size();
		layoutMap.put(node, new Point(avgX, y));
	}

	/** 計算每個節點的子樹寬度（原有演算法） */
	private static void calculateSubtreeWidths(Node node, Map<Node, List<Node>> tree, Map<Node, Double> widths) {
		List<Node> children = tree.getOrDefault(node, new ArrayList<>());
		if (children.isEmpty()) {
			widths.put(node, 50.0);
			return;
		}
		double totalWidth = 0.0;
		for (Node child : children) {
			calculateSubtreeWidths(child, tree, widths);
			totalWidth += widths.getOrDefault(child, 50.0);
		}
		widths.put(node, totalWidth);
	}

	/** 由 MST 建立單向樹 (從 root 出發) */
	private static Map<Node, List<Node>> buildTreeFromGraph(Node root, Map<Node, List<Node>> adjacencyList) {
		Map<Node, List<Node>> tree = new HashMap<>();
		Queue<Node> queue = new LinkedList<>();
		Set<Node> visited = new HashSet<>();

		queue.add(root);
		visited.add(root);
		tree.put(root, new ArrayList<>());

		while (!queue.isEmpty()) {
			Node parent = queue.poll();
			for (Node child : adjacencyList.getOrDefault(parent, Collections.emptyList())) {
				if (!visited.contains(child)) {
					visited.add(child);
					tree.computeIfAbsent(parent, k -> new ArrayList<>()).add(child);
					tree.computeIfAbsent(child, k -> new ArrayList<>()); // 確保每個節點有條目
					queue.add(child);
				}
			}
		}
		return tree;
	}

	// -------------------------
	// 方案A：後處理壓縮（核心）
	// -------------------------

	/**
	 * 將整棵子樹（以 root 為根）做水平壓縮，使各子樹在不重疊前提下盡量靠攏。 採用「逐層掃描 + 子樹輪廓」的近似演算法。
	 */
	private static void horizontalCompaction(Node root, Map<Node, List<Node>> tree, Map<Node, Point> layout, Map<Node, Integer> depthMap, double minGap) {
		// 依深度分組
		Map<Integer, List<Node>> byDepth = groupNodesByDepth(root, tree, depthMap);
		List<Integer> depths = new ArrayList<>(byDepth.keySet());
		Collections.sort(depths);

		// 逐層處理：將同層的子樹從左到右掃，碰到重疊就把右側子樹往左推
		for (int d : depths) {
			List<Node> level = new ArrayList<>(byDepth.get(d));
			level.sort(Comparator.comparingDouble(n -> layout.get(n).x));

			Contour acc = null; // 累積已放置之子樹的「右輪廓」

			for (Node u : level) {
				// 以 u 為根的子樹輪廓
				Contour cu = buildContour(u, tree, layout, depthMap);
				if (acc == null) {
					acc = cu;
					continue;
				}

				// 計算為避免重疊所需的左移量（負值表示往左移）
				double dx = overlapNeeded(acc, cu, minGap);
				if (dx < 0) {
					shiftSubtree(u, tree, layout, dx);
					cu = cu.shifted(dx);
				}
				acc = Contour.merge(acc, cu);
			}
		}
	}

	/** 將整棵子樹（u 為根）水平平移 dx */
	private static void shiftSubtree(Node u, Map<Node, List<Node>> tree, Map<Node, Point> layout, double dx) {
		Point p = layout.get(u);
		layout.put(u, new Point(p.x + dx, p.y));
		for (Node v : tree.getOrDefault(u, List.of())) {
			shiftSubtree(v, tree, layout, dx);
		}
	}

	/** 將所有 x 平移，使最小 x >= 0（可選） */
	private static void normalizeToNonNegative(Map<Node, Point> layout) {
		double minX = Double.POSITIVE_INFINITY;
		for (Point p : layout.values())
			minX = Math.min(minX, p.x);
		if (minX < 0) {
			double dx = -minX + 5; // 留 5px 邊距
			for (Map.Entry<Node, Point> e : layout.entrySet()) {
				Point p = e.getValue();
				e.setValue(new Point(p.x + dx, p.y));
			}
		}
	}

	/** 以 root 為根，建立每個節點所屬深度的分組 */
	private static Map<Integer, List<Node>> groupNodesByDepth(Node root, Map<Node, List<Node>> tree, Map<Node, Integer> depthMap) {
		Map<Integer, List<Node>> byDepth = new HashMap<>();
		Queue<Node> q = new LinkedList<>();
		Set<Node> vis = new HashSet<>();
		q.add(root);
		vis.add(root);
		while (!q.isEmpty()) {
			Node u = q.poll();
			int d = depthMap.getOrDefault(u, 0);
			byDepth.computeIfAbsent(d, k -> new ArrayList<>()).add(u);
			for (Node v : tree.getOrDefault(u, List.of())) {
				if (!vis.contains(v)) {
					vis.add(v);
					q.add(v);
				}
			}
		}
		return byDepth;
	}

	// -------------------------
	// 輪廓（contour）輔助結構與運算
	// -------------------------

	/**
	 * 子樹輪廓：紀錄各深度的最左/最右 x。 使用絕對深度（與 depthMap 一致），以便跨子樹比較。
	 */
	private static class Contour {
		final Map<Integer, Double> left = new HashMap<>();
		final Map<Integer, Double> right = new HashMap<>();

		void include(int depth, double x) {
			left.put(depth, Math.min(left.getOrDefault(depth, x), x));
			right.put(depth, Math.max(right.getOrDefault(depth, x), x));
		}

		Contour shifted(double dx) {
			Contour c = new Contour();
			for (Map.Entry<Integer, Double> e : left.entrySet())
				c.left.put(e.getKey(), e.getValue() + dx);
			for (Map.Entry<Integer, Double> e : right.entrySet())
				c.right.put(e.getKey(), e.getValue() + dx);
			return c;
		}

		static Contour merge(Contour a, Contour b) {
			Contour c = new Contour();
			// 合併左界
			Set<Integer> depths = new HashSet<>();
			depths.addAll(a.left.keySet());
			depths.addAll(b.left.keySet());
			for (int d : depths) {
				double la = a.left.getOrDefault(d, Double.POSITIVE_INFINITY);
				double lb = b.left.getOrDefault(d, Double.POSITIVE_INFINITY);
				c.left.put(d, Math.min(la, lb));
			}
			// 合併右界
			depths.clear();
			depths.addAll(a.right.keySet());
			depths.addAll(b.right.keySet());
			for (int d : depths) {
				double ra = a.right.getOrDefault(d, Double.NEGATIVE_INFINITY);
				double rb = b.right.getOrDefault(d, Double.NEGATIVE_INFINITY);
				c.right.put(d, Math.max(ra, rb));
			}
			return c;
		}
	}

	/** 建立以 u 為根之子樹輪廓 */
	private static Contour buildContour(Node u, Map<Node, List<Node>> tree, Map<Node, Point> layout, Map<Node, Integer> depthMap) {
		Contour c = new Contour();
		Deque<Node> stack = new ArrayDeque<>();
		stack.push(u);
		while (!stack.isEmpty()) {
			Node x = stack.pop();
			int d = depthMap.getOrDefault(x, 0);
			c.include(d, layout.get(x).x);
			List<Node> children = tree.getOrDefault(x, List.of());
			for (int i = children.size() - 1; i >= 0; --i) {
				stack.push(children.get(i));
			}
		}
		return c;
	}

	/**
	 * 計算使 cu 與 acc 不重疊所需的位移量（負值 = 往左移）。 overlap = max_over_common_depth (acc.right + minGap - cu.left) 若 overlap <= 0 → 無需移動，回傳 0；否則回傳 -overlap。
	 */
	private static double overlapNeeded(Contour acc, Contour cu, double minGap) {
		double need = Double.NEGATIVE_INFINITY;
		Set<Integer> depths = new HashSet<>(acc.right.keySet());
		depths.retainAll(cu.left.keySet());
		if (depths.isEmpty())
			return 0.0;
		for (int d : depths) {
			double r = acc.right.getOrDefault(d, Double.NEGATIVE_INFINITY);
			double l = cu.left.getOrDefault(d, Double.POSITIVE_INFINITY);
			need = Math.max(need, (r + minGap) - l);
		}
		if (need <= 0)
			return 0.0; // 不需要移動
		return -need; // 往左移
	}

	// （工具）計算目前佈局寬度（可用來比較不同策略的結果）
	static double computeWidth(Map<Node, Point> layout) {
		double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
		for (Point p : layout.values()) {
			min = Math.min(min, p.x);
			max = Math.max(max, p.x);
		}
		return (layout.isEmpty() ? 0 : max - min);
	}
}