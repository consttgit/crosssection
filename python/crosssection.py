import math


class CrossSection(object):
    
    def __init__(self, nodes):
        self.__nodes = self.__connect(nodes)
        self.__section_area = 0.0  # F
        self.__sectorial_static_moment = 0.0  # Sw
        self.__sectorial_inertia_moment = 0.0  # Iw
        self.__sectorial_linear_static_moment = None  # Swx,y (Point)
        self.__inertia_moment = None  # Ix,y (Point)
        self.__gravity_center = None  # Point
        self.__rigidity_center = None  # Point
        self.__pole = None  # Point

    def get_section_area(self, lazy=True):
        """Return a total area of the cross section.
        """
        if self.__section_area > 0 and lazy:
            return self.__section_area

        self.__section_area = 0.0
        self.__traverse_nodes(self.__nodes[0], self.__section_area_callback)

        return self.__section_area

    def __section_area_callback(self, node):
        if node.parent is None: return

        ds = node.distance_to(node.parent)
        thickness = 0.5 * (node.thickness + node.parent.thickness)
        self.__section_area += thickness * ds

    def get_gravity_center(self, lazy=True):
        """Return a point where the center of gravity is located.
        """
        if self.__gravity_center and lazy:
            return self.__gravity_center

        self.__gravity_center = Point()
        self.__traverse_nodes(self.__nodes[0], self.__gravity_center_callback)

        self.__gravity_center.x /= self.get_section_area()
        self.__gravity_center.y /= self.get_section_area()

        return self.__gravity_center

    def __gravity_center_callback(self, node):
        if node.parent is None: return

        ds = node.distance_to(node.parent)
        thickness = 0.5 * (node.thickness + node.parent.thickness)

        self.__gravity_center.x += 0.5 * (
            node.x + node.parent.x
        ) * thickness * ds

        self.__gravity_center.y += 0.5 * (
            node.y + node.parent.y
        ) * thickness * ds

    def get_inertia_moment(self, lazy=True):
        """Return a point whose coordinates represent main moments of inertia
        calculated for the corresponding axis (Ix,y).
        """
        if self.__inertia_moment and lazy:
            return self.__inertia_moment

        self.__inertia_moment = Point()
        self.__traverse_nodes(self.__nodes[0], self.__inertia_moment_callback)

        sa = self.get_section_area()
        gc = self.get_gravity_center()

        self.__inertia_moment.x -= sa * gc.y**2;
        self.__inertia_moment.y -= sa * gc.x**2;

        return self.__inertia_moment

    def __inertia_moment_callback(self, node):
        if node.parent is None: return

        ds = node.distance_to(node.parent)
        thickness = 0.5 * (node.thickness + node.parent.thickness)

        self.__inertia_moment.x += 0.5 * (
            node.y**2 + node.parent.y**2
        ) * thickness * ds

        self.__inertia_moment.y += 0.5 * (
            node.x**2 + node.parent.x**2
        ) * thickness * ds

    def get_polar_inertia_moment(self, lazy=True):
        """Return a polar moment of inertia (Ip).
        """
        return (self.get_inertia_moment(lazy).x
                + self.get_inertia_moment(lazy).y)

    def __get_area_sign(self, start_point, end_point):
        """Return a sign of the sectorial area defined by given two points and
        a reference origin.
        """
        sign = lambda x: (x > 0) - (x < 0)
        return sign(
            self.__get_angle(end_point) - self.__get_angle(start_point)
        )

    def __get_angle(self, point):
        """Return an angle between X-axis and a radius vector defined by a
        given point and the reference origin.
        """
        angle = math.degrees(math.atan2(point.y, point.x))

        if round(angle, 2) < 0:
            angle += 360

        return angle

    def __get_triangle_area(self, a, b, c):
        """Return an area of a triangle formed by given three points using the
        Heron's formula.
        """
        ab = a.distance_to(b)
        bc = b.distance_to(c)
        ca = c.distance_to(a)
        p = (ab + bc + ca) / 2
        return (p*(p - ab)*(p - bc)*(p - ca))**0.5

    def __update_sectorial_area(self, root_node, pole):
        """Update values of the sectorial area in nodes given a root node to
        start from and a pole point.
        """
        for node in self.__nodes:
            node.sectorial_area = 0.0
        self.__pole = pole
        self.__traverse_nodes(root_node, self.__update_sectorial_area_callback)

    def __update_sectorial_area_callback(self, node):
        if node.parent is None: return

        node_pos = Point(
            node.x - self.__pole.x,
            node.y - self.__pole.y
        )

        node_parent_pos = Point(
            node.parent.x - self.__pole.x,
            node.parent.y - self.__pole.y
        )

        origin_pos = Point()

        area_sign = self.__get_area_sign(node_parent_pos, node_pos)
        area = self.__get_triangle_area(node_parent_pos, node_pos, origin_pos)
        area_inc = area_sign * area * 2

        node.sectorial_area = node.parent.sectorial_area + area_inc

    def get_sectorial_static_moment(self, root_node, pole):
        """Return a sectorial static moment of inertia (Sw).
        """
        self.__sectorial_static_moment = 0.0
        self.__update_sectorial_area(root_node, pole)
        self.__traverse_nodes(root_node, self.__sectorial_static_moment_callback)
        return self.__sectorial_static_moment

    def __sectorial_static_moment_callback(self, node):
        if node.parent is None: return

        node_pos = Point(
            node.x - self.__pole.x,
            node.y - self.__pole.y
        )

        node_parent_pos = Point(
            node.parent.x - self.__pole.x,
            node.parent.y - self.__pole.y
        )

        ds = node_pos.distance_to(node_parent_pos)
        thickness = 0.5 * (node.thickness + node.parent.thickness)

        self.__sectorial_static_moment += 0.5 * (
            node.sectorial_area + node.parent.sectorial_area
        ) * thickness * ds

    def get_sectorial_linear_static_moment(self, root_node, pole):
        """Return a point whose coordinates represent sectorial linear static
        moments of inertia calculated for the corresponding axes (Swx,y).
        """
        self.__sectorial_linear_static_moment = Point()
        self.__update_sectorial_area(root_node, pole)
        self.__traverse_nodes(root_node, self.__sectorial_linear_static_moment_callback)
        return self.__sectorial_linear_static_moment

    def __sectorial_linear_static_moment_callback(self, node):
        if node.parent is None: return

        node_pos = Point(
            node.x - self.get_gravity_center().x,
            node.y - self.get_gravity_center().y
        )

        node_parent_pos = Point(
            node.parent.x - self.get_gravity_center().x,
            node.parent.y - self.get_gravity_center().y
        )

        ds = node_pos.distance_to(node_parent_pos)
        thickness = 0.5 * (node.thickness + node.parent.thickness)

        self.__sectorial_linear_static_moment.x += 0.5 * (
            node_pos.y * node.sectorial_area +
            node_parent_pos.y * node.parent.sectorial_area
        ) * thickness * ds

        self.__sectorial_linear_static_moment.y += 0.5 * (
            node_pos.x * node.sectorial_area +
            node_parent_pos.x * node.parent.sectorial_area
        ) * thickness * ds

    def get_rigidity_center(self, lazy=True):
        """Return a point where the rigidity center is located.
        """
        if self.__rigidity_center and lazy:
            return self.__rigidity_center

        pole = Point()

        inertia_moment = self.get_inertia_moment()
        sectorial_linear_static_moment = \
            self.get_sectorial_linear_static_moment(self.__nodes[0], pole)

        self.__rigidity_center = Point(
            pole.x + sectorial_linear_static_moment.x / inertia_moment.x,
            pole.y - sectorial_linear_static_moment.y / inertia_moment.y
        )

        return self.__rigidity_center

    def get_sectorial_inertia_moment(self, lazy=True):
        """Return a sectorial moment of inertia (Iw).
        """
        if self.__sectorial_inertia_moment and lazy:
            return self.__sectorial_inertia_moment

        root_node = None
        pole = self.get_rigidity_center()

        s_min = float('inf')
        for node in self.__nodes:
            ssm = self.get_sectorial_static_moment(node, pole)
            slsm = self.get_sectorial_linear_static_moment(node, pole)
            s = abs(ssm) + abs(slsm.x) + abs(slsm.y)
            if s < s_min:
                s_min = s
                root_node = node

        self.__update_sectorial_area(root_node, pole)

        self.__sectorial_inertia_moment = 0.0
        self.__traverse_nodes(root_node, self.__sectorial_inertia_moment_callback)

        return self.__sectorial_inertia_moment

    def __sectorial_inertia_moment_callback(self, node):
        if node.parent is None: return

        node_pos = Point(
            node.x - self.get_rigidity_center().x,
            node.y - self.get_rigidity_center().y
        )

        node_parent_pos = Point(
            node.parent.x - self.get_rigidity_center().x,
            node.parent.y - self.get_rigidity_center().y
        )

        ds = node_pos.distance_to(node_parent_pos)
        thickness = 0.5 * (node.thickness + node.parent.thickness)

        self.__sectorial_inertia_moment += 0.5 * (
            node.sectorial_area**2 + node.parent.sectorial_area**2        
        ) * thickness * ds

    def __traverse_nodes(self, root_node, callback):
        """Traverse nodes using DFS invoking at each step a callback function
        which receives the current node as its parameter.
        """
        for node in self.__nodes:
            node.parent = None

        visited_nodes = []
        nodes_to_visit = [root_node]

        while nodes_to_visit:
            cur_node = nodes_to_visit.pop()
            for node in cur_node.links:
                if node not in visited_nodes:
                    node.parent = cur_node
                    nodes_to_visit.append(node)
            callback(cur_node)
            visited_nodes.append(cur_node)

    def __connect(self, nodes):
        """Connect nodes in a given list. That is, build a minimum spanning
        tree (MST) using a greedy algorithm.
        """
        disconnected_nodes = nodes[:]
        connected_nodes = [disconnected_nodes.pop()]

        while disconnected_nodes:
            node_pair = {
                'dist': float('inf'),
                'd_node': None,
                'c_node': None
            }
            for c_node in connected_nodes:
                for d_node in disconnected_nodes:
                    dist = d_node.distance_to(c_node)
                    if dist < node_pair['dist']:
                        node_pair['dist'] = dist
                        node_pair['d_node'] = d_node
                        node_pair['c_node'] = c_node

            node_pair['c_node'].connect(node_pair['d_node'])
            node_pair['d_node'].connect(node_pair['c_node'])

            connected_nodes.append(node_pair['d_node'])
            disconnected_nodes.remove(node_pair['d_node'])

        return connected_nodes


class Point(object):

    def __init__(self, x=0.0, y=0.0):
        self.x = x
        self.y = y

    def distance_to(self, other):
        return ((other.x - self.x)**2 + (other.y - self.y)**2)**0.5

    def __str__(self):
        return '({x:.2f}, {y:.2f})'.format(x=self.x, y=self.y)

    def __repr__(self):
        return '{cls}(x={x:.2f}, y={y:.2f})'.format(
            cls=self.__class__.__name__, x=self.x, y=self.y
        )


class Node(Point):

    def __init__(self, x=0.0, y=0.0, thickness=1.0):
        super(Node, self).__init__(x, y)
        self.thickness = thickness
        self.sectorial_area = 0.0
        self.links = []
        self.parent = None

    def connect(self, node):
        if node not in self.links:
            self.links.append(node)

    def disconnect(self, node):
        if node in self.links:
            self.links.remove(node)

    def __str__(self):
        return 'node: {p} linked with: {ps}'.format(
            p=super(Node, self).__str__(),
            ps=', '.join([super(Node, node).__str__() for node in self.links])
        )

    def __repr__(self):
        return '{cls}(x={x:.2f}, y={y:.2f}, thickness={thickness})'.format(
            cls=self.__class__.__name__, x=self.x, y=self.y,
            thickness=self.thickness
        )
