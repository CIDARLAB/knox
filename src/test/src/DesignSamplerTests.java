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

/**
 * Created by admin on 4/25/17.
 */

public class DesignSamplerTests extends TestCase {
    private static final Logger LOG = LoggerFactory.getLogger(DesignSamplerTests.class);
    private DesignSampler designSampler;

    // assigning the values
    protected void setUp() {
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

    // test method to add two values
    public void testDfs() {
        designSampler.enumerate(EnumerateType.DFS, 5);
        LOG.info("done");
    }
}