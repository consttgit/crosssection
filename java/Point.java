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

    public double distanceTo(Point other) {
        return Math.sqrt(
            Math.pow(other.x - this.x, 2) + Math.pow(other.y - this.y, 2)
        );
    }

    public String toString() {
        return String.format("(%.2f, %.2f)", this.x, this.y);
    }

    public static int getNum() {
        return num;
    }

    protected void finalize() throws Throwable {
        --num;
        super.finalize();
    }
}
