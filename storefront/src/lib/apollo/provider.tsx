'use client';

import type { ReactNode } from 'react';
import { ApolloProvider as ApolloProviderBase } from '@apollo/client';
import { apolloClient } from './client';

export function ApolloProvider({ children }: { children: ReactNode }) {
  return <ApolloProviderBase client={apolloClient}>{children}</ApolloProviderBase>;
}
