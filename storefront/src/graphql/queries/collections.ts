// GraphQL queries for collections
export const GET_COLLECTIONS = `
  query GetCollections {
    collections {
      id
      name
    }
  }
`;
