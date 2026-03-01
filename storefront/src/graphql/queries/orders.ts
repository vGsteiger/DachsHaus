// GraphQL queries for orders
export const GET_MY_ORDERS = `
  query GetMyOrders {
    myOrders {
      id
      status
    }
  }
`;
