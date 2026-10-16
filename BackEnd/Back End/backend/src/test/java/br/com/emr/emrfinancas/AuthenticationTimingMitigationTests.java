package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.model.UserRole;
import br.com.emr.emrfinancas.security.*;
import br.com.emr.emrfinancas.service.EmailNormalizer;
import br.com.emr.emrfinancas.support.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:bcrypt_timing13;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "app.security.rate-limit.exempt-local=false",
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "logging.level.org.hibernate.stat=OFF",
    "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"})
class AuthenticationTimingMitigationTests extends PasswordHttpSupport {
    @Autowired EmailNormalizer normalizer;
    @Autowired EntityManagerFactory emf;
    private DaoAuthenticationProvider provider(BCryptPasswordEncoder delegate) {
        DaoAuthenticationProvider provider=new DaoAuthenticationProvider(new CustomUserDetailsService(users,normalizer));
        provider.setPasswordEncoder(new Utf8BoundedBcryptPasswordEncoder(delegate));
        // Warm the provider's real unknown-user hash, outside the measured/instrumented request.
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(
            new UsernamePasswordAuthenticationToken("warmup-missing@test.invalid","ErradoBb2!xx")));
        clearInvocations(delegate);
        return provider;
    }
    @Test void realProviderUsesOneFixedCost10ComparisonForBothLongRejections() {
        user("known@test.invalid",LegacyBcryptFixtures.HASH_2A,UserRole.USER);
        BCryptPasswordEncoder delegate=spy(new BCryptPasswordEncoder());
        DaoAuthenticationProvider provider=provider(delegate);
        for(String email:List.of("known@test.invalid","missing@test.invalid")) {
            clearInvocations(delegate);
            assertThrows(BadCredentialsException.class, () -> provider.authenticate(
                new UsernamePasswordAuthenticationToken(email,LegacyBcryptFixtures.BOUNDARY+"suffix")));
            var calls=mockingDetails(delegate).getInvocations().stream()
                .filter(call -> call.getMethod().getName().equals("matches")).toList();
            assertEquals(1,calls.size(),"one real BCrypt comparison");
            Object[] args=calls.get(0).getArguments();
            assertTrue("synthetic-length-check".equals(args[0]),"received password or prefix never delegated");
            assertTrue(LegacyBcryptFixtures.COMPENSATION_HASH.equals(args[1]),"fixed synthetic cost10 hash");
            assertTrue(mockingDetails(delegate).getInvocations().stream().noneMatch(call -> call.getMethod().getName().equals("encode")),"no per-request hash generation");
        }
    }
    @Test void realProviderKeepsOrdinaryWrongPasswordAndUnknownUserDummyWork() {
        user("known@test.invalid",LegacyBcryptFixtures.HASH_2A,UserRole.USER);
        BCryptPasswordEncoder delegate=spy(new BCryptPasswordEncoder());
        DaoAuthenticationProvider provider=provider(delegate);
        for(boolean known:List.of(true,false)) {
            clearInvocations(delegate);
            assertThrows(BadCredentialsException.class, () -> provider.authenticate(new UsernamePasswordAuthenticationToken(
                known?"known@test.invalid":"missing@test.invalid","ErradoBb2!xx")));
            var calls=mockingDetails(delegate).getInvocations().stream().filter(call -> call.getMethod().getName().equals("matches")).toList();
            assertEquals(1,calls.size());Object[] args=calls.get(0).getArguments();
            assertTrue("ErradoBb2!xx".equals(args[0]));
            assertTrue(known?LegacyBcryptFixtures.HASH_2A.equals(args[1]):((String)args[1]).startsWith("$2a$10$"));
        }
    }
    @Test void recordsFortyPairsPerCategoryAfterTenSeparateWarmupPairs() throws Exception {
        List<String> rows=new ArrayList<>(); rows.add("category,pair,order,existing_ms,unknown_ms");
        int fixture=0;
        for(String category:List.of("ordinary_wrong","excess_bytes")) {
            String raw=category.equals("ordinary_wrong")?"ErradoBb2!xx":LegacyBcryptFixtures.BOUNDARY+"suffix";
            for(int sample=-10;sample<40;sample++) {
                int id=++fixture;String ip="192.0.2."+id;
                var known=user("timing-known-"+id+"@test.invalid",LegacyBcryptFixtures.HASH_2B,UserRole.USER);
                String unknown="timing-missing-"+id+"@test.invalid";
                long[] nanos=new long[2];org.springframework.test.web.servlet.MvcResult[] results=new org.springframework.test.web.servlet.MvcResult[2];
                boolean existingFirst=(sample&1)==0;
                for(int step=0;step<2;step++) {
                    int target=existingFirst?step:1-step;
                    long start=System.nanoTime();
                    results[target]=request("/api/auth/login",Map.of("email",target==0?known.getEmail():unknown,"senha",raw),null,ip);
                    nanos[target]=System.nanoTime()-start;
                    status(results[target],401);noSensitiveBody(results[target],raw);
                }
                var existing=json.readTree(results[0].getResponse().getContentAsString());
                var missing=json.readTree(results[1].getResponse().getContentAsString());
                ((tools.jackson.databind.node.ObjectNode)existing).remove("timestamp");
                ((tools.jackson.databind.node.ObjectNode)missing).remove("timestamp");
                assertTrue(existing.equals(missing),"same generic response without timestamp");
                assertEquals(1,saved(known).getFailedLoginAttempts());unchanged(known,LegacyBcryptFixtures.HASH_2B,0);
                assertFalse(users.existsByEmailIgnoreCase(unknown));
                if(sample>=0) rows.add(String.format(Locale.ROOT,"%s,%d,%s,%.6f,%.6f",category,sample,existingFirst?"existing_first":"unknown_first",nanos[0]/1e6,nanos[1]/1e6));
            }
        }
        String directory=System.getenv("EMR_SECURITY_EVIDENCE_DIRECTORY");
        Path output=directory==null?Path.of("target","security-timing"):Path.of(directory);
        Files.createDirectories(output);Files.write(output.resolve("timing-pairs.csv"),rows,StandardCharsets.UTF_8);
        // Diagnostic samples do not establish constant-time behavior or a universal latency threshold.
    }
    @Test void investigatesExistingFailureWritesAndPreexistingLockedEarlyExit() throws Exception {
        var statistics=emf.unwrap(SessionFactory.class).getStatistics();
        List<String> rows=new ArrayList<>();rows.add("category,account_state,queries,statements,entity_updates");
        for(String category:List.of("ordinary_wrong","excess_bytes")) {
            var known=user("sql-"+category+"@test.invalid",LegacyBcryptFixtures.HASH_2B,UserRole.USER);
            String raw=category.equals("ordinary_wrong")?"ErradoBb2!xx":LegacyBcryptFixtures.BOUNDARY+"suffix";
            for(boolean existing:List.of(true,false)) {
                statistics.clear();
                status(request("/api/auth/login",Map.of("email",existing?known.getEmail():"missing-sql@test.invalid","senha",raw),null,"198.51.100.81"),401);
                assertEquals(2,statistics.getQueryExecutionCount());
                assertEquals(existing?1:0,statistics.getEntityUpdateCount());
                rows.add(category+","+(existing?"existing_unlocked":"unknown")+","+statistics.getQueryExecutionCount()+","+statistics.getPrepareStatementCount()+","+statistics.getEntityUpdateCount());
            }
        }
        var locked=user("sql-locked@test.invalid",LegacyBcryptFixtures.HASH_2B,UserRole.USER);
        locked.setFailedLoginAttempts(3);locked.setLockedUntil(NOW.plusSeconds(900));users.saveAndFlush(locked);
        statistics.clear();status(request("/api/auth/login",Map.of("email",locked.getEmail(),"senha",LegacyBcryptFixtures.NORMAL),null,"198.51.100.82"),401);
        assertEquals(1,statistics.getQueryExecutionCount());assertEquals(0,statistics.getEntityUpdateCount());
        rows.add("correct_password,existing_locked,"+statistics.getQueryExecutionCount()+","+statistics.getPrepareStatementCount()+","+statistics.getEntityUpdateCount());
        String directory=System.getenv("EMR_SECURITY_EVIDENCE_DIRECTORY");
        Path output=directory==null?Path.of("target","security-timing"):Path.of(directory);
        Files.createDirectories(output);Files.write(output.resolve("timing-sql-paths.csv"),rows,StandardCharsets.UTF_8);
    }
}
