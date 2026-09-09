import { LegalPageLayout } from '../components/LegalPageLayout';

export function PrivacyPolicyPage() {
  return (
    <LegalPageLayout title="Política de Privacidade — EMR Finanças" alternatePath="/termos-de-uso" alternateLabel="Termos de Uso">
      <section>
        <h2>1. Objetivo desta Política</h2>
        <p>Esta Política de Privacidade explica, de forma simples e transparente, como determinadas informações podem ser tratadas durante a utilização do EMR Finanças.</p>
        <p>O EMR Finanças é um projeto demonstrativo e educacional destinado à apresentação em portfólio profissional e LinkedIn, não sendo uma plataforma financeira comercial.</p>
      </section>

      <section>
        <h2>2. Dados que podem ser tratados</h2>
        <p>Dependendo das funcionalidades utilizadas, a aplicação poderá processar informações como:</p>
        <p><strong>Dados de cadastro</strong></p>
        <ul><li>nome;</li><li>endereço de e-mail;</li><li>senha armazenada de forma protegida por hash.</li></ul>
        <p>A senha original não deve ser armazenada em texto legível.</p>
        <p><strong>Dados financeiros inseridos pelo usuário</strong></p>
        <p>Podem incluir:</p>
        <ul>
          <li>receitas;</li><li>despesas;</li><li>recebimentos;</li><li>investimentos;</li><li>valores;</li>
          <li>categorias;</li><li>datas;</li><li>descrições relacionadas aos lançamentos.</li>
        </ul>
        <p>Esses dados são fornecidos voluntariamente pelo próprio usuário.</p>
        <p>Por se tratar de uma aplicação demonstrativa, recomenda-se utilizar informações fictícias.</p>
        <p><strong>Dados técnicos e de segurança</strong></p>
        <p>Algumas informações técnicas poderão ser processadas para proteção da aplicação, como:</p>
        <ul><li>endereço IP;</li><li>tentativas de autenticação;</li><li>informações relacionadas à sessão;</li><li>registros técnicos necessários para segurança e diagnóstico de erros.</li></ul>
      </section>

      <section>
        <h2>3. Finalidades do tratamento</h2>
        <p>As informações poderão ser utilizadas exclusivamente para possibilitar funcionalidades do sistema, como:</p>
        <ul>
          <li>criação e gerenciamento da conta;</li><li>autenticação;</li><li>exibição do dashboard;</li>
          <li>gerenciamento dos registros financeiros;</li><li>proteção contra acessos não autorizados;</li>
          <li>limitação de tentativas de login;</li><li>recuperação de senha;</li><li>manutenção e segurança da aplicação.</li>
        </ul>
        <p>Os dados não são coletados para criação de perfis publicitários ou comercialização de informações pessoais.</p>
      </section>

      <section>
        <h2>4. Senhas e autenticação</h2>
        <p>As credenciais utilizadas no sistema são protegidas por mecanismos de segurança implementados na aplicação.</p>
        <p>As senhas são armazenadas utilizando técnicas de hash, de modo que a aplicação não necessita armazenar a senha original em formato legível.</p>
        <p>A aplicação também utiliza mecanismos de autenticação para restringir o acesso às informações pertencentes a cada usuário.</p>
      </section>

      <section>
        <h2>5. Isolamento das informações</h2>
        <p>Os registros financeiros são associados à conta responsável por sua criação.</p>
        <p>A aplicação possui mecanismos destinados a impedir que um usuário autenticado consulte ou modifique registros pertencentes a outra conta.</p>
      </section>

      <section>
        <h2>6. Recuperação de senha</h2>
        <p>Quando o recurso de recuperação de senha for utilizado, o endereço de e-mail informado poderá ser utilizado para envio de instruções de recuperação.</p>
        <p>Tokens utilizados nesse processo possuem finalidade temporária e destinam-se exclusivamente à redefinição da senha da conta.</p>
      </section>

      <section>
        <h2>7. Serviços de terceiros</h2>
        <p>Para permitir o funcionamento da aplicação demonstrativa, o EMR Finanças utiliza serviços de infraestrutura de terceiros.</p>
        <p>Entre eles podem estar serviços destinados a:</p>
        <ul><li>hospedagem do Front-End;</li><li>hospedagem do Back-End;</li><li>armazenamento em banco de dados;</li><li>envio de e-mails transacionais.</li></ul>
        <p>Atualmente, o projeto poderá utilizar provedores como Vercel, Render, Neon e Brevo, cada um sujeito às suas próprias políticas, medidas de segurança e condições de utilização.</p>
      </section>

      <section>
        <h2>8. Compartilhamento de dados</h2>
        <p>O EMR Finanças não comercializa informações pessoais dos usuários.</p>
        <p>Informações poderão ser processadas por provedores de infraestrutura apenas na medida necessária para disponibilizar as funcionalidades técnicas da aplicação.</p>
        <p>Também poderá ocorrer tratamento quando necessário para cumprimento de obrigação legal aplicável.</p>
      </section>

      <section>
        <h2>9. Armazenamento e segurança</h2>
        <p>São adotadas medidas técnicas destinadas a reduzir riscos de acesso indevido aos dados, incluindo mecanismos como:</p>
        <ul>
          <li>autenticação;</li><li>criptografia de transporte HTTPS/TLS;</li><li>armazenamento seguro de senhas;</li>
          <li>controle de acesso;</li><li>limitação de tentativas de autenticação;</li><li>isolamento de dados entre usuários;</li>
          <li>validação das solicitações realizadas à API.</li>
        </ul>
        <p>Nenhum sistema conectado à internet pode garantir segurança absoluta.</p>
      </section>

      <section>
        <h2>10. Retenção dos dados</h2>
        <p>Os dados poderão permanecer armazenados enquanto forem necessários para funcionamento e demonstração da aplicação.</p>
        <p>Como o EMR Finanças é um projeto de portfólio, ambientes, bancos de dados e informações poderão ser modificados ou removidos durante manutenções, testes ou atualizações.</p>
        <p>Por esse motivo, o sistema não deve ser utilizado como forma permanente de armazenamento de informações financeiras importantes.</p>
      </section>

      <section>
        <h2>11. Direitos relacionados aos dados</h2>
        <p>Quando aplicável, o usuário poderá solicitar informações relacionadas aos seus dados ou solicitar sua correção ou exclusão, observadas as limitações técnicas e legais aplicáveis ao projeto.</p>
        <p>Solicitações poderão ser encaminhadas pelos canais de contato profissionais disponibilizados pelo responsável pelo projeto.</p>
      </section>

      <section>
        <h2>12. Cookies e armazenamento local</h2>
        <p>A aplicação poderá utilizar mecanismos de armazenamento necessários ao funcionamento da autenticação e da experiência do usuário.</p>
        <p>Esses recursos são utilizados para funcionamento técnico da aplicação e não possuem finalidade de publicidade comportamental.</p>
      </section>

      <section>
        <h2>13. Dados financeiros sensíveis</h2>
        <p>O usuário não deve inserir no EMR Finanças:</p>
        <ul>
          <li>senhas bancárias;</li><li>números completos de cartões;</li><li>códigos CVV;</li><li>chaves privadas;</li>
          <li>tokens bancários;</li><li>credenciais de instituições financeiras;</li><li>documentos financeiros confidenciais;</li>
          <li>dados de terceiros sem autorização.</li>
        </ul>
        <p>O sistema foi desenvolvido para demonstração e deve preferencialmente ser utilizado com dados fictícios.</p>
      </section>

      <section>
        <h2>14. Uso por menores</h2>
        <p>O projeto não foi especificamente desenvolvido como serviço destinado a crianças ou adolescentes.</p>
        <p>Por sua natureza demonstrativa, recomenda-se que seu uso seja realizado apenas para avaliação das funcionalidades técnicas apresentadas.</p>
      </section>

      <section>
        <h2>15. Alterações desta Política</h2>
        <p>Esta Política de Privacidade poderá ser atualizada quando houver mudanças relevantes nas funcionalidades, infraestrutura ou forma de tratamento das informações.</p>
        <p>A versão e a data do documento poderão ser atualizadas sempre que necessário.</p>
      </section>

      <section>
        <h2>16. Contato</h2>
        <p>Para dúvidas relacionadas à privacidade ou ao projeto EMR Finanças, utilize os canais profissionais disponibilizados no portfólio ou perfil do desenvolvedor.</p>
        <p><strong>EMR Finanças — Projeto demonstrativo para fins educacionais e de portfólio profissional.</strong></p>
      </section>
    </LegalPageLayout>
  );
}
