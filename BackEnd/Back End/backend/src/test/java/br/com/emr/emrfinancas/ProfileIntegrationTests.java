package br.com.emr.emrfinancas;
import br.com.emr.emrfinancas.model.*;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class ProfileIntegrationTests {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UsuarioRepository users; @Autowired PasswordEncoder encoder;
 final String oldPassword="Anterior@12345", newPassword="NovaSenha@67890";
 Usuario create() { var u=new Usuario();u.setNome("Pessoa Teste");u.setEmail(UUID.randomUUID()+"@perfil.test");u.setSenha(encoder.encode(oldPassword));return users.saveAndFlush(u); }
 String login(Usuario u,String password) throws Exception {return json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("email",u.getEmail(),"senha",password)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("accessToken").asText();}
 org.springframework.test.web.servlet.ResultActions change(String token, Map<String,Object> body) throws Exception {return mvc.perform(post("/api/users/me/password").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));}
 @Test void profilePatchPreflightAcceptsConfiguredOriginOnly() throws Exception {
  mvc.perform(options("/api/users/me").header("Origin","http://localhost:5173").header("Access-Control-Request-Method","PATCH")).andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:5173"));
  mvc.perform(options("/api/users/me").header("Origin","https://untrusted.example").header("Access-Control-Request-Method","PATCH")).andExpect(status().isForbidden());
 }
 @Test void profileEditsOnlySelfAndNeverEscalatesRole() throws Exception {
  var a=create();var b=create();var token=login(a,oldPassword);
  mvc.perform(patch("/api/users/me").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("nome","  Nome Atualizado  ","id",b.getCodigo(),"role","ADMIN","email",b.getEmail())))).andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Nome Atualizado")).andExpect(jsonPath("$.id").value(a.getCodigo())).andExpect(jsonPath("$.role").value("ROLE_USER")).andExpect(jsonPath("$.senha").doesNotExist());
  assertThat(users.findById(b.getCodigo()).orElseThrow().getNome()).isEqualTo("Pessoa Teste");
  mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+token)).andExpect(jsonPath("$.nome").value("Nome Atualizado"));
  mvc.perform(get("/api/usuarios").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
 }
 @Test void validChangeReplacesBcryptAndOldLoginFailsAndCannotTargetOtherUser() throws Exception {
  var a=create();var b=create();String hash=a.getSenha(), otherHash=b.getSenha(),token=login(a,oldPassword);
  change(token,Map.of("senhaAtual",oldPassword,"novaSenha",newPassword,"userId",b.getCodigo())).andExpect(status().isNoContent()).andExpect(content().string(""));
  var saved=users.findById(a.getCodigo()).orElseThrow();assertThat(saved.getSenha()).startsWith("$2").isNotEqualTo(hash);assertThat(encoder.matches(newPassword,saved.getSenha())).isTrue();assertThat(encoder.matches(oldPassword,saved.getSenha())).isFalse();assertThat(users.findById(b.getCodigo()).orElseThrow().getSenha()).isEqualTo(otherHash);
  mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("email",a.getEmail(),"senha",oldPassword)))).andExpect(status().isUnauthorized());
  login(a,newPassword);login(b,oldPassword);
 }
 @Test void incorrectCurrentAndWeakNewPasswordsDoNotChangeHash() throws Exception {
  var a=create();String hash=a.getSenha(),token=login(a,oldPassword);
  change(token,Map.of("senhaAtual","Errada@12345","novaSenha",newPassword)).andExpect(status().isBadRequest());
  for(String weak:new String[]{"Curta@1","semmaiuscula@123","SEMMINUSCULA@123","SemNumeroAqui@","SemEspecial123","A1@"+"a".repeat(70)}) change(token,Map.of("senhaAtual",oldPassword,"novaSenha",weak)).andExpect(status().isBadRequest());
  assertThat(users.findById(a.getCodigo()).orElseThrow().getSenha()).isEqualTo(hash);
 }
 @Test void unauthenticatedAndInvalidProfileRequestsFail() throws Exception {
  mvc.perform(patch("/api/users/me").contentType(MediaType.APPLICATION_JSON).content("{} ")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/users/me/password").contentType(MediaType.APPLICATION_JSON).content("{} ")).andExpect(status().isUnauthorized());
  var a=create();var token=login(a,oldPassword);
  for(String name:new String[]{"","  "," a ","a".repeat(101)}) mvc.perform(patch("/api/users/me").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("nome",name)))).andExpect(status().isBadRequest());
 }
}
