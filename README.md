Bezerra Ranch - Desktop (Kotlin + Compose)

Estrutura:
- `frontend/` — aplicação Compose Desktop
- `backend/` — código backend (leitura de .env, supabase placeholder, export PDF)
- `assets/` — ícones para os botões (coloque PNGs aqui)

Como rodar (requere Java 11+, Gradle):

```bash
cd frontend
./gradlew run
```

Coloque suas variáveis `SUPABASE_URL` e `SUPABASE_KEY` em `.env` na raiz (já ignorado pelo git).

O botão `Exportar PDF` captura a área central do app e salva `export_report_*.pdf` no diretório do projeto.
