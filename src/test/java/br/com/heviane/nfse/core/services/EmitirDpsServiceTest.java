package br.com.heviane.nfse.core.services;

import br.com.heviane.nfse.core.domain.*;
import br.com.heviane.nfse.core.ports.out.AssinadorDpsPort;
import br.com.heviane.nfse.core.ports.out.TransmissorSefinPort;
import br.com.heviane.nfse.core.ports.out.ValidadorSchemaPort;
import br.com.heviane.nfse.testutils.CertificadoTesteHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários: EmitirDpsService e Respostas Assíncronas (COM-05)")
class EmitirDpsServiceTest {

    @Mock
    private ValidadorSchemaPort validadorSchemaPort;

    @Mock
    private AssinadorDpsPort assinadorDpsPort;

    @Mock
    private TransmissorSefinPort transmissorSefinPort;

    private EmitirDpsService service;
    private CertificadoA1 certificado;

    private static final String CHAVE_ACESSO_50 = "32052002601000000000019100001000000000000001123456";
    private static final String XML_DPS = "<DPS versao=\"1.00\"><infDPS Id=\"DPS123\"></infDPS></DPS>";
    private static final String XML_ASSINADO = "<DPS versao=\"1.00\"><infDPS Id=\"DPS123\"></infDPS><Signature/></DPS>";

    @BeforeEach
    void setUp() {
        certificado = CertificadoTesteHelper.obterCertificadoTeste();
        service = new EmitirDpsService(
                validadorSchemaPort,
                assinadorDpsPort,
                transmissorSefinPort,
                AsyncPollingConfig.semEspera(3)
        );
    }

    @Test
    @DisplayName("COM-05: Deve emitir DPS síncrona diretamente com sucesso (status 100)")
    void deveEmitirDpsSincronaComSucesso() {
        when(assinadorDpsPort.assinar(XML_DPS, certificado)).thenReturn(XML_ASSINADO);

        RespostaEmissaoSefin respostaSefin = new RespostaEmissaoSefin(
                2, "Sefin_1.0", "2026-11-05T12:00:00-03:00",
                100, "NFS-e gerada com sucesso", CHAVE_ACESSO_50, "H4sICAAA"
        );
        when(transmissorSefinPort.emitirDps(any(RequisicaoEnvioDps.class), eq(certificado)))
                .thenReturn(respostaSefin);

        RespostaEmissaoSefin resultado = service.emitir(XML_DPS, AmbienteSefin.HOMOLOGACAO, certificado);

        assertThat(resultado).isNotNull();
        assertThat(resultado.isSucesso()).isTrue();
        assertThat(resultado.codigoStatus()).isEqualTo(100);
        assertThat(resultado.chaveAcesso()).isEqualTo(CHAVE_ACESSO_50);

        verify(validadorSchemaPort).validarDpsOuFalhar(XML_DPS);
        verify(transmissorSefinPort, never()).consultarPorChave(anyString(), any(), any());
    }

    @Test
    @DisplayName("COM-05: Deve resolver resposta assíncrona (status 105) via polling com sucesso na 2ª tentativa")
    void deveResolverProcessamentoAssincronoViaPollingComSucesso() {
        when(assinadorDpsPort.assinar(XML_DPS, certificado)).thenReturn(XML_ASSINADO);

        RespostaEmissaoSefin respostaAssincrona = new RespostaEmissaoSefin(
                2, "Sefin_1.0", "2026-11-05T12:00:00-03:00",
                105, "Lote/DPS em processamento", CHAVE_ACESSO_50, null, "PROT-998877"
        );
        when(transmissorSefinPort.emitirDps(any(RequisicaoEnvioDps.class), eq(certificado)))
                .thenReturn(respostaAssincrona);

        RespostaConsultaNfse consultaSucesso = new RespostaConsultaNfse(
                2, "SefinNacional_v1.0", "2026-11-05T12:00:02-03:00",
                100, "NFS-e autorizada com sucesso", CHAVE_ACESSO_50, "H4sICAUTORIZADO"
        );

        when(transmissorSefinPort.consultarPorChave(CHAVE_ACESSO_50, AmbienteSefin.HOMOLOGACAO, certificado))
                .thenThrow(new SefinTransmissaoException("Ainda processando", 404, null, null))
                .thenReturn(consultaSucesso);

        RespostaEmissaoSefin resultado = service.emitir(XML_DPS, AmbienteSefin.HOMOLOGACAO, certificado);

        assertThat(resultado).isNotNull();
        assertThat(resultado.isSucesso()).isTrue();
        assertThat(resultado.codigoStatus()).isEqualTo(100);
        assertThat(resultado.chaveAcesso()).isEqualTo(CHAVE_ACESSO_50);
        assertThat(resultado.numeroProtocolo()).isEqualTo("PROT-998877");
        assertThat(resultado.nfseXmlGZipB64()).isEqualTo("H4sICAUTORIZADO");

        verify(transmissorSefinPort, times(2)).consultarPorChave(CHAVE_ACESSO_50, AmbienteSefin.HOMOLOGACAO, certificado);
    }

    @Test
    @DisplayName("COM-05: Deve retornar status 105 limpo após esgotar tentativas de polling sem autorização")
    void deveRetornarRespostaEmProcessamentoAposEsgotarPolling() {
        when(assinadorDpsPort.assinar(XML_DPS, certificado)).thenReturn(XML_ASSINADO);

        RespostaEmissaoSefin respostaAssincrona = new RespostaEmissaoSefin(
                2, "Sefin_1.0", "2026-11-05T12:00:00-03:00",
                105, "Lote/DPS em processamento", CHAVE_ACESSO_50, null, "PROT-112233"
        );
        when(transmissorSefinPort.emitirDps(any(RequisicaoEnvioDps.class), eq(certificado)))
                .thenReturn(respostaAssincrona);

        when(transmissorSefinPort.consultarPorChave(CHAVE_ACESSO_50, AmbienteSefin.HOMOLOGACAO, certificado))
                .thenThrow(new SefinTransmissaoException("Ainda não processado", 404, null, null));

        RespostaEmissaoSefin resultado = service.emitir(XML_DPS, AmbienteSefin.HOMOLOGACAO, certificado);

        assertThat(resultado).isNotNull();
        assertThat(resultado.isSucesso()).isFalse();
        assertThat(resultado.isEmProcessamento()).isTrue();
        assertThat(resultado.codigoStatus()).isEqualTo(105);
        assertThat(resultado.numeroProtocolo()).isEqualTo("PROT-112233");

        verify(transmissorSefinPort, times(3)).consultarPorChave(CHAVE_ACESSO_50, AmbienteSefin.HOMOLOGACAO, certificado);
    }

    @Test
    @DisplayName("COM-05: Não deve executar polling se resposta assíncrona não contiver chave de acesso")
    void deveRetornarStatus105DiretamenteQuandoChaveNula() {
        when(assinadorDpsPort.assinar(XML_DPS, certificado)).thenReturn(XML_ASSINADO);

        RespostaEmissaoSefin respostaSemChave = new RespostaEmissaoSefin(
                2, "Sefin_1.0", "2026-11-05T12:00:00-03:00",
                105, "Lote em processamento", null, null, "PROT-000111"
        );
        when(transmissorSefinPort.emitirDps(any(RequisicaoEnvioDps.class), eq(certificado)))
                .thenReturn(respostaSemChave);

        RespostaEmissaoSefin resultado = service.emitir(XML_DPS, AmbienteSefin.HOMOLOGACAO, certificado);

        assertThat(resultado).isNotNull();
        assertThat(resultado.isEmProcessamento()).isTrue();
        assertThat(resultado.chaveAcesso()).isNull();

        verify(transmissorSefinPort, never()).consultarPorChave(anyString(), any(), any());
    }

    @Test
    @DisplayName("Deve validar que XML da DPS é obrigatório")
    void deveValidarXmlDpsObrigatorio() {
        assertThatThrownBy(() -> service.emitir(null, AmbienteSefin.HOMOLOGACAO, certificado))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não pode ser nulo ou vazio");

        assertThatThrownBy(() -> service.emitir("   ", AmbienteSefin.HOMOLOGACAO, certificado))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não pode ser nulo ou vazio");
    }

    @Test
    @DisplayName("Deve rejeitar emissão com ValidacaoFiscalException quando XML contiver tpAmb=2 e servidor for PRODUCAO")
    void deveRejeitarEmissaoQuandoTpAmbDivergirDoAmbienteDoServidor() {
        String xmlComHomologacao = "<DPS versao=\"1.00\"><infDPS Id=\"DPS1\"><tpAmb>2</tpAmb></infDPS></DPS>";

        assertThatThrownBy(() -> service.emitir(xmlComHomologacao, AmbienteSefin.PRODUCAO, certificado))
                .isInstanceOf(ValidacaoFiscalException.class)
                .hasMessageContaining("Inconsistência de ambiente fiscal: O XML da DPS declara <tpAmb>2</tpAmb>, mas o serviço está configurado para operar em PRODUCAO (código 1).");

        verify(assinadorDpsPort, never()).assinar(anyString(), any());
        verify(transmissorSefinPort, never()).emitirDps(any(), any());
    }

    @Test
    @DisplayName("Deve rejeitar emissão com ValidacaoFiscalException quando XML contiver tpAmb=1 e servidor for HOMOLOGACAO")
    void deveRejeitarEmissaoQuandoTpAmbProducaoEnviadoParaServidorHomologacao() {
        String xmlComProducao = "<DPS versao=\"1.00\"><infDPS Id=\"DPS1\"><tpAmb>1</tpAmb></infDPS></DPS>";

        assertThatThrownBy(() -> service.emitir(xmlComProducao, AmbienteSefin.HOMOLOGACAO, certificado))
                .isInstanceOf(ValidacaoFiscalException.class)
                .hasMessageContaining("Inconsistência de ambiente fiscal: O XML da DPS declara <tpAmb>1</tpAmb>, mas o serviço está configurado para operar em HOMOLOGACAO (código 2).");

        verify(assinadorDpsPort, never()).assinar(anyString(), any());
        verify(transmissorSefinPort, never()).emitirDps(any(), any());
    }

    @Test
    @DisplayName("Deve permitir emissão mantendo XML 100% intacto quando tpAmb coincidir com o servidor")
    void devePermitirEmissaoMantendoXmlIntactoQuandoTpAmbCoincidirComServidor() {
        String xmlValido = "<DPS versao=\"1.00\"><infDPS Id=\"DPS1\"><tpAmb>2</tpAmb></infDPS></DPS>";

        when(assinadorDpsPort.assinar(eq(xmlValido), eq(certificado)))
                .thenReturn(xmlValido + "<Signature/>");

        RespostaEmissaoSefin respostaMock = new RespostaEmissaoSefin(
                2, "Sefin_1.0", "2026-11-05T12:00:00-03:00",
                100, "Autorizado", CHAVE_ACESSO_50, "GZIP"
        );
        when(transmissorSefinPort.emitirDps(any(RequisicaoEnvioDps.class), eq(certificado)))
                .thenReturn(respostaMock);

        RespostaEmissaoSefin resultado = service.emitir(xmlValido, AmbienteSefin.HOMOLOGACAO, certificado);

        assertThat(resultado).isNotNull();
        assertThat(resultado.isSucesso()).isTrue();
        // Garante que o XML passado ao assinador foi 100% idêntico ao original (imutabilidade)
        verify(assinadorDpsPort).assinar(eq(xmlValido), eq(certificado));
    }

    @Test
    @DisplayName("Deve validar que Certificado A1 é obrigatório")
    void deveValidarCertificadoObrigatorio() {
        assertThatThrownBy(() -> service.emitir(XML_DPS, AmbienteSefin.HOMOLOGACAO, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Certificado A1 é obrigatório");
    }
}
