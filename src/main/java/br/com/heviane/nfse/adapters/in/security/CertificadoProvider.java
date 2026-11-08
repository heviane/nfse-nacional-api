package br.com.heviane.nfse.adapters.in.security;

import br.com.heviane.nfse.core.domain.CertificadoA1;

import java.util.Base64;

/**
 * Provedor responsável por resolver a instância de {@link CertificadoA1} em memória a partir
 * de cabeçalhos HTTP (se autorizados) ou de variáveis/arquivos do servidor Tomcat (setenv.sh).
 * <p>
 * Pertence à camada de adaptadores de entrada / infraestrutura de segurança para isolar o Core Fiscal.
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

        // 2. Prioridade: arquivo em disco configurado no servidor (NFSE_CERT_PATH / setenv.sh - recomendado para Linux/Tomcat)
        String certPath = System.getenv("NFSE_CERT_PATH");
        if (certPath == null || certPath.isBlank()) {
            certPath = System.getProperty("nfse.cert.path");
        }

        if (certPath != null && !certPath.isBlank()) {
            java.nio.file.Path path = java.nio.file.Path.of(certPath.trim());
            if (!java.nio.file.Files.exists(path)) {
                throw new IllegalStateException("Arquivo de certificado digital não encontrado no caminho configurado (NFSE_CERT_PATH): " + certPath);
            }
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(path);
                String senha = resolverSenhaServidor();
                return new CertificadoA1(bytes, senha);
            } catch (Exception e) {
                throw new IllegalStateException("Falha ao ler o arquivo de certificado digital configurado em NFSE_CERT_PATH: " + certPath);
            }
        }

        // 3. Prioridade: string Base64 configurada no servidor (NFSE_CERT_BASE64 - recomendado para containers/K8s)
        String envBase64 = System.getenv("NFSE_CERT_BASE64");
        if (envBase64 == null || envBase64.isBlank()) {
            envBase64 = System.getProperty("nfse.cert.base64");
        }

        if (envBase64 != null && !envBase64.isBlank()) {
            try {
                byte[] bytes = Base64.getDecoder().decode(envBase64.replaceAll("\\s+", ""));
                String senha = resolverSenhaServidor();
                return new CertificadoA1(bytes, senha);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Falha ao carregar certificado digital configurado nas variáveis de ambiente: formato Base64 inválido.");
            } catch (Exception e) {
                throw new IllegalStateException("Falha ao inicializar certificado digital configurado nas variáveis de ambiente.");
            }
        }

        throw new IllegalStateException(
                "Nenhum Certificado Digital A1 foi configurado no servidor via arquivo (NFSE_CERT_PATH) ou Base64 (NFSE_CERT_BASE64)."
        );
    }

    private String resolverSenhaServidor() {
        String senha = System.getenv("NFSE_CERT_PASSWORD");
        if (senha == null) {
            senha = System.getProperty("nfse.cert.password", "");
        }
        return senha;
    }
}

