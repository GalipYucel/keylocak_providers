/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package keycloakverfiyapi;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriBuilderException;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class VerifyEmailByCode implements RequiredActionProvider, RequiredActionFactory, ServerInfoAwareProviderFactory {
    public static final String VERIFY_EMAIL_CODE = "VERIFY_EMAIL_CODE";
    private static final String AUTH_NOTE_CODE = "code";
    private static final String AUTH_NOTE_TTL = "ttl";
    public static final String INVALID_CODE = "emailTOTPCodeInvalid";
    private static final String TOTP_FORM = "totp-form.ftl";
    private static final String TOTP_EMAIL = "totp-email.ftl";
    public static final String CONFIG_CODE_LENGTH = "code-length";
    public static final String CONFIG_CODE_SYMBOLS = "code-symbols";
    private static final String FormHeaderText = "headerText";
    private static final String AUTH_NOTE_REMAINING_RETRIES = "remainingRetries";
    public static final int DEFAULT_CODE_LENGTH = 6;
    public static final String DEFAULT_CODE_SYMBOLS = String.valueOf(SecretGenerator.DIGITS);
    private static final Logger logger = Logger.getLogger(VerifyEmailByCode.class);
    private int codeLength;
    private String codeSymbols;
    
    private void redirectToLogin(RequiredActionContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        URI loginUri = UriBuilder.fromUri(context.getUriInfo().getBaseUri())
                                 .path("realms")
                                 .path(context.getRealm().getName())
                                 .path("protocol/openid-connect/auth")
                                 .queryParam("client_id", context.getAuthenticationSession().getClient().getClientId())
                                 .queryParam("redirect_uri", context.getAuthenticationSession().getRedirectUri())
                                 .build();

        Response response = Response.status(Response.Status.FOUND) // 302 Found
                                    .location(loginUri)
                                    .build();
        authSession.clearAuthNotes();
        context.challenge(response); // Yönlendirme işlemini gerçekleştir
    }

    private static void createFormChallenge(RequiredActionContext context, FormMessage errorMessage) {
        LoginFormsProvider loginFormsProvider = context.form();
        if (Objects.nonNull(errorMessage)) {
            loginFormsProvider = loginFormsProvider.addError(new FormMessage(AUTH_NOTE_CODE, INVALID_CODE));
        }
        
        Response challenge = loginFormsProvider
                .setAttribute("user", new ProfileBean(context.getUser())).setAttribute(FormHeaderText, "emailTOTPCodeEmailVerify")
                .createForm(TOTP_FORM);
        context.challenge(challenge);
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        if (context.getRealm().isVerifyEmail()
                && !context.getUser().isEmailVerified()) {
            context.getUser().addRequiredAction(VERIFY_EMAIL_CODE);
            logger.debug("User is required to verify email");
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        if (context.getUser().isEmailVerified()) {
            context.getAuthenticationSession().removeAuthNote(VERIFY_EMAIL_CODE);
            context.success();
            return;
        }

        String email = context.getUser().getEmail();
        if (Validation.isBlank(email)) {
            context.ignore();
            return;
        }

        sendVerifyEmailAndCreateForm(context);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        EventBuilder event = context.getEvent().clone().event(EventType.VERIFY_EMAIL).detail(Details.EMAIL, context.getUser().getEmail());

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String code = authSession.getAuthNote(AUTH_NOTE_CODE);
        String ttl = authSession.getAuthNote(AUTH_NOTE_TTL);
        String remainingAttemptsStr = authSession.getAuthNote(AUTH_NOTE_REMAINING_RETRIES);
        int remainingAttempts = remainingAttemptsStr == null ? 0 : Integer.parseInt(remainingAttemptsStr);
 
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String enteredCode = formData.getFirst(AUTH_NOTE_CODE);
        logger.info(code+"send-Code"+enteredCode);
        boolean isValid = enteredCode.equals(code);
        if (isValid) {
            if (Long.parseLong(ttl) < System.currentTimeMillis()) {
       
            	redirectToLogin(context);
            } else {
                // Kod geçerli
                context.getUser().setEmailVerified(true);
                context.getAuthenticationSession().removeAuthNote(VERIFY_EMAIL_CODE);
                event.success();
                context.success();
            }
        } else {
            // Kod geçersiz
            remainingAttempts--;
            authSession.setAuthNote(AUTH_NOTE_REMAINING_RETRIES, Integer.toString(remainingAttempts));

            if (remainingAttempts > 0) {
            	 context.challenge(context.form().setAttribute("realm", context.getRealm())
                         .setError("emailTOTPCodeInvalid", Integer.toString(remainingAttempts)).setAttribute(FormHeaderText, "emailTOTPCodeEmailVerify")
                         .createForm(TOTP_FORM));
            	 
                event.error(INVALID_CODE);
        
                
            } else {
  
            	redirectToLogin(context);
            }
        }
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        codeLength = config.getInt(CONFIG_CODE_LENGTH, DEFAULT_CODE_LENGTH);
        codeSymbols = config.get(CONFIG_CODE_SYMBOLS, DEFAULT_CODE_SYMBOLS);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return VERIFY_EMAIL_CODE;
    }

    private void sendVerifyEmailAndCreateForm(RequiredActionContext context) throws UriBuilderException, IllegalArgumentException {
        KeycloakSession session = context.getSession();
        UserModel user = context.getUser();
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        int ttl =300;
        EventBuilder event = context.getEvent().clone().event(EventType.SEND_VERIFY_EMAIL).detail(Details.EMAIL, user.getEmail());
        String code = SecretGenerator.getInstance().randomString(codeLength, codeSymbols.toCharArray());
        authSession.setAuthNote(AUTH_NOTE_CODE, code);
        authSession.setAuthNote(AUTH_NOTE_TTL, Long.toString(System.currentTimeMillis() + (ttl * 1000L)));
        authSession.setAuthNote(AUTH_NOTE_REMAINING_RETRIES, Integer.toString(3));
        RealmModel realm = session.getContext().getRealm();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("code", code);
        attributes.put("ttl", Math.floorDiv(ttl, 60));
        LoginFormsProvider form = context.form();
        try {
            session
                    .getProvider(EmailTemplateProvider.class)
                    .setAuthenticationSession(authSession)
                    .setRealm(realm)
                    .setUser(user)
                    .send("emailTOTPCodeEmailVerify", TOTP_EMAIL, attributes);
            event.success();
        } catch (EmailException e) {
            logger.error("Failed to send verification email", e);
            event.error(Errors.EMAIL_SEND_FAILED);
            form.setError(Errors.EMAIL_SEND_FAILED);
        }
     
        createFormChallenge(context, null);
    }

    @Override
    public String getDisplayText() {
        logger.info("Retrieved display text for VerifyEmailByCode");
        return "Verify Email by code";
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        Map<String, String> ret = new LinkedHashMap<>();
        ret.put(VERIFY_EMAIL_CODE + "." + CONFIG_CODE_LENGTH, String.valueOf(codeLength));
        ret.put(VERIFY_EMAIL_CODE + "." + CONFIG_CODE_SYMBOLS, codeSymbols);
        return ret;
    }
}