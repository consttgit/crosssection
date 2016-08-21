import java.util.Stack;
import java.util.Arrays;
import java.util.ArrayList;


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

    public CrossSection(Node[] nodes) {
        this.nodes = connect(nodes);
    }

    /**
     * Return a total area of the cross section.
     *
     */
    public double getSectionArea(boolean lazy) {
        if (sectionArea > 0 && lazy) {
            return sectionArea;
        }

        sectionArea = 0.0;
        traverseNodes(nodes.get(0),
                      (Node node) -> sectionAreaCallback(node));

        return sectionArea;
    }

    public double getSectionArea() {
        return getSectionArea(true);
    }

    private void sectionAreaCallback(Node node) {
        if (node.parent == null) return;

        double ds = node.distanceTo(node.parent);
        double thickness = 0.5 * (node.thickness + node.parent.thickness);
        sectionArea += thickness * ds;
    }

    /**
     * Return a point where the center of gravity is located.
     *
     */
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

    public Point getGravityCenter() {
        return getGravityCenter(true);
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

    /**
     * Return a point whose coordinates represent main moments of inertia
     * calculated for the corresponding axis (Ix,y).
     *
     */
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

    public Point getInertiaMoment() {
        return getInertiaMoment(true);
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

    /**
     * Return a polar moment of inertia (Ip).
     *
     */
    public double getPolarInertiaMoment(boolean lazy) {
        return getInertiaMoment(lazy).x + getInertiaMoment(lazy).y;
    }

    public double getPolarInertiaMoment() {
        return getInertiaMoment(true).x + getInertiaMoment(true).y;
    }

    /**
     * Return a sign of the sectorial area defined by given two points and a
     * reference origin.
     *
     */
    private double getAreaSign(Point start, Point end) {
        return Math.signum(getAngle(end) - getAngle(start));
    }

    /**
     * Return an angle between X-axis and a radius vector defined by a given
     * point and the reference origin.
     *
     */
    private double getAngle(Point p) {
        double angle = Math.toDegrees(Math.atan2(p.y, p.x));
        angle = (double) Math.round(angle * 100) / 100;

        if (angle < 0.0) {
            angle += 360.0;
        }

        return angle;
    }

    /**
     * Return an area of a triangle formed by given three points using the
     * Heron's formula.
     *
     */
    private double getTriangleArea(Point a, Point b, Point c) {
        double ab = a.distanceTo(b);
        double bc = b.distanceTo(c);
        double ca = c.distanceTo(a);
        double p = (ab + bc + ca) / 2.0;
        return Math.sqrt(p*(p - ab)*(p - bc)*(p - ca));
    }

    /**
     * Update values of the sectorial area in nodes given a root node to start
     * from and a pole point.
     *
     */
    private void updateSectorialArea(Node rootNode, Point pole) {
        for (Node node : nodes) {
            node.sectorialArea = 0.0;
        }
        this.pole = pole;
        traverseNodes(rootNode,
                      (Node node) -> updateSectorialAreaCallback(node));
    }

    private void updateSectorialAreaCallback(Node node) {
        if (node.parent == null) return;

        Point nodePos = new Point(
            node.x - pole.x,
            node.y - pole.y
        );

        Point nodeParentPos = new Point(
            node.parent.x - pole.x,
            node.parent.y - pole.y
        );

        Point originPos = new Point();

        double areaSign = getAreaSign(nodeParentPos, nodePos);
        double area = getTriangleArea(nodeParentPos, nodePos, originPos);
        double areaInc = areaSign * area * 2.0;

        node.sectorialArea = node.parent.sectorialArea + areaInc;
    }

    /**
     * Return a sectorial static moment of inertia (Sw).
     *
     */
    public double getSectorialStaticMoment(Node rootNode, Point pole) {
        sectorialStaticMoment = 0.0;
        updateSectorialArea(rootNode, pole);
        traverseNodes(rootNode,
                      (Node node) -> sectorialStaticMomentCallback(node));
        return sectorialStaticMoment;
    }

    private void sectorialStaticMomentCallback(Node node) {
        if (node.parent == null) return;

        Point nodePos = new Point(
            node.x - pole.x,
            node.y - pole.y
        );

        Point nodeParentPos = new Point(
            node.parent.x - pole.x,
            node.parent.y - pole.y
        );

        double ds = nodePos.distanceTo(nodeParentPos);
        double thickness = 0.5 * (node.thickness + node.parent.thickness);

        sectorialStaticMoment += 0.5 * (
            node.sectorialArea + node.parent.sectorialArea
        ) * thickness * ds;
    }

    /**
     * Return a point whose coordinates represent sectorial linear static
     * moments of inertia calculated for the corresponding axes (Swx,y).
     *
     */
    public Point getSectorialLinearStaticMoment(Node rootNode, Point pole) {
        sectorialLinearStaticMoment = new Point();
        updateSectorialArea(rootNode, pole);
        traverseNodes(rootNode,
                      (Node node) -> sectorialLinearStaticMomentCallback(node));
        return sectorialLinearStaticMoment;
    }

    private void sectorialLinearStaticMomentCallback(Node node) {
        if (node.parent == null) return;

        Point nodePos = new Point(
            node.x - getGravityCenter().x,
            node.y - getGravityCenter().y
        );

        Point nodeParentPos = new Point(
            node.parent.x - getGravityCenter().x,
            node.parent.y - getGravityCenter().y
        );

        double ds = nodePos.distanceTo(nodeParentPos);
        double thickness = 0.5 * (node.thickness + node.parent.thickness);

        sectorialLinearStaticMoment.x += 0.5 * (
            nodePos.y * node.sectorialArea +
            nodeParentPos.y * node.parent.sectorialArea
        ) * thickness * ds;

        sectorialLinearStaticMoment.y += 0.5 * (
            nodePos.x * node.sectorialArea +
            nodeParentPos.x * node.parent.sectorialArea
        ) * thickness * ds;
    }

    /**
     * Return a point where the rigidity center is located.
     *
     */
    public Point getRigidityCenter(boolean lazy) {
        if (rigidityCenter != null && lazy) {
            return rigidityCenter;
        }

        Point pole = new Point();

        Point inertiaMoment = getInertiaMoment();
        Point sectorialLinearStaticMoment =
            getSectorialLinearStaticMoment(nodes.get(0), pole);

        rigidityCenter = new Point(
            pole.x + sectorialLinearStaticMoment.x / inertiaMoment.x,
            pole.y - sectorialLinearStaticMoment.y / inertiaMoment.y
        );

        return rigidityCenter;
    }

    public Point getRigidityCenter() {
        return getRigidityCenter(true);
    }

    /**
     * Return a sectorial moment of inertia (Iw).
     *
     */
    public double getSectorialInertiaMoment(boolean lazy) {
        if (sectorialInertiaMoment > 0.0 && lazy) {
            return sectorialInertiaMoment;
        }

        Node rootNode = null;
        Point pole = getRigidityCenter();

        double sMin = Double.POSITIVE_INFINITY;
        for (Node node : nodes) {
            double ssm = getSectorialStaticMoment(node, pole);
            Point slsm = getSectorialLinearStaticMoment(node, pole);
            double s = Math.abs(ssm) + Math.abs(slsm.x) + Math.abs(slsm.y);
            if (s < sMin) {
                sMin = s;
                rootNode = node;
            }
        }

        updateSectorialArea(rootNode, pole);

        sectorialInertiaMoment = 0.0;
        traverseNodes(rootNode,
                      (Node node) -> sectorialInertiaMomentCallback(node));

        return sectorialInertiaMoment;
    }

    public double getSectorialInertiaMoment() {
        return getSectorialInertiaMoment(true);
    }

    private void sectorialInertiaMomentCallback(Node node) {
        if (node.parent == null) return;

        Point nodePos = new Point(
            node.x - getRigidityCenter().x,
            node.y - getRigidityCenter().y
        );

        Point nodeParentPos = new Point(
            node.parent.x - getRigidityCenter().x,
            node.parent.y - getRigidityCenter().y
        );

        double ds = nodePos.distanceTo(nodeParentPos);
        double thickness = 0.5 * (node.thickness + node.parent.thickness);

        sectorialInertiaMoment += 0.5 * (
            Math.pow(node.sectorialArea, 2) +
            Math.pow(node.parent.sectorialArea, 2)
        ) * thickness * ds;
    }

    /**
     * Traverse nodes using DFS invoking at each step a callback function which
     * receives the current node as its parameter.
     *
     */
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

    /**
     * Connect nodes in a given list. That is, build a minimum spanning tree
     * (MST) using a greedy algorithm.
     *
     */
    private ArrayList<Node> connect(Node[] nodes) {
        ArrayList<Node> connectedNodes = new ArrayList<Node>();
        ArrayList<Node> disconnectedNodes = 
            new ArrayList<Node>(Arrays.asList(nodes));

        connectedNodes.add(disconnectedNodes.remove(nodes.length - 1));

        while (disconnectedNodes.size() > 0) {
            double minDist = Double.POSITIVE_INFINITY;
            Node dNode = null;
            Node cNode = null;

            for (Node c : connectedNodes) {
                for (Node d : disconnectedNodes) {
                    double dist = d.distanceTo(c);
                    if (dist < minDist) {
                        minDist = dist;
                        dNode = d;
                        cNode = c;
                    }
                }
            }

            cNode.connect(dNode);
            dNode.connect(cNode);

            connectedNodes.add(dNode);
            disconnectedNodes.remove(dNode);
        }

        return connectedNodes;
    }

    interface Callback {
        void invoke(Node node);
    }
}
