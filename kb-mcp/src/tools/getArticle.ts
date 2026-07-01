import { z } from 'zod';
import type { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import type { RepositoryApi } from '../kb/repository.js';

const outputShape = {
  id: z.string(),
  title: z.string(),
  category: z.string(),
  body: z.string(),
  tags: z.array(z.string()),
};

export function registerGetArticleTool(server: McpServer, repo: RepositoryApi): void {
  server.registerTool(
    'get_article',
    {
      title: 'Get KB article',
      description: 'Fetch a single KB article by its id, including the full markdown body.',
      inputSchema: {
        id: z.string().min(1).describe("Article id, e.g. 'password-reset'"),
      },
      outputSchema: outputShape,
    },
    async ({ id }) => {
      const article = repo.getArticleById(id);
      if (!article) {
        return {
          content: [{ type: 'text', text: `No article found with id "${id}".` }],
          isError: true,
        };
      }
      const structuredContent = {
        id: article.id,
        title: article.title,
        category: article.category,
        body: article.body,
        tags: article.tags,
      };
      return {
        content: [{ type: 'text', text: JSON.stringify(structuredContent, null, 2) }],
        structuredContent,
      };
    },
  );
}
