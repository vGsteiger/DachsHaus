// Operation parser utility
export function parseOperation(query: string): string | null {
  if (!query || typeof query !== 'string') {
    return null;
  }

  // Remove comments (both single-line and multi-line)
  const cleanedQuery = query.replace(/#[^\n]*/g, '').replace(/"""[\s\S]*?"""/g, '').replace(/"[^"]*"/g, '');

  // Match operation pattern: (query|mutation|subscription) OperationName
  // or just OperationName { ... } for shorthand queries
  const operationMatch = cleanedQuery.match(/(?:query|mutation|subscription)\s+(\w+)/);
  if (operationMatch) {
    return operationMatch[1];
  }

  // Try to match shorthand query (operation name without keyword)
  const shorthandMatch = cleanedQuery.match(/^\s*(\w+)\s*[({]/);
  if (shorthandMatch && shorthandMatch[1] !== 'query' && shorthandMatch[1] !== 'mutation' && shorthandMatch[1] !== 'subscription') {
    return shorthandMatch[1];
  }

  // For IntrospectionQuery or anonymous operations
  if (cleanedQuery.includes('__schema') || cleanedQuery.includes('__type')) {
    return 'IntrospectionQuery';
  }

  return null;
}
