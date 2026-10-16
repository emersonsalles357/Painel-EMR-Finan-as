package br.com.emr.emrfinancas;
import br.com.emr.emrfinancas.support.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.test.annotation.DirtiesContext;
import org.apache.coyote.AbstractProtocol;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
 "server.address=127.0.0.1",
 "spring.datasource.url=jdbc:h2:mem:bcrypt_live13;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
 "app.security.rate-limit.exempt-local=false"})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class PasswordBoundaryLiveHttpTests extends PasswordHttpSupport {
 @LocalServerPort int port;
 @Autowired ServletWebServerApplicationContext context;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
 static Stream<String> acceptable(){return validByteFixtures().stream();}
 static Stream<String> excessive(){return excessiveByteFixtures().stream();}
 private HttpResponse<String> tcp(String path,Object body,String jwt) throws Exception {
  var protocol=(AbstractProtocol<?>)((TomcatWebServer)context.getWebServer()).getTomcat().getConnector().getProtocolHandler();
  assertTrue(protocol.getAddress()!=null && protocol.getAddress().isLoopbackAddress(),"actual listener restricted to loopback");
  assertEquals(List.of("test"),List.of(context.getEnvironment().getActiveProfiles()));
  var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(10));
  if(jwt!=null)b.header("Authorization","Bearer "+jwt);
  if(body==null)b.GET();else b.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
  return client.send(b.build(),HttpResponse.BodyHandlers.ofString());
 }
 private String tcpLogin(String email,String raw) throws Exception {
  var r=tcp("/api/auth/login",Map.of("email",email,"senha",raw),null);assertEquals(200,r.statusCode());
  return json.readTree(r.body()).get("accessToken").asText();
 }
 @ParameterizedTest(name="TCP accepts valid byte fixture {index}") @MethodSource("acceptable")
 void realTcpRegisterLoginAndBearerAcceptBoundary(String raw) throws Exception {
  var r=tcp("/api/auth/register",Map.of("nome","Conta TCP","email","tcp@test.invalid","senha",raw),null);
  assertEquals(201,r.statusCode());
  String jwt=tcpLogin("tcp@test.invalid",raw);
  assertEquals(200,tcp("/api/auth/me",null,jwt).statusCode());
 }
 @Test void realTcpLongLoginRejectsSuffixAndCountsFailure() throws Exception {
  var u=owner();String raw=LegacyBcryptFixtures.BOUNDARY+"different";
  var r=tcp("/api/auth/login",Map.of("email",u.getEmail(),"senha",raw),null);
  assertEquals(401,r.statusCode(),"TCP long credential rejected");
  assertFalse(json.readTree(r.body()).has("accessToken"));assertFalse(r.body().contains(raw));
  unchanged(u,u.getSenha(),0);assertEquals(1,saved(u).getFailedLoginAttempts());
 }
 @ParameterizedTest(name="TCP valid change boundary {index}") @MethodSource("acceptable")
 void realTcpValidChangeAcceptsByteBoundaryAndRevokesJwt(String raw) throws Exception {
  var u=owner();String old=tcpLogin(u.getEmail(),LegacyBcryptFixtures.BOUNDARY);
  assertEquals(204,tcp("/api/users/me/password",Map.of("senhaAtual",LegacyBcryptFixtures.BOUNDARY,"novaSenha",raw),old).statusCode());
  assertEquals(1,saved(u).getTokenVersion());assertTrue(encoder.matches(raw,saved(u).getSenha()));
  assertEquals(401,tcp("/api/auth/me",null,old).statusCode());tcpLogin(u.getEmail(),raw);
 }
 @ParameterizedTest(name="TCP valid reset boundary {index}") @MethodSource("acceptable")
 void realTcpValidResetAcceptsByteBoundaryAndConsumesToken(String raw) throws Exception {
  var u=owner();String old=tcpLogin(u.getEmail(),LegacyBcryptFixtures.BOUNDARY),token=resetToken(u);
  assertEquals(200,tcp("/api/auth/reset-password",Map.of("token",token,"novaSenha",raw),null).statusCode());
  assertNotNull(resets.findAll().get(0).getUsedAt());assertEquals(1,saved(u).getTokenVersion());
  assertTrue(encoder.matches(raw,saved(u).getSenha()));assertEquals(401,tcp("/api/auth/me",null,old).statusCode());tcpLogin(u.getEmail(),raw);
 }
 @ParameterizedTest(name="TCP rejects new change bytes {index}") @MethodSource("excessive")
 void realTcpInvalidChangeCannotWriteOrRevokeJwt(String raw) throws Exception {
  var u=owner();String jwt=tcpLogin(u.getEmail(),LegacyBcryptFixtures.BOUNDARY);
  var rejected=tcp("/api/users/me/password",Map.of("senhaAtual",LegacyBcryptFixtures.BOUNDARY,"novaSenha",raw),jwt);
  assertEquals(400,rejected.statusCode());unchanged(u,u.getSenha(),0);assertFalse(rejected.body().contains(raw));
  assertEquals(200,tcp("/api/auth/me",null,jwt).statusCode());
 }
 @ParameterizedTest(name="TCP rejects new reset bytes {index}") @MethodSource("excessive")
 void realTcpInvalidResetCannotConsumeTokenOrRevokeJwt(String raw) throws Exception {
  var u=owner();String jwt=tcpLogin(u.getEmail(),LegacyBcryptFixtures.BOUNDARY),token=resetToken(u);
  var rejected=tcp("/api/auth/reset-password",Map.of("token",token,"novaSenha",raw),null);
  assertEquals(400,rejected.statusCode());unchanged(u,u.getSenha(),0);assertNull(resets.findAll().get(0).getUsedAt());
  assertFalse(rejected.body().contains(raw));assertFalse(rejected.body().contains(token));
  assertEquals(200,tcp("/api/auth/me",null,jwt).statusCode());
 }
 @ParameterizedTest(name="TCP register rejects byte fixture {index}") @MethodSource("excessive")
 void realTcpRegisterRejectsExcessiveBytesWithoutAccount(String raw) throws Exception {
  var r=tcp("/api/auth/register",Map.of("nome","Conta TCP","email","tcp@test.invalid","senha",raw),null);
  assertEquals(400,r.statusCode());assertEquals(0,users.count());assertFalse(r.body().contains(raw));
 }
 @Test void realTcpCurrentPasswordCannotUseDiscardedSuffix() throws Exception {
  var u=owner();String jwt=tcpLogin(u.getEmail(),LegacyBcryptFixtures.BOUNDARY);
  var r=tcp("/api/users/me/password",Map.of("senhaAtual",LegacyBcryptFixtures.BOUNDARY+"different","novaSenha",LegacyBcryptFixtures.NORMAL),jwt);
  assertEquals(400,r.statusCode());unchanged(u,u.getSenha(),0);
 }
 @Test void realTcpRecoveryInvalidNewPasswordPreservesTokenThenValidResetRevokesJwt() throws Exception {
  var u=owner();String jwt=tcpLogin(u.getEmail(),LegacyBcryptFixtures.BOUNDARY);
  AtomicReference<String> link=new AtomicReference<>();
  doAnswer(inv->{link.set(inv.getArgument(1));return null;}).when(emailService).sendPasswordResetEmail(anyString(),anyString());
  assertEquals(200,tcp("/api/auth/forgot-password",Map.of("email",u.getEmail()),null).statusCode());
  assertNotNull(link.get(),"synthetic delivery captured without external SMTP");
  String token=link.get().substring(link.get().indexOf("token=")+6);
  var rejected=tcp("/api/auth/reset-password",Map.of("token",token,"novaSenha",LegacyBcryptFixtures.UNICODE_73),null);
  assertEquals(400,rejected.statusCode());unchanged(u,u.getSenha(),0);assertNull(resets.findAll().get(0).getUsedAt());
  assertFalse(rejected.body().contains(token));
  assertEquals(200,tcp("/api/auth/reset-password",Map.of("token",token,"novaSenha",LegacyBcryptFixtures.NORMAL),null).statusCode());
  assertNotNull(resets.findAll().get(0).getUsedAt());assertEquals(1,saved(u).getTokenVersion());
  assertEquals(401,tcp("/api/auth/me",null,jwt).statusCode());
  tcpLogin(u.getEmail(),LegacyBcryptFixtures.NORMAL);
 }
}
