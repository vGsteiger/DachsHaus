// GraphQL subscriptions for order status
export const ORDER_STATUS_CHANGED = `
  subscription OrderStatusChanged($orderId: ID!) {
    orderStatusChanged(orderId: $orderId) {
      status
    }
  }
`;
