/**
 * Safely evaluates mathematical expressions (e.g. "250 + 120 * 1.05", "4500 / 3")
 * directly inside the amount input field, replicating Android MathEvaluator.kt.
 */
export function evaluateMathExpression(expression: string): { isValid: boolean; value: number; formatted: string } {
  if (!expression || expression.trim() === '') {
    return { isValid: false, value: 0, formatted: '0' };
  }

  const sanitized = expression.replace(/,/g, '.').replace(/\s+/g, '');
  
  // Return directly if it's already a clean positive number
  if (/^\d+(\.\d+)?$/.test(sanitized)) {
    const val = parseFloat(sanitized);
    return { isValid: true, value: val, formatted: sanitized };
  }

  // Check for allowed characters: numbers, +, -, *, /, %, (, ), .
  if (/[^0-9+\-*/%().]/.test(sanitized)) {
    return { isValid: false, value: 0, formatted: expression };
  }

  try {
    // Function constructor safer than eval for mathematical expressions
    const fn = new Function(`return (${sanitized});`);
    const result = fn();

    if (typeof result === 'number' && !isNaN(result) && isFinite(result)) {
      const rounded = Math.round(result * 100) / 100;
      return {
        isValid: true,
        value: Math.max(0, rounded),
        formatted: rounded.toString()
      };
    }
  } catch (e) {
    // Invalid math expression while typing
  }

  return { isValid: false, value: 0, formatted: expression };
}
