import { z } from 'zod';
import type { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import type { RepositoryApi } from '../kb/repository.js';

const outputShape = {
  categories: z.array(
    z.object({
      category: z.string(),
      count: z.number(),
    }),
  ),
};

export function registerListCategoriesTool(server: McpServer, repo: RepositoryApi): void {
  server.registerTool(
    'list_categories',
    {
      title: 'List KB categories',
      description: 'List all KB article categories with the number of articles in each.',
      outputSchema: outputShape,
    },
    async () => {
      const structuredContent = { categories: repo.listCategories() };
      return {
        content: [{ type: 'text', text: JSON.stringify(structuredContent, null, 2) }],
        structuredContent,
      };
    },
  );
}
