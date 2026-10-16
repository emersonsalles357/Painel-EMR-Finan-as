package br.com.emr.emrfinancas.support;

import br.com.emr.emrfinancas.model.*;
import br.com.emr.emrfinancas.repository.*;
import br.com.emr.emrfinancas.security.RateLimiterService;
import br.com.emr.emrfinancas.service.EmailService;
import br.com.emr.emrfinancas.service.PasswordResetService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(properties={
 "spring.datasource.url=jdbc:h2:mem:bcrypt_tasks13;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
 "app.security.rate-limit.exempt-local=false"})
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
@ActiveProfiles("test")
public abstract class PasswordHttpSupport {
 protected static final Instant NOW=Instant.parse("2026-10-13T12:00:00Z");
 @Autowired protected MockMvc mvc;
 @Autowired protected ObjectMapper json;
 @Autowired protected UsuarioRepository users;
 @Autowired protected PasswordResetTokenRepository resets;
 @Autowired protected PasswordEncoder encoder;
 @Autowired protected RateLimiterService limiter;
 @Autowired protected JdbcTemplate jdbc;
 @MockitoBean protected EmailService emailService;
 @MockitoBean protected Clock clock;

 @BeforeEach void isolateFixture(){
  when(clock.instant()).thenReturn(NOW); when(clock.getZone()).thenReturn(ZoneOffset.UTC);
  for(String table:List.of("tb_emr_password_reset_token","tb_emr_gasto","tb_emr_recebimento","tb_emr_investimento","tb_emr_usuario"))
   jdbc.update("DELETE FROM "+table);
  limiter.clear(); // between cases only; never during a protection sequence
 }
 protected Usuario user(String email,String hash,UserRole role){
  Usuario u=new Usuario();u.setNome("Dono Sintetico");u.setEmail(email);u.setSenha(hash);u.setRole(role);
  return users.saveAndFlush(u);
 }
 protected Usuario owner(){return user("owner@test.invalid",LegacyBcryptFixtures.BOUNDARY_HASH,UserRole.USER);}
 protected MvcResult request(String path,Object body,String token,String ip) throws Exception {
  MockHttpServletRequestBuilder r=post(path).contentType(MediaType.APPLICATION_JSON)
   .content(json.writeValueAsString(body)).with(req->{req.setRemoteAddr(ip);return req;});
  if(token!=null)r.header("Authorization","Bearer "+token);
  return mvc.perform(r).andReturn();
 }
 protected MvcResult request(String path,Object body) throws Exception{return request(path,body,null,"198.51.100.61");}
 protected void status(MvcResult r,int expected){assertEquals(expected,r.getResponse().getStatus(),"HTTP status");}
 protected String login(Usuario u,String raw) throws Exception {
  MvcResult r=request("/api/auth/login",Map.of("email",u.getEmail(),"senha",raw));status(r,200);
  return json.readTree(r.getResponse().getContentAsString()).get("accessToken").asText();
 }
 protected String resetToken(Usuario u){
  String raw="synthetic-reset-"+u.getCodigo();
  resets.saveAndFlush(new PasswordResetToken(u,PasswordResetService.hashToken(raw),NOW,NOW.plusSeconds(900)));
  return raw;
 }
 protected Usuario saved(Usuario u){return users.findById(u.getCodigo()).orElseThrow();}
 protected void unchanged(Usuario u,String hash,long version){
  Usuario current=saved(u);assertTrue(Objects.equals(hash,current.getSenha()),"stored hash unchanged");
  assertEquals(version,current.getTokenVersion(),"tokenVersion unchanged");
 }
 protected void noSensitiveBody(MvcResult r,String... values) throws Exception {
  String text=r.getResponse().getContentAsString();
  for(String value:values)assertFalse(text.contains(value),"no synthetic credential echoed");
  assertFalse(json.readTree(text).has("accessToken"),"no JWT emitted");
 }
 public static List<String> validByteFixtures(){
  return List.of("Aa1!"+"x".repeat(67),"Aa1!"+"x".repeat(68),"Aa1!"+"é".repeat(34),
   "Aa1!"+"€".repeat(22)+"xx","Aa1!"+"😀".repeat(17));
 }
 public static List<String> excessiveByteFixtures(){
  return List.of("Aa1!"+"x".repeat(69),LegacyBcryptFixtures.UNICODE_73,
   "Aa1!"+"€".repeat(22)+"xxx","Aa1!"+"😀".repeat(17)+"x");
 }
}
