import { describe, expect, it } from 'vitest';
import { buildLinkedClient, parseToolTextResult } from '../helpers/testClient.js';

describe('list_categories tool', () => {
  it('returns categories with counts matching the fixture data, alphabetically sorted', async () => {
    const client = await buildLinkedClient();
    const result = await client.callTool({ name: 'list_categories', arguments: {} });
    const parsed = parseToolTextResult(result as any) as {
      categories: { category: string; count: number }[];
    };

    expect(parsed.categories).toEqual([
      { category: 'alpha', count: 2 },
      { category: 'beta', count: 1 },
    ]);
    expect((result as any).isError).toBeFalsy();
  });
});
