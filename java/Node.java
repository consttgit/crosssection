import java.util.ArrayList;


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
        if (!links.contains(node)) {
            links.add(node);    
        }
    }

    public void disconnect(Node node) {
        links.remove(node);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Node node : this.links) {
            sb.append(node.point).append(", ");
        }
        String points = sb.toString();
        return String.format("node: %s linked with: %s", this.point, points);
    }
}
