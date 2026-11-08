package br.com.heviane.nfse.core.services;

import br.com.heviane.nfse.core.domain.*;
import br.com.heviane.nfse.core.ports.out.TransmissorSefinPort;
import br.com.heviane.nfse.testutils.CertificadoTesteHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários: CancelarNfseService")
class CancelarNfseServiceTest {

    @Mock
    private TransmissorSefinPort transmissorSefinPort;

    private CancelarNfseService service;
    private CertificadoA1 certificado;
    private static final String CHAVE_50 = "32052002601000000000019100001000000000000001123456";

    @BeforeEach
    void setUp() {
        certificado = CertificadoTesteHelper.obterCertificadoTeste();
        service = new CancelarNfseService(transmissorSefinPort);
    }

    @Test
    @DisplayName("Deve delegar cancelamento ao TransmissorSefinPort com sucesso")
    void deveDelegarCancelamentoAoTransmissorComSucesso() {
        RequisicaoCancelamentoDps requisicao = new RequisicaoCancelamentoDps(
                CHAVE_50, "00000000000191", "1", "Cancelamento por motivo de erro cadastral", AmbienteSefin.HOMOLOGACAO
        );

        RespostaEventoCancelamento respostaMock = new RespostaEventoCancelamento(
                2, "v1.0", "2026-11-08T18:00:00-03:00", 135, "Evento homologado e vinculado",
                CHAVE_50, "EVT123", "H4sICAAA"
        );

        when(transmissorSefinPort.cancelar(requisicao, certificado)).thenReturn(respostaMock);

        RespostaEventoCancelamento resultado = service.cancelar(requisicao, certificado);

        assertThat(resultado).isNotNull();
        assertThat(resultado.isSucesso()).isTrue();
        assertThat(resultado.codigoStatus()).isEqualTo(135);
        assertThat(resultado.idEvento()).isEqualTo("EVT123");

        verify(transmissorSefinPort).cancelar(requisicao, certificado);
    }

    @Test
    @DisplayName("Deve validar que requisição de cancelamento não pode ser nula")
    void deveValidarRequisicaoObrigatoria() {
        assertThatThrownBy(() -> service.cancelar(null, certificado))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requisição de cancelamento não pode ser nula");
    }

    @Test
    @DisplayName("Deve validar que Certificado A1 não pode ser nulo")
    void deveValidarCertificadoObrigatorio() {
        RequisicaoCancelamentoDps requisicao = new RequisicaoCancelamentoDps(
                CHAVE_50, "00000000000191", "1", "Cancelamento por motivo de erro cadastral", AmbienteSefin.HOMOLOGACAO
        );

        assertThatThrownBy(() -> service.cancelar(requisicao, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Certificado A1 é obrigatório para cancelamento fiscal");
    }
}

