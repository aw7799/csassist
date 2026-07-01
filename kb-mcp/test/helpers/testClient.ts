import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { InMemoryTransport } from '@modelcontextprotocol/sdk/inMemory.js';
import { createServer } from '../../src/server.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
export const FIXTURE_DATA_DIR = path.join(__dirname, '../fixtures/data');

export async function buildLinkedClient(dataDir: string = FIXTURE_DATA_DIR): Promise<Client> {
  const server = await createServer({ dataDir });
  const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
  const client = new Client({ name: 'test-client', version: '0.0.0' });
  await Promise.all([client.connect(clientTransport), server.connect(serverTransport)]);
  return client;
}

export function parseToolTextResult(result: { content: Array<{ type: string; text?: string }> }): unknown {
  const textBlock = result.content.find((block) => block.type === 'text');
  if (!textBlock?.text) {
    throw new Error('Expected a text content block in tool result');
  }
  return JSON.parse(textBlock.text);
}
