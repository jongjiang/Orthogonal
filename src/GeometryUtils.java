// 3. 幾何運算工具類
class GeometryUtils {
	public static boolean doIntersect(LineSegment seg1, LineSegment seg2) {
		Point p1 = seg1.p1, q1 = seg1.p2;
		Point p2 = seg2.p1, q2 = seg2.p2;

		int o1 = orientation(p1, q1, p2);
		int o2 = orientation(p1, q1, q2);
		int o3 = orientation(p2, q2, p1);
		int o4 = orientation(p2, q2, q1);

		if (o1 != o2 && o3 != o4) {
			return true;
		}

		if (o1 == 0 && onSegment(p1, p2, q1))
			return true;
		if (o2 == 0 && onSegment(p1, q2, q1))
			return true;
		if (o3 == 0 && onSegment(p2, q2, p1))
			return true;
		if (o4 == 0 && onSegment(p2, q1, q2))
			return true;

		return false;
	}

	private static int orientation(Point p, Point q, Point r) {
		double val = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
		if (Math.abs(val) < 1e-9)
			return 0;
		return (val > 0) ? 1 : 2;
	}

	private static boolean onSegment(Point p, Point q, Point r) {
		return q.x <= Math.max(p.x, r.x) && q.x >= Math.min(p.x, r.x) && q.y <= Math.max(p.y, r.y) && q.y >= Math.min(p.y, r.y);
	}
}
