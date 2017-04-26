package src;

import junit.framework.TestCase;
import knox.spring.data.neo4j.domain.DesignSpace;
import knox.spring.data.neo4j.domain.Edge;
import knox.spring.data.neo4j.domain.EnumerateType;
import knox.spring.data.neo4j.domain.Node;
import knox.spring.data.neo4j.sample.DesignSampler;
import org.parboiled.common.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Created by admin on 4/25/17.
 */

public class DesignSamplerTests extends TestCase {
    private static final Logger LOG = LoggerFactory.getLogger(DesignSamplerTests.class);
    private DesignSampler designSampler;
    private DesignSampler complexDesignSampler;

    protected void setUp() {
        setUpDesignSampler();
        setUpComplexDesignSampler();
    }

    private void  setUpComplexDesignSampler() {
        Node n1 = new Node("n1", "start");
        Node n2 = new Node("n2", "");
        Node n3 = new Node("n3", "");
        Node n4 = new Node("n4", "");
        Node n5 = new Node("n5", "");
        Node n6 = new Node("n6", "");
        Node n7 = new Node("n7", "");
        Node n8 = new Node("n8", "");
        Node n9 = new Node("n9", "");
        Node n10 = new Node("n10", "");
        Node n11 = new Node("n11", "");
        Node n12 = new Node("n12", "");
        Node n13 = new Node("n13", "end");

        ArrayList<String> emptylist = new ArrayList<>();

        ArrayList<String> e1Ids = new ArrayList<>(Arrays.asList(new String[] {"p1"}));
        ArrayList<String> e1Roles = new ArrayList<>(Arrays.asList(new String[] {"promoter"}));
        Edge e1 = new Edge(n1, n2, e1Ids, e1Roles);

        Edge e2 = new Edge(n2, n3, emptylist, emptylist);

        ArrayList<String> e3Ids = new ArrayList<>(Arrays.asList(new String[] {"p2"}));
        ArrayList<String> e3Roles = new ArrayList<>(Arrays.asList(new String[] {"promoter"}));
        Edge e3 = new Edge(n3, n4, e3Ids, e3Roles);

        Edge e4 = new Edge(n4, n5, emptylist, emptylist);

        ArrayList<String> e5Ids = new ArrayList<>(Arrays.asList(new String[] {"rz1"}));
        ArrayList<String> e5Roles = new ArrayList<>(Arrays.asList(new String[] {"ribozyme"}));
        Edge e5 = new Edge(n5, n6, e5Ids, e5Roles);

        Edge e6 = new Edge(n6, n7, emptylist, emptylist);

        ArrayList<String> e7Ids = new ArrayList<>(Arrays.asList(new String[] {"rb1"}));
        ArrayList<String> e7Roles = new ArrayList<>(Arrays.asList(new String[] {"ribosome_entry_site"}));
        Edge e7 = new Edge(n7, n8, e7Ids, e7Roles);

        Edge e8 = new Edge(n8, n9, emptylist, emptylist);

        ArrayList<String> e9Ids = new ArrayList<>(Arrays.asList(new String[] {"c1"}));
        ArrayList<String> e9Roles = new ArrayList<>(Arrays.asList(new String[] {"CDS"}));
        Edge e9 = new Edge(n9, n10, e9Ids, e9Roles);

        Edge e10 = new Edge(n10, n11, emptylist, emptylist);

        ArrayList<String> e11Ids = new ArrayList<>(Arrays.asList(new String[] {"t1"}));
        ArrayList<String> e11Roles = new ArrayList<>(Arrays.asList(new String[] {"terminator"}));
        Edge e11 = new Edge(n11, n13, e11Ids, e11Roles);

        n1.addEdge(e1);
        n2.addEdge(e2);
        n3.addEdge(e3);
        n4.addEdge(e4);
        n5.addEdge(e5);
        n6.addEdge(e6);
        n7.addEdge(e7);
        n8.addEdge(e8);
        n9.addEdge(e9);
        n10.addEdge(e10);
        n11.addEdge(e11);

        DesignSpace designSpace = new DesignSpace("space id");
        designSpace.addNode(n1);
        designSpace.addNode(n2);
        designSpace.addNode(n3);
        designSpace.addNode(n4);
        designSpace.addNode(n5);
        designSpace.addNode(n6);
        designSpace.addNode(n6);
        designSpace.addNode(n7);
        designSpace.addNode(n8);
        designSpace.addNode(n9);
        designSpace.addNode(n10);
        designSpace.addNode(n11);
        designSpace.addNode(n12);
        designSpace.addNode(n13);

        complexDesignSampler = new DesignSampler(designSpace);
    }

    private void  setUpDesignSampler() {
        Node nodeA = new Node("node a", "start");
        Node nodeB = new Node("node b", "");
        Node nodeC = new Node("node c", "");
        Node nodeD = new Node("node d", "end");

        ArrayList<String> edgeAIds = new ArrayList<>(Arrays.asList(new String[] {"id 1", "id 2"}));
        ArrayList<String> edgeARoles = new ArrayList<>(Arrays.asList(new String[] {"role 1", "role 2"}));
        Edge edgeA = new Edge(nodeA, nodeB, edgeAIds, edgeARoles, 0.5);

        ArrayList<String> edgeBIds = new ArrayList<>(Arrays.asList(new String[] {"id 3", "id 4"}));
        ArrayList<String> edgeBRoles = new ArrayList<>(Arrays.asList(new String[] {"role 3", "role 4"}));
        Edge edgeB = new Edge(nodeB, nodeC, edgeBIds, edgeBRoles, 0.5);

        ArrayList<String> edgeCIds = new ArrayList<>(Arrays.asList(new String[] {"id 5", "id 6"}));
        ArrayList<String> edgeCRoles = new ArrayList<>(Arrays.asList(new String[] {"role 5", "role 6"}));
        Edge edgeC = new Edge(nodeC, nodeD, edgeCIds, edgeCRoles, 0.5);

        nodeA.addEdge(edgeA);
        nodeB.addEdge(edgeB);
        nodeC.addEdge(edgeC);

        DesignSpace designSpace = new DesignSpace("space id");
        designSpace.addNode(nodeA);
        designSpace.addNode(nodeB);
        designSpace.addNode(nodeC);
        designSpace.addNode(nodeD);

        designSampler = new DesignSampler(designSpace);
    }

    public void testDfs() {
        Set<List<String>> set = designSampler.enumerate(EnumerateType.DFS, Integer.MAX_VALUE);
        assertEquals(set.size(), 8);

        set = complexDesignSampler.enumerate(EnumerateType.DFS, Integer.MAX_VALUE);
        assertEquals(set.size(), 1);
    }

    public void testBfs() {
        Set<List<String>> set = designSampler.enumerate(EnumerateType.BFS, Integer.MAX_VALUE);
        assertEquals(set.size(), 8);

        set = complexDesignSampler.enumerate(EnumerateType.BFS, Integer.MAX_VALUE);
        assertEquals(set.size(), 1);
    }
}