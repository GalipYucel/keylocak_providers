package keycloakverfiyapi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resource.RealmResourceProvider;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import keycloakverfiyapi.dto.SendMailCodeRequest;
import keycloakverfiyapi.dto.VerificationData;
import keycloakverfiyapi.dto.VerifyCodeRequest;

public class CodeVerifyMailApiRealmResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;
    private static final Logger logger = Logger.getLogger(CodeVerifyMailApiRealmResourceProvider.class);
    	private static final int expireSecond=180;
    // In-memory store for verification codes and timestamps
    private static final Map<String, VerificationData> verificationDataStore = new HashMap<>();

    public CodeVerifyMailApiRealmResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void close() {
        // Kapatılacak kaynak yok
    }

    @Override
    public Object getResource() {
        return this;
    }

    @POST
    @Path("verify-code")
    @Produces(MediaType.APPLICATION_JSON)
    public Response verifyCode(VerifyCodeRequest request) {
        try {
            KeycloakContext context = session.getContext();
            RealmModel realm = context.getRealm();
            UserModel user = session.users().getUserByEmail(realm, request.getUsername());

            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
            }

            VerificationData verificationData = verificationDataStore.get(request.getUsername());

            if (verificationData == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Verification code not found").build();
            }

            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - verificationData.getTimestamp();

            if (elapsedTime > TimeUnit.MINUTES.toMillis(expireSecond)) {
                verificationDataStore.remove(request.getUsername());
                return Response.status(Response.Status.BAD_REQUEST).entity("Verification code expired").build();
            }

            if (!verificationData.getCode().equals(request.getCode())) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid code").build();
            }

            // Code is valid
            verificationDataStore.remove(request.getUsername()); // Optionally remove after use
            return Response.ok("success").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    
    
    @Path("send-mail-code")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendMailCode(SendMailCodeRequest request) {
        try {
            KeycloakContext context = session.getContext();
            RealmModel realm = context.getRealm();
            UserModel user = session.users().getUserByUsername(realm, request.getUsername());

            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
            }

            String code = generateRandomCode();
            long timestamp = System.currentTimeMillis();
            verificationDataStore.put(request.getUsername(), new VerificationData(code, timestamp));
            sendEmail(realm, user, code,request.getMailHeader());
            logger.warn("Email sent successfully");
        } catch (Exception e) {
            logger.error("An error occurred when attempting to email an TOTP auth:", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        return Response.ok("Verification code sent successfully").build();
    }

    private String generateRandomCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

	private void sendEmail(RealmModel realm, UserModel user, String code,String mailHeader) throws EmailException {
        String realmName = Strings.isNullOrEmpty(realm.getDisplayName()) ? realm.getName() : realm.getDisplayName();
        List<Object> subjAttr = ImmutableList.of(realmName);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("code", code);
        attributes.put("ttl", 1000/expireSecond);
        FreeMarkerEmailTemplateProvider mailSender = new FreeMarkerEmailTemplateProvider(session);
        mailSender.setRealm(realm);
        mailSender.setUser(user);
        mailSender.send(mailHeader, subjAttr, "totp-email-verify.ftl", attributes);
    }


}
