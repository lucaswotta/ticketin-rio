package dev.lucaswotta.popinrio.ticketing.domain.sale;

/**
 * Retirada de ingressos do estoque de uma venda, segura sob concorrência.
 *
 * <p>É aqui que mora o problema central do sistema: milhares de requisições disputando a mesma
 * linha do banco. Cada implementação é uma estratégia diferente de concorrência, e todas precisam
 * garantir a mesma coisa: nunca retirar mais do que existe.
 */
public interface TicketInventory {

    /**
     * Tenta retirar {@code quantity} ingressos da venda.
     *
     * @return {@code true} se retirou; {@code false} se a venda não existe ou não tem estoque
     *     suficiente. Quem chama distingue os dois casos, porque o caminho feliz não deve pagar por
     *     uma consulta extra.
     */
    boolean tryWithdraw(SaleId saleId, TicketQuantity quantity);
}
