package br.com.heviane.nfse.core.services;

import br.com.heviane.nfse.core.domain.CertificadoA1;

import java.util.Base64;

/**
 * Provedor responsável por resolver a instância de {@link CertificadoA1} em memória
 * com base na hierarquia:
 * 1. Cabeçalhos HTTP da requisição (X-Certificado-Base64 e X-Certificado-Senha) para chamadas dinâmicas/multi-tenant.
 * 2. Variáveis de ambiente do servidor Tomcat (NFSE_CERT_BASE64 e NFSE_CERT_PASSWORD).
 */
public class CertificadoProvider {

    public CertificadoA1 obterCertificado(String headerBase64, String headerSenha) {
        // 1. Prioridade: cabeçalhos informados na requisição
        if (headerBase64 != null && !headerBase64.isBlank()) {
            try {
                byte[] bytes = Base64.getDecoder().decode(headerBase64.replaceAll("\\s+", ""));
                String senha = (headerSenha != null) ? headerSenha : "";
                return new CertificadoA1(bytes, senha);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Falha ao processar o certificado digital fornecido no cabeçalho X-Certificado-Base64: formato Base64 inválido.");
            } catch (Exception e) {
                throw new IllegalArgumentException("Falha ao inicializar o certificado digital fornecido no cabeçalho X-Certificado-Base64.");
            }
        }

        // 2. Prioridade: variáveis de ambiente configuradas no servidor (setenv.sh)
        String envBase64 = System.getenv("NFSE_CERT_BASE64");
        if (envBase64 == null || envBase64.isBlank()) {
            envBase64 = System.getProperty("nfse.cert.base64");
        }

        if (envBase64 != null && !envBase64.isBlank()) {
            try {
                byte[] bytes = Base64.getDecoder().decode(envBase64.replaceAll("\\s+", ""));
                String senha = System.getenv("NFSE_CERT_PASSWORD");
                if (senha == null) {
                    senha = System.getProperty("nfse.cert.password", "");
                }
                return new CertificadoA1(bytes, senha);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Falha ao carregar certificado digital configurado nas variáveis de ambiente: formato Base64 inválido.");
            } catch (Exception e) {
                throw new IllegalStateException("Falha ao inicializar certificado digital configurado nas variáveis de ambiente.");
            }
        }

        throw new IllegalStateException(
                "Nenhum Certificado Digital A1 foi configurado no servidor (NFSE_CERT_BASE64) nem fornecido nos cabeçalhos da requisição (X-Certificado-Base64)."
        );
    }
}

