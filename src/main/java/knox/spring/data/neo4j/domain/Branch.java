package knox.spring.data.neo4j.domain;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.*;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;

@JsonIdentityInfo(generator=JSOGGenerator.class)
@NodeEntity
public class Branch {

	@GraphId
	Long id;

	String branchID;

	int idIndex;

	@Relationship(type = "CONTAINS") 
	Set<Commit> commits;

	@Relationship(type = "LATEST") 
	Commit latestCommit;

	public Branch() {

	}

	public Branch(String branchID, int idIndex) {
		this.branchID = branchID;
		this.idIndex = idIndex;
	}

	public void addCommit(Commit commit) {
		if (!hasCommits()) {
			commits = new HashSet<Commit>();
		}
		commits.add(commit);
	}
	
	public boolean containsCommit(Commit commit) {
		if (hasCommits()) {
			return commits.contains(commit);
		} else {
			return false;
		}
	}

	public Commit copyCommit(Commit commit) {
		Commit commitCopy = createCommit();
		
		commitCopy.copySnapshot(commit.getSnapshot());
		
		return commitCopy;
	}

	public Commit createCommit() {
		Commit commit = new Commit("c" + idIndex++);
		
		addCommit(commit);
		
		return commit;
	}
	
	public boolean deleteCommits(Set<Commit> deletedCommits) {
		if (hasCommits()) {
			return commits.removeAll(deletedCommits);
		} else {
			return false;
		}
	}

	public Set<Commit> getCommits() {
		return commits;
	}

	public Commit getLatestCommit() {
		return latestCommit;
	}

	public String getBranchID() {
		return branchID;
	}

	public int getIdIndex() {
		return idIndex;
	}

	public boolean hasCommits() {
		if (commits == null) {
			return false;
		} else {
			return commits.size() > 0;
		}
	}
	
	public Set<Commit> retainCommits(Set<Commit> retainedCommits) {
		Set<Commit> diffCommits = new HashSet<Commit>();
		
		if (hasCommits()) {
			for (Commit commit : commits) {
				if (!retainedCommits.contains(commit)) {
					diffCommits.add(commit);
				}
			}
			
			deleteCommits(diffCommits);
		}
		
		return diffCommits;
	}
	
	public void setLatestCommit(Commit commit) {
		latestCommit = commit;
	}
}
