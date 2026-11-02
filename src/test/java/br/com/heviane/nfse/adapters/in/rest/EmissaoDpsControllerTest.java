package br.com.heviane.nfse.adapters.in.rest;

import br.com.heviane.nfse.adapters.in.rest.dto.EmissaoDpsRequest;
import br.com.heviane.nfse.core.domain.*;
import br.com.heviane.nfse.core.ports.in.EmitirDpsUseCase;
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
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EmissaoDpsController.class)
class EmissaoDpsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmitirDpsUseCase emitirDpsUseCase;

    @MockBean
    private CertificadoProvider certificadoProvider;

    private CertificadoA1 certificadoA1;
    private String certBase64;

    @BeforeEach
    void setUp() throws Exception {
        certificadoA1 = CertificadoTesteHelper.obterCertificadoTeste();
        byte[] pfxBytes = certificadoA1.getInputStream().readAllBytes();
        certBase64 = Base64.getEncoder().encodeToString(pfxBytes);
    }

    private String compactarGZipBase64(String conteudo) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(conteudo.getBytes(StandardCharsets.UTF_8));
        }
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    @Test
    @DisplayName("Deve emitir DPS com sucesso e retornar HTTP 200 com NFS-e autorizada")
    void deveEmitirDpsComSucesso() throws Exception {
        String xmlDps = "<DPS><infDPS Id=\"DPS320520020000000000019100001000000000000001\"></infDPS></DPS>";
        String xmlNfse = "<NFSe versao=\"1.00\"><infNFSe Id=\"NFS32052002601000000000019100001000000000000001123456\"></infNFSe></NFSe>";
        String gzipB64 = compactarGZipBase64(xmlNfse);

        RespostaEmissaoSefin respostaSefin = new RespostaEmissaoSefin(
                2,
                "Sefin_v1.0",
                "2026-11-02T16:00:00-03:00",
                100,
                "NFS-e gerada com sucesso",
                "32052002601000000000019100001000000000000001123456",
                gzipB64
        );

        when(certificadoProvider.obterCertificado(eq(certBase64), eq("123456"))).thenReturn(certificadoA1);
        when(emitirDpsUseCase.emitir(eq(xmlDps), eq(AmbienteSefin.HOMOLOGACAO), any(CertificadoA1.class)))
                .thenReturn(respostaSefin);

        EmissaoDpsRequest request = new EmissaoDpsRequest(xmlDps, AmbienteSefin.HOMOLOGACAO);

        mockMvc.perform(post("/api/v1/dps/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Certificado-Base64", certBase64)
                        .header("X-Certificado-Senha", "123456")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.codigoStatus").value(100))
                .andExpect(jsonPath("$.motivoStatus").value("NFS-e gerada com sucesso"))
                .andExpect(jsonPath("$.chaveAcesso").value("32052002601000000000019100001000000000000001123456"))
                .andExpect(jsonPath("$.xmlNfse").value(xmlNfse));
    }

    @Test
    @DisplayName("Deve retornar HTTP 400 quando xmlDps não for informado")
    void deveRetornarBadRequestQuandoXmlDpsForNulo() throws Exception {
        EmissaoDpsRequest request = new EmissaoDpsRequest(null, AmbienteSefin.HOMOLOGACAO);

        mockMvc.perform(post("/api/v1/dps/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusHttp").value(400))
                .andExpect(jsonPath("$.mensagem").value("O conteúdo do XML da DPS (xmlDps) é obrigatório."));
    }

    @Test
    @DisplayName("Deve retornar HTTP 422 quando houver falha de validação contra o XSD")
    void deveRetornarUnprocessableEntityQuandoFalharValidacaoXsd() throws Exception {
        String xmlInvalido = "<DPS>invalido</DPS>";
        when(certificadoProvider.obterCertificado(any(), any())).thenReturn(certificadoA1);
        when(emitirDpsUseCase.emitir(eq(xmlInvalido), any(), any()))
                .thenThrow(new ValidacaoFiscalException("Falha na validação do schema XSD", List.of("cvc-complex-type.2.4.a: Linha 1: Elemento incorreto")));

        EmissaoDpsRequest request = new EmissaoDpsRequest(xmlInvalido, AmbienteSefin.HOMOLOGACAO);

        mockMvc.perform(post("/api/v1/dps/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.statusHttp").value(422))
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("Falha na validação do schema XSD")))
                .andExpect(jsonPath("$.detalhes[0]").value("cvc-complex-type.2.4.a: Linha 1: Elemento incorreto"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 502 quando Sefin falhar na transmissão")
    void deveRetornarBadGatewayQuandoFalharTransmissaoSefin() throws Exception {
        String xmlDps = "<DPS>valido</DPS>";
        when(certificadoProvider.obterCertificado(any(), any())).thenReturn(certificadoA1);
        when(emitirDpsUseCase.emitir(eq(xmlDps), any(), any()))
                .thenThrow(new SefinTransmissaoException("Erro interno no servidor do Sefin", 500, null, null));

        EmissaoDpsRequest request = new EmissaoDpsRequest(xmlDps, AmbienteSefin.HOMOLOGACAO);

        mockMvc.perform(post("/api/v1/dps/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.statusHttp").value(502))
                .andExpect(jsonPath("$.mensagem").value("Erro interno no servidor do Sefin"));
    }
}
