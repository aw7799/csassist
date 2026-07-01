import { readdir, readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { extractSnippet, scoreArticle, tokenizeQuery } from './search.js';
import type { Article, SearchHit } from './types.js';

const REQUIRED_FIELDS = ['id', 'title', 'category', 'body', 'tags'] as const;

const DEFAULT_LIMIT = 5;
const MAX_LIMIT = 20;

export interface RepositoryApi {
  getArticleById(id: string): Article | undefined;
  listCategories(): { category: string; count: number }[];
  searchArticles(query: string, opts?: { category?: string; limit?: number }): SearchHit[];
}

export function validateArticleShape(data: unknown, sourceFile: string): Article {
  if (typeof data !== 'object' || data === null) {
    throw new Error(`Invalid KB article in ${sourceFile}: expected a JSON object`);
  }
  const record = data as Record<string, unknown>;
  for (const field of REQUIRED_FIELDS) {
    if (!(field in record) || record[field] === undefined || record[field] === null) {
      throw new Error(`Invalid KB article in ${sourceFile}: missing required field "${field}"`);
    }
  }
  return record as unknown as Article;
}

function defaultDataDir(): string {
  const here = path.dirname(fileURLToPath(import.meta.url));
  return path.join(here, '../../data');
}

export async function loadArticles(dataDir?: string): Promise<Article[]> {
  const resolvedDataDir = dataDir ?? defaultDataDir();
  const articlesDir = path.join(resolvedDataDir, 'articles');
  const files = (await readdir(articlesDir)).filter((file) => file.endsWith('.json'));

  const articles: Article[] = [];
  for (const file of files) {
    const raw = await readFile(path.join(articlesDir, file), 'utf-8');
    const parsed = JSON.parse(raw);
    articles.push(validateArticleShape(parsed, file));
  }
  return articles;
}

export async function createRepository(dataDir?: string): Promise<RepositoryApi> {
  const articles = await loadArticles(dataDir);
  const byId = new Map(articles.map((article) => [article.id, article]));

  return {
    getArticleById(id: string): Article | undefined {
      return byId.get(id);
    },

    listCategories(): { category: string; count: number }[] {
      const counts = new Map<string, number>();
      for (const article of articles) {
        counts.set(article.category, (counts.get(article.category) ?? 0) + 1);
      }
      return [...counts.entries()]
        .map(([category, count]) => ({ category, count }))
        .sort((a, b) => a.category.localeCompare(b.category));
    },

    searchArticles(query: string, opts?: { category?: string; limit?: number }): SearchHit[] {
      const terms = tokenizeQuery(query);
      const limit = Math.min(Math.max(opts?.limit ?? DEFAULT_LIMIT, 1), MAX_LIMIT);

      const candidates = opts?.category
        ? articles.filter((article) => article.category === opts.category)
        : articles;

      return candidates
        .map((article) => ({ article, score: scoreArticle(article, terms) }))
        .filter(({ score }) => score > 0)
        .sort((a, b) => b.score - a.score)
        .slice(0, limit)
        .map(({ article, score }) => ({
          id: article.id,
          title: article.title,
          category: article.category,
          score,
          snippet: extractSnippet(article.body, terms),
        }));
    },
  };
}
