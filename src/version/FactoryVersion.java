package version;

public class FactoryVersion {

	public static Version createVersion(String index, String path,String name){
	
		Version vv = null;
		
		switch(index){
		case "arcan":
			vv = new VersionADI(path, name);
			break;
		case "sonargraph":
			vv = new VersionSonarGraph(path, name);
		}
		return vv;
	}
	
}
