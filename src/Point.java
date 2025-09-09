// 1. 資料結構：點

import java.util.Objects;

public class Point {
	double x;
	double y;

	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double distanceTo(Point other) {
		return Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2));
	}

	@Override
	public String toString() {
		return "(" + String.format("%.1f", x) + ", " + String.format("%.1f", y) + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Point point = (Point) o;
		return Double.compare(point.x, x) == 0 && Double.compare(point.y, y) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y);
	}
}