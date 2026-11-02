package br.com.heviane.nfse.adapters.in.rest;

import br.com.heviane.nfse.core.domain.AmbienteSefin;
import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.core.domain.RespostaConsultaNfse;
import br.com.heviane.nfse.core.domain.SefinTransmissaoException;
import br.com.heviane.nfse.core.ports.in.ConsultarNfseUseCase;
import br.com.heviane.nfse.core.services.CertificadoProvider;
import br.com.heviane.nfse.testutils.CertificadoTesteHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ConsultaNfseController.class)
class ConsultaNfseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConsultarNfseUseCase consultarNfseUseCase;

    @MockBean
    private CertificadoProvider certificadoProvider;

    private CertificadoA1 certificadoA1;

    @BeforeEach
    void setUp() throws Exception {
        certificadoA1 = CertificadoTesteHelper.obterCertificadoTeste();
    }

    private String compactarGZipBase64(String conteudo) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(conteudo.getBytes(StandardCharsets.UTF_8));
        }
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    @Test
    @DisplayName("Deve consultar NFS-e por chave de acesso com sucesso e retornar HTTP 200")
    void deveConsultarNfsePorChaveComSucesso() throws Exception {
        String chave = "32052001236030278000159000000003203726110114333510";
        String xmlNfse = "<NFSe versao=\"1.00\"><infNFSe Id=\"NFS" + chave + "\"></infNFSe></NFSe>";
        String gzipB64 = compactarGZipBase64(xmlNfse);

        RespostaConsultaNfse respostaSefin = new RespostaConsultaNfse(
                2,
                "Sefin_v1.0",
                "2026-11-02T16:00:00-03:00",
                100,
                "NFS-e localizada com sucesso",
                chave,
                gzipB64
        );

        when(certificadoProvider.obterCertificado(any(), any())).thenReturn(certificadoA1);
        when(consultarNfseUseCase.consultar(eq(chave), eq(AmbienteSefin.HOMOLOGACAO), any(CertificadoA1.class)))
                .thenReturn(respostaSefin);

        mockMvc.perform(get("/api/v1/nfse/" + chave)
                        .param("ambiente", "HOMOLOGACAO")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localizada").value(true))
                .andExpect(jsonPath("$.codigoStatus").value(100))
                .andExpect(jsonPath("$.motivoStatus").value("NFS-e localizada com sucesso"))
                .andExpect(jsonPath("$.chaveAcesso").value(chave))
                .andExpect(jsonPath("$.xmlNfse").value(xmlNfse));
    }

    @Test
    @DisplayName("Deve retornar HTTP 400 quando a chave de acesso não contiver 50 dígitos numéricos")
    void deveRetornarBadRequestQuandoChaveInvalida() throws Exception {
        mockMvc.perform(get("/api/v1/nfse/12345invalida")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusHttp").value(400))
                .andExpect(jsonPath("$.mensagem").value("Chave de acesso deve conter exatamente 50 dígitos numéricos."));
    }

    @Test
    @DisplayName("Deve retornar HTTP 502 quando Sefin falhar na consulta")
    void deveRetornarBadGatewayQuandoSefinFalhar() throws Exception {
        String chave = "32052001236030278000159000000003203726110114333510";
        when(certificadoProvider.obterCertificado(any(), any())).thenReturn(certificadoA1);
        when(consultarNfseUseCase.consultar(eq(chave), any(), any()))
                .thenThrow(new SefinTransmissaoException("Serviço Sefin temporariamente indisponível", 503, null, null));

        mockMvc.perform(get("/api/v1/nfse/" + chave)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.statusHttp").value(502))
                .andExpect(jsonPath("$.mensagem").value("Serviço Sefin temporariamente indisponível"));
    }
}
