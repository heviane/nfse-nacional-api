package br.com.heviane.nfse.adapters.in.rest;

import br.com.heviane.nfse.adapters.in.observability.FiscalAuditLogger;
import br.com.heviane.nfse.adapters.in.observability.NfseMetrics;
import br.com.heviane.nfse.adapters.in.observability.RegistroEmissaoAuditoria;
import br.com.heviane.nfse.adapters.in.observability.RegistroEmissaoDuravelService;
import br.com.heviane.nfse.core.domain.AmbienteSefin;
import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.core.domain.RespostaConsultaNfse;
import br.com.heviane.nfse.core.domain.SefinTransmissaoException;
import br.com.heviane.nfse.core.ports.in.ConsultarNfseUseCase;
import br.com.heviane.nfse.adapters.in.security.CertificadoProvider;
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
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
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

    @MockBean
    private NfseMetrics nfseMetrics;

    @MockBean
    private FiscalAuditLogger fiscalAuditLogger;

    @MockBean
    private RegistroEmissaoDuravelService registroEmissaoDuravelService;

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
        String chave = "32052002601000000000019100001000000000000001123456";
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
        String chave = "32052002601000000000019100001000000000000001123456";
        when(certificadoProvider.obterCertificado(any(), any())).thenReturn(certificadoA1);
        when(consultarNfseUseCase.consultar(eq(chave), any(), any()))
                .thenThrow(new SefinTransmissaoException("Serviço Sefin temporariamente indisponível", 503, null, null));

        mockMvc.perform(get("/api/v1/nfse/" + chave)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.statusHttp").value(502))
                .andExpect(jsonPath("$.mensagem").value("Serviço Sefin temporariamente indisponível"));
    }

    @Test
    @DisplayName("Deve recuperar registro do histórico local por Id da DPS com sucesso")
    void deveRecuperarRegistroHistoricoLocalPorIdDpsComSucesso() throws Exception {
        String idDps = "DPS32052002601000000000019100001000000000000001";
        String chave = "32052002601000000000019100001000000000000001123456";
        RegistroEmissaoAuditoria registro = new RegistroEmissaoAuditoria(
                Instant.now(),
                idDps,
                chave,
                "202611090001",
                100,
                "00.***.***/0001-**",
                "HOMOLOGACAO",
                "REST",
                "corr-abc"
        );

        when(registroEmissaoDuravelService.buscarPorIdDps(idDps)).thenReturn(Optional.of(registro));

        mockMvc.perform(get("/api/v1/nfse/historico-local/" + idDps)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idDps").value(idDps))
                .andExpect(jsonPath("$.chaveAcesso").value(chave))
                .andExpect(jsonPath("$.numeroProtocolo").value("202611090001"))
                .andExpect(jsonPath("$.statusFiscal").value(100))
                .andExpect(jsonPath("$.origem").value("REST"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 404 quando Id da DPS não existir no histórico local")
    void deveRetornarNotFoundQuandoIdDpsNaoEncontradoNoHistoricoLocal() throws Exception {
        when(registroEmissaoDuravelService.buscarPorIdDps("DPS_INEXISTENTE")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/nfse/historico-local/DPS_INEXISTENTE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
