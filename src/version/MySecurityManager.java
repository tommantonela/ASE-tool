package version;

import java.security.Permission;

public class MySecurityManager extends SecurityManager {

	public static SecurityManager baseSecurityManager;

	private static MySecurityManager msm = null;
	
	public static MySecurityManager getMySecurityManager(SecurityManager baseSecurityManager){
		if(msm == null)
			msm = new MySecurityManager(baseSecurityManager);
		return msm;
	}
	
	private MySecurityManager(SecurityManager baseSecurityManagerO) {
		baseSecurityManager = baseSecurityManagerO;
	}

	@Override
	public void checkPermission(Permission permission) {
		
		if (permission.getName().startsWith("exitVM")) {
			throw new SecurityException("System exit not allowed");
		}

		if (baseSecurityManager != null) {

			baseSecurityManager.checkPermission(permission);
		} 
		else {
			return;
		}
	}

}
