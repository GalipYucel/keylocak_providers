package keycloakverfiyapi;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class CustomVerifyMailActionFactory implements RequiredActionFactory {

	public static final String PROVIDER_ID="CustomVerifyMailActionProvider";
	private static final Logger logger = Logger.getLogger(CustomVerifyMailActionFactory.class);
	private static final CustomVerifyMailActionProvider singletion = new CustomVerifyMailActionProvider();
	@Override
	public RequiredActionProvider create(KeycloakSession session) {
		// TODO Auto-generated method stub
		logger.warn("New Provider");;
		return singletion;
	}

	@Override
	public void init(Scope config) {
		logger.warn("CustomVerifyMailActionFactory init");
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return PROVIDER_ID;
	}

	@Override
	public String getDisplayText() {
		// TODO Auto-generated method stub
		return "Custom Verify Mail";
	}
	

}
