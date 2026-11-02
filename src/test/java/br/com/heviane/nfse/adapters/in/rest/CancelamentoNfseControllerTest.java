package br.com.heviane.nfse.adapters.in.rest;

import br.com.heviane.nfse.adapters.in.rest.dto.CancelamentoNfseRequest;
import br.com.heviane.nfse.core.domain.AmbienteSefin;
import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.core.domain.RespostaEventoCancelamento;
import br.com.heviane.nfse.core.domain.SefinTransmissaoException;
import br.com.heviane.nfse.core.ports.in.CancelarNfseUseCase;
import br.com.heviane.nfse.core.services.CertificadoProvider;
import br.com.heviane.nfse.testutils.CertificadoTesteHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CancelamentoNfseController.class)
class CancelamentoNfseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CancelarNfseUseCase cancelarNfseUseCase;

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
    @DisplayName("Deve registrar evento de cancelamento com sucesso e retornar HTTP 200")
    void deveCancelarNfseComSucesso() throws Exception {
        String chave = "32052002601000000000019100001000000000000001123456";
        String idEvento = "E1011013205200260100000000001910000100000000000000112345601";
        String xmlEvento = "<retEvento><infEvento Id=\"" + idEvento + "\"></infEvento></retEvento>";
        String gzipB64 = compactarGZipBase64(xmlEvento);

        RespostaEventoCancelamento respostaSefin = new RespostaEventoCancelamento(
                2,
                "Sefin_v1.0",
                "2026-11-02T16:00:00-03:00",
                135,
                "Evento registrado e vinculado a NFS-e",
                chave,
                idEvento,
                gzipB64
        );

        when(certificadoProvider.obterCertificado(any(), any())).thenReturn(certificadoA1);
        when(cancelarNfseUseCase.cancelar(any(), any())).thenReturn(respostaSefin);

        CancelamentoNfseRequest request = new CancelamentoNfseRequest(
                chave,
                "00000000000191",
                "1",
                "Cancelamento solicitado devido a erro de emissao pelo prestador",
                AmbienteSefin.HOMOLOGACAO
        );

        mockMvc.perform(post("/api/v1/nfse/cancelar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.codigoStatus").value(135))
                .andExpect(jsonPath("$.motivoStatus").value("Evento registrado e vinculado a NFS-e"))
                .andExpect(jsonPath("$.chaveAcesso").value(chave))
                .andExpect(jsonPath("$.idEvento").value(idEvento))
                .andExpect(jsonPath("$.xmlEvento").value(xmlEvento));
    }

    @Test
    @DisplayName("Deve retornar HTTP 400 quando a chave de acesso for inválida no cancelamento")
    void deveRetornarBadRequestQuandoChaveInvalida() throws Exception {
        when(certificadoProvider.obterCertificado(any(), any())).thenReturn(certificadoA1);

        CancelamentoNfseRequest request = new CancelamentoNfseRequest(
                "chave_invalida_curta",
                "00000000000191",
                "1",
                "Cancelamento solicitado devido a erro de emissao pelo prestador",
                AmbienteSefin.HOMOLOGACAO
        );

        mockMvc.perform(post("/api/v1/nfse/cancelar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusHttp").value(400))
                .andExpect(jsonPath("$.mensagem").value("Chave de acesso deve conter exatamente 50 dígitos numéricos."));
    }

    @Test
    @DisplayName("Deve retornar HTTP 400 quando a justificativa tiver menos de 15 caracteres")
    void deveRetornarBadRequestQuandoJustificativaCurta() throws Exception {
        when(certificadoProvider.obterCertificado(any(), any())).thenReturn(certificadoA1);

        CancelamentoNfseRequest request = new CancelamentoNfseRequest(
                "32052002601000000000019100001000000000000001123456",
                "00000000000191",
                "1",
                "Curto",
                AmbienteSefin.HOMOLOGACAO
        );

        mockMvc.perform(post("/api/v1/nfse/cancelar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusHttp").value(400))
                .andExpect(jsonPath("$.mensagem").value("Justificativa de cancelamento deve conter no mínimo 15 caracteres."));
    }

    @Test
    @DisplayName("Deve retornar HTTP 502 quando Sefin falhar no cancelamento")
    void deveRetornarBadGatewayQuandoSefinFalhar() throws Exception {
        when(certificadoProvider.obterCertificado(any(), any())).thenReturn(certificadoA1);
        when(cancelarNfseUseCase.cancelar(any(), any()))
                .thenThrow(new SefinTransmissaoException("Erro no servidor de eventos Sefin", 500, null, null));

        CancelamentoNfseRequest request = new CancelamentoNfseRequest(
                "32052002601000000000019100001000000000000001123456",
                "00000000000191",
                "1",
                "Cancelamento solicitado devido a erro de emissao pelo prestador",
                AmbienteSefin.HOMOLOGACAO
        );

        mockMvc.perform(post("/api/v1/nfse/cancelar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.statusHttp").value(502))
                .andExpect(jsonPath("$.mensagem").value("Erro no servidor de eventos Sefin"));
    }
}
