export function debounce<TArgs extends unknown[]>(
  fn: (...args: TArgs) => void,
  waitMs: number,
): (...args: TArgs) => void {
  let timer: number | undefined
  return (...args: TArgs) => {
    if (timer) window.clearTimeout(timer)
    timer = window.setTimeout(() => fn(...args), waitMs)
  }
}

