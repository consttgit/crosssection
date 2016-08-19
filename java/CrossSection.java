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
        traverseNodes(this.nodes.get(0), "sectionAreaCallback");

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
        traverseNodes(this.nodes.get(0), "gravityCenterCallback");

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



    private void traverseNodes(Node rootNode, String methodName) {
        Method callback = null;

        try {
            callback = this.getClass().getMethod(methodName, Node.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return;
        }

        // ...

        try {
            callback.invoke(this, this.nodes.get(0));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
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
}
