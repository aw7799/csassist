import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { createRepository } from './kb/repository.js';
import { registerGetArticleTool } from './tools/getArticle.js';
import { registerListCategoriesTool } from './tools/listCategories.js';
import { registerSearchKbTool } from './tools/searchKb.js';

export interface CreateServerOptions {
  dataDir?: string;
}

export async function createServer(options?: CreateServerOptions): Promise<McpServer> {
  const server = new McpServer({ name: 'csassist-kb', version: '0.1.0' });
  const repo = await createRepository(options?.dataDir);
  registerSearchKbTool(server, repo);
  registerGetArticleTool(server, repo);
  registerListCategoriesTool(server, repo);
  return server;
}
