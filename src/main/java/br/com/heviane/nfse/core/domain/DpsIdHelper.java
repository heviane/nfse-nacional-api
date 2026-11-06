package br.com.heviane.nfse.core.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Utilitário de domínio para geração e validação do atributo {@code Id} da DPS ({@code infDPS}).
 * <p>
 * Em conformidade estrita com a regra oficial do tipo {@code TSIdDPS} dos Schemas XSD do Sefin Nacional:
 * <pre>
 * "DPS" (3) + Cód.Mun (7) + TipoInsc (1: 1=CNPJ, 2=CPF) + Inscrição Federal (14) + Série DPS (5) + Núm. DPS (15) = 45 caracteres
 * Regex: ^DPS[0-9]{42}$
 * </pre>
 */
public final class DpsIdHelper {

    public static final String PREFIXO_DPS = "DPS";
    public static final int TAMANHO_TOTAL_ID = 45;
    public static final int TAMANHO_DIGITOS = 42;

    public static final String TIPO_INSC_CNPJ = "1";
    public static final String TIPO_INSC_CPF = "2";

    private static final Pattern PATTERN_ID_DPS = Pattern.compile("^DPS[0-9]{42}$");

    private DpsIdHelper() {
        // Construtor privado para classe utilitária estática
    }

    /**
     * Valida se a string fornecida atende rigorosamente ao padrão {@code TSIdDPS} da NFS-e Nacional.
     *
     * @param idDps Identificador a validar (ex: "DPS320520010000000000019100001000000000000001")
     * @return {@code true} se o Id for válido; {@code false} caso contrário
     */
    public static boolean isValido(String idDps) {
        return idDps != null && PATTERN_ID_DPS.matcher(idDps).matches();
    }

    /**
     * Gera o identificador oficial de 45 caracteres da DPS a partir dos campos fundamentais.
     *
     * @param cMun             Código IBGE do município emissor (7 dígitos)
     * @param tipoInsc         Tipo de inscrição do emitente ("1" para CNPJ, "2" para CPF)
     * @param inscricaoFederal CNPJ (14 dígitos) ou CPF (11 dígitos, que será completado com 3 zeros à esquerda)
     * @param serie            Série da DPS (completada com zeros à esquerda até 5 posições)
     * @param nDPS             Número da DPS (completado com zeros à esquerda até 15 posições)
     * @return Identificador canônico formatado com exatamente 45 posições (ex: "DPS320520010000000000019100001000000000000001")
     */
    public static String gerarId(String cMun, String tipoInsc, String inscricaoFederal, String serie, String nDPS) {
        Objects.requireNonNull(cMun, "Código do município (cMun) não pode ser nulo.");
        Objects.requireNonNull(tipoInsc, "Tipo de inscrição (tipoInsc) não pode ser nulo.");
        Objects.requireNonNull(inscricaoFederal, "Inscrição Federal (CNPJ/CPF) não pode ser nula.");
        Objects.requireNonNull(serie, "Série da DPS não pode ser nula.");
        Objects.requireNonNull(nDPS, "Número da DPS (nDPS) não pode ser nulo.");

        String cMunLimpo = extrairDigitos(cMun);
        if (cMunLimpo.length() != 7) {
            throw new IllegalArgumentException("Código do município (cMun) deve conter exatamente 7 dígitos numéricos IBGE: " + cMun);
        }

        String tipoLimpo = extrairDigitos(tipoInsc);
        if (!TIPO_INSC_CNPJ.equals(tipoLimpo) && !TIPO_INSC_CPF.equals(tipoLimpo)) {
            throw new IllegalArgumentException("Tipo de inscrição inválido (esperado '1' para CNPJ ou '2' para CPF): " + tipoInsc);
        }

        String inscLimpa = extrairDigitos(inscricaoFederal);
        String inscFormatada;
        if (TIPO_INSC_CNPJ.equals(tipoLimpo)) {
            if (inscLimpa.length() != 14) {
                throw new IllegalArgumentException("CNPJ deve conter exatamente 14 dígitos numéricos: " + inscricaoFederal);
            }
            inscFormatada = inscLimpa;
        } else {
            if (inscLimpa.length() > 11) {
                throw new IllegalArgumentException("CPF deve conter no máximo 11 dígitos numéricos: " + inscricaoFederal);
            }
            // Regra oficial do Sefin Nacional: CPF completar com zeros à esquerda até 14 posições
            inscFormatada = preencherZerosEsquerda(inscLimpa, 14);
        }

        String serieLimpa = extrairDigitos(serie);
        if (serieLimpa.isEmpty() || serieLimpa.length() > 5) {
            throw new IllegalArgumentException("Série da DPS deve conter entre 1 e 5 dígitos: " + serie);
        }
        String serieFormatada = preencherZerosEsquerda(serieLimpa, 5);

        String nDpsLimpo = extrairDigitos(nDPS);
        if (nDpsLimpo.isEmpty() || nDpsLimpo.length() > 15) {
            throw new IllegalArgumentException("Número da DPS (nDPS) deve conter entre 1 e 15 dígitos: " + nDPS);
        }
        String nDpsFormatado = preencherZerosEsquerda(nDpsLimpo, 15);

        String idGerado = PREFIXO_DPS + cMunLimpo + tipoLimpo + inscFormatada + serieFormatada + nDpsFormatado;

        if (!isValido(idGerado)) {
            throw new IllegalStateException("Falha interna ao gerar Id da DPS. Formato resultante inválido: " + idGerado);
        }

        return idGerado;
    }

    /**
     * Sobrecarga conveniente para emitentes pessoas jurídicas (CNPJ, tipoInsc = "1").
     */
    public static String gerarIdParaCnpj(String cMun, String cnpj, String serie, String nDPS) {
        return gerarId(cMun, TIPO_INSC_CNPJ, cnpj, serie, nDPS);
    }

    /**
     * Sobrecarga conveniente para emitentes pessoas físicas (CPF, tipoInsc = "2").
     */
    public static String gerarIdParaCpf(String cMun, String cpf, String serie, String nDPS) {
        return gerarId(cMun, TIPO_INSC_CPF, cpf, serie, nDPS);
    }

    private static String extrairDigitos(String texto) {
        return texto.replaceAll("\\D", "");
    }

    private static String preencherZerosEsquerda(String texto, int tamanho) {
        if (texto.length() >= tamanho) {
            return texto;
        }
        return "0".repeat(tamanho - texto.length()) + texto;
    }
}

