public class Point {
    public double x;
    public double y;
    private static int num = 0;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
        ++num;
    }

    public Point() {
        this(0.0, 0.0);
    }

    public double distance(Point other) {
        return Math.sqrt(
            Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2)
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof Point)) return false;
        Point point = (Point)obj;
        return ((this.x == point.x) && (this.y == point.y));
    }

    public String toString() {
        return String.format("(%.2f, %.2f)", this.x, this.y);
    }

    public static int getNum() {
        return num;
    }
}
