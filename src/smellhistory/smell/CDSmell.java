package smellhistory.smell;

public class CDSmell extends ArchSmell {

	public CDSmell(String description, int nVersions) {
		super(description, nVersions);
	}

	public int getSize() {
		return (this.getDescription().split(";").length);
	}

}
