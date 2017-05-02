package knox.spring.data.neo4j.eugene;

import java.util.List;
import java.util.Set;

public class CompositeDevice {
    private String id;

    private List<Device> subDevices;

    private Set<Rule> rules;

    public CompositeDevice(String id, List<Device> subDevices) {
        this.id = id;

        this.subDevices = subDevices;
    }

    public CompositeDevice(String id, List<Device> subDevices, Set<Rule> rules) {
        this.id = id;

        this.subDevices = subDevices;

        this.rules = rules;
    }

    public String getID() { return id; }

    public List<Device> getSubDevices() { return subDevices; }

    public Set<Rule> getRules() { return rules; }
}
