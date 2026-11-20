# SPEC-Driven Development 约束文档
- 参考 Notion Page ID: 37d0ec7a4b9280bd8c84c922f57f6d3e

# 全局研发环境与行为准则
- 参考 Notion Page ID: 37d0ec7a4b928005aee8d863af5b2518

# 开源车联网研发与行为准则
- 参考 Notion Page ID: 37d0ec7a4b92802d8915c7c310e24486

# 当前项目需求文档
- 参考 Notion Page ID: 37d0ec7a4b9280c5b30ff027cd4cea2c
- 变更记录 Notion Page ID: 3800ec7a4b9280199dfdd3a7b32af111

# 当前项目设计文档
- 参考 Notion Page ID: 37d0ec7a4b9280918af1e3b5763a6212
- 变更记录 Notion Page ID: 3800ec7a4b9280fb900ec147b3be42b6

# 当前项目系统架构文档
- 参考 Notion Page ID: 4120ec7a4b928052b6a0e9d302add7f9

# 当前项目功能架构文档
- 参考 Notion Page ID: 4140ec7a4b92806384dcd6250c3991aa

# 当前项目数据库文档
- 参考 Notion Page ID: 4140ec7a4b9280b0afe1e4ead9a577e3

# 当前项目错误码文档
- 参考 Notion Page ID: 41e0ec7a4b92801bbfbdd4d5e2dadad7

# graphify

This project has a graphify knowledge graph at graphify-out/.

Rules:
- Before answering architecture or codebase questions, read graphify-out/GRAPH_REPORT.md for god nodes and community structure
- If graphify-out/wiki/index.md exists, navigate it instead of reading raw files
- For cross-module "how does X relate to Y" questions, prefer `graphify query "<question>"`, `graphify path "<A>" "<B>"`, or `graphify explain "<concept>"` over grep — these traverse the graph's EXTRACTED + INFERRED edges instead of scanning files
- After modifying code files in this session, run `graphify update .` to keep the graph current (AST-only, no API cost)
