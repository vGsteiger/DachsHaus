import { Module } from '@nestjs/common';
import { GraphQLModule } from '@nestjs/graphql';
import { ApolloGatewayDriver, ApolloGatewayDriverConfig } from '@nestjs/apollo';
import { IntrospectAndCompose } from '@apollo/gateway';

@Module({
  imports: [
    GraphQLModule.forRoot<ApolloGatewayDriverConfig>({
      driver: ApolloGatewayDriver,
      gateway: {
        supergraphSdl: new IntrospectAndCompose({
          subgraphs: [
            { name: 'auth', url: process.env.AUTH_SERVICE_URL || 'http://localhost:8081/graphql' },
            { name: 'catalog', url: process.env.CATALOG_SERVICE_URL || 'http://localhost:8082/graphql' },
            { name: 'order', url: process.env.ORDER_SERVICE_URL || 'http://localhost:8083/graphql' },
            { name: 'customer', url: process.env.CUSTOMER_SERVICE_URL || 'http://localhost:8084/graphql' },
            { name: 'cart', url: process.env.CART_SERVICE_URL || 'http://localhost:8085/graphql' },
          ],
        }),
      },
    }),
  ],
})
export class GatewayModule {}
