import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import * as request from 'supertest';
import { AppModule } from '../src/app.module';
import { AuthVerifyService } from '../src/security/auth-verify.service';

describe('Gateway E2E Tests', () => {
  let app: INestApplication;
  let authVerifyService: AuthVerifyService;
  let originalEnv: NodeJS.ProcessEnv;

  beforeAll(async () => {
    originalEnv = process.env;
    process.env.HMAC_SECRET = 'test-secret-for-e2e';
    process.env.AUTH_SERVICE_URL = 'http://mock-auth-service:8084';

    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    authVerifyService = moduleFixture.get<AuthVerifyService>(AuthVerifyService);

    await app.init();
  });

  afterAll(async () => {
    process.env = originalEnv;
    await app.close();
  });

  describe('Health endpoints', () => {
    it('/health (GET) should return 200', () => {
      return request(app.getHttpServer())
        .get('/health')
        .expect(200);
    });

    it('/healthz (GET) should return 200', () => {
      return request(app.getHttpServer())
        .get('/healthz')
        .expect(200);
    });
  });

  describe('GraphQL requests - public operations', () => {
    it('should allow GetProducts without authentication', async () => {
      const query = `
        query GetProducts {
          products {
            id
            name
          }
        }
      `;

      // Note: This will fail to reach the actual GraphQL endpoint since subgraphs aren't running
      // but it should pass the middleware layer
      const response = await request(app.getHttpServer())
        .post('/graphql')
        .send({ query })
        .expect((res) => {
          // Should not be blocked by auth middleware (401)
          expect(res.status).not.toBe(401);
        });

      // Verify that security headers were added
      // (This would be checked in integration tests with actual subgraphs)
    });

    it('should allow Login without authentication', async () => {
      const query = `
        mutation Login($email: String!, $password: String!) {
          login(email: $email, password: $password) {
            token
          }
        }
      `;

      await request(app.getHttpServer())
        .post('/graphql')
        .send({
          query,
          variables: { email: 'test@example.com', password: 'password' }
        })
        .expect((res) => {
          expect(res.status).not.toBe(401);
        });
    });

    it('should allow Register without authentication', async () => {
      const query = `
        mutation Register($input: RegisterInput!) {
          register(input: $input) {
            token
          }
        }
      `;

      await request(app.getHttpServer())
        .post('/graphql')
        .send({
          query,
          variables: { input: { email: 'new@example.com', password: 'password' } }
        })
        .expect((res) => {
          expect(res.status).not.toBe(401);
        });
    });
  });

  describe('GraphQL requests - protected operations', () => {
    it('should block GetProfile without authentication', async () => {
      const query = `
        query GetProfile {
          profile {
            id
            email
          }
        }
      `;

      await request(app.getHttpServer())
        .post('/graphql')
        .send({ query })
        .expect(401)
        .expect((res) => {
          expect(res.body.errors).toBeDefined();
          expect(res.body.errors[0].message).toBe('Authentication required');
          expect(res.body.errors[0].extensions.code).toBe('UNAUTHENTICATED');
        });
    });

    it('should allow protected operation with valid token', async () => {
      // Mock the auth verification
      jest.spyOn(authVerifyService, 'verify').mockResolvedValue({
        valid: true,
        userId: 'user-123',
        email: 'test@example.com',
        roles: ['customer'],
      });

      const query = `
        query GetProfile {
          profile {
            id
            email
          }
        }
      `;

      await request(app.getHttpServer())
        .post('/graphql')
        .set('Authorization', 'Bearer valid-token')
        .send({ query })
        .expect((res) => {
          // Should not be blocked by auth (401)
          expect(res.status).not.toBe(401);
        });
    });

    it('should block protected operation with invalid token', async () => {
      jest.spyOn(authVerifyService, 'verify').mockResolvedValue({
        valid: false,
        reason: 'expired',
      });

      const query = `
        query GetProfile {
          profile {
            id
            email
          }
        }
      `;

      await request(app.getHttpServer())
        .post('/graphql')
        .set('Authorization', 'Bearer expired-token')
        .send({ query })
        .expect(401);
    });
  });

  describe('Request signing', () => {
    it('should add security headers to all requests', async () => {
      const query = `
        query GetProducts {
          products {
            id
          }
        }
      `;

      // Since we can't easily inspect the headers sent to subgraphs in unit tests,
      // this would be better verified in integration tests with actual subgraphs
      // For now, we just verify the request goes through
      await request(app.getHttpServer())
        .post('/graphql')
        .send({ query })
        .expect((res) => {
          expect(res.status).not.toBe(401);
        });
    });
  });
});
