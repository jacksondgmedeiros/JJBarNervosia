# Gestão de Restaurante (MVP)

Sistema simples em Java/Spring Boot para:

- Garçom lançar pedidos por mesa.
- Cozinha visualizar fila de preparo e atualizar status.
- Caixa acompanhar comandas abertas e fechar comanda.

## Stack

- Java 17
- Spring Boot 3
- Spring Web + Spring Data JPA
- H2 (banco em memória)
- Front-end com Bootstrap 5 + JavaScript

## Como rodar

```bash
mvn spring-boot:run
```

Abra: `http://localhost:8080`

Console H2: `http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:mem:restaurante`
- User: `sa`
- Password: *(vazio)*

## Fluxo implementado

1. Garçom lança pedido da mesa (cada lançamento gera um ticket para a cozinha).
2. Se a mesa pedir mais itens, o garçom lança novamente e os itens são adicionados na mesma comanda aberta.
3. Cozinha vê pedidos pendentes/em preparo e marca como concluídos.
4. Caixa visualiza a comanda consolidada por mesa e fecha quando necessário.
