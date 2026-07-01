import { z } from 'zod';
import type { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import type { RepositoryApi } from '../kb/repository.js';

const searchHitShape = {
  id: z.string(),
  title: z.string(),
  category: z.string(),
  score: z.number(),
  snippet: z.string(),
};

const outputShape = {
  results: z.array(z.object(searchHitShape)),
};

export function registerSearchKbTool(server: McpServer, repo: RepositoryApi): void {
  server.registerTool(
    'search_kb',
    {
      title: 'Search knowledge base',
      description: 'Search KB articles by free-text query, with optional category filter and result limit.',
      inputSchema: {
        query: z.string().min(1).describe("Free-text search query, e.g. 'reset my password'"),
        category: z.string().optional().describe("Optional category filter, e.g. 'account'"),
        limit: z.number().int().min(1).max(20).optional().describe('Max results to return (default 5)'),
      },
      outputSchema: outputShape,
    },
    async ({ query, category, limit }) => {
      const results = repo.searchArticles(query, { category, limit });
      const structuredContent = { results };
      return {
        content: [{ type: 'text', text: JSON.stringify(structuredContent, null, 2) }],
        structuredContent,
      };
    },
  );
}
