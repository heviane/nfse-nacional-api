package br.com.heviane.nfse.core.ports.in;

import br.com.heviane.nfse.core.domain.AmbienteSefin;
import br.com.heviane.nfse.core.domain.CertificadoA1;
import br.com.heviane.nfse.core.domain.RespostaEmissaoSefin;

/**
 * Caso de uso: Validação, assinatura digital e transmissão síncrona da DPS ao Sefin Nacional.
 */
public interface EmitirDpsUseCase {

    /**
     * Valida o XML contra o XSD oficial, assina digitalmente com o certificado A1
     * e transmite síncronamente ao Sefin Nacional.
     *
     * @param xmlDps XML original da DPS
     * @param ambiente Ambiente alvo (Homologação ou Produção)
     * @param certificado Certificado digital A1 em memória
     * @return Resposta do Sefin Nacional com chave de acesso e nota autorizada
     */
    RespostaEmissaoSefin emitir(String xmlDps, AmbienteSefin ambiente, CertificadoA1 certificado);
}

