// GraphQL mutations for cart
export const ADD_TO_CART = `
  mutation AddToCart($productId: ID!) {
    addToCart(productId: $productId) {
      id
    }
  }
`;
