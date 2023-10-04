import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class Program {
    private static Graph readFromFile(String path) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(path));
            int[] vertices = new int[lines.size()];
            List<Edge> edges = new ArrayList<>();
            for (int i = 0; i < vertices.length; i++) {
                vertices[i] = i + 1;
                char[] line = lines.get(i).toCharArray();
                for (int j = 0; j < line.length; j++) {
                    if (line[j] == '1') {
                        edges.add(new Edge(i + 1, j + 1));
                    }
                }
            }
            return new Graph(vertices, edges);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        Graph graph = readFromFile("graph6.txt");
        graph.print("Исходный граф");
        System.out.println();
        Graph decomposedGraph = graph.decompose();
        System.out.println();
        decomposedGraph.print("Декомпозированный граф");
    }

}

class Edge {
    private final int source;
    private final int dest;

    public Edge(int source, int dest) {
        this.source = source;
        this.dest = dest;
    }

    public int getSource() {
        return source;
    }

    public int getDest() {
        return dest;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Edge)) {
            return false;
        }

        Edge e = (Edge) o;

        return Integer.compare(source, e.getSource()) == 0
                && Integer.compare(dest, e.getDest()) == 0;
    }
}

class Graph {
    private class Subgraph {
        private final int index;

        private final List<Integer> vertices;

        public Subgraph(List<Integer> _vertices) {
            index = _vertices.get(0);
            vertices = _vertices;
        }

        public int getIndex() {
            return index;
        }

        public List<Integer> getVertices() {
            Collections.sort(vertices);
            return vertices;
        }
    }

    private final List<Edge> edges;
    private final List<Integer> vertices;

    public Graph(int[] vertices, List<Edge> edges) {
        this.vertices = new ArrayList<>();
        for (int vertex : vertices) {
            this.vertices.add(vertex);
        }
        this.edges = edges;
    }

    public void print(String text) {
        System.out.println(text);
        for (int vert : vertices) {
            List<Integer> outSeq = new ArrayList<>();
            for (Edge e : edges) {
                if (e.getSource() == vert) {
                    outSeq.add(e.getDest());
                }
            }
            Collections.sort(outSeq);
            System.out.printf("G(%d) = %s%n", vert, outSeq.isEmpty() ? "0" : String.join(", ", outSeq.toString()));
        }
    }

    public Graph decompose() {
        List<Subgraph> subgraphes = new ArrayList<>();
        List<Integer> accessVerts = vertices;
        Subgraph currentSubgraph;
        System.out.println("Сильно связные подграфы");
        while (!accessVerts.isEmpty()) {
            var vertex = accessVerts.get(0);
            var R = findR(vertex);
            var Q = findQ(vertex);
            currentSubgraph = new Subgraph(R.stream()
                    .distinct().filter(Q::contains).collect(Collectors.toList()));

            System.out.println("V(" + currentSubgraph.getIndex() + ") = " + Arrays.toString(currentSubgraph.getVertices().toArray()));

            if (currentSubgraph.getVertices().isEmpty()) {
                var lastSubgraph = new Subgraph(accessVerts);
                subgraphes.add(lastSubgraph);
                break;
            }
            subgraphes.add(currentSubgraph);
            accessVerts.removeAll(currentSubgraph.getVertices());
        }

        List<Edge> newEdges = new ArrayList<Edge>();
        int[] newVerts = new int[subgraphes.size()];
        int i = 0;
        for (Subgraph subgraph : subgraphes) {
            newVerts[i] = subgraph.getIndex();
            i++;
            List<Edge> outEdges = new ArrayList<>();
            for (Edge e : edges) {
                if (subgraph.vertices.contains(e.getSource()) && !subgraph.vertices.contains(e.getDest())) {
                    outEdges.add(e);
                }
            }

            var otherSubgraphs = subgraphes.stream()
                    .filter(j -> subgraph != j)
                    .collect(Collectors.toList());

            for (Edge edge : outEdges) {
                int inIndex = -1;
                for (Subgraph s : otherSubgraphs) {
                    if (s.vertices.contains(edge.getDest())) {
                        inIndex = s.getIndex();
                        break;
                    }
                }

                if (inIndex != -1) {
                    var newEdge = new Edge(subgraph.getIndex(), inIndex);
                    if (!newEdges.contains(newEdge)) {
                        newEdges.add(newEdge);
                    }
                }
            }
        }

        return new Graph(newVerts, newEdges.stream()
                .distinct()
                .collect(Collectors.toList()));
    }

    private List<Integer> findR(int start) {
        List<Integer> resultSet = new ArrayList<Integer>() {
            {
                add(start);
            }
        };

        List<Integer> achieveVerts = new ArrayList<Integer>();
        for (int i = 0; i < resultSet.size(); i++) {
            List<Integer> addedVerts = new ArrayList<Integer>();
            do {
                for (Edge e : edges) {
                    if (e.getSource() == resultSet.get(i)) {
                        achieveVerts.add(e.getDest());
                    }
                }
                achieveVerts.removeAll(addedVerts);
                resultSet.addAll(achieveVerts);
                resultSet = resultSet.stream().distinct().collect(Collectors.toList());
                addedVerts.addAll(achieveVerts);
                addedVerts = addedVerts.stream().distinct().collect(Collectors.toList());
            } while (!achieveVerts.isEmpty());
        }
        return resultSet.stream().distinct().collect(Collectors.toList());
    }

    private List<Integer> findQ(int end) {
        List<Integer> resultSet = new ArrayList<Integer>() {
            {
                add(end);
            }
        };

        List<Integer> achieveVerts = new ArrayList<Integer>();
        for (int i = 0; i < resultSet.size(); i++) {
            List<Integer> addedVerts = new ArrayList<Integer>();
            do {
                for (Edge e : edges) {
                    if (e.getDest() == resultSet.get(i)) {
                        achieveVerts.add(e.getSource());
                    }
                }
                achieveVerts.removeAll(addedVerts);
                resultSet.addAll(achieveVerts);
                resultSet = resultSet.stream().distinct().collect(Collectors.toList());
                addedVerts.addAll(achieveVerts);
                addedVerts = addedVerts.stream().distinct().collect(Collectors.toList());

            } while (!achieveVerts.isEmpty());
        }
        return resultSet.stream().distinct().collect(Collectors.toList());
    }
}