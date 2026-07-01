import type { Article } from './types.js';

const STOPWORDS = new Set([
  'the', 'a', 'an', 'is', 'to', 'for', 'of', 'and', 'my', 'i', 'how', 'do', 'can',
]);

const BODY_HIT_CAP = 4;

export function tokenizeQuery(query: string): string[] {
  return query
    .toLowerCase()
    .split(/[^a-z0-9]+/)
    .filter((term) => term.length > 0 && !STOPWORDS.has(term));
}

export function scoreArticle(article: Article, queryTerms: string[]): number {
  const title = article.title.toLowerCase();
  const tags = article.tags.map((tag) => tag.toLowerCase());
  const body = article.body.toLowerCase();
  const normalizedQuery = queryTerms.join(' ');

  let score = 0;

  if (queryTerms.length > 1 && normalizedQuery.length > 0 && title.includes(normalizedQuery)) {
    score += 5;
  }

  for (const term of queryTerms) {
    if (title.includes(term)) {
      score += 10;
    }

    if (tags.includes(term)) {
      score += 6;
    } else if (tags.some((tag) => tag.includes(term))) {
      score += 3;
    }

    const bodyOccurrences = countOccurrences(body, term);
    if (bodyOccurrences > 0) {
      score += Math.min(bodyOccurrences, BODY_HIT_CAP);
    }
  }

  return score;
}

export function extractSnippet(body: string, queryTerms: string[], radius = 120): string {
  const plain = stripMarkdown(body);
  const lowerPlain = plain.toLowerCase();

  let matchIndex = -1;
  for (const term of queryTerms) {
    const index = lowerPlain.indexOf(term);
    if (index !== -1 && (matchIndex === -1 || index < matchIndex)) {
      matchIndex = index;
    }
  }

  if (matchIndex === -1) {
    const fallback = plain.slice(0, radius * 2).trim();
    return plain.length > radius * 2 ? `${fallback}…` : fallback;
  }

  const start = Math.max(0, matchIndex - radius);
  const end = Math.min(plain.length, matchIndex + radius);
  const middle = plain.slice(start, end).trim();

  const prefix = start > 0 ? '…' : '';
  const suffix = end < plain.length ? '…' : '';
  return `${prefix}${middle}${suffix}`;
}

function countOccurrences(haystack: string, needle: string): number {
  if (needle.length === 0) {
    return 0;
  }
  let count = 0;
  let index = haystack.indexOf(needle);
  while (index !== -1) {
    count += 1;
    index = haystack.indexOf(needle, index + needle.length);
  }
  return count;
}

function stripMarkdown(body: string): string {
  return body
    .split('\n')
    .map((line) => line.replace(/^\s*(#+|[-*]|\d+\.)\s*/, ''))
    .join('\n');
}
