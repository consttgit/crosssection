import math


class CrossSection(object):
    
    def __init__(self, nodes):
        self.nodes = self.__connect(nodes)
        self.__section_area = 0.0  # F
        self.__sectorial_static_moment = 0.0  # Sw
        self.__sectorial_inertia_moment = 0.0  # Iw
        self.__sectorial_linear_static_moment = None  # Swx,y (Point)
        self.__inertia_moment = None  # Ix,y (Point)
        self.__gravity_center = None  # Point
        self.__rigidity_center = None  # Point
        self.__pole_point = None  # Point

    def get_section_area(self, lazy=True):
        """Return a total area of the cross section.
        """
        if self.__section_area > 0 and lazy:
            return self.__section_area

        self.__section_area = 0.0
        self.__traverse_nodes(self.nodes[0], self.__section_area_callback)

        return self.__section_area

    def __section_area_callback(self, node):
        if node.parent is None: return

        ds = self.__contour_increment(node.point, node.parent.point)
        thickness = 0.5 * (node.thickness + node.parent.thickness)
        self.__section_area += thickness * ds

    def get_gravity_center(self, lazy=True):
        """Return a point where the center of gravity is located.
        """
        if self.__gravity_center and lazy:
            return self.__gravity_center

        self.__gravity_center = Point()
        self.__traverse_nodes(self.nodes[0], self.__gravity_center_callback)

        self.__gravity_center.x /= self.get_section_area()
        self.__gravity_center.y /= self.get_section_area()

        return self.__gravity_center

    def __gravity_center_callback(self, node):
        if node.parent is None: return

        ds = self.__contour_increment(node.point, node.parent.point)
        thickness = 0.5 * (node.thickness + node.parent.thickness)

        self.__gravity_center.x += 0.5 * (
            node.point.x + node.parent.point.x
        ) * thickness * ds

        self.__gravity_center.y += 0.5 * (
            node.point.y + node.parent.point.y
        ) * thickness * ds

    def get_inertia_moment(self, lazy=True):
        """Return a point whose coordinates represent main moments of inertia
        calculated for the corresponding axis (Ix,y).
        """
        if self.__inertia_moment and lazy:
            return self.__inertia_moment

        self.__inertia_moment = Point()
        self.__traverse_nodes(self.nodes[0], self.__inertia_moment_callback)

        gc = self.get_gravity_center()
        sa = self.get_section_area()

        self.__inertia_moment.x -= sa * gc.y**2;
        self.__inertia_moment.y -= sa * gc.x**2;

        return self.__inertia_moment

    def __inertia_moment_callback(self, node):
        if node.parent is None: return

        ds = self.__contour_increment(node.point, node.parent.point)
        thickness = 0.5 * (node.thickness + node.parent.thickness)

        self.__inertia_moment.x += 0.5 * (
            node.point.y**2 + node.parent.point.y**2
        ) * thickness * ds

        self.__inertia_moment.y += 0.5 * (
            node.point.x**2 + node.parent.point.x**2
        ) * thickness * ds

    def get_polar_inertia_moment(self, lazy=True):
        return (self.get_inertia_moment(lazy).x
                + self.get_inertia_moment(lazy).y)

    def __contour_increment(self, cur_point, prev_point):
        """Return a linear approximation of a non-linear contour increment.
        """
        dx = cur_point.x - prev_point.x
        dy = cur_point.y - prev_point.y
        return (dx**2 + dy**2)**0.5

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

    def __get_triangle_area(self, a_point, b_point, c_point):
        """Return an area of a triangle formed by given three points using the
        Heron's formula.
        """
        ab = ((b_point.x - a_point.x)**2 + (b_point.y - a_point.y)**2)**0.5
        bc = ((c_point.x - b_point.x)**2 + (c_point.y - b_point.y)**2)**0.5
        ca = ((a_point.x - c_point.x)**2 + (a_point.y - c_point.y)**2)**0.5
        p = (ab + bc + ca) / 2
        return (p*(p - ab)*(p - bc)*(p - ca))**0.5

    def __update_sectorial_area(self, root_node, pole_point):
        """Update values of the sectorial area in nodes given a root node to
        start from and a pole point.
        """
        for node in self.nodes:
            node.sectorial_area = 0.0
        self.__pole_point = pole_point
        self.__traverse_nodes(root_node, self.__update_sectorial_area_callback)

    def __update_sectorial_area_callback(self, node):
        if node.parent is None: return

        start_point = Point(
            node.parent.point.x - self.__pole_point.x,
            node.parent.point.y - self.__pole_point.y
        )
        end_point = Point(
            node.point.x - self.__pole_point.x,
            node.point.y - self.__pole_point.y
        )
        center_point = Point()

        area_sign = self.__get_area_sign(start_point, end_point)
        area = self.__get_triangle_area(start_point, end_point, center_point)
        area_inc = area_sign * area * 2

        node.sectorial_area = node.parent.sectorial_area + area_inc

    def get_sectorial_static_moment(self, root_node, pole_point):
        """Return a sectorial static moment of inertia (Sw).
        """
        self.__sectorial_static_moment = 0.0
        self.__update_sectorial_area(root_node, pole_point)
        self.__traverse_nodes(root_node, self.__sectorial_static_moment_callback)
        return self.__sectorial_static_moment

    def __sectorial_static_moment_callback(self, node):
        if node.parent is None: return

        node_point = Point(
            node.point.x - self.__pole_point.x,
            node.point.y - self.__pole_point.y
        )
        node_parent_point = Point(
            node.parent.point.x - self.__pole_point.x,
            node.parent.point.y - self.__pole_point.y
        )

        ds = self.__contour_increment(node_point, node_parent_point)
        thickness = 0.5 * (node.thickness + node.parent.thickness)

        self.__sectorial_static_moment += 0.5 * (
            node.sectorial_area + node.parent.sectorial_area
        ) * thickness * ds

    def get_sectorial_linear_static_moment(self, root_node, pole_point):
        """Return a point whose coordinates represent sectorial linear static
        moments of inertia calculated for the corresponding axes (Swx,y).
        """
        self.__sectorial_linear_static_moment = Point()
        self.__update_sectorial_area(root_node, pole_point)
        self.__traverse_nodes(root_node, self.__sectorial_linear_static_moment_callback)
        return self.__sectorial_linear_static_moment

    def __sectorial_linear_static_moment_callback(self, node):
        if node.parent is None: return

        node_point = Point(
            node.point.x - self.get_gravity_center().x,
            node.point.y - self.get_gravity_center().y
        )
        node_parent_point = Point(
            node.parent.point.x - self.get_gravity_center().x,
            node.parent.point.y - self.get_gravity_center().y
        )

        ds = self.__contour_increment(node_point, node_parent_point)
        thickness = 0.5 * (node.thickness + node.parent.thickness)

        self.__sectorial_linear_static_moment.x += 0.5 * (
            node_point.y * node.sectorial_area +
            node_parent_point.y * node.parent.sectorial_area
        ) * thickness * ds

        self.__sectorial_linear_static_moment.y += 0.5 * (
            node_point.x * node.sectorial_area +
            node_parent_point.x * node.parent.sectorial_area
        ) * thickness * ds

    def get_rigidity_center(self, lazy=True):
        """Return a point where the rigidity center is located.
        """
        if self.__rigidity_center and lazy:
            return self.__rigidity_center

        pole_point = Point()
        sectorial_linear_static_moment = \
            self.get_sectorial_linear_static_moment(self.nodes[0], pole_point)
        inertia_moment = self.get_inertia_moment()

        self.__rigidity_center = Point(
            pole_point.x + sectorial_linear_static_moment.x / inertia_moment.x,
            pole_point.y - sectorial_linear_static_moment.y / inertia_moment.y
        )

        return self.__rigidity_center

    def get_sectorial_inertia_moment(self, lazy=True):
        """Return a sectorial moment of inertia (Iw).
        """
        if self.__sectorial_inertia_moment and lazy:
            return self.__sectorial_inertia_moment

        root_node = None
        pole_point = self.get_rigidity_center()

        s_min = float('inf')
        for node in self.nodes:
            ssm = self.get_sectorial_static_moment(node, pole_point)
            slsm = self.get_sectorial_linear_static_moment(node, pole_point)
            s = abs(ssm) + abs(slsm.x) + abs(slsm.y)
            if s < s_min:
                s_min = s
                root_node = node

        self.__update_sectorial_area(root_node, pole_point)

        self.__sectorial_inertia_moment = 0.0
        self.__traverse_nodes(root_node, self.__sectorial_inertia_moment_callback)

        return self.__sectorial_inertia_moment

    def __sectorial_inertia_moment_callback(self, node):
        if node.parent is None: return

        node_point = Point(
            node.point.x - self.get_rigidity_center().x,
            node.point.y - self.get_rigidity_center().y
        )
        node_parent_point = Point(
            node.parent.point.x - self.get_rigidity_center().x,
            node.parent.point.y - self.get_rigidity_center().y
        )

        ds = self.__contour_increment(node_point, node_parent_point)
        thickness = 0.5 * (node.thickness + node.parent.thickness)

        self.__sectorial_inertia_moment += 0.5 * (
            node.sectorial_area**2 + node.parent.sectorial_area**2        
        ) * thickness * ds

    def __traverse_nodes(self, root_node, callback):
        """Traverse nodes using DFS invoking at each step a callback function
        which receives the current node as its parameter.
        """
        for node in self.nodes:
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
                    dist = d_node.point.dist_to(c_node.point)
                    if dist < node_pair['dist']:
                        node_pair['dist'] = dist
                        node_pair['d_node'] = d_node
                        node_pair['c_node'] = c_node

            node_pair['c_node'].connect(node_pair['d_node'])
            node_pair['d_node'].connect(node_pair['c_node'])

            connected_nodes.append(node_pair['d_node'])
            disconnected_nodes.remove(node_pair['d_node'])

        return connected_nodes


class Node(object):

    def __init__(self, point, thickness=1.0):
        self.point = point
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
        return 'node: {point} linked with: {points}'.format(
            point=self.point,
            points=', '.join([str(node.point) for node in self.links])
        )


class Point(object):

    def __init__(self, x=0.0, y=0.0):
        self.x = x
        self.y = y

    def dist_to(self, other):
        return ((self.x - other.x)**2 + (self.y - other.y)**2)**0.5

    def __eq__(self, other):
        return (isinstance(other, self.__class__) and self.x == other.x and
            self.y == other.y)

    def __ne__(self, other):
        return not self.__eq__(other)

    def __str__(self):
        return '({x:.2f}, {y:.2f})'.format(x=self.x, y=self.y)