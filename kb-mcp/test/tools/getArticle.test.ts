import { describe, expect, it } from 'vitest';
import { buildLinkedClient, parseToolTextResult } from '../helpers/testClient.js';

describe('get_article tool', () => {
  it('returns the full article for a known id', async () => {
    const client = await buildLinkedClient();
    const result = await client.callTool({ name: 'get_article', arguments: { id: 'widget-basics' } });
    const parsed = parseToolTextResult(result as any) as {
      id: string;
      title: string;
      category: string;
      tags: string[];
      body: string;
    };

    expect(parsed.id).toBe('widget-basics');
    expect(parsed.title).toBe('Widget basics');
    expect(parsed.category).toBe('alpha');
    expect(parsed.tags).toContain('widget');
    expect(parsed.body.length).toBeGreaterThan(0);
    expect((result as any).isError).toBeFalsy();
  });

  it('returns isError:true with a human-readable message for an unknown id', async () => {
    const client = await buildLinkedClient();
    const result = await client.callTool({ name: 'get_article', arguments: { id: 'does-not-exist' } });

    expect((result as any).isError).toBe(true);
    const textBlock = (result as any).content.find((block: any) => block.type === 'text');
    expect(textBlock.text).toMatch(/does-not-exist/);
  });
});
