package br.com.heviane.nfse.core.domain;

import java.util.regex.Pattern;

/**
 * Utilitário de domínio para formação e validação do identificador único do Pedido de Registro
 * de Eventos da NFS-e Nacional (atributo 'Id' da tag infPedReg), conforme a especificação dos
 * esquemas XSD oficiais (tipos 'TSIdPedRefEvt' em v1.00 e 'TSIdPedRegEvt' em v1.01).
 * <p>
 * Regra de formação oficial:
 * <ul>
 *   <li>Literal <b>"PRE"</b> (3 posições)</li>
 *   <li>Chave de Acesso da NFS-e (50 dígitos numéricos)</li>
 *   <li>Código do Tipo de Evento (6 dígitos numéricos, ex: "101101" para cancelamento)</li>
 * </ul>
 * <b>Tamanho total:</b> exatamente 59 caracteres (formato regex: <code>PRE[0-9]{56}</code>).
 */
public final class PedRegEventoIdHelper {

    public static final String TIPO_EVENTO_CANCELAMENTO = "101101";
    public static final String TIPO_EVENTO_CANCELAMENTO_SUBSTITUICAO = "105102";

    private static final String PREFIXO_ID = "PRE";
    private static final Pattern PATTERN_ID_OFICIAL = Pattern.compile("^PRE\\d{56}$");

    private PedRegEventoIdHelper() {
        // Classe utilitária pura
    }

    /**
     * Gera o identificador oficial para um Pedido de Registro de Evento da NFS-e.
     *
     * @param chaveAcesso Chave de acesso da NFS-e vinculada (50 dígitos numéricos)
     * @param tipoEvento  Código do tipo de evento (6 dígitos numéricos)
     * @return Identificador no formato oficial {@code PRE[0-9]{56}} (59 caracteres)
     * @throws IllegalArgumentException caso os parâmetros não atendam às regras do schema
     */
    public static String gerarId(String chaveAcesso, String tipoEvento) {
        if (chaveAcesso == null || !chaveAcesso.matches("\\d{50}")) {
            throw new IllegalArgumentException("Chave de acesso da NFS-e deve conter exatamente 50 dígitos numéricos.");
        }
        if (tipoEvento == null || !tipoEvento.matches("\\d{6}")) {
            throw new IllegalArgumentException("Código do tipo de evento deve conter exatamente 6 dígitos numéricos (ex: 101101).");
        }

        String id = PREFIXO_ID + chaveAcesso + tipoEvento;

        if (id.length() != 59) {
            throw new IllegalStateException("O Id do pedido de evento gerado deve conter exatamente 59 caracteres.");
        }

        return id;
    }

    /**
     * Gera o identificador oficial para o evento de Cancelamento de NFS-e (Evento 101101).
     *
     * @param chaveAcesso Chave de acesso da NFS-e vinculada (50 dígitos numéricos)
     * @return Identificador formatado no padrão {@code PRE[0-9]{56}}
     */
    public static String gerarIdCancelamento(String chaveAcesso) {
        return gerarId(chaveAcesso, TIPO_EVENTO_CANCELAMENTO);
    }

    /**
     * Valida sintaticamente se uma String atende à regra de identificador oficial {@code TSIdPedRefEvt} / {@code TSIdPedRegEvt}.
     *
     * @param id Identificador a ser validado
     * @return {@code true} se for válido; {@code false} caso contrário
     */
    public static boolean isValido(String id) {
        if (id == null || id.length() != 59) {
            return false;
        }
        return PATTERN_ID_OFICIAL.matcher(id).matches();
    }
}

