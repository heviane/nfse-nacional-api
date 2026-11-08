package br.com.heviane.nfse.core.services;

import br.com.heviane.nfse.core.domain.AmbienteSefin;
import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.core.domain.RespostaConsultaNfse;
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
@DisplayName("Testes Unitários: ConsultarNfseService")
class ConsultarNfseServiceTest {

    @Mock
    private TransmissorSefinPort transmissorSefinPort;

    private ConsultarNfseService service;
    private CertificadoA1 certificado;
    private static final String CHAVE_50 = "32052002601000000000019100001000000000000001123456";

    @BeforeEach
    void setUp() {
        certificado = CertificadoTesteHelper.obterCertificadoTeste();
        service = new ConsultarNfseService(transmissorSefinPort);
    }

    @Test
    @DisplayName("Deve delegar consulta ao TransmissorSefinPort com sucesso")
    void deveDelegarConsultaAoTransmissorComSucesso() {
        RespostaConsultaNfse respostaMock = new RespostaConsultaNfse(
                2, "v1.0", "2026-11-08T18:00:00-03:00", 100, "NFS-e localizada",
                CHAVE_50, "H4sICAAA"
        );

        when(transmissorSefinPort.consultarPorChave(CHAVE_50, AmbienteSefin.HOMOLOGACAO, certificado))
                .thenReturn(respostaMock);

        RespostaConsultaNfse resultado = service.consultar(CHAVE_50, AmbienteSefin.HOMOLOGACAO, certificado);

        assertThat(resultado).isNotNull();
        assertThat(resultado.isEncontrada()).isTrue();
        assertThat(resultado.chaveAcesso()).isEqualTo(CHAVE_50);

        verify(transmissorSefinPort).consultarPorChave(CHAVE_50, AmbienteSefin.HOMOLOGACAO, certificado);
    }

    @Test
    @DisplayName("Deve rejeitar chave de acesso que não contenha exatamente 50 dígitos numéricos")
    void deveRejeitarChaveInvalida() {
        assertThatThrownBy(() -> service.consultar("123", AmbienteSefin.HOMOLOGACAO, certificado))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chave de acesso deve conter exatamente 50 dígitos numéricos");

        assertThatThrownBy(() -> service.consultar(null, AmbienteSefin.HOMOLOGACAO, certificado))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chave de acesso deve conter exatamente 50 dígitos numéricos");
    }

    @Test
    @DisplayName("Deve validar que Certificado A1 não pode ser nulo")
    void deveValidarCertificadoObrigatorio() {
        assertThatThrownBy(() -> service.consultar(CHAVE_50, AmbienteSefin.HOMOLOGACAO, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Certificado A1 é obrigatório para consulta fiscal");
    }
}

