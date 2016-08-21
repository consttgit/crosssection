import java.util.Stack;
import java.util.ArrayList;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;


public class CrossSection {
    private ArrayList<Node> nodes = null;
    private double sectionArea = 0.0;
    private double sectorialStaticMoment = 0.0;
    private double sectorialInertiaMoment = 0.0;
    private Point sectorialLinearStaticMoment = null;
    private Point inertiaMoment = null;
    private Point gravityCenter = null;
    private Point rigidityCenter = null;
    private Point pole = null;

    public CrossSection(ArrayList<Node> nodes) {
        this.nodes = connect(nodes);
    }

    public double getSectionArea() {
        return getSectionArea(true);
    }

    public double getSectionArea(boolean lazy) {
        if (sectionArea > 0 && lazy) {
            return sectionArea;
        }

        sectionArea = 0.0;
        traverseNodes(nodes.get(0),
                      (Node node) -> sectionAreaCallback(node));

        return sectionArea;
    }

    private void sectionAreaCallback(Node node) {
        if (node.parent == null) return;

        double ds = node.distanceTo(node.parent);
        double thickness = 0.5 * (node.thickness + node.parent.thickness);
        sectionArea += thickness * ds;
    }

    public Point getGravityCenter() {
        return getGravityCenter(true);
    }

    public Point getGravityCenter(boolean lazy) {
        if (gravityCenter != null && lazy) {
            return gravityCenter;
        }

        gravityCenter = new Point();
        traverseNodes(nodes.get(0),
                      (Node node) -> gravityCenterCallback(node));

        gravityCenter.x /= getSectionArea();
        gravityCenter.y /= getSectionArea();

        return gravityCenter;
    }

    private void gravityCenterCallback(Node node) {
        if (node.parent == null) return;

        double ds = node.distanceTo(node.parent);
        double thickness = 0.5 * (node.thickness + node.parent.thickness);

        gravityCenter.x += 0.5 * (
            node.x + node.parent.x
        ) * thickness * ds;

        gravityCenter.y += 0.5 * (
            node.y + node.parent.y
        ) * thickness * ds;
    }

    public Point getInertiaMoment() {
        return getInertiaMoment(true);
    }

    public Point getInertiaMoment(boolean lazy) {
        if (inertiaMoment != null && lazy) {
            return inertiaMoment;
        }

        inertiaMoment = new Point();
        traverseNodes(nodes.get(0),
                      (Node node) -> inertiaMomentCallback(node));

        double sa = getSectionArea();
        Point gc = getGravityCenter();

        inertiaMoment.x -= sa * Math.pow(gc.y, 2);
        inertiaMoment.y -= sa * Math.pow(gc.x, 2);

        return inertiaMoment;
    }

    private void inertiaMomentCallback(Node node) {
        if (node.parent == null) return;

        double ds = node.distanceTo(node.parent);
        double thickness = 0.5 * (node.thickness + node.parent.thickness);

        inertiaMoment.x += 0.5 * (
            Math.pow(node.y, 2) + Math.pow(node.parent.y, 2)
        ) * thickness * ds;

        inertiaMoment.y += 0.5 * (
            Math.pow(node.x, 2) + Math.pow(node.parent.x, 2)
        ) * thickness * ds;
    }

    public double getPolarInertiaMoment() {
        return getInertiaMoment(true).x + getInertiaMoment(true).y;
    }

    public double getPolarInertiaMoment(boolean lazy) {
        return getInertiaMoment(lazy).x + getInertiaMoment(lazy).y;
    }

    private void traverseNodes(Node rootNode, Callback callback) {
        for (Node node : nodes) {
            node.parent = null;
        }

        ArrayList<Node> visitedNodes = new ArrayList<Node>();
        Stack<Node> nodesToVisit = new Stack<Node>();

        nodesToVisit.push(rootNode);

        while (!nodesToVisit.empty()) {
            Node curNode = nodesToVisit.pop();
            for (Node node : curNode.links) {
                if (!visitedNodes.contains(node)) {
                    node.parent = curNode;
                    nodesToVisit.push(node);
                }
            }
            callback.invoke(curNode);
            visitedNodes.add(curNode);
        }
    }

    private ArrayList<Node> connect(ArrayList<Node> nodes) {
        ArrayList<Node> connectedNodes = new ArrayList<Node>();
        ArrayList<Node> disconnectedNodes = new ArrayList<Node>(nodes);

        connectedNodes.add(disconnectedNodes.remove(nodes.size() - 1));

        while (disconnectedNodes.size() > 0) {
            double min_dist = Double.POSITIVE_INFINITY;
            Node d_node = null;
            Node c_node = null;

            for (Node c : connectedNodes) {
                for (Node d : disconnectedNodes) {
                    double dist = d.distanceTo(c);
                    if (dist < min_dist) {
                        min_dist = dist;
                        d_node = d;
                        c_node = c;
                    }
                }
            }

            c_node.connect(d_node);
            d_node.connect(c_node);

            connectedNodes.add(d_node);
            disconnectedNodes.remove(d_node);
        }

        return connectedNodes;
    }

    interface Callback {
        void invoke(Node node);
    }
}
