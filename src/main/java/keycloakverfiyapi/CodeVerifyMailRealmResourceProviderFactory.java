package keycloakverfiyapi;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class CodeVerifyMailRealmResourceProviderFactory implements RealmResourceProviderFactory {

	@Override
	public RealmResourceProvider create(KeycloakSession session) {
		// TODO Auto-generated method stub
		return  new CodeVerifyMailApiRealmResourceProvider(session);
	}

	@Override
	public void init(Scope config) {
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
        return "verfiy-mail-custom-resource";
    }

}
