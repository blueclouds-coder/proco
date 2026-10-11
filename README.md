# Proco

購買（調達）SaaS。標準業務機能をもとに UI・業務機能を刷新し、AI 機能を組み込む。

- 開発ルール: [CLAUDE.md](CLAUDE.md)
- 構成: [docs/architecture/overview.md](docs/architecture/overview.md)
- 要件: [docs/requirements/](docs/requirements/)
- 用語集: [docs/domain/glossary.md](docs/domain/glossary.md)

## ローカル起動

```bash
docker compose up -d
cd services && mvn -q verify
pnpm install && pnpm --filter web dev
```
