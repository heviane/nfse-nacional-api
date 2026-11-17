package br.com.heviane.nfse.adapters.in.security;

import br.com.heviane.nfse.core.domain.CertificadoA1;

/**
 * Subclasse de {@link CertificadoA1} gerenciada exclusivamente pelo servidor Tomcat/Spring.
 * Seu método {@link #close()} é intencionalmente no-op para impedir que requisições individuais
 * executadas via try-with-resources destruam o certificado compartilhado do servidor.
 * A limpeza de memória real ocorre apenas quando o arquivo é alterado em disco ou na parada da aplicação.
 */
public class CertificadoServidorA1 extends CertificadoA1 {

    public CertificadoServidorA1(byte[] arquivoBytes, String senha) {
        super(arquivoBytes, senha);
    }

    @Override
    public void close() {
        // No-op para chamadas de ciclo de vida de requisições individuais
    }

    /**
     * Executa a limpeza física da memória quando o servidor recarrega ou encerra o certificado.
     */
    void fecharFisicamente() {
        super.close();
    }
}
