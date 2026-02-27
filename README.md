# Gestão de Restaurante (MVP)

Sistema simples em Java/Spring Boot para:

- Garçom lançar pedidos por mesa usando **somente produtos do estoque**.
- Cozinha visualizar fila de preparo e atualizar status (em fila/preparando/concluído).
- Caixa acompanhar comandas abertas e fechar apenas as que o garçom finalizou.
- Responsável de estoque cadastrar produtos e valores por categoria.

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

### Como parar a execução

No terminal onde o app está rodando, pressione:

```bash
Ctrl + C
```

## Banco H2

Console H2: `http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:mem:restaurante`
- User: `sa`
- Password: *(vazio)*

## Fluxo implementado

1. O responsável de estoque cadastra produtos com categoria e valor (há carga inicial para testes).
2. O garçom lança pedidos por mesa escolhendo itens do estoque.
3. O garçom acompanha comandas em aberto com status dos pedidos (em fila/preparando/concluído).
4. Quando solicitado pelo cliente, o garçom finaliza a comanda e envia para o caixa.
5. O caixa só consegue fechar comandas finalizadas pelo garçom.
6. Antes de fechar, o caixa abre um resumo detalhado da comanda com quantidade, descrição, valor unitário e total final.
7. O garçom consulta o histórico diário de comandas para facilitar visualização.
