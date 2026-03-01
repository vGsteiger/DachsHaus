'use client';

import { ApolloProvider as ApolloProviderBase } from '@apollo/client';
import { apolloClient } from './client';

export function ApolloProvider({ children }: { children: React.ReactNode }) {
  return <ApolloProviderBase client={apolloClient}>{children}</ApolloProviderBase>;
}
