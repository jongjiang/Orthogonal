import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

// --- Swing 繪圖部分 ---
@SuppressWarnings("serial")
class TreeGraphPanel extends JPanel {
	private final List<Edge> mstEdges;
	private final Map<Point, Node> pointToNodeMap;
	private final Node specifiedRoot;

	private final List<Point> allPoints2;
	private final List<LineSegment> obstacles;
	// 新增：點編號的映射
	private final Map<Point, Integer> pointNumberMap;

	// 修改建構子，接收點編號的映射
	public TreeGraphPanel(Map<Point, Node> pointToNodeMap, List<Edge> mstEdges, Node specifiedRoot, List<Point> allPoints2, List<LineSegment> obstacles, Map<Point, Integer> pointNumberMap) {
		this.pointToNodeMap = pointToNodeMap;
		this.mstEdges = mstEdges;
		this.specifiedRoot = specifiedRoot;
		this.allPoints2 = allPoints2;
		this.obstacles = obstacles;
		this.pointNumberMap = pointNumberMap; // 儲存點編號的映射
		setPreferredSize(new Dimension(1700, 1000));
		setBackground(Color.WHITE);
	}

	@Override
	protected void paintComponent(Graphics g) {

		int xOffset = 10;

		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// 修改這裡：從 TreeLayoutCalculator 換成 BuchheimTreeLayout
		// Map<Node, Point> nodeLayout = BuchheimTreeLayout.calculateLayout(pointToNodeMap, mstEdges, 500, getHeight(), specifiedRoot);
		Map<Node, Point> nodeLayout = TreeLayoutCalculator.calculateLayout(pointToNodeMap, mstEdges, 500, getHeight(), specifiedRoot);

		g2d.setColor(Color.BLUE);
		g2d.setStroke(new BasicStroke(2));
		for (Edge edge : mstEdges) {
			Point p1 = nodeLayout.get(edge.source);
			Point p2 = nodeLayout.get(edge.target);
			if (p1 != null && p2 != null) {
				g2d.drawLine((int) (p1.x + xOffset), (int) p1.y, (int) (p2.x + xOffset), (int) p2.y);
			}
		}

		// draw tree points
		g2d.setColor(Color.BLACK);
		for (Map.Entry<Node, Point> entry : nodeLayout.entrySet()) {
			Point p = entry.getValue();
			g2d.fillOval((int) (p.x - 5 + xOffset), (int) p.y - 5, 10, 10);
			g2d.drawString("x=" + String.valueOf((p.x - 5 + xOffset)), (int) (p.x - 5 + xOffset), (int) p.y + 15);

			// g2d.drawString(entry.getKey().point.toString(), (int) (p.x + 10 + +xOffset), (int) p.y);
			// 修改這裡：從點編號的映射中取得編號
			Integer pointNumber = pointNumberMap.get(entry.getKey().point);
			if (pointNumber != null) {
				g2d.drawString(String.valueOf(pointNumber), (int) (p.x + 10 + xOffset), (int) p.y);
			} else {
				// 如果找不到編號，仍舊顯示原始座標
				g2d.drawString(entry.getKey().point.toString(), (int) (p.x + 10 + xOffset), (int) p.y);
			}
		}

		// paint network
		paint2(g2d);

		// 新增：特別標示指定的根節點
		if (specifiedRoot != null && nodeLayout.containsKey(specifiedRoot)) {
			g2d.setColor(Color.ORANGE);
			Point rootP = nodeLayout.get(specifiedRoot);
			if (rootP != null) {
				g2d.fillOval((int) (rootP.x - 8 + xOffset), (int) rootP.y - 8, 16, 16);
				g2d.setColor(Color.BLACK);
				g2d.setFont(new Font("新細明體", Font.BOLD, 14));
				g2d.drawString("Root", (int) (rootP.x + 10 + xOffset), (int) rootP.y - 20);
			}
		}

		g2d.setColor(Color.BLACK);
		g2d.setFont(new Font("新細明體", Font.BOLD, 14));
		g2d.drawString("藍色線: 最小生成樹 (樹狀佈局)", 10, 880);
	}

	void paint2(Graphics2D g2d) {

		int xOffset = 850;

		// 繪製障礙物
		g2d.setColor(Color.RED);
		g2d.setStroke(new BasicStroke(3));
		for (LineSegment obstacle : obstacles) {
			g2d.drawLine((int) (obstacle.p1.x * 50 + xOffset), (int) (obstacle.p1.y * 50), (int) (obstacle.p2.x * 50 + xOffset), (int) (obstacle.p2.y * 50));
		}

		// 繪製最小生成樹的邊
		g2d.setColor(Color.BLUE);
		g2d.setStroke(new BasicStroke(3));
		for (Edge edge : mstEdges) {
			Point p1 = edge.source.point;
			Point p2 = edge.target.point;
			g2d.drawLine((int) (p1.x * 50 + xOffset), (int) (p1.y * 50), (int) (p2.x * 50 + xOffset), (int) (p2.y * 50));
		}

		// 繪製所有點
		g2d.setColor(Color.BLACK);
		for (Point p : allPoints2) {
			g2d.fillOval((int) (p.x * 50 - 5 + xOffset), (int) (p.y * 50 - 5), 10, 10);

			// g2d.drawString(p.toString(), (int) (p.x * 50 + 10 + xOffset), (int) (p.y * 50));
			// 修改這裡：從點編號的映射中取得編號
			Integer pointNumber = pointNumberMap.get(p);
			if (pointNumber != null) {
				g2d.drawString(String.valueOf(pointNumber), (int) (p.x * 50 + 10 + xOffset), (int) (p.y * 50));
			} else {
				// 如果找不到編號，仍舊顯示原始座標
				g2d.drawString(p.toString(), (int) (p.x * 50 + 10 + xOffset), (int) (p.y * 50));
			}
		}

		// 新增：特別標示指定的根節點
		if (specifiedRoot != null) {
			g2d.setColor(Color.ORANGE);
			Point rootP = specifiedRoot.point;
			if (rootP != null) {
				g2d.fillOval((int) (rootP.x * 50 - 8 + xOffset), (int) (rootP.y * 50 - 8), 16, 16);
				g2d.setColor(Color.BLACK);
				g2d.setFont(new Font("新細明體", Font.BOLD, 14));
				g2d.drawString("Root", (int) (rootP.x * 50 + 10 + xOffset), (int) (rootP.y * 50 - 10));
			}
		}

	}
}