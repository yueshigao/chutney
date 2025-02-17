export abstract class Equals<T> {
  abstract equals(obj: T): boolean;
}

// Unlike classes, interfaces exist only at compile-time. It is not possible to do a common instanceof check
export function instanceOfEquals<T>(obj: any): obj is Equals<T> {
  return 'equals' in obj;
}

export function areEquals(val1: any, val2: any): boolean {
  // NaN === NaN --> false
  // isNaN(undefined) --> true
  if (isNaN(val1) && isNaN(val2) && typeof val1 === 'number' && typeof val2 === 'number') return true;

  if (val1 === val2) return true;

  if (typeof val1 === 'object') {
    if (val1 == null) return false;
    if (val2 == null) return false;

    if ((val1 instanceof Array) && (val2 instanceof Array)) {
      if (val1.length != val2.length) return false;
      for (const idx in val1) {
        if (!areEquals(val1[idx], val2[idx])) return false;
      }

      return true;
    }

    if (instanceOfEquals(val1) && instanceOfEquals(val2)) {
      return val1.equals(val2);
    }

    return val1 == val2;
  }

  return false;
}

export function isNotEmpty(value: string): boolean {
  return value && value !== '';
}
