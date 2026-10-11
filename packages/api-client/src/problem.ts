// RFC 9457 Problem Details の型と、それを運ぶ例外クラス
export interface ProblemDetails {
  type?: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
  /** バックエンドの ProcoException(code, messageKey) に対応する拡張メンバ */
  code?: string;
  [extension: string]: unknown;
}

export class ProblemDetailsError extends Error {
  readonly problem: ProblemDetails;

  constructor(problem: ProblemDetails) {
    super(`${problem.status} ${problem.title}${problem.detail ? `: ${problem.detail}` : ''}`);
    this.name = 'ProblemDetailsError';
    this.problem = problem;
  }

  get status(): number {
    return this.problem.status;
  }
}

/** unknown な例外から ProblemDetails を取り出す（UI 表示用の補助） */
export function toProblemDetails(err: unknown): ProblemDetails {
  if (err instanceof ProblemDetailsError) return err.problem;
  if (err instanceof Error) return { title: err.name, status: 0, detail: err.message };
  return { title: 'Unknown error', status: 0, detail: String(err) };
}
