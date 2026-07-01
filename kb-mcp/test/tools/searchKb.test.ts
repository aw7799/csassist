import { describe, expect, it } from 'vitest';
import { buildLinkedClient, parseToolTextResult } from '../helpers/testClient.js';

describe('search_kb tool', () => {
  it('returns matching articles across categories with a non-empty snippet', async () => {
    const client = await buildLinkedClient();
    const result = await client.callTool({ name: 'search_kb', arguments: { query: 'widget' } });
    const parsed = parseToolTextResult(result as any) as { results: Array<{ id: string; snippet: string }> };

    const ids = parsed.results.map((hit) => hit.id);
    expect(ids).toContain('widget-basics');
    expect(parsed.results[0].snippet.length).toBeGreaterThan(0);
  });

  it('narrows results with the category filter', async () => {
    const client = await buildLinkedClient();
    const result = await client.callTool({
      name: 'search_kb',
      arguments: { query: 'widget', category: 'alpha' },
    });
    const parsed = parseToolTextResult(result as any) as { results: Array<{ id: string; category: string }> };

    expect(parsed.results.every((hit) => hit.category === 'alpha')).toBe(true);
    expect(parsed.results.map((hit) => hit.id)).not.toContain('gadget-guide');
  });

  it('respects the limit parameter', async () => {
    const client = await buildLinkedClient();
    const result = await client.callTool({
      name: 'search_kb',
      arguments: { query: 'widget', limit: 1 },
    });
    const parsed = parseToolTextResult(result as any) as { results: unknown[] };
    expect(parsed.results).toHaveLength(1);
  });

  it('returns an empty results array (not an error) when nothing matches', async () => {
    const client = await buildLinkedClient();
    const result = await client.callTool({
      name: 'search_kb',
      arguments: { query: 'nonexistentterm' },
    });
    const parsed = parseToolTextResult(result as any) as { results: unknown[] };

    expect(parsed.results).toEqual([]);
    expect((result as any).isError).toBeFalsy();
  });

  it('includes structuredContent matching the declared output shape', async () => {
    const client = await buildLinkedClient();
    const result = await client.callTool({ name: 'search_kb', arguments: { query: 'widget' } });
    expect((result as any).structuredContent).toBeDefined();
    expect((result as any).structuredContent.results).toBeInstanceOf(Array);
  });
});
