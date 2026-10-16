package br.com.emr.emrfinancas;
import br.com.emr.emrfinancas.support.*;
import br.com.emr.emrfinancas.model.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
class LegacyBcryptCompatibilityTests extends PasswordHttpSupport {
 static Stream<String> hashes(){return Stream.of(LegacyBcryptFixtures.HASH_2A,LegacyBcryptFixtures.HASH_2B,LegacyBcryptFixtures.HASH_2Y);}
 @ParameterizedTest(name="legacy format and cost {index}") @MethodSource("hashes")
 void validLegacyHashAuthenticatesWithoutRewriting(String hash) throws Exception {
  var u=user("legacy@test.invalid",hash,UserRole.USER);login(u,LegacyBcryptFixtures.NORMAL);
  unchanged(u,hash,0);assertFalse(encoder.matches("ErradoBb2!xx",hash));
  assertFalse(encoder.upgradeEncoding(hash),"no automatic credential upgrade");
 }
 @Test void oversizedUnicodeLegacyInputIsRejectedWithoutHashRewrite() throws Exception {
  var u=user("legacy@test.invalid",LegacyBcryptFixtures.UNICODE_LEGACY_HASH,UserRole.USER);
  var r=request("/api/auth/login",Map.of("email",u.getEmail(),"senha",LegacyBcryptFixtures.UNICODE_73));
  status(r,401);unchanged(u,u.getSenha(),0);assertEquals(1,saved(u).getFailedLoginAttempts());
 }
 @Test void shorterLegacyPasswordDoesNotAcquireNewCompositionRequirements() throws Exception {
  String legacy="old";
  // representative pre-policy hash created with the old engine, not a new registration
  String hash=new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(legacy);
  var u=user("legacy@test.invalid",hash,UserRole.USER);login(u,legacy);unchanged(u,hash,0);
 }
 @Test void alreadyTruncatedLegacyHashStillRecognizesValid72BytePrefix() throws Exception {
  var u=user("legacy@test.invalid",LegacyBcryptFixtures.UNICODE_LEGACY_HASH,UserRole.USER);
  String explicitLegacyPrefix="Aa1!"+"\u00e9".repeat(34);
  assertEquals(72,explicitLegacyPrefix.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
  // The stored legacy hash has already lost the extra byte. The guard cannot recover that entropy.
  login(u,explicitLegacyPrefix);unchanged(u,LegacyBcryptFixtures.UNICODE_LEGACY_HASH,0);
 }
}
