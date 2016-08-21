import java.util.ArrayList;


public class CrossSectionTest {

    public static void main(String[] args) {
        final double THICKNESS = 4.4;
        Node[] nodes = new Node[] {
            new Node(32.0, -25.0, THICKNESS),
            new Node(28.44, -25.0, THICKNESS),
            new Node(24.89, -25.0, THICKNESS),
            new Node(21.33, -25.0, THICKNESS),
            new Node(17.78, -25.0, THICKNESS),
            new Node(14.22, -25.0, THICKNESS),
            new Node(10.67, -25.0, THICKNESS),
            new Node(7.11, -25.0, THICKNESS),
            new Node(3.56, -25.0, THICKNESS),
            new Node(0.0, -25.0, THICKNESS),
            new Node(0.0, -21.15, THICKNESS),
            new Node(0.0, -17.31, THICKNESS),
            new Node(0.0, -13.46, THICKNESS),
            new Node(0.0, -9.62, THICKNESS),
            new Node(0.0, -5.77, THICKNESS),
            new Node(0.0, -1.92, THICKNESS),
            new Node(0.0, 0.0, THICKNESS),
            new Node(0.0, 1.92, THICKNESS),
            new Node(0.0, 5.77, THICKNESS),
            new Node(0.0, 9.62, THICKNESS),
            new Node(0.0, 13.46, THICKNESS),
            new Node(0.0, 17.31, THICKNESS),
            new Node(0.0, 21.15, THICKNESS),
            new Node(0.0, 25.0, THICKNESS),
            new Node(3.56, 25.0, THICKNESS),
            new Node(7.11, 25.0, THICKNESS),
            new Node(10.67, 25.0, THICKNESS),
            new Node(14.22, 25.0, THICKNESS),
            new Node(17.78, 25.0, THICKNESS),
            new Node(21.33, 25.0, THICKNESS),
            new Node(24.89, 25.0, THICKNESS),
            new Node(28.44, 25.0, THICKNESS),
            new Node(32.0, 25.0, THICKNESS)
        };

        CrossSection cs = new CrossSection(nodes);

        System.out.printf("** Sectorial properties:\n");
        System.out.printf("-- Section area (F): %.2f mm^2\n", 
                          cs.getSectionArea());
        System.out.printf("-- Center of gravity (x, y): %s mm\n",
                          cs.getGravityCenter());
        System.out.printf("-- Main moments of inertia (Ix, Iy): %s mm^4\n",
                          cs.getInertiaMoment());
        System.out.printf("-- Polar moment of inertia (Ip): %.2f mm^4\n",
                          cs.getPolarInertiaMoment());
        System.out.printf("-- Center of rigidity (x, y): %s mm\n",
                          cs.getRigidityCenter());
        System.out.printf("-- Sectorial moment of inertia (Iw): %.2f mm^6\n",
                          cs.getSectorialInertiaMoment());
    }
}
