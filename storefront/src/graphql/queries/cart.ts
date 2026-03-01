// GraphQL queries for cart
export const GET_CART = `
  query GetCart {
    cart {
      id
      items {
        id
      }
    }
  }
`;
