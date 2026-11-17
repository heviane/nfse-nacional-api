package br.com.heviane.nfse.adapters.in.security;

import br.com.heviane.nfse.config.NfseProperties;
import br.com.heviane.nfse.core.domain.CertificadoA1;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Base64;
import java.util.Objects;

/**
 * Provedor responsável por resolver a instância de {@link CertificadoA1} em memória a partir
 * de cabeçalhos HTTP (se autorizados) ou de variáveis/arquivos do servidor Tomcat (setenv.sh).
 * <p>
 * Pertence à camada de adaptadores de entrada / infraestrutura de segurança para isolar o Core Fiscal.
 * Mantém cache thread-safe da instância do servidor com invalidação automática ao detectar
 * alteração na data de modificação do arquivo em disco (para suporte a renovação anual a quente).
 */
public class CertificadoProvider {

    private final NfseProperties.CertificadoProperties certProperties;

    // Cache thread-safe de certificado do servidor a partir de arquivo em disco
    private Path cachedFilePath;
    private FileTime cachedFileModifiedTime;
    private String cachedFilePassword;
    private CertificadoServidorA1 cachedFileCertificado;

    // Cache thread-safe de certificado do servidor a partir de Base64 em variável de ambiente
    private String cachedBase64Conteudo;
    private String cachedBase64Password;
    private CertificadoServidorA1 cachedBase64Certificado;

    public CertificadoProvider() {
        this(new NfseProperties.CertificadoProperties(resolverAllowCertHeadersDefault(), null, null, null));
    }

    public CertificadoProvider(boolean allowCertHeaders) {
        this(new NfseProperties.CertificadoProperties(allowCertHeaders, null, null, null));
    }

    public CertificadoProvider(NfseProperties.CertificadoProperties certProperties) {
        this.certProperties = (certProperties != null) ? certProperties : new NfseProperties.CertificadoProperties();
    }

    public CertificadoProvider(NfseProperties nfseProperties) {
        this((nfseProperties != null && nfseProperties.getCert() != null)
                ? nfseProperties.getCert()
                : new NfseProperties.CertificadoProperties());
    }

    private static boolean resolverAllowCertHeadersDefault() {
        String env = System.getenv("NFSE_ALLOW_CERT_HEADERS");
        if (env == null || env.isBlank()) {
            env = System.getProperty("nfse.cert.allow-headers", "false");
        }
        return Boolean.parseBoolean(env.trim());
    }

    public boolean isAllowCertHeaders() {
        return certProperties.isAllowHeaders() || resolverAllowCertHeadersDefault();
    }

    public CertificadoA1 obterCertificado(String headerBase64, String headerSenha) {
        // 1. Prioridade: cabeçalhos informados na requisição (se expressamente autorizados)
        if (headerBase64 != null && !headerBase64.isBlank()) {
            if (!isAllowCertHeaders()) {
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
        String certPath = certProperties.getPath();
        if (certPath == null || certPath.isBlank()) {
            certPath = resolverCertPathFallback();
        }

        if (certPath != null && !certPath.isBlank()) {
            Path path = Path.of(certPath.trim());
            if (!Files.exists(path)) {
                throw new IllegalStateException("Arquivo de certificado digital não encontrado no caminho configurado (NFSE_CERT_PATH): " + certPath);
            }
            String senha = resolverSenhaServidor();
            return obterOuCarregarCertificadoArquivo(path, senha);
        }

        // 3. Prioridade: string Base64 configurada no servidor (NFSE_CERT_BASE64 - recomendado para containers/K8s)
        String envBase64 = certProperties.getBase64();
        if (envBase64 == null || envBase64.isBlank()) {
            envBase64 = resolverCertBase64Fallback();
        }

        if (envBase64 != null && !envBase64.isBlank()) {
            String senha = resolverSenhaServidor();
            return obterOuCarregarCertificadoBase64(envBase64, senha);
        }

        throw new IllegalStateException(
                "Nenhum Certificado Digital A1 foi configurado no servidor via arquivo (NFSE_CERT_PATH) ou Base64 (NFSE_CERT_BASE64)."
        );
    }

    private synchronized CertificadoServidorA1 obterOuCarregarCertificadoArquivo(Path path, String senha) {
        try {
            FileTime lastModified = Files.getLastModifiedTime(path);
            if (cachedFileCertificado != null
                    && path.equals(cachedFilePath)
                    && lastModified.equals(cachedFileModifiedTime)
                    && Objects.equals(senha, cachedFilePassword)) {
                return cachedFileCertificado;
            }

            if (cachedFileCertificado != null) {
                cachedFileCertificado.fecharFisicamente();
            }

            byte[] bytes = Files.readAllBytes(path);
            CertificadoServidorA1 novoCert = new CertificadoServidorA1(bytes, senha);

            this.cachedFilePath = path;
            this.cachedFileModifiedTime = lastModified;
            this.cachedFilePassword = senha;
            this.cachedFileCertificado = novoCert;

            return novoCert;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao ler o arquivo de certificado digital configurado em NFSE_CERT_PATH: " + path);
        }
    }

    private synchronized CertificadoServidorA1 obterOuCarregarCertificadoBase64(String envBase64, String senha) {
        if (cachedBase64Certificado != null
                && envBase64.equals(cachedBase64Conteudo)
                && Objects.equals(senha, cachedBase64Password)) {
            return cachedBase64Certificado;
        }

        if (cachedBase64Certificado != null) {
            cachedBase64Certificado.fecharFisicamente();
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(envBase64.replaceAll("\\s+", ""));
            CertificadoServidorA1 novoCert = new CertificadoServidorA1(bytes, senha);

            this.cachedBase64Conteudo = envBase64;
            this.cachedBase64Password = senha;
            this.cachedBase64Certificado = novoCert;

            return novoCert;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Falha ao carregar certificado digital configurado nas variáveis de ambiente: formato Base64 inválido.");
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao inicializar certificado digital configurado nas variáveis de ambiente.");
        }
    }

    public synchronized void limparCache() {
        if (cachedFileCertificado != null) {
            cachedFileCertificado.fecharFisicamente();
            cachedFileCertificado = null;
            cachedFilePath = null;
            cachedFileModifiedTime = null;
            cachedFilePassword = null;
        }
        if (cachedBase64Certificado != null) {
            cachedBase64Certificado.fecharFisicamente();
            cachedBase64Certificado = null;
            cachedBase64Conteudo = null;
            cachedBase64Password = null;
        }
    }

    private String resolverSenhaServidor() {
        String senha = certProperties.getPassword();
        if (senha == null || senha.isBlank()) {
            senha = System.getenv("NFSE_CERT_PASSWORD");
            if (senha == null) {
                senha = System.getProperty("nfse.cert.password", "");
            }
        }
        return (senha != null) ? senha : "";
    }

    private static String resolverCertPathFallback() {
        String certPath = System.getenv("NFSE_CERT_PATH");
        if (certPath == null || certPath.isBlank()) {
            certPath = System.getProperty("nfse.cert.path");
        }
        return certPath;
    }

    private static String resolverCertBase64Fallback() {
        String envBase64 = System.getenv("NFSE_CERT_BASE64");
        if (envBase64 == null || envBase64.isBlank()) {
            envBase64 = System.getProperty("nfse.cert.base64");
        }
        return envBase64;
    }
}
