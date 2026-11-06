package br.com.heviane.nfse.core.services;

import br.com.heviane.nfse.core.domain.CertificadoA1;

import java.util.Base64;

/**
 * Provedor responsável por resolver a instância de {@link CertificadoA1} em memória.
 * <p>
 * Por padrão de segurança fiscal (Cliente 001 / BrasilCloud), o carregamento de certificados
 * diretamente via cabeçalhos HTTP (X-Certificado-Base64 / X-Certificado-Senha) é <b>desabilitado</b>
 * para impedir vazamento de chaves em logs de proxies reversos e WAFs.
 * O certificado deve ser configurado diretamente no ambiente do servidor Tomcat (setenv.sh).
 */
public class CertificadoProvider {

    private final boolean allowCertHeaders;

    public CertificadoProvider() {
        this(resolverAllowCertHeadersDefault());
    }

    public CertificadoProvider(boolean allowCertHeaders) {
        this.allowCertHeaders = allowCertHeaders;
    }

    private static boolean resolverAllowCertHeadersDefault() {
        String env = System.getenv("NFSE_ALLOW_CERT_HEADERS");
        if (env == null || env.isBlank()) {
            env = System.getProperty("nfse.cert.allow-headers", "false");
        }
        return Boolean.parseBoolean(env.trim());
    }

    public boolean isAllowCertHeaders() {
        return allowCertHeaders;
    }

    public CertificadoA1 obterCertificado(String headerBase64, String headerSenha) {
        // 1. Prioridade: cabeçalhos informados na requisição (se expressamente autorizados)
        if (headerBase64 != null && !headerBase64.isBlank()) {
            if (!allowCertHeaders) {
                throw new SecurityException(
                        "O envio de certificado digital via cabeçalhos HTTP está desabilitado neste servidor por diretriz de segurança fiscal (NFSE_ALLOW_CERT_HEADERS=false). Utilize o certificado digital configurado no ambiente do servidor."
                );
            }
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

