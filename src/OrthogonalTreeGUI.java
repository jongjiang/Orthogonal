import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class OrthogonalTreeGUI {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			List<LineSegment> obstacles = new ArrayList<>();
			obstacles.add(new LineSegment(new Point(1, 3), new Point(3, 2)));
			obstacles.add(new LineSegment(new Point(3, 2), new Point(9, 8)));
			obstacles.add(new LineSegment(new Point(3, 2), new Point(8, 2)));
			obstacles.add(new LineSegment(new Point(3, 8), new Point(4, 4)));

			List<Point> points = new ArrayList<>();
			// 由於 Kruskal 演算法會對 points 排序，我們需要一個原始的列表來保持點的順序。
			List<Point> originalPoints = new ArrayList<>();
			Random r = new Random(System.currentTimeMillis());

			// 新增：點與編號的映射
			Map<Point, Integer> pointNumberMap = new HashMap<>();
			int pointCounter = 1;

			// 5 x 5 grids
			int maxI = 9;
			int maxJ = 9;
			for (int i = 1; i <= maxI; i += 2) {
				for (int j = 1; j <= maxJ; j += 2) {
					double d1 = r.nextInt(9) * 0.1;
					double d2 = r.nextInt(9) * 0.1;
					Point pt = new Point(i + d1, j + d2);
					Point pt2 = new Point(pt.x, pt.y);
					points.add(new Point(i + d1, j + d2));
					originalPoints.add(pt2); // 儲存原始順序的點
					pointNumberMap.put(pt, pointCounter++); // 關聯編號
				}
			}

			Point pt_1st = new Point(0.5, 0.5);
			Point pt_last = new Point(maxI + 0.9, maxJ + 0.9);
			points.add(pt_1st);
			points.add(pt_last);
			originalPoints.add(new Point(0.5, 0.5));
			originalPoints.add(new Point(maxI + 0.9, maxJ + 0.9));
			pointNumberMap.put(pt_1st, 0);
			pointNumberMap.put(pt_last, pointCounter++);
			Point customRootPoint = points.get(r.nextInt(points.size()));

			Map<Point, Node> pointToNodeMap = points.stream().collect(Collectors.toMap(p -> p, Node::new, (existing, replacement) -> existing));

			VisibilityChecker checker = new VisibilityChecker(obstacles);
			// 最小生成樹 (MST) 演算法 - Kruskal
			List<Edge> mstEdges = KruskalMST.findMST(points, checker, pointToNodeMap);

			// 指定根節點
			Node specifiedRoot = pointToNodeMap.get(customRootPoint);

			JFrame frame = new JFrame("可見性圖的最小生成樹 (樹狀佈局)");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			// 傳入 pointNumberMap 和原始 points 列表
			frame.add(new JScrollPane(new TreeGraphPanel(pointToNodeMap, mstEdges, specifiedRoot, originalPoints, obstacles, pointNumberMap)));
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}
}