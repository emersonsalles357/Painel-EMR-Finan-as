package br.com.emr.emrfinancas;
import br.com.emr.emrfinancas.support.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class PasswordBoundaryHttpIntegrationTests extends PasswordHttpSupport {
 static Stream<String> acceptable(){return validByteFixtures().stream();}
 static Stream<String> excessive(){return excessiveByteFixtures().stream();}
 @Test void loginRejectsSuffixAfter72Utf8BytesWithoutIssuingToken() throws Exception {
  var u=owner();String raw=LegacyBcryptFixtures.BOUNDARY+"different";
  var r=request("/api/auth/login",Map.of("email",u.getEmail(),"senha",raw));
  status(r,401);noSensitiveBody(r,raw,u.getSenha());unchanged(u,u.getSenha(),0);
  assertEquals(1,saved(u).getFailedLoginAttempts());
 }
 @Test void currentPasswordRejectsSuffixWithoutChangingHashOrVersion() throws Exception {
  var u=owner();String jwt=login(u,LegacyBcryptFixtures.BOUNDARY);
  var r=request("/api/users/me/password",Map.of("senhaAtual",LegacyBcryptFixtures.BOUNDARY+"different","novaSenha",LegacyBcryptFixtures.NORMAL),jwt,"198.51.100.62");
  status(r,400);unchanged(u,u.getSenha(),0);
 }
 @ParameterizedTest(name="register accepts byte fixture {index}") @MethodSource("acceptable")
 void registerAcceptsValidUtf8Boundary(String raw) throws Exception {
  var r=request("/api/auth/register",Map.of("nome","Conta Sintetica","email","new@test.invalid","senha",raw));
  status(r,201);var u=users.findByEmailIgnoreCase("new@test.invalid").orElseThrow();
  assertTrue(encoder.matches(raw,u.getSenha()));
 }
 @ParameterizedTest(name="register rejects excessive byte fixture {index}") @MethodSource("excessive")
 void registerRejectsExcessiveBytesWithoutSaving(String raw) throws Exception {
  var r=request("/api/auth/register",Map.of("nome","Conta Sintetica","email","new@test.invalid","senha",raw));
  status(r,400);assertEquals(0,users.count());noSensitiveBody(r,raw);
 }
 @ParameterizedTest(name="change rejects byte fixture {index}") @MethodSource("excessive")
 void changeRejectsExcessiveNewBytesWithoutWrites(String raw) throws Exception {
  var u=owner();String jwt=login(u,LegacyBcryptFixtures.BOUNDARY);
  var r=request("/api/users/me/password",Map.of("senhaAtual",LegacyBcryptFixtures.BOUNDARY,"novaSenha",raw),jwt,"198.51.100.63");
  status(r,400);unchanged(u,u.getSenha(),0);noSensitiveBody(r,raw,u.getSenha());
 }
 @ParameterizedTest(name="reset rejects byte fixture {index}") @MethodSource("excessive")
 void resetRejectsExcessiveNewBytesWithoutConsumingToken(String raw) throws Exception {
  var u=owner();String token=resetToken(u);
  var r=request("/api/auth/reset-password",Map.of("token",token,"novaSenha",raw));
  status(r,400);unchanged(u,u.getSenha(),0);assertNull(resets.findAll().get(0).getUsedAt());noSensitiveBody(r,raw,token,u.getSenha());
 }
 @Test void validResetStillConsumesTokenAndRevokesOldJwt() throws Exception {
  var u=owner();String oldJwt=login(u,LegacyBcryptFixtures.BOUNDARY),token=resetToken(u);
  status(request("/api/auth/reset-password",Map.of("token",token,"novaSenha",LegacyBcryptFixtures.NORMAL)),200);
  assertNotNull(resets.findAll().get(0).getUsedAt());assertEquals(1,saved(u).getTokenVersion());
  assertTrue(encoder.matches(LegacyBcryptFixtures.NORMAL,saved(u).getSenha()));
  assertFalse(encoder.matches(LegacyBcryptFixtures.BOUNDARY,saved(u).getSenha()));
  var denied=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/me")
   .header("Authorization","Bearer "+oldJwt)).andReturn();status(denied,401);
  login(saved(u),LegacyBcryptFixtures.NORMAL);
 }
}
