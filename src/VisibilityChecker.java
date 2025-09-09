// 4. 可見性檢查器

import java.util.List;

public class VisibilityChecker {
	private final List<LineSegment> obstacles;

	public VisibilityChecker(List<LineSegment> obstacles) {
		this.obstacles = obstacles;
	}

	public boolean isVisible(Point start, Point end) {
		LineSegment path = new LineSegment(start, end);
		return obstacles.stream().noneMatch(obstacle -> GeometryUtils.doIntersect(path, obstacle));
	}
}