export interface Article {
  id: string;
  title: string;
  category: string;
  body: string;
  tags: string[];
}

export interface ArticleSummary {
  id: string;
  title: string;
  category: string;
  tags: string[];
}

export interface SearchHit {
  id: string;
  title: string;
  category: string;
  score: number;
  snippet: string;
}
