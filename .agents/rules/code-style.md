# Regra de Estilo: Condicionais if de Instrução Única

## Diretriz
Todo `if` cujo bloco de execução contenha apenas uma única instrução (guard clauses, validações, `throw`, `return`, `continue`, `break`, ou atribuição simples) **NUNCA deve abrir chaves `{}`**.

- O comando subsequente deve ser indentado na linha imediatamente abaixo.
- Chaves `{}` são reservadas **exclusivamente** para blocos condicionais que contenham duas ou mais instruções.

## Exemplos

### ✅ Correto:
```java
if (carteiraId == null)
    throw new DomainException("O ID da carteira é obrigatório.");

if (linhaLimpa.isEmpty())
    continue;

if (domain.getId() != null)
    entity.setId(domain.getId());

if (ativos == null || ativos.isEmpty())
    return 0.0;
```

### ❌ Incorreto:
```java
if (carteiraId == null) {
    throw new DomainException("O ID da carteira é obrigatório.");
}

if (linhaLimpa.isEmpty()) {
    continue;
}
```
