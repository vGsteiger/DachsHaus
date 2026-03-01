import { ApolloProvider } from '@/lib/apollo/provider';
import { AuthProvider } from '@/lib/auth/context';
import { Layout } from '@/components/layout/Layout';
import './globals.css';

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <ApolloProvider>
          <AuthProvider>
            <Layout>{children}</Layout>
          </AuthProvider>
        </ApolloProvider>
      </body>
    </html>
  );
}
