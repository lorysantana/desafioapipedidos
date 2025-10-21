# 🚀 Desafio Técnico --- Vertical Logística (LuizaLabs)

Sistema que recebe um arquivo legado via API REST, normaliza os dados e retorna um JSON com a estrutura pedida (`users` → `orders` → `products`).

---

## 1. O Desafio

Esta seção descreve os requisitos conforme fornecido no documento do desafio técnico.

### 1.1. Objetivo

O objetivo é criar um sistema que receba um arquivo via API REST e o processe para que os dados normalizados sejam retornados também via API REST.

### 1.2. Entrada de Dados

O sistema deve processar um arquivo legado onde cada linha representa uma parte de um pedido. Os dados são padronizados por tamanho fixo, conforme a tabela a seguir:

| campo            | tamanho | tipo                         |
|:-----------------|:--------|:-----------------------------|
| id usuário       | 10      | numérico                     |
| nome             | 45      | texto                        |
| id pedido        | 10      | numérico                     |
| id produto       | 10      | numérico                     |
| valor do produto | 12      | decimal                      |
| data compra      | 8       | numérico (formato: yyyymmdd) |


**Observação de formatação:** Campos numéricos são completados com '0' à esquerda, e os demais campos (texto) com espaço à esquerda.

### 1.3. Saída de Dados (API REST)

A saída de dados deve ser uma API REST que retorna os dados agrupados por usuário. Cada usuário terá uma lista de seus pedidos, e cada pedido conterá o total (soma dos produtos) e a lista de produtos daquele pedido.

Além da consulta geral, a API de retorno deve permitir os seguintes filtros:
* ID do pedido;
* Intervalo de data de compra (data início e data fim).

---

## 2. A Solução Implementada

Esta seção detalha a arquitetura, tecnologias e o modo de execução da solução proposta para o desafio.

### 2.1. Tecnologias Utilizadas

* **Java 21**
* **Spring Boot** (Web, Data JPA, Test)
* **H2 Database** (in-memory)
* **Jakarta Persistence (JPA/Hibernate 6+)**
* **Lombok**
* **JUnit 5 / Mockito / AssertJ** para testes
* **Swagger / OpenAPI** para documentação da API

### 2.2. Pré-requisitos

Antes de executar o projeto, você precisa ter instalado:

* **JDK 21** ☕
* **Maven 3.6+**
* IDE com suporte a **Lombok** (IntelliJ, Eclipse)

### 2.3. Como Executar

**1. Build do Projeto**

```bash
# Limpa e empacota o projeto (gerando o .jar)
mvn clean package

```

**2\. Executar a Aplicação**

Bash

```
# Inicia a aplicação via Spring Boot
mvn spring-boot:run

```

*ou, executando o JAR gerado:*

Bash

```
java -jar target/desafio-vertical-logistica-0.0.1-SNAPSHOT.jar

```

A API estará disponível em: `http://localhost:8080`

### 2.4. Endpoints da API

#### 1️⃣ Upload e Processamento do Arquivo

Endpoint para enviar o arquivo `.txt` legado e receber o JSON processado.

**POST** `/api/v1/orders/upload`

-   **Request Body**: `multipart/form-data` com o campo `file`.

-   **Response**: `200 OK` com o JSON normalizado (`List<UserResponseDto>`).

**Exemplo (curl):**

Bash

```
curl -X POST "http://localhost:8080/api/files/process"\
     -F "file=@/caminho/para/seu/arquivo.txt;type=text/plain"\
     -H "Accept: application/json"

```

#### 2️⃣ Consulta de Pedidos

Endpoint para consultar os dados processados (previamente persistidos no banco H2).

**GET** `/api/v1/orders`

-   **Parâmetros Opcionais (Query Params):**

    -   `orderId` (long): Filtra por um ID de pedido específico.

    -   `startDate` (String `yyyy-MM-dd`): Data inicial do filtro de período.

    -   `endDate` (String `yyyy-MM-dd`): Data final do filtro de período.

-   **Response**: `200 OK` com o JSON filtrado (`List<UserResponseDto>`).

**Exemplos (GET):**

Bash

```
# Busca todos os pedidos
GET http://localhost:8080/api/v1/orders

# Busca por ID de pedido
GET http://localhost:8080/api/v1/orders?orderId=123

# Busca por intervalo de datas
GET http://localhost:8080/api/v1/orders?startDate=2021-01-01&endDate=2021-12-31

```

#### 3️⃣ Documentação Swagger

A API está documentada e pode ser acessada via Swagger UI para testes e visualização dos schemas:

🌐 **http://localhost:8080/swagger-ui.html**

### 2.5. Interpretação do Parser (Formato de Entrada)

Para o parsing do arquivo de entrada, foi implementado um parser de linha (posicional) baseado nos requisitos. Cada linha (95 caracteres) é dividida da seguinte forma:

| Campo     | Posição (start/end) | Tamanho | Observação                 |
|-----------|---------------------|---------|----------------------------|
| userId    | 0..9                | 10      | numérico, zeros à esquerda |
| userName  | 10..54              | 45      | texto, espaços à direita   |
| orderId   | 55..64              | 10      | numérico, zeros à esquerda |
| productId | 65..74              | 10      | numérico, zeros à esquerda |
| value     | 75..86              | 12      | decimal (parser flexível)  |
| date      | 87..94              | 8       | `yyyyMMdd`                 |



**Exemplo de linha de entrada:**

```
0000000002                 Medeiros00000123450000000111     256.2420201201
0000000001                  Zarelli00000001230000000111     512.2420211201

```

### 2.6. Formato de Saída (JSON)

A API retorna uma lista de usuários, seguindo a estrutura `snake_case` para os campos JSON:

JSON

```
[
  {
    "user_id": 1,
    "name": "Zarelli",
    "orders": [
      {
        "order_id": 123,
        "total": "1024.48",
        "date": "2021-12-01",
        "products": [
          { "product_id": 111, "value": "512.24" },
          { "product_id": 122, "value": "512.24" }
        ]
      }
    ]
  },
  {
    "user_id": 2,
    "name": "Medeiros",
    "orders": [
      {
        "order_id": 12345,
        "total": "512.48",
        "date": "2020-12-01",
        "products": [
          { "product_id": 111, "value": "256.24" },
          { "product_id": 122, "value": "256.24" }
        ]
      }
    ]
  }
]

```

### 2.7. Arquitetura e Decisões de Projeto

-   **Parser Dedicado:** A lógica de parsing (`LegacyLineParser`) é isolada, facilitando testes unitários e manutenção.


-   **Normalização em Memória:** O serviço processa o arquivo, normaliza os dados em DTOs e, em seguida, persiste no banco H2. Isso evita duplicidade de dados caso o mesmo arquivo seja enviado múltiplas vezes (usando `findOrCreate`).


-   **Entidades JPA:** Foram criadas entidades `UserEntity`, `OrderEntity`, `ProductEntity` e uma entidade associativa `OrderItemEntity` para modelar o relacionamento N:N (Pedido ↔ Produto).


-   **Transactional Boundary:** O processamento e persistência do arquivo são envoltos em uma transação (`@Transactional`) para garantir consistência.


-   **Swagger:** A documentação da API foi gerada com Swagger/OpenAPI para facilitar o teste e a compreensão dos endpoints.


### 2.8. Testes

A solução inclui testes unitários e de integração para garantir a qualidade e o funcionamento correto da lógica de negócio.

-   **Testes Unitários:**

    -   `LegacyLineParserTest`: Valida o parsing correto de cada campo da linha (valores, datas, padding).

    -   `FileProcessingServiceUnitTest`: Testa a lógica de agregação (soma de totais, agrupamento) com repositórios mockados.

-   **Testes de Integração:**

    -   `FileProcessingServiceIntegrationTest`: Processa um arquivo de teste real e valida a persistência e recuperação dos dados no banco H2.

    -   `RepositoriesIntegrationTest`: Valida os mapeamentos JPA e os relacionamentos entre as entidades.

**Para rodar todos os testes:**

Bash

```
mvn test
```

### 2.9. Observações

-   **Validação de Linha:** Linhas com menos de 95 caracteres são consideradas inválidas e lançam uma `BadFileFormatException`.

-   **Valores Monetários:** O parser aceita valores decimais com ponto (ex: `256.24`) ou como centavos (ex: `00000025624`).