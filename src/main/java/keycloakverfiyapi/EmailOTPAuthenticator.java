package keycloakverfiyapi;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.services.messages.Messages;

public class EmailOTPAuthenticator implements Authenticator {

	private static final String TOTP_FORM = "totp-form.ftl";
	private static final String TOTP_EMAIL = "totp-email.ftl";
	private static final String AUTH_NOTE_CODE = "code";
	private static final String AUTH_NOTE_TTL = "ttl";
	private static final String FormHeaderText = "headerText";
	private static final String AUTH_NOTE_REMAINING_RETRIES = "remainingRetries";
	private static final Logger logger = Logger.getLogger(EmailOTPAuthenticator.class);

	private static final String ALPHA_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final String ALPHA_LOWER = "abcdefghijklmnopqrstuvwxyz";
	private static final String NUM = "0123456789";

	@Override
	public void authenticate(AuthenticationFlowContext context) {
		try {
			AuthenticatorConfigModel config = context.getAuthenticatorConfig();
			int ttl = Integer.parseInt(config.getConfig().get(EmailOTPAuthenticatorFactory.CONFIG_PROP_TTL));
			String emailSubject = config.getConfig().get(EmailOTPAuthenticatorFactory.CONFIG_PROP_EMAIL_SUBJECT);
			String headerText = config.getConfig().get(EmailOTPAuthenticatorFactory.CONFIG_PROP_HEADER_TEXT);
			Boolean isSimulation = Boolean.parseBoolean(config.getConfig().getOrDefault(EmailOTPAuthenticatorFactory.CONFIG_PROP_SIMULATION, "false"));
			UserModel user = context.getUser();
			if (user == null) {
			     /*context.forkWithSuccessMessage(new FormMessage(Messages.EMAIL_SENT));
		            return;*/
				context.challenge(context.form().setAttribute("realm", context.getRealm())
						.setAttribute(FormHeaderText, headerText).createForm(TOTP_FORM));
				return;
			} 
	
			KeycloakSession session = context.getSession();
				String code = getCode(config);
				int maxRetries = getMaxRetries(config);
				AuthenticationSessionModel authSession = context.getAuthenticationSession();
				authSession.setAuthNote(AUTH_NOTE_CODE, code);
				authSession.setAuthNote(AUTH_NOTE_TTL, Long.toString(System.currentTimeMillis() + (ttl * 1000L)));
				authSession.setAuthNote(AUTH_NOTE_REMAINING_RETRIES, Integer.toString(maxRetries));
				authSession.setAuthNote(FormHeaderText, headerText);

				RealmModel realm = context.getRealm();

				if (isSimulation) {
					logger.warn(String.format("***** SIMULATION MODE ***** Would send a TOTP email to %s with code: %s",
							user.getEmail(), code));
				} else {
					String realmName = Strings.isNullOrEmpty(realm.getDisplayName()) ? realm.getName()
							: realm.getDisplayName();
					List<Object> subjAttr = ImmutableList.of(realmName);
					Map<String, Object> attributes = new HashMap<>();
					attributes.put("code", code);
					attributes.put("ttl", Math.floorDiv(ttl, 60));
					session.getProvider(EmailTemplateProvider.class).setAuthenticationSession(authSession)
							.setRealm(realm).setUser(user).setAttribute("realmName", realmName)
							.send(emailSubject, subjAttr, TOTP_EMAIL, attributes);
				}

				context.challenge(context.form().setAttribute("realm", context.getRealm())
						.setAttribute(FormHeaderText, headerText).createForm(TOTP_FORM));
			
		} catch (Exception e) {
			logger.error("An error occurred when attempting to email a TOTP auth:", e);
			context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
					context.form().setError("emailTOTPEmailNotSent", e.getMessage())
							.createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
		}
	}

	@Override
	public void action(AuthenticationFlowContext context) {
		String enteredCode = context.getHttpRequest().getDecodedFormParameters().getFirst("code");

		AuthenticationSessionModel authSession = context.getAuthenticationSession();
		String code = authSession.getAuthNote(AUTH_NOTE_CODE);
		String ttl = authSession.getAuthNote(AUTH_NOTE_TTL);
		String formHeaderText = authSession.getAuthNote(FormHeaderText);
		String remainingAttemptsStr = authSession.getAuthNote(AUTH_NOTE_REMAINING_RETRIES);
		int remainingAttempts = remainingAttemptsStr == null ? 0 : Integer.parseInt(remainingAttemptsStr);

		if (code == null || ttl == null) {
			context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
					context.form().createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
			return;
		}

		boolean isValid = enteredCode.equals(code);
		if (isValid) {
			if (Long.parseLong(ttl) < System.currentTimeMillis()) {
				// expired
				/*
				 * context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE,
				 * context.form().setError("emailTOTPCodeExpired").createErrorPage(Response.
				 * Status.BAD_REQUEST));
				 */
				context.resetFlow();
				/*
				 * context.challenge(context.form().setAttribute("realm", context.getRealm())
				 * .setError("emailTOTPCodeExpired") .createForm(TOTP_FORM));
				 */
			} else {
				// valid
				context.success();
			}
		} else {
			// Code is invalid
			remainingAttempts--;
			authSession.setAuthNote(AUTH_NOTE_REMAINING_RETRIES, Integer.toString(remainingAttempts));

			if (remainingAttempts > 0) {
				// Inform user of the remaining attempts
				context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, context.form()
						.setAttribute("realm", context.getRealm()).setAttribute(FormHeaderText, formHeaderText)
						.setError("emailTOTPCodeInvalid", Integer.toString(remainingAttempts)).createForm(TOTP_FORM));
			} else {
				// Reset login
				context.resetFlow();
				/*
				 * context.challenge(context.form().setAttribute("realm", context.getRealm())
				 * .setError("emailTOTPCodeInvalid") .createForm(TOTP_FORM));
				 */
			}
		}
	}

	@Override
	public boolean requiresUser() {
		return false;
	}

	@Override
	public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
		return user.getEmail() != null;
	}

	@Override
	public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
	}

	@Override
	public void close() {
	}
 
	private int getMaxRetries(AuthenticatorConfigModel config) {
		int maxRetries = Integer
				.parseInt(config.getConfig().getOrDefault(EmailOTPAuthenticatorFactory.CONFIG_PROP_MAX_RETRIES, "3"));
		return maxRetries;
	}

	private String getCode(AuthenticatorConfigModel config) {
		int length = Integer.parseInt(config.getConfig().get(EmailOTPAuthenticatorFactory.CONFIG_PROP_LENGTH));
		Boolean allowUppercase = Boolean.parseBoolean(
				config.getConfig().getOrDefault(EmailOTPAuthenticatorFactory.CONFIG_PROP_ALLOW_UPPERCASE, "true"));
		Boolean allowLowercase = Boolean.parseBoolean(
				config.getConfig().getOrDefault(EmailOTPAuthenticatorFactory.CONFIG_PROP_ALLOW_LOWERCASE, "true"));
		Boolean allowNumbers = Boolean.parseBoolean(
				config.getConfig().getOrDefault(EmailOTPAuthenticatorFactory.CONFIG_PROP_ALLOW_NUMBERS, "true"));

		StringBuilder sb = new StringBuilder();

		if (allowUppercase) {
			sb.append(ALPHA_UPPER);
		}
		if (allowLowercase) {
			sb.append(ALPHA_LOWER);
		}
		if (allowNumbers) {
			sb.append(NUM);
		}

		// if the string builder is empty allow all charsets as default
		if (sb.length() == 0) {
			sb.append(ALPHA_UPPER).append(ALPHA_LOWER).append(NUM);
		}

		char[] symbols = sb.toString().toCharArray();
		return SecretGenerator.getInstance().randomString(length, symbols);
	}

}