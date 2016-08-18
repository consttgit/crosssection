public class Node {
    public Point point;
    public double thickness;
    public double sectorialArea = 0.0;
    public ArrayList<Node> links = null;
    public Node parent = null;

    public Node(Point point, double thickness) {
        this.point = point;
        this.thickness = thickness;
    }

    public Node(Point point) {
        this(point, 1.0);
    }

    public void connect(Node node) {
        throw new NotImplementedException();
    }

    public void disconnect(Node node) {
        throw new NotImplementedException();
    }

    public String toString() {
        throw new NotImplementedException();
    }
}
