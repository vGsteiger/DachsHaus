'use client';

import { ApolloProvider as ApolloProviderBase } from '@apollo/client/react';
import type { ReactNode } from 'react';

import { apolloClient } from './client';

export function ApolloProvider({ children }: { children: ReactNode }) {
  return <ApolloProviderBase client={apolloClient}>{children}</ApolloProviderBase>;
}
