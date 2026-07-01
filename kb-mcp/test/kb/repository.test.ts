import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { createRepository, loadArticles, validateArticleShape } from '../../src/kb/repository.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const FIXTURE_DATA_DIR = path.join(__dirname, '../fixtures/data');

describe('loadArticles', () => {
  it('loads all fixture articles with the required fields', async () => {
    const articles = await loadArticles(FIXTURE_DATA_DIR);
    expect(articles).toHaveLength(3);
    for (const article of articles) {
      expect(article.id).toBeTruthy();
      expect(article.title).toBeTruthy();
      expect(article.category).toBeTruthy();
      expect(article.body).toBeTruthy();
      expect(Array.isArray(article.tags)).toBe(true);
    }
  });
});

describe('validateArticleShape', () => {
  it('throws a descriptive error naming the file when a required field is missing', () => {
    const malformed = { id: 'broken', title: 'Broken article', category: 'alpha', tags: [] };
    expect(() => validateArticleShape(malformed, 'broken.json')).toThrow(/broken\.json/);
    expect(() => validateArticleShape(malformed, 'broken.json')).toThrow(/body/);
  });

  it('returns the article unchanged when all required fields are present', () => {
    const valid = { id: 'ok', title: 'Ok', category: 'alpha', tags: ['x'], body: 'text' };
    expect(validateArticleShape(valid, 'ok.json')).toEqual(valid);
  });
});

describe('createRepository', () => {
  it('getArticleById returns the matching article and undefined for unknown ids', async () => {
    const repo = await createRepository(FIXTURE_DATA_DIR);
    expect(repo.getArticleById('widget-basics')?.title).toBe('Widget basics');
    expect(repo.getArticleById('does-not-exist')).toBeUndefined();
  });

  it('listCategories returns alphabetically sorted category counts', async () => {
    const repo = await createRepository(FIXTURE_DATA_DIR);
    expect(repo.listCategories()).toEqual([
      { category: 'alpha', count: 2 },
      { category: 'beta', count: 1 },
    ]);
  });

  it('searchArticles respects the category filter', async () => {
    const repo = await createRepository(FIXTURE_DATA_DIR);
    const unfiltered = repo.searchArticles('widget');
    const ids = unfiltered.map((hit) => hit.id);
    expect(ids).toContain('gadget-guide');

    const filtered = repo.searchArticles('widget', { category: 'alpha' });
    expect(filtered.every((hit) => hit.category === 'alpha')).toBe(true);
    expect(filtered.map((hit) => hit.id)).not.toContain('gadget-guide');
  });

  it('searchArticles respects limit and returns results in descending score order', async () => {
    const repo = await createRepository(FIXTURE_DATA_DIR);
    const limited = repo.searchArticles('widget', { limit: 1 });
    expect(limited).toHaveLength(1);

    const all = repo.searchArticles('widget');
    for (let i = 1; i < all.length; i += 1) {
      expect(all[i - 1].score).toBeGreaterThanOrEqual(all[i].score);
    }
    expect(limited[0].id).toBe(all[0].id);
  });
});
