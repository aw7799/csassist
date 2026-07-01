import { describe, expect, it } from 'vitest';
import { extractSnippet, scoreArticle, tokenizeQuery } from '../../src/kb/search.js';
import type { Article } from '../../src/kb/types.js';

function makeArticle(overrides: Partial<Article> = {}): Article {
  return {
    id: 'sample-article',
    title: 'Reset your password',
    category: 'account',
    tags: ['password', 'reset', 'login'],
    body: 'To reset your password, go to the login page and click forgot password. Enter your email and follow the link.',
    ...overrides,
  };
}

describe('tokenizeQuery', () => {
  it('lowercases and splits on whitespace/punctuation', () => {
    expect(tokenizeQuery('Reset MY Password!')).toEqual(['reset', 'password']);
  });

  it('drops stopwords', () => {
    expect(tokenizeQuery('How do I reset the password')).toEqual(['reset', 'password']);
  });

  it('returns an empty array for a query of only stopwords', () => {
    expect(tokenizeQuery('how do i')).toEqual([]);
  });
});

describe('scoreArticle', () => {
  it('returns 0 when no query terms match anywhere', () => {
    const article = makeArticle();
    expect(scoreArticle(article, ['printer', 'toner'])).toBe(0);
  });

  it('scores a title match higher than a body-only match for the same term', () => {
    const titleHit = makeArticle({ title: 'Reset your password', tags: [], body: 'no relevant terms here' });
    const bodyOnlyHit = makeArticle({
      title: 'Something unrelated',
      tags: [],
      body: 'this article mentions password once in the body',
    });
    expect(scoreArticle(titleHit, ['password'])).toBeGreaterThan(scoreArticle(bodyOnlyHit, ['password']));
  });

  it('scores a tag exact match higher than a tag substring match', () => {
    const exactTag = makeArticle({ title: 'Unrelated title', tags: ['pass'], body: 'no match' });
    const substringTag = makeArticle({ title: 'Unrelated title', tags: ['password'], body: 'no match' });
    expect(scoreArticle(exactTag, ['pass'])).toBeGreaterThan(scoreArticle(substringTag, ['pass']));
  });

  it('sums scores across multiple query terms', () => {
    const article = makeArticle();
    const singleTerm = scoreArticle(article, ['password']);
    const twoTerms = scoreArticle(article, ['password', 'reset']);
    expect(twoTerms).toBeGreaterThan(singleTerm);
  });
});

describe('extractSnippet', () => {
  it('centers the snippet on the first body match with ellipsis markers', () => {
    const body =
      'A'.repeat(200) + ' the printer needs a new toner cartridge installed today ' + 'B'.repeat(200);
    const snippet = extractSnippet(body, ['toner'], 20);
    expect(snippet).toContain('toner');
    expect(snippet.startsWith('…')).toBe(true);
    expect(snippet.endsWith('…')).toBe(true);
  });

  it('falls back to the start of the body when no term is found in it', () => {
    const body = 'This body does not mention the search term at all, repeated content follows.';
    const snippet = extractSnippet(body, ['printer'], 20);
    expect(snippet.startsWith('This body')).toBe(true);
  });
});
