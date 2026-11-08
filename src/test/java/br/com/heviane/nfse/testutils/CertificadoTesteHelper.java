package br.com.heviane.nfse.testutils;

import br.com.heviane.nfse.core.domain.CertificadoA1;

import java.io.File;
import java.nio.file.Files;

/**
 * Utilitário exclusivo de testes automatizados para gerar um Certificado Digital A1
 * (PKCS#12 com par de chaves RSA-2048 e certificado X.509) em memória via keytool nativo.
 * O arquivo temporário é destruído imediatamente após a leitura dos bytes para a memória.
 */
public final class CertificadoTesteHelper {

    private static volatile CertificadoA1 certificadoCache;

    private CertificadoTesteHelper() {
    }

    public static synchronized CertificadoA1 obterCertificadoTeste() {
        if (certificadoCache != null) {
            return certificadoCache;
        }

        try {
            File tempKeystore = File.createTempFile("keystore_teste_", ".p12");
            Files.deleteIfExists(tempKeystore.toPath());
            tempKeystore.deleteOnExit();

            String password = "testpassword123";
            String alias = "teste_a1";
            String keytoolExec = resolverKeytoolExecutable();

            ProcessBuilder pb = new ProcessBuilder(
                    keytoolExec,
                    "-genkeypair",
                    "-alias", alias,
                    "-keyalg", "RSA",
                    "-keysize", "2048",
                    "-storetype", "PKCS12",
                    "-keystore", tempKeystore.getAbsolutePath(),
                    "-storepass", password,
                    "-keypass", password,
                    "-dname", "CN=EMPRESA DE TESTE LTDA, OU=TI, O=TESTE, C=BR",
                    "-validity", "365"
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String error = new String(process.getInputStream().readAllBytes());
                throw new IllegalStateException("Falha ao gerar certificado com keytool: " + error);
            }

            byte[] bytes = Files.readAllBytes(tempKeystore.toPath());
            Files.deleteIfExists(tempKeystore.toPath());

            certificadoCache = new CertificadoA1(bytes, password);
            return certificadoCache;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao inicializar certificado de teste em memória: " + e.getMessage(), e);
        }
    }

    private static String resolverKeytoolExecutable() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null && !javaHome.isBlank()) {
            boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
            String execName = isWindows ? "keytool.exe" : "keytool";
            java.nio.file.Path keytoolPath = java.nio.file.Path.of(javaHome, "bin", execName);
            if (java.nio.file.Files.exists(keytoolPath)) {
                return keytoolPath.toAbsolutePath().toString();
            }
        }
        return "keytool";
    }
}
