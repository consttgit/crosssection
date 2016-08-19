import java.util.ArrayList;


public class Node extends Point {
    public double thickness = 1.0;
    public double sectorialArea = 0.0;
    public ArrayList<Node> links = new ArrayList<Node>();
    public Node parent = null;

    public Node(double x, double y, double thickness) {
        super(x, y);
        this.thickness = thickness;
    }

    public Node(double x, double y) {
        super(x, y);
    }

    public Node() {
        super();
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
        sb.append(String.format("node: (%.2f, %.2f) linked with: ",
                  this.x, this.y));
        for (Node node : this.links) {
            sb.append(String.format("(%.2f, %.2f), ", node.x, node.y));
        }
        sb.append("\n");
        return sb.toString();
    }
}
