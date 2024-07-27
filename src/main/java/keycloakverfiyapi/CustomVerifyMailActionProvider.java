package keycloakverfiyapi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

public class CustomVerifyMailActionProvider implements RequiredActionProvider {
	private static final Logger logger = Logger.getLogger(CustomVerifyMailActionProvider.class);

	@Override
	public InitiatedActionSupport initiatedActionSupport() {
		return InitiatedActionSupport.SUPPORTED;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public void evaluateTriggers(RequiredActionContext context) {
		// TODO Auto-generated method stub
		context.getAuthenticationSession().addRequiredAction("CustomVerifyMailActionProvider");
	}

	
	private void sendMail(RequiredActionContext context)
	{
		try {
			logger.warn("evaluateTriggers Girdi");
			String code = "444";

			KeycloakSession session = context.getSession();
			UserModel user = context.getUser();

			RealmModel realm = context.getRealm();

			String realmName = Strings.isNullOrEmpty(realm.getDisplayName()) ? realm.getName() : realm.getDisplayName();
			List<Object> subjAttr = ImmutableList.of(realmName);
			Map<String, Object> attributes = new HashMap<>();
			attributes.put("code", code);
			attributes.put("ttl", Math.floorDiv(1000, 60));
			
			session.getProvider(EmailTemplateProvider.class).setRealm(realm)
					.setUser(user).setAttribute("realmName", realmName).send("Test", subjAttr, "totp-email.ftl", attributes);
			logger.warn("Action Gird ve Mail Gönderdi");
		} catch (EmailException e) {
			logger.error("An error occurred when attempting to email an TOTP auth:", e);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public void requiredActionChallenge(RequiredActionContext context) {
		// TODO Auto-generated method stub
		logger.warn("requiredActionChallenge Girdi");
		sendMail(context);
		context.challenge(null);
	
	
	}

	@Override
	public void processAction(RequiredActionContext context) {
		// TODO Auto-generated method stub
		logger.warn("requiredActionChallenge Girdi");
		
		context.success();
		context.getAuthenticationSession().removeRequiredAction("CustomVerifyMailActionProvider");
		

	}

}
